package com.byto.inventario.domain

import com.byto.inventario.data.model.Alerta
import com.byto.inventario.data.model.EstadoAlerta
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.ExistenciaDetalle
import com.byto.inventario.data.model.Movimiento
import com.byto.inventario.data.model.TipoAlerta
import com.byto.inventario.data.model.TipoMovimiento
import com.byto.inventario.data.model.Ubicacion
import java.time.LocalDate

/** Los tres reportes de P15 (Caso §3.4: exportación a Excel simulada localmente). */
enum class TipoReporte { STOCK, MOVIMIENTOS, ALERTAS }

/** Errores del formulario de exportación (P15); null = el campo está bien. */
data class ErroresExportacion(
    val tipo: Boolean = false,
    val ubicacion: Boolean = false
) {
    val hayErrores: Boolean get() = tipo || ubicacion
}

/**
 * Arma el texto de un reporte CSV pensado para Excel en español (docs/decisiones_E1.md):
 * - Separador ";" porque en Chile la coma es el separador decimal y Excel la confundiría.
 * - UTF-8 con BOM (un carácter invisible al inicio) para que Excel muestre bien las tildes.
 * - Fechas en hora de Chile y operadores solo con su ID anonimizado.
 *
 * Es pura (sin Android): recibe los datos ya cargados y devuelve el texto. Escribirlo en un
 * archivo es tarea de dispositivo/EscritorArchivos.
 */
object ReporteCsv {

    private const val SEPARADOR = ";"
    private const val BOM = "﻿"

    /** Fin de línea de Windows: es el que espera Excel. */
    private const val FIN_LINEA = "\r\n"

    /** Tipo y ubicación son obligatorios ("todas" cuenta como elegida). */
    fun validar(tipo: TipoReporte?, ubicacionElegida: Boolean) =
        ErroresExportacion(tipo = tipo == null, ubicacion = !ubicacionElegida)

    /**
     * Nombre sugerido: "byto_stock_T-ST-1_2026-11-05.csv".
     * @param ubicacionId null = todas las ubicaciones.
     */
    fun nombreArchivo(tipo: TipoReporte, ubicacionId: String?, hoy: LocalDate): String =
        "byto_${tipo.name.lowercase()}_${ubicacionId ?: "todas"}_$hoy.csv"

    // ---------- Los tres reportes ----------

    fun stock(filas: List<Pair<Ubicacion, ExistenciaDetalle>>): String = armar(
        listOf(
            "Ubicación", "Nombre ubicación", "Código interno", "EAN-13", "Producto",
            "Categoría", "Stock", "Stock mínimo", "Estado"
        ),
        filas.map { (ubicacion, detalle) ->
            listOf(
                ubicacion.id, ubicacion.nombre, detalle.producto.codigoInterno, detalle.producto.ean13,
                detalle.producto.nombre, detalle.producto.categoria, detalle.existencia.stock.toString(),
                detalle.existencia.stockMinimo.toString(), texto(detalle.estado)
            )
        }
    )

    /** @param nombres código interno → nombre del producto. */
    fun movimientos(movimientos: List<Movimiento>, nombres: Map<String, String>): String = armar(
        listOf(
            "ID", "Fecha (hora de Chile)", "Ubicación", "Código interno", "Producto", "Tipo",
            "Cantidad", "Stock antes", "Stock después", "Operador", "Motivo", "Observación"
        ),
        movimientos.map { m ->
            listOf(
                m.id, Fechas.fechaHoraChile(m.fechaUtc), m.ubicacionId, m.codigoInterno,
                nombres[m.codigoInterno].orEmpty(), texto(m.tipo), m.cantidad.toString(),
                m.stockAntes.toString(), m.stockDespues.toString(), m.operadorId,
                m.motivo.orEmpty(), m.observacion.orEmpty()
            )
        }
    )

    /** @param nombres código interno → nombre del producto. */
    fun alertas(alertas: List<Alerta>, nombres: Map<String, String>): String = armar(
        listOf(
            "ID", "Fecha (hora de Chile)", "Ubicación", "Código interno", "Producto", "Tipo",
            "Nivel", "Diferencia", "Estado", "Declarada por", "Observación"
        ),
        alertas.map { a ->
            listOf(
                a.id, Fechas.fechaHoraChile(a.fechaUtc), a.ubicacionId, a.codigoInterno,
                nombres[a.codigoInterno].orEmpty(), texto(a.tipo), a.nivel?.let { texto(it) }.orEmpty(),
                a.diferencia?.toString().orEmpty(), texto(a.estado), a.declaradaPorId.orEmpty(),
                a.observacion.orEmpty()
            )
        }
    )

    // ---------- Formato CSV ----------

    /** Une encabezados y filas: una línea por fila, campos separados por ";". */
    fun armar(encabezados: List<String>, filas: List<List<String>>): String =
        (listOf(encabezados) + filas).joinToString(separator = FIN_LINEA, prefix = BOM, postfix = FIN_LINEA) { fila ->
            fila.joinToString(SEPARADOR) { campo(it) }
        }

    /**
     * Si un texto trae ";", comillas o saltos de línea, Excel lo partiría en varias columnas.
     * Regla del formato CSV: se encierra entre comillas y cada comilla interna se duplica.
     */
    fun campo(valor: String): String =
        if (valor.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + valor.replace("\"", "\"\"") + "\""
        } else {
            valor
        }

    // ---------- Valores en español dentro del archivo ----------

    private fun texto(estado: EstadoStock) = when (estado) {
        EstadoStock.NORMAL -> "Normal"
        EstadoStock.BAJO -> "Bajo"
        EstadoStock.CRITICO -> "Crítico"
        EstadoStock.SIN_STOCK -> "Sin stock"
    }

    private fun texto(tipo: TipoMovimiento) = when (tipo) {
        TipoMovimiento.ENTRADA -> "Entrada"
        TipoMovimiento.SALIDA -> "Salida"
        TipoMovimiento.AJUSTE -> "Ajuste"
        TipoMovimiento.CONTEO -> "Conteo"
    }

    private fun texto(tipo: TipoAlerta) = when (tipo) {
        TipoAlerta.STOCK_BAJO -> "Stock bajo"
        TipoAlerta.DESCALCE -> "Descalce"
        TipoAlerta.REPOSICION -> "Reposición"
    }

    private fun texto(estado: EstadoAlerta) = when (estado) {
        EstadoAlerta.ABIERTA -> "Abierta"
        EstadoAlerta.REVISADA -> "Revisada"
        EstadoAlerta.CERRADA -> "Cerrada"
    }
}
