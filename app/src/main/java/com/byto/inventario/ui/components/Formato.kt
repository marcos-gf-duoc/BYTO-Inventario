package com.byto.inventario.ui.components

/**
 * Número con signo explícito: +3, −4 o 0. Usa el signo menos tipográfico (−),
 * más legible que el guion (-) y el que leen bien los lectores de pantalla.
 */
fun conSigno(valor: Int): String = when {
    valor > 0 -> "+$valor"
    valor < 0 -> "−${-valor}"
    else -> "0"
}
