package com.byto.inventario.data.local

import androidx.room.TypeConverter
import java.time.Instant

/**
 * SQLite solo guarda números, texto y bytes. Room usa estas funciones para
 * convertir un Instant (fecha UTC) en un número al guardar, y al revés al leer.
 *
 * Se guardan los milisegundos desde 1970 en UTC: así ordenar por fecha en SQL
 * es ordenar números, y la zona horaria solo se aplica al mostrarla.
 */
class Conversores {

    @TypeConverter
    fun instanteDesdeMilisegundos(valor: Long?): Instant? = valor?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun instanteAMilisegundos(instante: Instant?): Long? = instante?.toEpochMilli()
}
