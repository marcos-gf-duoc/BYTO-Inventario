package com.byto.inventario.ui.screens.ubicaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byto.inventario.data.model.TipoUbicacion
import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.ReglaNegocioException
import com.byto.inventario.data.repository.SesionRepository
import com.byto.inventario.data.repository.SinConexionException
import com.byto.inventario.domain.Permisos
import com.byto.inventario.domain.Regiones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Ubicaciones de una región, en el orden de los datos (bodega primero, luego sus tiendas). */
data class GrupoRegion(val region: String, val ubicaciones: List<Ubicacion>)

/** Aviso tras un cambio (evento de un solo uso): qué ubicación cambió y cómo quedó. */
data class CambioRealizado(val ubicacionId: String, val activa: Boolean)

/**
 * Estado de P14.
 * @param cambiandoId ubicación cuyo cambio está en curso (su interruptor se desactiva).
 * @param porDesactivar ubicación que espera confirmación para desactivarse (abre un diálogo).
 */
data class EstadoUbicaciones(
    val cargando: Boolean = true,
    val grupos: List<GrupoRegion> = emptyList(),
    val cambiandoId: String? = null,
    val porDesactivar: Ubicacion? = null,
    val cambio: CambioRealizado? = null,
    val errorCambio: Boolean = false,
    val sinPermiso: Boolean = false,
    val error: Boolean = false,
    val irASinConexion: Boolean = false,
    val sinSesion: Boolean = false
) {
    private val todas: List<Ubicacion> get() = grupos.flatMap { it.ubicaciones }
    val total: Int get() = todas.size
    val activas: Int get() = todas.count { it.activa }
}

class AdministrarUbicacionesViewModel(
    private val repositorio: InventarioRepository,
    private val sesionRepository: SesionRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoUbicaciones())
    val estado: StateFlow<EstadoUbicaciones> = _estado.asStateFlow()

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
        // Defensa: P05 es el único acceso, pero la regla se revisa igual.
        if (!Permisos.puedeAdministrarUbicaciones(usuario)) {
            _estado.update { it.copy(cargando = false, sinPermiso = true) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = it.grupos.isEmpty(), error = false) }
            try {
                // Casa Matriz no tiene stock: no se activa ni desactiva desde aquí.
                val ubicaciones = repositorio.obtenerUbicaciones(usuario.id)
                    .filter { it.tipo != TipoUbicacion.ADMINISTRATIVA }
                _estado.update { it.copy(cargando = false, grupos = agrupar(ubicaciones)) }
            } catch (e: SinConexionException) {
                _estado.update { it.copy(cargando = false, irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                _estado.update { it.copy(cargando = false, error = true) }
            }
        }
    }

    /** groupBy conserva el orden de aparición: Santiago, La Serena y Concepción, como en los datos. */
    private fun agrupar(ubicaciones: List<Ubicacion>): List<GrupoRegion> =
        ubicaciones
            .groupBy { Regiones.nombreDe(it.id).orEmpty() }
            .map { (region, lista) -> GrupoRegion(region, lista) }

    /**
     * Activar es inmediato. Desactivar pide confirmar primero: deja a los operadores
     * sin poder registrar movimientos en esa ubicación.
     */
    fun alTocarInterruptor(ubicacion: Ubicacion) {
        if (_estado.value.cambiandoId != null) return
        if (ubicacion.activa) {
            _estado.update { it.copy(porDesactivar = ubicacion) }
        } else {
            cambiar(ubicacion, activa = true)
        }
    }

    fun confirmarDesactivar() {
        val ubicacion = _estado.value.porDesactivar ?: return
        _estado.update { it.copy(porDesactivar = null) }
        cambiar(ubicacion, activa = false)
    }

    fun cancelarDesactivar() {
        _estado.update { it.copy(porDesactivar = null) }
    }

    /** Endpoint 3. La lista se actualiza con la respuesta, que es lo que quedó guardado. */
    private fun cambiar(ubicacion: Ubicacion, activa: Boolean) {
        _estado.update { it.copy(cambiandoId = ubicacion.id, errorCambio = false) }
        viewModelScope.launch {
            try {
                val actualizada = repositorio.cambiarEstadoUbicacion(ubicacion.id, activa)
                // Si el Gerente está trabajando en esa ubicación, la sesión debe ver el estado nuevo.
                if (sesionRepository.ubicacionActiva.value?.id == actualizada.id) {
                    sesionRepository.seleccionarUbicacion(actualizada)
                }
                _estado.update { actual ->
                    actual.copy(
                        cambiandoId = null,
                        grupos = actual.grupos.map { grupo ->
                            grupo.copy(ubicaciones = grupo.ubicaciones.map { if (it.id == actualizada.id) actualizada else it })
                        },
                        cambio = CambioRealizado(actualizada.id, actualizada.activa)
                    )
                }
            } catch (e: SinConexionException) {
                _estado.update { it.copy(cambiandoId = null, irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                _estado.update { it.copy(cambiandoId = null, errorCambio = true) }
            }
        }
    }

    fun avisoMostrado() {
        _estado.update { it.copy(cambio = null, errorCambio = false) }
    }

    fun navegacionSinConexionRealizada() {
        _estado.update { it.copy(irASinConexion = false) }
    }

    /** Recarga al volver a la pantalla (por ejemplo, desde P16). La primera vez ya cargó init. */
    fun alMostrarse() {
        if (yaSeMostro) cargar() else yaSeMostro = true
    }
}
