package com.byto.inventario.domain

import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.TipoMovimiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas de las reglas puras de docs/decisiones_E1.md (umbrales, stock resultante y validaciones).
 * Cada nombre dice qué regla se comprueba; si una falla, el nombre indica qué se rompió.
 */
class ReglasInventarioTest {

    // ---------- Estado del stock (Normal ≥ 10, Bajo 5–9, Crítico 1–4, Sin stock 0) ----------

    @Test
    fun estadoEnLosBordesDeCadaUmbral() {
        assertEquals(EstadoStock.NORMAL, ReglasInventario.estadoDe(10))
        assertEquals(EstadoStock.BAJO, ReglasInventario.estadoDe(9))
        assertEquals(EstadoStock.BAJO, ReglasInventario.estadoDe(5))
        assertEquals(EstadoStock.CRITICO, ReglasInventario.estadoDe(4))
        assertEquals(EstadoStock.CRITICO, ReglasInventario.estadoDe(1))
        assertEquals(EstadoStock.SIN_STOCK, ReglasInventario.estadoDe(0))
    }

    // ---------- Stock resultante de los 4 tipos ----------

    @Test
    fun entradaSumaYSalidaResta() {
        assertEquals(12, ReglasInventario.calcularStockResultante(TipoMovimiento.ENTRADA, 7, 5))
        assertEquals(2, ReglasInventario.calcularStockResultante(TipoMovimiento.SALIDA, 7, 5))
    }

    @Test
    fun ajusteUsaElSignoDeLaCantidad() {
        assertEquals(9, ReglasInventario.calcularStockResultante(TipoMovimiento.AJUSTE, 7, 2))
        assertEquals(5, ReglasInventario.calcularStockResultante(TipoMovimiento.AJUSTE, 7, -2))
    }

    @Test
    fun conteoReemplazaElStockPorLoContado() {
        assertEquals(3, ReglasInventario.calcularStockResultante(TipoMovimiento.CONTEO, 7, 3))
    }

    // ---------- Validaciones ----------

    @Test
    fun salidaMayorAlStockEsInsuficiente() {
        assertEquals(
            ErrorValidacion.STOCK_INSUFICIENTE,
            ReglasInventario.validarMovimiento(TipoMovimiento.SALIDA, 7, 8, null, null)
        )
    }

    @Test
    fun salidaDeTodoElStockEsValida() {
        assertNull(ReglasInventario.validarMovimiento(TipoMovimiento.SALIDA, 7, 7, null, null))
    }

    @Test
    fun entradaYSalidaExigenCantidadMayorQueCero() {
        assertEquals(
            ErrorValidacion.CANTIDAD_INVALIDA,
            ReglasInventario.validarMovimiento(TipoMovimiento.ENTRADA, 7, 0, null, null)
        )
        assertEquals(
            ErrorValidacion.CANTIDAD_INVALIDA,
            ReglasInventario.validarMovimiento(TipoMovimiento.SALIDA, 7, -1, null, null)
        )
    }

    @Test
    fun ajusteSinMotivoOConMotivoVacioNoSePermite() {
        assertEquals(
            ErrorValidacion.MOTIVO_OBLIGATORIO,
            ReglasInventario.validarMovimiento(TipoMovimiento.AJUSTE, 7, -2, null, null)
        )
        assertEquals(
            ErrorValidacion.MOTIVO_OBLIGATORIO,
            ReglasInventario.validarMovimiento(TipoMovimiento.AJUSTE, 7, -2, "   ", null)
        )
    }

    @Test
    fun ajusteEnCeroNoSePermite() {
        assertEquals(
            ErrorValidacion.CANTIDAD_INVALIDA,
            ReglasInventario.validarMovimiento(TipoMovimiento.AJUSTE, 7, 0, "Merma", null)
        )
    }

    @Test
    fun ajusteQueDejaStockNegativoNoSePermite() {
        assertEquals(
            ErrorValidacion.STOCK_NEGATIVO,
            ReglasInventario.validarMovimiento(TipoMovimiento.AJUSTE, 3, -4, "Merma", null)
        )
    }

    @Test
    fun ajusteQueDejaStockEnCeroEsValido() {
        assertNull(ReglasInventario.validarMovimiento(TipoMovimiento.AJUSTE, 3, -3, "Merma", null))
    }

    @Test
    fun conteoEnCeroEsValido() {
        // Contar 0 es posible: el producto puede haberse perdido.
        assertNull(ReglasInventario.validarMovimiento(TipoMovimiento.CONTEO, 7, 0, null, null))
    }

    @Test
    fun observacionDe200CaracteresEsValidaYDe201No() {
        assertNull(ReglasInventario.validarMovimiento(TipoMovimiento.ENTRADA, 7, 1, null, "a".repeat(200)))
        assertEquals(
            ErrorValidacion.OBSERVACION_MUY_LARGA,
            ReglasInventario.validarMovimiento(TipoMovimiento.ENTRADA, 7, 1, null, "a".repeat(201))
        )
    }
}
