package com.byto.inventario.ui.screens.seleccion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byto.inventario.R
import com.byto.inventario.data.model.Perfil
import com.byto.inventario.data.model.TipoUbicacion
import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.data.model.Usuario
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.ui.components.EtiquetaTexto
import com.byto.inventario.ui.components.IndicadorCarga
import com.byto.inventario.ui.components.textoRes
import com.byto.inventario.ui.theme.BYTOTheme

/** P02 Selección de ubicación: conecta el ViewModel con la navegación. */
@Composable
fun PantallaSeleccionUbicacion(
    alElegir: (Perfil) -> Unit,
    alSinConexion: () -> Unit,
    alSinSesion: () -> Unit,
    viewModel: SeleccionUbicacionViewModel = viewModel(factory = FabricaViewModels.Factory)
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

    ContenidoSeleccion(
        estado = estado,
        alElegir = { opcion -> viewModel.elegir(opcion)?.let(alElegir) }
    )
}

@Composable
private fun ContenidoSeleccion(estado: EstadoSeleccion, alElegir: (OpcionUbicacion) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.titulo_p02),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        estado.usuario?.let { usuario ->
            Text(
                text = stringResource(R.string.seleccion_usuario, usuario.etiqueta, usuario.id),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = stringResource(R.string.seleccion_instruccion),
            style = MaterialTheme.typography.bodyLarge
        )

        when {
            estado.cargando -> IndicadorCarga()
            estado.error -> Text(
                text = stringResource(R.string.error_carga),
                color = MaterialTheme.colorScheme.error
            )
            estado.opciones.isEmpty() -> Text(stringResource(R.string.seleccion_sin_ubicaciones))
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(estado.opciones, key = { it.ubicacion.id }) { opcion ->
                    TarjetaUbicacion(opcion = opcion, alElegir = { alElegir(opcion) })
                }
            }
        }
    }
}

/**
 * Tarjeta de una ubicación. Si no es seleccionable, clickable(enabled = false) hace que
 * TalkBack la anuncie como "desactivada", y la etiqueta "Inactiva" lo dice en texto.
 */
@Composable
private fun TarjetaUbicacion(opcion: OpcionUbicacion, alElegir: () -> Unit) {
    val ubicacion = opcion.ubicacion
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = opcion.seleccionable, role = Role.Button, onClick = alElegir),
        colors = CardDefaults.cardColors(
            containerColor = if (opcion.seleccionable) {
                MaterialTheme.colorScheme.surfaceContainerLowest
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = "${ubicacion.id} · ${ubicacion.nombre}",
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(ubicacion.tipo.textoRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (opcion.soloConsulta) EtiquetaTexto(stringResource(R.string.seleccion_solo_consulta))
                if (!ubicacion.activa) EtiquetaTexto(stringResource(R.string.seleccion_inactiva))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContenidoSeleccionPreview() {
    BYTOTheme {
        ContenidoSeleccion(
            estado = EstadoSeleccion(
                cargando = false,
                usuario = Usuario("GE-001", "Gerente BYTO", Perfil.GERENTE, emptyList()),
                opciones = listOf(
                    OpcionUbicacion(
                        Ubicacion("B-ST-1", "Bodega Regional Santiago", TipoUbicacion.BODEGA, "CM", true),
                        soloConsulta = false, seleccionable = true
                    ),
                    OpcionUbicacion(
                        Ubicacion("T-ST-1", "BYTO Santiago Centro", TipoUbicacion.TIENDA, "B-ST-1", true),
                        soloConsulta = true, seleccionable = true
                    ),
                    OpcionUbicacion(
                        Ubicacion("T-CC-3", "BYTO Concepción Sur", TipoUbicacion.TIENDA, "B-CC-1", false),
                        soloConsulta = false, seleccionable = false
                    )
                )
            ),
            alElegir = {}
        )
    }
}
