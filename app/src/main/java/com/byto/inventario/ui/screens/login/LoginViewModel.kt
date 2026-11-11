package com.byto.inventario.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.ReglaNegocioException
import com.byto.inventario.data.repository.SesionRepository
import com.byto.inventario.data.repository.SimuladorConexion
import com.byto.inventario.data.repository.SinConexionException
import com.byto.inventario.domain.ErroresLogin
import com.byto.inventario.domain.ValidacionLogin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de P01 Login.
 * @param errores errores que se muestran ahora, uno por campo. Los de "obligatorio" solo
 * aparecen después del primer intento, para no marcar en rojo un formulario recién abierto.
 * @param ingresando se está verificando: el botón muestra carga y no se puede tocar otra vez.
 * @param errorGeneral el ingreso falló por algo que no es de un campo.
 * @param sesionIniciada credenciales correctas: la pantalla debe navegar a P02 (evento).
 * @param irASinConexion true = la pantalla debe navegar a P16 (evento).
 * @param conexionSimuladaCortada true = el interruptor oculto de la demo está activo.
 */
data class EstadoLogin(
    val usuarioId: String = "",
    val pin: String = "",
    val errores: ErroresLogin = ErroresLogin(),
    val ingresando: Boolean = false,
    val errorGeneral: Boolean = false,
    val sesionIniciada: Boolean = false,
    val irASinConexion: Boolean = false,
    val conexionSimuladaCortada: Boolean = false
) {
    /** El botón se habilita mientras no haya errores visibles ni una verificación en curso. */
    val puedeIngresar: Boolean get() = !ingresando && !errores.hayErrores
}

/**
 * Este ViewModel no conoce los PIN: valida el formato con [ValidacionLogin] (dominio) y le
 * pide al repositorio que verifique las credenciales, como se hará con el servidor.
 *
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

    /** true después del primer toque en "Ingresar". */
    private var intentoIngresar = false

    fun cambiarUsuarioId(texto: String) {
        actualizar { it.copy(usuarioId = texto) }
    }

    fun cambiarPin(texto: String) {
        actualizar { it.copy(pin = ValidacionLogin.limpiarPin(texto)) }
    }

    /**
     * Aplica un cambio de la persona y recalcula los errores con la regla del dominio.
     * Los errores que dio el repositorio (usuario inexistente, PIN incorrecto) se borran:
     * correspondían a lo que estaba escrito antes.
     */
    private fun actualizar(cambio: (EstadoLogin) -> EstadoLogin) {
        _estado.update { actual ->
            val nuevo = cambio(actual)
            nuevo.copy(
                errores = if (intentoIngresar) {
                    ValidacionLogin.validar(nuevo.usuarioId, nuevo.pin)
                } else {
                    ErroresLogin()
                },
                errorGeneral = false
            )
        }
    }

    /** Valida el formulario y, si está completo, pide al repositorio verificar las credenciales. */
    fun ingresar() {
        if (_estado.value.ingresando) return
        intentoIngresar = true
        actualizar { it }
        val actual = _estado.value
        if (actual.errores.hayErrores) return

        viewModelScope.launch {
            _estado.update { it.copy(ingresando = true) }
            try {
                val usuario = repositorio.autenticar(
                    ValidacionLogin.normalizarId(actual.usuarioId), actual.pin
                )
                sesionRepository.iniciarSesion(usuario)
                intentoIngresar = false
                // El PIN se borra del estado apenas se usa: no queda escrito si se vuelve al login.
                _estado.update { it.copy(ingresando = false, pin = "", sesionIniciada = true) }
            } catch (e: SinConexionException) {
                _estado.update { it.copy(ingresando = false, irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                // El repositorio rechazó las credenciales: el error va al campo que corresponde.
                val deCampo = ValidacionLogin.desdeRechazo(e.codigo)
                _estado.update {
                    it.copy(
                        ingresando = false,
                        errores = deCampo ?: it.errores,
                        errorGeneral = deCampo == null
                    )
                }
            }
        }
    }

    /** La pantalla avisa que ya navegó a P02, para no volver a hacerlo. */
    fun navegacionInicioRealizada() {
        _estado.update { it.copy(sesionIniciada = false) }
    }

    /** La pantalla avisa que ya navegó a P16, para no volver a hacerlo. */
    fun navegacionSinConexionRealizada() {
        _estado.update { it.copy(irASinConexion = false) }
    }

    /** Interruptor oculto de la demo (mantener presionado el título). */
    fun alternarConexionSimulada() {
        simulador.sinConexion = !simulador.sinConexion
        _estado.update { it.copy(conexionSimuladaCortada = simulador.sinConexion) }
    }
}
