package com.byto.inventario.domain

import java.util.Locale

/** Motivos por los que un campo del login (P01) no es válido. */
enum class ErrorLogin {
    USUARIO_OBLIGATORIO,

    /** El repositorio no conoce ese ID. */
    USUARIO_NO_ENCONTRADO,
    PIN_OBLIGATORIO,

    /** Tiene menos de 6 dígitos. */
    PIN_INCOMPLETO,

    /** El repositorio rechazó el PIN. */
    PIN_INCORRECTO
}

/** Error de cada campo; null = el campo está bien. */
data class ErroresLogin(
    val usuario: ErrorLogin? = null,
    val pin: ErrorLogin? = null
) {
    val hayErrores: Boolean get() = usuario != null || pin != null
}

/**
 * Validación pura del formulario de login (sin Android, se prueba con un test simple).
 *
 * Aquí solo se revisa el formato de lo escrito. Si el usuario existe y si el PIN es el
 * suyo lo decide el repositorio (en el futuro, el servidor): [desdeRechazo] traduce su
 * respuesta al campo que hay que corregir.
 */
object ValidacionLogin {

    const val LARGO_PIN = 6

    /** Deja solo dígitos y como máximo 6: el teclado numérico podría dejar pasar otros caracteres. */
    fun limpiarPin(texto: String): String = texto.filter { it in '0'..'9' }.take(LARGO_PIN)

    /** " op-001 " → "OP-001". Locale.ROOT: mismas mayúsculas en cualquier idioma del teléfono. */
    fun normalizarId(texto: String): String = texto.trim().uppercase(Locale.ROOT)

    /** Revisa cada campo por separado, para mostrar el error junto al que corresponde. */
    fun validar(usuarioId: String, pin: String): ErroresLogin = ErroresLogin(
        usuario = if (usuarioId.isBlank()) ErrorLogin.USUARIO_OBLIGATORIO else null,
        pin = when {
            pin.isEmpty() -> ErrorLogin.PIN_OBLIGATORIO
            pin.length != LARGO_PIN || !pin.all { it in '0'..'9' } -> ErrorLogin.PIN_INCOMPLETO
            else -> null
        }
    )

    /**
     * Convierte el código de rechazo del repositorio (campo "codigo" de la API) en el
     * error del campo correspondiente. Devuelve null si el código no es de un campo.
     */
    fun desdeRechazo(codigo: String): ErroresLogin? = when (codigo) {
        "NO_ENCONTRADO" -> ErroresLogin(usuario = ErrorLogin.USUARIO_NO_ENCONTRADO)
        "PIN_INCORRECTO" -> ErroresLogin(pin = ErrorLogin.PIN_INCORRECTO)
        else -> null
    }
}
