package com.byto.inventario.data.local

import com.byto.inventario.data.model.Alerta
import com.byto.inventario.data.model.Existencia
import com.byto.inventario.data.model.Movimiento
import com.byto.inventario.data.model.Producto
import com.byto.inventario.data.model.Ubicacion

// Conversión entre entidades de Room y modelos de la app.
// Son funciones de extensión: se llaman como si fueran métodos de la clase,
// por ejemplo `entidad.aModelo()` o `ubicacion.aEntidad()`.

fun UbicacionEntity.aModelo() = Ubicacion(id, nombre, tipo, dependeDeId, activa)

fun Ubicacion.aEntidad() = UbicacionEntity(id, nombre, tipo, dependeDeId, activa)

fun ProductoEntity.aModelo() = Producto(ean13, codigoInterno, nombre, categoria)

fun Producto.aEntidad() = ProductoEntity(codigoInterno, ean13, nombre, categoria)

fun ExistenciaEntity.aModelo() = Existencia(ubicacionId, codigoInterno, stock, stockMinimo)

fun Existencia.aEntidad() = ExistenciaEntity(ubicacionId, codigoInterno, stock, stockMinimo)

fun MovimientoEntity.aModelo() = Movimiento(
    id, tipo, ubicacionId, codigoInterno, cantidad, stockAntes, stockDespues,
    fechaUtc, operadorId, motivo, observacion
)

fun Movimiento.aEntidad() = MovimientoEntity(
    id, tipo, ubicacionId, codigoInterno, cantidad, stockAntes, stockDespues,
    fechaUtc, operadorId, motivo, observacion
)

fun AlertaEntity.aModelo() = Alerta(
    id, tipo, ubicacionId, codigoInterno, estado, fechaUtc,
    nivel, diferencia, movimientoId, declaradaPorId, observacion
)

fun Alerta.aEntidad() = AlertaEntity(
    id, tipo, ubicacionId, codigoInterno, estado, fechaUtc,
    nivel, diferencia, movimientoId, declaradaPorId, observacion
)
