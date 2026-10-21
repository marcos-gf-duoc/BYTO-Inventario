package com.byto.inventario.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
 * Botón grande de acceso rápido (mínimo 88 dp de alto, muy por sobre los 48 dp exigidos).
 * El ícono es decorativo porque el texto ya dice adónde lleva.
 */
@Composable
fun AccesoGrande(acceso: Acceso, modifier: Modifier = Modifier) {
    Card(
        onClick = acceso.alTocar,
        modifier = modifier.heightIn(min = 88.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            Icon(painterResource(acceso.icono), contentDescription = null, modifier = Modifier.size(32.dp))
            Text(
                text = acceso.texto,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Cuadrícula de 2 columnas con los accesos. Cada fila usa IntrinsicSize.Min para que
 * sus dos botones tengan la misma altura aunque un texto ocupe más líneas.
 */
@Composable
fun CuadriculaAccesos(accesos: List<Acceso>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // chunked(2) agrupa la lista de a dos: [[a, b], [c, d]].
        accesos.chunked(2).forEach { fila ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                fila.forEach { acceso ->
                    AccesoGrande(acceso, Modifier.weight(1f).fillMaxHeight())
                }
                // Si la fila quedó con uno solo, un espacio vacío ocupa el lugar del segundo.
                if (fila.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
