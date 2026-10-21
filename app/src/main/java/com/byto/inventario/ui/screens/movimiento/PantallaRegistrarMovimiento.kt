package com.byto.inventario.ui.screens.movimiento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byto.inventario.R
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.Existencia
import com.byto.inventario.data.model.ExistenciaDetalle
import com.byto.inventario.data.model.Producto
import com.byto.inventario.data.model.TipoMovimiento
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.domain.ErrorValidacion
import com.byto.inventario.domain.ReglasInventario
import com.byto.inventario.ui.components.ChipEstadoStock
import com.byto.inventario.ui.components.IndicadorCarga
import com.byto.inventario.ui.components.TituloSeccion
import com.byto.inventario.ui.components.coloresDe
import com.byto.inventario.ui.components.conSigno
import com.byto.inventario.ui.components.textoRes
import com.byto.inventario.ui.theme.BYTOTheme
import com.byto.inventario.ui.theme.EstadoBajoFondo
import com.byto.inventario.ui.theme.EstadoBajoTexto

/** P09 Registrar movimiento. [alRegistrado] abre P10 cuando el movimiento quedó guardado. */
@Composable
fun PantallaRegistrarMovimiento(
    alRegistrado: (DatosConfirmacion) -> Unit,
    alSinConexion: () -> Unit,
    alSinSesion: () -> Unit,
    viewModel: RegistrarMovimientoViewModel = viewModel(factory = FabricaViewModels.Factory)
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(Unit) { viewModel.reintentarSiHaceFalta() }
    LaunchedEffect(estado.irASinConexion) {
        if (estado.irASinConexion) {
            viewModel.navegacionSinConexionRealizada()
            alSinConexion()
        }
    }
    LaunchedEffect(estado.sinSesion) {
        if (estado.sinSesion) alSinSesion()
    }
    // Guardado con éxito: navega una sola vez a P10 y avisa al ViewModel.
    LaunchedEffect(estado.registrado) {
        estado.registrado?.let { datos ->
            viewModel.navegacionConfirmacionRealizada()
            alRegistrado(datos)
        }
    }

    ContenidoRegistro(
        estado = estado,
        acciones = AccionesRegistro(
            alCambiarTipo = viewModel::cambiarTipo,
            alCambiarCantidad = viewModel::cambiarCantidad,
            alSumarUno = viewModel::sumarUno,
            alRestarUno = viewModel::restarUno,
            alCambiarSentido = viewModel::cambiarSentidoAjuste,
            alElegirMotivo = viewModel::elegirMotivo,
            alCambiarMotivoOtro = viewModel::cambiarMotivoOtro,
            alCambiarObservacion = viewModel::cambiarObservacion,
            alConfirmar = viewModel::confirmar
        )
    )
}

/** Agrupa los eventos del formulario para no pasar nueve parámetros sueltos. */
private data class AccionesRegistro(
    val alCambiarTipo: (TipoMovimiento) -> Unit,
    val alCambiarCantidad: (String) -> Unit,
    val alSumarUno: () -> Unit,
    val alRestarUno: () -> Unit,
    val alCambiarSentido: (Boolean) -> Unit,
    val alElegirMotivo: (MotivoAjuste) -> Unit,
    val alCambiarMotivoOtro: (String) -> Unit,
    val alCambiarObservacion: (String) -> Unit,
    val alConfirmar: () -> Unit
)

@Composable
private fun ContenidoRegistro(estado: EstadoRegistro, acciones: AccionesRegistro) {
    val detalle = estado.detalle
    when {
        estado.cargando -> IndicadorCarga()
        estado.sinPermiso -> MensajeCentrado(stringResource(R.string.registrar_sin_permiso))
        estado.error || detalle == null -> MensajeCentrado(stringResource(R.string.error_carga))
        else -> Column(modifier = Modifier.fillMaxSize()) {
            // El formulario se desplaza; el botón "Confirmar" queda fijo abajo.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TarjetaProducto(detalle)

                TituloSeccion(stringResource(R.string.registrar_tipo))
                SelectorTipo(estado.tipo, acciones.alCambiarTipo)

                if (estado.tipo == TipoMovimiento.AJUSTE) {
                    TituloSeccion(stringResource(R.string.registrar_sentido))
                    SelectorSentido(estado.sumar, acciones.alCambiarSentido)
                }

                SeccionCantidad(estado, detalle.existencia.stock, acciones)

                if (estado.tipo == TipoMovimiento.AJUSTE) {
                    SeccionMotivo(estado, acciones)
                }

                CampoObservacion(estado, acciones.alCambiarObservacion)

                TarjetaStockResultante(
                    antes = detalle.existencia.stock,
                    despues = estado.stockResultante,
                    estadoDespues = estado.estadoResultante
                )
            }

            if (estado.errorEnvio) {
                Text(
                    text = stringResource(R.string.registrar_error_envio),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 8.dp)
                )
            }
            // Mientras guarda, el botón se deshabilita y muestra una rueda: no se puede enviar dos veces.
            Button(
                onClick = acciones.alConfirmar,
                enabled = estado.puedeConfirmar && !estado.enviando,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(min = 56.dp)
            ) {
                if (estado.enviando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.registrar_guardando),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                } else {
                    Text(stringResource(R.string.registrar_confirmar), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun MensajeCentrado(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    )
}

/** Nombre, códigos, stock actual y estado del producto. */
@Composable
private fun TarjetaProducto(detalle: ExistenciaDetalle) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = detalle.producto.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.registrar_codigos, detalle.producto.codigoInterno, detalle.producto.ean13),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.registrar_stock_actual, detalle.existencia.stock),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            ChipEstadoStock(detalle.estado, Modifier.padding(start = 12.dp))
        }
    }
}

/**
 * Botones segmentados para elegir una sola opción. Material marca el elegido con ✓
 * (no solo con color) y TalkBack lo anuncia como "seleccionado".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorTipo(tipo: TipoMovimiento, alElegir: (TipoMovimiento) -> Unit) {
    val tipos = TipoMovimiento.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        tipos.forEachIndexed { indice, opcion ->
            SegmentedButton(
                selected = opcion == tipo,
                onClick = { alElegir(opcion) },
                shape = SegmentedButtonDefaults.itemShape(index = indice, count = tipos.size),
                // Sin ícono ✓: con 4 opciones no cabría el texto; el fondo y TalkBack indican la elegida.
                icon = {},
                modifier = Modifier.heightIn(min = 56.dp),
                label = { Text(stringResource(opcion.textoRes()), maxLines = 1) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorSentido(sumar: Boolean, alElegir: (Boolean) -> Unit) {
    val opciones = listOf(true to R.string.registrar_sumar, false to R.string.registrar_restar)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        opciones.forEachIndexed { indice, (valor, texto) ->
            SegmentedButton(
                selected = valor == sumar,
                onClick = { alElegir(valor) },
                shape = SegmentedButtonDefaults.itemShape(index = indice, count = opciones.size),
                modifier = Modifier.heightIn(min = 56.dp),
                label = { Text(stringResource(texto)) }
            )
        }
    }
}

/** Cantidad con botones − y + de 56 dp, el campo editable, sus errores y (en conteo) la diferencia. */
@Composable
private fun SeccionCantidad(estado: EstadoRegistro, stockActual: Int, acciones: AccionesRegistro) {
    val etiqueta = stringResource(
        if (estado.tipo == TipoMovimiento.CONTEO) R.string.registrar_cantidad_contada else R.string.registrar_cantidad
    )
    TituloSeccion(etiqueta)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalIconButton(onClick = acciones.alRestarUno, modifier = Modifier.size(56.dp)) {
                Icon(painterResource(R.drawable.ic_menos), contentDescription = stringResource(R.string.registrar_disminuir))
            }
            OutlinedTextField(
                value = estado.cantidadTexto,
                onValueChange = acciones.alCambiarCantidad,
                singleLine = true,
                isError = estado.cantidadVacia || estado.errorCantidad != null,
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                // El título de arriba no está "pegado" al campo, así que TalkBack necesita su nombre.
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = etiqueta }
            )
            FilledTonalIconButton(onClick = acciones.alSumarUno, modifier = Modifier.size(56.dp)) {
                Icon(painterResource(R.drawable.ic_mas), contentDescription = stringResource(R.string.registrar_aumentar))
            }
        }
    }

    val cantidad = estado.cantidad ?: 0
    val error = when {
        estado.cantidadVacia -> stringResource(R.string.error_cantidad_vacia)
        estado.errorCantidad == ErrorValidacion.CANTIDAD_INVALIDA -> stringResource(R.string.error_cantidad_invalida)
        estado.errorCantidad == ErrorValidacion.STOCK_INSUFICIENTE ->
            stringResource(R.string.error_stock_insuficiente, cantidad, stockActual)
        estado.errorCantidad == ErrorValidacion.STOCK_NEGATIVO ->
            stringResource(R.string.error_stock_negativo, conSigno(stockActual - cantidad))
        else -> null
    }
    if (error != null) TextoError(error)

    // Conteo: diferencia con el sistema y aviso de descalce si no coincide.
    estado.diferenciaConteo?.let { diferencia ->
        Text(
            text = stringResource(R.string.registrar_diferencia, conSigno(diferencia)),
            style = MaterialTheme.typography.bodyLarge
        )
        if (diferencia != 0) {
            Aviso(stringResource(R.string.registrar_aviso_descalce))
        } else {
            Text(
                text = stringResource(R.string.registrar_sin_diferencia),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Motivo del ajuste: chips de una sola elección y, si es "Otro", un campo de texto. */
@Composable
private fun SeccionMotivo(estado: EstadoRegistro, acciones: AccionesRegistro) {
    TituloSeccion(stringResource(R.string.registrar_motivo))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MotivoAjuste.entries.forEach { motivo ->
            val elegido = motivo == estado.motivo
            FilterChip(
                selected = elegido,
                onClick = { acciones.alElegirMotivo(motivo) },
                label = { Text(stringResource(textoDe(motivo))) },
                leadingIcon = if (elegido) {
                    {
                        Icon(
                            painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
    if (estado.motivo == MotivoAjuste.OTRO) {
        OutlinedTextField(
            value = estado.motivoOtro,
            onValueChange = acciones.alCambiarMotivoOtro,
            label = { Text(stringResource(R.string.registrar_motivo_otro)) },
            singleLine = true,
            isError = estado.errorMotivo,
            modifier = Modifier.fillMaxWidth()
        )
    }
    if (estado.errorMotivo) TextoError(stringResource(R.string.error_motivo))
}

@Composable
private fun CampoObservacion(estado: EstadoRegistro, alCambiar: (String) -> Unit) {
    val maximo = ReglasInventario.MAX_CARACTERES_OBSERVACION
    OutlinedTextField(
        value = estado.observacion,
        onValueChange = alCambiar,
        label = { Text(stringResource(R.string.registrar_observacion)) },
        minLines = 3,
        isError = estado.errorObservacion,
        supportingText = {
            // Contador a la derecha; si se pasa del límite, además el mensaje de error.
            Text(
                text = if (estado.errorObservacion) {
                    stringResource(R.string.error_observacion, maximo)
                } else {
                    stringResource(R.string.registrar_contador, estado.observacion.length, maximo)
                },
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * "Stock resultante X → Y" con el color y el nombre del estado que tendría.
 * Si la cantidad aún no es válida, muestra "—" en un fondo neutro.
 */
@Composable
private fun TarjetaStockResultante(antes: Int, despues: Int?, estadoDespues: EstadoStock?) {
    val colores = estadoDespues?.let { coloresDe(it) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(
            containerColor = colores?.fondo ?: MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = colores?.texto ?: MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.registrar_stock_resultante), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "$antes → ${despues ?: "—"}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (estadoDespues != null && colores != null) {
                // Píldora blanca (como en el mockup): un chip del mismo color que la tarjeta no se vería.
                // En "Sin stock" el texto del estado es blanco, así que se usa el color oscuro del fondo.
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    contentColor = if (estadoDespues == EstadoStock.SIN_STOCK) colores.fondo else colores.texto
                ) {
                    Text(
                        text = stringResource(estadoDespues.textoRes()),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TextoError(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(start = 4.dp)
    )
}

/** Recuadro ámbar de advertencia (texto, no solo color). */
@Composable
private fun Aviso(texto: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = EstadoBajoFondo, contentColor = EstadoBajoTexto)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_advertencia), contentDescription = null)
            Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

private fun textoDe(motivo: MotivoAjuste): Int = when (motivo) {
    MotivoAjuste.PRODUCTO_DANADO -> R.string.motivo_producto_danado
    MotivoAjuste.MERMA -> R.string.motivo_merma
    MotivoAjuste.ERROR_REGISTRO -> R.string.motivo_error_registro
    MotivoAjuste.OTRO -> R.string.motivo_otro
}

@Preview(showBackground = true)
@Composable
private fun ContenidoRegistroPreview() {
    BYTOTheme {
        ContenidoRegistro(
            estado = EstadoRegistro(
                cargando = false,
                detalle = ExistenciaDetalle(
                    Existencia("T-ST-1", "DE-03", 7),
                    Producto("2000000000084", "DE-03", "Mat de yoga", "Deportes"),
                    EstadoStock.BAJO
                ),
                tipo = TipoMovimiento.SALIDA,
                cantidadTexto = "2",
                stockResultante = 5,
                estadoResultante = EstadoStock.BAJO,
                puedeConfirmar = true
            ),
            acciones = AccionesRegistro({}, {}, {}, {}, {}, {}, {}, {}, {})
        )
    }
}
