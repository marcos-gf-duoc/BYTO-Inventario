package com.byto.inventario.domain

import com.byto.inventario.data.fake.DatosSinteticos
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de quién puede registrar movimientos, con los usuarios y ubicaciones
 * sintéticos reales (los mismos que usa la app). Deciden si P07 y P08 muestran "Registrar".
 */
class PermisosTest {

    private fun usuario(id: String) = DatosSinteticos.usuarios.first { it.id == id }
    private fun ubicacion(id: String) = DatosSinteticos.ubicaciones.first { it.id == id }

    @Test
    fun operadorRegistraEnSuTienda() {
        assertTrue(Permisos.puedeRegistrarMovimientos(usuario("OP-001"), ubicacion("T-ST-1")))
    }

    @Test
    fun operadorNoRegistraEnTiendaAjena() {
        assertFalse(Permisos.puedeRegistrarMovimientos(usuario("OP-001"), ubicacion("T-LS-1")))
    }

    @Test
    fun encargadoNoRegistraNiEnSuBodegaNiEnTiendasDeConsulta() {
        assertFalse(Permisos.puedeRegistrarMovimientos(usuario("EB-001"), ubicacion("B-ST-1")))
        assertFalse(Permisos.puedeRegistrarMovimientos(usuario("EB-001"), ubicacion("T-ST-1")))
    }

    @Test
    fun gerenteNoRegistra() {
        assertFalse(Permisos.puedeRegistrarMovimientos(usuario("GE-001"), ubicacion("T-ST-1")))
    }

    @Test
    fun sinSesionNoRegistra() {
        assertFalse(Permisos.puedeRegistrarMovimientos(null, ubicacion("T-ST-1")))
    }
}
