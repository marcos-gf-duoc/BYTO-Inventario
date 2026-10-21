package com.byto.inventario.ui.screens.buscar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byto.inventario.data.model.ExistenciaDetalle
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.ReglaNegocioException
import com.byto.inventario.data.repository.SesionRepository
import com.byto.inventario.data.repository.SinConexionException
import com.byto.inventario.domain.ErrorEan
import com.byto.inventario.domain.Permisos
import com.byto.inventario.domain.ValidadorEan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de P08.
 * @param error problema del código escrito (se muestra bajo el campo); null = sin error.
 * @param resultado el producto encontrado en la ubicación activa.
 * @param noEncontrado el código es válido, pero no hay producto con él en esta ubicación.
 * @param puedeRegistrar muestra el botón "Registrar" (solo Operador en sus tiendas).
 */
data class EstadoBuscarCodigo(
    val codigo: String = "",
    val error: ErrorEan? = null,
    val buscando: Boolean = false,
    val resultado: ExistenciaDetalle? = null,
    val noEncontrado: Boolean = false,
    val falloCarga: Boolean = false,
    val puedeRegistrar: Boolean = false,
    val irASinConexion: Boolean = false,
    val sinSesion: Boolean = false
)

class BuscarCodigoViewModel(
    private val repositorio: InventarioRepository,
    private val sesionRepository: SesionRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(
        EstadoBuscarCodigo(
            puedeRegistrar = Permisos.puedeRegistrarMovimientos(
                sesionRepository.usuarioActivo.value, sesionRepository.ubicacionActiva.value
            )
        )
    )
    val estado: StateFlow<EstadoBuscarCodigo> = _estado.asStateFlow()

    private var reintentarAlVolver = false

    /** Al cambiar el texto se borran el error y el resultado anteriores: ya no corresponden. */
    fun cambiarCodigo(texto: String) {
        // Solo dígitos y como máximo 13: el teclado numérico podría dejar pasar otros caracteres.
        val limpio = texto.filter { it.isDigit() }.take(ValidadorEan.LARGO)
        _estado.update {
            it.copy(codigo = limpio, error = null, resultado = null, noEncontrado = false, falloCarga = false)
        }
    }

    /** Valida con la función pura del dominio y, si el código es válido, lo busca. */
    fun buscar() {
        val codigo = _estado.value.codigo
        val error = ValidadorEan.validar(codigo)
        if (error != null) {
            _estado.update { it.copy(error = error, resultado = null, noEncontrado = false) }
            return
        }
        val ubicacion = sesionRepository.ubicacionActiva.value
        if (ubicacion == null) {
            _estado.update { it.copy(sinSesion = true) }
            return
        }
        viewModelScope.launch {
            _estado.update {
                it.copy(buscando = true, resultado = null, noEncontrado = false, falloCarga = false)
            }
            try {
                // Paso 1: ¿existe un producto con ese EAN en el catálogo? (endpoint 6)
                val producto = repositorio.buscarProductoPorEan(codigo)
                // Paso 2: ¿está en la ubicación activa, y con cuánto stock? (endpoint 5)
                val detalle = producto?.let { repositorio.obtenerExistencia(ubicacion.id, it.codigoInterno) }
                _estado.update { it.copy(buscando = false, resultado = detalle, noEncontrado = detalle == null) }
            } catch (e: SinConexionException) {
                reintentarAlVolver = true
                _estado.update { it.copy(buscando = false, irASinConexion = true) }
            } catch (e: ReglaNegocioException) {
                // NO_ENCONTRADO: el producto existe, pero no en esta ubicación.
                val noEsta = e.codigo == "NO_ENCONTRADO"
                _estado.update { it.copy(buscando = false, noEncontrado = noEsta, falloCarga = !noEsta) }
            }
        }
    }

    fun navegacionSinConexionRealizada() {
        _estado.update { it.copy(irASinConexion = false) }
    }

    /** Al volver de P16 se repite la última búsqueda. */
    fun reintentarSiHaceFalta() {
        if (reintentarAlVolver) {
            reintentarAlVolver = false
            buscar()
        }
    }
}
