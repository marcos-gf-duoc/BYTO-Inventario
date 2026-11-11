package com.byto.inventario

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.byto.inventario.ui.navigation.BytoNavegacion
import com.byto.inventario.ui.theme.BYTOTheme

/** Única Activity: solo aplica el tema y muestra la navegación de la app. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash del sistema (SplashScreen API): Android lo muestra apenas se toca el ícono,
        // mientras la app todavía se está abriendo. La Activity parte con Theme.BYTO.Splash
        // (ver el manifiesto) y esta llamada lo cambia por el tema normal cuando hay algo
        // que dibujar. Debe ir antes de super.onCreate(). Después sigue PantallaSplash (P00).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BYTOTheme {
                BytoNavegacion()
            }
        }
    }
}
