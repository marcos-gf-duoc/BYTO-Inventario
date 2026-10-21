package com.byto.inventario.data.model

/**
 * Una fila del inventario lista para mostrar: la existencia junto a su producto
 * y su estado ya calculado. Así la pantalla no tiene que cruzar listas ni
 * conocer los umbrales.
 */
data class ExistenciaDetalle(
    val existencia: Existencia,
    val producto: Producto,
    val estado: EstadoStock
)
