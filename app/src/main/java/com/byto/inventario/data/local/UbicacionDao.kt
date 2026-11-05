package com.byto.inventario.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/**
 * DAO (Data Access Object): Room genera el código de cada función a partir del SQL
 * de su anotación. Las funciones son `suspend`, así Room las ejecuta fuera del hilo
 * principal y la pantalla no se congela.
 *
 * "ORDER BY rowid" devuelve las filas en el orden en que se insertaron (el de
 * DatosSinteticos), que es el orden en que la app las muestra.
 */
@Dao
interface UbicacionDao {

    @Query("SELECT * FROM ubicaciones ORDER BY rowid")
    suspend fun obtenerTodas(): List<UbicacionEntity>

    /** Devuelve null si no existe. */
    @Query("SELECT * FROM ubicaciones WHERE id = :id")
    suspend fun obtenerPorId(id: String): UbicacionEntity?

    @Query("SELECT COUNT(*) FROM ubicaciones")
    suspend fun contar(): Int

    @Insert
    suspend fun insertarTodas(ubicaciones: List<UbicacionEntity>)

    /** @Update busca la fila por su clave primaria (id) y reemplaza sus columnas. */
    @Update
    suspend fun actualizar(ubicacion: UbicacionEntity)
}
