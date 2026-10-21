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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byto.inventario.R
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.Producto
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.ui.components.Acceso
import com.byto.inventario.ui.components.CuadriculaAccesos
import com.byto.inventario.ui.components.IndicadorCarga
import com.byto.inventario.ui.components.TarjetaResumen
import com.byto.inventario.ui.components.TituloSeccion
import com.byto.inventario.ui.components.coloresDe
import com.byto.inventario.ui.components.textoRes
import com.byto.inventario.ui.navigation.Pantalla
import com.byto.inventario.ui.theme.BYTOTheme
import com.byto.inventario.ui.theme.EstadoBajoFondo
import com.byto.inventario.ui.theme.EstadoBajoTexto

/**
 * P03 Inicio Operador. [alAbrir] navega a la pantalla indicada;
 * [alVerInventario] abre P06 con un filtro de estado ya aplicado.
 */
@Composable
fun PantallaInicioOperador(
    alAbrir: (Pantalla) -> Unit,
    alVerInventario: (EstadoStock) -> Unit,
    alSinConexion: () -> Unit,
    alSinSesion: () -> Unit,
    viewModel: InicioOperadorViewModel = viewModel(factory = FabricaViewModels.Factory)
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

    ContenidoInicioOperador(estado = estado, alAbrir = alAbrir, alVerInventario = alVerInventario)
}

@Composable
private fun ContenidoInicioOperador(
    estado: EstadoInicioOperador,
    alAbrir: (Pantalla) -> Unit,
    alVerInventario: (EstadoStock) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.inicio_saludo, estado.operadorId),
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
                if (estado.reposiciones.isNotEmpty()) {
                    AvisoReposiciones(estado.reposiciones)
                }

                TituloSeccion(stringResource(R.string.operador_estado_stock))
                // Tarjetas con el color del estado y su nombre en texto (nunca solo color).
                // Al tocarlas se abre el inventario filtrado por ese estado.
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
                        val colores = coloresDe(estadoStock)
                        TarjetaResumen(
                            valor = cantidad.toString(),
                            descripcion = stringResource(estadoStock.textoRes()),
                            colorFondo = colores.fondo,
                            colorContenido = colores.texto,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            alTocar = { alVerInventario(estadoStock) }
                        )
                    }
                }
            }
        }

        TituloSeccion(stringResource(R.string.inicio_accesos))
        CuadriculaAccesos(
            listOf(
                Acceso(stringResource(R.string.acceso_buscar_codigo), R.drawable.ic_codigo_barras) {
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
private fun AvisoReposiciones(reposiciones: List<ReposicionPendiente>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = EstadoBajoFondo, contentColor = EstadoBajoTexto)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            // Decorativo: el título ya explica el aviso.
            Icon(painterResource(R.drawable.ic_advertencia), contentDescription = null)
            Column(
                modifier = Modifier.padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = pluralStringResource(R.plurals.operador_reposiciones_titulo, reposiciones.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                reposiciones.forEach { reposicion ->
                    Text(
                        text = stringResource(
                            R.string.operador_reposicion_producto,
                            reposicion.producto.nombre,
                            reposicion.producto.codigoInterno,
                            reposicion.declaradaPorId
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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
                reposiciones = listOf(
                    ReposicionPendiente(
                        Producto("2000000000213", "MU-01", "Silla de escritorio", "Muebles"),
                        declaradaPorId = "EB-001"
                    )
                )
            ),
            alAbrir = {},
            alVerInventario = {}
        )
    }
}
