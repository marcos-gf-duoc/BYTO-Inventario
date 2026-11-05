package com.byto.inventario.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** Catálogo de productos. En E2 solo se lee: el catálogo no se edita desde la app. */
@Dao
interface ProductoDao {

    @Query("SELECT * FROM productos ORDER BY rowid")
    suspend fun obtenerTodos(): List<ProductoEntity>

    /** Devuelve null si ningún producto tiene ese EAN. */
    @Query("SELECT * FROM productos WHERE ean13 = :ean")
    suspend fun obtenerPorEan(ean: String): ProductoEntity?

    @Insert
    suspend fun insertarTodos(productos: List<ProductoEntity>)
}
