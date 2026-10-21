package com.byto.inventario.ui.screens.sinconexion

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byto.inventario.R
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.ui.theme.BYTOTheme

/** P16 Sin conexión. [alReintentar] vuelve a la pantalla anterior, que repite su carga. */
@Composable
fun PantallaSinConexion(
    alReintentar: () -> Unit,
    viewModel: SinConexionViewModel = viewModel(factory = FabricaViewModels.Factory)
) {
    val cortada by viewModel.conexionSimuladaCortada.collectAsState()
    ContenidoSinConexion(
        conexionSimuladaCortada = cortada,
        alReintentar = alReintentar,
        alMantenerTitulo = viewModel::alternarConexionSimulada
    )
}

@Composable
private fun ContenidoSinConexion(
    conexionSimuladaCortada: Boolean,
    alReintentar: () -> Unit,
    alMantenerTitulo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Decorativo: el título dice lo mismo en texto.
        Icon(
            painter = painterResource(R.drawable.ic_sin_conexion),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = stringResource(R.string.titulo_p16),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            // Interruptor oculto de la demo, igual que en el login.
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onLongPress = { alMantenerTitulo() })
            }
        )
        Text(
            text = stringResource(R.string.sin_conexion_mensaje),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        if (conexionSimuladaCortada) {
            Text(
                text = stringResource(R.string.demo_sin_conexion_activa),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
        Button(
            onClick = alReintentar,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text(stringResource(R.string.sin_conexion_reintentar))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContenidoSinConexionPreview() {
    BYTOTheme {
        ContenidoSinConexion(conexionSimuladaCortada = true, alReintentar = {}, alMantenerTitulo = {})
    }
}
