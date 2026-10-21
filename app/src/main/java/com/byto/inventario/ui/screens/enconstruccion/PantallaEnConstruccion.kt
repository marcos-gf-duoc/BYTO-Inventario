package com.byto.inventario.ui.screens.enconstruccion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.byto.inventario.R
import com.byto.inventario.ui.navigation.Pantalla
import com.byto.inventario.ui.theme.BYTOTheme

/**
 * Marcador para las pantallas que aún no se implementan.
 * No tiene ViewModel porque no tiene estado ni datos: solo muestra texto fijo.
 */
@Composable
fun PantallaEnConstruccion(pantalla: Pantalla, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Decorativo: el texto de abajo ya dice lo mismo.
        Icon(
            painter = painterResource(R.drawable.ic_en_construccion),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "${pantalla.codigo} · ${stringResource(pantalla.titulo)}",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.en_construccion),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaEnConstruccionPreview() {
    BYTOTheme { PantallaEnConstruccion(Pantalla.INVENTARIO) }
}
