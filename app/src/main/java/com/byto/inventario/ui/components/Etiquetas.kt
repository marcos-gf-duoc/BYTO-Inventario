package com.byto.inventario.ui.components

import androidx.annotation.StringRes
import com.byto.inventario.R
import com.byto.inventario.data.model.Perfil
import com.byto.inventario.data.model.TipoUbicacion

// Traducen los enums del dominio a textos de strings.xml.
// Así los modelos no dependen de Android y los textos siguen en un solo lugar.

@StringRes
fun Perfil.textoRes(): Int = when (this) {
    Perfil.OPERADOR -> R.string.perfil_operador
    Perfil.ENCARGADO_BODEGA -> R.string.perfil_encargado_bodega
    Perfil.GERENTE -> R.string.perfil_gerente
}

@StringRes
fun TipoUbicacion.textoRes(): Int = when (this) {
    TipoUbicacion.TIENDA -> R.string.tipo_tienda
    TipoUbicacion.BODEGA -> R.string.tipo_bodega
    TipoUbicacion.ADMINISTRATIVA -> R.string.tipo_administrativa
}
