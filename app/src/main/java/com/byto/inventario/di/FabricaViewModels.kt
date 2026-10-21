package com.byto.inventario.di

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.byto.inventario.BytoApp
import com.byto.inventario.ui.navigation.SesionViewModel
import com.byto.inventario.ui.screens.inicio.InicioEncargadoViewModel
import com.byto.inventario.ui.screens.inicio.InicioGerenteViewModel
import com.byto.inventario.ui.screens.inicio.InicioOperadorViewModel
import com.byto.inventario.ui.screens.login.LoginViewModel
import com.byto.inventario.ui.screens.seleccion.SeleccionUbicacionViewModel
import com.byto.inventario.ui.screens.sinconexion.SinConexionViewModel

/**
 * Fábrica única de ViewModels. Android no sabe crear un ViewModel que pide parámetros
 * (un repositorio, por ejemplo), así que aquí se indica cómo crear cada uno,
 * sacando sus dependencias del [AppContainer].
 *
 * Uso en una pantalla: `viewModel(factory = FabricaViewModels.Factory)`.
 */
object FabricaViewModels {

    val Factory = viewModelFactory {
        initializer { SesionViewModel(contenedor().sesionRepository) }
        initializer {
            val c = contenedor()
            LoginViewModel(c.inventarioRepository, c.sesionRepository, c.repositorioFalso)
        }
        initializer {
            val c = contenedor()
            SeleccionUbicacionViewModel(c.inventarioRepository, c.sesionRepository)
        }
        initializer { SinConexionViewModel(contenedor().repositorioFalso) }
        initializer {
            val c = contenedor()
            InicioOperadorViewModel(c.inventarioRepository, c.sesionRepository)
        }
        initializer {
            val c = contenedor()
            InicioEncargadoViewModel(c.inventarioRepository, c.sesionRepository)
        }
        initializer {
            val c = contenedor()
            InicioGerenteViewModel(c.inventarioRepository, c.sesionRepository)
        }
    }

    /** Obtiene el AppContainer desde la Application que Android entrega al crear el ViewModel. */
    private fun CreationExtras.contenedor(): AppContainer =
        (this[APPLICATION_KEY] as BytoApp).container
}
