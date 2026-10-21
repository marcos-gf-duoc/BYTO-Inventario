package com.byto.inventario.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Esquema de colores de Material 3 con la paleta de BYTO.
 * Los roles que no se indican aquí usan los valores por defecto de Material 3.
 */
private val EsquemaClaro = lightColorScheme(
    primary = MoradoByto,
    onPrimary = TextoSobreMorado,
    background = FondoByto,
    onBackground = TextoPrincipal,
    // "surface" es el fondo de tarjetas y barras; se usa el mismo tono del fondo.
    surface = FondoByto,
    onSurface = TextoPrincipal,
    // Material usa onSurfaceVariant para textos secundarios (subtítulos, ayudas).
    onSurfaceVariant = TextoSecundario
)

/**
 * Tema de la app. Solo hay tema claro, y se omite el "color dinámico" de Android 12+
 * porque reemplazaría la paleta de BYTO por los colores del fondo de pantalla del usuario.
 */
@Composable
fun BYTOTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaClaro,
        typography = Typography,
        content = content
    )
}
