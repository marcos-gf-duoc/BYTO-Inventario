package com.byto.inventario.ui.screens.reposicion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byto.inventario.data.model.EstadoAlerta
import com.byto.inventario.data.model.ExistenciaDetalle
import com.byto.inventario.data.model.TipoAlerta
import com.byto.inventario.data.model.TipoUbicacion
import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.ReglaNegocioException
import com.byto.inventario.data.repository.SesionRepository
import com.byto.inventario.data.repository.SinConexionException
import com.byto.inventario.domain.ErrorReposicion
import com.byto.inventario.domain.ErroresReposicion
import com.byto.inventario.domain.ValidacionReposicion
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Confirmación que la pantalla muestra en un aviso tras declarar (evento de un solo uso). */
data class ReposicionDeclarada(val nombreProducto: String, val tiendaId: String)

/**
 * Estado de P12.
 * @param tiendas tiendas activas a cargo del Encargado (las que puede elegir).
 * @param productos existencias de la tienda elegida, de menor a mayor stock.
 * @param errores errores que se muestran ahora. Los de "obligatorio" solo aparecen después
 * del primer intento de envío, para no marcar en rojo un formulario recién abierto.
 * @param errorGeneral el envío falló por algo que no es de un campo.
 */
data class EstadoReposicion(
    val cargando: Boolean = true,
    val tiendas: List<Ubicacion> = emptyList(),
    val tiendaId: String? = null,
    val cargandoProductos: Boolean = false,
    val productos: List<ExistenciaDetalle> = emptyList(),
    val codigoInterno: String? = null,
    val observacion: String = "",
    val errores: ErroresReposicion = ErroresReposicion(),
    val enviando: Boolean = false,
    val declarada: ReposicionDeclarada? = null,
    val errorGeneral: Boolean = false,
    val error: Boolean = false,
    val irASinConexion: Boolean = false,
    val sinSesion: Boolean = false
) {
    /** El botón se habilita mientras no haya errores visibles ni un envío en curso. */
    val puedeEnviar: Boolean get() = !cargando && !enviando && !errores.hayErrores
}

class DeclararReposicionViewModel(
    private val repositorio: InventarioRepository,
    private val sesionRepository: SesionRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoReposicion())
    val estado: StateFlow<EstadoReposicion> = _estado.asStateFlow()

    /** Reposiciones abiertas como pares (tienda, código): una sola por producto y tienda. */
    private var abiertas: Set<Pair<String, String>> = emptySet()

    /** true después del primer toque en "Declarar reposición". */
    private var intentoEnviar = false

    private var cargaProductos: Job? = null

    init {
        cargar()
    }

    private fun cargar() {
        val usuario = sesionRepository.usuarioActivo.value
        if (usuario == null) {
            _estado.update { it.copy(cargando = false, sinSesion = true) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = false) }
            try {
                // Solo tiendas: la reposición se cumple con una entrada en la tienda.
                // Solo activas: en una tienda deshabilitada no se puede registrar esa entrada.
                val tiendas = repositorio.obtenerUbicaciones(usuario.id).filter {
                    it.tipo == TipoUbicacion.TIENDA && it.activa && it.id in usuario.ubicacionesConsultaIds
                }
                abiertas = repositorio.obtenerAlertas(tipo = TipoAlerta.REPOSICION, estado = EstadoAlerta.ABIERTA)
                    .map { it.ubicacionId to it.codigoInterno }
                    .toSet()
                _estado.update { it.copy(cargando = false, tiendas = tiendas) }
            } catch (e: SinConexionException) {
                // cargando = false: al volver de P16, alMostrarse ve que no hay tiendas y reintenta.
                _estado.update { it.copy(cargando = false, irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                _estado.update { it.copy(cargando = false, error = true) }
            }
        }
    }

    /** Al cambiar de tienda se borra el producto elegido: puede no tener sentido en la otra. */
    fun elegirTienda(tiendaId: String) {
        if (tiendaId == _estado.value.tiendaId) return
        _estado.update { it.copy(tiendaId = tiendaId, codigoInterno = null, productos = emptyList()) }
        validar()
        cargaProductos?.cancel()
        cargaProductos = viewModelScope.launch {
            _estado.update { it.copy(cargandoProductos = true) }
            try {
                // Primero los que más necesitan reposición (menos stock).
                val productos = repositorio.obtenerExistencias(tiendaId).sortedBy { it.existencia.stock }
                _estado.update { it.copy(cargandoProductos = false, productos = productos) }
            } catch (e: SinConexionException) {
                _estado.update { it.copy(cargandoProductos = false, irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                _estado.update { it.copy(cargandoProductos = false, error = true) }
            }
        }
    }

    fun elegirProducto(codigoInterno: String) {
        _estado.update { it.copy(codigoInterno = codigoInterno) }
        validar()
    }

    fun cambiarObservacion(texto: String) {
        _estado.update { it.copy(observacion = texto) }
        validar()
    }

    /** Endpoint 10. Valida todo primero; si hay errores, solo los muestra. */
    fun declarar() {
        intentoEnviar = true
        validar()
        val actual = _estado.value
        if (actual.errores.hayErrores || actual.enviando) return
        val usuario = sesionRepository.usuarioActivo.value
        val tiendaId = actual.tiendaId ?: return
        val codigo = actual.codigoInterno ?: return
        if (usuario == null) {
            _estado.update { it.copy(sinSesion = true) }
            return
        }
        val nombre = actual.productos.firstOrNull { it.producto.codigoInterno == codigo }?.producto?.nombre ?: codigo

        _estado.update { it.copy(enviando = true, errorGeneral = false) }
        viewModelScope.launch {
            try {
                repositorio.declararReposicion(
                    ubicacionId = tiendaId,
                    codigoInterno = codigo,
                    declaradaPor = usuario.id,
                    observacion = actual.observacion.trim().ifEmpty { null }
                )
                abiertas = abiertas + (tiendaId to codigo)
                // Listo para declarar otra en la misma tienda: se limpian producto y observación.
                intentoEnviar = false
                _estado.update {
                    it.copy(
                        enviando = false,
                        codigoInterno = null,
                        observacion = "",
                        errores = ErroresReposicion(),
                        declarada = ReposicionDeclarada(nombre, tiendaId)
                    )
                }
            } catch (e: SinConexionException) {
                _estado.update { it.copy(enviando = false, irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                // La regla también la revisa el repositorio: si otra persona la declaró recién,
                // el error se muestra en el campo que corresponde.
                when (e.codigo) {
                    "REPOSICION_YA_ABIERTA" -> {
                        abiertas = abiertas + (tiendaId to codigo)
                        _estado.update { it.copy(enviando = false) }
                        validar()
                    }
                    "OBSERVACION_MUY_LARGA" -> {
                        _estado.update { it.copy(enviando = false) }
                        validar()
                    }
                    else -> _estado.update { it.copy(enviando = false, errorGeneral = true) }
                }
            }
        }
    }

    /**
     * Recalcula los errores con la regla del dominio. Antes del primer intento se ocultan
     * los de "obligatorio"; los demás (reposición repetida, observación larga) se ven al instante.
     */
    private fun validar() {
        _estado.update { actual ->
            val todos = ValidacionReposicion.validar(actual.tiendaId, actual.codigoInterno, actual.observacion, abiertas)
            val visibles = if (intentoEnviar) {
                todos
            } else {
                todos.copy(
                    tienda = null,
                    producto = todos.producto.takeIf { it != ErrorReposicion.PRODUCTO_OBLIGATORIO }
                )
            }
            actual.copy(errores = visibles)
        }
    }

    fun avisoMostrado() {
        _estado.update { it.copy(declarada = null) }
    }

    fun navegacionSinConexionRealizada() {
        _estado.update { it.copy(irASinConexion = false) }
    }

    /** Al volver de P16 se reintenta la carga inicial si había fallado. */
    fun alMostrarse() {
        if (_estado.value.tiendas.isEmpty() && !_estado.value.cargando) cargar()
    }
}
