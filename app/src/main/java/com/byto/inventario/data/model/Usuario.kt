package com.byto.inventario.data.model

/**
 * Usuario simulado (login sin contraseña, se elige de una lista).
 *
 * @param id ID anonimizado, por ejemplo "OP-001". No hay nombres reales.
 * @param etiqueta texto que se muestra en la lista del login.
 * @param ubicacionesIds ubicaciones donde puede trabajar (registrar o gestionar).
 * @param ubicacionesConsultaIds ubicaciones que solo puede consultar (caso del Encargado).
 */
data class Usuario(
    val id: String,
    val etiqueta: String,
    val perfil: Perfil,
    val ubicacionesIds: List<String>,
    val ubicacionesConsultaIds: List<String> = emptyList()
)
