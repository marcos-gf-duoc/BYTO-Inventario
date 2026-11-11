package com.byto.inventario.ui.screens.login

import com.byto.inventario.data.fake.FakeInventarioRepository
import com.byto.inventario.data.repository.SesionRepository
import com.byto.inventario.domain.ErrorLogin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas del login contra el repositorio en memoria.
 *
 * El ViewModel verifica las credenciales en una corrutina; StandardTestDispatcher permite
 * avanzarla a mano con advanceUntilIdle() y revisar el estado después.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val despachador = StandardTestDispatcher()
    private lateinit var repositorio: FakeInventarioRepository
    private lateinit var sesion: SesionRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
        repositorio = FakeInventarioRepository(retardoMs = 0)
        sesion = SesionRepository()
        viewModel = LoginViewModel(repositorio, sesion, repositorio)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun escribir(id: String, pin: String) {
        viewModel.cambiarUsuarioId(id)
        viewModel.cambiarPin(pin)
    }

    @Test
    fun formularioRecienAbiertoNoMuestraErrores() {
        assertFalse(viewModel.estado.value.errores.hayErrores)
        assertTrue(viewModel.estado.value.puedeIngresar)
    }

    @Test
    fun ingresarConCamposVaciosMuestraErrorEnCadaCampo() = runTest(despachador) {
        viewModel.ingresar()
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertEquals(ErrorLogin.USUARIO_OBLIGATORIO, estado.errores.usuario)
        assertEquals(ErrorLogin.PIN_OBLIGATORIO, estado.errores.pin)
        assertFalse(estado.sesionIniciada)
    }

    @Test
    fun pinIncompletoNoIniciaSesion() = runTest(despachador) {
        escribir("OP-001", "123")
        viewModel.ingresar()
        advanceUntilIdle()

        assertEquals(ErrorLogin.PIN_INCOMPLETO, viewModel.estado.value.errores.pin)
        assertNull(sesion.usuarioActivo.value)
    }

    @Test
    fun usuarioInexistenteMarcaElCampoUsuario() = runTest(despachador) {
        escribir("XX-999", "123456")
        viewModel.ingresar()
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertEquals(ErrorLogin.USUARIO_NO_ENCONTRADO, estado.errores.usuario)
        assertNull(estado.errores.pin)
        assertNull(sesion.usuarioActivo.value)
    }

    @Test
    fun pinIncorrectoMarcaElCampoPin() = runTest(despachador) {
        escribir("OP-001", "111111")
        viewModel.ingresar()
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertEquals(ErrorLogin.PIN_INCORRECTO, estado.errores.pin)
        assertNull(estado.errores.usuario)
        assertNull(sesion.usuarioActivo.value)
    }

    @Test
    fun corregirElPinBorraElErrorDelRepositorio() = runTest(despachador) {
        escribir("OP-001", "111111")
        viewModel.ingresar()
        advanceUntilIdle()

        viewModel.cambiarPin("123456")

        assertFalse(viewModel.estado.value.errores.hayErrores)
    }

    @Test
    fun credencialesCorrectasInicianSesionYBorranElPin() = runTest(despachador) {
        // En minúsculas y con espacios: el ID se normaliza antes de enviarlo.
        escribir(" op-001 ", "123456")
        viewModel.ingresar()
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertTrue(estado.sesionIniciada)
        assertEquals("", estado.pin)
        assertEquals("OP-001", sesion.usuarioActivo.value?.id)
    }

    @Test
    fun cadaUsuarioDeDemostracionTieneSuPropioPin() = runTest(despachador) {
        // El PIN de OP-001 no sirve para el Gerente.
        escribir("GE-001", "123456")
        viewModel.ingresar()
        advanceUntilIdle()
        assertEquals(ErrorLogin.PIN_INCORRECTO, viewModel.estado.value.errores.pin)

        viewModel.cambiarPin("123459")
        viewModel.ingresar()
        advanceUntilIdle()
        assertEquals("GE-001", sesion.usuarioActivo.value?.id)
    }

    @Test
    fun sinConexionPideIrAP16YNoIniciaSesion() = runTest(despachador) {
        viewModel.alternarConexionSimulada()
        escribir("OP-001", "123456")
        viewModel.ingresar()
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertTrue(estado.irASinConexion)
        assertTrue(estado.conexionSimuladaCortada)
        assertNull(sesion.usuarioActivo.value)
    }

    @Test
    fun elPinSoloAceptaSeisDigitos() {
        viewModel.cambiarPin("12ab34567890")
        assertEquals("123456", viewModel.estado.value.pin)
    }
}
