package com.byto.inventario.data.model

/**
 * Producto del catálogo (igual en todas las ubicaciones).
 *
 * @param ean13 código de barras ficticio de 13 dígitos (prefijo interno 2).
 * @param codigoInterno código corto que usa BYTO, por ejemplo "TE-01". Es la clave del producto.
 */
data class Producto(
    val ean13: String,
    val codigoInterno: String,
    val nombre: String,
    val categoria: String
)
