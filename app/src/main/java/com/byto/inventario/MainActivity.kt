package com.byto.inventario

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.byto.inventario.ui.navigation.BytoNavegacion
import com.byto.inventario.ui.theme.BYTOTheme

/** Única Activity: solo aplica el tema y muestra la navegación de la app. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BYTOTheme {
                BytoNavegacion()
            }
        }
    }
}
