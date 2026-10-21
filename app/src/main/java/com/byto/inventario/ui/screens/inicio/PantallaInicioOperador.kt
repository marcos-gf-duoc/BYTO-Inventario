package com.byto.inventario.ui.screens.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byto.inventario.R
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.Producto
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.ui.components.Acceso
import com.byto.inventario.ui.components.ChipEstadoStock
import com.byto.inventario.ui.components.CuadriculaAccesos
import com.byto.inventario.ui.components.IndicadorCarga
import com.byto.inventario.ui.components.TarjetaResumen
import com.byto.inventario.ui.navigation.Pantalla
import com.byto.inventario.ui.theme.BYTOTheme
import com.byto.inventario.ui.theme.EstadoBajoFondo
import com.byto.inventario.ui.theme.EstadoBajoTexto

/** P03 Inicio Operador. [alAbrir] navega a la pantalla indicada. */
@Composable
fun PantallaInicioOperador(
    alAbrir: (Pantalla) -> Unit,
    alSinConexion: () -> Unit,
    alSinSesion: () -> Unit,
    viewModel: InicioOperadorViewModel = viewModel(factory = FabricaViewModels.Factory)
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

    ContenidoInicioOperador(estado = estado, alAbrir = alAbrir)
}

@Composable
private fun ContenidoInicioOperador(estado: EstadoInicioOperador, alAbrir: (Pantalla) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.inicio_saludo, estado.operadorId),
            style = MaterialTheme.typography.headlineSmall
        )

        when {
            estado.cargando -> IndicadorCarga()
            estado.error -> Text(
                text = stringResource(R.string.error_carga),
                color = MaterialTheme.colorScheme.error
            )
            else -> {
                if (estado.reposiciones.isNotEmpty()) {
                    AvisoReposiciones(estado.reposiciones)
                }

                Text(
                    text = stringResource(R.string.operador_estado_stock),
                    style = MaterialTheme.typography.titleMedium
                )
                // Cada tarjeta: el número y el chip con el nombre del estado (texto + color).
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tarjetas = listOf(
                        EstadoStock.BAJO to estado.productosBajo,
                        EstadoStock.CRITICO to estado.productosCritico,
                        EstadoStock.SIN_STOCK to estado.productosSinStock
                    )
                    tarjetas.forEach { (estadoStock, cantidad) ->
                        TarjetaResumen(
                            valor = cantidad.toString(),
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            ChipEstadoStock(estadoStock)
                        }
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.inicio_accesos),
            style = MaterialTheme.typography.titleMedium
        )
        CuadriculaAccesos(
            listOf(
                Acceso(stringResource(R.string.acceso_buscar_codigo), R.drawable.ic_buscar) {
                    alAbrir(Pantalla.BUSCAR_CODIGO)
                },
                Acceso(stringResource(R.string.acceso_inventario), R.drawable.ic_inventario) {
                    alAbrir(Pantalla.INVENTARIO)
                },
                // Registrar un movimiento empieza por encontrar el producto, por eso también lleva a P08.
                Acceso(stringResource(R.string.acceso_registrar_movimiento), R.drawable.ic_movimiento) {
                    alAbrir(Pantalla.BUSCAR_CODIGO)
                },
                Acceso(stringResource(R.string.acceso_alertas), R.drawable.ic_alertas) {
                    alAbrir(Pantalla.ALERTAS)
                }
            )
        )
    }
}

/** Aviso destacado en ámbar (color de advertencia) con el detalle en texto. */
@Composable
private fun AvisoReposiciones(productos: List<Producto>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = EstadoBajoFondo, contentColor = EstadoBajoTexto)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            // Decorativo: el título ya explica el aviso.
            Icon(painterResource(R.drawable.ic_reposicion), contentDescription = null)
            Column(
                modifier = Modifier.padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = pluralStringResource(
                        R.plurals.operador_reposiciones_titulo, productos.size, productos.size
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
                productos.forEach { producto ->
                    Text(
                        text = stringResource(
                            R.string.operador_reposicion_producto, producto.codigoInterno, producto.nombre
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = stringResource(R.string.operador_reposiciones_ayuda),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContenidoInicioOperadorPreview() {
    BYTOTheme {
        ContenidoInicioOperador(
            estado = EstadoInicioOperador(
                cargando = false,
                operadorId = "OP-001",
                productosBajo = 1,
                productosCritico = 1,
                productosSinStock = 1,
                reposiciones = listOf(Producto("2000000000213", "MU-01", "Silla de escritorio", "Muebles"))
            ),
            alAbrir = {}
        )
    }
}
