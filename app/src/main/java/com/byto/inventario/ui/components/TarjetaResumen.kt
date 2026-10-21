package com.byto.inventario.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Tarjeta con un número grande y, debajo, lo que significa.
 * [descripcion] es un "slot": quien la usa decide qué dibujar (un texto, un chip de estado…).
 *
 * mergeDescendants: TalkBack lee la tarjeta completa de una vez ("3, Bajo")
 * en vez de detenerse en cada texto.
 */
@Composable
fun TarjetaResumen(
    valor: String,
    modifier: Modifier = Modifier,
    descripcion: @Composable () -> Unit
) {
    Card(
        modifier = modifier.semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = valor, style = MaterialTheme.typography.headlineMedium)
            descripcion()
        }
    }
}

/** Atajo para el caso común: la descripción es un texto. */
@Composable
fun TarjetaResumen(valor: String, descripcion: String, modifier: Modifier = Modifier) {
    TarjetaResumen(valor = valor, modifier = modifier) {
        Text(text = descripcion, style = MaterialTheme.typography.bodyMedium)
    }
}
