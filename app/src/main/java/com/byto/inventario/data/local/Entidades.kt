package com.byto.inventario.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.byto.inventario.data.model.EstadoAlerta
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.TipoAlerta
import com.byto.inventario.data.model.TipoMovimiento
import com.byto.inventario.data.model.TipoUbicacion
import java.time.Instant

// Entidades de Room: cada clase es una tabla y cada propiedad, una columna.
// Son una copia "plana" de los modelos de data/model. Se mantienen separadas para
// que la UI no dependa de la base de datos: el repositorio convierte de una a otra
// (ver Mapeos.kt).
//
// Room guarda los enums como texto con su nombre (por ejemplo "TIENDA") sin ayuda
// extra. Instant sí necesita un conversor (ver Conversores.kt).

@Entity(tableName = "ubicaciones")
data class UbicacionEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val tipo: TipoUbicacion,
    val dependeDeId: String?,
    val activa: Boolean
)

/** El índice único sobre ean13 acelera la búsqueda por código e impide EAN repetidos. */
@Entity(tableName = "productos", indices = [Index(value = ["ean13"], unique = true)])
data class ProductoEntity(
    @PrimaryKey val codigoInterno: String,
    val ean13: String,
    val nombre: String,
    val categoria: String
)

/** Clave compuesta: hay una sola fila por producto en cada ubicación. */
@Entity(tableName = "existencias", primaryKeys = ["ubicacionId", "codigoInterno"])
data class ExistenciaEntity(
    val ubicacionId: String,
    val codigoInterno: String,
    val stock: Int,
    val stockMinimo: Int
)

@Entity(tableName = "movimientos")
data class MovimientoEntity(
    @PrimaryKey val id: String,
    val tipo: TipoMovimiento,
    val ubicacionId: String,
    val codigoInterno: String,
    val cantidad: Int,
    val stockAntes: Int,
    val stockDespues: Int,
    val fechaUtc: Instant,
    val operadorId: String,
    val motivo: String?,
    val observacion: String?
)

@Entity(tableName = "alertas")
data class AlertaEntity(
    @PrimaryKey val id: String,
    val tipo: TipoAlerta,
    val ubicacionId: String,
    val codigoInterno: String,
    val estado: EstadoAlerta,
    val fechaUtc: Instant,
    val nivel: EstadoStock?,
    val diferencia: Int?,
    val movimientoId: String?,
    val declaradaPorId: String?,
    val observacion: String?
)
