package com.byto.inventario.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas unitarias del validador EAN-13. Corren en el computador (sin emulador)
 * porque la función es pura: Ejecutar con clic derecho → Run 'ValidadorEanTest'.
 */
class ValidadorEanTest {

    @Test
    fun codigoDelCatalogoEsValido() {
        // Mat de yoga (DE-03).
        assertNull(ValidadorEan.validar("2000000000084"))
    }

    @Test
    fun codigoValidoQueNoEstaEnElCatalogo() {
        // Su dígito verificador es correcto; que no exista lo decide la búsqueda, no el validador.
        assertNull(ValidadorEan.validar("2000000000268"))
    }

    @Test
    fun codigoCortoTieneErrorDeLargo() {
        assertEquals(ErrorEan.LARGO_INVALIDO, ValidadorEan.validar("123"))
    }

    @Test
    fun codigoConLetrasNoEsNumerico() {
        assertEquals(ErrorEan.NO_NUMERICO, ValidadorEan.validar("20000000000A4"))
    }

    @Test
    fun ultimoDigitoIncorrectoEsInvalido() {
        assertEquals(ErrorEan.DIGITO_VERIFICADOR_INVALIDO, ValidadorEan.validar("2000000000085"))
    }

    @Test
    fun todosLosCodigosDelCatalogoSonValidos() {
        // Los 25 EAN de docs/decisiones_E1.md: 200000000001 a 200000000025 más su dígito.
        val catalogo = listOf(
            "2000000000015", "2000000000022", "2000000000039", "2000000000046", "2000000000053",
            "2000000000060", "2000000000077", "2000000000084", "2000000000091", "2000000000107",
            "2000000000114", "2000000000121", "2000000000138", "2000000000145", "2000000000152",
            "2000000000169", "2000000000176", "2000000000183", "2000000000190", "2000000000206",
            "2000000000213", "2000000000220", "2000000000237", "2000000000244", "2000000000251"
        )
        catalogo.forEach { assertNull("Falla $it", ValidadorEan.validar(it)) }
    }
}
