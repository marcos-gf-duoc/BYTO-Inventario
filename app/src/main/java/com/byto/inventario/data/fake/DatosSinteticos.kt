package com.byto.inventario.data.fake

import com.byto.inventario.data.model.Alerta
import com.byto.inventario.data.model.EstadoAlerta
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.Existencia
import com.byto.inventario.data.model.Movimiento
import com.byto.inventario.data.model.Perfil
import com.byto.inventario.data.model.Producto
import com.byto.inventario.data.model.TipoAlerta
import com.byto.inventario.data.model.TipoMovimiento
import com.byto.inventario.data.model.TipoUbicacion
import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.data.model.Usuario
import java.time.Instant

/**
 * Datos ficticios iniciales de BYTO, copiados de docs/decisiones_E1.md.
 * Si cambias algo aquí, cambia primero el documento: el documento manda.
 *
 * Es un `object` (una única instancia) porque son constantes de solo lectura;
 * el FakeInventarioRepository copia estas listas y trabaja sobre sus copias.
 */
object DatosSinteticos {

    // ---------- Ubicaciones (13: Casa Matriz + 3 bodegas + 9 tiendas) ----------

    val ubicaciones: List<Ubicacion> = listOf(
        Ubicacion("CM", "Casa Matriz BYTO", TipoUbicacion.ADMINISTRATIVA, null, activa = true),
        Ubicacion("B-ST-1", "Bodega Regional Santiago", TipoUbicacion.BODEGA, "CM", activa = true),
        Ubicacion("T-ST-1", "BYTO Santiago Centro", TipoUbicacion.TIENDA, "B-ST-1", activa = true),
        Ubicacion("T-ST-2", "BYTO Santiago Norte", TipoUbicacion.TIENDA, "B-ST-1", activa = true),
        Ubicacion("T-ST-3", "BYTO Santiago Sur", TipoUbicacion.TIENDA, "B-ST-1", activa = true),
        Ubicacion("B-LS-1", "Bodega Regional La Serena", TipoUbicacion.BODEGA, "CM", activa = true),
        Ubicacion("T-LS-1", "BYTO La Serena Centro", TipoUbicacion.TIENDA, "B-LS-1", activa = true),
        Ubicacion("T-LS-2", "BYTO La Serena Norte", TipoUbicacion.TIENDA, "B-LS-1", activa = true),
        Ubicacion("T-LS-3", "BYTO La Serena Sur", TipoUbicacion.TIENDA, "B-LS-1", activa = true),
        Ubicacion("B-CC-1", "Bodega Regional Concepción", TipoUbicacion.BODEGA, "CM", activa = true),
        Ubicacion("T-CC-1", "BYTO Concepción Centro", TipoUbicacion.TIENDA, "B-CC-1", activa = true),
        Ubicacion("T-CC-2", "BYTO Concepción Norte", TipoUbicacion.TIENDA, "B-CC-1", activa = true),
        // Inactiva a propósito, para demostrar la administración de tiendas.
        Ubicacion("T-CC-3", "BYTO Concepción Sur", TipoUbicacion.TIENDA, "B-CC-1", activa = false)
    )

    // ---------- Usuarios simulados (4) ----------

    val usuarios: List<Usuario> = listOf(
        Usuario(
            id = "OP-001",
            etiqueta = "Operario Santiago",
            perfil = Perfil.OPERADOR,
            ubicacionesIds = listOf("T-ST-1", "T-ST-2", "T-ST-3")
        ),
        Usuario(
            id = "OP-002",
            etiqueta = "Operario La Serena",
            perfil = Perfil.OPERADOR,
            ubicacionesIds = listOf("T-LS-1", "T-LS-2", "T-LS-3")
        ),
        Usuario(
            id = "EB-001",
            etiqueta = "Encargado Bodega Santiago",
            perfil = Perfil.ENCARGADO_BODEGA,
            ubicacionesIds = listOf("B-ST-1"),
            // Las tiendas que abastece su bodega: solo consulta.
            ubicacionesConsultaIds = listOf("T-ST-1", "T-ST-2", "T-ST-3")
        ),
        Usuario(
            id = "GE-001",
            etiqueta = "Gerente BYTO",
            perfil = Perfil.GERENTE,
            ubicacionesIds = ubicaciones.map { it.id } // Todas
        )
    )

    // ---------- Catálogo (25 productos: 5 categorías × 5) ----------

    val productos: List<Producto> = listOf(
        Producto("2000000000015", "TE-01", "Audífonos inalámbricos", "Tecnología"),
        Producto("2000000000022", "TE-02", "Mouse inalámbrico", "Tecnología"),
        Producto("2000000000039", "TE-03", "Teclado USB", "Tecnología"),
        Producto("2000000000046", "TE-04", "Parlante Bluetooth portátil", "Tecnología"),
        Producto("2000000000053", "TE-05", "Cargador USB-C 20 W", "Tecnología"),
        Producto("2000000000060", "DE-01", "Balón de fútbol N.º 5", "Deportes"),
        Producto("2000000000077", "DE-02", "Mancuernas 5 kg (par)", "Deportes"),
        Producto("2000000000084", "DE-03", "Mat de yoga", "Deportes"),
        Producto("2000000000091", "DE-04", "Botella deportiva 750 ml", "Deportes"),
        Producto("2000000000107", "DE-05", "Cuerda para saltar", "Deportes"),
        Producto("2000000000114", "RH-01", "Polera de algodón básica, talla M", "Ropa Hombre"),
        Producto("2000000000121", "RH-02", "Jeans corte recto, talla M", "Ropa Hombre"),
        Producto("2000000000138", "RH-03", "Polerón con capucha, talla M", "Ropa Hombre"),
        Producto("2000000000145", "RH-04", "Camisa manga larga, talla M", "Ropa Hombre"),
        Producto("2000000000152", "RH-05", "Chaqueta cortaviento, talla M", "Ropa Hombre"),
        Producto("2000000000169", "RM-01", "Blusa manga corta, talla M", "Ropa Mujer"),
        Producto("2000000000176", "RM-02", "Jeans tiro alto, talla M", "Ropa Mujer"),
        Producto("2000000000183", "RM-03", "Vestido casual, talla M", "Ropa Mujer"),
        Producto("2000000000190", "RM-04", "Polerón cuello redondo, talla M", "Ropa Mujer"),
        Producto("2000000000206", "RM-05", "Chaqueta acolchada, talla M", "Ropa Mujer"),
        Producto("2000000000213", "MU-01", "Silla de escritorio", "Muebles"),
        Producto("2000000000220", "MU-02", "Mesa de centro", "Muebles"),
        Producto("2000000000237", "MU-03", "Estante de 5 niveles", "Muebles"),
        Producto("2000000000244", "MU-04", "Velador de 2 cajones", "Muebles"),
        Producto("2000000000251", "MU-05", "Escritorio 120 cm", "Muebles")
    )

    // ---------- Stock inicial ----------

    private const val STOCK_BODEGA = 100
    private const val STOCK_TIENDA_ACTIVA = 20
    private const val STOCK_TIENDA_INACTIVA = 0

    /**
     * Excepciones a la regla general (escenarios de demostración).
     * Clave: par (ubicacionId, codigoInterno) → stock.
     */
    private val excepcionesStock: Map<Pair<String, String>, Int> = mapOf(
        ("T-ST-1" to "TE-01") to 18, // Normal (tras una entrada)
        ("T-ST-1" to "DE-03") to 7,  // Bajo
        ("T-ST-1" to "RH-01") to 3,  // Crítico
        ("T-ST-1" to "MU-01") to 0,  // Sin stock + reposición declarada
        ("T-ST-2" to "RM-02") to 6,  // Bajo producido por un descalce
        ("B-ST-1" to "DE-02") to 8,  // Bajo en bodega
        ("T-LS-1" to "TE-04") to 20  // Normal (tras una entrada)
    )

    /**
     * Una existencia por cada producto en cada ubicación con stock.
     * Casa Matriz (ADMINISTRATIVA) no tiene existencias.
     * Resultado: 12 ubicaciones × 25 productos = 300 existencias.
     */
    val existencias: List<Existencia> = ubicaciones
        .filter { it.tipo != TipoUbicacion.ADMINISTRATIVA }
        .flatMap { ubicacion ->
            productos.map { producto ->
                val stockGeneral = when {
                    ubicacion.tipo == TipoUbicacion.BODEGA -> STOCK_BODEGA
                    ubicacion.activa -> STOCK_TIENDA_ACTIVA
                    else -> STOCK_TIENDA_INACTIVA
                }
                // Si el par está en las excepciones se usa ese valor; si no, la regla general.
                val stock = excepcionesStock[ubicacion.id to producto.codigoInterno] ?: stockGeneral
                Existencia(ubicacion.id, producto.codigoInterno, stock)
            }
        }

    // ---------- Movimientos históricos (6, fechas en UTC) ----------

    val movimientos: List<Movimiento> = listOf(
        Movimiento(
            id = "MOV-0001", tipo = TipoMovimiento.ENTRADA,
            ubicacionId = "T-ST-1", codigoInterno = "TE-01",
            cantidad = 8, stockAntes = 10, stockDespues = 18,
            fechaUtc = Instant.parse("2026-10-16T13:10:00Z"), operadorId = "OP-001",
            observacion = "Recepción semanal"
        ),
        Movimiento(
            id = "MOV-0002", tipo = TipoMovimiento.SALIDA,
            ubicacionId = "T-ST-1", codigoInterno = "DE-03",
            cantidad = 13, stockAntes = 20, stockDespues = 7,
            fechaUtc = Instant.parse("2026-10-16T18:40:00Z"), operadorId = "OP-001",
            observacion = "Ventas del día"
        ),
        Movimiento(
            id = "MOV-0003", tipo = TipoMovimiento.CONTEO,
            ubicacionId = "T-ST-2", codigoInterno = "RM-02",
            cantidad = 6, stockAntes = 10, stockDespues = 6,
            fechaUtc = Instant.parse("2026-10-17T12:30:00Z"), operadorId = "OP-001",
            observacion = "Conteo de sección mujer"
        ),
        Movimiento(
            id = "MOV-0004", tipo = TipoMovimiento.AJUSTE,
            ubicacionId = "T-ST-1", codigoInterno = "RH-01",
            cantidad = -2, stockAntes = 5, stockDespues = 3,
            fechaUtc = Instant.parse("2026-10-17T15:05:00Z"), operadorId = "OP-001",
            // El documento lo anota como "Motivo: producto dañado"; va en el campo motivo,
            // que es obligatorio en los ajustes.
            motivo = "Producto dañado"
        ),
        Movimiento(
            id = "MOV-0005", tipo = TipoMovimiento.SALIDA,
            ubicacionId = "T-ST-1", codigoInterno = "MU-01",
            cantidad = 4, stockAntes = 4, stockDespues = 0,
            fechaUtc = Instant.parse("2026-10-18T14:20:00Z"), operadorId = "OP-001",
            observacion = "Ventas del día"
        ),
        Movimiento(
            id = "MOV-0006", tipo = TipoMovimiento.ENTRADA,
            ubicacionId = "T-LS-1", codigoInterno = "TE-04",
            cantidad = 12, stockAntes = 8, stockDespues = 20,
            fechaUtc = Instant.parse("2026-10-18T16:00:00Z"), operadorId = "OP-002",
            observacion = "Recepción semanal"
        )
    )

    // ---------- Alertas iniciales (7, todas abiertas) ----------
    // Los IDs siguen el orden del documento; coinciden con el ejemplo de la API
    // (ALE-0002 descalce y ALE-0003 stock bajo nacen del movimiento MOV-0003).

    val alertas: List<Alerta> = listOf(
        Alerta(
            id = "ALE-0001", tipo = TipoAlerta.STOCK_BAJO,
            ubicacionId = "T-ST-1", codigoInterno = "DE-03", estado = EstadoAlerta.ABIERTA,
            fechaUtc = Instant.parse("2026-10-16T18:40:00Z"),
            nivel = EstadoStock.BAJO, movimientoId = "MOV-0002"
        ),
        Alerta(
            id = "ALE-0002", tipo = TipoAlerta.DESCALCE,
            ubicacionId = "T-ST-2", codigoInterno = "RM-02", estado = EstadoAlerta.ABIERTA,
            fechaUtc = Instant.parse("2026-10-17T12:30:00Z"),
            diferencia = -4, movimientoId = "MOV-0003"
        ),
        Alerta(
            id = "ALE-0003", tipo = TipoAlerta.STOCK_BAJO,
            ubicacionId = "T-ST-2", codigoInterno = "RM-02", estado = EstadoAlerta.ABIERTA,
            fechaUtc = Instant.parse("2026-10-17T12:30:00Z"),
            nivel = EstadoStock.BAJO, movimientoId = "MOV-0003"
        ),
        Alerta(
            id = "ALE-0004", tipo = TipoAlerta.STOCK_BAJO,
            ubicacionId = "T-ST-1", codigoInterno = "RH-01", estado = EstadoAlerta.ABIERTA,
            fechaUtc = Instant.parse("2026-10-17T15:05:00Z"),
            nivel = EstadoStock.CRITICO, movimientoId = "MOV-0004"
        ),
        Alerta(
            id = "ALE-0005", tipo = TipoAlerta.STOCK_BAJO,
            ubicacionId = "T-ST-1", codigoInterno = "MU-01", estado = EstadoAlerta.ABIERTA,
            fechaUtc = Instant.parse("2026-10-18T14:20:00Z"),
            nivel = EstadoStock.SIN_STOCK, movimientoId = "MOV-0005"
        ),
        Alerta(
            id = "ALE-0006", tipo = TipoAlerta.REPOSICION,
            ubicacionId = "T-ST-1", codigoInterno = "MU-01", estado = EstadoAlerta.ABIERTA,
            fechaUtc = Instant.parse("2026-10-18T17:00:00Z"),
            declaradaPorId = "EB-001"
        ),
        Alerta(
            id = "ALE-0007", tipo = TipoAlerta.STOCK_BAJO,
            ubicacionId = "B-ST-1", codigoInterno = "DE-02", estado = EstadoAlerta.ABIERTA,
            // Carga inicial: no la originó ningún movimiento.
            fechaUtc = Instant.parse("2026-10-16T12:00:00Z"),
            nivel = EstadoStock.BAJO
        )
    )
}
