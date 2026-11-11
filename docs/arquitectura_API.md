# BYTO — Arquitectura y contrato de la API (Entrega 2)

Confirmado por el estudiante:
- **Etapa (c)**: Navigation Compose, retardo simulado (~300 ms) e interruptor de "sin conexión".
- **Entrega 2**: Room para la persistencia local, cámara con CameraX y ML Kit, exportación CSV y vibración.
- **Mejoras posteriores**: splash (SplashScreen API + P00), login con ID y PIN verificado por el repositorio, linterna en el escáner y compartir el CSV.

## Arquitectura
- App Android: Kotlin + Jetpack Compose + MVVM, un solo módulo.
- Capas: `ui` (pantallas y ViewModels) → `data` (repositorios) → origen de datos.
- Regla central: los ViewModels solo conocen la **interfaz** `InventarioRepository`. Así se puede cambiar el origen de datos sin tocar las pantallas.
  - E2: `LocalInventarioRepository`, que guarda en Room (base de datos SQLite del teléfono). Conserva el retardo simulado de ~300 ms y el interruptor oculto de "sin conexión", para seguir mostrando P16 como si hubiera servidor.
  - `FakeInventarioRepository` (en memoria) se conserva para las pruebas unitarias.
  - Futuro: `RemoteInventarioRepository`, con llamadas HTTP al backend Spring Boot (Caso §3.4).
- Reglas de negocio puras en `domain/ReglasInventario` (cálculo de stock resultante, estado del stock y validaciones). En E2 las usan el repositorio local y la vista previa de "stock resultante". Cuando exista el backend, él será la autoridad y la app las usará solo para la vista previa.
- Sesión (usuario y ubicación activa) en memoria, en `SesionRepository`.
- Dependencias creadas a mano en `AppContainer` (sin Hilt ni Koin). `AppContainer` crea la base de datos de Room una sola vez para toda la app.
- **Recursos nativos** (cámara con linterna, archivos, compartir y vibración):
  - Viven fuera de los repositorios de inventario, en `dispositivo/`, para que la lógica no dependa de Android y siga siendo fácil de probar.
  - La pantalla pide los permisos y abre los selectores del sistema, porque estas APIs necesitan una Activity.
  - El ViewModel decide qué hacer con el resultado.
- El texto del CSV lo arma una función pura (`domain/ReporteCsv`), con pruebas unitarias. Escribirlo en el archivo elegido es tarea de `dispositivo/`.

## Estructura de paquetes
```
com.byto.inventario
├── BytoApp.kt                 (Application: crea el AppContainer)
├── MainActivity.kt
├── di/AppContainer.kt
├── domain/                    (ReglasInventario, ValidadorEan, ValidacionLogin, ValidacionReposicion, Permisos, Fechas, Regiones, ReporteCsv: lógica pura)
├── data/
│   ├── model/                 (Ubicacion, Producto, Existencia, Movimiento, Alerta, Usuario, enums)
│   ├── repository/            (InventarioRepository, SesionRepository)
│   ├── local/                 (Room: BytoDatabase, entidades, DAOs, conversores, LocalInventarioRepository)
│   └── fake/                  (FakeInventarioRepository para pruebas, DatosSinteticos para la carga inicial)
├── dispositivo/               (Vibrador, EscritorArchivos: acceso a recursos del teléfono)
└── ui/
    ├── theme/                 (colores y tipografía accesibles)
    ├── navigation/            (rutas y NavHost)
    ├── components/            (botones, tarjetas, chips de estado reutilizables)
    └── screens/               (splash, login, seleccion, inicio, inventario, detalle, buscar, escaner, movimiento,
                                alertas, reposicion, historial, ubicaciones, exportar, sinconexion)
```

## Librerías
| Librería | Para qué | Cuándo | Estado |
|---|---|---|---|
| Jetpack Compose, Material 3, Lifecycle/ViewModel | Base del proyecto (vienen en la plantilla) | E2 | Incluidas |
| Navigation Compose | Navegar entre pantallas | E2 | Aprobada |
| Room (runtime + ktx + compiler) | Persistencia local en SQLite | E2 | Aprobada |
| KSP (plugin de Gradle) | Genera el código de Room a partir de las anotaciones | E2 | Aprobada |
| CameraX (core, camera2, lifecycle, view) | Mostrar la cámara y entregar imágenes al lector | E2 | Aprobada |
| ML Kit Barcode Scanning (modelo incluido) | Detectar códigos en las imágenes, sin conexión (la app solo acepta EAN-13) | E2 | Aprobada |
| Core SplashScreen (`androidx.core:core-splashscreen`) | Splash del sistema al abrir la app, igual en todas las versiones de Android | E2 | Aprobada |
| Retrofit + convertidor JSON | Llamar a la API REST | Futuro | A decidir |

La vibración y el guardado de archivos usan APIs propias de Android (`Vibrator` y "Guardar como" del sistema) y no requieren librerías.

## Permisos de Android
| Permiso | Para qué | Tipo |
|---|---|---|
| `CAMERA` | Escáner de P17 | Se pide al usuario la primera vez que abre el escáner |
| `VIBRATE` | Vibración al leer un código | Se concede al instalar |

No se piden otros permisos. El CSV se guarda con el selector del sistema, que no necesita permiso de almacenamiento.

## Convenciones del contrato
- Base: `/api/v1`. JSON en camelCase. Enums en MAYÚSCULAS.
- Fechas: ISO-8601 en UTC, por ejemplo `2026-10-16T13:10:00Z`. La hora del movimiento la asigna el servidor, no el teléfono.
- Errores: `{ "codigo": "STOCK_INSUFICIENTE", "mensaje": "La salida (5) supera el stock actual (3)." }`
- Códigos HTTP: 200 OK, 201 creado, 400 datos inválidos, 404 no existe, 409 conflicto de regla (por ejemplo, ubicación inactiva).
- Autenticación: en E2 el login pide ID de usuario y PIN de 6 dígitos, y el repositorio local los verifica (endpoint 13). Cuando exista el backend, el servidor verificará las credenciales, devolverá un token y obtendrá el operador desde la sesión o el token, no desde el cuerpo de la petición.

## Endpoints
En E2 no hay servidor: cada endpoint corresponde a una función de `InventarioRepository`, que implementa el repositorio local. La columna "E2" indica si la app ya lo usa.

| # | Método y ruta | Para qué | Perfil | E2 |
|---|---|---|---|---|
| 1 | `GET /usuarios/demo` | Lista de usuarios ficticios, sin PIN (filtro de operador del historial) | Encargado, Gerente | Sí |
| 2 | `GET /ubicaciones?usuarioId=` | Ubicaciones asignadas al usuario (todas si es Gerente) | Todos | Sí |
| 3 | `PATCH /ubicaciones/{id}` | Habilitar o deshabilitar una ubicación `{ "estado": "ACTIVA" }` | Gerente | Sí |
| 4 | `GET /ubicaciones/{id}/existencias?q=&estado=` | Inventario de una ubicación con búsqueda y filtro (NORMAL, BAJO, CRITICO, SIN_STOCK) | Operador, Encargado, Gerente | Sí |
| 5 | `GET /ubicaciones/{id}/existencias/{codigoInterno}` | Detalle de un producto en esa ubicación | Todos | Sí |
| 6 | `GET /productos/ean/{ean}` | Buscar producto por código de barras | Operador | Sí (búsqueda manual P08 y escáner P17) |
| 7 | `POST /movimientos` | Registrar entrada, salida, ajuste o conteo | Operador | Sí |
| 8 | `GET /movimientos?ubicacionId=&usuarioId=&tipo=&desde=&hasta=` | Historial y auditoría filtrable (Caso §4.1) | Gerente; Encargado (sus ubicaciones); Operador (propios) | Sí |
| 9 | `GET /alertas?ubicacionId=&tipo=&estado=` | Alertas abiertas o cerradas | Todos (según asignación) | Sí |
| 10 | `POST /alertas/reposicion` | Declarar reposición `{ "ubicacionId", "codigoInterno", "observacion" }` (observación opcional) | Encargado | Sí |
| 11 | `PATCH /alertas/{id}` | Marcar descalce como revisado `{ "estado": "REVISADA" }` | Encargado, Gerente | Sí |
| 12 | `GET /tablero` | Resumen: totales por ubicación y alertas abiertas | Gerente | Sí |
| 13 | `POST /sesion` | Iniciar sesión `{ "usuarioId", "pin" }`; devuelve el usuario | Todos | Sí (login P01) |

La exportación CSV (P15) no es un endpoint: la app arma el archivo con los datos que ya obtiene de los endpoints 4, 8 y 9 (Caso §3.4: "se simulará localmente").

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
Una función `suspend` por endpoint; el número es el del endpoint. `Usuario` no incluye el PIN: solo `autenticar` lo recibe. La implementan `LocalInventarioRepository` (E2, Room) y `FakeInventarioRepository` (pruebas). En el futuro la implementará también `RemoteInventarioRepository`. Código en `data/repository/InventarioRepository.kt`.

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
        ubicacionId: String, codigoInterno: String, declaradaPor: String,
        observacion: String? = null                                                      // nuevo en E2 (mockup P12)
    ): Alerta
    suspend fun marcarDescalceRevisado(alertaId: String): Alerta                         // 11
    suspend fun obtenerTablero(): Tablero                                                // 12
    suspend fun autenticar(usuarioId: String, pin: String): Usuario                      // 13
}
```

Tipos de apoyo (en `data/model/`):
- `ExistenciaDetalle(existencia, producto, estado)`: fila del inventario con su producto y estado calculado.
- `NuevoMovimiento(tipo, ubicacionId, codigoInterno, cantidad, operadorId, motivo?, observacion?)`: cuerpo del POST. `operadorId` lo pone la app desde la sesión mientras no haya servidor; con el backend, el servidor lo obtendrá de la sesión o el token.
- `ResultadoMovimiento(movimiento, alertasGeneradas)`: respuesta del POST.
- `Alerta` suma en E2 el campo `observacion?`, que solo usa REPOSICION (máx. 200 caracteres).
- `FiltroMovimientos(ubicacionId?, usuarioId?, tipo?, desde?, hasta?)`: filtros del historial (`null` = sin filtrar; fechas inclusivas).
- `Tablero(resumenes, totalAlertasAbiertas)` y `ResumenUbicacion(ubicacion, totalUnidades, productosNormal, productosBajo, productosCritico, productosSinStock, alertasAbiertas)`.

### Errores
- `SinConexionException`: no hay comunicación con el servidor (en E2 la lanza el interruptor oculto del repositorio local).
- `ReglaNegocioException(codigo, mensaje)`: equivale al JSON de error. Códigos usados:

| Código | HTTP | Cuándo |
|---|---|---|
| `NO_ENCONTRADO` | 404 | Usuario, ubicación, existencia o alerta inexistente |
| `PIN_INCORRECTO` | 401 | El PIN no corresponde al usuario (login) |
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
