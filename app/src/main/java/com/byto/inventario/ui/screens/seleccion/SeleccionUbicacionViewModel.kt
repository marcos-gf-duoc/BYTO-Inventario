package com.byto.inventario.ui.screens.seleccion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byto.inventario.data.model.Perfil
import com.byto.inventario.data.model.TipoUbicacion
import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.data.model.Usuario
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.ReglaNegocioException
import com.byto.inventario.data.repository.SesionRepository
import com.byto.inventario.data.repository.SinConexionException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Una ubicación tal como se muestra en P02.
 * @param soloConsulta el usuario puede verla pero no registrar movimientos (caso del Encargado).
 * @param seleccionable false = deshabilitada (solo la ve el Gerente, con la etiqueta "Inactiva").
 */
data class OpcionUbicacion(
    val ubicacion: Ubicacion,
    val soloConsulta: Boolean,
    val seleccionable: Boolean
)

/**
 * Estado de P02.
 * @param sinSesion no hay usuario activo (no debería pasar): la pantalla vuelve al login.
 */
data class EstadoSeleccion(
    val cargando: Boolean = true,
    val usuario: Usuario? = null,
    val opciones: List<OpcionUbicacion> = emptyList(),
    val error: Boolean = false,
    val irASinConexion: Boolean = false,
    val sinSesion: Boolean = false
)

class SeleccionUbicacionViewModel(
    private val repositorio: InventarioRepository,
    private val sesionRepository: SesionRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoSeleccion())
    val estado: StateFlow<EstadoSeleccion> = _estado.asStateFlow()

    /** Se activa si la carga falló por falta de conexión, para reintentar al volver de P16. */
    private var reintentarAlVolver = false

    init {
        cargarUbicaciones()
    }

    private fun cargarUbicaciones() {
        val usuario = sesionRepository.usuarioActivo.value
        if (usuario == null) {
            _estado.update { it.copy(cargando = false, sinSesion = true) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, usuario = usuario, error = false) }
            try {
                val ubicaciones = repositorio.obtenerUbicaciones(usuario.id)
                _estado.update { it.copy(cargando = false, opciones = opcionesPara(usuario, ubicaciones)) }
            } catch (e: SinConexionException) {
                reintentarAlVolver = true
                _estado.update { it.copy(irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                _estado.update { it.copy(cargando = false, error = true) }
            }
        }
    }

    /**
     * Qué ubicaciones se ofrecen según el perfil (docs/decisiones_E1.md):
     * - Casa Matriz nunca, porque no tiene stock.
     * - Operador y Encargado: solo las activas.
     * - Gerente: también las inactivas, visibles pero no seleccionables (T-CC-3).
     * - Las de ubicacionesConsultaIds se marcan "Solo consulta" (tiendas del Encargado).
     */
    private fun opcionesPara(usuario: Usuario, ubicaciones: List<Ubicacion>): List<OpcionUbicacion> =
        ubicaciones
            .filter { it.tipo != TipoUbicacion.ADMINISTRATIVA }
            .filter { it.activa || usuario.perfil == Perfil.GERENTE }
            .map { ubicacion ->
                OpcionUbicacion(
                    ubicacion = ubicacion,
                    soloConsulta = ubicacion.id in usuario.ubicacionesConsultaIds,
                    seleccionable = ubicacion.activa
                )
            }

    fun navegacionSinConexionRealizada() {
        _estado.update { it.copy(irASinConexion = false) }
    }

    fun reintentarSiHaceFalta() {
        if (reintentarAlVolver) {
            reintentarAlVolver = false
            cargarUbicaciones()
        }
    }

    /** Guarda la ubicación activa y devuelve el perfil, para que la pantalla sepa a qué inicio ir. */
    fun elegir(opcion: OpcionUbicacion): Perfil? {
        if (!opcion.seleccionable) return null
        sesionRepository.seleccionarUbicacion(opcion.ubicacion)
        return _estado.value.usuario?.perfil
    }
}
