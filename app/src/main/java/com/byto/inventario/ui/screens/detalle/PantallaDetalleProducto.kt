package com.byto.inventario.ui.screens.detalle

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byto.inventario.R
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.Existencia
import com.byto.inventario.data.model.ExistenciaDetalle
import com.byto.inventario.data.model.Movimiento
import com.byto.inventario.data.model.Producto
import com.byto.inventario.data.model.TipoMovimiento
import com.byto.inventario.data.model.TipoUbicacion
import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.domain.Fechas
import com.byto.inventario.domain.ReglasInventario
import com.byto.inventario.ui.components.ChipEstadoStock
import com.byto.inventario.ui.components.IndicadorCarga
import com.byto.inventario.ui.components.textoRes
import com.byto.inventario.ui.theme.BYTOTheme
import java.time.Instant
import kotlin.math.abs

/**
 * P07 Detalle de producto en la ubicación activa.
 * @param alConocerNombre avisa el nombre del producto para mostrarlo como título de la barra.
 * @param alRegistrar abre P09 con el código interno del producto.
 */
@Composable
fun PantallaDetalleProducto(
    alConocerNombre: (String) -> Unit,
    alRegistrar: (String) -> Unit,
    alSinConexion: () -> Unit,
    alSinSesion: () -> Unit,
    viewModel: DetalleProductoViewModel = viewModel(factory = FabricaViewModels.Factory)
) {
    val estado by viewModel.estado.collectAsState()

    // Corre cada vez que la pantalla aparece: al volver a ella, el ViewModel recarga los datos.
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
    // Cuando llega el producto, su nombre pasa a ser el título de la barra superior.
    val nombre = estado.detalle?.producto?.nombre
    LaunchedEffect(nombre) {
        if (nombre != null) alConocerNombre(nombre)
    }

    ContenidoDetalle(estado = estado, alRegistrar = alRegistrar)
}

@Composable
private fun ContenidoDetalle(estado: EstadoDetalle, alRegistrar: (String) -> Unit) {
    val detalle = estado.detalle
    Column(modifier = Modifier.fillMaxSize()) {
        // weight(1f): lo de arriba se desplaza y el botón queda siempre visible abajo.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                estado.cargando -> IndicadorCarga()
                estado.noEncontrado -> Text(stringResource(R.string.detalle_no_encontrado))
                estado.error || detalle == null -> Text(
                    text = stringResource(R.string.error_carga),
                    color = MaterialTheme.colorScheme.error
                )
                else -> {
                    TarjetaStock(detalle)
                    TarjetaDatos(detalle, estado.ubicacion)
                    Text(
                        text = stringResource(R.string.detalle_ultimos_movimientos),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    if (estado.movimientos.isEmpty()) {
                        Text(
                            text = stringResource(R.string.detalle_sin_movimientos),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        estado.movimientos.forEach { FilaMovimiento(it) }
                    }
                }
            }
        }

        // Pie: botón para el Operador en sus tiendas; aviso de solo consulta para los demás.
        if (detalle != null) {
            if (estado.puedeRegistrar) {
                Button(
                    onClick = { alRegistrar(detalle.producto.codigoInterno) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .heightIn(min = 56.dp)
                ) {
                    // Decorativo: el texto del botón ya dice la acción.
                    Icon(
                        painterResource(R.drawable.ic_movimiento),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.detalle_registrar), style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Text(
                    text = stringResource(R.string.detalle_solo_consulta),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

/** Stock grande con su estado (chip con texto) y los umbrales de referencia. */
@Composable
private fun TarjetaStock(detalle: ExistenciaDetalle) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.detalle_stock_actual),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = detalle.existencia.stock.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.detalle_unidades),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
                ChipEstadoStock(detalle.estado)
            }
            Text(
                text = stringResource(
                    R.string.detalle_umbrales, detalle.existencia.stockMinimo, ReglasInventario.UMBRAL_BAJO
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Datos fijos del producto en filas "etiqueta … valor". */
@Composable
private fun TarjetaDatos(detalle: ExistenciaDetalle, ubicacion: Ubicacion?) {
    val filas = listOf(
        stringResource(R.string.detalle_codigo_interno) to detalle.producto.codigoInterno,
        stringResource(R.string.detalle_ean) to detalle.producto.ean13,
        stringResource(R.string.detalle_categoria) to detalle.producto.categoria,
        stringResource(R.string.detalle_ubicacion) to (ubicacion?.let { "${it.id} · ${it.nombre}" } ?: "")
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        filas.forEachIndexed { indice, (etiqueta, valor) ->
            if (indice > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = etiqueta,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = valor,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                )
            }
        }
    }
}

/** "Salida −13" y debajo "16-10-2026 15:40 · OP-001 · Ventas del día" (hora de Chile). */
@Composable
private fun FilaMovimiento(movimiento: Movimiento) {
    val cantidad = when (movimiento.tipo) {
        TipoMovimiento.ENTRADA -> "+${movimiento.cantidad}"
        // "−" es el signo menos tipográfico, más legible que el guion "-".
        TipoMovimiento.SALIDA -> "−${movimiento.cantidad}"
        TipoMovimiento.AJUSTE ->
            if (movimiento.cantidad > 0) "+${movimiento.cantidad}" else "−${abs(movimiento.cantidad)}"
        TipoMovimiento.CONTEO ->
            stringResource(R.string.detalle_conteo_cambio, movimiento.stockAntes, movimiento.stockDespues)
    }
    val detalle = listOfNotNull(
        Fechas.fechaHoraChile(movimiento.fechaUtc),
        movimiento.operadorId,
        movimiento.motivo,
        movimiento.observacion
    ).joinToString(" · ")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decorativo.
            Icon(
                painterResource(R.drawable.ic_movimiento),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = stringResource(
                        R.string.detalle_movimiento_titulo, stringResource(movimiento.tipo.textoRes()), cantidad
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContenidoDetallePreview() {
    BYTOTheme {
        ContenidoDetalle(
            estado = EstadoDetalle(
                cargando = false,
                detalle = ExistenciaDetalle(
                    Existencia("T-ST-1", "DE-03", 7),
                    Producto("2000000000084", "DE-03", "Mat de yoga", "Deportes"),
                    EstadoStock.BAJO
                ),
                ubicacion = Ubicacion("T-ST-1", "BYTO Santiago Centro", TipoUbicacion.TIENDA, "B-ST-1", true),
                movimientos = listOf(
                    Movimiento(
                        "MOV-0002", TipoMovimiento.SALIDA, "T-ST-1", "DE-03", 13, 20, 7,
                        Instant.parse("2026-10-16T18:40:00Z"), "OP-001", observacion = "Ventas del día"
                    )
                ),
                puedeRegistrar = true
            ),
            alRegistrar = {}
        )
    }
}
