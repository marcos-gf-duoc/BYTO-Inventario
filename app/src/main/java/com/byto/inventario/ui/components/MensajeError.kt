package com.byto.inventario.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.byto.inventario.R

/**
 * Mensaje de error de un campo de formulario: ícono de advertencia y texto en rojo
 * (nunca solo color). Aparece y desaparece con una animación corta, para que el cambio
 * se note sin distraer.
 *
 * liveRegion: TalkBack lee el mensaje apenas aparece, sin que la persona tenga que buscarlo.
 *
 * @param texto el error a mostrar; null = sin error (el mensaje se oculta).
 */
@Composable
fun MensajeError(texto: String?, modifier: Modifier = Modifier) {
    // Último mensaje mostrado: durante la animación de salida [texto] ya es null,
    // y así se desvanece el mensaje completo en vez de un renglón vacío.
    // Es un objeto simple (no un estado de Compose) porque cambiarlo no debe redibujar nada.
    val ultimo = remember { UltimoTexto() }
    if (texto != null) ultimo.valor = texto

    AnimatedVisibility(
        visible = texto != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 4.dp, top = 4.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            // Decorativo: el texto ya dice que es un error.
            Icon(
                painter = painterResource(R.drawable.ic_advertencia),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = texto ?: ultimo.valor,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/** Guarda el último texto de [MensajeError] entre redibujos. */
private class UltimoTexto(var valor: String = "")
