package com.byto.inventario.data.model

/**
 * Usuario simulado.
 *
 * @param id ID anonimizado, por ejemplo "OP-001". No hay nombres reales.
 * @param etiqueta texto que se muestra en la lista del login.
 * @param ubicacionesIds ubicaciones donde puede trabajar (registrar o gestionar).
 * @param ubicacionesConsultaIds ubicaciones que solo puede consultar (caso del Encargado).
 * @param password contraseña de 6 dígitos para autenticación.
 */
data class Usuario(
    val id: String,
    val etiqueta: String,
    val perfil: Perfil,
    val ubicacionesIds: List<String>,
    val ubicacionesConsultaIds: List<String> = emptyList(),
    val password: String = "123456"
)
