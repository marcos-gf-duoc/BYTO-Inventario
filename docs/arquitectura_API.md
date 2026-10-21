# BYTO — Arquitectura y contrato de la API (Etapa c)

Etapa (c) confirmada por el estudiante: Navigation Compose aprobado; retardo simulado (~300 ms) e interruptor de "sin conexión" aprobados.

## Arquitectura
- App Android: Kotlin + Jetpack Compose + MVVM, un solo módulo.
- Capas: `ui` (pantallas y ViewModels) → `data` (repositorios) → origen de datos.
- Regla central: los ViewModels solo conocen la **interfaz** `InventarioRepository`.
  - E1: `FakeInventarioRepository` (en memoria, imita al backend, con un retardo simulado de ~300 ms y un interruptor oculto para simular "sin conexión").
  - E2: `RemoteInventarioRepository` (llamadas HTTP al backend Spring Boot).
- Reglas de negocio puras en `domain/ReglasInventario` (cálculo de stock resultante, estado del stock, validaciones). En E1 las usan el repositorio falso y la vista previa de "stock resultante"; en E2 el backend es la autoridad y la app las usa solo para la vista previa.
- Sesión (usuario y ubicación activa) en memoria, en `SesionRepository`.
- Dependencias creadas a mano en `AppContainer` (sin Hilt ni Koin).

## Estructura de paquetes
```
com.byto.inventario
├── BytoApp.kt                 (Application: crea el AppContainer)
├── MainActivity.kt
├── di/AppContainer.kt
├── domain/ReglasInventario.kt
├── data/
│   ├── model/                 (Ubicacion, Producto, Existencia, Movimiento, Alerta, Usuario, enums)
│   ├── repository/            (InventarioRepository, SesionRepository)
│   └── fake/                  (FakeInventarioRepository, DatosSinteticos)
└── ui/
    ├── theme/                 (colores y tipografía accesibles)
    ├── navigation/            (rutas y NavHost)
    ├── components/            (botones, tarjetas, chips de estado reutilizables)
    └── screens/               (login, seleccion, inicio, inventario, detalle, movimiento, historial, alertas, tablero, ubicaciones)
```

## Librerías
| Librería | Para qué | Cuándo | Estado |
|---|---|---|---|
| Jetpack Compose, Material 3, Lifecycle/ViewModel | Base del proyecto (vienen en la plantilla) | E1 | Incluidas |
| Navigation Compose | Navegar entre pantallas | E1 | Aprobada |
| Retrofit + convertidor JSON | Llamar a la API REST | E2 | A decidir en E2 |
| CameraX + ML Kit | Escáner | Futuro | A decidir |

## Convenciones del contrato
- Base: `/api/v1`. JSON en camelCase. Enums en MAYÚSCULAS.
- Fechas: ISO-8601 en UTC, por ejemplo `2026-10-16T13:10:00Z`. La hora del movimiento la asigna el servidor, no el teléfono.
- Errores: `{ "codigo": "STOCK_INSUFICIENTE", "mensaje": "La salida (5) supera el stock actual (3)." }`
- Códigos HTTP: 200 OK, 201 creado, 400 datos inválidos, 404 no existe, 409 conflicto de regla (por ejemplo, ubicación inactiva).
- Autenticación: no definida aún. En E1 el login elige un usuario de una lista. En E2 el servidor debe obtener el operador desde la sesión o el token, no desde el cuerpo de la petición.

## Endpoints
| # | Método y ruta | Para qué | Perfil | E1 |
|---|---|---|---|---|
| 1 | `GET /usuarios/demo` | Lista de usuarios ficticios para el login simulado | Todos | Sí |
| 2 | `GET /ubicaciones?usuarioId=` | Ubicaciones asignadas al usuario (todas si es Gerente) | Todos | Sí |
| 3 | `PATCH /ubicaciones/{id}` | Habilitar o deshabilitar una ubicación `{ "estado": "ACTIVA" }` | Gerente | Sí |
| 4 | `GET /ubicaciones/{id}/existencias?q=&estado=` | Inventario de una ubicación con búsqueda y filtro (NORMAL, BAJO, CRITICO, SIN_STOCK) | Operador, Encargado, Gerente | Sí |
| 5 | `GET /ubicaciones/{id}/existencias/{codigoInterno}` | Detalle de un producto en esa ubicación | Todos | Sí |
| 6 | `GET /productos/ean/{ean}` | Buscar producto por código de barras | Operador | Sí (búsqueda manual P08; escáner futuro) |
| 7 | `POST /movimientos` | Registrar entrada, salida, ajuste o conteo | Operador | Sí |
| 8 | `GET /movimientos?ubicacionId=&usuarioId=&tipo=&desde=&hasta=` | Historial y auditoría filtrable (Caso §4.1) | Gerente; Operador (propios) | Sí |
| 9 | `GET /alertas?ubicacionId=&tipo=&estado=` | Alertas abiertas o cerradas | Todos (según asignación) | Sí |
| 10 | `POST /alertas/reposicion` | Declarar reposición `{ "ubicacionId", "codigoInterno" }` | Encargado | Sí |
| 11 | `PATCH /alertas/{id}` | Marcar descalce como revisado `{ "estado": "REVISADA" }` | Encargado, Gerente | Sí |
| 12 | `GET /tablero` | Resumen: totales por ubicación y alertas abiertas | Gerente | Sí |

## Ejemplo: registrar un conteo
Petición `POST /api/v1/movimientos`
```json
{
  "tipo": "CONTEO",
  "ubicacionId": "T-ST-2",
  "codigoInterno": "RM-02",
  "cantidad": 6,
  "motivo": null,
  "observacion": "Conteo de sección mujer"
}
```
Respuesta `201 Created`
```json
{
  "movimiento": {
    "id": "MOV-0003",
    "tipo": "CONTEO",
    "ubicacionId": "T-ST-2",
    "codigoInterno": "RM-02",
    "cantidad": 6,
    "stockAntes": 10,
    "stockDespues": 6,
    "fechaUtc": "2026-10-17T12:30:00Z",
    "operadorId": "OP-001",
    "observacion": "Conteo de sección mujer"
  },
  "alertasGeneradas": [
    { "id": "ALE-0002", "tipo": "DESCALCE", "diferencia": -4, "estado": "ABIERTA" },
    { "id": "ALE-0003", "tipo": "STOCK_BAJO", "nivel": "BAJO", "estado": "ABIERTA" }
  ]
}
```

## Interfaz `InventarioRepository` (Kotlin)
Una función `suspend` por endpoint; el número es el del endpoint. La implementan `FakeInventarioRepository` (E1) y `RemoteInventarioRepository` (E2). Código en `data/repository/InventarioRepository.kt`.

```kotlin
interface InventarioRepository {
    suspend fun obtenerUsuariosDemo(): List<Usuario>                                    // 1
    suspend fun obtenerUbicaciones(usuarioId: String): List<Ubicacion>                  // 2
    suspend fun cambiarEstadoUbicacion(ubicacionId: String, activa: Boolean): Ubicacion // 3
    suspend fun obtenerExistencias(                                                      // 4
        ubicacionId: String, busqueda: String? = null, estado: EstadoStock? = null
    ): List<ExistenciaDetalle>
    suspend fun obtenerExistencia(ubicacionId: String, codigoInterno: String): ExistenciaDetalle // 5
    suspend fun buscarProductoPorEan(ean: String): Producto?                             // 6 (null = no existe)
    suspend fun registrarMovimiento(nuevo: NuevoMovimiento): ResultadoMovimiento         // 7
    suspend fun obtenerMovimientos(filtro: FiltroMovimientos = FiltroMovimientos()): List<Movimiento> // 8
    suspend fun obtenerAlertas(                                                          // 9
        ubicacionId: String? = null, tipo: TipoAlerta? = null, estado: EstadoAlerta? = null
    ): List<Alerta>
    suspend fun declararReposicion(                                                      // 10
        ubicacionId: String, codigoInterno: String, declaradaPor: String
    ): Alerta
    suspend fun marcarDescalceRevisado(alertaId: String): Alerta                         // 11
    suspend fun obtenerTablero(): Tablero                                                // 12
}
```

Tipos de apoyo (en `data/model/`):
- `ExistenciaDetalle(existencia, producto, estado)`: fila del inventario con su producto y estado calculado.
- `NuevoMovimiento(tipo, ubicacionId, codigoInterno, cantidad, operadorId, motivo?, observacion?)`: cuerpo del POST. `operadorId` existe solo en E1; en E2 el servidor lo obtiene de la sesión o el token.
- `ResultadoMovimiento(movimiento, alertasGeneradas)`: respuesta del POST.
- `FiltroMovimientos(ubicacionId?, usuarioId?, tipo?, desde?, hasta?)`: filtros del historial (`null` = sin filtrar; fechas inclusivas).
- `Tablero(resumenes, totalAlertasAbiertas)` y `ResumenUbicacion(ubicacion, totalUnidades, productosNormal, productosBajo, productosCritico, productosSinStock, alertasAbiertas)`.

### Errores
- `SinConexionException`: no hay comunicación con el servidor (en E1, interruptor del repositorio falso).
- `ReglaNegocioException(codigo, mensaje)`: equivale al JSON de error. Códigos usados:

| Código | HTTP | Cuándo |
|---|---|---|
| `NO_ENCONTRADO` | 404 | Usuario, ubicación, existencia o alerta inexistente |
| `CANTIDAD_INVALIDA` | 400 | Cantidad fuera de lo permitido para el tipo de movimiento |
| `STOCK_INSUFICIENTE` | 400 | Salida mayor que el stock actual |
| `MOTIVO_OBLIGATORIO` | 400 | Ajuste sin motivo |
| `STOCK_NEGATIVO` | 400 | Ajuste que deja el stock bajo 0 |
| `OBSERVACION_MUY_LARGA` | 400 | Observación de más de 200 caracteres |
| `UBICACION_INACTIVA` | 409 | Movimiento en una ubicación deshabilitada |
| `UBICACION_SIN_STOCK` | 409 | Movimiento en Casa Matriz |
| `UBICACION_NO_ES_TIENDA` | 409 | Reposición declarada fuera de una tienda |
| `REPOSICION_YA_ABIERTA` | 409 | Ya existe una reposición abierta de ese producto en esa tienda |
| `ALERTA_NO_REVISABLE` | 409 | Se intenta revisar algo que no es un descalce abierto |

### Efectos de `registrarMovimiento` sobre las alertas
1. Conteo con diferencia → abre un `DESCALCE` con `diferencia = contado − sistema`.
2. Stock resultante < 10 → abre un `STOCK_BAJO` si no hay uno abierto; si ya hay uno, solo actualiza su `nivel`. Stock ≥ 10 → cierra el abierto.
3. Entrada → cierra la `REPOSICION` abierta de ese producto en esa ubicación.
