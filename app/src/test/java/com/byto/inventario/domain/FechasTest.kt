package com.byto.inventario.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** Las fechas se guardan en UTC y se muestran en hora de Chile (America/Santiago). */
class FechasTest {

    @Test
    fun fechaCortaMuestraLaHoraDeChile() {
        // Movimiento 3 de los datos sintéticos: 12:30 UTC son las 09:30 en Chile (UTC−3 en octubre).
        assertEquals("17-10 09:30", Fechas.fechaHoraCortaChile(Instant.parse("2026-10-17T12:30:00Z")))
    }

    @Test
    fun fechaCompletaIncluyeElAnio() {
        assertEquals("16-10-2026 15:40", Fechas.fechaHoraChile(Instant.parse("2026-10-16T18:40:00Z")))
    }

    // ---------- Rango de días del historial (P13) ----------

    @Test
    fun unDiaEnChileEmpiezaYTerminaTresHorasDespuesEnUtc() {
        val dia = LocalDate.of(2026, 10, 16)
        assertEquals(Instant.parse("2026-10-16T03:00:00Z"), Fechas.inicioDelDia(dia))
        assertEquals(Instant.parse("2026-10-17T02:59:59.999999999Z"), Fechas.finDelDia(dia))
    }

    @Test
    fun desdeNoPuedeSerPosteriorAHasta() {
        val dia16 = LocalDate.of(2026, 10, 16)
        val dia18 = LocalDate.of(2026, 10, 18)
        assertTrue(Fechas.rangoValido(dia16, dia18))
        assertTrue(Fechas.rangoValido(dia16, dia16))
        assertFalse(Fechas.rangoValido(dia18, dia16))
    }

    @Test
    fun rangoAbiertoPorUnLadoEsValido() {
        assertTrue(Fechas.rangoValido(null, LocalDate.of(2026, 10, 16)))
        assertTrue(Fechas.rangoValido(LocalDate.of(2026, 10, 16), null))
        assertTrue(Fechas.rangoValido(null, null))
    }
}
