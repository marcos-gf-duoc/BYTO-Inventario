package com.byto.inventario.domain

import com.byto.inventario.data.model.Producto
import java.text.Normalizer

/**
 * Búsqueda tolerante para el inventario: no distingue mayúsculas ni tildes,
 * así "cajon", "CAJÓN" y "Cajón" encuentran lo mismo.
 */
object BusquedaTexto {

    // \p{Mn} = "marcas" que se combinan con una letra, como la tilde de "ó".
    private val MARCAS_DIACRITICAS = Regex("\\p{Mn}+")

    /**
     * Pasa a minúsculas y quita tildes y diéresis.
     * NFD separa "ó" en "o" + tilde; luego se borra la tilde y queda solo "o".
     * (La "ñ" también se separa en "n" + virgulilla: "pañal" coincide con "panal",
     * algo aceptable para buscar.)
     */
    fun normalizar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(MARCAS_DIACRITICAS, "")
            .lowercase()
            .trim()

    /** true si [busqueda] aparece en el nombre o en el código interno. Vacía = coincide todo. */
    fun coincide(producto: Producto, busqueda: String): Boolean {
        val texto = normalizar(busqueda)
        if (texto.isEmpty()) return true
        return normalizar(producto.nombre).contains(texto) ||
            normalizar(producto.codigoInterno).contains(texto)
    }
}
