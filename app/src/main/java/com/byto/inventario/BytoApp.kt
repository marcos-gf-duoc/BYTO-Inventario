package com.byto.inventario

import android.app.Application
import com.byto.inventario.di.AppContainer

/**
 * Clase Application: Android la crea una vez, antes que cualquier pantalla, y vive
 * mientras viva la app. Por eso es el lugar del AppContainer: los datos en memoria
 * sobreviven a rotaciones de pantalla y cambios entre Activities.
 */
class BytoApp : Application() {

    // lateinit: se asigna en onCreate, no en el constructor.
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}
