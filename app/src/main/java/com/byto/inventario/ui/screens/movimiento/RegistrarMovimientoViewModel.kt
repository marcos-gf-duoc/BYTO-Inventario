package com.byto.inventario.ui.screens.movimiento

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byto.inventario.data.model.Alerta
import com.byto.inventario.data.model.EstadoAlerta
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.ExistenciaDetalle
import com.byto.inventario.data.model.NuevoMovimiento
import com.byto.inventario.data.model.TipoAlerta
import com.byto.inventario.data.model.TipoMovimiento
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.ReglaNegocioException
import com.byto.inventario.data.repository.SesionRepository
import com.byto.inventario.data.repository.SinConexionException
import com.byto.inventario.domain.ErrorValidacion
import com.byto.inventario.domain.Permisos
import com.byto.inventario.domain.ReglasInventario
import com.byto.inventario.ui.navigation.Pantalla
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Motivos de ajuste. [texto] es lo que se guarda en el movimiento (dato, no texto de pantalla);
 * los chips muestran sus propios textos desde strings.xml.
 */
enum class MotivoAjuste(val texto: String) {
    PRODUCTO_DANADO("Producto dañado"),
    MERMA("Merma"),
    ERROR_REGISTRO("Error de registro"),
    OTRO("Otro")
}

/**
 * Lo que P09 le pasa a P10 al guardar: solo IDs, porque la navegación transporta textos.
 * P10 vuelve a pedir los detalles al repositorio con ellos.
 *
 * @param actualizadas alertas de stock bajo que ya estaban abiertas y siguen abiertas.
 * @param cerradas alertas que el movimiento cerró (stock bajo recuperado o reposición cumplida).
 */
data class DatosConfirmacion(
    val movimientoId: String,
    val generadas: List<String>,
    val actualizadas: List<String>,
    val cerradas: List<String>
)

/**
 * Estado de P09. Los campos de arriba son lo que escribe el usuario; los de abajo
 * (errores, stock resultante) se recalculan con ReglasInventario en cada cambio.
 *
 * @param cantidadTexto lo escrito en el campo; puede estar vacío mientras se edita.
 * @param sumar solo AJUSTE: true = suma unidades, false = resta.
 * @param sinPermiso el usuario no puede registrar en esta ubicación (ver [Permisos]).
 * @param enviando se está guardando: el botón muestra carga y no se puede tocar otra vez.
 * @param errorEnvio el servidor rechazó el movimiento (por ejemplo, el stock cambió mientras tanto).
 * @param registrado guardado con éxito: la pantalla debe abrir P10 con estos datos.
 * @param cantidadVacia el campo de cantidad está vacío.
 * @param errorCantidad error de la cantidad (inválida, supera el stock o deja el stock negativo).
 * @param errorMotivo solo AJUSTE: falta el motivo.
 * @param stockResultante stock que quedaría; null si la cantidad aún no es válida.
 */
data class EstadoRegistro(
    val cargando: Boolean = true,
    val detalle: ExistenciaDetalle? = null,
    val tipo: TipoMovimiento = TipoMovimiento.ENTRADA,
    val cantidadTexto: String = "1",
    val sumar: Boolean = true,
    val motivo: MotivoAjuste? = null,
    val motivoOtro: String = "",
    val observacion: String = "",
    val sinPermiso: Boolean = false,
    val enviando: Boolean = false,
    val errorEnvio: Boolean = false,
    val registrado: DatosConfirmacion? = null,
    val error: Boolean = false,
    val irASinConexion: Boolean = false,
    val sinSesion: Boolean = false,
    // ----- Calculado -----
    val cantidadVacia: Boolean = false,
    val errorCantidad: ErrorValidacion? = null,
    val errorMotivo: Boolean = false,
    val errorObservacion: Boolean = false,
    val stockResultante: Int? = null,
    val estadoResultante: EstadoStock? = null,
    val puedeConfirmar: Boolean = false
) {
    /** Cantidad escrita como número (null si el campo está vacío). */
    val cantidad: Int? get() = cantidadTexto.toIntOrNull()

    /** Solo CONTEO: contado − sistema (por ejemplo −4). */
    val diferenciaConteo: Int?
        get() = if (tipo == TipoMovimiento.CONTEO && detalle != null) {
            cantidad?.let { it - detalle.existencia.stock }
        } else {
            null
        }
}

class RegistrarMovimientoViewModel(
    private val repositorio: InventarioRepository,
    private val sesionRepository: SesionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val codigoInterno: String = savedStateHandle.get<String>(Pantalla.ARG_CODIGO_INTERNO).orEmpty()

    private val _estado = MutableStateFlow(EstadoRegistro())
    val estado: StateFlow<EstadoRegistro> = _estado.asStateFlow()

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
        // Defensa extra: aunque los botones ya se ocultan, P09 no se abre sin permiso.
        if (!Permisos.puedeRegistrarMovimientos(usuario, ubicacion)) {
            _estado.update { it.copy(cargando = false, sinPermiso = true) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = false) }
            try {
                val detalle = repositorio.obtenerExistencia(ubicacion.id, codigoInterno)
                actualizar { it.copy(cargando = false, detalle = detalle) }
            } catch (e: SinConexionException) {
                reintentarAlVolver = true
                _estado.update { it.copy(irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                _estado.update { it.copy(cargando = false, error = true) }
            }
        }
    }

    // ---------- Acciones del formulario ----------

    /**
     * Al cambiar de tipo se reinicia la cantidad: el conteo parte en el stock actual
     * (lo más probable es que coincida) y los demás tipos en 1.
     */
    fun cambiarTipo(tipo: TipoMovimiento) {
        actualizar { actual ->
            val inicial = if (tipo == TipoMovimiento.CONTEO) actual.detalle?.existencia?.stock ?: 0 else 1
            actual.copy(tipo = tipo, cantidadTexto = inicial.toString())
        }
    }

    /** Solo dígitos y hasta 5 (99.999 unidades): evita números que no caben en un Int. */
    fun cambiarCantidad(texto: String) {
        actualizar { it.copy(cantidadTexto = texto.filter { c -> c.isDigit() }.take(5)) }
    }

    /** Botones − y +: suman o restan 1 sin bajar de 0. */
    fun sumarUno() = cambiarEnUno(+1)
    fun restarUno() = cambiarEnUno(-1)

    private fun cambiarEnUno(delta: Int) {
        actualizar { actual ->
            val nueva = ((actual.cantidad ?: 0) + delta).coerceIn(0, 99_999)
            actual.copy(cantidadTexto = nueva.toString())
        }
    }

    fun cambiarSentidoAjuste(sumar: Boolean) {
        actualizar { it.copy(sumar = sumar) }
    }

    fun elegirMotivo(motivo: MotivoAjuste) {
        actualizar { it.copy(motivo = motivo) }
    }

    fun cambiarMotivoOtro(texto: String) {
        actualizar { it.copy(motivoOtro = texto) }
    }

    fun cambiarObservacion(texto: String) {
        actualizar { it.copy(observacion = texto.take(ReglasInventario.MAX_CARACTERES_OBSERVACION)) }
    }

    // ---------- Cálculos (todos con ReglasInventario) ----------

    /** Aplica un cambio y recalcula errores y stock resultante en el mismo paso. */
    private fun actualizar(cambio: (EstadoRegistro) -> EstadoRegistro) {
        _estado.update { recalcular(cambio(it)) }
    }

    private fun recalcular(e: EstadoRegistro): EstadoRegistro {
        val detalle = e.detalle ?: return e
        val stock = detalle.existencia.stock
        val cantidad = e.cantidad ?: return e.copy(
            cantidadVacia = true, errorCantidad = null, stockResultante = null,
            estadoResultante = null, puedeConfirmar = false
        )
        val cantidadConSigno = cantidadConSigno(e.tipo, cantidad, e.sumar)
        val motivo = motivoFinal(e)

        // Cada campo se valida con la regla del dominio, dejando "neutros" los demás:
        // así el error aparece justo bajo el campo que hay que corregir.
        val errorCantidad = ReglasInventario.validarMovimiento(
            e.tipo, stock, cantidadConSigno, motivo = MOTIVO_NEUTRO, observacion = null
        )
        val errorMotivo = e.tipo == TipoMovimiento.AJUSTE && ReglasInventario.validarMovimiento(
            TipoMovimiento.AJUSTE, stock, cantidad = 1, motivo = motivo, observacion = null
        ) == ErrorValidacion.MOTIVO_OBLIGATORIO
        val errorObservacion = ReglasInventario.validarMovimiento(
            TipoMovimiento.ENTRADA, stock, cantidad = 1, motivo = null, observacion = e.observacion
        ) == ErrorValidacion.OBSERVACION_MUY_LARGA

        // La validación completa es la que decide si se puede confirmar.
        val valido = ReglasInventario.validarMovimiento(
            e.tipo, stock, cantidadConSigno, motivo, e.observacion
        ) == null
        val resultante = if (errorCantidad == null) {
            ReglasInventario.calcularStockResultante(e.tipo, stock, cantidadConSigno)
        } else {
            null
        }

        return e.copy(
            cantidadVacia = false,
            errorCantidad = errorCantidad,
            errorMotivo = errorMotivo,
            errorObservacion = errorObservacion,
            stockResultante = resultante,
            estadoResultante = resultante?.let { ReglasInventario.estadoDe(it) },
            puedeConfirmar = valido
        )
    }

    /** En AJUSTE la cantidad lleva signo: "Restar 2" se guarda como −2. */
    private fun cantidadConSigno(tipo: TipoMovimiento, cantidad: Int, sumar: Boolean): Int =
        if (tipo == TipoMovimiento.AJUSTE && !sumar) -cantidad else cantidad

    /** Texto del motivo que se guardará; null si falta (o si "Otro" no tiene descripción). */
    private fun motivoFinal(e: EstadoRegistro): String? = when (e.motivo) {
        null -> null
        MotivoAjuste.OTRO -> e.motivoOtro.trim().takeIf { it.isNotEmpty() }?.let { "${MotivoAjuste.OTRO.texto}: $it" }
        else -> e.motivo.texto
    }

    // ---------- Guardar ----------

    /**
     * Registra el movimiento. Antes y después lee las alertas abiertas del producto para saber
     * cuáles se actualizaron o cerraron (la API solo informa las nuevas).
     */
    fun confirmar() {
        val actual = _estado.value
        // Evita el doble envío: si ya se está guardando o hay errores, no hace nada.
        if (actual.enviando || !actual.puedeConfirmar) return
        val usuario = sesionRepository.usuarioActivo.value
        val ubicacion = sesionRepository.ubicacionActiva.value
        val cantidad = actual.cantidad
        if (usuario == null || ubicacion == null || cantidad == null) return

        val nuevo = NuevoMovimiento(
            tipo = actual.tipo,
            ubicacionId = ubicacion.id,
            codigoInterno = codigoInterno,
            cantidad = cantidadConSigno(actual.tipo, cantidad, actual.sumar),
            operadorId = usuario.id,
            motivo = if (actual.tipo == TipoMovimiento.AJUSTE) motivoFinal(actual) else null,
            observacion = actual.observacion.trim().ifEmpty { null }
        )

        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, errorEnvio = false) }
            try {
                val abiertasAntes = alertasDelProducto(ubicacion.id).filter { it.estado == EstadoAlerta.ABIERTA }
                val resultado = repositorio.registrarMovimiento(nuevo)
                // Si aquí se corta la conexión, el movimiento ya quedó guardado: se sigue a P10
                // sin el detalle de cambios, para no ofrecer reenviar el formulario.
                val despues = try {
                    alertasDelProducto(ubicacion.id)
                } catch (e: SinConexionException) {
                    emptyList()
                }
                val estadoDespues = despues.associate { it.id to it.estado }

                val datos = DatosConfirmacion(
                    movimientoId = resultado.movimiento.id,
                    generadas = resultado.alertasGeneradas.map { it.id },
                    actualizadas = abiertasAntes
                        .filter { it.tipo == TipoAlerta.STOCK_BAJO && estadoDespues[it.id] == EstadoAlerta.ABIERTA }
                        .map { it.id },
                    cerradas = abiertasAntes
                        .filter { estadoDespues[it.id] == EstadoAlerta.CERRADA }
                        .map { it.id }
                )
                _estado.update { it.copy(enviando = false, registrado = datos) }
            } catch (e: SinConexionException) {
                // No se guardó nada: al volver de P16 el formulario sigue igual y se puede reintentar.
                _estado.update { it.copy(enviando = false, irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                _estado.update { it.copy(enviando = false, errorEnvio = true) }
            }
        }
    }

    private suspend fun alertasDelProducto(ubicacionId: String): List<Alerta> =
        repositorio.obtenerAlertas(ubicacionId = ubicacionId).filter { it.codigoInterno == codigoInterno }

    /** La pantalla avisa que ya abrió P10. */
    fun navegacionConfirmacionRealizada() {
        _estado.update { it.copy(registrado = null) }
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
        /** Motivo de relleno para validar solo la cantidad de un ajuste. */
        const val MOTIVO_NEUTRO = "-"
    }
}
