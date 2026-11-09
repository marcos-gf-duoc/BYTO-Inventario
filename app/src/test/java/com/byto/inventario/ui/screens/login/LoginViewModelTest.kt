package com.byto.inventario.ui.screens.login

import com.byto.inventario.data.fake.FakeInventarioRepository
import com.byto.inventario.data.repository.SesionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var inventarioRepository: FakeInventarioRepository
    private lateinit var sesionRepository: SesionRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        inventarioRepository = FakeInventarioRepository()
        sesionRepository = SesionRepository()
        viewModel = LoginViewModel(inventarioRepository, sesionRepository, inventarioRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun ingresarConUsuarioInvalidoFalla() = runTest {
        testScheduler.advanceUntilIdle()
        viewModel.actualizarUsuarioId("INVALIDO")
        viewModel.actualizarPassword("123456")

        val exito = viewModel.ingresar()
        assertFalse(exito)
        assertTrue(viewModel.estado.value.errorUsuario)
    }

    @Test
    fun ingresarConPasswordIncorrectoFalla() = runTest {
        testScheduler.advanceUntilIdle()
        viewModel.actualizarUsuarioId("OP-001")
        viewModel.actualizarPassword("111111") // wrong password

        val exito = viewModel.ingresar()
        assertFalse(exito)
        assertTrue(viewModel.estado.value.errorPassword)
    }

    @Test
    fun ingresarConPasswordCorrectoExito() = runTest {
        testScheduler.advanceUntilIdle()
        val usuarios = inventarioRepository.obtenerUsuariosDemo()
        val usuario = usuarios.first()
        viewModel.actualizarUsuarioId(usuario.id)
        viewModel.actualizarPassword(usuario.password) // correct 6-digit password

        val exito = viewModel.ingresar()
        assertTrue(exito)
        assertFalse(viewModel.estado.value.errorPassword)
        assertEquals(usuario, sesionRepository.usuarioActivo.value)
    }
}
