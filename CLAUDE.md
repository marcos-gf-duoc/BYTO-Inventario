# CLAUDE.md — Proyecto BYTO

Instrucciones para Claude Code en este repositorio. Léelas completas antes de cada tarea.

## Qué es este proyecto
- App Android **BYTO**: inventario multitienda (MVP académico, asignatura DSY1105, Duoc UC). Proyecto **grupal** de dos estudiantes que están aprendiendo.
- Simulación pedagógica: **todos los datos son ficticios**.
- Entrega actual: **Entrega 2 = Evaluación Parcial 2** de DSY1105 (encargo en parejas). La rúbrica y cómo se cumple están en `docs/decisiones_E1.md`, sección "Requisitos de la rúbrica".
- Las decisiones de negocio están en `docs/decisiones_E1.md` (el nombre del archivo se conserva, pero su contenido es de la E2) y la arquitectura y el contrato de la API en `docs/arquitectura_API.md`. **Esos documentos mandan**: no inventes requisitos, ubicaciones, usuarios, productos ni reglas que no estén ahí. Si algo falta o es ambiguo, pregunta antes de decidir.
- Material de referencia **solo local** (excluido en `.gitignore`, puede no existir en otro clon):
  - `docs/002_Formato_Caso_MultiTienda.pdf`: el Caso.
  - `docs/Ev. Parcial 2_Estudiante_Encargo_DSY1105.pdf`: la pauta del encargo.
  - `docs/Evaluación Parcial 3_Estudiante_Presentación_DSY1105.pdf`: la rúbrica de la presentación.
  - `docs/BYTO_mockups_1080x2400/`: los mockups P01 a P16.

## Stack
- Kotlin, Jetpack Compose, Material 3, MVVM, Navigation Compose.
- Un solo módulo (`app`). Paquete raíz: `com.byto.inventario`. `minSdk` 26.
- Entrega 2: **sin backend, con persistencia local en Room**.
  - `LocalInventarioRepository` guarda los datos en el teléfono, carga `DatosSinteticos` en el primer arranque y conserva el retardo simulado (~300 ms) y el interruptor de "sin conexión".
  - `FakeInventarioRepository` (en memoria) queda para las pruebas.
  - En el futuro se agregará `RemoteInventarioRepository` (Retrofit hacia Spring Boot), que cumplirá la misma interfaz `InventarioRepository`.
- Recursos nativos: **cámara** (escáner EAN-13, con linterna), **guardado de archivos** (reporte CSV), **compartir** el CSV con otras apps y **vibración**.
- Dependencias creadas a mano en `di/AppContainer.kt` (sin Hilt ni Koin).
- **No agregues librerías nuevas** sin preguntar primero y explicar para qué sirven. Aprobadas por ahora:
  - las de la plantilla de Android Studio;
  - Navigation Compose;
  - Room, con el plugin KSP;
  - CameraX;
  - ML Kit Barcode Scanning, en su versión con el modelo incluido;
  - Core SplashScreen (`androidx.core:core-splashscreen`), para el splash del sistema.

## Estructura de paquetes
```
com.byto.inventario
├── BytoApp.kt, MainActivity.kt
├── di/AppContainer.kt
├── domain/                           (lógica pura: ReglasInventario, ValidadorEan, ValidacionLogin, ValidacionReposicion, Permisos, Fechas, Regiones, ReporteCsv)
├── data/model/                       (Ubicacion, Producto, Existencia, Movimiento, Alerta, Usuario, enums)
├── data/repository/                  (InventarioRepository, SesionRepository)
├── data/local/                       (Room: BytoDatabase, entidades, DAOs, LocalInventarioRepository)
├── data/fake/                        (FakeInventarioRepository para pruebas, DatosSinteticos)
├── dispositivo/                      (Vibrador, EscritorArchivos: recursos del teléfono)
└── ui/theme, ui/navigation, ui/components, ui/screens/<pantalla>/ (Pantalla + ViewModel)
```

## Reglas de arquitectura
- Los Composables solo dibujan y envían eventos; **nunca** acceden a repositorios.
- Cada pantalla tiene su ViewModel, que expone el estado con `StateFlow` y recibe las acciones del usuario.
- Los ViewModels dependen de la **interfaz** `InventarioRepository`, no de la implementación.
- Las reglas de negocio (umbrales, cálculo de stock, validaciones) viven solo en `domain/`. Los ViewModels las usan para validar cada campo, y la pantalla solo muestra el error que recibe en el estado.
- Las entidades y DAOs de Room no salen de `data/local/`. El repositorio las convierte a los modelos de `data/model/`, para que la UI no dependa de la base de datos.
- Los recursos nativos se usan a través de `dispositivo/`. Los permisos y los selectores del sistema se lanzan desde la pantalla y su resultado se entrega al ViewModel como evento.
- Cada formulario valida **por campo**, con mensaje e ícono de error. Toda pantalla nueva debe incluir animaciones que den retroalimentación (ver "Animaciones" en `docs/decisiones_E1.md`).
- Las fechas se guardan en UTC (`java.time.Instant`) y se muestran en hora de Chile (`America/Santiago`).
- Los movimientos guardan el **ID anonimizado** del operador (por ejemplo `OP-001`), nunca un nombre.

## Accesibilidad (obligatoria, Caso §3.3)
- Elementos táctiles de **48 dp** como mínimo.
- Contraste de texto de al menos **4,5:1** (3:1 en textos grandes e íconos). No uses texto gris claro sobre fondos claros.
- Todo ícono o imagen con significado lleva `contentDescription` en español; los decorativos, `null`.
- El estado del stock nunca se comunica solo con color: siempre con texto ("Bajo", "Crítico", "Sin stock") además del color.
- Textos en `sp` y que soporten el aumento de tamaño de letra del sistema.
- Los errores de formulario nunca se comunican solo con color: siempre ícono y texto.
- La vibración nunca es la única señal: siempre va con un aviso visual.
- La vista de la cámara tiene `contentDescription` y botón Volver de 48 dp.

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
- **No hagas commits ni push.** El estudiante revisa y hace los commits él mismo. Si un paso completa un hito, sugiere un mensaje de commit técnico y descriptivo (la pauta penaliza los mensajes no técnicos).
- Después de la fecha de entrega no se toca el repositorio (la pauta lo sanciona con nota 1.0). Si se pide un cambio en esa situación, advierte antes de hacerlo.
- No borres ni reescribas archivos que no tengan relación con la tarea.

## Seguridad y privacidad (Leyes 19.628 y 21.719, Caso §5.1)
- Nunca incluyas claves, tokens, contraseñas ni credenciales en el código o en el repositorio.
  - Única excepción: los PIN **ficticios** de los 4 usuarios de demostración, escritos solo en `DatosSinteticos.pinesDemo`. No se repiten en pantallas, `strings.xml`, README ni pruebas de interfaz; para la presentación se listan en la guía de demostración.
  - Los PIN solo los lee el repositorio en `autenticar`. `Usuario`, los ViewModels y las pantallas no los contienen ni los reciben.
- Nada de datos personales reales: ni nombres, ni RUT, ni correos, ni direcciones, ni marcas comerciales reales.
- Permisos permitidos: solo `CAMERA` (pedido al usarlo, con manejo del rechazo) y `VIBRATE`. No pidas otros (ni ubicación ni almacenamiento: el CSV se guarda con el selector del sistema).
- No subas al repositorio el material de referencia de `docs/` que está en `.gitignore`.
- El respaldo automático de Android está desactivado (`allowBackup="false"` y reglas en `res/xml/` que excluyen todo). No lo vuelvas a activar.
