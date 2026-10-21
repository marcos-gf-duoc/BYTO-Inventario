package com.byto.inventario.ui.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.ui.components.BarraInferior
import com.byto.inventario.ui.components.BarraSuperior
import com.byto.inventario.ui.components.SeccionInferior
import com.byto.inventario.ui.screens.enconstruccion.PantallaEnConstruccion
import com.byto.inventario.ui.screens.inicio.PantallaInicioEncargado
import com.byto.inventario.ui.screens.inicio.PantallaInicioGerente
import com.byto.inventario.ui.screens.inicio.PantallaInicioOperador
import com.byto.inventario.ui.screens.login.PantallaLogin
import com.byto.inventario.ui.screens.seleccion.PantallaSeleccionUbicacion
import com.byto.inventario.ui.screens.sinconexion.PantallaSinConexion

/**
 * Raíz de la interfaz: barras superior e inferior más el NavHost, que cambia
 * la pantalla del centro según la ruta actual.
 */
@Composable
fun BytoNavegacion(
    navController: NavHostController = rememberNavController(),
    sesionViewModel: SesionViewModel = viewModel(factory = FabricaViewModels.Factory)
) {
    val usuario by sesionViewModel.usuarioActivo.collectAsState()
    val ubicacion by sesionViewModel.ubicacionActiva.collectAsState()

    // Pantalla visible: se recalcula cada vez que se navega.
    val entradaActual by navController.currentBackStackEntryAsState()
    val pantallaActual = Pantalla.desdeRuta(entradaActual?.destination?.route)
    val inicio = Pantalla.inicioDe(usuario?.perfil)
    // null = la pantalla actual no lleva barras (login, selección, sin conexión).
    val pantallaConBarras = pantallaActual?.takeIf { it !in Pantalla.sinBarras }

    // Íconos de la barra de estado (hora, batería): blancos sobre la barra morada,
    // oscuros sobre el fondo claro de login, selección y sin conexión.
    // SideEffect aplica el cambio después de cada redibujo; no es parte del dibujo de Compose.
    val ventana = LocalActivity.current?.window
    val vista = LocalView.current
    SideEffect {
        if (ventana != null) {
            WindowCompat.getInsetsController(ventana, vista).isAppearanceLightStatusBars =
                pantallaConBarras == null
        }
    }

    // Accesos de los inicios: Inventario, Alertas e Historial se abren igual que desde la
    // barra inferior; el resto se apila encima del inicio (y muestra la flecha "Volver").
    val abrir: (Pantalla) -> Unit = { destino ->
        if (destino in Pantalla.raices) {
            navegarComoSeccion(navController, destino, inicio)
        } else {
            navController.navigate(destino.ruta)
        }
    }
    val irASinConexion = { navController.navigate(Pantalla.SIN_CONEXION.ruta) }
    val irALogin = { navController.navegarLimpiandoPila(Pantalla.LOGIN) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (pantallaConBarras != null) {
                BarraSuperior(
                    titulo = stringResource(pantallaConBarras.titulo),
                    ubicacionActiva = ubicacion?.let { "${it.id} · ${it.nombre}" },
                    alCambiarUbicacion = { navController.navigate(Pantalla.SELECCION_UBICACION.ruta) },
                    alCerrarSesion = {
                        sesionViewModel.cerrarSesion()
                        navController.navegarLimpiandoPila(Pantalla.LOGIN)
                    },
                    alVolver = if (pantallaConBarras in Pantalla.raices) null else {
                        { navController.navigateUp() }
                    }
                )
            }
        },
        bottomBar = {
            if (pantallaConBarras != null) {
                BarraInferior(
                    seleccionada = seccionDe(pantallaConBarras),
                    alElegir = { seccion -> irASeccion(navController, seccion, inicio) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Pantalla.LOGIN.ruta,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Pantalla.LOGIN.ruta) {
                PantallaLogin(
                    alIngresar = { navController.navigate(Pantalla.SELECCION_UBICACION.ruta) },
                    alSinConexion = irASinConexion
                )
            }
            composable(Pantalla.SELECCION_UBICACION.ruta) {
                PantallaSeleccionUbicacion(
                    // Al entrar al inicio se borra la pila: "atrás" no vuelve al login ni a la selección.
                    alElegir = { perfil -> navController.navegarLimpiandoPila(Pantalla.inicioDe(perfil)) },
                    alSinConexion = irASinConexion,
                    alSinSesion = irALogin
                )
            }
            composable(Pantalla.INICIO_OPERADOR.ruta) {
                PantallaInicioOperador(alAbrir = abrir, alSinConexion = irASinConexion, alSinSesion = irALogin)
            }
            composable(Pantalla.INICIO_ENCARGADO.ruta) {
                PantallaInicioEncargado(alAbrir = abrir, alSinConexion = irASinConexion, alSinSesion = irALogin)
            }
            composable(Pantalla.INICIO_GERENTE.ruta) {
                PantallaInicioGerente(alAbrir = abrir, alSinConexion = irASinConexion, alSinSesion = irALogin)
            }
            composable(Pantalla.SIN_CONEXION.ruta) {
                // popBackStack vuelve a la pantalla que falló, y esta repite su carga.
                PantallaSinConexion(alReintentar = { navController.popBackStack() })
            }

            // Las demás aún muestran el marcador "En construcción".
            Pantalla.entries
                .filter { it !in implementadas }
                .forEach { pantalla ->
                    composable(pantalla.ruta) { PantallaEnConstruccion(pantalla) }
                }
        }
    }
}

/** Pantallas que ya tienen su versión real (las demás muestran "En construcción"). */
private val implementadas = setOf(
    Pantalla.LOGIN,
    Pantalla.SELECCION_UBICACION,
    Pantalla.SIN_CONEXION,
    Pantalla.INICIO_OPERADOR,
    Pantalla.INICIO_ENCARGADO,
    Pantalla.INICIO_GERENTE
)

/** Navega a [pantalla] borrando toda la pila, para que "atrás" no regrese a lo anterior. */
private fun NavHostController.navegarLimpiandoPila(pantalla: Pantalla) {
    navigate(pantalla.ruta) {
        popUpTo(graph.id) { inclusive = true }
    }
}

/** Qué ítem de la barra inferior se resalta en cada pantalla. */
private fun seccionDe(pantalla: Pantalla?): SeccionInferior? = when (pantalla) {
    Pantalla.INICIO_OPERADOR, Pantalla.INICIO_ENCARGADO, Pantalla.INICIO_GERENTE -> SeccionInferior.INICIO
    Pantalla.INVENTARIO -> SeccionInferior.INVENTARIO
    Pantalla.ALERTAS -> SeccionInferior.ALERTAS
    Pantalla.HISTORIAL -> SeccionInferior.HISTORIAL
    else -> null
}

/**
 * Navegación de la barra inferior:
 * - popUpTo(inicio): no se acumulan pantallas al saltar entre secciones; "atrás" vuelve al inicio.
 * - launchSingleTop: tocar la sección actual no la abre dos veces.
 * No se usa saveState/restoreState a propósito: así cada sección vuelve a cargar sus datos
 * (el stock pudo cambiar) y nunca se restaura una pantalla de otra ubicación o sesión.
 */
private fun irASeccion(navController: NavHostController, seccion: SeccionInferior, inicio: Pantalla) {
    val destino = when (seccion) {
        SeccionInferior.INICIO -> inicio
        SeccionInferior.INVENTARIO -> Pantalla.INVENTARIO
        SeccionInferior.ALERTAS -> Pantalla.ALERTAS
        SeccionInferior.HISTORIAL -> Pantalla.HISTORIAL
    }
    navegarComoSeccion(navController, destino, inicio)
}

/** Navega a una pantalla raíz con las reglas de la barra inferior (ver [irASeccion]). */
private fun navegarComoSeccion(navController: NavHostController, destino: Pantalla, inicio: Pantalla) {
    navController.navigate(destino.ruta) {
        popUpTo(inicio.ruta)
        launchSingleTop = true
    }
}
