package com.byto.inventario.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Base de datos local de BYTO (un archivo SQLite dentro de la app, "byto.db").
 * Room genera la implementación real a partir de esta clase abstracta.
 *
 * - entities: las tablas.
 * - version: se sube cada vez que cambia la estructura de las tablas.
 * - exportSchema = false: no se guarda un historial del esquema (no hay migraciones aún).
 */
@Database(
    entities = [
        UbicacionEntity::class,
        ProductoEntity::class,
        ExistenciaEntity::class,
        MovimientoEntity::class,
        AlertaEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Conversores::class)
abstract class BytoDatabase : RoomDatabase() {

    abstract fun ubicacionDao(): UbicacionDao
    abstract fun productoDao(): ProductoDao
    abstract fun existenciaDao(): ExistenciaDao
    abstract fun movimientoDao(): MovimientoDao
    abstract fun alertaDao(): AlertaDao

    companion object {
        /**
         * Abrir la base es costoso: se llama una sola vez, desde AppContainer.
         * Se usa applicationContext para no retener una Activity en memoria.
         */
        fun crear(context: Context): BytoDatabase =
            Room.databaseBuilder(context.applicationContext, BytoDatabase::class.java, "byto.db")
                .build()
    }
}
