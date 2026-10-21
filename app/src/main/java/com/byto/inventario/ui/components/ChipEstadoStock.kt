package com.byto.inventario.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byto.inventario.R
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.ui.theme.EstadoBajoFondo
import com.byto.inventario.ui.theme.EstadoBajoTexto
import com.byto.inventario.ui.theme.EstadoCriticoFondo
import com.byto.inventario.ui.theme.EstadoCriticoTexto
import com.byto.inventario.ui.theme.EstadoNormalFondo
import com.byto.inventario.ui.theme.EstadoNormalTexto
import com.byto.inventario.ui.theme.EstadoSinStockFondo
import com.byto.inventario.ui.theme.EstadoSinStockTexto

/**
 * Etiqueta del estado del stock. Siempre muestra el texto ("Bajo", "Crítico"...)
 * además del color, para que se entienda aunque la persona no distinga colores.
 *
 * No es clickeable: solo informa. Por eso usa Surface y no un Chip de Material
 * (que espera una acción al tocarlo).
 */
@Composable
fun ChipEstadoStock(estado: EstadoStock, modifier: Modifier = Modifier) {
    // "when" sobre un enum obliga a cubrir todos los casos: si se agrega un estado, no compila.
    val (texto, colorTexto, colorFondo) = when (estado) {
        EstadoStock.NORMAL -> Triple(R.string.estado_normal, EstadoNormalTexto, EstadoNormalFondo)
        EstadoStock.BAJO -> Triple(R.string.estado_bajo, EstadoBajoTexto, EstadoBajoFondo)
        EstadoStock.CRITICO -> Triple(R.string.estado_critico, EstadoCriticoTexto, EstadoCriticoFondo)
        EstadoStock.SIN_STOCK -> Triple(R.string.estado_sin_stock, EstadoSinStockTexto, EstadoSinStockFondo)
    }

    Surface(
        modifier = modifier.heightIn(min = 32.dp),
        shape = RoundedCornerShape(8.dp),
        color = colorFondo,
        contentColor = colorTexto
    ) {
        Text(
            text = stringResource(texto),
            // Sin "color": Text toma el contentColor del Surface.
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
