package com.byto.inventario.data.model

/**
 * Usuario simulado.
 *
 * No incluye el PIN a propósito: la pantalla y los ViewModels nunca lo reciben. La
 * verificación la hace el repositorio (InventarioRepository.autenticar), igual que la
 * hará el servidor cuando exista.
 *
 * @param id ID anonimizado, por ejemplo "OP-001". No hay nombres reales.
 * @param etiqueta texto descriptivo del usuario (por ejemplo "Operario Santiago").
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
