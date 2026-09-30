# Contrato Z — Las cuadras de una zona

## Z1 — El pedido a Overpass

Un POST a `https://overpass-api.de/api/interpreter` por zona, con `postear`. La URL vive en un
solo lugar, como las del mapa de fondo y del ajuste a calles. El cuerpo es la consulta, en texto:

```text
[out:json][timeout:25];
way["highway"~"^(trunk|primary|secondary|tertiary|unclassified|residential|living_street|pedestrian|road)$"]
   ["area"!="yes"]["access"!~"^(private|no)$"]["foot"!="no"]
   (S,O,N,E);
out geom;
```

- `S,O,N,E` es el rectángulo que envuelve el borde, agrandado 200 m para cada lado (D1). Va con
  punto decimal y 6 decimales, sin depender del idioma del teléfono.
- **Entran**: calles, avenidas, pasajes y peatonales.
- **No entran**:
  - autopistas y sus accesos (`motorway`, `*_link`);
  - senderos (`footway`, `path`), escaleras y ciclovías;
  - calles de servicio (`service`): cocheras, playas de estacionamiento, pasillos;
  - lo privado o prohibido a pie (`access`, `foot`), como las calles internas de un barrio cerrado
    y los carriles centrales de una avenida sin vereda;
  - las plazas secas dibujadas como área.
- Cabeceras: `Content-Type: application/json`, que Overpass acepta igual, y `User-Agent:
  buscar-patentes`. Sin una identificación propia, Overpass contesta 406 (D1).
- Espera: la de `postear`, 20 s. El máximo medido es 5,9 s.

**Lo que se lee de la respuesta**:
- `elements[]` con `type == "way"`;
- de cada vía, `nodes[]` (los ids) y `geometry[]` (`lat`, `lon`, uno por nodo, en el mismo orden);
- de sus etiquetas, `tags.name` y `tags.oneway`.

Lo demás se ignora. Una respuesta que no se entiende es lo mismo que no tener respuesta: se
reintenta.

## Z2 — De las vías a las cuadras

1. **Grafo**: cada par de nodos seguidos de una vía es un segmento. El grado de un nodo es cuántos
   segmentos lo tocan, sumando todas las vías.
2. **Cadenas**: una cuadra empieza en un nodo de grado distinto de 2 y sigue de segmento en segmento
   mientras el nodo del medio tenga grado 2. Un circuito cerrado sin ninguna esquina es una sola
   cuadra. El nombre de la cuadra es el de su primera vía; lo mismo vale para la mano única.
3. **Cortas**: se descartan las cuadras de menos de 30 m (D2).
4. **De la zona**: se muestrea cada cuadra cada 5 m y se queda si la mitad de sus muestras, o más,
   caen adentro del borde (D4).
5. **Gemelas**: se aparean las cuadras con la regla del D3. En cada pareja, la de id menor es la
   principal y la otra guarda `gemelaDe`.
6. **Cantidad**: se cuentan las principales. Con 0 o con más de 2.000, la zona no se arma (D8).

**Garantías** (las verifica `CuadrasTest`, con una respuesta real recortada de una zona pública):
- una calle recta que cruzan cinco calles da seis cuadras, cortadas en los cinco cruces;
- una calle que el mapa parte en dos vías sin ninguna esquina en el medio da una sola cuadra;
- un sendero que toca la calle no la corta, porque no vino en el pedido;
- dos cuadras seguidas de la misma calle de una mano nunca son gemelas.

## Z3 — Qué cuadras están recorridas

Entrada:
- las cuadras de la zona;
- los tramos ajustados de las salidas que cuentan, cada uno con el `iniciadoEn` de su salida
  (data-model, "Salida").

1. Cada cuadra se muestrea cada 5 m.
2. Una muestra queda cubierta por la **primera** salida, en orden de `iniciadoEn`, que pasa a 10 m o
   menos de ella (D5).
3. Una cuadra está recorrida si el 80% de sus muestras, o más, están cubiertas. Su fecha de
   recorrida es el percentil 80 de las fechas de sus muestras (D6).
4. Una pareja de gemelas está recorrida si lo está cualquiera de las dos, y su fecha es la más
   temprana de las dos.
5. Las quitadas no cuentan ni arriba ni abajo del porcentaje.

Salida: la `Cobertura de zona` de [data-model.md](../data-model.md).

**Garantías** (las verifica `CoberturaDeZonaTest`, con la respuesta real de la 007 y las cuadras
de Z2):
- las cuadras caminadas de esquina a esquina están recorridas;
- las que la caminata solo cruzó en una esquina, no;
- media cuadra un día y la otra media otro día dan una cuadra recorrida;
- una salida anterior a "desde cuándo cuenta" no suma;
- 99 de 100 dan 99%, nunca 100%;
- la fecha de completada es la de la última cuadra que se recorrió.

## Z4 — Qué se dibuja

| Dónde | Borde | Cuadras |
|---|---|---|
| Pantalla principal, cobertura o antigüedad | Punteado, de cada zona activa | Solo las pendientes de las zonas activas, resaltadas, debajo de los trazos |
| Pantalla principal, recorridos apagados | No | No |
| Mapa de una zona activa | Continuo | Recorridas, pendientes y quitadas, cada clase con su color. Se tocan (FR-012) |
| Mapa de una zona terminada | Continuo | Recorridas y pendientes según `recorridaAlTerminar`, y las quitadas. No se tocan |
| Mientras se dibuja una zona | Las esquinas marcadas y las rectas entre ellas, cerrando contra la primera | No |
