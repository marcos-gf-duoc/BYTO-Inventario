package com.byto.inventario.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ExistenciaDao {

    @Query("SELECT * FROM existencias ORDER BY rowid")
    suspend fun obtenerTodas(): List<ExistenciaEntity>

    @Query("SELECT * FROM existencias WHERE ubicacionId = :ubicacionId ORDER BY rowid")
    suspend fun obtenerDeUbicacion(ubicacionId: String): List<ExistenciaEntity>

    /** Devuelve null si ese producto no tiene fila en esa ubicación. */
    @Query(
        "SELECT * FROM existencias WHERE ubicacionId = :ubicacionId AND codigoInterno = :codigoInterno"
    )
    suspend fun obtener(ubicacionId: String, codigoInterno: String): ExistenciaEntity?

    @Insert
    suspend fun insertarTodas(existencias: List<ExistenciaEntity>)

    /** Busca la fila por su clave compuesta (ubicacionId + codigoInterno). */
    @Update
    suspend fun actualizar(existencia: ExistenciaEntity)
}
