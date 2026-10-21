package com.byto.inventario.di

import com.byto.inventario.data.fake.FakeInventarioRepository
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.SesionRepository

/**
 * Crea a mano las dependencias de la app, una sola vez (inyección manual, sin Hilt ni Koin).
 * Los ViewModels las reciben desde aquí en vez de crearlas ellos mismos.
 *
 * En E2 bastará con cambiar la línea de [inventarioRepository] por el repositorio remoto.
 */
class AppContainer {

    /** Se guarda con su tipo concreto solo para poder usar el interruptor "sin conexión". */
    val repositorioFalso = FakeInventarioRepository()

    /** Lo que deben usar los ViewModels: la interfaz, no la implementación. */
    val inventarioRepository: InventarioRepository = repositorioFalso

    val sesionRepository = SesionRepository()
}
