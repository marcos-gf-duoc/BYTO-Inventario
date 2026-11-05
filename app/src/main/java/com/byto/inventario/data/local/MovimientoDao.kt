package com.byto.inventario.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.byto.inventario.data.model.TipoMovimiento
import java.time.Instant

/** El historial solo crece: los movimientos se insertan y nunca se editan ni se borran. */
@Dao
interface MovimientoDao {

    /**
     * Historial filtrado (endpoint 8). Cada condición "(:x IS NULL OR ...)" se cumple
     * sola cuando el filtro viene en null, así una misma consulta sirve para cualquier
     * combinación de filtros. Las fechas son inclusivas.
     *
     * Room convierte el enum [tipo] a su nombre y los Instant a milisegundos
     * (Conversores) antes de enviarlos a SQLite.
     *
     * Ante fechas iguales se usa el orden de inserción (rowid), como en el repositorio falso.
     */
    @Query(
        """
        SELECT * FROM movimientos
        WHERE (:ubicacionId IS NULL OR ubicacionId = :ubicacionId)
          AND (:operadorId IS NULL OR operadorId = :operadorId)
          AND (:tipo IS NULL OR tipo = :tipo)
          AND (:desde IS NULL OR fechaUtc >= :desde)
          AND (:hasta IS NULL OR fechaUtc <= :hasta)
        ORDER BY fechaUtc DESC, rowid ASC
        """
    )
    suspend fun obtenerFiltrados(
        ubicacionId: String?,
        operadorId: String?,
        tipo: TipoMovimiento?,
        desde: Instant?,
        hasta: Instant?
    ): List<MovimientoEntity>

    /** Sirve para numerar el siguiente movimiento (MOV-0007, MOV-0008…). */
    @Query("SELECT COUNT(*) FROM movimientos")
    suspend fun contar(): Int

    @Insert
    suspend fun insertar(movimiento: MovimientoEntity)

    @Insert
    suspend fun insertarTodos(movimientos: List<MovimientoEntity>)
}
