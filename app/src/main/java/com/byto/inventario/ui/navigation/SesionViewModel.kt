package com.byto.inventario.ui.navigation

import androidx.lifecycle.ViewModel
import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.data.model.Usuario
import com.byto.inventario.data.repository.SesionRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel de la "carcasa" de la app (barras y navegación). Les entrega a las
 * barras el usuario y la ubicación activos, sin que los Composables toquen el repositorio.
 */
class SesionViewModel(
    private val sesionRepository: SesionRepository
) : ViewModel() {

    val usuarioActivo: StateFlow<Usuario?> = sesionRepository.usuarioActivo
    val ubicacionActiva: StateFlow<Ubicacion?> = sesionRepository.ubicacionActiva

    fun cerrarSesion() {
        sesionRepository.cerrarSesion()
    }
}
