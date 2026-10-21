package com.byto.inventario.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.byto.inventario.R

/** Las 4 secciones de la barra inferior. */
enum class SeccionInferior(@StringRes val texto: Int, @DrawableRes val icono: Int) {
    INICIO(R.string.seccion_inicio, R.drawable.ic_inicio),
    INVENTARIO(R.string.seccion_inventario, R.drawable.ic_inventario),
    ALERTAS(R.string.seccion_alertas, R.drawable.ic_alertas),
    HISTORIAL(R.string.seccion_historial, R.drawable.ic_historial)
}

/**
 * Barra inferior con ícono y texto en cada ítem. Cada ítem ocupa toda la altura
 * de la barra (80 dp), más que el mínimo táctil de 48 dp.
 *
 * @param seleccionada sección resaltada; null si la pantalla actual no es ninguna de las 4.
 */
@Composable
fun BarraInferior(
    seleccionada: SeccionInferior?,
    alElegir: (SeccionInferior) -> Unit
) {
    NavigationBar {
        SeccionInferior.entries.forEach { seccion ->
            NavigationBarItem(
                selected = seccion == seleccionada,
                onClick = { alElegir(seccion) },
                // El texto visible ya describe el ítem, por eso el ícono es decorativo (null).
                icon = { Icon(painterResource(seccion.icono), contentDescription = null) },
                label = { Text(stringResource(seccion.texto)) }
            )
        }
    }
}
