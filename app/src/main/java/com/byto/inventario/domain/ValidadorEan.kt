package com.byto.inventario.domain

/** Motivos por los que un código EAN-13 no es válido. */
enum class ErrorEan {
    /** No tiene exactamente 13 caracteres. */
    LARGO_INVALIDO,

    /** Tiene caracteres que no son dígitos. */
    NO_NUMERICO,

    /** El último dígito no coincide con el calculado a partir de los otros 12. */
    DIGITO_VERIFICADOR_INVALIDO
}

/**
 * Validación pura de códigos EAN-13 (sin Android, se puede probar con un test simple).
 *
 * El último dígito de un EAN-13 es un "dígito verificador": se calcula con los 12
 * anteriores y sirve para detectar errores de tipeo o de lectura.
 */
object ValidadorEan {

    const val LARGO = 13

    /** Devuelve null si el código es válido, o el primer error encontrado. */
    fun validar(codigo: String): ErrorEan? {
        if (codigo.length != LARGO) return ErrorEan.LARGO_INVALIDO
        if (!codigo.all { it.isDigit() }) return ErrorEan.NO_NUMERICO
        val esperado = digitoVerificador(codigo.take(LARGO - 1))
        if (codigo.last().digitToInt() != esperado) return ErrorEan.DIGITO_VERIFICADOR_INVALIDO
        return null
    }

    /**
     * Calcula el dígito verificador a partir de los primeros 12 dígitos:
     * 1. Se multiplican por 1 y 3 alternadamente (posición 1 × 1, posición 2 × 3, …).
     * 2. Se suman los resultados.
     * 3. El dígito es lo que falta para llegar a la siguiente decena (0 si ya es múltiplo de 10).
     * Ejemplo: 200000000008 → 2×1 + 8×3 = 26 → faltan 4 → código 2000000000084.
     */
    fun digitoVerificador(primeros12: String): Int {
        val suma = primeros12.mapIndexed { indice, caracter ->
            val peso = if (indice % 2 == 0) 1 else 3
            caracter.digitToInt() * peso
        }.sum()
        return (10 - suma % 10) % 10
    }
}
