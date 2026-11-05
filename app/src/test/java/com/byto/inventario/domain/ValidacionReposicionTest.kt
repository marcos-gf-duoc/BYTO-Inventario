package com.byto.inventario.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas del formulario de reposición (P12). MU-01 en T-ST-1 ya tiene una abierta en los datos sintéticos. */
class ValidacionReposicionTest {

    private val abiertas = setOf("T-ST-1" to "MU-01")

    @Test
    fun formularioCompletoEsValido() {
        val errores = ValidacionReposicion.validar("T-ST-1", "DE-03", "Enviar desde B-ST-1", abiertas)
        assertFalse(errores.hayErrores)
    }

    @Test
    fun tiendaYProductoSonObligatorios() {
        val errores = ValidacionReposicion.validar(null, null, "", abiertas)
        assertEquals(ErrorReposicion.TIENDA_OBLIGATORIA, errores.tienda)
        assertEquals(ErrorReposicion.PRODUCTO_OBLIGATORIO, errores.producto)
        assertNull(errores.observacion)
    }

    @Test
    fun noSeRepiteUnaReposicionAbierta() {
        val errores = ValidacionReposicion.validar("T-ST-1", "MU-01", "", abiertas)
        assertEquals(ErrorReposicion.REPOSICION_YA_ABIERTA, errores.producto)
    }

    @Test
    fun elMismoProductoEnOtraTiendaSiSePuede() {
        assertFalse(ValidacionReposicion.validar("T-ST-2", "MU-01", "", abiertas).hayErrores)
    }

    @Test
    fun observacionTieneUnMaximoDe200Caracteres() {
        assertNull(ValidacionReposicion.validar("T-ST-1", "DE-03", "x".repeat(200), abiertas).observacion)
        val errores = ValidacionReposicion.validar("T-ST-1", "DE-03", "x".repeat(201), abiertas)
        assertEquals(ErrorReposicion.OBSERVACION_MUY_LARGA, errores.observacion)
        assertTrue(errores.hayErrores)
    }
}
