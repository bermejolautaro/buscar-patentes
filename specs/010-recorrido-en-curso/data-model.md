# Data Model: Recorrido en curso

## Lo que cambia en la base

### `estado_del_juego` (existe)

| Campo | Tipo | Nuevo | Notas |
|---|---|---|---|
| `pesoKg` | `Int?` | Sí | El peso del jugador, de 1 a 300. Null mientras no lo carga: sin peso no hay calorías (FR-009) |

- La base pasa de la **8** a la **9** con `SQL_8_9`, en `data/Migraciones.kt`:
  `ALTER TABLE estado_del_juego ADD COLUMN pesoKg INTEGER`.
- `Respaldo.VERSION_BASE = 9`. El formato del respaldo no cambia: un respaldo de la 8 se restaura y
  Room lo migra.

Nada más se guarda. Los puntos y las patentes no se tocan (FR-021, Principio II).

## Lo que se calcula sin guardar

### `ResumenDeCaminata`

| Campo | Tipo | De dónde sale |
|---|---|---|
| `duracionMs` | `Long` | `finalizadoEn - iniciadoEn`, o `ahora - iniciadoEn` si está en curso |
| `metros` | `Double` | La suma de cada tramo de `Geo.tramos` sobre sus puntos (D1) |
| `kcal` | `Int?` | `0,5 × pesoKg × km`, redondeado a 5. Null sin peso (D2) |

### `Periodo` y `TotalesDelPeriodo`

| Campo | Tipo | Notas |
|---|---|---|
| `tipo` | `DIA`, `SEMANA` o `MES` | La pestaña |
| `inicio` | `LocalDate` | El día, el lunes de la semana o el día 1 del mes |
| `salidas` | `Int` | Las que **empezaron** en el período (FR-019) |
| `duracionMs` | `Long` | La suma de las duraciones |
| `metros` | `Double` | La suma de los metros |
| `kcal` | `Int?` | La suma de las kcal; null sin peso |
| `patentes` | `Int` | Las anotadas en el período, estén o no en una salida |

Reglas:
- Los límites de cada período son medianoches en la zona horaria del teléfono.
- El período siguiente al actual no existe: la flecha hacia adelante se apaga en el actual (FR-017).
- Las barras son los km de los últimos 7 días, 8 semanas o 12 meses, terminando en el período
  elegido (FR-018).
