package com.byto.inventario.domain

import com.byto.inventario.data.model.Ubicacion

/**
 * Regiones de BYTO, según el formato de ID Tipo-Región-Número (docs/decisiones_E1.md):
 * ST = Santiago, LS = La Serena, CC = Concepción. Casa Matriz ("CM") no tiene región.
 */
object Regiones {

    private val NOMBRES = mapOf(
        "ST" to "Santiago",
        "LS" to "La Serena",
        "CC" to "Concepción"
    )

    /** "T-ST-1" → "Santiago". Devuelve null si el ID no sigue el formato (Casa Matriz). */
    fun nombreDe(ubicacionId: String): String? = ubicacionId.split("-").getOrNull(1)?.let { NOMBRES[it] }

    /**
     * Nombre sin la región, para listas ya agrupadas por región (mockup P14):
     * "BYTO Santiago Centro" → "Centro"; "Bodega Regional Santiago" → "Bodega Regional".
     */
    fun nombreCorto(ubicacion: Ubicacion): String {
        val region = nombreDe(ubicacion.id) ?: return ubicacion.nombre
        return ubicacion.nombre.removePrefix("BYTO $region ").removeSuffix(" $region")
    }
}
