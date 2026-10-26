# CLAUDE.md — Proyecto BYTO

Instrucciones para Claude Code en este repositorio. Léelas completas antes de cada tarea.

## Qué es este proyecto
- App Android **BYTO**: inventario multitienda (MVP académico, asignatura DSY1105, Duoc UC). Proyecto **grupal** de dos estudiantes que están aprendiendo.
- Simulación pedagógica: **todos los datos son ficticios**.
- Las decisiones de negocio están en `docs/decisiones_E1.md` y la arquitectura y el contrato de la API en `docs/arquitectura_API.md`. **Esos documentos mandan**: no inventes requisitos, ubicaciones, usuarios, productos ni reglas que no estén ahí. Si algo falta o es ambiguo, pregunta antes de decidir.

## Stack
- Kotlin, Jetpack Compose, Material 3, MVVM, Navigation Compose.
- Un solo módulo (`app`). Paquete raíz: `com.byto.inventario`. `minSdk` 26.
- Entrega 1: **sin backend ni base de datos**. Los datos viven en memoria en `FakeInventarioRepository` (retardo simulado ~300 ms e interruptor de "sin conexión"). En el futuro se agregará `RemoteInventarioRepository` (Retrofit hacia Spring Boot) cumpliendo la misma interfaz `InventarioRepository`.
- Dependencias creadas a mano en `di/AppContainer.kt` (sin Hilt ni Koin).
- **No agregues librerías nuevas** sin preguntar primero y explicar para qué sirven. Aprobadas por ahora: las de la plantilla de Android Studio y Navigation Compose.

## Estructura de paquetes
```
com.byto.inventario
├── BytoApp.kt, MainActivity.kt
├── di/AppContainer.kt
├── domain/ReglasInventario.kt        (reglas puras: stock resultante, estado del stock, validaciones)
├── data/model/                       (Ubicacion, Producto, Existencia, Movimiento, Alerta, Usuario, enums)
├── data/repository/                  (InventarioRepository, SesionRepository)
├── data/fake/                        (FakeInventarioRepository, DatosSinteticos)
└── ui/theme, ui/navigation, ui/components, ui/screens/<pantalla>/ (Pantalla + ViewModel)
```

## Reglas de arquitectura
- Los Composables solo dibujan y envían eventos; **nunca** acceden a repositorios.
- Cada pantalla tiene su ViewModel, que expone el estado con `StateFlow` y recibe las acciones del usuario.
- Los ViewModels dependen de la **interfaz** `InventarioRepository`, no de la implementación.
- Las reglas de negocio (umbrales, cálculo de stock, validaciones) viven solo en `domain/ReglasInventario.kt`.
- Las fechas se guardan en UTC (`java.time.Instant`) y se muestran en hora de Chile (`America/Santiago`).
- Los movimientos guardan el **ID anonimizado** del operador (por ejemplo `OP-001`), nunca un nombre.

## Accesibilidad (obligatoria, Caso §3.3)
- Elementos táctiles de **48 dp** como mínimo.
- Contraste de texto de al menos **4,5:1** (3:1 en textos grandes e íconos). No uses texto gris claro sobre fondos claros.
- Todo ícono o imagen con significado lleva `contentDescription` en español; los decorativos, `null`.
- El estado del stock nunca se comunica solo con color: siempre con texto ("Bajo", "Crítico", "Sin stock") además del color.
- Textos en `sp` y que soporten el aumento de tamaño de letra del sistema.

## Estilo de código
- Nombres de clases, funciones y variables de dominio en **español** (`Existencia`, `registrarMovimiento`), coherentes con los documentos.
- Comentarios en español, breves, explicando el **porqué**. El estudiante está aprendiendo: comenta lo que no sea obvio para un principiante.
- Textos visibles al usuario en `res/values/strings.xml`.
- Código simple antes que ingenioso.

## Forma de trabajar
- **Responde siempre en español**, incluidos los resúmenes y las explicaciones.
- Haz **un paso pequeño a la vez**. Al terminar cada paso:
  1. Resume qué archivos creaste o cambiaste y por qué.
  2. Explica en pocas líneas los conceptos nuevos.
  3. Indica cómo ejecutarlo y **qué debería verse** en el emulador.
- El proyecto debe **compilar al final de cada paso**. Si cambias algo de Gradle, avisa que hay que sincronizar.
- **No hagas commits ni push.** El estudiante revisa y hace los commits él mismo. Si un paso completa un hito, sugiere un mensaje de commit.
- No borres ni reescribas archivos que no tengan relación con la tarea.

## Seguridad y privacidad (Leyes 19.628 y 21.719, Caso §5.1)
- Nunca incluyas claves, tokens, contraseñas ni credenciales en el código o en el repositorio.
- Nada de datos personales reales: ni nombres, ni RUT, ni correos, ni direcciones, ni marcas comerciales reales.
- No pidas permisos que la app no necesita (por ejemplo, ubicación).
