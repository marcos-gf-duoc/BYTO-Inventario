package com.byto.inventario.ui.screens.inventario

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.ExistenciaDetalle
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.ReglaNegocioException
import com.byto.inventario.data.repository.SesionRepository
import com.byto.inventario.data.repository.SinConexionException
import com.byto.inventario.domain.BusquedaTexto
import com.byto.inventario.ui.navigation.Pantalla
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de P06.
 * @param todas todas las existencias de la ubicación activa (se cargan una vez).
 * @param resultados las que cumplen la búsqueda y el filtro; es lo que se dibuja.
 * @param filtro estado elegido en los chips; null = "Todos".
 */
data class EstadoInventario(
    val cargando: Boolean = true,
    val todas: List<ExistenciaDetalle> = emptyList(),
    val resultados: List<ExistenciaDetalle> = emptyList(),
    val busqueda: String = "",
    val filtro: EstadoStock? = null,
    val error: Boolean = false,
    val irASinConexion: Boolean = false,
    val sinSesion: Boolean = false
)

/**
 * Se carga el inventario completo una vez y se filtra en memoria: con 25 productos
 * es instantáneo y evita una llamada (con su espera) por cada letra escrita.
 *
 * @param savedStateHandle trae los argumentos de la ruta; aquí, el filtro inicial
 * cuando se llega desde las tarjetas del inicio ("inventario?estado=BAJO").
 */
class InventarioViewModel(
    private val repositorio: InventarioRepository,
    private val sesionRepository: SesionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _estado = MutableStateFlow(
        EstadoInventario(
            // runCatching evita un cierre de la app si el texto no es un estado válido.
            filtro = savedStateHandle.get<String>(Pantalla.ARG_ESTADO)
                ?.let { runCatching { EstadoStock.valueOf(it) }.getOrNull() }
        )
    )
    val estado: StateFlow<EstadoInventario> = _estado.asStateFlow()

    /** false hasta la primera vez que la pantalla se muestra (esa carga ya la hizo init). */
    private var yaSeMostro = false

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
            _estado.update { it.copy(cargando = it.error, error = false) }
            try {
                val existencias = repositorio.obtenerExistencias(ubicacion.id)
                actualizar { it.copy(cargando = false, todas = existencias) }
            } catch (e: SinConexionException) {
                _estado.update { it.copy(irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                _estado.update { it.copy(cargando = false, error = true) }
            }
        }
    }

    fun cambiarBusqueda(texto: String) {
        actualizar { it.copy(busqueda = texto) }
    }

    fun cambiarFiltro(filtro: EstadoStock?) {
        actualizar { it.copy(filtro = filtro) }
    }

    fun limpiar() {
        actualizar { it.copy(busqueda = "", filtro = null) }
    }

    /**
     * Aplica un cambio y recalcula [EstadoInventario.resultados] en el mismo paso,
     * para que la lista nunca quede desfasada de la búsqueda o el filtro.
     */
    private fun actualizar(cambio: (EstadoInventario) -> EstadoInventario) {
        _estado.update { actual ->
            val nuevo = cambio(actual)
            nuevo.copy(
                resultados = nuevo.todas.filter { detalle ->
                    (nuevo.filtro == null || detalle.estado == nuevo.filtro) &&
                        BusquedaTexto.coincide(detalle.producto, nuevo.busqueda)
                }
            )
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
