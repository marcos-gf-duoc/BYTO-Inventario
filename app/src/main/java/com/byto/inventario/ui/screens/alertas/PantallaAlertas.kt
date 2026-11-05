package com.byto.inventario.ui.screens.alertas

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byto.inventario.R
import com.byto.inventario.data.model.Alerta
import com.byto.inventario.data.model.EstadoAlerta
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.TipoAlerta
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.domain.Fechas
import com.byto.inventario.ui.components.ColoresEstado
import com.byto.inventario.ui.components.IndicadorCarga
import com.byto.inventario.ui.components.coloresDe
import com.byto.inventario.ui.components.conSigno
import com.byto.inventario.ui.theme.BYTOTheme
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * P11 Alertas abiertas de las ubicaciones del usuario.
 * @param alConocerSubtitulo avisa el texto "EB-001 · 7 abiertas" para la barra superior.
 */
@Composable
fun PantallaAlertas(
    alConocerSubtitulo: (String) -> Unit,
    alSinConexion: () -> Unit,
    alSinSesion: () -> Unit,
    viewModel: AlertasViewModel = viewModel(factory = FabricaViewModels.Factory)
) {
    val estado by viewModel.estado.collectAsState()
    val avisos = remember { SnackbarHostState() }

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

    // El subtítulo cuenta todas las abiertas (no solo las del filtro), como en el mockup.
    val subtitulo = pluralStringResource(
        R.plurals.alertas_subtitulo, estado.todas.size, estado.usuarioId, estado.todas.size
    )
    LaunchedEffect(subtitulo, estado.cargando) {
        if (!estado.cargando) alConocerSubtitulo(subtitulo)
    }

    // Texto del aviso; se calcula aquí porque stringResource solo funciona dentro de un Composable.
    val textoAviso = when (estado.mensaje) {
        MensajeAlertas.DESCALCE_REVISADO -> stringResource(R.string.alertas_revisado)
        MensajeAlertas.YA_NO_REVISABLE -> stringResource(R.string.alertas_ya_no_revisable)
        MensajeAlertas.ERROR_REVISION -> stringResource(R.string.alertas_error_revision)
        null -> null
    }
    // showSnackbar espera hasta que el aviso se oculta. Se lanza en un alcance aparte para que
    // marcar el mensaje como mostrado (que cambia la clave de este efecto) no lo cancele.
    val alcance = rememberCoroutineScope()
    LaunchedEffect(textoAviso) {
        if (textoAviso != null) {
            viewModel.mensajeMostrado()
            alcance.launch { avisos.showSnackbar(textoAviso) }
        }
    }

    Box(Modifier.fillMaxSize()) {
        ContenidoAlertas(
            estado = estado,
            alCambiarFiltro = viewModel::cambiarFiltro,
            alMarcarRevisado = viewModel::marcarRevisado
        )
        SnackbarHost(avisos, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ContenidoAlertas(
    estado: EstadoAlertas,
    alCambiarFiltro: (TipoAlerta?) -> Unit,
    alMarcarRevisado: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FiltrosTipo(
            seleccionado = estado.filtro,
            alElegir = alCambiarFiltro,
            modifier = Modifier.padding(top = 12.dp)
        )
        when {
            estado.cargando -> IndicadorCarga()
            estado.error -> Text(
                text = stringResource(R.string.error_carga),
                color = MaterialTheme.colorScheme.error
            )
            estado.visibles.isEmpty() -> Text(
                text = stringResource(
                    if (estado.filtro == null) R.string.alertas_vacio else R.string.alertas_vacio_filtro
                ),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // key: identifica cada fila por su alerta. Así animateItem sabe cuál entra,
                // cuál sale y cuál se mueve al filtrar o al revisar una.
                itemsIndexed(estado.visibles, key = { _, fila -> fila.alerta.id }) { indice, fila ->
                    FilaAlertaTarjeta(
                        fila = fila,
                        primera = indice == 0,
                        ultima = indice == estado.visibles.lastIndex,
                        puedeRevisar = estado.puedeRevisar,
                        revisando = estado.revisandoId == fila.alerta.id,
                        alMarcarRevisado = { alMarcarRevisado(fila.alerta.id) },
                        // Fundido al aparecer o desaparecer y desplazamiento suave de las demás.
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

/** Chips Todas / Stock bajo / Descalce / Reposición. El elegido muestra un ✓ además del color. */
@Composable
private fun FiltrosTipo(
    seleccionado: TipoAlerta?,
    alElegir: (TipoAlerta?) -> Unit,
    modifier: Modifier = Modifier
) {
    // null representa "Todas".
    val opciones: List<Pair<TipoAlerta?, Int>> = listOf(
        null to R.string.alertas_filtro_todas,
        TipoAlerta.STOCK_BAJO to R.string.alertas_filtro_stock_bajo,
        TipoAlerta.DESCALCE to R.string.alertas_filtro_descalce,
        TipoAlerta.REPOSICION to R.string.alertas_filtro_reposicion
    )
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opciones.forEach { (tipo, texto) ->
            val elegido = tipo == seleccionado
            FilterChip(
                selected = elegido,
                onClick = { alElegir(tipo) },
                label = { Text(stringResource(texto)) },
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
}

/**
 * Una alerta. Como en el mockup, las filas forman un solo bloque blanco: solo la primera
 * redondea las esquinas de arriba y solo la última las de abajo; entre filas hay una línea.
 */
@Composable
private fun FilaAlertaTarjeta(
    fila: FilaAlerta,
    primera: Boolean,
    ultima: Boolean,
    puedeRevisar: Boolean,
    revisando: Boolean,
    alMarcarRevisado: () -> Unit,
    modifier: Modifier = Modifier
) {
    val radio = 16.dp
    val forma = RoundedCornerShape(
        topStart = if (primera) radio else 0.dp,
        topEnd = if (primera) radio else 0.dp,
        bottomStart = if (ultima) radio else 0.dp,
        bottomEnd = if (ultima) radio else 0.dp
    )
    val alerta = fila.alerta
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = forma,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp
    ) {
        Column {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EtiquetaAlerta(alerta)
                Text(text = fila.nombreProducto, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = detalleDe(fila),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                alerta.observacion?.let {
                    Text(
                        text = stringResource(R.string.alertas_observacion, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (alerta.tipo == TipoAlerta.DESCALCE && puedeRevisar) {
                    OutlinedButton(
                        onClick = alMarcarRevisado,
                        enabled = !revisando,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.alertas_marcar_revisado), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            if (!ultima) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** Línea gris bajo el nombre: dónde está y el dato clave de cada tipo de alerta. */
@Composable
private fun detalleDe(fila: FilaAlerta): String {
    val alerta = fila.alerta
    return when (alerta.tipo) {
        TipoAlerta.REPOSICION -> stringResource(
            R.string.alertas_detalle_reposicion, alerta.ubicacionId, alerta.declaradaPorId.orEmpty()
        )
        TipoAlerta.DESCALCE -> {
            val fecha = Fechas.fechaHoraCortaChile(alerta.fechaUtc)
            if (fila.operadorConteo != null) {
                stringResource(R.string.alertas_detalle_descalce, alerta.ubicacionId, fecha, fila.operadorConteo)
            } else {
                stringResource(R.string.alertas_detalle_descalce_sin_operador, alerta.ubicacionId, fecha)
            }
        }
        TipoAlerta.STOCK_BAJO -> stringResource(
            R.string.alertas_detalle_stock, alerta.ubicacionId, fila.stockActual ?: 0
        )
    }
}

/**
 * Píldora con el tipo de alerta en texto (nunca solo color). Los colores de stock
 * son los mismos de los chips de estado; la reposición usa el color de marca.
 */
@Composable
private fun EtiquetaAlerta(alerta: Alerta) {
    val marca = ColoresEstado(
        texto = MaterialTheme.colorScheme.onPrimaryContainer,
        fondo = MaterialTheme.colorScheme.primaryContainer
    )
    val (texto: String, colores: ColoresEstado) = when (alerta.tipo) {
        TipoAlerta.REPOSICION -> stringResource(R.string.alertas_etiqueta_reposicion) to marca
        TipoAlerta.DESCALCE -> stringResource(
            R.string.alertas_etiqueta_descalce, conSigno(alerta.diferencia ?: 0)
        ) to coloresDe(EstadoStock.CRITICO)
        TipoAlerta.STOCK_BAJO -> {
            val nivel = alerta.nivel ?: EstadoStock.BAJO
            stringResource(etiquetaNivel(nivel)) to coloresDe(nivel)
        }
    }
    Surface(shape = RoundedCornerShape(50), color = colores.fondo, contentColor = colores.texto) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            color = Color.Unspecified,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@StringRes
private fun etiquetaNivel(nivel: EstadoStock): Int = when (nivel) {
    EstadoStock.SIN_STOCK -> R.string.alertas_etiqueta_sin_stock
    EstadoStock.CRITICO -> R.string.alertas_etiqueta_critico
    // NORMAL no debería tener alerta abierta; si llegara, se muestra como stock bajo.
    EstadoStock.BAJO, EstadoStock.NORMAL -> R.string.alertas_etiqueta_bajo
}

@Preview(showBackground = true)
@Composable
private fun ContenidoAlertasPreview() {
    val ahora = Instant.parse("2026-10-18T17:00:00Z")
    val filas = listOf(
        FilaAlerta(
            Alerta("ALE-0006", TipoAlerta.REPOSICION, "T-ST-1", "MU-01", EstadoAlerta.ABIERTA, ahora, declaradaPorId = "EB-001"),
            "Silla de escritorio"
        ),
        FilaAlerta(
            Alerta("ALE-0002", TipoAlerta.DESCALCE, "T-ST-2", "RM-02", EstadoAlerta.ABIERTA, ahora, diferencia = -4),
            "Jeans tiro alto", operadorConteo = "OP-001"
        ),
        FilaAlerta(
            Alerta("ALE-0004", TipoAlerta.STOCK_BAJO, "T-ST-1", "RH-01", EstadoAlerta.ABIERTA, ahora, nivel = EstadoStock.CRITICO),
            "Polera de algodón básica, talla M", stockActual = 3
        )
    )
    BYTOTheme {
        ContenidoAlertas(
            estado = EstadoAlertas(cargando = false, usuarioId = "EB-001", todas = filas, visibles = filas, puedeRevisar = true),
            alCambiarFiltro = {},
            alMarcarRevisado = {}
        )
    }
}
