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

**Decision**:

1. **Coordenadas globales.** Se pasa todo a una grilla común de zoom 14: `x × 4096 + px`. Las teselas
   vecinas cuantizan a la misma grilla, así que un punto que aparece en las dos queda igual.
2. **Recorte.** Cada calle se recorta al cuadrado propio de su tesela: lo que cae en el margen lo
   trae la de al lado.
3. **Nodos.** Los puntos idénticos son el mismo nodo. Los puntos que crea el recorte, sobre el borde,
   se juntan con los de la otra tesela a menos de 2 unidades: los dos lados recortan tramos apenas
   distintos.
4. **Esquinas en T.** Una punta suelta que cae a menos de 2 unidades de un tramo de otra calle del
   mismo nivel (`layer`) parte ese tramo ahí, y es una esquina.

Las cadenas de `Cuadras.armar` siguen por los nodos de grado 2, así que una calle partida en el borde
de la tesela vuelve a ser una sola cuadra.

## D5. Los nombres

`transportation_name` trae las calles con nombre, unidas en líneas largas, sobre la misma grilla.
Cada pedazo de calle toma el nombre de la línea que pasa a menos de 2 unidades de la mayoría de sus
puntos. Sin ninguna, queda sin nombre, como una calle sin nombre en Overpass.

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
