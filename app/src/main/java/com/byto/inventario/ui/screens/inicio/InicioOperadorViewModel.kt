package com.byto.inventario.ui.screens.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byto.inventario.data.model.EstadoAlerta
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.Producto
import com.byto.inventario.data.model.TipoAlerta
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
 * Estado de P03.
 * @param reposiciones productos con una reposición abierta en la ubicación activa.
 */
data class EstadoInicioOperador(
    val cargando: Boolean = true,
    val operadorId: String = "",
    val productosBajo: Int = 0,
    val productosCritico: Int = 0,
    val productosSinStock: Int = 0,
    val reposiciones: List<Producto> = emptyList(),
    val error: Boolean = false,
    val irASinConexion: Boolean = false,
    val sinSesion: Boolean = false
)

class InicioOperadorViewModel(
    private val repositorio: InventarioRepository,
    private val sesionRepository: SesionRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoInicioOperador())
    val estado: StateFlow<EstadoInicioOperador> = _estado.asStateFlow()

    private var reintentarAlVolver = false

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
            _estado.update { it.copy(cargando = true, operadorId = usuario.id, error = false) }
            try {
                // Las dos consultas a la vez: cada una tarda ~300 ms.
                val pedidoExistencias = async { repositorio.obtenerExistencias(ubicacion.id) }
                val pedidoReposiciones = async {
                    repositorio.obtenerAlertas(ubicacion.id, TipoAlerta.REPOSICION, EstadoAlerta.ABIERTA)
                }
                val existencias = pedidoExistencias.await()
                val reposiciones = pedidoReposiciones.await()

                // El estado de cada existencia ya viene calculado por el repositorio (umbrales del dominio).
                val estados = existencias.map { it.estado }
                // mapNotNull descarta los null: si una alerta no tuviera producto, se omite.
                val productosConReposicion = reposiciones.mapNotNull { alerta ->
                    existencias.find { it.producto.codigoInterno == alerta.codigoInterno }?.producto
                }

                _estado.update {
                    it.copy(
                        cargando = false,
                        productosBajo = estados.count { e -> e == EstadoStock.BAJO },
                        productosCritico = estados.count { e -> e == EstadoStock.CRITICO },
                        productosSinStock = estados.count { e -> e == EstadoStock.SIN_STOCK },
                        reposiciones = productosConReposicion
                    )
                }
            } catch (e: SinConexionException) {
                reintentarAlVolver = true
                _estado.update { it.copy(irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
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
}
