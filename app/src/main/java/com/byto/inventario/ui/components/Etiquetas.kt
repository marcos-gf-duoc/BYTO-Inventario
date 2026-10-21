package com.byto.inventario.ui.components

import androidx.annotation.StringRes
import com.byto.inventario.R
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.Perfil
import com.byto.inventario.data.model.TipoMovimiento
import com.byto.inventario.data.model.TipoUbicacion

// Traducen los enums del dominio a textos de strings.xml.
// Así los modelos no dependen de Android y los textos siguen en un solo lugar.

@StringRes
fun Perfil.textoRes(): Int = when (this) {
    Perfil.OPERADOR -> R.string.perfil_operador
    Perfil.ENCARGADO_BODEGA -> R.string.perfil_encargado_bodega
    Perfil.GERENTE -> R.string.perfil_gerente
}

@StringRes
fun EstadoStock.textoRes(): Int = when (this) {
    EstadoStock.NORMAL -> R.string.estado_normal
    EstadoStock.BAJO -> R.string.estado_bajo
    EstadoStock.CRITICO -> R.string.estado_critico
    EstadoStock.SIN_STOCK -> R.string.estado_sin_stock
}

@StringRes
fun TipoMovimiento.textoRes(): Int = when (this) {
    TipoMovimiento.ENTRADA -> R.string.movimiento_entrada
    TipoMovimiento.SALIDA -> R.string.movimiento_salida
    TipoMovimiento.AJUSTE -> R.string.movimiento_ajuste
    TipoMovimiento.CONTEO -> R.string.movimiento_conteo
}

@StringRes
fun TipoUbicacion.textoRes(): Int = when (this) {
    TipoUbicacion.TIENDA -> R.string.tipo_tienda
    TipoUbicacion.BODEGA -> R.string.tipo_bodega
    TipoUbicacion.ADMINISTRATIVA -> R.string.tipo_administrativa
}
