# BYTO — Registro de decisiones (Entrega 1)

Documento vivo con las decisiones confirmadas por el estudiante. Fuente para las siguientes etapas y base del futuro `CLAUDE.md` del repositorio. La arquitectura y el contrato de la API están en `docs/arquitectura_API.md`.

## Contexto de la Entrega 1 (E1)
- Foco: diseño y funcionamiento básico.
- Sin backend (lo construirá el estudiante después, con un supervisor) y sin base de datos.
- Datos en memoria: se reinician al cerrar la app (aceptado).
- Escáner con cámara: **fuera de E1** (decisión del estudiante; validar con el docente porque el Caso §3.3 lo declara obligatorio).
- QR: fuera del alcance.

## Estructura de BYTO
- Casa Matriz (Santiago): edificio administrativo, **sin stock**.
- 12 ubicaciones con inventario: 3 bodegas regionales y 9 tiendas.
- Formato de ID: `Tipo-Región-Número`. Tipo: T = tienda, B = bodega. Región siempre en 2 letras: ST = Santiago, LS = La Serena, CC = Concepción.

| ID | Nombre | Tipo | Depende de | Estado inicial |
|---|---|---|---|---|
| CM | Casa Matriz BYTO | Administrativa (sin stock) | — | Activa |
| B-ST-1 | Bodega Regional Santiago | Bodega | CM | Activa |
| T-ST-1 | BYTO Santiago Centro | Tienda | B-ST-1 | Activa |
| T-ST-2 | BYTO Santiago Norte | Tienda | B-ST-1 | Activa |
| T-ST-3 | BYTO Santiago Sur | Tienda | B-ST-1 | Activa |
| B-LS-1 | Bodega Regional La Serena | Bodega | CM | Activa |
| T-LS-1 | BYTO La Serena Centro | Tienda | B-LS-1 | Activa |
| T-LS-2 | BYTO La Serena Norte | Tienda | B-LS-1 | Activa |
| T-LS-3 | BYTO La Serena Sur | Tienda | B-LS-1 | Activa |
| B-CC-1 | Bodega Regional Concepción | Bodega | CM | Activa |
| T-CC-1 | BYTO Concepción Centro | Tienda | B-CC-1 | Activa |
| T-CC-2 | BYTO Concepción Norte | Tienda | B-CC-1 | Activa |
| T-CC-3 | BYTO Concepción Sur | Tienda | B-CC-1 | **Inactiva** (para demostrar la administración de tiendas) |

## Usuarios simulados (login sin contraseña, selección desde una lista)
| ID anonimizado | Etiqueta | Perfil | Ubicaciones |
|---|---|---|---|
| OP-001 | Operario Santiago | Operador | T-ST-1, T-ST-2, T-ST-3 |
| OP-002 | Operario La Serena | Operador | T-LS-1, T-LS-2, T-LS-3 |
| EB-001 | Encargado Bodega Santiago | Encargado de bodega | B-ST-1 + T-ST-1, T-ST-2, T-ST-3 (consulta) |
| GE-001 | Gerente BYTO | Gerencial | Todas |

## Umbrales de stock (globales, iguales para todos los productos y ubicaciones)
| Estado | Condición | Uso en E1 |
|---|---|---|
| Normal | stock ≥ 10 | — |
| Bajo | 5 ≤ stock ≤ 9 | Alerta automática de stock bajo |
| Crítico | 1 ≤ stock ≤ 4 | Alerta automática de stock bajo (nivel crítico) |
| Sin stock | stock = 0 | Caso especial de crítico |

- El atributo "stock mínimo" del Caso §3.2 se conserva con valor 10 para todos, de modo que se pueda personalizar más adelante.
- Reposición automática al llegar a crítico: **futuro**.

## Reglas de movimientos
| Tipo | Cantidad | Efecto en el stock | Validación |
|---|---|---|---|
| Entrada | > 0 | stock + cantidad | — |
| Salida | > 0 | stock − cantidad | No puede superar el stock actual |
| Ajuste | ≠ 0 (con signo) | stock ± cantidad | Motivo obligatorio; resultado ≥ 0 |
| Conteo | ≥ 0 (lo contado) | stock = cantidad contada | Si difiere del sistema → alerta de descalce |

- Conteo (confirmado): el conteo físico dedicado del operador sobrescribe al sistema, porque el sistema puede tener errores o no reflejar hechos fuera de él (por ejemplo, un producto perdido por mala gestión). La diferencia queda registrada como alerta de descalce.
- Todo movimiento guarda: tipo, producto, ubicación, cantidad, stock antes y después, marca de tiempo UTC, ID anonimizado del operador y observación (máx. 200 caracteres).

## Alertas
| Tipo | Origen | Se abre cuando | Se cierra cuando |
|---|---|---|---|
| Stock bajo (nivel bajo o crítico) | Automática | stock < 10 | stock ≥ 10 |
| Descalce | Automática (conteo) | contado ≠ sistema | Encargado o Gerente la marca como revisada |
| Reposición | Manual (Encargado) | El Encargado la declara | El operario registra una entrada del producto en esa tienda |
| Reposición automática por crítico | — | **Futuro** | — |

Reposición en E1: solo se registra la entrada en la tienda; la salida en la bodega (traspaso vinculado) queda para el futuro.

## Catálogo sintético (25 productos: 5 categorías × 5; EAN-13 ficticios con prefijo interno 2)
Sin marcas. Ropa en talla única M (las tallas como variantes quedan fuera de E1).

| EAN-13 | Código interno | Nombre | Categoría |
|---|---|---|---|
| 2000000000015 | TE-01 | Audífonos inalámbricos | Tecnología |
| 2000000000022 | TE-02 | Mouse inalámbrico | Tecnología |
| 2000000000039 | TE-03 | Teclado USB | Tecnología |
| 2000000000046 | TE-04 | Parlante Bluetooth portátil | Tecnología |
| 2000000000053 | TE-05 | Cargador USB-C 20 W | Tecnología |
| 2000000000060 | DE-01 | Balón de fútbol N.º 5 | Deportes |
| 2000000000077 | DE-02 | Mancuernas 5 kg (par) | Deportes |
| 2000000000084 | DE-03 | Mat de yoga | Deportes |
| 2000000000091 | DE-04 | Botella deportiva 750 ml | Deportes |
| 2000000000107 | DE-05 | Cuerda para saltar | Deportes |
| 2000000000114 | RH-01 | Polera de algodón básica, talla M | Ropa Hombre |
| 2000000000121 | RH-02 | Jeans corte recto, talla M | Ropa Hombre |
| 2000000000138 | RH-03 | Polerón con capucha, talla M | Ropa Hombre |
| 2000000000145 | RH-04 | Camisa manga larga, talla M | Ropa Hombre |
| 2000000000152 | RH-05 | Chaqueta cortaviento, talla M | Ropa Hombre |
| 2000000000169 | RM-01 | Blusa manga corta, talla M | Ropa Mujer |
| 2000000000176 | RM-02 | Jeans tiro alto, talla M | Ropa Mujer |
| 2000000000183 | RM-03 | Vestido casual, talla M | Ropa Mujer |
| 2000000000190 | RM-04 | Polerón cuello redondo, talla M | Ropa Mujer |
| 2000000000206 | RM-05 | Chaqueta acolchada, talla M | Ropa Mujer |
| 2000000000213 | MU-01 | Silla de escritorio | Muebles |
| 2000000000220 | MU-02 | Mesa de centro | Muebles |
| 2000000000237 | MU-03 | Estante de 5 niveles | Muebles |
| 2000000000244 | MU-04 | Velador de 2 cajones | Muebles |
| 2000000000251 | MU-05 | Escritorio 120 cm | Muebles |

## Stock inicial
- Regla general: 100 unidades por producto en cada bodega y 20 en cada tienda activa; 0 en la tienda inactiva.
- Excepciones (escenarios de demostración en Santiago):

| Ubicación | Producto | Stock | Situación que muestra |
|---|---|---|---|
| T-ST-1 | Audífonos inalámbricos (TE-01) | 18 | Normal (tras una entrada) |
| T-ST-1 | Mat de yoga (DE-03) | 7 | Bajo |
| T-ST-1 | Polera de algodón (RH-01) | 3 | Crítico |
| T-ST-1 | Silla de escritorio (MU-01) | 0 | Sin stock + reposición declarada |
| T-ST-2 | Jeans tiro alto (RM-02) | 6 | Bajo producido por un descalce |
| B-ST-1 | Mancuernas 5 kg (DE-02) | 8 | Bajo en bodega |
| T-LS-1 | Parlante Bluetooth (TE-04) | 20 | Normal (tras una entrada) |

## Movimientos históricos iniciales (UTC)
| # | Fecha y hora UTC | Ubicación | Producto | Tipo | Cantidad | Antes → después | Operador | Observación |
|---|---|---|---|---|---|---|---|---|
| 1 | 2026-10-16T13:10:00Z | T-ST-1 | TE-01 | Entrada | 8 | 10 → 18 | OP-001 | Recepción semanal |
| 2 | 2026-10-16T18:40:00Z | T-ST-1 | DE-03 | Salida | 13 | 20 → 7 | OP-001 | Ventas del día |
| 3 | 2026-10-17T12:30:00Z | T-ST-2 | RM-02 | Conteo | 6 | 10 → 6 | OP-001 | Conteo de sección mujer |
| 4 | 2026-10-17T15:05:00Z | T-ST-1 | RH-01 | Ajuste | −2 | 5 → 3 | OP-001 | Motivo: producto dañado |
| 5 | 2026-10-18T14:20:00Z | T-ST-1 | MU-01 | Salida | 4 | 4 → 0 | OP-001 | Ventas del día |
| 6 | 2026-10-18T16:00:00Z | T-LS-1 | TE-04 | Entrada | 12 | 8 → 20 | OP-002 | Recepción semanal |

## Alertas iniciales
| Tipo | Ubicación | Producto | Origen | Estado |
|---|---|---|---|---|
| Stock bajo (bajo) | T-ST-1 | DE-03 | Movimiento 2 | Abierta |
| Descalce (−4) | T-ST-2 | RM-02 | Movimiento 3 | Abierta |
| Stock bajo (bajo) | T-ST-2 | RM-02 | Movimiento 3 | Abierta |
| Stock bajo (crítico) | T-ST-1 | RH-01 | Movimiento 4 | Abierta |
| Stock bajo (crítico, sin stock) | T-ST-1 | MU-01 | Movimiento 5 | Abierta |
| Reposición | T-ST-1 | MU-01 | Declarada por EB-001 (2026-10-18T17:00:00Z) | Abierta |
| Stock bajo (bajo) | B-ST-1 | DE-02 | Stock inicial | Abierta |
