package com.byto.inventario.ui.screens.inventario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byto.inventario.R
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.Existencia
import com.byto.inventario.data.model.ExistenciaDetalle
import com.byto.inventario.data.model.Producto
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.ui.components.ChipEstadoStock
import com.byto.inventario.ui.components.IndicadorCarga
import com.byto.inventario.ui.components.textoRes
import com.byto.inventario.ui.theme.BYTOTheme

/** P06 Inventario de la ubicación activa. [alAbrirDetalle] recibe el código interno. */
@Composable
fun PantallaInventario(
    alAbrirDetalle: (String) -> Unit,
    alSinConexion: () -> Unit,
    alSinSesion: () -> Unit,
    viewModel: InventarioViewModel = viewModel(factory = FabricaViewModels.Factory)
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

    ContenidoInventario(
        estado = estado,
        alCambiarBusqueda = viewModel::cambiarBusqueda,
        alCambiarFiltro = viewModel::cambiarFiltro,
        alLimpiar = viewModel::limpiar,
        alAbrirDetalle = alAbrirDetalle
    )
}

@Composable
private fun ContenidoInventario(
    estado: EstadoInventario,
    alCambiarBusqueda: (String) -> Unit,
    alCambiarFiltro: (EstadoStock?) -> Unit,
    alLimpiar: () -> Unit,
    alAbrirDetalle: (String) -> Unit
) {
    // Para cerrar el teclado al tocar "Buscar" en él.
    val foco = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = estado.busqueda,
            onValueChange = alCambiarBusqueda,
            label = { Text(stringResource(R.string.inventario_buscar)) },
            singleLine = true,
            leadingIcon = { Icon(painterResource(R.drawable.ic_buscar), contentDescription = null) },
            trailingIcon = {
                if (estado.busqueda.isNotEmpty()) {
                    IconButton(onClick = { alCambiarBusqueda("") }) {
                        Icon(
                            painterResource(R.drawable.ic_borrar),
                            contentDescription = stringResource(R.string.inventario_borrar_busqueda)
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { foco.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        FiltrosEstado(seleccionado = estado.filtro, alElegir = alCambiarFiltro)

        when {
            estado.cargando -> IndicadorCarga()
            estado.error -> Text(
                text = stringResource(R.string.error_carga),
                color = MaterialTheme.colorScheme.error
            )
            estado.resultados.isEmpty() -> SinResultados(alLimpiar)
            else -> {
                Text(
                    text = stringResource(
                        R.string.inventario_mostrando, estado.resultados.size, estado.todas.size
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(estado.resultados, key = { it.producto.codigoInterno }) { detalle ->
                        FilaExistencia(detalle, alTocar = { alAbrirDetalle(detalle.producto.codigoInterno) })
                    }
                }
            }
        }
    }
}

/**
 * Chips Todos / Normal / Bajo / Crítico / Sin stock. El elegido muestra un ✓ además
 * del color, y Material le da un área táctil de 48 dp aunque se vea más bajo.
 */
@Composable
private fun FiltrosEstado(seleccionado: EstadoStock?, alElegir: (EstadoStock?) -> Unit) {
    // null representa "Todos".
    val opciones: List<EstadoStock?> = listOf(null) + EstadoStock.entries
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opciones.forEach { opcion ->
            val elegido = opcion == seleccionado
            FilterChip(
                selected = elegido,
                onClick = { alElegir(opcion) },
                label = {
                    Text(stringResource(opcion?.textoRes() ?: R.string.inventario_filtro_todos))
                },
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

/** Fila tocable: nombre, código y unidades a la izquierda; estado (con texto) a la derecha. */
@Composable
private fun FilaExistencia(detalle: ExistenciaDetalle, alTocar: () -> Unit) {
    Card(
        onClick = alTocar,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = detalle.producto.nombre, style = MaterialTheme.typography.titleMedium)
                // "TE-01 · 18 u": código interno y unidades en stock.
                Text(
                    text = stringResource(
                        R.string.inventario_codigo_unidades, detalle.producto.codigoInterno, detalle.existencia.stock
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ChipEstadoStock(detalle.estado, Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
private fun SinResultados(alLimpiar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.inventario_sin_resultados),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        TextButton(onClick = alLimpiar, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.inventario_limpiar))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContenidoInventarioPreview() {
    val mat = Producto("2000000000084", "DE-03", "Mat de yoga", "Deportes")
    val polera = Producto("2000000000114", "RH-01", "Polera de algodón básica, talla M", "Ropa Hombre")
    val lista = listOf(
        ExistenciaDetalle(Existencia("T-ST-1", "DE-03", 7), mat, EstadoStock.BAJO),
        ExistenciaDetalle(Existencia("T-ST-1", "RH-01", 3), polera, EstadoStock.CRITICO)
    )
    BYTOTheme {
        ContenidoInventario(
            estado = EstadoInventario(cargando = false, todas = lista, resultados = lista),
            alCambiarBusqueda = {},
            alCambiarFiltro = {},
            alLimpiar = {},
            alAbrirDetalle = {}
        )
    }
}
