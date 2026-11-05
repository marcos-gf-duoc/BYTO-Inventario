package com.byto.inventario.domain

import com.byto.inventario.data.fake.DatosSinteticos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Región y nombre corto de las ubicaciones sintéticas (mockup P14). */
class RegionesTest {

    private fun ubicacion(id: String) = DatosSinteticos.ubicaciones.first { it.id == id }

    @Test
    fun laRegionSaleDelId() {
        assertEquals("Santiago", Regiones.nombreDe("T-ST-1"))
        assertEquals("La Serena", Regiones.nombreDe("B-LS-1"))
        assertEquals("Concepción", Regiones.nombreDe("T-CC-3"))
        assertNull(Regiones.nombreDe("CM"))
    }

    @Test
    fun nombreCortoQuitaLaRegion() {
        assertEquals("Centro", Regiones.nombreCorto(ubicacion("T-ST-1")))
        assertEquals("Sur", Regiones.nombreCorto(ubicacion("T-CC-3")))
        assertEquals("Bodega Regional", Regiones.nombreCorto(ubicacion("B-LS-1")))
        assertEquals("Casa Matriz BYTO", Regiones.nombreCorto(ubicacion("CM")))
    }
}
