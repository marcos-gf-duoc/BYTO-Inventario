package com.byto.inventario.ui.screens.login

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byto.inventario.R
import com.byto.inventario.data.model.Perfil
import com.byto.inventario.data.model.Usuario
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.ui.components.IndicadorCarga
import com.byto.inventario.ui.theme.BYTOTheme

/**
 * P01 Login profesional con credenciales (ID de Usuario y Contraseña de 6 dígitos).
 */
@Composable
fun PantallaLogin(
    alIngresar: () -> Unit,
    alSinConexion: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = FabricaViewModels.Factory)
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(Unit) { viewModel.reintentarSiHaceFalta() }

    LaunchedEffect(estado.irASinConexion) {
        if (estado.irASinConexion) {
            viewModel.navegacionSinConexionRealizada()
            alSinConexion()
        }
    }

    ContenidoLogin(
        estado = estado,
        alCambiarUsuarioId = viewModel::actualizarUsuarioId,
        alCambiarPassword = viewModel::actualizarPassword,
        alIngresar = { if (viewModel.ingresar()) alIngresar() },
        alMantenerTitulo = viewModel::alternarConexionSimulada
    )
}

@Composable
private fun ContenidoLogin(
    estado: EstadoLogin,
    alCambiarUsuarioId: (String) -> Unit,
    alCambiarPassword: (String) -> Unit,
    alIngresar: () -> Unit,
    alMantenerTitulo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Encabezado centrado con la marca
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
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
            text = "Iniciar sesión",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Introduce tu ID de usuario y contraseña de 6 dígitos",
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = estado.usuarioIdInput,
                    onValueChange = alCambiarUsuarioId,
                    label = { Text("ID de Usuario (ej. OP-001)") },
                    placeholder = { Text("OP-001") },
                    singleLine = true,
                    isError = estado.errorUsuario,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = estado.passwordInput,
                    onValueChange = alCambiarPassword,
                    label = { Text("Contraseña (6 dígitos)") },
                    placeholder = { Text("123456") },
                    singleLine = true,
                    isError = estado.errorPassword,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )

                if (estado.mensajeError != null) {
                    Text(
                        text = estado.mensajeError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Ayuda con credenciales para demostración / profesor
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "💡 Credenciales de demostración:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• Operario Santiago: OP-001 (PIN: 123456)\n• Operario La Serena: OP-002 (PIN: 123457)\n• Encargado Bodega: EB-001 (PIN: 123458)\n• Gerente BYTO: GE-001 (PIN: 123459)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Button(
            onClick = alIngresar,
            enabled = estado.usuarioIdInput.isNotBlank() && estado.passwordInput.length == 6 && !estado.cargando,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text(stringResource(R.string.login_ingresar))
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
                    Usuario("OP-001", "Operario Santiago", Perfil.OPERADOR, listOf("T-ST-1"), password = "123456")
                ),
                usuarioIdInput = "OP-001",
                passwordInput = "123456"
            ),
            alCambiarUsuarioId = {},
            alCambiarPassword = {},
            alIngresar = {},
            alMantenerTitulo = {}
        )
    }
}
