package com.byto.inventario.data.fake

import com.byto.inventario.data.model.Alerta
import com.byto.inventario.data.model.EstadoAlerta
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.Existencia
import com.byto.inventario.data.model.ExistenciaDetalle
import com.byto.inventario.data.model.FiltroMovimientos
import com.byto.inventario.data.model.Movimiento
import com.byto.inventario.data.model.NuevoMovimiento
import com.byto.inventario.data.model.Perfil
import com.byto.inventario.data.model.Producto
import com.byto.inventario.data.model.ResultadoMovimiento
import com.byto.inventario.data.model.ResumenUbicacion
import com.byto.inventario.data.model.Tablero
import com.byto.inventario.data.model.TipoAlerta
import com.byto.inventario.data.model.TipoMovimiento
import com.byto.inventario.data.model.TipoUbicacion
import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.data.model.Usuario
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.ReglaNegocioException
import com.byto.inventario.data.repository.SinConexionException
import com.byto.inventario.domain.ReglasInventario
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.Locale

/**
 * Implementación en memoria que imita al futuro backend.
 * Parte de una copia de [DatosSinteticos]; los cambios se pierden al cerrar la app.
 *
 * @param retardoMs espera simulada de cada llamada, como si fuera por la red.
 */
class FakeInventarioRepository(
    private val retardoMs: Long = 300
) : InventarioRepository {

    /**
     * Interruptor oculto para demostrar el estado "sin conexión".
     * @Volatile: el valor se cambia desde la UI y se lee desde corrutinas;
     * así todas ven siempre el valor más reciente.
     */
    @Volatile
    var sinConexion: Boolean = false

    // Copias modificables de los datos iniciales.
    private val usuarios = DatosSinteticos.usuarios
    private val productos = DatosSinteticos.productos
    private val ubicaciones = DatosSinteticos.ubicaciones.toMutableList()
    private val existencias = DatosSinteticos.existencias.toMutableList()
    private val movimientos = DatosSinteticos.movimientos.toMutableList()
    private val alertas = DatosSinteticos.alertas.toMutableList()

    /**
     * Candado: si dos corrutinas llaman a la vez (por ejemplo, dos pantallas),
     * solo una a la vez lee o modifica las listas. Evita datos a medio actualizar.
     */
    private val candado = Mutex()

    /** Toda llamada pasa por aquí: espera, revisa la conexión y ejecuta con el candado. */
    private suspend fun <T> llamada(bloque: () -> T): T {
        delay(retardoMs)
        if (sinConexion) throw SinConexionException()
        return candado.withLock { bloque() }
    }

    // ---------- 1. Usuarios ----------

    override suspend fun obtenerUsuariosDemo(): List<Usuario> = llamada { usuarios.toList() }

    // ---------- 2 y 3. Ubicaciones ----------

    override suspend fun obtenerUbicaciones(usuarioId: String): List<Ubicacion> = llamada {
        val usuario = usuarios.find { it.id == usuarioId }
            ?: throw noEncontrado("No existe el usuario $usuarioId.")
        if (usuario.perfil == Perfil.GERENTE) {
            ubicaciones.toList()
        } else {
            // Las de trabajo más las de solo consulta (caso del Encargado).
            val permitidas = usuario.ubicacionesIds + usuario.ubicacionesConsultaIds
            ubicaciones.filter { it.id in permitidas }
        }
    }

    override suspend fun cambiarEstadoUbicacion(ubicacionId: String, activa: Boolean): Ubicacion =
        llamada {
            val indice = ubicaciones.indexOfFirst { it.id == ubicacionId }
            if (indice == -1) throw noEncontrado("No existe la ubicación $ubicacionId.")
            // copy() crea una nueva Ubicacion igual salvo por "activa".
            val actualizada = ubicaciones[indice].copy(activa = activa)
            ubicaciones[indice] = actualizada
            actualizada
        }

    // ---------- 4, 5 y 6. Existencias y productos ----------

    override suspend fun obtenerExistencias(
        ubicacionId: String,
        busqueda: String?,
        estado: EstadoStock?
    ): List<ExistenciaDetalle> = llamada {
        buscarUbicacion(ubicacionId)
        val texto = busqueda?.trim().orEmpty()
        existencias
            .filter { it.ubicacionId == ubicacionId }
            .map { detalleDe(it) }
            .filter { estado == null || it.estado == estado }
            .filter { texto.isEmpty() || coincide(it.producto, texto) }
    }

    override suspend fun obtenerExistencia(
        ubicacionId: String,
        codigoInterno: String
    ): ExistenciaDetalle = llamada {
        detalleDe(buscarExistencia(ubicacionId, codigoInterno))
    }

    override suspend fun buscarProductoPorEan(ean: String): Producto? = llamada {
        productos.find { it.ean13 == ean.trim() }
    }

    // ---------- 7. Registrar movimiento ----------

    override suspend fun registrarMovimiento(nuevo: NuevoMovimiento): ResultadoMovimiento =
        llamada {
            val ubicacion = buscarUbicacion(nuevo.ubicacionId)
            if (ubicacion.tipo == TipoUbicacion.ADMINISTRATIVA) {
                throw ReglaNegocioException(
                    "UBICACION_SIN_STOCK", "${ubicacion.nombre} no maneja inventario."
                )
            }
            if (!ubicacion.activa) {
                throw ReglaNegocioException(
                    "UBICACION_INACTIVA", "${ubicacion.nombre} está deshabilitada."
                )
            }
            val existencia = buscarExistencia(nuevo.ubicacionId, nuevo.codigoInterno)
            val stockAntes = existencia.stock

            // Las reglas viven en el dominio; aquí solo se aplican.
            val error = ReglasInventario.validarMovimiento(
                nuevo.tipo, stockAntes, nuevo.cantidad, nuevo.motivo, nuevo.observacion
            )
            if (error != null) {
                throw ReglaNegocioException(
                    error.name,
                    "Movimiento inválido: ${error.name} (stock actual $stockAntes, cantidad ${nuevo.cantidad})."
                )
            }

            val stockDespues = ReglasInventario.calcularStockResultante(
                nuevo.tipo, stockAntes, nuevo.cantidad
            )
            reemplazarExistencia(existencia.copy(stock = stockDespues))

            // La hora la asigna el "servidor", no el teléfono.
            val ahora = Instant.now()
            val movimiento = Movimiento(
                id = siguienteId("MOV", movimientos.size),
                tipo = nuevo.tipo,
                ubicacionId = nuevo.ubicacionId,
                codigoInterno = nuevo.codigoInterno,
                cantidad = nuevo.cantidad,
                stockAntes = stockAntes,
                stockDespues = stockDespues,
                fechaUtc = ahora,
                operadorId = nuevo.operadorId,
                motivo = nuevo.motivo?.trim()?.ifEmpty { null },
                observacion = nuevo.observacion?.trim()?.ifEmpty { null }
            )
            movimientos.add(movimiento)

            // Orden: primero el descalce y luego el stock bajo, como en el ejemplo de la API.
            val generadas = mutableListOf<Alerta>()
            if (nuevo.tipo == TipoMovimiento.CONTEO && stockDespues != stockAntes) {
                generadas += abrirDescalce(movimiento)
            }
            actualizarAlertaStockBajo(movimiento)?.let { generadas += it }
            if (nuevo.tipo == TipoMovimiento.ENTRADA) {
                cerrarReposicion(nuevo.ubicacionId, nuevo.codigoInterno)
            }

            ResultadoMovimiento(movimiento, generadas)
        }

    // ---------- 8. Historial ----------

    override suspend fun obtenerMovimientos(filtro: FiltroMovimientos): List<Movimiento> =
        llamada {
            movimientos
                .filter { filtro.ubicacionId == null || it.ubicacionId == filtro.ubicacionId }
                .filter { filtro.usuarioId == null || it.operadorId == filtro.usuarioId }
                .filter { filtro.tipo == null || it.tipo == filtro.tipo }
                .filter { filtro.desde == null || !it.fechaUtc.isBefore(filtro.desde) }
                .filter { filtro.hasta == null || !it.fechaUtc.isAfter(filtro.hasta) }
                .sortedByDescending { it.fechaUtc }
        }

    // ---------- 9, 10 y 11. Alertas ----------

    override suspend fun obtenerAlertas(
        ubicacionId: String?,
        tipo: TipoAlerta?,
        estado: EstadoAlerta?
    ): List<Alerta> = llamada {
        alertas
            .filter { ubicacionId == null || it.ubicacionId == ubicacionId }
            .filter { tipo == null || it.tipo == tipo }
            .filter { estado == null || it.estado == estado }
            .sortedByDescending { it.fechaUtc }
    }

    override suspend fun declararReposicion(
        ubicacionId: String,
        codigoInterno: String,
        declaradaPor: String
    ): Alerta = llamada {
        val ubicacion = buscarUbicacion(ubicacionId)
        // La reposición se cierra con una entrada en la tienda, así que solo aplica a tiendas.
        if (ubicacion.tipo != TipoUbicacion.TIENDA) {
            throw ReglaNegocioException(
                "UBICACION_NO_ES_TIENDA", "La reposición solo se declara para tiendas."
            )
        }
        buscarExistencia(ubicacionId, codigoInterno)
        // Una sola reposición abierta por producto y tienda; si no, la entrada cerraría solo una.
        if (alertaAbierta(TipoAlerta.REPOSICION, ubicacionId, codigoInterno) != null) {
            throw ReglaNegocioException(
                "REPOSICION_YA_ABIERTA",
                "Ya hay una reposición abierta de $codigoInterno en $ubicacionId."
            )
        }
        val alerta = Alerta(
            id = siguienteId("ALE", alertas.size),
            tipo = TipoAlerta.REPOSICION,
            ubicacionId = ubicacionId,
            codigoInterno = codigoInterno,
            estado = EstadoAlerta.ABIERTA,
            fechaUtc = Instant.now(),
            declaradaPorId = declaradaPor
        )
        alertas.add(alerta)
        alerta
    }

    override suspend fun marcarDescalceRevisado(alertaId: String): Alerta = llamada {
        val indice = alertas.indexOfFirst { it.id == alertaId }
        if (indice == -1) throw noEncontrado("No existe la alerta $alertaId.")
        val alerta = alertas[indice]
        if (alerta.tipo != TipoAlerta.DESCALCE || alerta.estado != EstadoAlerta.ABIERTA) {
            throw ReglaNegocioException(
                "ALERTA_NO_REVISABLE", "Solo se puede revisar un descalce abierto."
            )
        }
        val revisada = alerta.copy(estado = EstadoAlerta.REVISADA)
        alertas[indice] = revisada
        revisada
    }

    // ---------- 12. Tablero ----------

    override suspend fun obtenerTablero(): Tablero = llamada {
        val abiertas = alertas.filter { it.estado == EstadoAlerta.ABIERTA }
        val resumenes = ubicaciones
            .filter { it.tipo != TipoUbicacion.ADMINISTRATIVA }
            .map { ubicacion ->
                val propias = existencias.filter { it.ubicacionId == ubicacion.id }
                val estados = propias.map { ReglasInventario.estadoDe(it.stock) }
                ResumenUbicacion(
                    ubicacion = ubicacion,
                    totalUnidades = propias.sumOf { it.stock },
                    productosNormal = estados.count { it == EstadoStock.NORMAL },
                    productosBajo = estados.count { it == EstadoStock.BAJO },
                    productosCritico = estados.count { it == EstadoStock.CRITICO },
                    productosSinStock = estados.count { it == EstadoStock.SIN_STOCK },
                    alertasAbiertas = abiertas.count { it.ubicacionId == ubicacion.id }
                )
            }
        Tablero(resumenes, abiertas.size)
    }

    // ---------- Ayudantes privados (se llaman ya dentro del candado) ----------

    private fun noEncontrado(mensaje: String) = ReglaNegocioException("NO_ENCONTRADO", mensaje)

    private fun buscarUbicacion(ubicacionId: String): Ubicacion =
        ubicaciones.find { it.id == ubicacionId }
            ?: throw noEncontrado("No existe la ubicación $ubicacionId.")

    private fun buscarExistencia(ubicacionId: String, codigoInterno: String): Existencia =
        existencias.find { it.ubicacionId == ubicacionId && it.codigoInterno == codigoInterno }
            ?: throw noEncontrado("No hay $codigoInterno en $ubicacionId.")

    private fun reemplazarExistencia(nueva: Existencia) {
        val indice = existencias.indexOfFirst {
            it.ubicacionId == nueva.ubicacionId && it.codigoInterno == nueva.codigoInterno
        }
        existencias[indice] = nueva
    }

    private fun detalleDe(existencia: Existencia): ExistenciaDetalle {
        val producto = productos.first { it.codigoInterno == existencia.codigoInterno }
        return ExistenciaDetalle(existencia, producto, ReglasInventario.estadoDe(existencia.stock))
    }

    /** Búsqueda sin distinguir mayúsculas en nombre, código interno o EAN. */
    private fun coincide(producto: Producto, texto: String): Boolean =
        producto.nombre.contains(texto, ignoreCase = true) ||
            producto.codigoInterno.contains(texto, ignoreCase = true) ||
            producto.ean13.contains(texto)

    /** "MOV" y 6 existentes → "MOV-0007". Locale.ROOT asegura dígitos 0–9 en cualquier idioma. */
    private fun siguienteId(prefijo: String, cantidadActual: Int): String =
        String.format(Locale.ROOT, "%s-%04d", prefijo, cantidadActual + 1)

    private fun alertaAbierta(tipo: TipoAlerta, ubicacionId: String, codigoInterno: String): Alerta? =
        alertas.find {
            it.tipo == tipo && it.estado == EstadoAlerta.ABIERTA &&
                it.ubicacionId == ubicacionId && it.codigoInterno == codigoInterno
        }

    private fun reemplazarAlerta(nueva: Alerta) {
        alertas[alertas.indexOfFirst { it.id == nueva.id }] = nueva
    }

    /** Conteo con diferencia: abre un descalce con (contado − sistema). */
    private fun abrirDescalce(movimiento: Movimiento): Alerta {
        val alerta = Alerta(
            id = siguienteId("ALE", alertas.size),
            tipo = TipoAlerta.DESCALCE,
            ubicacionId = movimiento.ubicacionId,
            codigoInterno = movimiento.codigoInterno,
            estado = EstadoAlerta.ABIERTA,
            fechaUtc = movimiento.fechaUtc,
            diferencia = movimiento.stockDespues - movimiento.stockAntes,
            movimientoId = movimiento.id
        )
        alertas.add(alerta)
        return alerta
    }

    /**
     * Mantiene una sola alerta de stock bajo abierta por producto y ubicación:
     * - stock < 10 y no hay una abierta → la abre y la devuelve (es "generada").
     * - stock < 10 y ya hay una abierta → solo actualiza su nivel (por ejemplo, de BAJO a CRITICO).
     * - stock ≥ 10 y hay una abierta → la cierra.
     */
    private fun actualizarAlertaStockBajo(movimiento: Movimiento): Alerta? {
        val nivel = ReglasInventario.estadoDe(movimiento.stockDespues)
        val abierta = alertaAbierta(
            TipoAlerta.STOCK_BAJO, movimiento.ubicacionId, movimiento.codigoInterno
        )

        if (nivel == EstadoStock.NORMAL) {
            abierta?.let { reemplazarAlerta(it.copy(estado = EstadoAlerta.CERRADA)) }
            return null
        }
        if (abierta != null) {
            if (abierta.nivel != nivel) reemplazarAlerta(abierta.copy(nivel = nivel))
            return null
        }
        val nueva = Alerta(
            id = siguienteId("ALE", alertas.size),
            tipo = TipoAlerta.STOCK_BAJO,
            ubicacionId = movimiento.ubicacionId,
            codigoInterno = movimiento.codigoInterno,
            estado = EstadoAlerta.ABIERTA,
            fechaUtc = movimiento.fechaUtc,
            nivel = nivel,
            movimientoId = movimiento.id
        )
        alertas.add(nueva)
        return nueva
    }

    /** Una entrada en la tienda cumple la reposición pendiente de ese producto. */
    private fun cerrarReposicion(ubicacionId: String, codigoInterno: String) {
        alertaAbierta(TipoAlerta.REPOSICION, ubicacionId, codigoInterno)?.let {
            reemplazarAlerta(it.copy(estado = EstadoAlerta.CERRADA))
        }
    }
}
