package com.byto.inventario.data.repository

import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.data.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Quién está usando la app y en qué ubicación trabaja. Vive en memoria:
 * al cerrar la app se pierde (login simulado de E1).
 *
 * Patrón habitual: el MutableStateFlow privado solo se cambia desde aquí, y hacia
 * afuera se expone como StateFlow de solo lectura. Las pantallas lo observan y se
 * redibujan solas cuando cambia.
 */
class SesionRepository {

    private val _usuarioActivo = MutableStateFlow<Usuario?>(null)
    val usuarioActivo: StateFlow<Usuario?> = _usuarioActivo.asStateFlow()

    private val _ubicacionActiva = MutableStateFlow<Ubicacion?>(null)
    val ubicacionActiva: StateFlow<Ubicacion?> = _ubicacionActiva.asStateFlow()

    /** Al cambiar de usuario se borra la ubicación: debe elegir una de las suyas. */
    fun iniciarSesion(usuario: Usuario) {
        _usuarioActivo.value = usuario
        _ubicacionActiva.value = null
    }

    fun seleccionarUbicacion(ubicacion: Ubicacion) {
        _ubicacionActiva.value = ubicacion
    }

    fun cerrarSesion() {
        _usuarioActivo.value = null
        _ubicacionActiva.value = null
    }
}
