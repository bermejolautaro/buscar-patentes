# Research: Cuadras de las teselas

Medido el 2026-11-16 con un decodificador de prueba en Python sobre las teselas de OpenFreeMap del
centro, fuera del repo.

## D1. Las teselas reemplazan a Overpass, y `Cuadras.armar` no cambia

**Decision**: `Teselas.vias(teselas)` convierte las teselas en la misma lista de `Via` que daba
`leerVias` con Overpass: nodos, posiciones, nombre y sentido único. `Cuadras.armar` (contrato Z2 de la
008) las corta, filtra y aparea igual que antes. Overpass se borra: la consulta, la lectura y su
respuesta de prueba.

**Rationale**: todo lo que decide qué es una cuadra ya está probado sobre `Via`. Lo nuevo es solo de
dónde salen.

**Alternatives considered**: copias de Overpass y ohsome, que fallaron el mismo día (ver la spec).
Dejar Overpass como respaldo: sería código que no corre nunca, y la otra fuente ya contesta en 0,1 s.

## D2. Las teselas

- **Cuál**: OpenFreeMap `planet`, el mismo origen que los estilos del mapa. Su TileJSON
  (`https://tiles.openfreemap.org/planet`) dice la plantilla de la versión de la semana, por ejemplo
  `…/planet/20261101_080013_pt/{z}/{x}/{y}.pbf`. Se pide primero el TileJSON y después las teselas.
- **Zoom 14**, el más detallado: una tesela cubre unos 2 km de lado, así que una zona de barrio con
  su margen de 200 m toca entre una y cuatro.
- **Formato**: Mapbox Vector Tile, un protobuf. Se lee con un decodificador propio de unas 100
  líneas, sin dependencia (Principio IV): varints, capas, rasgos, claves, valores y los comandos de
  geometría.

## D3. Qué calles entran

En la capa `transportation`, con el esquema de OpenMapTiles:

| Overpass (008) | Teselas |
|---|---|
| `trunk`, `primary`, `secondary`, `tertiary` | `class` igual |
| `unclassified`, `residential`, `living_street`, `road` | `class = minor` |
| `pedestrian` | `class = path` y `subclass = pedestrian` |
| sin `service` | `class = service` no entra |
| `access` distinto de `private` y `no` | `access` distinto de `no` (OpenMapTiles junta los dos) |
| `foot` distinto de `no` | `foot` distinto de `no` |
| sin áreas | solo las líneas (tipo 2), no los polígonos |

El sentido único es `oneway` 1 o -1. El nombre está en otra capa, `transportation_name`.

## D4. Las esquinas

Las teselas no traen nodos. En una tesela del centro, de las 838 puntas de las 518 calles que entran:

- **811 caen exactamente sobre un punto de otra calle**: la esquina es ese punto.
- **10 caen sobre un tramo de otra calle**, a menos de 2 unidades (1 m), sin compartir el punto. La
  simplificación de la tesela sacó el punto de la otra, que era recto.
- **7 no tocan nada**: calles sin salida.
- **208 están en el borde de la tesela**.

Con solo esas puntas, la franja del centro armaba 30 de las 55 cuadras que daba Overpass. Faltaba
lo que esa medición no miraba: **dos calles que se cruzan de lado a lado sin compartir el punto**.
La tesela simplifica cada calle por su lado, y a una calle recta le saca el punto del cruce aunque
ahí la corte otra. Uruguay pasaba por Sarmiento sin cortarla. Con los cruces, 50 de 55.

**Decision**:

1. **Coordenadas globales.** Se pasa todo a una grilla común de zoom 14: `x × 4096 + px`. Las teselas
   vecinas cuantizan a la misma grilla, así que un punto que aparece en las dos queda igual.
2. **Recorte.** Cada calle se recorta al cuadrado propio de su tesela: lo que cae en el margen lo
   trae la de al lado.
3. **Nodos.** Los puntos idénticos son el mismo nodo. Los puntos que crea el recorte, sobre el borde,
   se juntan con los de la otra tesela a menos de 2 unidades: los dos lados recortan tramos apenas
   distintos.
4. **Cruces.** Dos tramos de calles distintas del mismo nivel (`layer`) que se cruzan, o la punta de
   uno que cae a menos de 2 unidades del otro, se cortan ahí, con un nodo en común. Un puente sobre
   una calle va en otro nivel y no se corta.

Las cadenas de `Cuadras.armar` siguen por los nodos de grado 2, así que una calle partida en el borde
de la tesela vuelve a ser una sola cuadra.

## D5. Los nombres

`transportation_name` trae las calles con nombre, unidas en líneas largas, sobre la misma grilla.
Cada pedazo de calle toma el nombre de la línea que pasa a menos de 2 unidades de la mayoría de sus
muestras. Sin ninguna, queda sin nombre, como una calle sin nombre en Overpass.

**Las muestras son del medio de cada tramo, no sus puntas.** En una esquina la calle que cruza
queda a la misma distancia, y en una cuadrícula casi todos los puntos de una calle son esquinas: con
las puntas, 36 cuadras de la zona del jugador quedaban sin nombre, entre ellas las dos manos de las
avenidas, que sin nombre no se aparean. Darle más distancia al nombre no cambiaba nada.

## D6. Bajar bytes

`postear` manda un POST y devuelve texto. Hace falta un GET que devuelva bytes: `bajar(url)`, con la
misma espera de 20 s y el mismo `User-Agent`, en cada teléfono. El TileJSON se lee con `bajar` y se
decodifica como UTF-8.

## D7. Las pruebas con datos reales

La respuesta de Overpass de la franja del centro (`RespuestaDeOverpass.kt`) se reemplaza por las
teselas de la misma franja, recortadas a las dos capas que se leen y a los rasgos que tocan la
franja con su margen, en base64 (`RespuestaDeTeselas.kt`). Las pruebas que la usaban
(`CuadrasTest`, `CoberturaDeZonaTest`) leen las vías con `Teselas.vias`.

La comparación contra Overpass (SC-002) se hace una vez, fuera del repo, con el borde de la zona
completada del jugador y las cuadras que ya tiene guardadas.

## D8. Resultado contra la zona del jugador

Con el borde de la zona completada del jugador, seis teselas, comparando por las dos puntas con las
429 cuadras que salieron de Overpass en septiembre:

| | Overpass | Teselas |
|---|---|---|
| Cuadras | 429 | 428 |
| Las de Overpass que salen igual | | **425 (99%)** |
| Con el mismo nombre | | 424 de 425 |
| Parejas de manos de avenida | 9 | 7 (las otras 2 están entre las 4 que faltan) |
| Tiempo, en la PC | | 0,3 a 0,4 s |

Las cuatro que faltan son de dos avenidas y una calle donde las teselas cortan en otro lugar. Los
datos son de dos fechas distintas: septiembre en Overpass y noviembre en las teselas.
