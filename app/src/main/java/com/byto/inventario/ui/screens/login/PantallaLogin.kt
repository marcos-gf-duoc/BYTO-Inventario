package com.byto.inventario.ui.screens.login

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byto.inventario.R
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.domain.ErrorLogin
import com.byto.inventario.domain.ErroresLogin
import com.byto.inventario.ui.components.MensajeError
import com.byto.inventario.ui.theme.BYTOTheme

/**
 * P01 Login: ID de usuario y PIN de 6 dígitos. La pantalla solo dibuja el estado y envía
 * eventos; validar y verificar las credenciales es tarea del ViewModel.
 */
@Composable
fun PantallaLogin(
    alIngresar: () -> Unit,
    alSinConexion: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = FabricaViewModels.Factory)
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(estado.sesionIniciada) {
        if (estado.sesionIniciada) {
            viewModel.navegacionInicioRealizada()
            alIngresar()
        }
    }
    LaunchedEffect(estado.irASinConexion) {
        if (estado.irASinConexion) {
            viewModel.navegacionSinConexionRealizada()
            alSinConexion()
        }
    }

    ContenidoLogin(
        estado = estado,
        alCambiarUsuarioId = viewModel::cambiarUsuarioId,
        alCambiarPin = viewModel::cambiarPin,
        alIngresar = viewModel::ingresar,
        alMantenerTitulo = viewModel::alternarConexionSimulada
    )
}

@Composable
private fun ContenidoLogin(
    estado: EstadoLogin,
    alCambiarUsuarioId: (String) -> Unit,
    alCambiarPin: (String) -> Unit,
    alIngresar: () -> Unit,
    alMantenerTitulo: () -> Unit
) {
    // El formulario se desplaza (el teclado tapa parte de la pantalla); el botón queda fijo abajo.
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Encabezado centrado con la marca.
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
                    // Interruptor oculto de la demo: mantener presionado corta la conexión simulada.
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
                text = stringResource(R.string.login_instruccion),
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

            // Cada campo lleva su error justo debajo, con ícono y texto (nunca solo color).
            Column {
                OutlinedTextField(
                    value = estado.usuarioId,
                    onValueChange = alCambiarUsuarioId,
                    label = { Text(stringResource(R.string.login_campo_usuario)) },
                    placeholder = { Text(stringResource(R.string.login_ejemplo_usuario)) },
                    singleLine = true,
                    enabled = !estado.ingresando,
                    isError = estado.errores.usuario != null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                MensajeError(textoDeError(estado.errores.usuario, estado.pin.length))
            }
            Column {
                OutlinedTextField(
                    value = estado.pin,
                    onValueChange = alCambiarPin,
                    label = { Text(stringResource(R.string.login_campo_pin)) },
                    singleLine = true,
                    enabled = !estado.ingresando,
                    isError = estado.errores.pin != null,
                    // Oculta los dígitos y abre el teclado numérico.
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    // "Listo" en el teclado hace lo mismo que el botón Ingresar.
                    keyboardActions = KeyboardActions(onDone = { alIngresar() }),
                    modifier = Modifier.fillMaxWidth()
                )
                MensajeError(textoDeError(estado.errores.pin, estado.pin.length))
            }
            MensajeError(if (estado.errorGeneral) stringResource(R.string.login_error_general) else null)

            Text(
                text = stringResource(R.string.login_ayuda),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = alIngresar,
            enabled = estado.puedeIngresar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 56.dp)
        ) {
            if (estado.ingresando) {
                CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize), strokeWidth = 2.dp)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.login_ingresando), style = MaterialTheme.typography.titleMedium)
            } else {
                Text(stringResource(R.string.login_ingresar), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** Traduce el error del dominio a un texto de strings.xml; null = sin error. */
@Composable
private fun textoDeError(error: ErrorLogin?, digitosPin: Int): String? = when (error) {
    ErrorLogin.USUARIO_OBLIGATORIO -> stringResource(R.string.login_error_usuario_obligatorio)
    ErrorLogin.USUARIO_NO_ENCONTRADO -> stringResource(R.string.login_error_usuario_no_encontrado)
    ErrorLogin.PIN_OBLIGATORIO -> stringResource(R.string.login_error_pin_obligatorio)
    ErrorLogin.PIN_INCOMPLETO -> stringResource(R.string.login_error_pin_incompleto, digitosPin)
    ErrorLogin.PIN_INCORRECTO -> stringResource(R.string.login_error_pin_incorrecto)
    null -> null
}

@Preview(showBackground = true)
@Composable
private fun ContenidoLoginPreview() {
    BYTOTheme {
        ContenidoLogin(
            estado = EstadoLogin(
                usuarioId = "OP-001",
                pin = "1234",
                errores = ErroresLogin(pin = ErrorLogin.PIN_INCOMPLETO)
            ),
            alCambiarUsuarioId = {},
            alCambiarPin = {},
            alIngresar = {},
            alMantenerTitulo = {}
        )
    }
}
