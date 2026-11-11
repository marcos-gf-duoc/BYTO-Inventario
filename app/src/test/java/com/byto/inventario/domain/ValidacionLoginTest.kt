package com.byto.inventario.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionLoginTest {

    @Test
    fun camposVaciosMarcanCadaCampoComoObligatorio() {
        val errores = ValidacionLogin.validar(usuarioId = "   ", pin = "")
        assertEquals(ErrorLogin.USUARIO_OBLIGATORIO, errores.usuario)
        assertEquals(ErrorLogin.PIN_OBLIGATORIO, errores.pin)
        assertTrue(errores.hayErrores)
    }

    @Test
    fun pinCortoEsIncompleto() {
        val errores = ValidacionLogin.validar("OP-001", "1234")
        assertNull(errores.usuario)
        assertEquals(ErrorLogin.PIN_INCOMPLETO, errores.pin)
    }

    @Test
    fun pinConLetrasNoEsValido() {
        assertEquals(ErrorLogin.PIN_INCOMPLETO, ValidacionLogin.validar("OP-001", "12a456").pin)
    }

    @Test
    fun idYPinDeSeisDigitosNoTienenErrores() {
        assertFalse(ValidacionLogin.validar("OP-001", "123456").hayErrores)
    }

    @Test
    fun limpiarPinDejaSoloSeisDigitos() {
        assertEquals("123456", ValidacionLogin.limpiarPin("12 34-56789"))
        assertEquals("", ValidacionLogin.limpiarPin("abc"))
    }

    @Test
    fun normalizarIdQuitaEspaciosYUsaMayusculas() {
        assertEquals("OP-001", ValidacionLogin.normalizarId("  op-001 "))
    }

    @Test
    fun rechazoDelRepositorioSeAsignaAlCampoCorrecto() {
        assertEquals(ErrorLogin.USUARIO_NO_ENCONTRADO, ValidacionLogin.desdeRechazo("NO_ENCONTRADO")?.usuario)
        assertEquals(ErrorLogin.PIN_INCORRECTO, ValidacionLogin.desdeRechazo("PIN_INCORRECTO")?.pin)
        assertNull(ValidacionLogin.desdeRechazo("OTRO_CODIGO"))
    }
}
