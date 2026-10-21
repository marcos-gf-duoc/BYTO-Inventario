package com.byto.inventario.data.model

import java.time.Instant

/**
 * Registro de un cambio de stock. Es inmutable: el historial nunca se edita.
 *
 * @param cantidad en AJUSTE lleva signo (−2 = se restan 2); en CONTEO es lo contado.
 * @param fechaUtc instante en UTC; se muestra en hora de Chile solo al dibujarlo.
 * @param operadorId ID anonimizado (por ejemplo "OP-001"), nunca un nombre.
 * @param motivo obligatorio solo en AJUSTE.
 * @param observacion texto libre, máximo 200 caracteres.
 */
data class Movimiento(
    val id: String,
    val tipo: TipoMovimiento,
    val ubicacionId: String,
    val codigoInterno: String,
    val cantidad: Int,
    val stockAntes: Int,
    val stockDespues: Int,
    val fechaUtc: Instant,
    val operadorId: String,
    val motivo: String? = null,
    val observacion: String? = null
)
