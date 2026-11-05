package com.byto.inventario.di

import android.content.Context
import com.byto.inventario.data.local.BytoDatabase
import com.byto.inventario.data.local.LocalInventarioRepository
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.SesionRepository
import com.byto.inventario.data.repository.SimuladorConexion
import com.byto.inventario.dispositivo.EscritorArchivos
import com.byto.inventario.dispositivo.Vibrador

/**
 * Crea a mano las dependencias de la app, una sola vez (inyección manual, sin Hilt ni Koin).
 * Los ViewModels las reciben desde aquí en vez de crearlas ellos mismos.
 *
 * Para cambiar el origen de los datos (por ejemplo, al futuro repositorio remoto)
 * basta con cambiar la línea de [repositorioLocal]: los ViewModels no se enteran.
 *
 * @param context la Application; Room lo necesita para ubicar el archivo de la base,
 * y los recursos del teléfono (como el vibrador) para obtener sus servicios.
 */
class AppContainer(context: Context) {

    private val baseDatos = BytoDatabase.crear(context)

    private val repositorioLocal = LocalInventarioRepository(baseDatos)

    /** Lo que deben usar los ViewModels: la interfaz, no la implementación. */
    val inventarioRepository: InventarioRepository = repositorioLocal

    /** El mismo repositorio, visto solo como interruptor "sin conexión" (P01 y P16). */
    val simuladorConexion: SimuladorConexion = repositorioLocal

    val sesionRepository = SesionRepository()

    val vibrador = Vibrador(context)

    val escritorArchivos = EscritorArchivos(context)
}
