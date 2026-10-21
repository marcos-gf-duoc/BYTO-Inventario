package com.byto.inventario.data.model

/**
 * Cuánto hay de un producto en una ubicación (una "fila" del inventario).
 *
 * @param stockMinimo atributo del Caso §3.2. En E1 vale 10 para todos (igual al umbral
 * de "Normal"); se guarda por existencia para poder personalizarlo más adelante.
 */
data class Existencia(
    val ubicacionId: String,
    val codigoInterno: String,
    val stock: Int,
    val stockMinimo: Int = 10
)
