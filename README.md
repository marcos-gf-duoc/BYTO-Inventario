# BYTO Inventario

App móvil de gestión de inventario multi-tienda para Android, desarrollada como proyecto académico en la asignatura **DSY1105 — Desarrollo de Aplicaciones Móviles** · Duoc UC, Escuela de Informática y Telecomunicaciones.

**Entrega 2 — Evaluación Parcial 2.** Todos los datos de la app son ficticios.

---

## Descripción

BYTO Inventario permite a operadores, encargados de bodega y gerentes gestionar el stock de productos distribuidos en las tiendas y bodegas de la red BYTO. Reemplaza el registro manual en planillas Excel por una app que registra movimientos, genera alertas automáticas y exporta reportes.

En esta entrega la app funciona sin backend: los datos se guardan en el teléfono con Room. La conexión con un backend Spring Boot (API REST) queda para una entrega posterior, sin cambiar las pantallas.

---

## Capturas de pantalla

Tomadas en el emulador con los datos sintéticos; están en [`docs/capturas/`](docs/capturas/).

| Pantalla | Capturas |
|---|---|
| P00 Splash Screen | Animación de carga inicial y SplashScreen API |
| P01 Login | Login con ID de usuario y PIN de 6 dígitos |
| P08 Buscar por código | [Resultado](docs/capturas/P08_buscar_resultado.png) · [Error de validación](docs/capturas/P08_buscar_error_validacion.png) |
| P09 Registrar movimiento | [Stock resultante](docs/capturas/P09_registrar_stock_resultante.png) · [Error de validación](docs/capturas/P09_registrar_error_validacion.png) |
| P11 Alertas | [Alertas abiertas](docs/capturas/P11_alertas.png) |
| P12 Declarar reposición | [Campos obligatorios](docs/capturas/P12_reposicion_campos_obligatorios.png) · [Reposición ya abierta](docs/capturas/P12_reposicion_ya_abierta.png) |
| P13 Historial | [Movimientos](docs/capturas/P13_historial.png) · [Error de rango de fechas](docs/capturas/P13_historial_error_rango.png) |
| P14 Ubicaciones | [Listado](docs/capturas/P14_ubicaciones.png) · [Confirmar desactivación](docs/capturas/P14_ubicaciones_confirmar.png) |
| P15 Exportar reporte | [Formulario](docs/capturas/P15_exportar.png) · [Compartir CSV](docs/capturas/P15_exportar_error_validacion.png) |
| P17 Escanear código | [Sin permiso de cámara](docs/capturas/P17_escaner_sin_permiso.png) · [Vista de la cámara (emulador)](docs/capturas/P17_escaner_camara_emulador.png) |

---

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM (pantalla → ViewModel → repositorio) |
| Navegación | Navigation Compose |
| Estado | StateFlow + ViewModel |
| Persistencia local | Room (SQLite), con KSP |
| Cámara y escáner | CameraX + ML Kit Barcode Scanning (compatible con todos los formatos: EAN, QR, Code 128, etc.) |
| Inyección de dependencias | Manual (AppContainer) |
| Pruebas | JUnit (unitarias) y AndroidJUnit4 (instrumentadas, con Room en memoria) |
| Control de versiones | Git + GitHub |

---

## Estructura del proyecto

```
com.byto.inventario
├── di/                  Inyección de dependencias (AppContainer, FabricaViewModels)
├── domain/              Lógica pura con pruebas: ReglasInventario, ValidadorEan, Permisos,
│                        Fechas, Regiones, ValidacionReposicion, ReporteCsv
├── data/
│   ├── model/           Modelos (Producto, Existencia, Movimiento, Alerta, Ubicacion, Usuario con PIN…)
│   ├── repository/      Contratos (InventarioRepository, SesionRepository, SimuladorConexion)
│   ├── local/           Room: BytoDatabase, entidades, DAOs y LocalInventarioRepository
│   └── fake/            Datos sintéticos y repositorio en memoria (para pruebas)
├── dispositivo/         Recursos del teléfono: Vibrador, EscritorArchivos
└── ui/
    ├── theme/           Colores y tipografía accesibles
    ├── navigation/      Rutas, NavHost y SesionViewModel
    ├── components/      Componentes reutilizables (chips de estado, MensajeError…)
    └── screens/         Una carpeta por pantalla (incluyendo splash y login), con su Pantalla y ViewModel
```

---

## Perfiles de usuario

| Perfil | Qué puede hacer |
|---|---|
| **Operador** | Seleccionar su tienda, escanear o buscar productos, registrar movimientos (entrada, salida, ajuste, conteo) y ver su historial |
| **Encargado de bodega** | Consultar stock, ver alertas, revisar descalces, declarar reposiciones y ver el historial de sus ubicaciones |
| **Gerente** | Tablero analítico, alertas e historial de todas las ubicaciones, administrar ubicaciones y exportar/compartir reportes |

---

## Pantallas implementadas

| Código | Pantalla |
|---|---|
| P00 | Splash Screen: pantalla de carga inicial animada en Jetpack Compose |
| P01 | Login profesional con credenciales (ID de usuario y contraseña de 6 dígitos / PIN) |
| P02 | Selección de ubicación |
| P03 | Inicio Operador |
| P04 | Inicio Encargado |
| P05 | Inicio Gerente |
| P06 | Inventario de la ubicación activa |
| P07 | Detalle de producto |
| P08 | Buscar por código de barras (manual o con cámara) |
| P09 | Registrar movimiento |
| P10 | Confirmación de movimiento |
| P11 | Alertas abiertas con filtros por tipo y "Marcar revisado" para descalces |
| P12 | Declarar reposición (Encargado), con validación por campo |
| P13 | Historial / auditoría con filtros por ubicación, fechas, operador y tipo |
| P14 | Administrar ubicaciones (Gerente): activar o desactivar tiendas y bodegas |
| P15 | Exportar reporte (Gerente): stock, movimientos o alertas en CSV con opción de compartir |
| P16 | Sin conexión |
| P17 | Escanear código: lectura universal de códigos de barras y QR (CameraX + ML Kit) y vibración al leer |

### Funcionalidades transversales implementadas

| Funcionalidad | Detalle |
|---|---|
| Persistencia local | Room: movimientos, alertas y estado de las ubicaciones se conservan al cerrar la app |
| Recursos nativos | Cámara (escáner universal), almacenamiento ("Guardar como"), compartir archivos (`Intent.ACTION_SEND` para CSV) y vibración |
| Formularios validados | Errores por campo con ícono y texto en todos los formularios (P01, P08, P09, P12, P13 y P15); reglas en `domain/` con pruebas unitarias |
| Animaciones | Transiciones entre pantallas, splash de carga (P00), resultado de búsqueda (P08), stock resultante (P09), confirmación (P10), filas de listas (P11, P13), estado de ubicaciones (P14), marco del escáner (P17) y mensajes de error |
| Accesibilidad | Botones de 48 dp o más, contraste ≥ 4,5:1, estados siempre con texto además del color y descripciones para TalkBack |
| Privacidad | Autenticación con ID y PIN de 6 dígitos; los datos no se respaldan en la nube; únicos permisos: cámara y vibración |

---

## Datos sintéticos

En el primer arranque la app guarda en Room estos datos ficticios; desde ahí, cada cambio queda guardado:

- **13 ubicaciones**: Casa Matriz, 3 bodegas regionales y 9 tiendas (Santiago, La Serena, Concepción)
- **4 usuarios simulados con PIN de 6 dígitos**: 2 operadores, 1 encargado de bodega y 1 gerente
- **25 productos** en 5 categorías: Tecnología, Deportes, Ropa Hombre, Ropa Mujer, Muebles
- **300 existencias** (12 ubicaciones × 25 productos)
- **6 movimientos históricos** y **7 alertas iniciales** preconfiguradas

| Usuario | Perfil | PIN | Ubicaciones |
|---|---|---|---|
| OP-001 · Operario Santiago | Operador | 123456 | T-ST-1, T-ST-2, T-ST-3 |
| OP-002 · Operario La Serena | Operador | 123457 | T-LS-1, T-LS-2, T-LS-3 |
| EB-001 · Encargado Bodega Santiago | Encargado de bodega | 123458 | B-ST-1 y consulta de T-ST-1 a T-ST-3 |
| GE-001 · Gerente BYTO | Gerente | 123459 | Todas |

---

## Reglas de negocio

| Movimiento | Efecto en stock | Validaciones |
|---|---|---|
| Entrada | stock + cantidad | cantidad > 0 |
| Salida | stock − cantidad | cantidad > 0; no superar stock actual |
| Ajuste | stock ± cantidad | cantidad ≠ 0; motivo obligatorio; resultado ≥ 0 |
| Conteo | stock = contado | cantidad ≥ 0; genera alerta de descalce si difiere |

Umbrales de stock: Normal ≥ 10 · Bajo 5–9 · Crítico 1–4 · Sin stock 0. Bajo 10 se abre automáticamente una alerta de stock bajo.

---

## Cómo ejecutar el proyecto

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/marcos-gf-duoc/BYTO-Inventario.git
   ```
2. Abrir la carpeta en **Android Studio** (una versión reciente, compatible con Android Gradle Plugin 9.4).
3. Esperar a que termine la sincronización de Gradle (o usar **File › Sync Project with Gradle Files**).
4. Elegir un emulador o teléfono con **Android 8.0 (API 26)** o superior.
5. Ejecutar con **Run › Run 'app'** y autenticarse con el ID de usuario y PIN de 6 dígitos en el login (P01).

> No se requiere backend, cuentas ni claves.

**Probar el escáner (P17):** abre `docs/anexos/codigos_prueba_EAN13.html` en el navegador (o imprímelo al 100 %) y escanea cualquiera de los 25 códigos (o cualquier código QR/barras real) desde Buscar por código › Escanear con cámara. En el emulador, configura la cámara trasera como *Webcam0* en el Device Manager, o usa un teléfono real.

**Ver la pantalla Sin conexión (P16):** en el login, mantén presionado el título "BYTO" para cortar la conexión simulada; repite el gesto para restablecerla.

**Volver a los datos iniciales:** Ajustes de Android › Apps › BYTO › Almacenamiento › Borrar datos.

### Pruebas

```bash
./gradlew testDebugUnitTest            # pruebas unitarias (reglas del dominio, repositorio en memoria y login)
./gradlew connectedDebugAndroidTest    # pruebas instrumentadas (Room y lector de códigos), con un emulador encendido
```

También se pueden ejecutar desde Android Studio: clic derecho sobre la carpeta `test` o `androidTest` › **Run Tests**.

---

## Equipo

| Integrante | Rol |
|---|---|
| Marcos Godoy | Desarrollo |
| Caden Nieto | Desarrollo |

**Docente:** Freddy Campos  
**Sede:** Padre Alonso de Ovalle  
**Asignatura:** DSY1105 — Desarrollo de Aplicaciones Móviles  
**Institución:** Duoc UC · Escuela de Informática y Telecomunicaciones
