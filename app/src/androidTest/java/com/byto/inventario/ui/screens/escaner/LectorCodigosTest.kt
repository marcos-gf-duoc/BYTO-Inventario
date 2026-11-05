package com.byto.inventario.ui.screens.escaner

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.byto.inventario.data.fake.DatosSinteticos
import com.byto.inventario.domain.ValidadorEan
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba el lector de ML Kit con la misma configuración del escáner (P17), sin cámara:
 * dibuja el código de barras de cada producto del catálogo en una imagen y comprueba que
 * se lee el mismo EAN y que la app lo acepta como válido.
 *
 * Así se verifica también que los códigos de docs/anexos/codigos_prueba_EAN13.html, generados
 * con el mismo algoritmo, son legibles.
 */
@RunWith(AndroidJUnit4::class)
class LectorCodigosTest {

    @Test
    fun leeLos25CodigosDelCatalogo() {
        val lector = AnalizadorCodigos.crearLector()
        try {
            for (producto in DatosSinteticos.productos) {
                val imagen = InputImage.fromBitmap(dibujarEan13(producto.ean13), 0)
                // Tasks.await espera el resultado de ML Kit (en una prueba se puede bloquear el hilo).
                val leidos = Tasks.await(lector.process(imagen)).mapNotNull { it.rawValue }

                assertEquals("Lectura de ${producto.codigoInterno}", listOf(producto.ean13), leidos)
                assertNull(ValidadorEan.validar(leidos.first()))
            }
        } finally {
            lector.close()
        }
    }

    // ---------- Dibujo de un EAN-13 (norma GS1) ----------

    private val codigosL = listOf(
        "0001101", "0011001", "0010011", "0111101", "0100011",
        "0110001", "0101111", "0111011", "0110111", "0001011"
    )
    private val codigosR = codigosL.map { c -> c.map { if (it == '0') '1' else '0' }.joinToString("") }
    private val codigosG = codigosR.map { it.reversed() }

    /** El primer dígito no se dibuja: decide si cada dígito izquierdo usa el juego L o G. */
    private val paridad = listOf(
        "LLLLLL", "LLGLGG", "LLGGLG", "LLGGGL", "LGLLGG",
        "LGGLLG", "LGGGLL", "LGLGLG", "LGLGGL", "LGGLGL"
    )

    /** 95 módulos: guía + 6 dígitos + guía central + 6 dígitos + guía (1 = barra negra). */
    private fun modulos(ean: String): String {
        val digitos = ean.map { it.digitToInt() }
        val izquierda = digitos.subList(1, 7).mapIndexed { i, d ->
            if (paridad[digitos[0]][i] == 'L') codigosL[d] else codigosG[d]
        }
        val derecha = digitos.subList(7, 13).map { codigosR[it] }
        return "101" + izquierda.joinToString("") + "01010" + derecha.joinToString("") + "101"
    }

    /** Imagen en blanco y negro con zona silenciosa de 11 módulos a cada lado. */
    private fun dibujarEan13(ean: String, anchoModulo: Int = 6, alto: Int = 240): Bitmap {
        val barras = modulos(ean)
        val margen = 11 * anchoModulo
        val ancho = barras.length * anchoModulo + 2 * margen
        val bitmap = Bitmap.createBitmap(ancho, alto + 2 * margen, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        barras.forEachIndexed { i, modulo ->
            if (modulo == '1') {
                for (x in 0 until anchoModulo) {
                    for (y in 0 until alto) {
                        bitmap.setPixel(margen + i * anchoModulo + x, margen + y, Color.BLACK)
                    }
                }
            }
        }
        return bitmap
    }
}
