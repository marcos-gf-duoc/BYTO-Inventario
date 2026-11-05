package com.byto.inventario.ui.screens.ubicaciones

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byto.inventario.R
import com.byto.inventario.data.model.TipoUbicacion
import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.domain.Regiones
import com.byto.inventario.ui.components.IndicadorCarga
import com.byto.inventario.ui.components.TituloSeccion
import com.byto.inventario.ui.theme.BYTOTheme
import kotlinx.coroutines.launch

/**
 * P14 Administrar ubicaciones (solo Gerente): habilita o deshabilita tiendas y bodegas.
 * @param alConocerSubtitulo avisa el texto "11 de 12 activas" para la barra superior.
 */
@Composable
fun PantallaAdministrarUbicaciones(
    alConocerSubtitulo: (String) -> Unit,
    alSinConexion: () -> Unit,
    alSinSesion: () -> Unit,
    viewModel: AdministrarUbicacionesViewModel = viewModel(factory = FabricaViewModels.Factory)
) {
    val estado by viewModel.estado.collectAsState()
    val avisos = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()

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

    val subtitulo = stringResource(R.string.ubicaciones_subtitulo, estado.activas, estado.total)
    LaunchedEffect(subtitulo, estado.cargando) {
        if (!estado.cargando && estado.total > 0) alConocerSubtitulo(subtitulo)
    }

    // Confirmación del cambio. Se lanza aparte para que avisoMostrado() no la cancele.
    val textoAviso = when {
        estado.errorCambio -> stringResource(R.string.ubicaciones_error_cambio)
        estado.cambio?.activa == true -> stringResource(R.string.ubicaciones_activada, estado.cambio!!.ubicacionId)
        estado.cambio?.activa == false -> stringResource(R.string.ubicaciones_desactivada, estado.cambio!!.ubicacionId)
        else -> null
    }
    LaunchedEffect(textoAviso) {
        if (textoAviso != null) {
            viewModel.avisoMostrado()
            alcance.launch { avisos.showSnackbar(textoAviso) }
        }
    }

    Box(Modifier.fillMaxSize()) {
        ContenidoUbicaciones(estado = estado, alTocarInterruptor = viewModel::alTocarInterruptor)
        SnackbarHost(avisos, modifier = Modifier.align(Alignment.BottomCenter))
    }

    estado.porDesactivar?.let { ubicacion ->
        DialogoDesactivar(
            ubicacion = ubicacion,
            alConfirmar = viewModel::confirmarDesactivar,
            alCancelar = viewModel::cancelarDesactivar
        )
    }
}

@Composable
private fun ContenidoUbicaciones(estado: EstadoUbicaciones, alTocarInterruptor: (Ubicacion) -> Unit) {
    when {
        estado.cargando -> IndicadorCarga()
        estado.sinPermiso -> MensajeCentrado(stringResource(R.string.ubicaciones_sin_permiso))
        estado.error -> Text(
            text = stringResource(R.string.error_carga),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(estado.grupos, key = { it.region }) { grupo ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TituloSeccion(grupo.region)
                    TarjetaRegion(grupo, estado.cambiandoId, alTocarInterruptor)
                }
            }
        }
    }
}

/** Una tarjeta por región con una fila por ubicación, separadas por líneas (como en el mockup). */
@Composable
private fun TarjetaRegion(grupo: GrupoRegion, cambiandoId: String?, alTocarInterruptor: (Ubicacion) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        grupo.ubicaciones.forEachIndexed { indice, ubicacion ->
            FilaUbicacion(
                ubicacion = ubicacion,
                habilitada = cambiandoId == null,
                alTocar = { alTocarInterruptor(ubicacion) }
            )
            if (indice < grupo.ubicaciones.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

/**
 * Toda la fila es el interruptor (toggleable con rol Switch): se puede tocar en cualquier parte,
 * mide más de 48 dp y TalkBack la lee como "T-ST-1 · Centro, Activa, interruptor activado".
 * Por eso el Switch no tiene su propio onCheckedChange.
 */
@Composable
private fun FilaUbicacion(ubicacion: Ubicacion, habilitada: Boolean, alTocar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(
                value = ubicacion.activa,
                enabled = habilitada,
                role = Role.Switch,
                onValueChange = { alTocar() }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.ubicaciones_fila, ubicacion.id, Regiones.nombreCorto(ubicacion)),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        // El estado también va en texto (nunca solo el interruptor); cambia con un fundido.
        AnimatedContent(
            targetState = ubicacion.activa,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "estadoUbicacion"
        ) { activa ->
            Text(
                text = stringResource(if (activa) R.string.seleccion_activa else R.string.seleccion_inactiva),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
        Switch(checked = ubicacion.activa, onCheckedChange = null, enabled = habilitada)
    }
}

@Composable
private fun DialogoDesactivar(ubicacion: Ubicacion, alConfirmar: () -> Unit, alCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text(stringResource(R.string.ubicaciones_confirmar_titulo, ubicacion.id)) },
        text = {
            Text(
                stringResource(R.string.ubicaciones_confirmar_texto, "${ubicacion.id} · ${ubicacion.nombre}"),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            TextButton(onClick = alConfirmar) {
                Text(stringResource(R.string.ubicaciones_confirmar_si), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = alCancelar) { Text(stringResource(R.string.ubicaciones_confirmar_no)) }
        }
    )
}

@Composable
private fun MensajeCentrado(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun ContenidoUbicacionesPreview() {
    val grupo = GrupoRegion(
        "Concepción",
        listOf(
            Ubicacion("B-CC-1", "Bodega Regional Concepción", TipoUbicacion.BODEGA, "CM", true),
            Ubicacion("T-CC-3", "BYTO Concepción Sur", TipoUbicacion.TIENDA, "B-CC-1", false)
        )
    )
    BYTOTheme {
        ContenidoUbicaciones(estado = EstadoUbicaciones(cargando = false, grupos = listOf(grupo)), alTocarInterruptor = {})
    }
}
