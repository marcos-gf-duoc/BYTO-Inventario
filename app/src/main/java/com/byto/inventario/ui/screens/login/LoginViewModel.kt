package com.byto.inventario.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byto.inventario.data.model.Usuario
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.SesionRepository
import com.byto.inventario.data.repository.SimuladorConexion
import com.byto.inventario.data.repository.SinConexionException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de P01 Login.
 * @param irASinConexion true = la pantalla debe navegar a P16.
 * @param conexionSimuladaCortada true = el interruptor oculto de la demo está activo.
 */
data class EstadoLogin(
    val cargando: Boolean = true,
    val usuarios: List<Usuario> = emptyList(),
    val usuarioIdInput: String = "",
    val passwordInput: String = "",
    val errorUsuario: Boolean = false,
    val errorPassword: Boolean = false,
    val mensajeError: String? = null,
    val irASinConexion: Boolean = false,
    val conexionSimuladaCortada: Boolean = false
)

/**
 * @param simulador solo para la demo: permite cortar la conexión simulada con el
 * interruptor oculto.
 */
class LoginViewModel(
    private val repositorio: InventarioRepository,
    private val sesionRepository: SesionRepository,
    private val simulador: SimuladorConexion
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

    /** La pantalla lo llama cada vez que vuelve a mostrarse. */
    fun reintentarSiHaceFalta() {
        if (reintentarAlVolver) {
            reintentarAlVolver = false
            cargarUsuarios()
        }
    }

    fun actualizarUsuarioId(id: String) {
        _estado.update { 
            it.copy(
                usuarioIdInput = id,
                errorUsuario = false,
                mensajeError = null
            ) 
        }
    }

    fun actualizarPassword(password: String) {
        val filtrado = password.filter { it.isDigit() }.take(6)
        _estado.update { 
            it.copy(
                passwordInput = filtrado,
                errorPassword = false,
                mensajeError = null
            ) 
        }
    }

    /** Valida el ID de usuario y la contraseña de 6 dígitos. Inicia sesión si son correctos. */
    fun ingresar(): Boolean {
        val actual = _estado.value
        val idTrim = actual.usuarioIdInput.trim()
        val usuario = actual.usuarios.find { it.id.equals(idTrim, ignoreCase = true) }

        if (usuario == null) {
            _estado.update {
                it.copy(
                    errorUsuario = true,
                    mensajeError = "Usuario no encontrado"
                )
            }
            return false
        }

        if (actual.passwordInput.length != 6) {
            _estado.update {
                it.copy(
                    errorPassword = true,
                    mensajeError = "La contraseña debe tener 6 dígitos"
                )
            }
            return false
        }

        if (actual.passwordInput != usuario.password) {
            _estado.update {
                it.copy(
                    errorPassword = true,
                    mensajeError = "Contraseña incorrecta"
                )
            }
            return false
        }

        sesionRepository.iniciarSesion(usuario)
        return true
    }

    /** Interruptor oculto de la demo (mantener presionado el título). */
    fun alternarConexionSimulada() {
        simulador.sinConexion = !simulador.sinConexion
        _estado.update { it.copy(conexionSimuladaCortada = simulador.sinConexion) }
    }
}
