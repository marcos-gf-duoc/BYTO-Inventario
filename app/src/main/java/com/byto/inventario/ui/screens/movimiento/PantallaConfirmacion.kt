package com.byto.inventario.ui.screens.movimiento

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.byto.inventario.data.model.Alerta
import com.byto.inventario.data.model.EstadoAlerta
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.Movimiento
import com.byto.inventario.data.model.Producto
import com.byto.inventario.data.model.TipoAlerta
import com.byto.inventario.data.model.TipoMovimiento
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.domain.Fechas
import com.byto.inventario.domain.ReglasInventario
import com.byto.inventario.ui.components.ChipEstadoStock
import com.byto.inventario.ui.components.ColoresEstado
import com.byto.inventario.ui.components.IndicadorCarga
import com.byto.inventario.ui.components.TituloSeccion
import com.byto.inventario.ui.components.coloresDe
import com.byto.inventario.ui.components.conSigno
import com.byto.inventario.ui.components.textoRes
import com.byto.inventario.ui.theme.BYTOTheme
import com.byto.inventario.ui.theme.EstadoNormalTexto
import java.time.Instant

/**
 * P10 Confirmación de movimiento.
 * @param alRegistrarOtro vuelve a P09 del mismo producto (recibe su código interno).
 */
@Composable
fun PantallaConfirmacion(
    alRegistrarOtro: (String) -> Unit,
    alVolverInventario: () -> Unit,
    alSinConexion: () -> Unit,
    alSinSesion: () -> Unit,
    viewModel: ConfirmacionViewModel = viewModel(factory = FabricaViewModels.Factory)
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

    ContenidoConfirmacion(estado, alRegistrarOtro, alVolverInventario)
}

@Composable
private fun ContenidoConfirmacion(
    estado: EstadoConfirmacion,
    alRegistrarOtro: (String) -> Unit,
    alVolverInventario: () -> Unit
) {
    val movimiento = estado.movimiento
    val producto = estado.producto
    when {
        estado.cargando -> IndicadorCarga()
        estado.error || movimiento == null || producto == null -> Text(
            text = stringResource(R.string.error_carga),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
        else -> Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Encabezado()
            TarjetaResumen(movimiento, producto)

            TituloSeccion(stringResource(R.string.confirmacion_alertas))
            if (estado.alertas.isEmpty()) {
                Text(stringResource(R.string.confirmacion_sin_alertas), style = MaterialTheme.typography.bodyLarge)
            } else {
                estado.alertas.forEach { TarjetaAlerta(it, producto.nombre) }
            }

            OutlinedButton(
                // Texto en el color de marca, como en el mockup (por defecto Material lo deja gris).
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                onClick = { alRegistrarOtro(movimiento.codigoInterno) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(min = 56.dp)
            ) {
                Text(stringResource(R.string.confirmacion_registrar_otro), style = MaterialTheme.typography.titleMedium)
            }
            Button(
                onClick = alVolverInventario,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                Text(stringResource(R.string.confirmacion_volver_inventario), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** Círculo verde con ✓ (decorativo) y el título, que es lo que lee TalkBack. */
@Composable
private fun Encabezado() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .size(88.dp)
                .background(EstadoNormalTexto, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.size(56.dp)
            )
        }
        Text(
            text = stringResource(R.string.confirmacion_titulo),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

/** Filas "etiqueta … valor": tipo, producto, cantidad, stock antes → después y hora de Chile. */
@Composable
private fun TarjetaResumen(movimiento: Movimiento, producto: Producto) {
    val cantidad = when (movimiento.tipo) {
        TipoMovimiento.CONTEO -> stringResource(R.string.confirmacion_contado, movimiento.cantidad)
        // El ajuste lleva signo (+2 / −2); entrada y salida se entienden por el tipo.
        TipoMovimiento.AJUSTE -> stringResource(R.string.confirmacion_unidades, conSigno(movimiento.cantidad))
        else -> stringResource(R.string.confirmacion_unidades, movimiento.cantidad.toString())
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Fila(stringResource(R.string.confirmacion_tipo)) { Valor(stringResource(movimiento.tipo.textoRes())) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Fila(stringResource(R.string.confirmacion_producto)) {
            Valor(stringResource(R.string.confirmacion_producto_valor, producto.nombre, producto.codigoInterno))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Fila(stringResource(R.string.confirmacion_cantidad)) { Valor(cantidad) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Fila(stringResource(R.string.confirmacion_stock)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Valor("${movimiento.stockAntes} → ${movimiento.stockDespues}")
                ChipEstadoStock(
                    ReglasInventario.estadoDe(movimiento.stockDespues),
                    Modifier.padding(start = 12.dp)
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Fila(stringResource(R.string.confirmacion_hora)) { Valor(Fechas.fechaHoraChile(movimiento.fechaUtc)) }
    }
}

@Composable
private fun Fila(etiqueta: String, valor: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp)
        )
        valor()
    }
}

@Composable
private fun Valor(texto: String) {
    Text(text = texto, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
}

/**
 * Una alerta afectada por el movimiento: título con su cambio ("Stock bajo (actualizada)")
 * y una frase que lo explica. El color acompaña, pero el texto siempre dice qué pasó.
 */
@Composable
private fun TarjetaAlerta(item: AlertaConCambio, nombreProducto: String) {
    val alerta = item.alerta
    val minimo = ReglasInventario.UMBRAL_NORMAL
    val nombre = stringResource(nombreDe(alerta))
    val titulo = when (item.cambio) {
        CambioAlerta.NUEVA -> stringResource(R.string.alerta_nueva, nombre)
        CambioAlerta.ACTUALIZADA -> stringResource(R.string.alerta_actualizada, nombre)
        CambioAlerta.CERRADA -> stringResource(R.string.alerta_cerrada, nombre)
    }
    val detalle = when (alerta.tipo) {
        TipoAlerta.STOCK_BAJO -> when (item.cambio) {
            CambioAlerta.NUEVA -> stringResource(R.string.alerta_detalle_bajo_nueva, nombreProducto, minimo)
            CambioAlerta.ACTUALIZADA -> stringResource(R.string.alerta_detalle_bajo_actualizada, nombreProducto, minimo)
            CambioAlerta.CERRADA -> stringResource(R.string.alerta_detalle_bajo_cerrada, nombreProducto, minimo)
        }
        TipoAlerta.DESCALCE -> stringResource(R.string.alerta_detalle_descalce, conSigno(alerta.diferencia ?: 0))
        TipoAlerta.REPOSICION -> if (item.cambio == CambioAlerta.CERRADA) {
            stringResource(R.string.alerta_detalle_reposicion_cerrada, nombreProducto)
        } else {
            stringResource(R.string.alerta_detalle_reposicion, nombreProducto)
        }
    }
    val colores = coloresDeAlerta(item)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = colores.fondo, contentColor = colores.texto)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            // Decorativo: ✓ si se cerró, ⚠ si sigue abierta.
            Icon(
                painterResource(if (item.cambio == CambioAlerta.CERRADA) R.drawable.ic_check else R.drawable.ic_advertencia),
                contentDescription = null
            )
            Column(modifier = Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(detalle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Nombre de la alerta; las de stock bajo se nombran según su nivel actual. */
private fun nombreDe(alerta: Alerta): Int = when (alerta.tipo) {
    TipoAlerta.STOCK_BAJO -> when (alerta.nivel) {
        EstadoStock.CRITICO -> R.string.alerta_stock_critico
        EstadoStock.SIN_STOCK -> R.string.alerta_sin_stock
        else -> R.string.alerta_stock_bajo
    }
    TipoAlerta.DESCALCE -> R.string.alerta_descalce
    TipoAlerta.REPOSICION -> R.string.alerta_reposicion
}

/** Verde si se cerró; rojo si es crítica o descalce; ámbar en los demás casos. */
private fun coloresDeAlerta(item: AlertaConCambio): ColoresEstado = when {
    item.cambio == CambioAlerta.CERRADA -> coloresDe(EstadoStock.NORMAL)
    item.alerta.tipo == TipoAlerta.DESCALCE -> coloresDe(EstadoStock.CRITICO)
    item.alerta.nivel == EstadoStock.CRITICO || item.alerta.nivel == EstadoStock.SIN_STOCK ->
        coloresDe(EstadoStock.CRITICO)
    else -> coloresDe(EstadoStock.BAJO)
}

@Preview(showBackground = true)
@Composable
private fun ContenidoConfirmacionPreview() {
    BYTOTheme {
        ContenidoConfirmacion(
            estado = EstadoConfirmacion(
                cargando = false,
                movimiento = Movimiento(
                    "MOV-0007", TipoMovimiento.SALIDA, "T-ST-1", "DE-03", 2, 7, 5,
                    Instant.parse("2026-10-21T13:15:00Z"), "OP-001"
                ),
                producto = Producto("2000000000084", "DE-03", "Mat de yoga", "Deportes"),
                alertas = listOf(
                    AlertaConCambio(
                        Alerta(
                            "ALE-0001", TipoAlerta.STOCK_BAJO, "T-ST-1", "DE-03", EstadoAlerta.ABIERTA,
                            Instant.parse("2026-10-16T18:40:00Z"), nivel = EstadoStock.BAJO
                        ),
                        CambioAlerta.ACTUALIZADA
                    )
                )
            ),
            alRegistrarOtro = {},
            alVolverInventario = {}
        )
    }
}
