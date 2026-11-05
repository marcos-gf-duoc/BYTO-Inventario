package com.byto.inventario.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.byto.inventario.data.model.EstadoAlerta
import com.byto.inventario.data.model.TipoAlerta

@Dao
interface AlertaDao {

    /** Alertas filtradas (endpoint 9): un filtro en null significa "todas". */
    @Query(
        """
        SELECT * FROM alertas
        WHERE (:ubicacionId IS NULL OR ubicacionId = :ubicacionId)
          AND (:tipo IS NULL OR tipo = :tipo)
          AND (:estado IS NULL OR estado = :estado)
        ORDER BY fechaUtc DESC, rowid ASC
        """
    )
    suspend fun obtenerFiltradas(
        ubicacionId: String?,
        tipo: TipoAlerta?,
        estado: EstadoAlerta?
    ): List<AlertaEntity>

    @Query("SELECT * FROM alertas WHERE id = :id")
    suspend fun obtenerPorId(id: String): AlertaEntity?

    /**
     * La alerta de un tipo y estado para un producto en una ubicación, o null.
     * El repositorio la usa con estado ABIERTA: de cada tipo hay como máximo una abierta.
     */
    @Query(
        """
        SELECT * FROM alertas
        WHERE tipo = :tipo AND estado = :estado
          AND ubicacionId = :ubicacionId AND codigoInterno = :codigoInterno
        LIMIT 1
        """
    )
    suspend fun buscar(
        tipo: TipoAlerta,
        estado: EstadoAlerta,
        ubicacionId: String,
        codigoInterno: String
    ): AlertaEntity?

    /** Sirve para numerar la siguiente alerta (ALE-0008, ALE-0009…). */
    @Query("SELECT COUNT(*) FROM alertas")
    suspend fun contar(): Int

    @Insert
    suspend fun insertar(alerta: AlertaEntity)

    @Insert
    suspend fun insertarTodas(alertas: List<AlertaEntity>)

    @Update
    suspend fun actualizar(alerta: AlertaEntity)
}
