# Contrato A — El camino ajustado

## A1 — Formato guardado

`recorrido.caminoAjustado` es texto. Para esta feature puede valer:

| Valor | Significa |
|---|---|
| `null` | Nunca se ajustó |
| Cualquier texto que **no** empiece con `3:` | Ajustado con un criterio anterior: antes de la 007, o el `2:` de la primera versión de la 007, que llevaba puntos medidos. Se trata como pendiente |
| `3:` | Se ajustó y no salió ningún pedazo de calle |
| `3:` + `T1;T2;…;Tn` | Ajustado. Cada `Ti` es un pedazo de calle, en orden |

- Cada tramo es una **polilínea codificada** con el algoritmo de Google, a **1e6** (la precisión de
  Valhalla), con 2 posiciones o más.
- `;` separa pedazos. Entre dos pedazos no se dibuja nada.
- `3` y `:` son los caracteres 51 y 58, y el `;` es el 59. Una polilínea solo usa caracteres del 63
  en adelante, así que ninguno de los tres puede aparecer dentro de un tramo, ni un camino viejo
  puede empezar con `3:`.
- Una cadena corrupta se lee hasta donde se pueda, como hoy: un camino a medias es mejor que una
  pantalla que se cae.

## A2 — El pedido al servicio

Uno por **tramo de señal** con 2 puntos o más. El cuerpo es el de hoy (`AjustarACalles.cuerpo`):

- `shape`: los puntos del tramo, **sin `radius`**: el servicio busca la calle con su radio por
  defecto (D7);
- `costing`: `pedestrian`;
- `shape_match`: `map_snap`.

POST a `trace_attributes`, 20 s de espera. Sin red, no se guarda nada de esa salida y se reintenta.
Si el servicio rechaza un tramo (no contesta 200, o contesta algo que no se entiende), ese tramo no
pinta nada; si rechaza todos, se reintenta.

## A3 — Lo que se lee de la respuesta

| Campo | Para qué |
|---|---|
| `shape` | Las posiciones emparejadas, en una polilínea a 1e6 |
| `edges[].begin_shape_index`, `edges[].end_shape_index` | Dónde se corta `shape`: si una arista no empieza donde terminó la anterior, ahí hay un pedazo nuevo |

El resto de la respuesta se ignora. `matched_points` dice qué puntos emparejaron, pero desde que se
pintan solo calles no hace falta: lo que no emparejó no está en `shape`.

## A4 — Los pedazos de calle

`shape` se parte en pedazos en cada discontinuidad de las aristas: donde el `begin_shape_index` de
una arista no es el `end_shape_index` de la anterior. Cada pedazo de 2 posiciones o más es un
tramo guardado. Nada más: ni los puntos medidos entre pedazos, ni los que no emparejaron.

**Garantía**: cada tramo guardado es una sucesión de aristas contiguas del mapa de calles. Nunca
une dos pedazos con una recta (FR-001, FR-002).

## A5 — Qué se dibuja

| Camino | Tramos | Huecos punteados |
|---|---|---|
| Vista por defecto, ajustado (`3:` con pedazos) | Los pedazos | Ninguno |
| Vista por defecto, `3:` solo | Ninguno | Ninguno |
| Vista de los puntos reales, o todavía sin ajustar | `Geo.tramos(puntos medidos)` | Entre un tramo y el siguiente |

La pantalla principal y el detalle arrancan en la vista por defecto, y cada interruptor pasa a la
otra (FR-006 a FR-009). Lo decide `trazoDe`, en `mapa/Colecciones.kt`.
