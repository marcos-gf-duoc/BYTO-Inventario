package com.byto.inventario.ui.components

import androidx.compose.ui.graphics.Color
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.ui.theme.EstadoBajoFondo
import com.byto.inventario.ui.theme.EstadoBajoTexto
import com.byto.inventario.ui.theme.EstadoCriticoFondo
import com.byto.inventario.ui.theme.EstadoCriticoTexto
import com.byto.inventario.ui.theme.EstadoNormalFondo
import com.byto.inventario.ui.theme.EstadoNormalTexto
import com.byto.inventario.ui.theme.EstadoSinStockFondo
import com.byto.inventario.ui.theme.EstadoSinStockTexto

/** Par de colores (texto y fondo) de un estado del stock. */
data class ColoresEstado(val texto: Color, val fondo: Color)

/**
 * Colores de cada estado para tarjetas grandes (por ejemplo, las de P03).
 * Siempre se acompañan del nombre del estado en texto.
 */
fun coloresDe(estado: EstadoStock): ColoresEstado = when (estado) {
    EstadoStock.NORMAL -> ColoresEstado(EstadoNormalTexto, EstadoNormalFondo)
    EstadoStock.BAJO -> ColoresEstado(EstadoBajoTexto, EstadoBajoFondo)
    EstadoStock.CRITICO -> ColoresEstado(EstadoCriticoTexto, EstadoCriticoFondo)
    EstadoStock.SIN_STOCK -> ColoresEstado(EstadoSinStockTexto, EstadoSinStockFondo)
}
