package com.byto.inventario.ui.screens.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byto.inventario.R
import com.byto.inventario.data.model.Perfil
import com.byto.inventario.data.model.Usuario
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.ui.components.IndicadorCarga
import com.byto.inventario.ui.components.textoRes
import com.byto.inventario.ui.theme.BYTOTheme

/**
 * P01 Login. Esta función conecta el ViewModel con la navegación;
 * el dibujo está en [ContenidoLogin], que solo recibe datos (y así tiene vista previa).
 */
@Composable
fun PantallaLogin(
    alIngresar: () -> Unit,
    alSinConexion: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = FabricaViewModels.Factory)
) {
    val estado by viewModel.estado.collectAsState()

    // LaunchedEffect(Unit) corre cada vez que la pantalla aparece, incluso al volver de P16.
    LaunchedEffect(Unit) { viewModel.reintentarSiHaceFalta() }

    // Corre cuando cambia irASinConexion: navega una sola vez y avisa al ViewModel.
    LaunchedEffect(estado.irASinConexion) {
        if (estado.irASinConexion) {
            viewModel.navegacionSinConexionRealizada()
            alSinConexion()
        }
    }

    ContenidoLogin(
        estado = estado,
        alSeleccionar = viewModel::seleccionar,
        alIngresar = { if (viewModel.ingresar()) alIngresar() },
        alMantenerTitulo = viewModel::alternarConexionSimulada
    )
}

@Composable
private fun ContenidoLogin(
    estado: EstadoLogin,
    alSeleccionar: (Usuario) -> Unit,
    alIngresar: () -> Unit,
    alMantenerTitulo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Encabezado centrado con la marca, como en el mockup.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                // Interruptor oculto de la demo: mantener presionado corta o restablece la conexión simulada.
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { alMantenerTitulo() })
                }
            )
            Text(
                text = stringResource(R.string.login_subtitulo),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = stringResource(R.string.login_titulo),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = stringResource(R.string.login_ayuda),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (estado.conexionSimuladaCortada) {
            Text(
                text = stringResource(R.string.demo_sin_conexion_activa),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (estado.cargando) {
            IndicadorCarga(Modifier.weight(1f))
        } else {
            // weight(1f): la lista ocupa el espacio libre y el botón queda siempre visible abajo.
            // selectableGroup: TalkBack entiende que es un grupo de opciones de las que se elige una.
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(estado.usuarios, key = { it.id }) { usuario ->
                    TarjetaUsuario(
                        usuario = usuario,
                        seleccionado = usuario.id == estado.seleccionadoId,
                        alSeleccionar = { alSeleccionar(usuario) }
                    )
                }
            }
        }

        Button(
            onClick = alIngresar,
            enabled = estado.seleccionadoId != null,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text(stringResource(R.string.login_ingresar))
        }
    }
}

/**
 * Tarjeta seleccionable. La selección se ve con el botón de opción y el borde,
 * no solo con color, y TalkBack la anuncia como "seleccionado".
 */
@Composable
private fun TarjetaUsuario(usuario: Usuario, seleccionado: Boolean, alSeleccionar: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = seleccionado, onClick = alSeleccionar, role = Role.RadioButton),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionado) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (seleccionado) 0.dp else 1.dp),
        border = if (seleccionado) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // onClick = null: el clic lo maneja la tarjeta completa, que es un objetivo táctil más grande.
            RadioButton(selected = seleccionado, onClick = null)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(text = usuario.etiqueta, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(
                        R.string.login_id_perfil, usuario.id, stringResource(usuario.perfil.textoRes())
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Decorativo: el texto ya identifica al usuario.
            Icon(
                painterResource(R.drawable.ic_persona),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContenidoLoginPreview() {
    BYTOTheme {
        ContenidoLogin(
            estado = EstadoLogin(
                cargando = false,
                usuarios = listOf(
                    Usuario("OP-001", "Operario Santiago", Perfil.OPERADOR, listOf("T-ST-1")),
                    Usuario("GE-001", "Gerente BYTO", Perfil.GERENTE, emptyList())
                ),
                seleccionadoId = "OP-001"
            ),
            alSeleccionar = {},
            alIngresar = {},
            alMantenerTitulo = {}
        )
    }
}
