package com.byto.inventario.ui.screens.buscar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byto.inventario.R
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.Existencia
import com.byto.inventario.data.model.ExistenciaDetalle
import com.byto.inventario.data.model.Producto
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.domain.ErrorEan
import com.byto.inventario.ui.components.ChipEstadoStock
import com.byto.inventario.ui.components.IndicadorCarga
import com.byto.inventario.ui.theme.BYTOTheme

/**
 * P08 Buscar por código. [alVerDetalle] y [alRegistrar] reciben el código interno del producto.
 */
@Composable
fun PantallaBuscarCodigo(
    alVerDetalle: (String) -> Unit,
    alRegistrar: (String) -> Unit,
    alSinConexion: () -> Unit,
    alSinSesion: () -> Unit,
    viewModel: BuscarCodigoViewModel = viewModel(factory = FabricaViewModels.Factory)
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(Unit) { viewModel.alMostrarse() }
    LaunchedEffect(estado.irASinConexion) {
        if (estado.irASinConexion) {
            viewModel.navegacionSinConexionRealizada()
            alSinConexion()
        }
    }
    LaunchedEffect(estado.sinSesion) {
        if (estado.sinSesion) alSinSesion()
    }

    ContenidoBuscarCodigo(
        estado = estado,
        alCambiarCodigo = viewModel::cambiarCodigo,
        alBuscar = viewModel::buscar,
        alVerDetalle = alVerDetalle,
        alRegistrar = alRegistrar
    )
}

@Composable
private fun ContenidoBuscarCodigo(
    estado: EstadoBuscarCodigo,
    alCambiarCodigo: (String) -> Unit,
    alBuscar: () -> Unit,
    alVerDetalle: (String) -> Unit,
    alRegistrar: (String) -> Unit
) {
    val foco = LocalFocusManager.current
    // Cierra el teclado y busca: lo usan el botón y la tecla "Buscar" del teclado.
    val buscar = {
        foco.clearFocus()
        alBuscar()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = estado.codigo,
            onValueChange = alCambiarCodigo,
            label = { Text(stringResource(R.string.buscar_etiqueta)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall,
            // Decorativo: la etiqueta del campo ya dice que es un código de barras.
            leadingIcon = { Icon(painterResource(R.drawable.ic_codigo_barras), contentDescription = null) },
            // isError pinta el campo de rojo y TalkBack anuncia el error; el texto explica cuál.
            isError = estado.error != null,
            supportingText = {
                Text(
                    text = when (estado.error) {
                        ErrorEan.LARGO_INVALIDO ->
                            stringResource(R.string.buscar_error_largo, estado.codigo.length)
                        ErrorEan.NO_NUMERICO -> stringResource(R.string.buscar_error_numerico)
                        ErrorEan.DIGITO_VERIFICADOR_INVALIDO -> stringResource(R.string.buscar_error_verificador)
                        null -> stringResource(R.string.buscar_ayuda)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { buscar() }),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = buscar,
            enabled = estado.codigo.isNotEmpty() && !estado.buscando,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Icon(
                painterResource(R.drawable.ic_buscar),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.buscar_boton), style = MaterialTheme.typography.titleMedium)
        }

        when {
            estado.buscando -> IndicadorCarga()
            estado.noEncontrado -> Text(
                text = stringResource(R.string.buscar_no_encontrado),
                style = MaterialTheme.typography.bodyLarge
            )
            estado.falloCarga -> Text(
                text = stringResource(R.string.error_carga),
                color = MaterialTheme.colorScheme.error
            )
            estado.resultado != null -> Resultado(
                detalle = estado.resultado,
                puedeRegistrar = estado.puedeRegistrar,
                alVerDetalle = { alVerDetalle(estado.resultado.producto.codigoInterno) },
                alRegistrar = { alRegistrar(estado.resultado.producto.codigoInterno) }
            )
        }
    }
}

/** Tarjeta del producto encontrado y sus acciones. "Registrar" solo si el perfil lo permite. */
@Composable
private fun Resultado(
    detalle: ExistenciaDetalle,
    puedeRegistrar: Boolean,
    alVerDetalle: () -> Unit,
    alRegistrar: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.buscar_resultado),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = 72.dp)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = detalle.producto.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(
                            R.string.buscar_resultado_detalle,
                            detalle.producto.codigoInterno,
                            detalle.existencia.stock,
                            detalle.producto.categoria
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ChipEstadoStock(detalle.estado, Modifier.padding(start = 12.dp))
            }
        }
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                // Texto en el color de marca, como en el mockup (por defecto Material lo deja gris).
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                onClick = alVerDetalle,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = 56.dp)
            ) {
                Text(stringResource(R.string.buscar_ver_detalle), style = MaterialTheme.typography.titleMedium)
            }
            if (puedeRegistrar) {
                Button(
                    onClick = alRegistrar,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 56.dp)
                ) {
                    Text(stringResource(R.string.buscar_registrar), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContenidoBuscarCodigoPreview() {
    BYTOTheme {
        ContenidoBuscarCodigo(
            estado = EstadoBuscarCodigo(
                codigo = "2000000000084",
                resultado = ExistenciaDetalle(
                    Existencia("T-ST-1", "DE-03", 7),
                    Producto("2000000000084", "DE-03", "Mat de yoga", "Deportes"),
                    EstadoStock.BAJO
                ),
                puedeRegistrar = true
            ),
            alCambiarCodigo = {},
            alBuscar = {},
            alVerDetalle = {},
            alRegistrar = {}
        )
    }
}
