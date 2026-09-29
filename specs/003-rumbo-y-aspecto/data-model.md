# Phase 1 Data Model: Rumbo y aspecto

**Esta feature no toca la evidencia de ningún registro.** Ese es el hecho más importante de
este documento, y es lo que hace pasar al Principio II: ninguna sentencia nueva nombra
latitud, longitud, precisión ni timestamp de un `registro_de_captura` en su `SET`.

**Lo que sí escribe, y llegó después de la primera redacción.** Este documento decía "no
agrega entidades, no agrega campos, no agrega migración y no agrega ni una sentencia de
escritura", y eso valía para la feature tal como estaba planeada. Las vueltas a la calle
agregaron tres cosas, cada una contra un requisito y ninguna sobre la evidencia:

| Qué | Dónde | Por qué no rompe el Principio II |
|---|---|---|
| Columna `recorrido.caminoAjustado` y migración v3→v4 | `guardarCaminoAjustado` (FR-031, FR-034) | Es una **interpretación** de los puntos medidos, vive aparte y se puede recalcular. Los puntos de trayecto no se tocan |
| `borrarConSuTrayecto` | `RecorridoDao` (FR-024, FR-025) | Borra el paseo, no lo que se vio: **ninguna patente se toca**. El Principio II protege la evidencia aunque se borre el recorrido que la rodeaba |
| Tabla `voto` y migración v4→v5 | `VotoDao` (FR-037) | Es lo que el jugador **opina** después, no lo que el teléfono midió. Se guarda aparte y nunca reescribe un registro |

La lectura sigue siendo el grueso de la feature: lo que sigue es lo que se lee, lo que se
deriva al vuelo y lo que se borró con la grilla.

---

## Lo que se lee

Nada de esto cambia de forma. Se listan los campos que esta feature efectivamente consume,
para dejar claro que no hace falta ninguno más.

### Registro de captura

| Campo | Para qué lo usa esta feature |
|---|---|
| `id` | Abrir la ficha desde el mapa (ya existía) |
| `numero` | La sugerencia del número actual en Buscar, y la etiqueta del destino |
| `latitud`, `longitud` | El destino del trayecto, y el cálculo de distancia y rumbo |
| `precisionMetros`, `precisionDegradada` | La advertencia del FR-004 antes de mandar al jugador |
| `capturadoEn` | El orden de los recientes en Buscar |
| `estado` | El color del marcador, como ya estaba |

### Recorrido

| Campo | Para qué lo usa esta feature |
|---|---|
| `id` | Agrupar los puntos en un trazo por salida |
| `iniciadoEn` | Ordenar las salidas, y elegir cuál es la más reciente |
| `estado` | Saber cuál está `EN_CURSO` para destacarla (FR-007a) |

### Punto de trayecto

| Campo | Para qué lo usa esta feature |
|---|---|
| `recorridoId` | Agrupar: un trazo por salida, nunca uno solo con todo |
| `latitud`, `longitud` | Los vértices del trazo |
| `registradoEn` | El orden dentro del trazo |

`precisionMetros` del punto **sí se usa**, y llegó con la cuarta salida: el servicio descarta
al grabar los puntos peores que un cuarto de manzana (FR-036), y el ajuste a calles le pasa a
Valhalla esa precisión como `radius`, para que no le crea lo mismo a un punto de 5 m que a uno
de 25 (FR-036a). La redacción original de este documento decía que no se usaba, porque el
FR-009 de entonces unía todo sin filtrar ni interpretar.

### Estado del juego

`numeroActual` alimenta la sugerencia del FR-011 en Buscar, además del indicador de la
pantalla principal donde ya se usaba.

---

## Lo que cambia en la capa de lectura

Lo que la feature planeada cambiaba era una consulta, y solo su orden (lo escrito abajo). Lo
que las vueltas a la calle agregaron después está en la tabla de arriba, y a eso se suman dos
lecturas nuevas sin consulta nueva: `VotoDao.todos()` para pintar el anillo de cada marcador
de una sola vez (FR-037) y `PuntoDeTrayectoDao.deRecorrido` para hacer crecer el trazo de la
salida en curso sin releer el histórico entero (FR-008, FR-010).

La consulta que la feature reordenó:

> Los puntos de trayecto se leen **agrupables por recorrido y ordenados dentro de cada uno**.
> Hoy la consulta que devuelve todos no declara ningún orden, así que el trazo saldría con los
> vértices en el orden que la base quiera darlos — un garabato, no un camino.

No es un campo nuevo ni un índice nuevo: el índice por `recorridoId` ya existe desde la 001.

---

## Lo que se deriva al vuelo, y no se guarda

### Distancia y rumbo hasta una patente (FR-003)

Se calculan cada vez que se mira una patente —en la ficha del mapa y en cada resultado de
búsqueda, que son los dos lugares del FR-002—, a partir de la posición actual del jugador y la
posición guardada del registro. **No se persisten, y es una decisión, no un olvido**: la
distancia depende de dónde está parado el jugador ahora. Guardarla dentro del registro sería
meter en la evidencia un dato de otro día, que es exactamente la carga retroactiva de metadata
que el Principio II prohíbe.

Si no se conoce la posición actual, el resultado es "desconocida" y no un número inventado
(FR-003a).

### El punto cardinal

Se deriva del rumbo, en ocho sectores. Es presentación, no dato.

### El trazo de un recorrido

Se arma leyendo los puntos de una salida y uniéndolos en orden. Se recalcula al cambiar los
datos, igual que los marcadores. No se guarda ninguna geometría en la base.

### Cuál recorrido va destacado (FR-007a)

Es el que está `EN_CURSO`. Si no hay ninguno, el de `iniciadoEn` más reciente. Se deriva de lo
que ya está guardado; no hay ningún campo "destacado" que mantener sincronizado.

---

## Lo que se borra

La grilla de cobertura y todo lo que existía únicamente para sostenerla (D4, FR-021):

| Qué | Dónde |
|---|---|
| `Celda`, `Limites` | `domain/Cobertura.kt` |
| `celdaDe`, `celdasExploradas`, `limitesDe`, `PASO_GRADOS` | `domain/Cobertura.kt` |
| La fuente y la capa de cobertura | `mapa/Mapa.kt` |
| El color `COBERTURA` | `ui/Tema.kt` |
| El campo `celdas` de `ResumenDeSalida` | `ui/PantallaRecorridos.kt` |
| Cinco pruebas de la grilla | `test/…/CoberturaTest.kt` |

**Sobrevive** `Cobertura.tieneElActual` y `Cobertura.consecutivosCubiertos`, con sus ocho
pruebas: alimentan el indicador del juego y no tienen nada que ver con la grilla. El objeto
`Cobertura` queda entonces siendo solo lo que su nombre dice —qué números tiene cubiertos el
jugador— y deja de ser dos cosas distintas bajo un mismo nombre.

**Ningún dato guardado se pierde.** Los puntos de trayecto, que eran el insumo de la grilla,
siguen enteros en la base: pasan a dibujarse como lo que son.

---

## Entidad nueva de dominio, sin persistencia

`Geo` (D3) es un objeto de funciones puras, no una entidad: distancia entre dos posiciones,
rumbo inicial, y rumbo a punto cardinal. Vive en `domain/` por la misma razón que `Patente` y
`Cobertura` —matemática sin Android, testeable en JVM— y no toca la base.
