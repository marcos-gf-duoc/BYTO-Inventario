package com.byto.inventario.ui.screens.sinconexion

import androidx.lifecycle.ViewModel
import com.byto.inventario.data.repository.SimuladorConexion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * P16 no carga datos: "Reintentar" solo vuelve a la pantalla anterior, que repite su carga.
 * Este ViewModel existe para el interruptor oculto de la demo (desaparecerá con el
 * futuro repositorio remoto).
 */
class SinConexionViewModel(
    private val simulador: SimuladorConexion
) : ViewModel() {

    private val _conexionSimuladaCortada = MutableStateFlow(simulador.sinConexion)
    val conexionSimuladaCortada: StateFlow<Boolean> = _conexionSimuladaCortada.asStateFlow()

    fun alternarConexionSimulada() {
        simulador.sinConexion = !simulador.sinConexion
        _conexionSimuladaCortada.value = simulador.sinConexion
    }
}
