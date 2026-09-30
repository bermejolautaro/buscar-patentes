# Contrato A — El camino ajustado

## A1 — Formato guardado

`recorrido.caminoAjustado` es texto. Para esta feature puede valer:

| Valor | Significa |
|---|---|
| `null` | Nunca se ajustó |
| Cualquier texto que **no** empiece con `2:` | Ajustado con el criterio anterior a la 007. Se trata como pendiente |
| `2:` | Se ajustó y ningún punto emparejó con una calle |
| `2:` + `T1;T2;…;Tn` | Ajustado. Cada `Ti` es un tramo, en orden |

- Cada tramo es una **polilínea codificada** con el algoritmo de Google, a **1e6** (la precisión de
  Valhalla), con 2 posiciones o más.
- `;` separa tramos. Entre dos tramos hubo un corte de señal y se dibuja un hueco punteado.
- `2` y `:` son los caracteres 50 y 58, y el `;` es el 59. Una polilínea solo usa caracteres del 63
  en adelante, así que ninguno de los tres puede aparecer dentro de un tramo, ni un camino viejo
  puede empezar con `2:`.
- Una cadena corrupta se lee hasta donde se pueda, como hoy: un camino a medias es mejor que una
  pantalla que se cae.

## A2 — El pedido al servicio

Uno por **tramo de señal** con 2 puntos o más. El cuerpo es el de hoy (`AjustarACalles.cuerpo`):

- `shape`: los puntos del tramo, cada uno con su `radius` entre 5 y 30 m según su precisión;
- `costing`: `pedestrian`;
- `shape_match`: `map_snap`.

POST a `trace_attributes`, 20 s de espera. Si un pedido de la salida no vuelve con un 200 y un JSON
que se pueda leer, no se guarda nada de esa salida.

## A3 — Lo que se lee de la respuesta

| Campo | Para qué |
|---|---|
| `shape` | Las posiciones emparejadas, en una polilínea a 1e6 |
| `edges[].begin_shape_index`, `edges[].end_shape_index` | Dónde se corta `shape`: si una arista no empieza donde terminó la anterior, ahí hay un pedazo nuevo |
| `matched_points[].type` | `matched`, `interpolated` o `unmatched`. Hay uno por cada punto pedido, en el mismo orden |
| `matched_points[].edge_index` | A qué arista, y por lo tanto a qué pedazo, pertenece un punto emparejado |

`begin_route_discontinuity` y `end_route_discontinuity` confirman los cortes, pero no hacen falta:
el corte ya se ve en los índices de las aristas. El resto de la respuesta se ignora.

## A4 — Cómo se arma un tramo

Con los puntos medidos `M[0..n]` de un tramo de señal y la respuesta:

1. `shape` se parte en pedazos `P[0..k]` en cada discontinuidad de las aristas.
2. Cada `M[i]` emparejado pertenece al pedazo de su arista. `interpolated` cuenta como emparejado.
3. El tramo se arma en orden:
   - los `M` sueltos antes del primer emparejado, con su posición medida;
   - `P[0]`;
   - entre `P[j]` y `P[j+1]`: los `M` desde el último que pertenece a `P[j]` hasta el primero que
     pertenece a `P[j+1]`, **inclusive**, con su posición medida;
   - `P[k]`;
   - los `M` sueltos después del último emparejado.
4. Sin ningún `M` emparejado, el tramo es `M` entero.

**Garantía**: en el tramo armado no hay dos posiciones seguidas que no estén unidas por una calle
de `shape` o por dos puntos medidos consecutivos (FR-001, FR-002).

## A5 — Qué se dibuja

| Camino | Tramos | Huecos punteados |
|---|---|---|
| Ajustado (`2:` con tramos) | Los guardados | Entre un tramo y el siguiente |
| Cualquier otro caso | `Geo.tramos(puntos medidos)` | Entre un tramo y el siguiente |

El mapa principal usa el ajustado cuando existe. El detalle de una salida arranca en el ajustado, y
el interruptor pasa al otro (FR-006 a FR-009).
