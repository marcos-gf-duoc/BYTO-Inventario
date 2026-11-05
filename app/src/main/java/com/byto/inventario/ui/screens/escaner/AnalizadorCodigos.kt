package com.byto.inventario.ui.screens.escaner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Recibe cada imagen de la cámara (CameraX) y busca en ella un código EAN-13 (ML Kit).
 *
 * CameraX llama a [analyze] en un hilo aparte con cada cuadro. ML Kit procesa la imagen y
 * entrega el resultado en el hilo principal, donde se llama a [alDetectar].
 *
 * Solo se busca EAN-13 (el formato del catálogo): el lector trabaja más rápido y no
 * confunde otros códigos que aparezcan en la imagen.
 */
class AnalizadorCodigos(
    private val alDetectar: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val lector: BarcodeScanner = crearLector()

    // imageProxy.image está marcado como "experimental" en CameraX; @OptIn indica que se usa a sabiendas.
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val imagen = imageProxy.image
        if (imagen == null) {
            imageProxy.close()
            return
        }
        // rotationDegrees: cómo está girado el teléfono, para que ML Kit lea la imagen derecha.
        val entrada = InputImage.fromMediaImage(imagen, imageProxy.imageInfo.rotationDegrees)
        lector.process(entrada)
            .addOnSuccessListener { codigos ->
                codigos.firstNotNullOfOrNull { it.rawValue }?.let(alDetectar)
            }
            // Cerrar siempre la imagen: mientras no se cierre, CameraX no entrega la siguiente.
            .addOnCompleteListener { imageProxy.close() }
    }

    /** Libera el lector al salir de la pantalla. */
    fun cerrar() {
        lector.close()
    }

    companion object {
        /** Lector de ML Kit configurado solo para EAN-13. Las pruebas usan esta misma configuración. */
        fun crearLector(): BarcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13)
                .build()
        )
    }
}
