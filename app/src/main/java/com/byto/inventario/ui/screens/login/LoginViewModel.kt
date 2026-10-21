package com.byto.inventario.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byto.inventario.data.fake.FakeInventarioRepository
import com.byto.inventario.data.model.Usuario
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.SesionRepository
import com.byto.inventario.data.repository.SinConexionException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de P01.
 * @param irASinConexion true = la pantalla debe navegar a P16 (se apaga apenas lo hace).
 * @param conexionSimuladaCortada true = el interruptor oculto de la demo está activo.
 */
data class EstadoLogin(
    val cargando: Boolean = true,
    val usuarios: List<Usuario> = emptyList(),
    val seleccionadoId: String? = null,
    val irASinConexion: Boolean = false,
    val conexionSimuladaCortada: Boolean = false
)

/**
 * @param simulador solo para la demo de E1: permite cortar la conexión simulada con el
 * interruptor oculto. En E2, con el repositorio remoto, este parámetro desaparece.
 */
class LoginViewModel(
    private val repositorio: InventarioRepository,
    private val sesionRepository: SesionRepository,
    private val simulador: FakeInventarioRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(
        EstadoLogin(conexionSimuladaCortada = simulador.sinConexion)
    )
    val estado: StateFlow<EstadoLogin> = _estado.asStateFlow()

    /** Se activa si la carga falló por falta de conexión, para reintentar al volver de P16. */
    private var reintentarAlVolver = false

    init {
        cargarUsuarios()
    }

    private fun cargarUsuarios() {
        viewModelScope.launch {
            // update{} copia el estado actual cambiando solo lo indicado.
            _estado.update { it.copy(cargando = true) }
            try {
                val usuarios = repositorio.obtenerUsuariosDemo()
                _estado.update { it.copy(cargando = false, usuarios = usuarios) }
            } catch (e: SinConexionException) {
                reintentarAlVolver = true
                _estado.update { it.copy(irASinConexion = true) }
            }
        }
    }

    /** La pantalla avisa que ya navegó a P16, para no volver a hacerlo. */
    fun navegacionSinConexionRealizada() {
        _estado.update { it.copy(irASinConexion = false) }
    }

    /** La pantalla lo llama cada vez que vuelve a mostrarse (por ejemplo, tras "Reintentar"). */
    fun reintentarSiHaceFalta() {
        if (reintentarAlVolver) {
            reintentarAlVolver = false
            cargarUsuarios()
        }
    }

    fun seleccionar(usuario: Usuario) {
        _estado.update { it.copy(seleccionadoId = usuario.id) }
    }

    /** Guarda al usuario elegido en la sesión. Devuelve false si no hay ninguno elegido. */
    fun ingresar(): Boolean {
        val actual = _estado.value
        val usuario = actual.usuarios.find { it.id == actual.seleccionadoId } ?: return false
        sesionRepository.iniciarSesion(usuario)
        return true
    }

    /** Interruptor oculto de la demo (mantener presionado el título). */
    fun alternarConexionSimulada() {
        simulador.sinConexion = !simulador.sinConexion
        _estado.update { it.copy(conexionSimuladaCortada = simulador.sinConexion) }
    }
}
