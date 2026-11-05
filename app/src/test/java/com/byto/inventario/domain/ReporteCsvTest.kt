package com.byto.inventario.domain

import com.byto.inventario.data.fake.DatosSinteticos
import com.byto.inventario.data.model.ExistenciaDetalle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Pruebas del CSV de P15 con los datos sintéticos. */
class ReporteCsvTest {

    private val nombres = DatosSinteticos.productos.associate { it.codigoInterno to it.nombre }

    /** Líneas del CSV sin el BOM inicial ni la línea vacía final. */
    private fun lineas(csv: String) = csv.removePrefix("\uFEFF").split("\r\n").dropLast(1)

    @Test
    fun empiezaConBomYUsaPuntoYComa() {
        val csv = ReporteCsv.armar(listOf("A", "B"), listOf(listOf("1", "2")))
        assertTrue(csv.startsWith("\uFEFF"))
        assertEquals(listOf("A;B", "1;2"), lineas(csv))
    }

    @Test
    fun camposConSeparadorOComillasSeEncierranEntreComillas() {
        assertEquals("simple", ReporteCsv.campo("simple"))
        assertEquals("\"uno;dos\"", ReporteCsv.campo("uno;dos"))
        assertEquals("\"dijo \"\"hola\"\"\"", ReporteCsv.campo("dijo \"hola\""))
        // La coma no es separador en este formato: no necesita comillas.
        assertEquals("Polera de algodón básica, talla M", ReporteCsv.campo("Polera de algodón básica, talla M"))
    }

    @Test
    fun movimientosMuestranFechaDeChileYOperadorAnonimizado() {
        val csv = ReporteCsv.movimientos(DatosSinteticos.movimientos.take(1), nombres)
        assertEquals(
            "MOV-0001;16-10-2026 10:10;T-ST-1;TE-01;Audífonos inalámbricos;Entrada;8;10;18;OP-001;;Recepción semanal",
            lineas(csv)[1]
        )
    }

    @Test
    fun stockTieneUnaFilaPorExistencia() {
        val ubicacion = DatosSinteticos.ubicaciones.first { it.id == "T-ST-1" }
        val producto = DatosSinteticos.productos.first { it.codigoInterno == "RH-01" }
        val existencia = DatosSinteticos.existencias.first { it.ubicacionId == "T-ST-1" && it.codigoInterno == "RH-01" }
        val detalle = ExistenciaDetalle(existencia, producto, ReglasInventario.estadoDe(existencia.stock))

        val filas = lineas(ReporteCsv.stock(listOf(ubicacion to detalle)))
        assertEquals(2, filas.size)
        assertEquals(
            "T-ST-1;BYTO Santiago Centro;RH-01;2000000000114;Polera de algodón básica, talla M;Ropa Hombre;3;10;Crítico",
            filas[1]
        )
    }

    @Test
    fun alertasIncluyenNivelDiferenciaYEstado() {
        val descalce = DatosSinteticos.alertas.first { it.diferencia != null }
        val fila = lineas(ReporteCsv.alertas(listOf(descalce), nombres))[1].split(";")
        assertEquals("Descalce", fila[5])
        assertEquals("-4", fila[7])
        assertEquals("Abierta", fila[8])
    }

    @Test
    fun nombreDelArchivoIncluyeTipoUbicacionYFecha() {
        val hoy = LocalDate.of(2026, 11, 5)
        assertEquals("byto_stock_T-ST-1_2026-11-05.csv", ReporteCsv.nombreArchivo(TipoReporte.STOCK, "T-ST-1", hoy))
        assertEquals("byto_alertas_todas_2026-11-05.csv", ReporteCsv.nombreArchivo(TipoReporte.ALERTAS, null, hoy))
    }

    @Test
    fun tipoYUbicacionSonObligatorios() {
        assertTrue(ReporteCsv.validar(null, false).tipo)
        assertTrue(ReporteCsv.validar(null, false).ubicacion)
        assertFalse(ReporteCsv.validar(TipoReporte.MOVIMIENTOS, true).hayErrores)
    }
}
