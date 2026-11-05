package com.byto.inventario.ui.screens.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byto.inventario.data.model.FiltroMovimientos
import com.byto.inventario.data.model.Movimiento
import com.byto.inventario.data.model.Perfil
import com.byto.inventario.data.model.TipoMovimiento
import com.byto.inventario.data.model.TipoUbicacion
import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.ReglaNegocioException
import com.byto.inventario.data.repository.SesionRepository
import com.byto.inventario.data.repository.SinConexionException
import com.byto.inventario.domain.Fechas
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Un movimiento con el nombre de su producto, listo para dibujar. */
data class FilaMovimiento(val movimiento: Movimiento, val nombreProducto: String)

/**
 * Estado de P13. Los filtros en null significan "todos".
 * @param ubicaciones las que el usuario puede auditar (opciones del filtro).
 * @param operadores IDs de operadores para el filtro; vacío si el usuario es Operador
 * (en ese caso [operadorId] queda fijo en su propio ID: solo ve sus movimientos).
 * @param desde,hasta días en hora de Chile, ambos inclusivos.
 * @param errorRango "Desde" es posterior a "Hasta": no se busca hasta corregirlo.
 */
data class EstadoHistorial(
    val cargando: Boolean = true,
    val buscando: Boolean = false,
    val usuarioId: String = "",
    val ubicaciones: List<Ubicacion> = emptyList(),
    val operadores: List<String> = emptyList(),
    val operadorFijo: Boolean = false,
    val ubicacionId: String? = null,
    val operadorId: String? = null,
    val tipo: TipoMovimiento? = null,
    val desde: LocalDate? = null,
    val hasta: LocalDate? = null,
    val errorRango: Boolean = false,
    val movimientos: List<FilaMovimiento> = emptyList(),
    val error: Boolean = false,
    val irASinConexion: Boolean = false,
    val sinSesion: Boolean = false
) {
    /** Hay algún filtro elegido por la persona (el operador fijo no cuenta). */
    val hayFiltros: Boolean
        get() = ubicacionId != null || tipo != null || desde != null || hasta != null ||
            (!operadorFijo && operadorId != null)
}

/**
 * Auditoría de movimientos (Caso §4.1: filtrada por tienda, fechas, usuario y tipo).
 * Quién ve qué:
 * - Gerente: todos los movimientos.
 * - Encargado: los de sus ubicaciones (su bodega y las tiendas que consulta).
 * - Operador: solo los suyos (endpoint 8: "Operador (propios)").
 */
class HistorialViewModel(
    private val repositorio: InventarioRepository,
    private val sesionRepository: SesionRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoHistorial())
    val estado: StateFlow<EstadoHistorial> = _estado.asStateFlow()

    /** Código interno → nombre. El catálogo es el mismo en todas las ubicaciones. */
    private var nombres: Map<String, String> = emptyMap()

    private var busqueda: Job? = null
    private var yaSeMostro = false

    init {
        cargar()
    }

    /** Carga las opciones de los filtros y el catálogo, y después busca. */
    private fun cargar() {
        val usuario = sesionRepository.usuarioActivo.value
        if (usuario == null) {
            _estado.update { it.copy(cargando = false, sinSesion = true) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = false, usuarioId = usuario.id) }
            try {
                // Casa Matriz no tiene stock, así que tampoco movimientos.
                val ubicaciones = repositorio.obtenerUbicaciones(usuario.id)
                    .filter { it.tipo != TipoUbicacion.ADMINISTRATIVA }
                val esOperador = usuario.perfil == Perfil.OPERADOR
                val operadores = if (esOperador) {
                    emptyList()
                } else {
                    repositorio.obtenerUsuariosDemo().filter { it.perfil == Perfil.OPERADOR }.map { it.id }
                }
                // Endpoint 4 de cualquier ubicación trae el catálogo completo (con sus nombres).
                nombres = ubicaciones.firstOrNull()
                    ?.let { repositorio.obtenerExistencias(it.id) }
                    .orEmpty()
                    .associate { it.producto.codigoInterno to it.producto.nombre }
                _estado.update {
                    it.copy(
                        cargando = false,
                        ubicaciones = ubicaciones,
                        operadores = operadores,
                        operadorFijo = esOperador,
                        operadorId = if (esOperador) usuario.id else it.operadorId
                    )
                }
                buscar()
            } catch (e: SinConexionException) {
                _estado.update { it.copy(cargando = false, irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                _estado.update { it.copy(cargando = false, error = true) }
            }
        }
    }

    fun elegirUbicacion(ubicacionId: String?) = cambiarFiltros { it.copy(ubicacionId = ubicacionId) }

    fun elegirOperador(operadorId: String?) {
        if (_estado.value.operadorFijo) return
        cambiarFiltros { it.copy(operadorId = operadorId) }
    }

    fun elegirTipo(tipo: TipoMovimiento?) = cambiarFiltros { it.copy(tipo = tipo) }

    fun elegirDesde(dia: LocalDate?) = cambiarFiltros { it.copy(desde = dia) }

    fun elegirHasta(dia: LocalDate?) = cambiarFiltros { it.copy(hasta = dia) }

    fun limpiarFiltros() = cambiarFiltros {
        it.copy(
            ubicacionId = null,
            operadorId = if (it.operadorFijo) it.operadorId else null,
            tipo = null,
            desde = null,
            hasta = null
        )
    }

    private fun cambiarFiltros(cambio: (EstadoHistorial) -> EstadoHistorial) {
        _estado.update(cambio)
        buscar()
    }

    /** Endpoint 8 con los filtros actuales. Si el rango de fechas es inválido, no busca. */
    private fun buscar() {
        val filtros = _estado.value
        busqueda?.cancel()
        // La regla vive en el dominio; aquí solo se aplica y se muestra.
        if (!Fechas.rangoValido(filtros.desde, filtros.hasta)) {
            _estado.update { it.copy(errorRango = true, buscando = false, movimientos = emptyList()) }
            return
        }
        _estado.update { it.copy(errorRango = false) }
        busqueda = viewModelScope.launch {
            _estado.update { it.copy(buscando = true, error = false) }
            try {
                val permitidas = filtros.ubicaciones.map { it.id }.toSet()
                val movimientos = repositorio.obtenerMovimientos(
                    FiltroMovimientos(
                        ubicacionId = filtros.ubicacionId,
                        usuarioId = filtros.operadorId,
                        tipo = filtros.tipo,
                        desde = filtros.desde?.let { Fechas.inicioDelDia(it) },
                        hasta = filtros.hasta?.let { Fechas.finDelDia(it) }
                    )
                )
                    // El filtro del endpoint admite una sola ubicación; para "Todas" se deja
                    // fuera lo que no es del usuario (caso del Encargado).
                    .filter { it.ubicacionId in permitidas }
                    .map { FilaMovimiento(it, nombres[it.codigoInterno] ?: it.codigoInterno) }
                _estado.update { it.copy(buscando = false, movimientos = movimientos) }
            } catch (e: SinConexionException) {
                _estado.update { it.copy(buscando = false, irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                _estado.update { it.copy(buscando = false, error = true) }
            }
        }
    }

    fun navegacionSinConexionRealizada() {
        _estado.update { it.copy(irASinConexion = false) }
    }

    /**
     * La pantalla lo llama cada vez que vuelve a verse. La primera vez no hace nada (init ya
     * cargó); las siguientes repite la carga, por ejemplo al volver de P16 o tras registrar un movimiento.
     */
    fun alMostrarse() {
        if (yaSeMostro) cargar() else yaSeMostro = true
    }
}
