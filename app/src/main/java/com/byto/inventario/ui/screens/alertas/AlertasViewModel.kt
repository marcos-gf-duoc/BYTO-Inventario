package com.byto.inventario.ui.screens.alertas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byto.inventario.data.model.Alerta
import com.byto.inventario.data.model.EstadoAlerta
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.FiltroMovimientos
import com.byto.inventario.data.model.TipoAlerta
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.ReglaNegocioException
import com.byto.inventario.data.repository.SesionRepository
import com.byto.inventario.data.repository.SinConexionException
import com.byto.inventario.domain.Permisos
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Una alerta lista para dibujar: la alerta más los datos que la tarjeta necesita
 * y que la alerta no trae (el nombre del producto, su stock actual, quién contó).
 *
 * @param stockActual solo STOCK_BAJO: unidades que hay hoy en esa ubicación.
 * @param operadorConteo solo DESCALCE: ID anonimizado de quien hizo el conteo.
 */
data class FilaAlerta(
    val alerta: Alerta,
    val nombreProducto: String,
    val stockActual: Int? = null,
    val operadorConteo: String? = null
)

/** Avisos de un solo uso que la pantalla muestra en una barra inferior (Snackbar). */
enum class MensajeAlertas { DESCALCE_REVISADO, YA_NO_REVISABLE, ERROR_REVISION }

/**
 * Estado de P11.
 * @param todas las alertas abiertas de las ubicaciones del usuario, ya ordenadas.
 * @param visibles las que cumplen el filtro; es lo que se dibuja.
 * @param filtro tipo elegido en los chips; null = "Todas".
 * @param revisandoId alerta que se está marcando como revisada (su botón se desactiva).
 */
data class EstadoAlertas(
    val cargando: Boolean = true,
    val usuarioId: String = "",
    val todas: List<FilaAlerta> = emptyList(),
    val visibles: List<FilaAlerta> = emptyList(),
    val filtro: TipoAlerta? = null,
    val puedeRevisar: Boolean = false,
    val revisandoId: String? = null,
    val mensaje: MensajeAlertas? = null,
    val error: Boolean = false,
    val irASinConexion: Boolean = false,
    val sinSesion: Boolean = false
)

class AlertasViewModel(
    private val repositorio: InventarioRepository,
    private val sesionRepository: SesionRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoAlertas())
    val estado: StateFlow<EstadoAlertas> = _estado.asStateFlow()

    /** false hasta la primera vez que la pantalla se muestra (esa carga ya la hizo init). */
    private var yaSeMostro = false

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
            _estado.update {
                it.copy(
                    cargando = it.error || it.todas.isEmpty(),
                    usuarioId = usuario.id,
                    puedeRevisar = Permisos.puedeRevisarDescalces(usuario),
                    error = false
                )
            }
            try {
                // Las ubicaciones del usuario (todas si es Gerente) deciden qué alertas ve.
                val misUbicaciones = repositorio.obtenerUbicaciones(usuario.id).map { it.id }.toSet()
                val abiertas = repositorio.obtenerAlertas(estado = EstadoAlerta.ABIERTA)
                    .filter { it.ubicacionId in misUbicaciones }
                val filas = completar(abiertas)
                actualizar { it.copy(cargando = false, todas = filas.sortedWith(ORDEN)) }
            } catch (e: SinConexionException) {
                _estado.update { it.copy(irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                _estado.update { it.copy(cargando = false, error = true) }
            }
        }
    }

    /**
     * Busca lo que cada tarjeta necesita. Las consultas se lanzan a la vez (async) y se
     * espera a todas (awaitAll): así la espera total es la de una sola llamada, no la suma.
     */
    private suspend fun completar(alertas: List<Alerta>): List<FilaAlerta> = coroutineScope {
        // Una consulta por producto y ubicación distintos (endpoint 5): trae nombre y stock.
        val pedidosExistencia = alertas
            .map { it.ubicacionId to it.codigoInterno }
            .distinct()
            .map { clave -> async { clave to repositorio.obtenerExistencia(clave.first, clave.second) } }
        // El operador de un descalce está en el movimiento de conteo que lo originó (endpoint 8).
        val pedidosMovimientos = alertas
            .filter { it.tipo == TipoAlerta.DESCALCE }
            .map { it.ubicacionId }
            .distinct()
            .map { ubicacionId -> async { repositorio.obtenerMovimientos(FiltroMovimientos(ubicacionId = ubicacionId)) } }

        val existencias = pedidosExistencia.awaitAll().toMap()
        val movimientos = pedidosMovimientos.awaitAll().flatten().associateBy { it.id }

        alertas.map { alerta ->
            val detalle = existencias.getValue(alerta.ubicacionId to alerta.codigoInterno)
            FilaAlerta(
                alerta = alerta,
                nombreProducto = detalle.producto.nombre,
                stockActual = detalle.existencia.stock.takeIf { alerta.tipo == TipoAlerta.STOCK_BAJO },
                operadorConteo = alerta.movimientoId?.let { movimientos[it]?.operadorId }
                    ?.takeIf { alerta.tipo == TipoAlerta.DESCALCE }
            )
        }
    }

    fun cambiarFiltro(filtro: TipoAlerta?) {
        actualizar { it.copy(filtro = filtro) }
    }

    /** Endpoint 11. Al revisarse deja de estar abierta, así que sale de la lista. */
    fun marcarRevisado(alertaId: String) {
        if (_estado.value.revisandoId != null) return
        _estado.update { it.copy(revisandoId = alertaId) }
        viewModelScope.launch {
            try {
                repositorio.marcarDescalceRevisado(alertaId)
                actualizar {
                    it.copy(
                        revisandoId = null,
                        todas = it.todas.filterNot { fila -> fila.alerta.id == alertaId },
                        mensaje = MensajeAlertas.DESCALCE_REVISADO
                    )
                }
            } catch (e: SinConexionException) {
                _estado.update { it.copy(revisandoId = null, irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                // ALERTA_NO_REVISABLE: otra persona ya la revisó; se recarga para mostrar lo real.
                val yaRevisada = e.codigo == "ALERTA_NO_REVISABLE"
                _estado.update {
                    it.copy(
                        revisandoId = null,
                        mensaje = if (yaRevisada) MensajeAlertas.YA_NO_REVISABLE else MensajeAlertas.ERROR_REVISION
                    )
                }
                if (yaRevisada) cargar()
            }
        }
    }

    fun mensajeMostrado() {
        _estado.update { it.copy(mensaje = null) }
    }

    /** Aplica un cambio y recalcula [EstadoAlertas.visibles] en el mismo paso. */
    private fun actualizar(cambio: (EstadoAlertas) -> EstadoAlertas) {
        _estado.update { actual ->
            val nuevo = cambio(actual)
            nuevo.copy(visibles = nuevo.todas.filter { nuevo.filtro == null || it.alerta.tipo == nuevo.filtro })
        }
    }

    fun navegacionSinConexionRealizada() {
        _estado.update { it.copy(irASinConexion = false) }
    }

    /**
     * La pantalla lo llama cada vez que vuelve a verse. La primera vez no hace nada (init ya
     * cargó); las siguientes recarga, por ejemplo al volver de P16 o tras registrar un movimiento.
     */
    fun alMostrarse() {
        if (yaSeMostro) cargar() else yaSeMostro = true
    }

    private companion object {
        /**
         * Primero lo que pide una acción (reposiciones por cumplir y descalces por revisar),
         * luego el stock bajo del más grave al más leve y, dentro de cada grupo, lo más reciente.
         */
        val ORDEN: Comparator<FilaAlerta> = compareBy<FilaAlerta> { prioridad(it.alerta) }
            .thenByDescending { it.alerta.fechaUtc }

        fun prioridad(alerta: Alerta): Int = when (alerta.tipo) {
            TipoAlerta.REPOSICION -> 0
            TipoAlerta.DESCALCE -> 1
            TipoAlerta.STOCK_BAJO -> when (alerta.nivel) {
                EstadoStock.SIN_STOCK -> 2
                EstadoStock.CRITICO -> 3
                else -> 4
            }
        }
    }
}
