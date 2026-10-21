package com.byto.inventario.ui.screens.detalle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byto.inventario.data.model.ExistenciaDetalle
import com.byto.inventario.data.model.FiltroMovimientos
import com.byto.inventario.data.model.Movimiento
import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.ReglaNegocioException
import com.byto.inventario.data.repository.SesionRepository
import com.byto.inventario.data.repository.SinConexionException
import com.byto.inventario.domain.Permisos
import com.byto.inventario.ui.navigation.Pantalla
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de P07.
 * @param movimientos los últimos movimientos del producto en esta ubicación (más reciente primero).
 * @param puedeRegistrar true solo para el Operador en sus tiendas (ver [Permisos]).
 * @param noEncontrado el producto no existe en la ubicación activa.
 */
data class EstadoDetalle(
    val cargando: Boolean = true,
    val detalle: ExistenciaDetalle? = null,
    val ubicacion: Ubicacion? = null,
    val movimientos: List<Movimiento> = emptyList(),
    val puedeRegistrar: Boolean = false,
    val error: Boolean = false,
    val noEncontrado: Boolean = false,
    val irASinConexion: Boolean = false,
    val sinSesion: Boolean = false
)

/** @param savedStateHandle trae el código interno de la ruta ("detalle_producto/DE-03"). */
class DetalleProductoViewModel(
    private val repositorio: InventarioRepository,
    private val sesionRepository: SesionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val codigoInterno: String = savedStateHandle.get<String>(Pantalla.ARG_CODIGO_INTERNO).orEmpty()

    private val _estado = MutableStateFlow(EstadoDetalle())
    val estado: StateFlow<EstadoDetalle> = _estado.asStateFlow()

    /** false hasta la primera vez que la pantalla se muestra (esa carga ya la hizo init). */
    private var yaSeMostro = false

    init {
        cargar()
    }

    private fun cargar() {
        val usuario = sesionRepository.usuarioActivo.value
        val ubicacion = sesionRepository.ubicacionActiva.value
        if (usuario == null || ubicacion == null) {
            _estado.update { it.copy(cargando = false, sinSesion = true) }
            return
        }
        viewModelScope.launch {
            _estado.update {
                it.copy(
                    cargando = it.error,
                    error = false,
                    ubicacion = ubicacion,
                    puedeRegistrar = Permisos.puedeRegistrarMovimientos(usuario, ubicacion)
                )
            }
            try {
                val pedidoDetalle = async { repositorio.obtenerExistencia(ubicacion.id, codigoInterno) }
                val pedidoMovimientos = async {
                    repositorio.obtenerMovimientos(FiltroMovimientos(ubicacionId = ubicacion.id))
                }
                val detalle = pedidoDetalle.await()
                // El filtro de la API no tiene producto, así que se filtra aquí y se toman los 5 últimos.
                val movimientos = pedidoMovimientos.await()
                    .filter { it.codigoInterno == codigoInterno }
                    .take(MAX_MOVIMIENTOS)

                _estado.update { it.copy(cargando = false, detalle = detalle, movimientos = movimientos) }
            } catch (e: SinConexionException) {
                _estado.update { it.copy(irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                _estado.update {
                    it.copy(cargando = false, error = true, noEncontrado = e.codigo == "NO_ENCONTRADO")
                }
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

    private companion object {
        const val MAX_MOVIMIENTOS = 5
    }
}
