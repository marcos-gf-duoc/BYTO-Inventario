package com.byto.inventario.ui.screens.escaner

import androidx.lifecycle.ViewModel
import com.byto.inventario.dispositivo.Vibrador
import com.byto.inventario.domain.ValidadorEan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Situación del permiso de cámara, según lo que informa la pantalla. */
enum class PermisoCamara { SIN_CONSULTAR, CONCEDIDO, DENEGADO }

/**
 * Estado de P17.
 * @param codigoInvalido se leyó algo que no es un EAN-13 válido; se sigue escaneando.
 * @param camaraNoDisponible el teléfono no tiene cámara o no se pudo abrir.
 * @param codigoLeido código válido ya leído: la pantalla debe devolverlo a P08 (evento).
 */
data class EstadoEscanear(
    val permiso: PermisoCamara = PermisoCamara.SIN_CONSULTAR,
    val codigoInvalido: Boolean = false,
    val camaraNoDisponible: Boolean = false,
    val codigoLeido: String? = null
)

/**
 * La pantalla se encarga de lo que exige Android (pedir el permiso y abrir la cámara)
 * y le avisa aquí cada resultado. Este ViewModel decide qué hacer con cada uno.
 */
class EscanearCodigoViewModel(
    private val vibrador: Vibrador
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoEscanear())
    val estado: StateFlow<EstadoEscanear> = _estado.asStateFlow()

    fun alResolverPermiso(concedido: Boolean) {
        _estado.update {
            it.copy(permiso = if (concedido) PermisoCamara.CONCEDIDO else PermisoCamara.DENEGADO)
        }
    }

    /**
     * La cámara entrega muchos cuadros por segundo, así que el mismo código puede llegar
     * varias veces: solo se usa el primero válido.
     *
     * Se valida con la misma regla que la búsqueda manual (dígito verificador incluido),
     * para no aceptar una lectura dañada.
     */
    fun alDetectarCodigo(valor: String) {
        if (_estado.value.codigoLeido != null) return
        if (ValidadorEan.validar(valor) != null) {
            _estado.update { it.copy(codigoInvalido = true) }
            return
        }
        vibrador.vibrarLectura()
        _estado.update { it.copy(codigoLeido = valor, codigoInvalido = false) }
    }

    fun alFallarCamara() {
        _estado.update { it.copy(camaraNoDisponible = true) }
    }
}
