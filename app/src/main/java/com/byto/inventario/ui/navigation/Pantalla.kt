package com.byto.inventario.ui.navigation

import androidx.annotation.StringRes
import com.byto.inventario.R
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.Perfil

/**
 * Las 17 pantallas de la app. Cada una tiene:
 * - [ruta]: el texto que usa Navigation Compose para identificarla (como una URL interna).
 * - [codigo]: el código del diseño (P01…P17), útil para conversar sobre ella.
 * - [titulo]: el título visible, desde strings.xml.
 * - [patron]: la ruta con sus argumentos, tal como se registra en el NavHost.
 *   Por ejemplo "detalle_producto/{codigoInterno}": al navegar, {codigoInterno}
 *   se reemplaza por un valor real ("detalle_producto/DE-03").
 *   Si la pantalla no recibe argumentos, el patrón es igual a la ruta.
 */
enum class Pantalla(
    val ruta: String,
    val codigo: String,
    @param:StringRes val titulo: Int,
    val patron: String = ruta
) {
    SPLASH("splash", "P00", R.string.titulo_p01),
    LOGIN("login", "P01", R.string.titulo_p01),
    SELECCION_UBICACION("seleccion_ubicacion", "P02", R.string.titulo_p02),
    INICIO_OPERADOR("inicio_operador", "P03", R.string.titulo_p03),
    INICIO_ENCARGADO("inicio_encargado", "P04", R.string.titulo_p04),
    INICIO_GERENTE("inicio_gerente", "P05", R.string.titulo_p05),
    // "?estado=" es opcional: sin él, el inventario se abre sin filtro.
    INVENTARIO("inventario", "P06", R.string.titulo_p06, "inventario?estado={estado}"),
    DETALLE_PRODUCTO("detalle_producto", "P07", R.string.titulo_p07, "detalle_producto/{codigoInterno}"),
    BUSCAR_CODIGO("buscar_codigo", "P08", R.string.titulo_p08),
    REGISTRAR_MOVIMIENTO(
        "registrar_movimiento", "P09", R.string.titulo_p09, "registrar_movimiento/{codigoInterno}"
    ),
    // Recibe el ID del movimiento y, opcionales, los IDs de sus alertas separados por comas.
    CONFIRMACION_MOVIMIENTO(
        "confirmacion_movimiento", "P10", R.string.titulo_p10,
        "confirmacion_movimiento/{movimientoId}?generadas={generadas}&actualizadas={actualizadas}&cerradas={cerradas}"
    ),
    ALERTAS("alertas", "P11", R.string.titulo_p11),
    DECLARAR_REPOSICION("declarar_reposicion", "P12", R.string.titulo_p12),
    HISTORIAL("historial", "P13", R.string.titulo_p13),
    ADMINISTRAR_UBICACIONES("administrar_ubicaciones", "P14", R.string.titulo_p14),
    EXPORTAR_REPORTE("exportar_reporte", "P15", R.string.titulo_p15),
    SIN_CONEXION("sin_conexion", "P16", R.string.titulo_p16),
    // Nueva en E2 (sin mockup): se abre desde P08 y le devuelve el código leído.
    ESCANEAR_CODIGO("escanear_codigo", "P17", R.string.titulo_p17);

    companion object {
        // Nombres de los argumentos, compartidos por las rutas y los ViewModels que los leen.
        const val ARG_ESTADO = "estado"
        const val ARG_CODIGO_INTERNO = "codigoInterno"
        const val ARG_MOVIMIENTO_ID = "movimientoId"
        const val ARG_GENERADAS = "generadas"
        const val ARG_ACTUALIZADAS = "actualizadas"
        const val ARG_CERRADAS = "cerradas"

        /** Clave con la que P17 deja el código leído para que P08 lo recoja al volver. */
        const val RESULTADO_CODIGO_ESCANEADO = "codigoEscaneado"

        /** Busca la pantalla a partir del patrón que informa el NavController. */
        fun desdeRuta(patron: String?): Pantalla? = entries.find { it.patron == patron }

        /** Ruta al inventario, opcionalmente con un filtro de estado ya aplicado. */
        fun rutaInventario(estado: EstadoStock? = null): String =
            if (estado == null) INVENTARIO.ruta else "${INVENTARIO.ruta}?$ARG_ESTADO=${estado.name}"

        /** Ruta al detalle de un producto en la ubicación activa. */
        fun rutaDetalle(codigoInterno: String): String = "${DETALLE_PRODUCTO.ruta}/$codigoInterno"

        /** Ruta para registrar un movimiento de un producto en la ubicación activa. */
        fun rutaRegistrar(codigoInterno: String): String = "${REGISTRAR_MOVIMIENTO.ruta}/$codigoInterno"

        /** Ruta a la confirmación: "confirmacion_movimiento/MOV-0007?generadas=ALE-0008&actualizadas=&cerradas=". */
        fun rutaConfirmacion(
            movimientoId: String,
            generadas: List<String>,
            actualizadas: List<String>,
            cerradas: List<String>
        ): String = "${CONFIRMACION_MOVIMIENTO.ruta}/$movimientoId" +
            "?$ARG_GENERADAS=${generadas.joinToString(",")}" +
            "&$ARG_ACTUALIZADAS=${actualizadas.joinToString(",")}" +
            "&$ARG_CERRADAS=${cerradas.joinToString(",")}"

        /** Pantallas sin barras: aún no hay sesión, o no se puede usar la app. */
        val sinBarras = setOf(SPLASH, LOGIN, SELECCION_UBICACION, SIN_CONEXION)

        /**
         * Barra superior, pero sin barra inferior: formularios a pantalla completa (como en el
         * mockup) y el escáner, que necesita todo el espacio para la cámara.
         */
        val sinBarraInferior = setOf(REGISTRAR_MOVIMIENTO, ESCANEAR_CODIGO)

        /** Sin flecha "Volver": P10 es un resultado final; se sale con sus botones (o con "atrás"). */
        // get(): se calcula al leerlo, así no importa que [raices] se declare más abajo.
        val sinFlechaVolver: Set<Pantalla> get() = raices + CONFIRMACION_MOVIMIENTO

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
