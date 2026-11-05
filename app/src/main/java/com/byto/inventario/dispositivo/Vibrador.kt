package com.byto.inventario.dispositivo

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Acceso al motor de vibración del teléfono (recurso nativo).
 * Necesita el permiso VIBRATE, que Android concede al instalar (está en el manifiesto).
 *
 * La vibración nunca es la única señal: la pantalla siempre muestra además un aviso visual.
 */
class Vibrador(context: Context) {

    // Desde Android 12 (API 31) el vibrador se obtiene a través de VibratorManager;
    // en versiones anteriores, directamente como servicio del sistema.
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    /** Vibración corta que confirma la lectura de un código. No hace nada si no hay vibrador. */
    fun vibrarLectura() {
        val motor = vibrator ?: return
        if (!motor.hasVibrator()) return
        motor.vibrate(VibrationEffect.createOneShot(DURACION_LECTURA_MS, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private companion object {
        const val DURACION_LECTURA_MS = 80L
    }
}
