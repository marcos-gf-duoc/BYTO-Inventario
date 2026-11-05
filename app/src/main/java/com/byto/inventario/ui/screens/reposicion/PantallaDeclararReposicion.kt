package com.byto.inventario.ui.screens.reposicion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byto.inventario.R
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.Existencia
import com.byto.inventario.data.model.ExistenciaDetalle
import com.byto.inventario.data.model.Producto
import com.byto.inventario.data.model.TipoUbicacion
import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.domain.ErrorReposicion
import com.byto.inventario.domain.ErroresReposicion
import com.byto.inventario.domain.ReglasInventario
import com.byto.inventario.ui.components.ChipEstadoStock
import com.byto.inventario.ui.components.IndicadorCarga
import com.byto.inventario.ui.components.MensajeError
import com.byto.inventario.ui.theme.BYTOTheme
import kotlinx.coroutines.launch

/** P12 Declarar reposición: el Encargado avisa que una de sus tiendas necesita un producto. */
@Composable
fun PantallaDeclararReposicion(
    alSinConexion: () -> Unit,
    alSinSesion: () -> Unit,
    viewModel: DeclararReposicionViewModel = viewModel(factory = FabricaViewModels.Factory)
) {
    val estado by viewModel.estado.collectAsState()
    val avisos = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()

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

    // Confirmación visible del envío. Se lanza aparte para que avisoMostrado() no la cancele.
    val textoDeclarada = estado.declarada?.let {
        stringResource(R.string.reposicion_declarada, it.nombreProducto, it.tiendaId)
    }
    LaunchedEffect(textoDeclarada) {
        if (textoDeclarada != null) {
            viewModel.avisoMostrado()
            alcance.launch { avisos.showSnackbar(textoDeclarada) }
        }
    }

    Box(Modifier.fillMaxSize()) {
        ContenidoReposicion(
            estado = estado,
            alElegirTienda = viewModel::elegirTienda,
            alElegirProducto = viewModel::elegirProducto,
            alCambiarObservacion = viewModel::cambiarObservacion,
            alDeclarar = viewModel::declarar
        )
        SnackbarHost(avisos, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ContenidoReposicion(
    estado: EstadoReposicion,
    alElegirTienda: (String) -> Unit,
    alElegirProducto: (String) -> Unit,
    alCambiarObservacion: (String) -> Unit,
    alDeclarar: () -> Unit
) {
    when {
        estado.cargando -> {
            IndicadorCarga()
            return
        }
        estado.error -> {
            Text(
                text = stringResource(R.string.error_carga),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
            return
        }
        estado.tiendas.isEmpty() -> {
            Text(
                text = stringResource(R.string.reposicion_sin_tiendas),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )
            return
        }
    }

    // El formulario se desplaza; el botón queda fijo abajo, como en el mockup.
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CampoTienda(estado, alElegirTienda)
            CampoProducto(estado, alElegirProducto)
            CampoObservacion(estado, alCambiarObservacion)
            AvisoNotificacion()
            MensajeError(
                texto = if (estado.errorGeneral) stringResource(R.string.reposicion_error_envio) else null
            )
        }
        Button(
            onClick = alDeclarar,
            enabled = estado.puedeEnviar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 56.dp)
        ) {
            Text(stringResource(R.string.reposicion_boton), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Lista desplegable de tiendas. Material la arma con un campo de solo lectura y un menú. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoTienda(estado: EstadoReposicion, alElegir: (String) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val elegida = estado.tiendas.firstOrNull { it.id == estado.tiendaId }

    Column {
        ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
            OutlinedTextField(
                value = elegida?.let { stringResource(R.string.reposicion_tienda_opcion, it.id, it.nombre) }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.reposicion_tienda)) },
                placeholder = { Text(stringResource(R.string.reposicion_tienda_elegir)) },
                // Decorativo: la etiqueta "Tienda" ya lo dice.
                leadingIcon = { Icon(painterResource(R.drawable.ic_tienda), contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
                isError = estado.errores.tienda != null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                estado.tiendas.forEach { tienda ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.reposicion_tienda_opcion, tienda.id, tienda.nombre)) },
                        onClick = {
                            alElegir(tienda.id)
                            abierto = false
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
        }
        MensajeError(texto = estado.errores.tienda?.let { stringResource(textoError(it)) })
    }
}

/** Lista de productos de la tienda elegida, con su stock y estado (texto y color). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoProducto(estado: EstadoReposicion, alElegir: (String) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val elegido = estado.productos.firstOrNull { it.producto.codigoInterno == estado.codigoInterno }
    // Sin tienda no hay productos que mostrar.
    val habilitado = estado.tiendaId != null && !estado.cargandoProductos

    Column {
        ExposedDropdownMenuBox(
            expanded = abierto && habilitado,
            onExpandedChange = { if (habilitado) abierto = it }
        ) {
            OutlinedTextField(
                value = elegido?.producto?.nombre.orEmpty(),
                onValueChange = {},
                readOnly = true,
                enabled = habilitado,
                label = { Text(stringResource(R.string.reposicion_producto)) },
                placeholder = {
                    Text(
                        stringResource(
                            if (estado.tiendaId == null) R.string.reposicion_producto_elegir_tienda
                            else R.string.reposicion_producto_elegir
                        )
                    )
                },
                // Debajo del nombre: código, unidades y estado del producto elegido.
                supportingText = elegido?.let {
                    {
                        Text(
                            stringResource(R.string.reposicion_producto_detalle, it.producto.codigoInterno, it.existencia.stock),
                            // Siempre gris: es información, no parte del error (que va en MensajeError).
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        elegido?.let { ChipEstadoStock(it.estado) }
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto)
                    }
                },
                isError = estado.errores.producto != null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = habilitado)
            )
            ExposedDropdownMenu(expanded = abierto && habilitado, onDismissRequest = { abierto = false }) {
                estado.productos.forEach { detalle ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(detalle.producto.nombre, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    stringResource(
                                        R.string.reposicion_producto_detalle,
                                        detalle.producto.codigoInterno,
                                        detalle.existencia.stock
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        trailingIcon = { ChipEstadoStock(detalle.estado) },
                        onClick = {
                            alElegir(detalle.producto.codigoInterno)
                            abierto = false
                        },
                        modifier = Modifier.heightIn(min = 56.dp)
                    )
                }
            }
        }
        if (estado.cargandoProductos) IndicadorCarga()
        MensajeError(texto = estado.errores.producto?.let { stringResource(textoError(it)) })
    }
}

@Composable
private fun CampoObservacion(estado: EstadoReposicion, alCambiar: (String) -> Unit) {
    val maximo = ReglasInventario.MAX_CARACTERES_OBSERVACION
    Column {
        OutlinedTextField(
            value = estado.observacion,
            onValueChange = alCambiar,
            label = { Text(stringResource(R.string.reposicion_observacion)) },
            minLines = 3,
            isError = estado.errores.observacion != null,
            supportingText = {
                // Contador a la derecha: ayuda a no pasarse del límite antes de que sea un error.
                Text(
                    text = stringResource(R.string.registrar_contador, estado.observacion.length, maximo),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        MensajeError(
            texto = estado.errores.observacion?.let { stringResource(R.string.error_observacion, maximo) }
        )
    }
}

/** Recuadro lila que explica qué pasa al declarar (como en el mockup). */
@Composable
private fun AvisoNotificacion() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Decorativo: el texto explica el aviso.
            Icon(painterResource(R.drawable.ic_alertas), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = stringResource(R.string.reposicion_aviso),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

private fun textoError(error: ErrorReposicion): Int = when (error) {
    ErrorReposicion.TIENDA_OBLIGATORIA -> R.string.reposicion_error_tienda
    ErrorReposicion.PRODUCTO_OBLIGATORIO -> R.string.reposicion_error_producto
    ErrorReposicion.REPOSICION_YA_ABIERTA -> R.string.reposicion_error_ya_abierta
    // La observación usa su propio texto con el máximo; este caso no se llama, pero el when lo exige.
    ErrorReposicion.OBSERVACION_MUY_LARGA -> R.string.error_observacion
}

@Preview(showBackground = true)
@Composable
private fun ContenidoReposicionPreview() {
    val silla = ExistenciaDetalle(
        Existencia("T-ST-1", "MU-01", 0),
        Producto("2000000000213", "MU-01", "Silla de escritorio", "Muebles"),
        EstadoStock.SIN_STOCK
    )
    BYTOTheme {
        ContenidoReposicion(
            estado = EstadoReposicion(
                cargando = false,
                tiendas = listOf(Ubicacion("T-ST-1", "BYTO Santiago Centro", TipoUbicacion.TIENDA, "B-ST-1", true)),
                tiendaId = "T-ST-1",
                productos = listOf(silla),
                codigoInterno = "MU-01",
                observacion = "Enviar desde B-ST-1",
                errores = ErroresReposicion(producto = ErrorReposicion.REPOSICION_YA_ABIERTA)
            ),
            alElegirTienda = {},
            alElegirProducto = {},
            alCambiarObservacion = {},
            alDeclarar = {}
        )
    }
}
