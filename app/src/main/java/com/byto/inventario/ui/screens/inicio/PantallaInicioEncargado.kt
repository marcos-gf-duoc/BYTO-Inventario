package com.byto.inventario.ui.screens.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.byto.inventario.ui.components.TituloSeccion
import com.byto.inventario.ui.navigation.Pantalla
import com.byto.inventario.ui.theme.BYTOTheme
import com.byto.inventario.ui.theme.EstadoBajoTexto
import com.byto.inventario.ui.theme.EstadoCriticoTexto

/** P04 Inicio Encargado. */
@Composable
fun PantallaInicioEncargado(
    alAbrir: (Pantalla) -> Unit,
    alSinConexion: () -> Unit,
    alSinSesion: () -> Unit,
    viewModel: InicioEncargadoViewModel = viewModel(factory = FabricaViewModels.Factory)
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
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        when {
            estado.cargando -> IndicadorCarga()
            estado.error -> Text(
                text = stringResource(R.string.error_carga),
                color = MaterialTheme.colorScheme.error
            )
            else -> {
                // El color del número refuerza el significado; el texto de abajo lo dice siempre.
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TarjetaResumen(
                        valor = estado.alertasAbiertas.toString(),
                        descripcion = stringResource(R.string.encargado_alertas_abiertas),
                        colorValor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    TarjetaResumen(
                        valor = estado.descalcesPorRevisar.toString(),
                        descripcion = stringResource(R.string.encargado_descalces),
                        colorValor = EstadoCriticoTexto,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    TarjetaResumen(
                        valor = estado.reposicionesAbiertas.toString(),
                        descripcion = stringResource(R.string.encargado_reposiciones),
                        colorValor = EstadoBajoTexto,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }

                TituloSeccion(stringResource(R.string.encargado_tiendas))
                ListaTiendas(estado.tiendasACargo)
            }
        }

        TituloSeccion(stringResource(R.string.inicio_accesos))
        CuadriculaAccesos(
            listOf(
                Acceso(stringResource(R.string.acceso_inventario), R.drawable.ic_inventario) {
                    alAbrir(Pantalla.INVENTARIO)
                },
                Acceso(stringResource(R.string.acceso_alertas), R.drawable.ic_alertas) {
                    alAbrir(Pantalla.ALERTAS)
                },
                // Queda solo en su fila, así que la cuadrícula lo muestra a todo el ancho.
                Acceso(stringResource(R.string.acceso_declarar_reposicion), R.drawable.ic_movimiento) {
                    alAbrir(Pantalla.DECLARAR_REPOSICION)
                }
            )
        )
    }
}

/** Tiendas que el Encargado solo puede consultar, en una tarjeta con separadores (no son clickeables). */
@Composable
private fun ListaTiendas(tiendas: List<Ubicacion>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        tiendas.forEachIndexed { indice, tienda ->
            if (indice > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decorativo.
                Icon(
                    painterResource(R.drawable.ic_tienda),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(text = tienda.id, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = tienda.nombre,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                EtiquetaTexto(stringResource(R.string.seleccion_solo_consulta))
            }
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
