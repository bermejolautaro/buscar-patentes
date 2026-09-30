# Research: Caminos sin diagonales

## D1 — De dónde salen las diagonales

**Método**: se le mandó al servicio de ajuste (Valhalla, `trace_attributes`, costeo peatonal y
`map_snap`, igual que la app) una caminata inventada cerca del Obelisco: 13 puntos por una calle,
un salto de unos 680 m sin puntos en el medio, y 11 puntos más. Nada del jugador salió de la PC.
Se miró la respuesta entera, no solo `shape`.

**Hallazgos**, tres causas distintas:

1. **Los pedazos del camino se pegan con una recta.** Cuando el servicio no puede unir dos
   puntos seguidos por la red de calles, corta el camino en dos pedazos. `shape` los devuelve
   **uno atrás del otro en la misma polilínea**, así que el final de uno y el principio del
   siguiente quedan unidos por una recta. En la prueba, entre los índices 30 y 31 de `shape` hay
   116 m en línea recta. Se ve en dos lugares de la respuesta:
   - en `edges`, el `begin_shape_index` de una arista no coincide con el `end_shape_index` de la
     anterior (`(28, 30)` seguida de `(31, 32)`);
   - en `matched_points`, el último punto antes del corte trae `begin_route_discontinuity` y el
     primero después, `end_route_discontinuity`.
2. **Un corte de señal se rellena con un camino inventado.** El salto de 680 m no produjo ningún
   corte: el servicio lo cruzó por 17 aristas de calles que nadie caminó. El trazo crudo, en
   cambio, corta a partir de 250 m (`Geo.SALTO_MAXIMO_M`) y dibuja el hueco punteado (FR-009 de la
   003). El ajustado no se corta nunca (`Colecciones.tramosDe`), así que esa invención se dibuja
   continua.
3. **Lo que no se pudo emparejar desaparece.** Los últimos 5 puntos de la prueba volvieron
   `unmatched`, y `shape` termina antes que la caminata. En el mapa, el final de la salida no
   existe.

Hay una cuarta, heredada: los caminos guardados antes de pasar a `trace_attributes` tienen varias
piernas separadas por `;`, y `Polilinea.decodificar` concatena sus **puntos**, así que el final de
una pierna y el principio de la siguiente también quedan unidos por una recta.

**Decisión**: las cuatro se resuelven con el mismo cambio: el camino ajustado deja de ser una sola
línea y pasa a ser una lista de **tramos**, armada con toda la respuesta (D2) y guardada con un
formato que distingue lo nuevo de lo viejo (D3).

**Descartado**:
- **Filtrar las rectas largas del `shape`**: una avenida recta produce segmentos de más de 100 m
  que son calle de verdad (en la prueba, el más largo mide 137 m y es una arista). No hay umbral de
  distancia que separe una cosa de la otra; la respuesta, en cambio, dice exactamente dónde cortó.
- **Cambiar a `trace_route`**: arma piernas con maniobras y, en una caminata que se pisa a sí
  misma, se queda en el primer tramo (ya se descartó en la 003, ver `AjustarACalles.SERVICIO`).

## D2 — Cómo se arma el camino ajustado

**Decisión**, por salida:

1. Los puntos medidos se cortan en **tramos de señal** con `Geo.tramos`, el mismo umbral que usa el
   trazo crudo. Así un corte es un corte en las dos vistas (FR-003).
2. **Un pedido al servicio por tramo de señal** con dos puntos o más. Un tramo de un solo punto no
   tiene camino y queda afuera, como hoy en el crudo.
3. Con cada respuesta se arma **un tramo continuo**:
   - `shape` se parte en **pedazos** donde las aristas no son contiguas;
   - cada punto medido se asigna a un pedazo por el `edge_index` de su `matched_point`, o queda
     suelto si vino `unmatched`;
   - el tramo es: pedazo, puntos medidos del medio, pedazo, y así. Entre dos pedazos van los
     puntos medidos **desde el último del pedazo anterior hasta el primero del siguiente**,
     inclusive: el empalme sigue lo que midió el teléfono y nunca es una recta entre dos pedazos;
   - los puntos sueltos del principio y del final van con sus posiciones medidas (hallazgo 3).
4. Si ningún punto de un tramo quedó emparejado, el tramo son sus puntos medidos. Si eso pasa en
   **todos** los tramos, la salida quedó "no se pudo ajustar" (D3).
5. Si **un** pedido falla (sin red, error del servicio, respuesta rara), la salida no se guarda y
   queda pendiente para la próxima vez, como hoy (FR-032 y FR-035 de la 003).

El armado es una función pura en `domain/`, con prueba: recibe los puntos medidos y las tres cosas
que importan de la respuesta, ya leídas del JSON.

**Por qué los puntos medidos en el empalme y no el punto emparejado**: el punto emparejado de un
`begin_route_discontinuity` cae sobre la calle, pero el siguiente pedazo puede arrancar a una
cuadra. Lo único que se sabe de ese tramo es lo que midió el teléfono, y el jugador ya confirmó
que el crudo sale "perfecto" (aclaración de la spec: el empalme no se distingue).

## D3 — Cómo se guarda, y cómo se sabe qué es viejo

**Decisión**: sigue siendo el `String` de `recorrido.caminoAjustado`, sin columnas nuevas:

| Valor | Qué es | Qué se dibuja |
|---|---|---|
| `null` | Pendiente: nunca se ajustó | Los puntos medidos |
| Sin prefijo (lo de hoy) | Ajustado con el criterio viejo: a reajustar | Los puntos medidos |
| `2:` solo | Se intentó y no se pudo: ningún punto emparejó | Los puntos medidos |
| `2:` + tramos separados por `;` | Ajustado con el criterio nuevo | Los tramos, con huecos punteados entre ellos |

Cada tramo va en el formato de polilínea de Valhalla, a 1e6, con `Polilinea.codificar`, que hoy no
existe y se suma.

**Por qué `2:`**: el formato de polilínea usa solo caracteres del 63 (`?`) en adelante, y `2` y `:`
son el 50 y el 58. Ningún camino viejo puede empezar así. Lo mismo el `;`, que ya separaba piernas.

**La cola**: `sinAjustar()` pasa a traer las terminadas cuyo camino es `null` o no empieza con
`2:`. Eso es FR-004 sin nada que administrar: lo viejo entra en la misma cola que lo pendiente, y
un `2:` solo no vuelve a intentarse. Una salida en curso sigue afuera.

**Descartado**:
- **Una columna nueva** (`ajusteVersion`, o un estado): cambia el esquema, así que obliga a una
  migración 7→8, que el iPhone todavía no tiene (su base nace en la 7), y a subir `VERSION_BASE`
  del respaldo, con lo que un respaldo nuevo dejaría de entrar en una app vieja. Todo eso para un
  dato que entra en dos caracteres.
- **Borrar los caminos viejos al actualizar**: una salida pendiente y una vieja se dibujan igual
  (FR-004), pero borrar haría perder el camino viejo si el reajuste nunca llega (sin red en
  semanas). Se deja como está hasta que lo reemplace uno nuevo.

**Respaldo**: el contrato R de la 006 no cambia. `caminoAjustado` viaja como texto, y un camino
viejo que llega en un respaldo se reajusta como cualquier otro.

## D4 — Cómo se dibuja

**Decisión**: `Trazo` pasa de `puntos` + `ajustado` a **`tramos: List<List<Pair<Double, Double>>>`**.
Quien arma el trazo decide los tramos:
- vista real, o sin camino ajustado nuevo: `Geo.tramos(puntos medidos)`, como hoy el crudo;
- vista ajustada: los tramos guardados.

`coleccionTrazos` y `coleccionHuecos` dejan de distinguir: dibujan los tramos, y un hueco punteado
entre cada tramo y el siguiente. Desaparece la rama "el ajustado no se corta", que es la causa 2 de
D1. La lectura de `caminoAjustado` vive en un solo lugar (`CaminoGuardado.leer`), porque la usan el
mapa principal y el detalle.

## D5 — El interruptor del detalle

**Decisión**: en `DetalleDeSalida`, una fila sobre el mapa con un `Switch` y un texto que dice qué
se está viendo: "Ajustado a las calles" prendido, "Lo que midió el teléfono" apagado. Es un
interruptor, como lo pidió el jugador, con el `Switch` de Material 3. Arranca
prendido; es un `remember` de la pantalla, así que no se recuerda entre visitas (aclaración de la
spec).

**Descartado**: los botones segmentados de Material 3. Dicen las dos opciones a la vez, pero ocupan
el doble y un interruptor alcanza para dos vistas.

- Sin camino nuevo, la fila no deja elegir y en su lugar dice "Todavía sin ajustar" (`null`, viejo
  o salida en curso) o "No se pudo ajustar" (`2:` solo).
- **La cámara no se mueve** (FR-008) sin hacer nada: `MapaDeFondo` encuadra una sola vez, la
  primera en que tiene algo que mostrar (`encuadrado`), y cambiar los trazos no vuelve a encuadrar.
  Se verifica en el teléfono.
- El mapa principal no tiene interruptor: dibuja el ajustado nuevo cuando existe y los puntos
  medidos cuando no (FR-009).

## D6 — Cuántos pedidos y cuándo

Sin cambios de mecanismo: `AjustarACalles.pendientes()` corre al entrar a la pantalla principal con
conexión (FR-032 de la 003). Lo nuevo es que la primera vez después de actualizar la cola trae
**todas** las salidas viejas, y cada una hace un pedido por tramo de señal. Con las salidas que
tiene el jugador son unas decenas de pedidos, una sola vez.

ponytail: pedidos en serie, sin límite de ritmo. Si el servicio empieza a contestar 429, se espacia
el bucle; hoy no hace falta.
