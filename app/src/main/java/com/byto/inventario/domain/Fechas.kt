package com.byto.inventario.domain

import java.time.Instant
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

    /** Instante UTC como texto en hora de Chile, por ejemplo 18:40Z → "16-10-2026 15:40". */
    fun fechaHoraChile(instante: Instant): String = FORMATO_FECHA_HORA.format(instante)

    /**
     * Primer y último instante del día de hoy en Chile, para filtrar movimientos.
     * Ambos son inclusivos, igual que [com.byto.inventario.data.model.FiltroMovimientos].
     * @param ahora se puede pasar otro instante para probar la función.
     */
    fun rangoDeHoyEnChile(ahora: Instant = Instant.now()): Pair<Instant, Instant> {
        // atZone(): el mismo instante visto con la hora de Chile. (LocalDate.ofInstant no existe en minSdk 26.)
        val hoy = ahora.atZone(ZONA_CHILE).toLocalDate()
        val desde = hoy.atStartOfDay(ZONA_CHILE).toInstant()
        val hasta = hoy.plusDays(1).atStartOfDay(ZONA_CHILE).toInstant().minusNanos(1)
        return desde to hasta
    }
}
