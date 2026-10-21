package com.byto.inventario.data.model

import java.time.Instant

/**
 * Aviso sobre un producto en una ubicación. Algunos campos solo aplican a un tipo:
 *
 * @param nivel solo STOCK_BAJO: BAJO, CRITICO o SIN_STOCK.
 * @param diferencia solo DESCALCE: contado − sistema (por ejemplo −4).
 * @param movimientoId movimiento que originó la alerta; null si vino de la carga inicial
 * o fue declarada a mano (REPOSICION).
 * @param declaradaPorId solo REPOSICION: ID del Encargado que la declaró.
 * @param fechaUtc cuándo se abrió, en UTC.
 */
data class Alerta(
    val id: String,
    val tipo: TipoAlerta,
    val ubicacionId: String,
    val codigoInterno: String,
    val estado: EstadoAlerta,
    val fechaUtc: Instant,
    val nivel: EstadoStock? = null,
    val diferencia: Int? = null,
    val movimientoId: String? = null,
    val declaradaPorId: String? = null
)
