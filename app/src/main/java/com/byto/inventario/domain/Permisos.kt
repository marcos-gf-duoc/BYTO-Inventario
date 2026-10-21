package com.byto.inventario.domain

import com.byto.inventario.data.model.Perfil
import com.byto.inventario.data.model.TipoUbicacion
import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.data.model.Usuario

/**
 * Qué puede hacer cada perfil. Se deja en un solo lugar porque lo usan varias pantallas
 * (Detalle P07 y Buscar por código P08) y así la regla no se repite ni se contradice.
 */
object Permisos {

    /**
     * Solo el Operador registra movimientos, y solo en sus propias tiendas activas.
     * Encargado y Gerente ven el detalle en modo de solo consulta.
     */
    fun puedeRegistrarMovimientos(usuario: Usuario?, ubicacion: Ubicacion?): Boolean {
        if (usuario == null || ubicacion == null) return false
        return usuario.perfil == Perfil.OPERADOR &&
            ubicacion.tipo == TipoUbicacion.TIENDA &&
            ubicacion.activa &&
            ubicacion.id in usuario.ubicacionesIds
    }
}
