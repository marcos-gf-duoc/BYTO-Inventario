package com.byto.inventario.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Ayudas de fecha. Los datos se guardan en UTC, pero "hoy" para BYTO es el día
 * en hora de Chile: un movimiento a las 22:00 en Santiago ya es "mañana" en UTC.
 */
object Fechas {

    /** Zona horaria en que se muestran las fechas (considera el horario de verano). */
    val ZONA_CHILE: ZoneId = ZoneId.of("America/Santiago")

    // withZone: al formatear un Instant (UTC) lo convierte primero a hora de Chile.
    private val FORMATO_FECHA_HORA: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").withZone(ZONA_CHILE)

    /** Sin año, para listas donde el espacio es poco (alertas e historial de los mockups). */
    private val FORMATO_FECHA_HORA_CORTA: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd-MM HH:mm").withZone(ZONA_CHILE)

    /** Instante UTC como texto en hora de Chile, por ejemplo 18:40Z → "16-10-2026 15:40". */
    fun fechaHoraChile(instante: Instant): String = FORMATO_FECHA_HORA.format(instante)

    /** Igual que [fechaHoraChile] pero sin año, por ejemplo 12:30Z → "17-10 09:30". */
    fun fechaHoraCortaChile(instante: Instant): String = FORMATO_FECHA_HORA_CORTA.format(instante)

    /**
     * Primer y último instante del día de hoy en Chile, para filtrar movimientos.
     * Ambos son inclusivos, igual que [com.byto.inventario.data.model.FiltroMovimientos].
     * @param ahora se puede pasar otro instante para probar la función.
     */
    fun rangoDeHoyEnChile(ahora: Instant = Instant.now()): Pair<Instant, Instant> {
        // atZone(): el mismo instante visto con la hora de Chile. (LocalDate.ofInstant no existe en minSdk 26.)
        val hoy = ahora.atZone(ZONA_CHILE).toLocalDate()
        return inicioDelDia(hoy) to finDelDia(hoy)
    }

    /** Primer instante (00:00:00) de ese día en Chile, expresado en UTC. */
    fun inicioDelDia(dia: LocalDate): Instant = dia.atStartOfDay(ZONA_CHILE).toInstant()

    /** Último instante (23:59:59,999…) de ese día en Chile, expresado en UTC. */
    fun finDelDia(dia: LocalDate): Instant = dia.plusDays(1).atStartOfDay(ZONA_CHILE).toInstant().minusNanos(1)

    /**
     * Regla del filtro de fechas del historial (P13): "Desde" no puede ser posterior a "Hasta".
     * Si falta alguna de las dos, el rango es válido (queda abierto por ese lado).
     */
    fun rangoValido(desde: LocalDate?, hasta: LocalDate?): Boolean =
        desde == null || hasta == null || !desde.isAfter(hasta)

    private val FORMATO_DIA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    /** Día como texto, por ejemplo "16-10-2026". */
    fun dia(dia: LocalDate): String = FORMATO_DIA.format(dia)
}
