package com.byto.inventario.data.model

// Enumeraciones del dominio. Se escriben en MAYÚSCULAS porque así viajarán
// en el JSON del contrato de la API (docs/arquitectura_API.md).

/** Clase de ubicación de BYTO. Solo TIENDA y BODEGA tienen stock. */
enum class TipoUbicacion { TIENDA, BODEGA, ADMINISTRATIVA }

/** Perfil del usuario simulado; define qué puede ver y hacer. */
enum class Perfil { OPERADOR, ENCARGADO_BODEGA, GERENTE }

/** Tipos de movimiento de inventario y su efecto en el stock (ver ReglasInventario). */
enum class TipoMovimiento { ENTRADA, SALIDA, AJUSTE, CONTEO }

/**
 * Estado del stock según los umbrales globales:
 * NORMAL ≥ 10, BAJO 5–9, CRITICO 1–4, SIN_STOCK = 0.
 */
enum class EstadoStock { NORMAL, BAJO, CRITICO, SIN_STOCK }

enum class TipoAlerta { STOCK_BAJO, DESCALCE, REPOSICION }

enum class EstadoAlerta { ABIERTA, REVISADA, CERRADA }
