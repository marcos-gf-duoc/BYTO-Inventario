package com.byto.inventario.data.repository

/**
 * Interruptor oculto de la demo para simular que no hay conexión con el servidor
 * y así mostrar P16. Mientras está activo, toda llamada del repositorio lanza
 * [SinConexionException].
 *
 * Es una interfaz para que los ViewModels de P01 y P16 no dependan de un repositorio
 * concreto: la cumplen tanto el repositorio local (Room) como el falso (pruebas).
 * Con el futuro repositorio remoto dejará de usarse.
 */
interface SimuladorConexion {
    var sinConexion: Boolean
}
