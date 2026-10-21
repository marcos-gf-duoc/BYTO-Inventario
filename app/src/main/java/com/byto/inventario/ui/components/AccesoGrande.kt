package com.byto.inventario.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Datos de un acceso: qué dice, qué ícono lleva y qué hace al tocarlo. */
data class Acceso(val texto: String, @DrawableRes val icono: Int, val alTocar: () -> Unit)

/**
 * Botón grande de acceso rápido: tarjeta blanca con ícono morado y texto oscuro
 * (mínimo 72 dp de alto, por sobre los 48 dp exigidos). El ícono es decorativo
 * porque el texto ya dice adónde lleva.
 *
 * @param horizontal true = ícono a la izquierda del texto (para el botón que ocupa todo el ancho).
 */
@Composable
fun AccesoGrande(acceso: Acceso, modifier: Modifier = Modifier, horizontal: Boolean = false) {
    Card(
        onClick = acceso.alTocar,
        modifier = modifier.heightIn(min = 72.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        val icono: @Composable () -> Unit = {
            Icon(
                painterResource(acceso.icono),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        val texto: @Composable () -> Unit = {
            Text(
                text = acceso.texto,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
        if (horizontal) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icono()
                texto()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                icono()
                texto()
            }
        }
    }
}

/**
 * Cuadrícula de 2 columnas con los accesos. Cada fila usa IntrinsicSize.Min para que
 * sus dos botones tengan la misma altura aunque un texto ocupe más líneas.
 * Si queda un acceso solo al final, ocupa todo el ancho en formato horizontal.
 */
@Composable
fun CuadriculaAccesos(accesos: List<Acceso>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // chunked(2) agrupa la lista de a dos: [[a, b], [c]].
        accesos.chunked(2).forEach { fila ->
            if (fila.size == 1) {
                AccesoGrande(fila.first(), Modifier.fillMaxWidth(), horizontal = true)
            } else {
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    fila.forEach { acceso ->
                        AccesoGrande(acceso, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}
