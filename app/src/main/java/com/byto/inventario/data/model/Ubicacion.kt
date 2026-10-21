package com.byto.inventario.data.model

/**
 * Tienda, bodega o edificio administrativo.
 *
 * @param id formato Tipo-Región-Número, por ejemplo "T-ST-1" (o "CM" para Casa Matriz).
 * @param dependeDeId ubicación de la que depende; null solo para Casa Matriz.
 * @param activa false = deshabilitada por el Gerente (por ejemplo, T-CC-3).
 */
data class Ubicacion(
    val id: String,
    val nombre: String,
    val tipo: TipoUbicacion,
    val dependeDeId: String?,
    val activa: Boolean
)
