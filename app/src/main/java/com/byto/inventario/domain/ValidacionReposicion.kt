package com.byto.inventario.domain

/** Motivos por los que un campo del formulario de reposición (P12) no es válido. */
enum class ErrorReposicion {
    TIENDA_OBLIGATORIA,
    PRODUCTO_OBLIGATORIO,

    /** Ya hay una reposición abierta de ese producto en esa tienda (una sola a la vez). */
    REPOSICION_YA_ABIERTA,
    OBSERVACION_MUY_LARGA
}

/** Error de cada campo; null = el campo está bien. */
data class ErroresReposicion(
    val tienda: ErrorReposicion? = null,
    val producto: ErrorReposicion? = null,
    val observacion: ErrorReposicion? = null
) {
    val hayErrores: Boolean get() = tienda != null || producto != null || observacion != null
}

/**
 * Validación pura del formulario P12 (sin Android, se prueba con un test simple).
 * Revisa cada campo por separado, para que la pantalla muestre el error junto a su campo.
 */
object ValidacionReposicion {

    /**
     * @param abiertas pares (tienda, código interno) que ya tienen una reposición abierta.
     */
    fun validar(
        tiendaId: String?,
        codigoInterno: String?,
        observacion: String,
        abiertas: Set<Pair<String, String>>
    ): ErroresReposicion = ErroresReposicion(
        tienda = if (tiendaId == null) ErrorReposicion.TIENDA_OBLIGATORIA else null,
        producto = when {
            codigoInterno == null -> ErrorReposicion.PRODUCTO_OBLIGATORIO
            tiendaId != null && (tiendaId to codigoInterno) in abiertas -> ErrorReposicion.REPOSICION_YA_ABIERTA
            else -> null
        },
        observacion = if (observacion.length > ReglasInventario.MAX_CARACTERES_OBSERVACION) {
            ErrorReposicion.OBSERVACION_MUY_LARGA
        } else {
            null
        }
    )
}
