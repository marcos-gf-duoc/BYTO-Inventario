package com.byto.inventario.ui.screens.exportar

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byto.inventario.data.model.FiltroMovimientos
import com.byto.inventario.data.model.TipoUbicacion
import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.ReglaNegocioException
import com.byto.inventario.data.repository.SesionRepository
import com.byto.inventario.data.repository.SinConexionException
import com.byto.inventario.dispositivo.EscritorArchivos
import com.byto.inventario.domain.ErroresExportacion
import com.byto.inventario.domain.Fechas
import com.byto.inventario.domain.ReporteCsv
import com.byto.inventario.domain.TipoReporte
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Instant

/** Qué ubicaciones abarca el reporte. */
sealed interface AlcanceReporte {
    data object Todas : AlcanceReporte
    data class Una(val ubicacionId: String) : AlcanceReporte
}

/** Problemas al guardar, que se muestran bajo el botón con ícono y texto. */
enum class ErrorGuardado { SIN_DESTINO, NO_SE_PUDO_ESCRIBIR }

/**
 * Estado de P15.
 * @param pedirDestino nombre sugerido: la pantalla debe abrir "Guardar como" con él (evento).
 * @param guardado nombre del archivo recién guardado, para el aviso de confirmación (evento).
 */
data class EstadoExportar(
    val cargando: Boolean = true,
    val usuarioId: String = "",
    val ubicaciones: List<Ubicacion> = emptyList(),
    val tipo: TipoReporte? = null,
    val alcance: AlcanceReporte? = null,
    val errores: ErroresExportacion = ErroresExportacion(),
    val errorGuardado: ErrorGuardado? = null,
    val exportando: Boolean = false,
    val pedirDestino: String? = null,
    val guardado: String? = null,
    val ultimoDestino: Uri? = null,
    val error: Boolean = false,
    val irASinConexion: Boolean = false,
    val sinSesion: Boolean = false
) {
    val puedeExportar: Boolean get() = !cargando && !exportando && !errores.hayErrores
}

/**
 * Flujo de exportación (Caso §3.4, simulada localmente):
 * 1. La persona elige tipo y ubicación; "Exportar" valida ([solicitarExportar]).
 * 2. La pantalla abre "Guardar como" con el nombre sugerido y devuelve el destino elegido.
 * 3. [guardarEn] reúne los datos, arma el CSV (dominio) y lo escribe (dispositivo).
 */
class ExportarReporteViewModel(
    private val repositorio: InventarioRepository,
    private val sesionRepository: SesionRepository,
    private val escritor: EscritorArchivos
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoExportar())
    val estado: StateFlow<EstadoExportar> = _estado.asStateFlow()

    private var intentoExportar = false

    /** Nombre sugerido del archivo en curso, para el aviso final. */
    private var nombreEnCurso = ""

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
            _estado.update { it.copy(cargando = true, error = false, usuarioId = usuario.id) }
            try {
                val ubicaciones = repositorio.obtenerUbicaciones(usuario.id)
                    .filter { it.tipo != TipoUbicacion.ADMINISTRATIVA }
                _estado.update { it.copy(cargando = false, ubicaciones = ubicaciones) }
            } catch (e: SinConexionException) {
                _estado.update { it.copy(cargando = false, irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                _estado.update { it.copy(cargando = false, error = true) }
            }
        }
    }

    fun elegirTipo(tipo: TipoReporte) {
        _estado.update { it.copy(tipo = tipo, errorGuardado = null) }
        validar()
    }

    fun elegirAlcance(alcance: AlcanceReporte) {
        _estado.update { it.copy(alcance = alcance, errorGuardado = null) }
        validar()
    }

    /** Paso 1: si el formulario está completo, pide a la pantalla abrir "Guardar como". */
    fun solicitarExportar() {
        intentoExportar = true
        validar()
        val actual = _estado.value
        val tipo = actual.tipo ?: return
        val alcance = actual.alcance ?: return
        if (actual.errores.hayErrores || actual.exportando) return
        val ubicacionId = (alcance as? AlcanceReporte.Una)?.ubicacionId
        // "Hoy" según la hora de Chile, igual que las fechas dentro del reporte.
        val hoy = Instant.now().atZone(Fechas.ZONA_CHILE).toLocalDate()
        nombreEnCurso = ReporteCsv.nombreArchivo(tipo, ubicacionId, hoy)
        _estado.update { it.copy(pedirDestino = nombreEnCurso, errorGuardado = null) }
    }

    fun destinoSolicitado() {
        _estado.update { it.copy(pedirDestino = null) }
    }

    /** Paso 3. [destino] es null si la persona cerró "Guardar como" sin elegir. */
    fun guardarEn(destino: Uri?) {
        if (destino == null) {
            _estado.update { it.copy(errorGuardado = ErrorGuardado.SIN_DESTINO) }
            return
        }
        val tipo = _estado.value.tipo ?: return
        val alcance = _estado.value.alcance ?: return
        _estado.update { it.copy(exportando = true, errorGuardado = null) }
        viewModelScope.launch {
            try {
                val texto = armarReporte(tipo, alcance)
                escritor.escribir(destino, texto)
                _estado.update { it.copy(exportando = false, guardado = nombreEnCurso, ultimoDestino = destino) }
            } catch (e: SinConexionException) {
                _estado.update { it.copy(exportando = false, irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                _estado.update { it.copy(exportando = false, errorGuardado = ErrorGuardado.NO_SE_PUDO_ESCRIBIR) }
            } catch (e: IOException) {
                _estado.update { it.copy(exportando = false, errorGuardado = ErrorGuardado.NO_SE_PUDO_ESCRIBIR) }
            } catch (e: SecurityException) {
                // El sistema negó el acceso al destino elegido (por ejemplo, una carpeta protegida).
                _estado.update { it.copy(exportando = false, errorGuardado = ErrorGuardado.NO_SE_PUDO_ESCRIBIR) }
            }
        }
    }

    /** Reúne los datos con los endpoints de siempre (4, 8 y 9) y arma el texto con el dominio. */
    private suspend fun armarReporte(tipo: TipoReporte, alcance: AlcanceReporte): String = coroutineScope {
        val ubicaciones = when (alcance) {
            AlcanceReporte.Todas -> _estado.value.ubicaciones
            is AlcanceReporte.Una -> _estado.value.ubicaciones.filter { it.id == alcance.ubicacionId }
        }
        val ids = ubicaciones.map { it.id }.toSet()
        val unaId = (alcance as? AlcanceReporte.Una)?.ubicacionId

        when (tipo) {
            TipoReporte.STOCK -> {
                // Una consulta por ubicación, todas a la vez; el resultado mantiene el orden de las ubicaciones.
                val filas = ubicaciones
                    .map { u -> async { repositorio.obtenerExistencias(u.id).map { u to it } } }
                    .awaitAll()
                    .flatten()
                ReporteCsv.stock(filas)
            }
            TipoReporte.MOVIMIENTOS -> {
                val pedidoNombres = async { nombresDeProductos(ubicaciones) }
                val movimientos = repositorio.obtenerMovimientos(FiltroMovimientos(ubicacionId = unaId))
                    .filter { it.ubicacionId in ids }
                ReporteCsv.movimientos(movimientos, pedidoNombres.await())
            }
            TipoReporte.ALERTAS -> {
                val pedidoNombres = async { nombresDeProductos(ubicaciones) }
                // Todas las alertas, abiertas y cerradas: el reporte sirve para auditar.
                val alertas = repositorio.obtenerAlertas(ubicacionId = unaId).filter { it.ubicacionId in ids }
                ReporteCsv.alertas(alertas, pedidoNombres.await())
            }
        }
    }

    /** El catálogo es el mismo en todas las ubicaciones: basta con el inventario de una. */
    private suspend fun nombresDeProductos(ubicaciones: List<Ubicacion>): Map<String, String> =
        ubicaciones.firstOrNull()
            ?.let { repositorio.obtenerExistencias(it.id) }
            .orEmpty()
            .associate { it.producto.codigoInterno to it.producto.nombre }

    /** Como en P12, los errores de "obligatorio" aparecen después del primer intento. */
    private fun validar() {
        _estado.update { actual ->
            val todos = ReporteCsv.validar(actual.tipo, actual.alcance != null)
            actual.copy(errores = if (intentoExportar) todos else ErroresExportacion())
        }
    }

    fun avisoMostrado() {
        _estado.update { it.copy(guardado = null) }
    }

    fun navegacionSinConexionRealizada() {
        _estado.update { it.copy(irASinConexion = false) }
    }

    /** Al volver de P16 se reintenta la carga si había fallado. */
    fun alMostrarse() {
        if (_estado.value.ubicaciones.isEmpty() && !_estado.value.cargando) cargar()
    }
}
