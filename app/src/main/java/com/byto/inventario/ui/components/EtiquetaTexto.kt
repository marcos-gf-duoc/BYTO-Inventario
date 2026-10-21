package com.byto.inventario.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.byto.inventario.ui.theme.EstadoNormalFondo
import com.byto.inventario.ui.theme.EstadoNormalTexto

/** Combinaciones de color de las etiquetas; todas con contraste de texto ≥ 4,5:1. */
enum class EstiloEtiqueta { NEUTRA, MARCA, POSITIVA }

/**
 * Etiqueta informativa en forma de píldora, por ejemplo "Tienda", "Activa" o "Solo consulta".
 * No es clickeable: solo agrega información en texto (nunca solo color).
 */
@Composable
fun EtiquetaTexto(
    texto: String,
    modifier: Modifier = Modifier,
    estilo: EstiloEtiqueta = EstiloEtiqueta.NEUTRA
) {
    val (fondo, contenido) = when (estilo) {
        EstiloEtiqueta.NEUTRA -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        EstiloEtiqueta.MARCA -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        EstiloEtiqueta.POSITIVA -> EstadoNormalFondo to EstadoNormalTexto
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = fondo,
        contentColor = contenido
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            color = Color.Unspecified,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
