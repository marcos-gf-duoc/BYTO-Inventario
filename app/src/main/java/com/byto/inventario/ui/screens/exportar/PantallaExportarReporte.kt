package com.byto.inventario.ui.screens.exportar

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byto.inventario.R
import com.byto.inventario.data.model.TipoUbicacion
import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.domain.ErroresExportacion
import com.byto.inventario.domain.TipoReporte
import com.byto.inventario.ui.components.IndicadorCarga
import com.byto.inventario.ui.components.MensajeError
import com.byto.inventario.ui.components.TituloSeccion
import com.byto.inventario.ui.theme.BYTOTheme
import kotlinx.coroutines.launch

/** P15 Exportar reporte (Gerente): guarda un CSV de stock, movimientos o alertas. */
@Composable
fun PantallaExportarReporte(
    alSinConexion: () -> Unit,
    alSinSesion: () -> Unit,
    viewModel: ExportarReporteViewModel = viewModel(factory = FabricaViewModels.Factory)
) {
    val estado by viewModel.estado.collectAsState()
    val avisos = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    val contexto = LocalContext.current

    // Selector "Guardar como" del sistema. CreateDocument recibe el tipo de archivo y, al
    // lanzarlo, el nombre sugerido; devuelve la dirección (Uri) elegida, o null si se cancela.
    val guardarComo = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { destino -> viewModel.guardarEn(destino) }

    LaunchedEffect(Unit) { viewModel.alMostrarse() }
    LaunchedEffect(estado.pedirDestino) {
        estado.pedirDestino?.let { nombre ->
            viewModel.destinoSolicitado()
            guardarComo.launch(nombre)
        }
    }
    LaunchedEffect(estado.irASinConexion) {
        if (estado.irASinConexion) {
            viewModel.navegacionSinConexionRealizada()
            alSinConexion()
        }
    }
    LaunchedEffect(estado.sinSesion) {
        if (estado.sinSesion) alSinSesion()
    }

    // Confirmación visible del guardado y botón de compartir.
    val textoGuardado = estado.guardado?.let { stringResource(R.string.exportar_guardado, it) }
    val ultimoUri = estado.ultimoDestino
    LaunchedEffect(textoGuardado, ultimoUri) {
        if (textoGuardado != null && ultimoUri != null) {
            viewModel.avisoMostrado()
            alcance.launch {
                val resultado = avisos.showSnackbar(
                    message = textoGuardado,
                    actionLabel = "Compartir",
                    duration = SnackbarDuration.Long
                )
                if (resultado == SnackbarResult.ActionPerformed) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, ultimoUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        contexto.startActivity(Intent.createChooser(intent, "Compartir reporte CSV"))
                    } catch (e: Exception) {
                        // Ignorar si no hay app para compartir
                    }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        ContenidoExportar(
            estado = estado,
            alElegirTipo = viewModel::elegirTipo,
            alElegirAlcance = viewModel::elegirAlcance,
            alExportar = viewModel::solicitarExportar
        )
        SnackbarHost(avisos, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ContenidoExportar(
    estado: EstadoExportar,
    alElegirTipo: (TipoReporte) -> Unit,
    alElegirAlcance: (AlcanceReporte) -> Unit,
    alExportar: () -> Unit
) {
    if (estado.cargando) {
        IndicadorCarga()
        return
    }
    if (estado.error) {
        Text(
            text = stringResource(R.string.error_carga),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
        return
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
            CampoTipo(estado.tipo, estado.errores, alElegirTipo)
            CampoUbicacion(estado, alElegirAlcance)
            CampoFormato()
            Text(
                text = stringResource(R.string.exportar_ayuda),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MensajeError(
                texto = when (estado.errorGuardado) {
                    ErrorGuardado.SIN_DESTINO -> stringResource(R.string.exportar_error_sin_destino)
                    ErrorGuardado.NO_SE_PUDO_ESCRIBIR -> stringResource(R.string.exportar_error_escritura)
                    null -> null
                }
            )
        }
        Button(
            onClick = alExportar,
            enabled = estado.puedeExportar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 56.dp)
        ) {
            // Mientras se arma y guarda el archivo, el ícono cambia por un indicador de progreso.
            if (estado.exportando) {
                CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize), strokeWidth = 2.dp)
            } else {
                Icon(
                    painterResource(R.drawable.ic_exportar),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
            }
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.exportar_boton), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * Opciones de radio en una tarjeta, como en el mockup. Cada fila completa es tocable
 * (selectable con rol RadioButton) y selectableGroup le dice a TalkBack que es una sola pregunta.
 */
@Composable
private fun CampoTipo(tipo: TipoReporte?, errores: ErroresExportacion, alElegir: (TipoReporte) -> Unit) {
    val opciones = listOf(
        TipoReporte.STOCK to R.string.exportar_tipo_stock,
        TipoReporte.MOVIMIENTOS to R.string.exportar_tipo_movimientos,
        TipoReporte.ALERTAS to R.string.exportar_tipo_alertas
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TituloSeccion(stringResource(R.string.exportar_tipo))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            opciones.forEachIndexed { indice, (opcion, texto) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .selectable(selected = tipo == opcion, role = Role.RadioButton, onClick = { alElegir(opcion) })
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // onClick = null: la fila completa ya maneja el toque.
                    RadioButton(selected = tipo == opcion, onClick = null)
                    Text(
                        stringResource(texto),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                if (indice < opciones.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        MensajeError(texto = if (errores.tipo) stringResource(R.string.exportar_error_tipo) else null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoUbicacion(estado: EstadoExportar, alElegir: (AlcanceReporte) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val todas = stringResource(R.string.exportar_ubicacion_todas)
    val opciones: List<Pair<AlcanceReporte, String>> = listOf<Pair<AlcanceReporte, String>>(AlcanceReporte.Todas to todas) +
        estado.ubicaciones.map { AlcanceReporte.Una(it.id) to stringResource(R.string.exportar_ubicacion_opcion, it.id, it.nombre) }
    val elegida = opciones.firstOrNull { it.first == estado.alcance }?.second.orEmpty()

    Column {
        ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
            OutlinedTextField(
                value = elegida,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.exportar_ubicacion)) },
                // Decorativo: la etiqueta "Ubicación" já lo dice.
                leadingIcon = { Icon(painterResource(R.drawable.ic_ubicacion), contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
                isError = estado.errores.ubicacion,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                opciones.forEach { (valor, texto) ->
                    DropdownMenuItem(
                        text = { Text(texto) },
                        onClick = {
                            alElegir(valor)
                            abierto = false
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
        }
        MensajeError(texto = if (estado.errores.ubicacion) stringResource(R.string.exportar_error_ubicacion) else null)
    }
}

/** Por ahora hay un solo formato: se muestra como dato, sin menú que abrir. */
@Composable
private fun CampoFormato() {
    OutlinedTextField(
        value = stringResource(R.string.exportar_formato_csv),
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.exportar_formato)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
private fun ContenidoExportarPreview() {
    BYTOTheme {
        ContenidoExportar(
            estado = EstadoExportar(
                cargando = false,
                ubicaciones = listOf(Ubicacion("T-ST-1", "BYTO Santiago Centro", TipoUbicacion.TIENDA, "B-ST-1", true)),
                tipo = TipoReporte.STOCK,
                alcance = AlcanceReporte.Una("T-ST-1")
            ),
            alElegirTipo = {},
            alElegirAlcance = {},
            alExportar = {}
        )
    }
}
