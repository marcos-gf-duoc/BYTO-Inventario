package com.byto.inventario.ui.navigation

import androidx.annotation.StringRes
import com.byto.inventario.R
import com.byto.inventario.data.model.Perfil

/**
 * Las 16 pantallas de la app. Cada una tiene:
 * - [ruta]: el texto que usa Navigation Compose para identificarla (como una URL interna).
 * - [codigo]: el código del diseño (P01…P16), útil para conversar sobre ella.
 * - [titulo]: el título visible, desde strings.xml.
 *
 * Por ahora ninguna ruta lleva argumentos; cuando se implementen pantallas como
 * Detalle (P07) se agregará, por ejemplo, el código del producto a su ruta.
 */
enum class Pantalla(val ruta: String, val codigo: String, @StringRes val titulo: Int) {
    LOGIN("login", "P01", R.string.titulo_p01),
    SELECCION_UBICACION("seleccion_ubicacion", "P02", R.string.titulo_p02),
    INICIO_OPERADOR("inicio_operador", "P03", R.string.titulo_p03),
    INICIO_ENCARGADO("inicio_encargado", "P04", R.string.titulo_p04),
    INICIO_GERENTE("inicio_gerente", "P05", R.string.titulo_p05),
    INVENTARIO("inventario", "P06", R.string.titulo_p06),
    DETALLE_PRODUCTO("detalle_producto", "P07", R.string.titulo_p07),
    BUSCAR_CODIGO("buscar_codigo", "P08", R.string.titulo_p08),
    REGISTRAR_MOVIMIENTO("registrar_movimiento", "P09", R.string.titulo_p09),
    CONFIRMACION_MOVIMIENTO("confirmacion_movimiento", "P10", R.string.titulo_p10),
    ALERTAS("alertas", "P11", R.string.titulo_p11),
    DECLARAR_REPOSICION("declarar_reposicion", "P12", R.string.titulo_p12),
    HISTORIAL("historial", "P13", R.string.titulo_p13),
    ADMINISTRAR_UBICACIONES("administrar_ubicaciones", "P14", R.string.titulo_p14),
    EXPORTAR_REPORTE("exportar_reporte", "P15", R.string.titulo_p15),
    SIN_CONEXION("sin_conexion", "P16", R.string.titulo_p16);

    companion object {
        /** Busca la pantalla a partir de su ruta (la que informa el NavController). */
        fun desdeRuta(ruta: String?): Pantalla? = entries.find { it.ruta == ruta }

        /** Pantallas sin barras: aún no hay sesión, o no se puede usar la app. */
        val sinBarras = setOf(LOGIN, SELECCION_UBICACION, SIN_CONEXION)

        /** Pantallas raíz de la barra inferior: no muestran flecha "Volver". */
        val raices = setOf(INICIO_OPERADOR, INICIO_ENCARGADO, INICIO_GERENTE, INVENTARIO, ALERTAS, HISTORIAL)

        /** Cada perfil tiene su propio inicio. */
        fun inicioDe(perfil: Perfil?): Pantalla = when (perfil) {
            Perfil.OPERADOR -> INICIO_OPERADOR
            Perfil.ENCARGADO_BODEGA -> INICIO_ENCARGADO
            Perfil.GERENTE -> INICIO_GERENTE
            // Sin sesión no hay inicio posible: se vuelve al login.
            null -> LOGIN
        }
    }
}
