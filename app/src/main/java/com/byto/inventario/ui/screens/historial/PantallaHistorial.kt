package com.byto.inventario.ui.screens.historial

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byto.inventario.R
import com.byto.inventario.data.model.Movimiento
import com.byto.inventario.data.model.TipoMovimiento
import com.byto.inventario.data.model.TipoUbicacion
import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.domain.Fechas
import com.byto.inventario.ui.components.IndicadorCarga
import com.byto.inventario.ui.components.MensajeError
import com.byto.inventario.ui.components.conSigno
import com.byto.inventario.ui.components.textoRes
import com.byto.inventario.ui.theme.BYTOTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * P13 Historial / auditoría de movimientos con filtros por ubicación, fechas, operador y tipo.
 * @param alConocerSubtitulo avisa el texto "GE-001 · 6 movimientos" para la barra superior.
 */
@Composable
fun PantallaHistorial(
    alConocerSubtitulo: (String) -> Unit,
    alSinConexion: () -> Unit,
    alSinSesion: () -> Unit,
    viewModel: HistorialViewModel = viewModel(factory = FabricaViewModels.Factory)
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

    val subtitulo = pluralStringResource(
        R.plurals.historial_subtitulo, estado.movimientos.size, estado.usuarioId, estado.movimientos.size
    )
    LaunchedEffect(subtitulo, estado.buscando, estado.cargando) {
        if (!estado.cargando && !estado.buscando) alConocerSubtitulo(subtitulo)
    }

    ContenidoHistorial(
        estado = estado,
        acciones = AccionesHistorial(
            alElegirUbicacion = viewModel::elegirUbicacion,
            alElegirOperador = viewModel::elegirOperador,
            alElegirTipo = viewModel::elegirTipo,
            alElegirDesde = viewModel::elegirDesde,
            alElegirHasta = viewModel::elegirHasta,
            alLimpiar = viewModel::limpiarFiltros
        )
    )
}

/** Agrupa las acciones de los filtros para no pasar seis parámetros sueltos. */
private class AccionesHistorial(
    val alElegirUbicacion: (String?) -> Unit,
    val alElegirOperador: (String?) -> Unit,
    val alElegirTipo: (TipoMovimiento?) -> Unit,
    val alElegirDesde: (LocalDate?) -> Unit,
    val alElegirHasta: (LocalDate?) -> Unit,
    val alLimpiar: () -> Unit
)

@Composable
private fun ContenidoHistorial(estado: EstadoHistorial, acciones: AccionesHistorial) {
    if (estado.cargando) {
        IndicadorCarga()
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Filtros(estado, acciones, Modifier.padding(top = 12.dp))
        MensajeError(texto = if (estado.errorRango) stringResource(R.string.historial_error_rango) else null)

        when {
            estado.buscando -> IndicadorCarga()
            estado.error -> Text(
                text = stringResource(R.string.error_carga),
                color = MaterialTheme.colorScheme.error
            )
            // Con el rango inválido no se busca: el mensaje de error ya explica por qué no hay lista.
            estado.errorRango -> Unit
            estado.movimientos.isEmpty() -> Text(
                text = stringResource(R.string.historial_vacio),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
            ) {
                itemsIndexed(estado.movimientos, key = { _, fila -> fila.movimiento.id }) { indice, fila ->
                    FilaHistorial(
                        fila = fila,
                        primera = indice == 0,
                        ultima = indice == estado.movimientos.lastIndex,
                        // Fundido y desplazamiento suave cuando un filtro agrega o quita filas.
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

/** Fila de chips de filtro. Cada uno abre su menú o su calendario. */
@Composable
private fun Filtros(estado: EstadoHistorial, acciones: AccionesHistorial, modifier: Modifier = Modifier) {
    val todas = stringResource(R.string.historial_ubicacion_todas)
    val todos = stringResource(R.string.historial_operador_todos)
    val sinFecha = stringResource(R.string.historial_sin_fecha)

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ChipConMenu(
            texto = estado.ubicacionId ?: todas,
            icono = R.drawable.ic_ubicacion,
            activo = estado.ubicacionId != null,
            opciones = listOf<Pair<String?, String>>(null to todas) +
                estado.ubicaciones.map { it.id to "${it.id} · ${it.nombre}" },
            alElegir = acciones.alElegirUbicacion
        )
        ChipFecha(
            texto = stringResource(R.string.historial_desde, estado.desde?.let { Fechas.dia(it) } ?: sinFecha),
            titulo = stringResource(R.string.historial_titulo_desde),
            dia = estado.desde,
            enError = estado.errorRango,
            alElegir = acciones.alElegirDesde
        )
        ChipFecha(
            texto = stringResource(R.string.historial_hasta, estado.hasta?.let { Fechas.dia(it) } ?: sinFecha),
            titulo = stringResource(R.string.historial_titulo_hasta),
            dia = estado.hasta,
            enError = estado.errorRango,
            alElegir = acciones.alElegirHasta
        )
        // El Operador solo ve sus movimientos: para él no hay filtro de operador.
        if (!estado.operadorFijo) {
            ChipConMenu(
                texto = estado.operadorId ?: todos,
                icono = R.drawable.ic_persona,
                activo = estado.operadorId != null,
                opciones = listOf<Pair<String?, String>>(null to todos) + estado.operadores.map { it to it },
                alElegir = acciones.alElegirOperador
            )
        }
        ChipConMenu(
            texto = estado.tipo?.let { stringResource(R.string.historial_tipo, stringResource(it.textoRes()).lowercase()) }
                ?: stringResource(R.string.historial_tipo_todos),
            icono = null,
            activo = estado.tipo != null,
            opciones = listOf<Pair<TipoMovimiento?, String>>(null to stringResource(R.string.historial_tipo_todos)) +
                TipoMovimiento.entries.map { it to stringResource(it.textoRes()) },
            alElegir = acciones.alElegirTipo
        )
        if (estado.hayFiltros) {
            TextButton(onClick = acciones.alLimpiar, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.historial_limpiar))
            }
        }
    }
}

/**
 * Chip que abre un menú de opciones. Se marca "activo" (con color y ✓) cuando el filtro
 * no está en "todos", para que se vea qué filtros están aplicados sin depender solo del color.
 */
@Composable
private fun <T> ChipConMenu(
    texto: String,
    @DrawableRes icono: Int?,
    activo: Boolean,
    opciones: List<Pair<T, String>>,
    alElegir: (T) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = activo,
            onClick = { abierto = true },
            label = { Text(texto) },
            leadingIcon = iconoDeChip(if (activo) R.drawable.ic_check else icono)
        )
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { (valor, etiqueta) ->
                DropdownMenuItem(
                    text = { Text(etiqueta) },
                    onClick = {
                        alElegir(valor)
                        abierto = false
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
    }
}

/**
 * Chip de fecha que abre el calendario de Material. Si el rango es inválido se pinta
 * en el color de error, además del mensaje con ícono que aparece debajo de los chips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipFecha(
    texto: String,
    titulo: String,
    dia: LocalDate?,
    enError: Boolean,
    alElegir: (LocalDate?) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    FilterChip(
        selected = dia != null,
        onClick = { abierto = true },
        label = { Text(texto) },
        leadingIcon = iconoDeChip(R.drawable.ic_calendario),
        colors = if (enError) {
            FilterChipDefaults.filterChipColors(
                labelColor = MaterialTheme.colorScheme.error,
                selectedLabelColor = MaterialTheme.colorScheme.error,
                selectedContainerColor = MaterialTheme.colorScheme.errorContainer
            )
        } else {
            FilterChipDefaults.filterChipColors()
        }
    )
    if (abierto) {
        // El DatePicker trabaja con milisegundos de la medianoche UTC del día elegido.
        val calendario = rememberDatePickerState(initialSelectedDateMillis = dia?.let { aMilisegundos(it) })
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    alElegir(calendario.selectedDateMillis?.let { desdeMilisegundos(it) })
                    abierto = false
                }) { Text(stringResource(R.string.historial_aceptar)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        alElegir(null)
                        abierto = false
                    }) { Text(stringResource(R.string.historial_quitar_fecha)) }
                    TextButton(onClick = { abierto = false }) { Text(stringResource(R.string.historial_cancelar)) }
                }
            }
        ) {
            DatePicker(
                state = calendario,
                title = { Text(titulo, modifier = Modifier.padding(start = 24.dp, top = 16.dp)) }
            )
        }
    }
}

private fun aMilisegundos(dia: LocalDate): Long = dia.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun desdeMilisegundos(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()

/** Ícono decorativo del chip (el texto del chip ya dice qué filtra). */
private fun iconoDeChip(@DrawableRes icono: Int?): (@Composable () -> Unit)? = icono?.let {
    {
        Icon(painterResource(it), contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
    }
}

/** Una fila del historial. Como en el mockup, las filas forman un solo bloque con líneas entre ellas. */
@Composable
private fun FilaHistorial(fila: FilaMovimiento, primera: Boolean, ultima: Boolean, modifier: Modifier = Modifier) {
    val radio = 16.dp
    val forma = RoundedCornerShape(
        topStart = if (primera) radio else 0.dp,
        topEnd = if (primera) radio else 0.dp,
        bottomStart = if (ultima) radio else 0.dp,
        bottomEnd = if (ultima) radio else 0.dp
    )
    val mov = fila.movimiento
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = forma,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp
    ) {
        Column {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // "18-10 13:00 · T-LS-1 · OP-002": cuándo (hora de Chile), dónde y quién.
                Text(
                    text = stringResource(
                        R.string.historial_encabezado,
                        Fechas.fechaHoraCortaChile(mov.fechaUtc),
                        mov.ubicacionId,
                        mov.operadorId
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = fila.nombreProducto, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = resumenDe(mov),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!ultima) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** "Entrada +12 · Recepción semanal", "Conteo 10 → 6 · …" o "Ajuste −2 · Producto dañado". */
@Composable
private fun resumenDe(mov: Movimiento): String {
    val tipo = stringResource(mov.tipo.textoRes())
    val cambio = when (mov.tipo) {
        TipoMovimiento.ENTRADA -> stringResource(R.string.historial_cantidad, tipo, conSigno(mov.cantidad))
        TipoMovimiento.SALIDA -> stringResource(R.string.historial_cantidad, tipo, conSigno(-mov.cantidad))
        TipoMovimiento.AJUSTE -> stringResource(R.string.historial_cantidad, tipo, conSigno(mov.cantidad))
        // En el conteo lo importante es de cuánto a cuánto quedó el stock.
        TipoMovimiento.CONTEO -> stringResource(R.string.historial_conteo, tipo, mov.stockAntes, mov.stockDespues)
    }
    // El motivo (obligatorio en ajustes) dice más que la observación, así que va primero.
    val nota = mov.motivo ?: mov.observacion
    return if (nota != null) stringResource(R.string.historial_con_nota, cambio, nota) else cambio
}

@Preview(showBackground = true)
@Composable
private fun ContenidoHistorialPreview() {
    val filas = listOf(
        FilaMovimiento(
            Movimiento(
                "MOV-0006", TipoMovimiento.ENTRADA, "T-LS-1", "TE-04", 12, 8, 20,
                Instant.parse("2026-10-18T16:00:00Z"), "OP-002", observacion = "Recepción semanal"
            ),
            "Parlante Bluetooth portátil"
        ),
        FilaMovimiento(
            Movimiento(
                "MOV-0003", TipoMovimiento.CONTEO, "T-ST-2", "RM-02", 6, 10, 6,
                Instant.parse("2026-10-17T12:30:00Z"), "OP-001", observacion = "Conteo de sección mujer"
            ),
            "Jeans tiro alto, talla M"
        )
    )
    BYTOTheme {
        ContenidoHistorial(
            estado = EstadoHistorial(
                cargando = false,
                usuarioId = "GE-001",
                ubicaciones = listOf(Ubicacion("T-ST-1", "BYTO Santiago Centro", TipoUbicacion.TIENDA, "B-ST-1", true)),
                operadores = listOf("OP-001", "OP-002"),
                movimientos = filas
            ),
            acciones = AccionesHistorial({}, {}, {}, {}, {}, {})
        )
    }
}
