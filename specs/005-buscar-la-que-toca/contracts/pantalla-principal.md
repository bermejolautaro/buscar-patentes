# Contratos: la barra, la lista, el recorrido y el mapa

La app no expone API. Sus contratos son de interfaz: qué garantiza cada pieza de la pantalla
principal, y qué garantiza el mapa a quien le pasa marcadores. Se numeran C1 a C7 para que las
tareas puedan citarlos.

## C1 — La barra de arriba

**Cerrada** (sin filtro):

```text
┌───────────────────────────────────────────┐
│ 318                                   🔍  │
│ 3 descubiertas                            │
└───────────────────────────────────────────┘
```

**Con filtro**:

```text
┌───────────────────────────────────────────┐
│ [ 245 ]                               ✕   │
│ 1 descubierta                             │
└───────────────────────────────────────────┘
```

Garantías:

- Nunca muestra "números seguidos cubiertos" (FR-003a).
- La cuenta es la del número que muestra: el actual sin filtro, el del filtro con filtro
  (FR-003). Cuenta todas, compartidas o no (FR-003b).
- La lupa y la `X` son los **únicos** controles de filtro. Tocar cualquier otro lugar de la barra
  despliega o pliega la lista (C2).
- La lupa: filtro = número actual, campo abierto con los tres dígitos seleccionados, foco pedido,
  teclado arriba (FR-005). Encuadra (C5).
- El campo acepta solo dígitos, como mucho tres. El filtro cambia al completar el tercero
  (FR-005b), y cada cambio encuadra (C5).
- Bajar el teclado no cierra el filtro (FR-005c). La `X` sí, y no mueve el mapa (FR-006c).
- Sin número actual cargado: la barra dice lo que dice hoy en ese caso, y la lupa abre el campo
  vacío sin filtrar.
- La barra **no** está en el camino de la carga rápida: el campo de número de abajo sigue igual y
  gana el ancho del botón de búsqueda que se va (C7).

## C2 — La lista de la barra

- Se despliega debajo de la barra, sobre el mapa, con alto de **40 % de la pantalla como mucho**.
- Una fila por patente del número que muestra la barra, en el orden de `Prioridad.paraRevisar`
  (data-model).
- Cada fila: patente (texto completo si lo hay, si no el número con ceros), distancia y rumbo,
  confianza "N de 10" (FR-011).
- Sin posición: una línea arriba dice "Sin tu ubicación: ordenadas por confianza" (FR-012b).
- Tocar una fila: se pliega la lista, el mapa se centra en esa patente y deja de seguir al
  jugador, y se abre su ficha (FR-011a).
- Al pie, "Armar recorrido" (C3), solo si hay dos o más patentes.
- Con cero patentes del número, tocar la barra no despliega nada: la cuenta ya lo dijo.

## C3 — Armar el recorrido

```text
┌─ Recorrido por las 318 ───────────────────┐
│ ☑ 318  ·  120 m NE  ·  conf. 0    ▲ ▼     │
│ ☑ 318  ·  340 m S   ·  conf. 6    ▲ ▼     │
│ ☐ 318  ·  2,1 km O  ·  conf. 0    ▲ ▼     │
│                                           │
│        [ Cancelar ]  [ Abrir en Maps ]    │
└───────────────────────────────────────────┘
```

- Arranca con todas incluidas, en el orden de `Prioridad.ordenDeParadas` (FR-015).
- La casilla saca o vuelve a poner una parada sin moverla de lugar (FR-016).
- ▲ y ▼ la intercambian con la vecina. En la primera fila ▲ está deshabilitada; en la última, ▼
  (FR-017).
- "Abrir" decide con `Ir.accionPara(incluidas)`, pura y probada en `IrTest`: 0 incluidas,
  deshabilitado. 1, `Ir.aLaPatente` (FR-020). De 2 a 10, Google Maps (C4). Más de 10: aviso
  "Google Maps acepta hasta 10 paradas: sacá N", y no abre (FR-019b).
- Cerrar el diálogo tira el recorrido (FR-022). No arranca ninguna salida (FR-021).

## C4 — La URL de Google Maps

`Ir.urlDeRecorrido(paradas: List<Pair<Double, Double>>): String`, pura:

```text
https://www.google.com/maps/dir/?api=1&destination=<últ>&waypoints=<p1>%7C<p2>…&travelmode=walking
```

- `destination` es la **última** parada; `waypoints`, las anteriores **en orden**.
- Con dos paradas, un solo waypoint. Nunca `origin`: arranca donde está el teléfono.
- Coordenadas con punto decimal y siete decimales, sin importar el idioma del teléfono.

`Ir.enGoogleMaps(context, paradas): ResultadoRecorrido`:

1. `ACTION_VIEW` con `setPackage("com.google.android.apps.maps")`.
2. Si no hay app, el mismo `Intent` sin paquete. Si hay más de **4** paradas, no se abre y se
   devuelve el aviso de que en el navegador entran 4 (FR-019b).
3. Si tampoco hay navegador, se devuelve "no se pudo abrir" y la pantalla lo avisa (FR-019a).

## C5 — El mapa: encuadrar, centrar, y la que toca

`EstadoDelMapa` gana dos operaciones:

- `encuadrar(posiciones)`: apaga el seguimiento, suma la última posición conocida si la hay, y
  anima a los límites. Una sola posición en total: centra a zoom de calle. Ninguna: no se mueve
  (FR-006a, FR-006b).
- `centrarEn(latitud, longitud)`: apaga el seguimiento y anima hasta ahí, sin cambiar el zoom si
  ya es de calle o más (FR-011a).

`MapaDeFondo` sigue recibiendo `List<Marcador>`, y sigue sin saber de filtros. Lo que cambia
adentro:

| Fuente | Qué recibe | Agrupa | Capa | Orden de dibujo |
|---|---|---|---|---|
| `recorridos`, `recorridos-huecos` | Trazos | — | Líneas | 1.º (abajo) |
| `patentes` | `Marcador` con `toca = false` | Sí (o acomodo, C6) | Pines blancos, grupos, cuentas | 2.º |
| `patentes-toca` | `Marcador` con `toca = true` | **Nunca** | Pines verdes | 3.º (arriba de todo) |

- Pin de la que toca: relleno `#2F9E44`, borde por probabilidad (los mismos tres colores), número
  en blanco, tamaño ×1,4 (FR-001, FR-001a).
- Tocar el mapa consulta las **dos** capas de pines.
- Un filtro activo no llega al mapa como filtro: llegan menos marcadores (D4).

## C6 — El acomodo (US5)

`Acomodo.para(pines, zoom)` según data-model. En `MapaDeFondo`:

- `OnCameraIdle`: si `floor(zoom)` cambió desde el último acomodo, recalcular y `setGeoJson` a
  `patentes`.
- Cambiar los marcadores (guardar, borrar, filtrar) recalcula para el zoom actual.
- Los grupos llevan `cuenta` y las capas de grupo filtran por `has("cuenta")`.
- La fuente `patentes-toca` no pasa por el acomodo.
- **Línea de corte** (D13): si no pasa la prueba del teléfono, esta sección se retira entera y
  vuelve `withCluster`. C5 no cambia.

## C7 — Lo que se va

- `PantallaBusqueda`, su destino y su botón en la franja (FR-013).
- La franja de acciones queda con: recorridos, patentes, Salidas, Ajustes.
- La ficha gana "Agregar foto" cuando no tiene (FR-013a). Mismo flujo que tenía la búsqueda:
  permiso de cámara al primer uso, archivo nuevo, y `Captura.adjuntarFotoA`, que solo toca
  `fotoRuta`.
- Ningún texto de la app nombra "Buscar" como pantalla (FR-013c).
