package com.byto.inventario.domain

import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.TipoMovimiento

/**
 * Motivos por los que un movimiento no es válido.
 * Los nombres coinciden con el campo "codigo" de los errores de la API.
 * La pantalla traduce cada uno a un texto de strings.xml (aquí no hay Android).
 */
enum class ErrorValidacion {
    CANTIDAD_INVALIDA,
    STOCK_INSUFICIENTE,
    MOTIVO_OBLIGATORIO,
    STOCK_NEGATIVO,
    OBSERVACION_MUY_LARGA
}

/**
 * Reglas de negocio puras de docs/decisiones_E1.md.
 *
 * "Puras" = solo reciben datos y devuelven un resultado: no leen repositorios,
 * no guardan nada y no dependen de Android. Por eso se pueden probar con un
 * test simple y reutilizar en el repositorio falso y en la vista previa de la pantalla.
 */
object ReglasInventario {

    /** Desde este stock el producto está "Normal" (umbral global de E1). */
    const val UMBRAL_NORMAL = 10

    /** Desde este stock (y bajo UMBRAL_NORMAL) el producto está "Bajo"; por debajo, "Crítico". */
    const val UMBRAL_BAJO = 5

    const val MAX_CARACTERES_OBSERVACION = 200

    /** Normal ≥ 10, Bajo 5–9, Crítico 1–4, Sin stock 0. */
    fun estadoDe(stock: Int): EstadoStock = when {
        stock >= UMBRAL_NORMAL -> EstadoStock.NORMAL
        stock >= UMBRAL_BAJO -> EstadoStock.BAJO
        stock >= 1 -> EstadoStock.CRITICO
        else -> EstadoStock.SIN_STOCK
    }

    /**
     * Stock que quedaría tras el movimiento. No valida: usar antes [validarMovimiento].
     * En AJUSTE la cantidad trae su signo; en CONTEO lo contado reemplaza al stock.
     */
    fun calcularStockResultante(tipo: TipoMovimiento, stockActual: Int, cantidad: Int): Int =
        when (tipo) {
            TipoMovimiento.ENTRADA -> stockActual + cantidad
            TipoMovimiento.SALIDA -> stockActual - cantidad
            TipoMovimiento.AJUSTE -> stockActual + cantidad
            TipoMovimiento.CONTEO -> cantidad
        }

    /**
     * Cantidad permitida según el tipo:
     * entrada y salida > 0, ajuste ≠ 0 (con signo), conteo ≥ 0.
     */
    fun esCantidadValida(tipo: TipoMovimiento, cantidad: Int): Boolean = when (tipo) {
        TipoMovimiento.ENTRADA, TipoMovimiento.SALIDA -> cantidad > 0
        TipoMovimiento.AJUSTE -> cantidad != 0
        TipoMovimiento.CONTEO -> cantidad >= 0
    }

    /**
     * Revisa todas las reglas de un movimiento.
     * Devuelve null si es válido, o el primer error encontrado.
     */
    fun validarMovimiento(
        tipo: TipoMovimiento,
        stockActual: Int,
        cantidad: Int,
        motivo: String?,
        observacion: String?
    ): ErrorValidacion? {
        if (!esCantidadValida(tipo, cantidad)) return ErrorValidacion.CANTIDAD_INVALIDA

        if (tipo == TipoMovimiento.SALIDA && cantidad > stockActual) {
            return ErrorValidacion.STOCK_INSUFICIENTE
        }

        if (tipo == TipoMovimiento.AJUSTE) {
            // isNullOrBlank: un motivo de puros espacios tampoco sirve.
            if (motivo.isNullOrBlank()) return ErrorValidacion.MOTIVO_OBLIGATORIO
            if (calcularStockResultante(tipo, stockActual, cantidad) < 0) {
                return ErrorValidacion.STOCK_NEGATIVO
            }
        }

        if (observacion != null && observacion.length > MAX_CARACTERES_OBSERVACION) {
            return ErrorValidacion.OBSERVACION_MUY_LARGA
        }

        return null
    }
}
