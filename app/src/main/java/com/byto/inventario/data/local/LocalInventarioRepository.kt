package com.byto.inventario.data.local

import androidx.room.withTransaction
import com.byto.inventario.data.fake.DatosSinteticos
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
import com.byto.inventario.data.repository.SimuladorConexion
import com.byto.inventario.data.repository.SinConexionException
import com.byto.inventario.domain.ReglasInventario
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.Locale

/**
 * Repositorio de la E2: guarda el inventario en la base de datos local (Room), así los
 * movimientos, alertas y cambios de ubicaciones se conservan al cerrar la app.
 *
 * Aplica las mismas reglas que FakeInventarioRepository (las pruebas unitarias se hacen
 * sobre ese) y conserva su retardo simulado y el interruptor "sin conexión", para que
 * la app se comporte igual que cuando exista el servidor.
 *
 * Los usuarios simulados no se guardan en la base: son fijos y vienen de [DatosSinteticos].
 *
 * @param retardoMs espera simulada de cada llamada, como si fuera por la red.
 */
class LocalInventarioRepository(
    private val baseDatos: BytoDatabase,
    private val retardoMs: Long = 300
) : InventarioRepository, SimuladorConexion {

    /** @Volatile: se cambia desde la UI y se lee desde corrutinas; todas ven el último valor. */
    @Volatile
    override var sinConexion: Boolean = false

    private val ubicacionDao = baseDatos.ubicacionDao()
    private val productoDao = baseDatos.productoDao()
    private val existenciaDao = baseDatos.existenciaDao()
    private val movimientoDao = baseDatos.movimientoDao()
    private val alertaDao = baseDatos.alertaDao()

    private val usuarios = DatosSinteticos.usuarios

    /**
     * Candado: una llamada a la vez. Así dos pantallas no pueden, por ejemplo, numerar
     * dos movimientos con el mismo ID.
     */
    private val candado = Mutex()

    /** Evita revisar en cada llamada si la base ya tiene datos. Solo se cambia con el candado. */
    private var datosRevisados = false

    /**
     * Toda llamada pasa por aquí: espera, revisa la conexión y ejecuta con el candado.
     *
     * withTransaction agrupa las lecturas y escrituras del bloque: o se guardan todas o
     * ninguna. Si el bloque lanza un error a mitad de camino (una regla de negocio, por
     * ejemplo), Room deshace lo que alcanzó a escribir.
     */
    private suspend fun <T> llamada(bloque: suspend () -> T): T {
        delay(retardoMs)
        if (sinConexion) throw SinConexionException()
        return candado.withLock {
            cargarDatosInicialesSiFaltan()
            baseDatos.withTransaction { bloque() }
        }
    }

    /**
     * Primer arranque: la base recién creada está vacía, así que se llena con los datos
     * de docs/decisiones_E1.md. En los siguientes arranques ya tiene datos y no se toca.
     */
    private suspend fun cargarDatosInicialesSiFaltan() {
        if (datosRevisados) return
        baseDatos.withTransaction {
            if (ubicacionDao.contar() == 0) {
                ubicacionDao.insertarTodas(DatosSinteticos.ubicaciones.map { it.aEntidad() })
                productoDao.insertarTodos(DatosSinteticos.productos.map { it.aEntidad() })
                existenciaDao.insertarTodas(DatosSinteticos.existencias.map { it.aEntidad() })
                movimientoDao.insertarTodos(DatosSinteticos.movimientos.map { it.aEntidad() })
                alertaDao.insertarTodas(DatosSinteticos.alertas.map { it.aEntidad() })
            }
        }
        datosRevisados = true
    }

    // ---------- 1. Usuarios ----------

    override suspend fun obtenerUsuariosDemo(): List<Usuario> = llamada { usuarios.toList() }

    // ---------- 13. Inicio de sesión ----------

    override suspend fun autenticar(usuarioId: String, pin: String): Usuario = llamada {
        // Sin distinguir mayúsculas: "op-001" y "OP-001" son el mismo usuario.
        val usuario = usuarios.find { it.id.equals(usuarioId.trim(), ignoreCase = true) }
            ?: throw noEncontrado("No existe el usuario $usuarioId.")
        if (DatosSinteticos.pinesDemo[usuario.id] != pin) {
            // El mensaje no incluye el PIN: no debe quedar en registros ni en pantalla.
            throw ReglaNegocioException("PIN_INCORRECTO", "PIN incorrecto para ${usuario.id}.")
        }
        usuario
    }

    // ---------- 2 y 3. Ubicaciones ----------

    override suspend fun obtenerUbicaciones(usuarioId: String): List<Ubicacion> = llamada {
        val usuario = usuarios.find { it.id == usuarioId }
            ?: throw noEncontrado("No existe el usuario $usuarioId.")
        val todas = ubicacionDao.obtenerTodas().map { it.aModelo() }
        if (usuario.perfil == Perfil.GERENTE) {
            todas
        } else {
            // Las de trabajo más las de solo consulta (caso del Encargado).
            val permitidas = usuario.ubicacionesIds + usuario.ubicacionesConsultaIds
            todas.filter { it.id in permitidas }
        }
    }

    override suspend fun cambiarEstadoUbicacion(ubicacionId: String, activa: Boolean): Ubicacion =
        llamada {
            val actualizada = buscarUbicacion(ubicacionId).copy(activa = activa)
            ubicacionDao.actualizar(actualizada.aEntidad())
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
        val productos = productosPorCodigo()
        existenciaDao.obtenerDeUbicacion(ubicacionId)
            .map { detalleDe(it.aModelo(), productos) }
            .filter { estado == null || it.estado == estado }
            .filter { texto.isEmpty() || coincide(it.producto, texto) }
    }

    override suspend fun obtenerExistencia(
        ubicacionId: String,
        codigoInterno: String
    ): ExistenciaDetalle = llamada {
        detalleDe(buscarExistencia(ubicacionId, codigoInterno), productosPorCodigo())
    }

    override suspend fun buscarProductoPorEan(ean: String): Producto? = llamada {
        productoDao.obtenerPorEan(ean.trim())?.aModelo()
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
            existenciaDao.actualizar(existencia.copy(stock = stockDespues).aEntidad())

            // La hora la asigna el "servidor", no la pantalla.
            val movimiento = Movimiento(
                id = siguienteId("MOV", movimientoDao.contar()),
                tipo = nuevo.tipo,
                ubicacionId = nuevo.ubicacionId,
                codigoInterno = nuevo.codigoInterno,
                cantidad = nuevo.cantidad,
                stockAntes = stockAntes,
                stockDespues = stockDespues,
                fechaUtc = Instant.now(),
                operadorId = nuevo.operadorId,
                motivo = nuevo.motivo?.trim()?.ifEmpty { null },
                observacion = nuevo.observacion?.trim()?.ifEmpty { null }
            )
            movimientoDao.insertar(movimiento.aEntidad())

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
            movimientoDao.obtenerFiltrados(
                ubicacionId = filtro.ubicacionId,
                operadorId = filtro.usuarioId,
                tipo = filtro.tipo,
                desde = filtro.desde,
                hasta = filtro.hasta
            ).map { it.aModelo() }
        }

    // ---------- 9, 10 y 11. Alertas ----------

    override suspend fun obtenerAlertas(
        ubicacionId: String?,
        tipo: TipoAlerta?,
        estado: EstadoAlerta?
    ): List<Alerta> = llamada {
        alertaDao.obtenerFiltradas(ubicacionId, tipo, estado).map { it.aModelo() }
    }

    override suspend fun declararReposicion(
        ubicacionId: String,
        codigoInterno: String,
        declaradaPor: String,
        observacion: String?
    ): Alerta = llamada {
        if (observacion != null && observacion.length > ReglasInventario.MAX_CARACTERES_OBSERVACION) {
            throw ReglaNegocioException(
                "OBSERVACION_MUY_LARGA",
                "La observación supera ${ReglasInventario.MAX_CARACTERES_OBSERVACION} caracteres."
            )
        }
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
            id = siguienteId("ALE", alertaDao.contar()),
            tipo = TipoAlerta.REPOSICION,
            ubicacionId = ubicacionId,
            codigoInterno = codigoInterno,
            estado = EstadoAlerta.ABIERTA,
            fechaUtc = Instant.now(),
            declaradaPorId = declaradaPor,
            observacion = observacion?.trim()?.ifEmpty { null }
        )
        alertaDao.insertar(alerta.aEntidad())
        alerta
    }

    override suspend fun marcarDescalceRevisado(alertaId: String): Alerta = llamada {
        val alerta = alertaDao.obtenerPorId(alertaId)?.aModelo()
            ?: throw noEncontrado("No existe la alerta $alertaId.")
        if (alerta.tipo != TipoAlerta.DESCALCE || alerta.estado != EstadoAlerta.ABIERTA) {
            throw ReglaNegocioException(
                "ALERTA_NO_REVISABLE", "Solo se puede revisar un descalce abierto."
            )
        }
        val revisada = alerta.copy(estado = EstadoAlerta.REVISADA)
        alertaDao.actualizar(revisada.aEntidad())
        revisada
    }

    // ---------- 12. Tablero ----------

    override suspend fun obtenerTablero(): Tablero = llamada {
        val abiertas = alertaDao.obtenerFiltradas(null, null, EstadoAlerta.ABIERTA)
        // groupBy arma un mapa ubicaciónId → sus existencias, para no consultar una por una.
        val existenciasPorUbicacion = existenciaDao.obtenerTodas().groupBy { it.ubicacionId }
        val resumenes = ubicacionDao.obtenerTodas()
            .map { it.aModelo() }
            .filter { it.tipo != TipoUbicacion.ADMINISTRATIVA }
            .map { ubicacion ->
                val propias = existenciasPorUbicacion[ubicacion.id].orEmpty()
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

    // ---------- Ayudantes privados (se llaman ya dentro del candado y la transacción) ----------

    private fun noEncontrado(mensaje: String) = ReglaNegocioException("NO_ENCONTRADO", mensaje)

    private suspend fun buscarUbicacion(ubicacionId: String): Ubicacion =
        ubicacionDao.obtenerPorId(ubicacionId)?.aModelo()
            ?: throw noEncontrado("No existe la ubicación $ubicacionId.")

    private suspend fun buscarExistencia(ubicacionId: String, codigoInterno: String): Existencia =
        existenciaDao.obtener(ubicacionId, codigoInterno)?.aModelo()
            ?: throw noEncontrado("No hay $codigoInterno en $ubicacionId.")

    /** Catálogo completo como mapa código → producto (son solo 25, cabe en memoria). */
    private suspend fun productosPorCodigo(): Map<String, Producto> =
        productoDao.obtenerTodos().associate { it.codigoInterno to it.aModelo() }

    private fun detalleDe(existencia: Existencia, productos: Map<String, Producto>): ExistenciaDetalle {
        val producto = productos.getValue(existencia.codigoInterno)
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

    private suspend fun alertaAbierta(
        tipo: TipoAlerta,
        ubicacionId: String,
        codigoInterno: String
    ): Alerta? = alertaDao.buscar(tipo, EstadoAlerta.ABIERTA, ubicacionId, codigoInterno)?.aModelo()

    /** Conteo con diferencia: abre un descalce con (contado − sistema). */
    private suspend fun abrirDescalce(movimiento: Movimiento): Alerta {
        val alerta = Alerta(
            id = siguienteId("ALE", alertaDao.contar()),
            tipo = TipoAlerta.DESCALCE,
            ubicacionId = movimiento.ubicacionId,
            codigoInterno = movimiento.codigoInterno,
            estado = EstadoAlerta.ABIERTA,
            fechaUtc = movimiento.fechaUtc,
            diferencia = movimiento.stockDespues - movimiento.stockAntes,
            movimientoId = movimiento.id
        )
        alertaDao.insertar(alerta.aEntidad())
        return alerta
    }

    /**
     * Mantiene una sola alerta de stock bajo abierta por producto y ubicación:
     * - stock < 10 y no hay una abierta → la abre y la devuelve (es "generada").
     * - stock < 10 y ya hay una abierta → solo actualiza su nivel (por ejemplo, de BAJO a CRITICO).
     * - stock ≥ 10 y hay una abierta → la cierra.
     */
    private suspend fun actualizarAlertaStockBajo(movimiento: Movimiento): Alerta? {
        val nivel = ReglasInventario.estadoDe(movimiento.stockDespues)
        val abierta = alertaAbierta(
            TipoAlerta.STOCK_BAJO, movimiento.ubicacionId, movimiento.codigoInterno
        )

        if (nivel == EstadoStock.NORMAL) {
            abierta?.let { alertaDao.actualizar(it.copy(estado = EstadoAlerta.CERRADA).aEntidad()) }
            return null
        }
        if (abierta != null) {
            if (abierta.nivel != nivel) alertaDao.actualizar(abierta.copy(nivel = nivel).aEntidad())
            return null
        }
        val nueva = Alerta(
            id = siguienteId("ALE", alertaDao.contar()),
            tipo = TipoAlerta.STOCK_BAJO,
            ubicacionId = movimiento.ubicacionId,
            codigoInterno = movimiento.codigoInterno,
            estado = EstadoAlerta.ABIERTA,
            fechaUtc = movimiento.fechaUtc,
            nivel = nivel,
            movimientoId = movimiento.id
        )
        alertaDao.insertar(nueva.aEntidad())
        return nueva
    }

    /** Una entrada en la tienda cumple la reposición pendiente de ese producto. */
    private suspend fun cerrarReposicion(ubicacionId: String, codigoInterno: String) {
        alertaAbierta(TipoAlerta.REPOSICION, ubicacionId, codigoInterno)?.let {
            alertaDao.actualizar(it.copy(estado = EstadoAlerta.CERRADA).aEntidad())
        }
    }
}
