package com.byto.inventario.ui.screens.movimiento

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byto.inventario.data.model.Alerta
import com.byto.inventario.data.model.FiltroMovimientos
import com.byto.inventario.data.model.Movimiento
import com.byto.inventario.data.model.Producto
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.ReglaNegocioException
import com.byto.inventario.data.repository.SesionRepository
import com.byto.inventario.data.repository.SinConexionException
import com.byto.inventario.ui.navigation.Pantalla
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Qué le pasó a una alerta con este movimiento. */
enum class CambioAlerta { NUEVA, ACTUALIZADA, CERRADA }

data class AlertaConCambio(val alerta: Alerta, val cambio: CambioAlerta)

/** Estado de P10. */
data class EstadoConfirmacion(
    val cargando: Boolean = true,
    val movimiento: Movimiento? = null,
    val producto: Producto? = null,
    val alertas: List<AlertaConCambio> = emptyList(),
    val error: Boolean = false,
    val irASinConexion: Boolean = false,
    val sinSesion: Boolean = false
)

/**
 * Recibe por la ruta el ID del movimiento y los IDs de sus alertas (nuevas, actualizadas
 * y cerradas), y pide al repositorio los datos completos para mostrarlos.
 */
class ConfirmacionViewModel(
    private val repositorio: InventarioRepository,
    private val sesionRepository: SesionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val movimientoId = savedStateHandle.get<String>(Pantalla.ARG_MOVIMIENTO_ID).orEmpty()

    // Las listas llegan como texto separado por comas ("ALE-0008,ALE-0009"); vacío = ninguna.
    private val idsPorCambio: Map<CambioAlerta, List<String>> = mapOf(
        CambioAlerta.NUEVA to ids(savedStateHandle, Pantalla.ARG_GENERADAS),
        CambioAlerta.ACTUALIZADA to ids(savedStateHandle, Pantalla.ARG_ACTUALIZADAS),
        CambioAlerta.CERRADA to ids(savedStateHandle, Pantalla.ARG_CERRADAS)
    )

    private val _estado = MutableStateFlow(EstadoConfirmacion())
    val estado: StateFlow<EstadoConfirmacion> = _estado.asStateFlow()

    private var reintentarAlVolver = false

    init {
        cargar()
    }

    private fun cargar() {
        val ubicacion = sesionRepository.ubicacionActiva.value
        if (ubicacion == null) {
            _estado.update { it.copy(cargando = false, sinSesion = true) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = false) }
            try {
                val pedidoMovimientos = async {
                    repositorio.obtenerMovimientos(FiltroMovimientos(ubicacionId = ubicacion.id))
                }
                val pedidoAlertas = async { repositorio.obtenerAlertas(ubicacionId = ubicacion.id) }
                val movimiento = pedidoMovimientos.await().first { it.id == movimientoId }
                val todas = pedidoAlertas.await()
                val producto = repositorio.obtenerExistencia(ubicacion.id, movimiento.codigoInterno).producto

                // Orden: nuevas, actualizadas y cerradas (como se leen de arriba abajo).
                val alertas = CambioAlerta.entries.flatMap { cambio ->
                    idsPorCambio.getValue(cambio).mapNotNull { id ->
                        todas.find { it.id == id }?.let { AlertaConCambio(it, cambio) }
                    }
                }
                _estado.update {
                    it.copy(cargando = false, movimiento = movimiento, producto = producto, alertas = alertas)
                }
            } catch (e: SinConexionException) {
                reintentarAlVolver = true
                _estado.update { it.copy(irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                _estado.update { it.copy(cargando = false, error = true) }
            } catch (e: NoSuchElementException) {
                // first{} no encontró el movimiento (no debería pasar).
                _estado.update { it.copy(cargando = false, error = true) }
            }
        }
    }

    fun navegacionSinConexionRealizada() {
        _estado.update { it.copy(irASinConexion = false) }
    }

    fun reintentarSiHaceFalta() {
        if (reintentarAlVolver) {
            reintentarAlVolver = false
            cargar()
        }
    }

    private companion object {
        fun ids(handle: SavedStateHandle, clave: String): List<String> =
            handle.get<String>(clave).orEmpty().split(",").filter { it.isNotBlank() }
    }
}
