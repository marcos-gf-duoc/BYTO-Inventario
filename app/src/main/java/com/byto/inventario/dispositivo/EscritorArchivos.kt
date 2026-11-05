package com.byto.inventario.dispositivo

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Guarda texto en un archivo elegido por la persona (recurso nativo: almacenamiento).
 *
 * La pantalla abre el selector "Guardar como" del sistema y este entrega una [Uri]: una
 * dirección al archivo que la app puede escribir, aunque esté en Descargas o en otra carpeta.
 * Por eso no hace falta el permiso de almacenamiento (funciona igual desde Android 8).
 */
class EscritorArchivos(context: Context) {

    // applicationContext: no retiene una pantalla en memoria.
    private val resolver = context.applicationContext.contentResolver

    /**
     * Escribe [texto] en UTF-8 reemplazando el contenido anterior ("wt" = escribir y truncar).
     * Corre en Dispatchers.IO: escribir un archivo es lento y no debe trabar la interfaz.
     * @throws IOException si no se pudo abrir o escribir el archivo.
     */
    suspend fun escribir(destino: Uri, texto: String) = withContext(Dispatchers.IO) {
        val salida = resolver.openOutputStream(destino, "wt")
            ?: throw IOException("No se pudo abrir el archivo de destino")
        // use: cierra el archivo al terminar, incluso si ocurre un error.
        salida.use { it.write(texto.toByteArray(Charsets.UTF_8)) }
    }
}
