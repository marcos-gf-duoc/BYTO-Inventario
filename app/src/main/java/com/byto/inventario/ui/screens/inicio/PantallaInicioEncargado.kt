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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byto.inventario.R
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

/** P04 Inicio Encargado. */
@Composable
fun PantallaInicioEncargado(
    alAbrir: (Pantalla) -> Unit,
    alSinConexion: () -> Unit,
    alSinSesion: () -> Unit,
    viewModel: InicioEncargadoViewModel = viewModel(factory = FabricaViewModels.Factory)
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

    ContenidoInicioEncargado(estado = estado, alAbrir = alAbrir)
}

@Composable
private fun ContenidoInicioEncargado(estado: EstadoInicioEncargado, alAbrir: (Pantalla) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.inicio_saludo, estado.encargadoId),
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
                    val tarjetas = listOf(
                        estado.alertasAbiertas to R.string.encargado_alertas_abiertas,
                        estado.descalcesPorRevisar to R.string.encargado_descalces,
                        estado.reposicionesAbiertas to R.string.encargado_reposiciones
                    )
                    tarjetas.forEach { (cantidad, texto) ->
                        TarjetaResumen(
                            valor = cantidad.toString(),
                            descripcion = stringResource(texto),
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.encargado_tiendas),
                    style = MaterialTheme.typography.titleMedium
                )
                estado.tiendasACargo.forEach { tienda -> FilaTienda(tienda) }
            }
        }

        Text(
            text = stringResource(R.string.inicio_accesos),
            style = MaterialTheme.typography.titleMedium
        )
        CuadriculaAccesos(
            listOf(
                Acceso(stringResource(R.string.acceso_inventario), R.drawable.ic_inventario) {
                    alAbrir(Pantalla.INVENTARIO)
                },
                Acceso(stringResource(R.string.acceso_alertas), R.drawable.ic_alertas) {
                    alAbrir(Pantalla.ALERTAS)
                },
                Acceso(stringResource(R.string.acceso_declarar_reposicion), R.drawable.ic_reposicion) {
                    alAbrir(Pantalla.DECLARAR_REPOSICION)
                }
            )
        )
    }
}

/** Tienda que el Encargado solo puede consultar (no es clickeable). */
@Composable
private fun FilaTienda(tienda: Ubicacion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "${tienda.id} · ${tienda.nombre}", style = MaterialTheme.typography.bodyLarge)
            EtiquetaTexto(stringResource(R.string.seleccion_solo_consulta))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContenidoInicioEncargadoPreview() {
    BYTOTheme {
        ContenidoInicioEncargado(
            estado = EstadoInicioEncargado(
                cargando = false,
                encargadoId = "EB-001",
                alertasAbiertas = 7,
                descalcesPorRevisar = 1,
                reposicionesAbiertas = 1,
                tiendasACargo = listOf(
                    Ubicacion("T-ST-1", "BYTO Santiago Centro", TipoUbicacion.TIENDA, "B-ST-1", true)
                )
            ),
            alAbrir = {}
        )
    }
}
