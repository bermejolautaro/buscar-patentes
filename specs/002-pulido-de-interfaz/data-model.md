# Phase 1 Data Model: Pulido de interfaz

**Fecha**: 2026-08-29
**Spec**: [spec.md](./spec.md) · **Research**: [research.md](./research.md)

Esta especificación **no crea entidades ni migra la base**. Trabaja sobre las cuatro que
definió la [001](../001-captura-patentes/data-model.md): `RegistroDeCaptura`,
`EstadoDelJuego`, `Recorrido` y `PuntoDeTrayecto`.

Lo que sí cambia es qué se puede escribir de un registro existente, y qué necesita saber el
mapa para dibujarlo. Esas dos cosas son este documento.

---

## La operación nueva: corregir el número

| Operación | Toca | Requisito |
|---|---|---|
| `corregirNumero(id, numero, patenteTexto, formato)` | Solo `numero`, `patenteTexto` y `formato` | FR-014 |

### Por qué esto no rompe el Principio II

El DAO de registros expone hoy tres escrituras, cada una acotada a lo suyo:

| Operación existente | Toca |
|---|---|
| `insertar` | Todos los campos, una vez |
| `adjuntarFoto(id, ruta)` | Solo `fotoRuta` |
| `marcarCompartida(id)` | Solo `estado` |

La cuarta sigue exactamente la misma forma. La lista de campos que **nunca** se pueden
escribir después de crear el registro no se mueve:

```
latitud · longitud · precisionMetros · precisionDegradada · capturadoEn
```

`numero` nunca estuvo en esa lista, y no es un descuido del data model original: el número
es lo que el jugador **leyó en la calle**; la ubicación, la precisión y el momento son lo
que el **teléfono midió**. El Principio II protege la medición, que es lo que distingue una
patente vista de una inventada. Un número mal tipeado no es evidencia falsa, es un typo.

### Qué se descartó, y por qué importa

Corregir "borrando y volviendo a crear con la misma ubicación" parece más respetuoso del
principio y es lo contrario: crear un registro con una ubicación que no se acaba de medir
es la **carga retroactiva de metadata** que el Principio II prohíbe en su segunda oración.

### Efectos de corregir el número

Cambiar el número cambia si ese registro ya toca por el contador del juego. Por lo tanto
dispara los mismos recálculos que guardar o compartir (FR-015):

- la cobertura de la pantalla principal —si tengo el actual, cuántos consecutivos—
- la reconciliación del conjunto de geofences

`recorridoId` no se toca: el registro se capturó durante ese recorrido pase lo que pase con
el número.

### Reglas de validación

| Regla | Origen |
|---|---|
| El número corregido está entre 0 y 999 | Assumptions de la 001 |
| Un texto de patente que no parsee a 3 dígitos se rechaza, no se guarda a medias | FR-010 de la 001 |
| Corregir un registro ya compartido está permitido; no lo devuelve a pendiente | Transiciones de estado de la 001 |

---

## Lo que el mapa necesita de cada registro

El mapa hoy recibe una vista mínima de cada registro con lo justo para pintarlo: posición y
dos banderas de color. Los marcadores con número que se pueden tocar necesitan dos datos
más.

| Campo | Para qué | Requisito |
|---|---|---|
| `id` | Saber qué registro abrir cuando el jugador toca el marcador | FR-012 |
| `numero` | Dibujarlo adentro del marcador | FR-010 |
| `latitud`, `longitud` | Dónde va | ya existía |
| `compartida` | Color: ya usada | FR-011, hereda FR-035 |
| `toca` | Color: su número ya llegó | FR-011, hereda FR-035 |

Sigue siendo una vista de lectura para dibujar, no la entidad: el mapa no necesita la foto,
ni la precisión, ni el recorrido. Esos aparecen recién en la ficha, que los lee del registro
completo.

### Presentación del número

El número se dibuja con tres dígitos, rellenando con ceros a la izquierda: el `7` se ve como
`007`. Es lo mismo que ya hacen la pantalla de búsqueda y la notificación de proximidad, y
mantiene el ancho del marcador constante.

---

## Agrupación de marcadores

Cuando varios registros caen muy juntos, la fuente del mapa los agrupa y muestra un solo
marcador con la cuenta (FR-016, D5). Es un comportamiento de presentación: **no altera
ningún dato**, no fusiona registros y no cambia sus coordenadas. Al acercar el zoom, el
grupo se abre en sus marcadores individuales.

---

## Estado de interfaz, no persistido

Tres cosas que esta feature introduce y que **no** van a la base, porque no sobreviven ni
deben sobrevivir al cierre de la app:

| Estado | Vive en | Por qué no se persiste |
|---|---|---|
| Qué registro tiene la ficha abierta | La pantalla | Cerrar la app cierra la ficha; reabrirla en la ficha sería raro |
| Si el mapa está siguiendo al jugador o no | El mapa | Al abrir siempre sigue (FR-005). Recordar que el jugador lo apagó ayer no ayuda |
| Modo claro u oscuro | El sistema | FR-002: se acompaña el del teléfono. Guardarlo sería el selector que las Assumptions descartaron |
