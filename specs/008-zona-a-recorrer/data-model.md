# Data Model: Zona a recorrer

La base pasa de la versión 7 a la **8**: se agregan dos tablas y no se toca ninguna de las que ya
existen. La migración 7→8 es común a los dos teléfonos (D7 de [research.md](./research.md)).

## Zona (`zona`, nueva)

| Campo | Tipo | Qué guarda |
|---|---|---|
| `id` | `INTEGER`, clave autogenerada | |
| `nombre` | `TEXT`, no nulo | Lo que escribió el jugador. Si no escribió nada, "Zona del 30/09" con la fecha de creación (FR-002) |
| `borde` | `TEXT`, no nulo | Las esquinas en orden, como polilínea a 1e6 (`Polilinea.codificar`). El borde se cierra solo: la última esquina no repite a la primera |
| `creadaEn` | `INTEGER`, no nulo | Instante de creación |
| `cuentaDesde` | `INTEGER`, no nulo | Comienzo del día elegido, en la hora del teléfono (D10) |
| `estado` | `TEXT`, no nulo | `BUSCANDO`, `ACTIVA`, `COMPLETADA` o `CERRADA` |
| `problema` | `TEXT`, nulo | Solo en `BUSCANDO`, cuando la búsqueda terminó y no sirve: "Adentro no hay calles" o "Tiene más de 2.000 cuadras" (D8) |
| `terminadaEn` | `INTEGER`, nulo | `COMPLETADA`: la fecha de la última cuadra recorrida (D6). `CERRADA`: el instante del cierre |
| `porcentajeFinal` | `INTEGER`, nulo | El porcentaje congelado al terminar. `COMPLETADA` siempre es 100 |

**Reglas**:
- `cuentaDesde` es menor o igual que hoy, y cambia solo mientras la zona está `ACTIVA` (FR-003).
- `terminadaEn` y `porcentajeFinal` son nulos hasta que la zona termina, y después no cambian
  (FR-016).

### Estados

```text
BUSCANDO ──cuadras encontradas (1 a 2.000)──▶ ACTIVA
BUSCANDO ──sin conexión o sin respuesta──▶ BUSCANDO (se reintenta)
BUSCANDO ──0 o más de 2.000──▶ BUSCANDO con problema (solo se borra)
ACTIVA ──todas las que cuentan recorridas──▶ COMPLETADA
ACTIVA ──el jugador la cierra──▶ CERRADA
cualquiera ──el jugador la borra──▶ (desaparece, con sus cuadras)
```

## Cuadra (`cuadra`, nueva)

| Campo | Tipo | Qué guarda |
|---|---|---|
| `id` | `INTEGER`, clave autogenerada | |
| `zonaId` | `INTEGER`, no nulo, con índice | La zona a la que pertenece |
| `nombre` | `TEXT`, nulo | El nombre de la calle. Nulo si el mapa no lo tiene |
| `forma` | `TEXT`, no nulo | La cuadra de esquina a esquina, como polilínea a 1e6 |
| `gemelaDe` | `INTEGER`, nulo | El id de su gemela principal, si es la otra mano de una avenida (D3) |
| `quitada` | `INTEGER` (bool), no nulo, 0 | La quitó el jugador (FR-012, FR-013) |
| `recorridaAlTerminar` | `INTEGER` (bool), no nulo, 0 | Solo tiene sentido con la zona terminada: si estaba recorrida en ese momento |

**Reglas**:
- **Las cuadras se escriben una vez**, cuando la búsqueda termina (FR-005). Después cambian dos
  campos, y nada más:
  - `quitada`, mientras la zona está activa;
  - `recorridaAlTerminar`, una sola vez, al terminar.
- **Una pareja de gemelas** es una sola cuadra para contar. Cuenta la principal, la que tiene
  `gemelaDe` nulo, y la pareja está recorrida si lo está cualquiera de las dos. Quitar o sumar una
  hace lo mismo con la otra.
- **Borrar una zona** borra sus cuadras en la misma transacción. No hay clave foránea, igual que
  entre `recorrido` y `punto_de_trayecto`.

## Salida (`recorrido`, sin cambios)

La zona lee de cada salida:
- `iniciadoEn`;
- `estado`;
- `caminoAjustado`, con `CaminoGuardado.leer` de la 007.

No escribe nada (FR-019). Una salida cuenta para una zona activa si cumple las tres cosas:
- está `TERMINADO`;
- su camino es `CaminoGuardado.Ajustado`;
- su `iniciadoEn` es igual o posterior a `cuentaDesde`.

## En memoria: la cuenta de una zona

`Cobertura de zona` es lo que devuelve la cuenta del D5 y del D6. No se guarda:

| Campo | Qué es |
|---|---|
| `recorridas` | Los ids de las cuadras principales recorridas |
| `total` | Las cuadras principales que no están quitadas |
| `porcentaje` | `recorridas que cuentan * 100 / total`, redondeado hacia abajo (FR-007) |
| `faltan` | `total - recorridas que cuentan` |
| `completadaEn` | Con `faltan == 0`, la fecha de la última cuadra recorrida (D6). Si no, nulo |

## Respaldo

Sin cambio de formato: el archivo es la base entera (contrato R de la 006), así que `zona` y
`cuadra` viajan solas. `Respaldo.VERSION_BASE` pasa a 8. Un respaldo de la 7 se migra al abrirlo y
queda sin zonas. Un respaldo de la 8 en un teléfono con la app vieja se rechaza como más nuevo, que
es lo que ya hace hoy con cualquier versión mayor.
