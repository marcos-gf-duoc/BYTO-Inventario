package com.byto.inventario.data.repository

/**
 * No se pudo hablar con el servidor. En E1 la lanza el repositorio falso cuando se
 * activa su interruptor "sin conexión"; en E2 la lanzará el remoto ante un fallo de red.
 */
class SinConexionException : Exception("Sin conexión con el servidor")

/**
 * El servidor rechazó la petición por una regla de negocio o un dato inválido.
 * Imita el JSON de error de la API: { "codigo": "...", "mensaje": "..." }.
 *
 * La pantalla debe decidir qué mostrar según [codigo] (con textos de strings.xml);
 * el mensaje sirve para depurar.
 */
class ReglaNegocioException(
    val codigo: String,
    mensaje: String
) : Exception(mensaje)
