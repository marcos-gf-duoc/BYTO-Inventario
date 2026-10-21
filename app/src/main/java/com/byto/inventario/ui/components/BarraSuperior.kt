package com.byto.inventario.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.byto.inventario.R

/**
 * Barra superior común: título de la pantalla, un subtítulo debajo y dos acciones.
 * Los IconButton de Material miden 48 dp, el mínimo táctil exigido.
 *
 * @param subtitulo segunda línea: la ubicación activa ("T-ST-1 · BYTO Santiago Centro") u otro
 * texto propio de la pantalla; null = no se muestra.
 * @param alVolver si no es null, aparece la flecha "Volver" a la izquierda.
 */
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar aún se marca como experimental en Material 3.
@Composable
fun BarraSuperior(
    titulo: String,
    subtitulo: String?,
    alCambiarUbicacion: () -> Unit,
    alCerrarSesion: () -> Unit,
    alVolver: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Column {
                Text(text = titulo, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitulo != null) {
                    Text(
                        text = subtitulo,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (alVolver != null) {
                IconButton(onClick = alVolver) {
                    Icon(
                        painter = painterResource(R.drawable.ic_volver),
                        contentDescription = stringResource(R.string.accion_volver)
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = alCambiarUbicacion) {
                Icon(
                    painter = painterResource(R.drawable.ic_ubicacion),
                    contentDescription = stringResource(R.string.accion_cambiar_ubicacion)
                )
            }
            IconButton(onClick = alCerrarSesion) {
                Icon(
                    painter = painterResource(R.drawable.ic_cerrar_sesion),
                    contentDescription = stringResource(R.string.accion_cerrar_sesion)
                )
            }
        },
        // Morado de marca con texto e íconos blancos (contraste mayor a 4,5:1).
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}
