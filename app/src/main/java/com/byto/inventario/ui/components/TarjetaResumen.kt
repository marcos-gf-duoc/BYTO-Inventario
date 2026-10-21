package com.byto.inventario.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Tarjeta con un número grande y, debajo, lo que significa (por ejemplo "7 / Alertas abiertas").
 *
 * mergeDescendants: TalkBack lee la tarjeta completa de una vez ("1, Bajo")
 * en vez de detenerse en cada texto.
 *
 * @param sufijo texto más pequeño junto al número, por ejemplo "de 12".
 * @param colorValor color del número; por defecto, el del texto de la tarjeta.
 * @param colorFondo y [colorContenido]: para tarjetas de estado (Bajo, Crítico…). Por defecto, blanca.
 * @param alTocar si no es null, la tarjeta se puede tocar (Material la anuncia como botón).
 */
@Composable
fun TarjetaResumen(
    valor: String,
    descripcion: String,
    modifier: Modifier = Modifier,
    sufijo: String? = null,
    colorValor: Color = Color.Unspecified,
    colorFondo: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    colorContenido: Color = MaterialTheme.colorScheme.onSurface,
    alTocar: (() -> Unit)? = null
) {
    val colores = CardDefaults.cardColors(containerColor = colorFondo, contentColor = colorContenido)
    val elevacion = CardDefaults.cardElevation(defaultElevation = 1.dp)
    val modificador = modifier.semantics(mergeDescendants = true) {}
    // El contenido se define una vez y se usa en las dos variantes de Card.
    val contenido: @Composable () -> Unit = {
        Column(
            // Margen y texto contenidos: en tarjetas angostas, "Reposiciones" no debe cortarse a mitad de palabra.
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = valor, style = MaterialTheme.typography.displaySmall, color = colorValor)
                if (sufijo != null) {
                    Text(
                        text = sufijo,
                        style = MaterialTheme.typography.titleMedium,
                        color = colorValor,
                        modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
                    )
                }
            }
            Text(text = descripcion, style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (alTocar != null) {
        Card(onClick = alTocar, modifier = modificador, colors = colores, elevation = elevacion) { contenido() }
    } else {
        Card(modifier = modificador, colors = colores, elevation = elevacion) { contenido() }
    }
}
