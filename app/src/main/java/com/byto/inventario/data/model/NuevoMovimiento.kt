package com.byto.inventario.data.model

import java.time.Instant

/**
 * Lo que la app envía para registrar un movimiento (cuerpo de POST /movimientos).
 * No trae id, fecha ni stock antes/después: eso lo asigna el servidor.
 *
 * @param operadorId en E1 lo pone la app desde la sesión. En E2 el servidor lo tomará
 * de la sesión o del token, y este campo desaparecerá (ver docs/arquitectura_API.md).
 */
data class NuevoMovimiento(
    val tipo: TipoMovimiento,
    val ubicacionId: String,
    val codigoInterno: String,
    val cantidad: Int,
    val operadorId: String,
    val motivo: String? = null,
    val observacion: String? = null
)

/** Respuesta de POST /movimientos: el movimiento guardado y las alertas que abrió. */
data class ResultadoMovimiento(
    val movimiento: Movimiento,
    val alertasGeneradas: List<Alerta>
)

/**
 * Filtros del historial (GET /movimientos). Un campo en null significa "sin filtrar".
 * [desde] y [hasta] son inclusivos.
 */
data class FiltroMovimientos(
    val ubicacionId: String? = null,
    val usuarioId: String? = null,
    val tipo: TipoMovimiento? = null,
    val desde: Instant? = null,
    val hasta: Instant? = null
)
