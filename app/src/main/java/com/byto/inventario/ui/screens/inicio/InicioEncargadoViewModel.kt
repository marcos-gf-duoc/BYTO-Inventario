package com.byto.inventario.ui.screens.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byto.inventario.data.model.EstadoAlerta
import com.byto.inventario.data.model.TipoAlerta
import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.ReglaNegocioException
import com.byto.inventario.data.repository.SesionRepository
import com.byto.inventario.data.repository.SinConexionException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de P04. Los contadores consideran todas las ubicaciones del Encargado
 * (su bodega y las tiendas que consulta), no solo la ubicación activa.
 */
data class EstadoInicioEncargado(
    val cargando: Boolean = true,
    val encargadoId: String = "",
    val alertasAbiertas: Int = 0,
    val descalcesPorRevisar: Int = 0,
    val reposicionesAbiertas: Int = 0,
    val tiendasACargo: List<Ubicacion> = emptyList(),
    val error: Boolean = false,
    val irASinConexion: Boolean = false,
    val sinSesion: Boolean = false
)

class InicioEncargadoViewModel(
    private val repositorio: InventarioRepository,
    private val sesionRepository: SesionRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoInicioEncargado())
    val estado: StateFlow<EstadoInicioEncargado> = _estado.asStateFlow()

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
            _estado.update { it.copy(cargando = it.error, encargadoId = usuario.id, error = false) }
            try {
                val pedidoUbicaciones = async { repositorio.obtenerUbicaciones(usuario.id) }
                val pedidoAlertas = async { repositorio.obtenerAlertas(estado = EstadoAlerta.ABIERTA) }
                val ubicaciones = pedidoUbicaciones.await()

                // Solo cuentan las alertas de sus ubicaciones (bodega + tiendas en consulta).
                val misIds = usuario.ubicacionesIds + usuario.ubicacionesConsultaIds
                val misAlertas = pedidoAlertas.await().filter { it.ubicacionId in misIds }

                _estado.update {
                    it.copy(
                        cargando = false,
                        alertasAbiertas = misAlertas.size,
                        descalcesPorRevisar = misAlertas.count { a -> a.tipo == TipoAlerta.DESCALCE },
                        reposicionesAbiertas = misAlertas.count { a -> a.tipo == TipoAlerta.REPOSICION },
                        tiendasACargo = ubicaciones.filter { u -> u.id in usuario.ubicacionesConsultaIds }
                    )
                }
            } catch (e: SinConexionException) {
                _estado.update { it.copy(irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                _estado.update { it.copy(cargando = false, error = true) }
            }
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
}
