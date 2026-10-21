package com.byto.inventario.ui.screens.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byto.inventario.data.model.FiltroMovimientos
import com.byto.inventario.data.model.ResumenUbicacion
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.ReglaNegocioException
import com.byto.inventario.data.repository.SesionRepository
import com.byto.inventario.data.repository.SinConexionException
import com.byto.inventario.domain.Fechas
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de P05.
 * @param resumenes solo las ubicaciones con alertas abiertas, de más a menos alertas.
 */
data class EstadoInicioGerente(
    val cargando: Boolean = true,
    val gerenteId: String = "",
    val ubicacionesActivas: Int = 0,
    val totalUbicaciones: Int = 0,
    val alertasAbiertas: Int = 0,
    val movimientosHoy: Int = 0,
    val resumenes: List<ResumenUbicacion> = emptyList(),
    val error: Boolean = false,
    val irASinConexion: Boolean = false,
    val sinSesion: Boolean = false
)

class InicioGerenteViewModel(
    private val repositorio: InventarioRepository,
    private val sesionRepository: SesionRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoInicioGerente())
    val estado: StateFlow<EstadoInicioGerente> = _estado.asStateFlow()

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
            _estado.update { it.copy(cargando = it.error, gerenteId = usuario.id, error = false) }
            try {
                // "Hoy" se mide en hora de Chile, aunque los movimientos se guarden en UTC.
                val (desde, hasta) = Fechas.rangoDeHoyEnChile()
                val pedidoTablero = async { repositorio.obtenerTablero() }
                val pedidoMovimientos = async {
                    repositorio.obtenerMovimientos(FiltroMovimientos(desde = desde, hasta = hasta))
                }
                val tablero = pedidoTablero.await()
                val movimientosHoy = pedidoMovimientos.await()

                _estado.update {
                    it.copy(
                        cargando = false,
                        ubicacionesActivas = tablero.resumenes.count { r -> r.ubicacion.activa },
                        totalUbicaciones = tablero.resumenes.size,
                        alertasAbiertas = tablero.totalAlertasAbiertas,
                        movimientosHoy = movimientosHoy.size,
                        resumenes = tablero.resumenes
                            .filter { r -> r.alertasAbiertas > 0 }
                            .sortedByDescending { r -> r.alertasAbiertas }
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
