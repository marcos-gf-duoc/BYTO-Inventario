# BYTO Inventario

App móvil de gestión de inventario multi-tienda para Android, desarrollada como proyecto académico en la asignatura **DSY1105 — Desarrollo de Aplicaciones Móviles** · Duoc UC, Escuela de Informática y Telecomunicaciones.

---

## Descripción

BYTO Inventario permite a operadores, encargados de bodega y gerentes gestionar el stock de productos distribuidos en tiendas y bodegas de la red BYTO. La app reemplaza el registro manual en planillas Excel por una solución móvil que centraliza movimientos, alertas y reportes en tiempo real.

---

## Capturas de pantalla

> _Próximamente_

---

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM |
| Navegación | Navigation Compose |
| Estado | StateFlow + ViewModel |
| Inyección de dependencias | Manual (AppContainer) |
| Control de versiones | Git + GitHub |
| IDE | Android Studio |

---

## Estructura del proyecto

```
com.byto.inventario
├── di/                  Inyección de dependencias (AppContainer, FabricaViewModels)
├── domain/              Lógica de negocio pura (ReglasInventario, ValidadorEan, Permisos)
├── data/
│   ├── model/           Modelos de datos (Producto, Movimiento, Alerta, Ubicacion…)
│   ├── repository/      Contratos e implementaciones (InventarioRepository, SesionRepository)
│   └── fake/            Datos sintéticos y repositorio en memoria para E1
└── ui/
    ├── theme/           Colores y tipografía accesibles
    ├── navigation/      Rutas, NavHost y SesionViewModel
    ├── components/      Componentes reutilizables
    └── screens/         Pantallas organizadas por funcionalidad
```

---

## Perfiles de usuario

| Perfil | Qué puede hacer |
|---|---|
| **Operador** | Seleccionar tienda, escanear productos, registrar movimientos (entrada, salida, ajuste, conteo) |
| **Encargado de bodega** | Consultar stock, declarar reposiciones, revisar descalces |
| **Gerente** | Tablero analítico, administrar ubicaciones, revisar historial y alertas globales |

---

## Pantallas implementadas (Entrega 1)

| Código | Pantalla |
|---|---|
| P01 | Login (selección de usuario simulado) |
| P02 | Selección de ubicación |
| P03 | Inicio Operador |
| P04 | Inicio Encargado |
| P05 | Inicio Gerente |
| P06 | Inventario de la ubicación activa |
| P07 | Detalle de producto |
| P08 | Buscar por código de barras (manual) |
| P09 | Registrar movimiento |
| P10 | Confirmación de movimiento |
| P16 | Sin conexión |

---

## Datos sintéticos (Entrega 1)

La app opera sin backend en esta etapa. Los datos se generan en memoria al iniciar:

- **13 ubicaciones**: Casa Matriz, 3 bodegas regionales y 9 tiendas (Santiago, La Serena, Concepción)
- **4 usuarios simulados**: 2 operadores, 1 encargado de bodega y 1 gerente
- **25 productos** en 5 categorías: Tecnología, Deportes, Ropa Hombre, Ropa Mujer, Muebles
- **300 existencias** (12 ubicaciones × 25 productos)
- **6 movimientos históricos** y **7 alertas iniciales** preconfiguradas

---

## Reglas de negocio

| Movimiento | Efecto en stock | Validaciones |
|---|---|---|
| Entrada | stock + cantidad | cantidad > 0 |
| Salida | stock − cantidad | cantidad > 0; no superar stock actual |
| Ajuste | stock ± cantidad | cantidad ≠ 0; motivo obligatorio; resultado ≥ 0 |
| Conteo | stock = contado | cantidad ≥ 0; genera alerta de descalce si difiere |

---

## Cómo ejecutar el proyecto

1. Clonar el repositorio:
```bash
git clone https://github.com/marcos-gf-duoc/BYTO-Inventario.git
```
2. Abrir en **Android Studio** (versión Hedgehog o superior recomendada)
3. Seleccionar un emulador o dispositivo con **Android 8.0 (API 26)** o superior
4. Ejecutar con **Run › Run 'app'**

> No se requiere backend ni configuración adicional para la Entrega 1.

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
