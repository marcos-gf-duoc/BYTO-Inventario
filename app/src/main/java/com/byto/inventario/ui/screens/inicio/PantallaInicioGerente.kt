package com.byto.inventario.ui.screens.inicio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byto.inventario.R
import com.byto.inventario.data.model.ResumenUbicacion
import com.byto.inventario.data.model.TipoUbicacion
import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.ui.components.Acceso
import com.byto.inventario.ui.components.CuadriculaAccesos
import com.byto.inventario.ui.components.EtiquetaTexto
import com.byto.inventario.ui.components.IndicadorCarga
import com.byto.inventario.ui.components.TarjetaResumen
import com.byto.inventario.ui.navigation.Pantalla
import com.byto.inventario.ui.theme.BYTOTheme

/** P05 Inicio Gerente. */
@Composable
fun PantallaInicioGerente(
    alAbrir: (Pantalla) -> Unit,
    alSinConexion: () -> Unit,
    alSinSesion: () -> Unit,
    viewModel: InicioGerenteViewModel = viewModel(factory = FabricaViewModels.Factory)
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

    ContenidoInicioGerente(estado = estado, alAbrir = alAbrir)
}

@Composable
private fun ContenidoInicioGerente(estado: EstadoInicioGerente, alAbrir: (Pantalla) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.inicio_saludo, estado.gerenteId),
            style = MaterialTheme.typography.headlineSmall
        )

        when {
            estado.cargando -> IndicadorCarga()
            estado.error -> Text(
                text = stringResource(R.string.error_carga),
                color = MaterialTheme.colorScheme.error
            )
            else -> {
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TarjetaResumen(
                        valor = stringResource(
                            R.string.gerente_activas_valor, estado.ubicacionesActivas, estado.totalUbicaciones
                        ),
                        descripcion = stringResource(R.string.gerente_ubicaciones_activas),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    TarjetaResumen(
                        valor = estado.alertasAbiertas.toString(),
                        descripcion = stringResource(R.string.gerente_alertas_abiertas),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    TarjetaResumen(
                        valor = estado.movimientosHoy.toString(),
                        descripcion = stringResource(R.string.gerente_movimientos_hoy),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }

                Text(
                    text = stringResource(R.string.gerente_alertas_por_ubicacion),
                    style = MaterialTheme.typography.titleMedium
                )
                TablaAlertasPorUbicacion(estado.resumenes)
            }
        }

        Text(
            text = stringResource(R.string.inicio_accesos),
            style = MaterialTheme.typography.titleMedium
        )
        CuadriculaAccesos(
            listOf(
                Acceso(stringResource(R.string.acceso_historial), R.drawable.ic_historial) {
                    alAbrir(Pantalla.HISTORIAL)
                },
                Acceso(stringResource(R.string.acceso_ubicaciones), R.drawable.ic_tienda) {
                    alAbrir(Pantalla.ADMINISTRAR_UBICACIONES)
                },
                Acceso(stringResource(R.string.acceso_exportar_reporte), R.drawable.ic_exportar) {
                    alAbrir(Pantalla.EXPORTAR_REPORTE)
                },
                Acceso(stringResource(R.string.acceso_alertas), R.drawable.ic_alertas) {
                    alAbrir(Pantalla.ALERTAS)
                }
            )
        )
    }
}

/** Una fila por ubicación: nombre a la izquierda y cantidad de alertas a la derecha. */
@Composable
private fun TablaAlertasPorUbicacion(resumenes: List<ResumenUbicacion>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        resumenes.forEachIndexed { indice, resumen ->
            if (indice > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    // TalkBack lee la fila completa: "T-ST-1 · BYTO Santiago Centro, 4 alertas".
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${resumen.ubicacion.id} · ${resumen.ubicacion.nombre}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (!resumen.ubicacion.activa) EtiquetaTexto(stringResource(R.string.seleccion_inactiva))
                }
                Text(
                    text = pluralStringResource(
                        R.plurals.gerente_alertas_cantidad, resumen.alertasAbiertas, resumen.alertasAbiertas
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    // En negrita las que tienen alertas, para que destaquen (además del número).
                    fontWeight = if (resumen.alertasAbiertas > 0) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContenidoInicioGerentePreview() {
    BYTOTheme {
        ContenidoInicioGerente(
            estado = EstadoInicioGerente(
                cargando = false,
                gerenteId = "GE-001",
                ubicacionesActivas = 11,
                totalUbicaciones = 12,
                alertasAbiertas = 7,
                movimientosHoy = 0,
                resumenes = listOf(
                    ResumenUbicacion(
                        Ubicacion("T-ST-1", "BYTO Santiago Centro", TipoUbicacion.TIENDA, "B-ST-1", true),
                        totalUnidades = 428, productosNormal = 22, productosBajo = 1,
                        productosCritico = 1, productosSinStock = 1, alertasAbiertas = 4
                    ),
                    ResumenUbicacion(
                        Ubicacion("T-CC-3", "BYTO Concepción Sur", TipoUbicacion.TIENDA, "B-CC-1", false),
                        totalUnidades = 0, productosNormal = 0, productosBajo = 0,
                        productosCritico = 0, productosSinStock = 25, alertasAbiertas = 0
                    )
                )
            ),
            alAbrir = {}
        )
    }
}
