package com.byto.inventario.data.model

/** Totales de una ubicación con stock para el tablero del Gerente. */
data class ResumenUbicacion(
    val ubicacion: Ubicacion,
    val totalUnidades: Int,
    val productosNormal: Int,
    val productosBajo: Int,
    val productosCritico: Int,
    val productosSinStock: Int,
    val alertasAbiertas: Int
)

/** Respuesta de GET /tablero. */
data class Tablero(
    val resumenes: List<ResumenUbicacion>,
    val totalAlertasAbiertas: Int
)
