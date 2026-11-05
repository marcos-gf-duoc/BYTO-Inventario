package com.byto.inventario.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

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
}
