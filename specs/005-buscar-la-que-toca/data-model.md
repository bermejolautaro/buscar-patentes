# Data Model: Ir a buscar la que toca

**Esta feature no toca la base.** Ninguna tabla nueva, ninguna columna nueva, ninguna migración.
La única escritura que hace es adjuntar una foto a un registro que no tenía (FR-013a), y esa ya
existía: se muda de pantalla, no cambia.

Lo que sí hay es estado de pantalla y lecturas derivadas. Van acá porque tienen reglas, y las
reglas son lo que se prueba.

## Lo que ya existe y se lee distinto

| Dato | Dónde vive | Qué cambia |
|---|---|---|
| `EstadoDelJuego.numeroActual` | `estado_del_juego` | Decide el fondo verde, la fuente sin agrupar (D1), qué muestra la barra sin filtro y con qué número abre la lupa |
| `RegistroDeCaptura.numero` | `registro_de_captura` | Se cuenta **sin mirar `estado`** para "descubiertas" (FR-003b) |
| `RegistroDeCaptura.estado` | `registro_de_captura` | Sigue decidiendo los avisos (FR-003c) y nada más. Nada de esta feature lo lee |
| `RegistroDeCaptura.probabilidadInicial` + `voto` | 2026-09-28, v7 | Dan la confianza de cada candidato (D8). Sin cambios |

## Estado de pantalla (no se persiste)

### Filtro del mapa

`filtro: Int?` en `PantallaPrincipal`.

| Estado | Qué dibuja el mapa | Qué dice la barra | Campo |
|---|---|---|---|
| `null` | Todas las patentes | Número actual · descubiertas · lupa | Cerrado |
| `n` | Solo las de `n` | `n` · descubiertas de `n` · `X` | Abierto (con o sin teclado) |

**Transiciones**:

```text
null --lupa--> numeroActual        (encuadra, sube el teclado)
n --escribir 3 dígitos--> m         (encuadra de nuevo)
n --1 o 2 dígitos--> n              (no cambia nada)
n --bajar el teclado--> n           (FR-005c)
n --X--> null                       (el mapa no se mueve, FR-006c)
cualquiera --cerrar la app--> null  (FR-009)
cualquiera --ir a Salidas o Ajustes y volver--> null  (FR-009: el estado vive en la pantalla)
```

Si `numeroActual` es null (primera apertura), la lupa abre el campo vacío y `filtro` sigue en
`null` hasta que se escriban tres dígitos.

### Lista desplegada

`listaAbierta: Boolean`. Se cierra al tocar una fila. La posición para ordenarla se lee al abrirla
(D7) y no se actualiza mientras está abierta.

### Recorrido planeado

Vive dentro del diálogo (D10) y muere con él (FR-022).

```text
Parada(registroId: Long, latitud, longitud, numero, confianza, incluida: Boolean)
```

- Al abrir el diálogo: todas las patentes de la lista, `incluida = true`, en el orden de D9.
- Sacar o volver a poner una parada no cambia su lugar en la lista (US4, escenario 2).
- Las flechas intercambian una parada con su vecina.
- Se abre con las `incluida` en el orden de la lista. Con 0: botón deshabilitado. Con 1: "Ir"
  (FR-020). Con más de 10: aviso (FR-019b, D11).

## Lecturas derivadas (funciones puras, en `domain/`)

### `Cobertura.descubiertas(cuenta: Int): String`

| `cuenta` | Texto |
|---|---|
| 0 | "Ninguna descubierta" |
| 1 | "1 descubierta" |
| n > 1 | "n descubiertas" |

Reemplaza a `tieneElActual` y `consecutivosCubiertos`, que se borran (D3).

### `Prioridad` — el orden de revisión y el del recorrido

Un tipo mínimo para no atar `domain/` a Room:

```text
Candidato(id: Long, latitud: Double, longitud: Double, confianza: Int, capturadoEn: Long)
```

**`paraRevisar(candidatos, desde: Pair<Double, Double>?)`** (D8):

- Con `desde`: ascendente por `distancia / (1 + confianza / PESO_CONFIANZA)`, con
  `PESO_CONFIANZA = 5`. Empate: `capturadoEn` descendente.
- Sin `desde`: `confianza` descendente, después `capturadoEn` descendente.

**`ordenDeParadas(candidatos, desde)`** (D9). No se llama `recorrido` para no pisarse con
`data.Recorrido`, que es la salida grabada:

- Con `desde`: vecino más cercano. Primero la más cercana a `desde`, después cada vez la más
  cercana a la anterior. Empate: el orden de `paraRevisar`.
- Sin `desde`: `paraRevisar(candidatos, null)`.

Las dos usan `Geo.distanciaMetros`, que ya existe y ya está probado.

**Casos de prueba (`PrioridadTest`)**: los escenarios 3, 4 y 5 de la US3 con sus números exactos;
un caso del SC-005 (A a 200 m con confianza 3, B a 450 m con confianza 3: A primero); empate por
fecha; sin posición; un recorrido de tres paradas en línea donde el vecino más cercano no es el
orden de revisión.

### `Acomodo.para(pines, zoom): List<Dibujo>` (D13, US5)

```text
Pin(id: Long, latitud: Double, longitud: Double)

Dibujo = Suelto(id, latitudDibujo, longitudDibujo)
       | Grupo(latitud, longitud, cuenta)
```

**Reglas**:

1. Dos pines **chocan** si en ese zoom quedan a menos de `ANCHO_PIN_DP` entre sí.
2. Cada conjunto conexo de choques se intenta separar por repulsión, con tope de iteraciones.
3. Si se separa sin que ningún pin se corra más de `LIMITE_ACOMODO_M = 50` metros, todos salen
   `Suelto` en su posición corrida.
4. Si no, el conjunto entero sale como **un** `Grupo` en el centroide, con `cuenta` = cantidad.
5. Un pin que no choca con nadie sale `Suelto` en su posición real, sin correrse.
6. **Determinista**: mismas entradas, mismo resultado. Es lo que hace que desplazar el mapa no mueva
   nada (FR-023c).

**Casos de prueba (`AcomodoTest`)**: un pin solo no se mueve; dos pines a 10 m en zoom 17 salen
sueltos, separados al menos un ancho de pin y cada uno a menos de 50 m de su lugar; los mismos dos
en zoom 13 salen como un grupo de 2; veinte pines en el mismo punto salen como un grupo de 20 en
cualquier zoom; dos corridas con la misma entrada dan exactamente lo mismo.

La posición corrida es **solo de dibujo** (FR-023b). La ficha, "Ir", la distancia y el recorrido
reciben el `RegistroDeCaptura` por `id`, nunca la posición del `Dibujo`.

## Lo que se borra

| Qué | Por qué |
|---|---|
| `Cobertura.tieneElActual`, `Cobertura.consecutivosCubiertos` y sus casos en `CoberturaTest` | La barra ya no los muestra (FR-003a) |
| `PantallaBusqueda.kt` entero, `Destino.BUSQUEDA` | FR-013 |
| `Marcador.toca` como propiedad del feature y `porToca(...)` | La que toca tiene fuente y capa propias (D1) |
| `opcionesDeAgrupacion()` y el filtro por `point_count` | Solo si la US5 pasa la línea de corte (D13) |
