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
 * Recibe cada imagen de la cámara (CameraX) y busca en ella códigos de barras o QR (ML Kit).
 *
 * CameraX llama a [analyze] en un hilo aparte con cada cuadro. ML Kit procesa la imagen y
 * entrega el resultado en el hilo principal, donde se llama a [alDetectar] con el texto de
 * todos los códigos vistos en ese cuadro.
 *
 * Se leen todos los formatos a propósito: el catálogo solo usa EAN-13, pero así un QR u
 * otro código ajeno se detecta y el ViewModel puede avisar que no sirve, en vez de que la
 * pantalla se quede sin responder. Decidir cuál código es válido no es tarea de esta clase.
 */
class AnalizadorCodigos(
    private val alDetectar: (List<String>) -> Unit
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
                val valores = codigos.mapNotNull { it.rawValue }
                if (valores.isNotEmpty()) alDetectar(valores)
            }
            // Cerrar siempre la imagen: mientras no se cierre, CameraX no entrega la siguiente.
            .addOnCompleteListener { imageProxy.close() }
    }

    /** Libera el lector al salir de la pantalla. */
    fun cerrar() {
        lector.close()
    }

    companion object {
        /**
         * Lector de ML Kit que detecta todos los formatos (EAN, QR, Code 128, UPC, etc.).
         * Detectar no es aceptar: solo un EAN-13 válido pasa el filtro del ViewModel.
         */
        fun crearLector(): BarcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )
    }
}
