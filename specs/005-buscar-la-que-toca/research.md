# Research: Ir a buscar la que toca

Decisiones de Phase 0. Cada una dice qué se eligió, por qué, y qué se descartó. Ninguna agrega
dependencias: todo sale de Compose, MapLibre 13.6 y lo que ya está en `domain/`.

## D1 — La que toca vive en una fuente propia, sin agrupar

**Decisión**: las patentes del número actual van a una segunda `GeoJsonSource` (`patentes-toca`)
**sin** `withCluster`, con su propia `SymbolLayer`, agregada **después** de las capas de grupo. El
resto sigue en la fuente agrupada de siempre.

**Por qué**: la agrupación de MapLibre es una propiedad de la fuente, no de cada feature. No hay
forma de decirle "agrupá todo menos estas". Dos fuentes cumplen FR-002 de raíz —lo que no está en
la fuente agrupada no puede quedar adentro de un grupo— y FR-002a sale gratis: la cuenta del grupo
solo ve lo que hay en su fuente. Que la capa se agregue después de los grupos es lo que la dibuja
encima (el orden de inserción es el orden de dibujo, contrato C2 de la 004).

**Efecto de rebote, a favor**: con una capa propia para la que toca, la capa común deja de
necesitar `porToca(...)` en tamaño, texto y `symbolSortKey`. Cada capa tiene valores fijos. Se
borran la expresión y la propiedad `toca` del feature.

**Descartado**:
- `clusterProperties` / `clusterMinPoints`: agregan datos al grupo o suben el mínimo, no excluyen
  features.
- Bajar `clusterMaxZoom`: reduce el problema a algunos zooms, no lo elimina. FR-002 dice "a ningún
  zoom".
- Filtro de capa sobre la fuente agrupada: filtra lo que se **dibuja**, pero la feature ya quedó
  adentro del grupo antes de que el filtro la vea.

## D2 — El fondo verde es otra tanda de bitmaps

**Decisión**: `bitmapDePin` recibe también el color de relleno. Se registran tres imágenes más
(`pin-toca-baja/media/alta`) con relleno `#2F9E44`, el `TOCA` que la 004 retiró, y la capa de la
que toca elige entre esas tres por probabilidad. El número va en **blanco** sobre el verde.

**Por qué**: el pin ya se elige por imagen y no por tinte (D1 de la 004), así que el verde es la
misma mecánica con un color más. `#2F9E44` es el verde que el jugador recuerda, y el borde alto
(`#40C057`) se eligió en su momento justamente para leerse sobre ese relleno (el comentario sigue
en `Tema.kt`). Negro sobre ese verde queda con poco contraste; blanco es lo que llevaba antes de
la 004.

**Descartado**: `iconColor` con imágenes SDF. Obligaría a rehacer el pin como SDF, que pierde el
borde de otro color: un SDF se tiñe de un solo color.

## D3 — "Descubiertas" reemplaza a la cuenta de números seguidos

**Decisión**: `Cobertura` pierde `tieneElActual` y `consecutivosCubiertos` —su único consumidor
es la barra, que deja de usarlas— y gana `descubiertas(cuenta: Int): String`, que devuelve
"Ninguna descubierta", "1 descubierta" o "N descubiertas". La cuenta es
`registros.count { it.numero == n }`, **sin filtrar por estado** (FR-003b).

**Por qué**: la cuenta es un `count`, no necesita función. El texto sí: tiene tres ramas, y la
constitución pide prueba para toda rama. Vive en `Cobertura` porque es exactamente lo que ese
objeto dice ser —qué tiene cubierto el jugador del número que toca—, y así `CoberturaTest` cambia
de casos en vez de nacer un archivo nuevo.

**Descartado**: `pluralStringResource`. Resuelve la gramática por idioma, y la app es de un solo
idioma; además no cubre el "Ninguna", que no es un plural sino otra frase.

## D4 — El filtro es un `Int?` en la pantalla principal

**Decisión**: `var filtro by remember { mutableStateOf<Int?>(null) }` en `PantallaPrincipal`. Los
marcadores se filtran **antes** de mandarlos a `MapaDeFondo`, igual que hoy se mandan vacíos
cuando las patentes están escondidas. La barra muestra `filtro ?: numeroActual`.

**Por qué**: es el mismo patrón que `patentesVisibles` de la 004 —estado de pantalla, no
persistido, y el mapa no se entera de por qué le llegan menos marcadores—. `remember` y no
`rememberSaveable`: FR-009 pide que el filtro no sobreviva a reabrir la app, y `rememberSaveable`
lo haría volver después de que el sistema mate el proceso.

**Descartado**: filtrar con una expresión de capa en MapLibre. Sería más rápido de conmutar, pero
la fuente agrupada seguiría agrupando las patentes ocultas con las visibles: un grupo con cuenta 4
donde se ve una sola 318.

## D5 — El campo de filtro: `TextFieldValue` con todo seleccionado y foco pedido

**Decisión**: la barra tiene dos estados. Cerrada muestra el número, la cuenta y la lupa. Al tocar
la lupa se arma el filtro con el número actual, el texto pasa a ser un `OutlinedTextField` con un
`TextFieldValue` cuyo rango de selección cubre los tres dígitos, y un `FocusRequester` pide el foco,
lo que sube el teclado (clarificación: el teclado sube de entrada). Una `X` cierra el filtro.

**Por qué**: con los dígitos seleccionados, tipear reemplaza en vez de agregar: "otro número" son
tres toques de teclado, sin borrar. `TextFieldValue` es justamente lo que la pantalla principal
dejó de usar en la 003 por no tener nada que seleccionar; acá sí lo hay.

**El teclado baja y el filtro sigue** (FR-005c): bajar el teclado le saca el foco al campo pero no
toca `filtro`. Cerrar el filtro es solo la `X`.

**Descartado**: abrir una pantalla o diálogo de búsqueda. Es lo que había, y es lo que el jugador
pidió sacar.

## D6 — Encuadrar al filtrar: un método nuevo en `EstadoDelMapa`

**Decisión**: `EstadoDelMapa.encuadrar(posiciones)` apaga el seguimiento (`CameraMode.NONE`,
`siguiendo = false`, lo mismo que hace el arrastre), agrega la **última posición conocida** del
`locationComponent` si está activo, y anima la cámara a los límites con un margen. La pantalla lo
llama cada vez que el filtro cambia de número. El cálculo de límites se comparte con el `encuadrar`
privado que ya existe (un punto: centrar a zoom de calle; dos o más: `LatLngBounds`).

**Por qué**: `EstadoDelMapa` ya es el canal por el que la pantalla le pide cosas a la cámara
(`recentrar`). FR-006b sale solo: al apagar el seguimiento, el botón de recentrar aparece, que es
la forma de volver.

**Margen**: el mismo `RELLENO_ENCUADRE` de hoy, más el alto de la barra arriba. Con el teclado
arriba la mitad de abajo queda tapada. Se acepta: el jugador eligió que el teclado suba, y el
encuadre queda bien apenas lo baja.

**Descartado**:
- Encuadrar sin la posición del jugador. Era la opción A de la clarificación; se eligió la B.
- Recalcular el margen con la altura del teclado: es un número que cambia mientras el teclado
  anima, y la corrección se ve apenas se baja el teclado.

## D7 — La lista se despliega debajo de la barra, sobre el mapa

**Decisión**: tocar la barra (fuera de la lupa) alterna un panel con una `LazyColumn` de alto
acotado (como mucho el 40 % de la pantalla) debajo de la barra. Cada fila lleva el número o la
patente completa, la distancia y el rumbo (`Geo.distanciaYRumbo`) y la confianza. Tocar una fila
cierra el panel, centra el mapa en esa patente (`EstadoDelMapa.centrarEn`, que también apaga el
seguimiento) y abre su ficha por el mismo camino que hoy usa tocar un pin (`registroTocado`).

**Posición**: se lee **una vez** al desplegar, con `ubicacion.leerAhora()`, igual que la ficha.
Sin permiso o sin lectura, `null`, y la lista ordena por confianza (FR-012b).

**Por qué**: es la lista que hoy vive en otra pantalla, puesta donde el jugador ya está mirando.
Acotarla al 40 % deja a la vista el mapa, que es lo que la lista describe. Con los tres o cuatro
registros que tiene un número, casi nunca hace falta desplazarla.

**Descartado**: `ModalBottomSheet`. Tapa el mapa con una hoja modal que hay que descartar para
volver a mirarlo, y desde abajo compite con el campo de carga y el teclado.

## D8 — El orden de revisión es la distancia dividida por la confianza

**Decisión**: `Prioridad.paraRevisar(candidatos, desde)` ordena por **distancia efectiva**:

```text
distancia efectiva = distancia en metros / (1 + confianza / PESO_CONFIANZA)
PESO_CONFIANZA = 5
```

A igualdad, gana la capturada más recientemente (FR-012c). Sin posición (`desde == null`), ordena
por confianza descendente y después por más reciente.

**Por qué este número**: con confianza 10 la distancia efectiva se divide por 3; con 5, por 2.
Cumple los tres escenarios de la US3:

| Caso | Efectiva A | Efectiva B | Primero |
|---|---|---|---|
| 100 m y 600 m, misma confianza | 100 | 600 | A |
| 50 m conf. 0 y 240 m conf. 10 | 50 | 80 | A (la cercana) |
| 300 m conf. 0 y 500 m conf. 10 | 300 | 167 | B (la de más confianza) |

Y garantiza el SC-005 por construcción: si B está a más del doble de distancia que A con igual o
menos confianza, su divisor es igual o menor y su distancia, más del doble, así que su efectiva
es mayor.

**Por qué división y no resta**: restar "N metros por punto de confianza" trata igual 50 m que
5 km. El pedido es que una muy cercana se revise aunque tenga poca confianza, y eso es una
proporción: cien metros de diferencia pesan mucho a media cuadra y nada a diez.

**ponytail**: `PESO_CONFIANZA` es el número a calibrar en la calle (FR-012a). Hoy casi todas las
patentes tienen confianza 0 (FR-024), así que el orden va a ser casi solo por distancia hasta que
se acumulen confirmaciones.

## D9 — El orden propuesto del recorrido es el vecino más cercano

**Decisión**: `Prioridad.ordenDeParadas(paradas, desde)` arranca en la parada más cercana a `desde` y
sigue cada vez por la más cercana a la anterior. Sin posición, devuelve el orden de revisión (D8)
tal cual.

**Por qué**: FR-015 lo pide así, y es la respuesta correcta para pocas paradas. Con diez paradas
como techo (D11) el recorrido óptimo sería otra cosa, y el jugador lo corrige a mano con las
flechas.

**Descartado**: el recorrido óptimo (TSP). Para diez paradas es resoluble por fuerza bruta, pero es
código y prueba para una diferencia de pocas cuadras que Google Maps además redibuja por calles.

## D10 — Armar el recorrido es un diálogo con flechas

**Decisión**: un `Dialog` de Compose, abierto desde un botón "Armar recorrido" al pie de la lista
de la barra. Una fila por parada con casilla (incluida o no), número, distancia · confianza, y
**dos flechas** para subirla o bajarla. Abajo, "Abrir en Google Maps", deshabilitado sin paradas
incluidas. El estado es una `List<Parada>` local del diálogo, y se pierde al cerrarlo (FR-022).

**Por qué flechas y no arrastrar**: FR-017 pide una mano y sin tipear. Arrastrar en una
`LazyColumn` de Compose no viene hecho: es detectar el gesto, calcular el desplazamiento y
reordenar mientras se mueve. Con tres o cuatro paradas, dos flechas por fila son un toque por
posición y cero código de gestos.

**Descartado**: pantalla propia. Un diálogo alcanza para una lista de a lo sumo diez filas y no
pide navegación.

## D11 — Google Maps por URL de recorrido, con el navegador de respaldo

**Decisión**: el recorrido se abre con una **Maps URL** de direcciones:

```text
https://www.google.com/maps/dir/?api=1
  &destination=LAT,LNG                    ← la última parada
  &waypoints=LAT,LNG|LAT,LNG              ← las del medio, en orden
  &travelmode=walking
```

Sin `origin`: Google Maps arranca en la ubicación del teléfono, que es FR-019. Primero se intenta
con `setPackage("com.google.android.apps.maps")`. Si no está instalada, el mismo `Intent` sin
paquete (lo abre el navegador). Si tampoco hay quien lo abra, se avisa (FR-019a). Una sola parada
usa `Ir.aLaPatente` tal cual (FR-020).

**Límites** (documentación de Maps URLs): hasta **9 waypoints** en la app, más el destino, o sea
**10 paradas**; en un navegador de teléfono, **3 waypoints**, o sea **4 paradas**. Con más de 10
incluidas se avisa antes de abrir. Si hubo que caer al navegador y hay más de 4, se avisa en vez
de abrir (FR-019b): Google Maps descartaría el resto sin decir nada.

**`travelmode=walking`**: el jugador camina. Así Google Maps no propone subir a una avenida.

**Por qué se aparta de `geo:`**: `geo:` es un punto, sin paradas. La spec ya declaró la
desviación de la D2 de la 003 (FR-019). "Ir" sigue usando `geo:`: para un punto, cualquier app de
mapas sirve.

**Prueba**: la URL se arma en una función pura, `Ir.urlDeRecorrido(paradas)`, que devuelve texto
y se prueba en JVM: orden de las paradas, destino = la última, separador `|` codificado, sin
`waypoints` cuando hay una sola parada. Y la decisión de qué hacer según cuántas paradas hay
incluidas —ninguna, una, más de las que entran, o abrir— es otra función pura,
`Ir.accionPara(incluidas, maximo)`, que usan tanto el diálogo (con el máximo de la app) como el
respaldo del navegador (con 4). Es la única rama de la US4, y por eso es la que se prueba.

**Descartado**: `google.navigation:`. Admite waypoints solo en versiones recientes y sin
documentación estable, y arranca la navegación paso a paso sin dejar ver el recorrido antes.

## D12 — La pantalla de búsqueda se borra, y "agregar foto" se muda a la ficha

**Decisión**: se borran `PantallaBusqueda.kt`, `Destino.BUSQUEDA`, el parámetro `onBuscar` y el
botón de la franja de acciones. El icono `ic_buscar` queda: pasa a ser la lupa de la barra. El
lanzador de cámara y el pedido de permiso que vivían en `PantallaBusqueda` se mudan a
`FichaConectada`, y `FichaDeRegistro` gana un botón "Agregar foto" cuando `fotoRuta == null`, que
llama a `Captura.adjuntarFotoA` como hoy.

**Efectos colaterales que hay que cerrar** (FR-013c):
- `PantallaAjustes` dice "borrá registros ya compartidos desde Buscar". Pasa a decir "desde la
  ficha".
- `BarraSuperior` justifica su existencia con tres usos; quedan dos (Salidas y Ajustes), que
  siguen alcanzando para el Principio IV. Se actualiza el comentario.
- La franja de acciones pierde un botón, así que el campo de número **gana** ancho. Es la
  dirección que el contrato C4 de la 004 pedía cuidar.

**Descartado**: dejar la pantalla como atajo. El jugador eligió sacarla, y dos caminos para lo
mismo es justo lo que el Principio IV no quiere.

## D13 — Pines que se hacen lugar: acomodo propio por zoom, reemplazando la agrupación de MapLibre

Es la decisión de más riesgo de la feature, y por eso va última y con línea de corte.

**Decisión**: la fuente de las patentes (no la de la que toca) deja de usar `withCluster`. En su
lugar, una función pura, `Acomodo.para(pines, zoom)`, calcula para un zoom entero qué pines se
dibujan sueltos —cada uno con su posición **de dibujo**— y cuáles se juntan en un grupo con su
cuenta:

1. Proyecta los pines a píxeles lógicos del mundo en ese zoom (Web Mercator, teselas de 512).
2. Arma los conjuntos de pines que se encimarían (a menos de un ancho de pin entre sí).
3. Para cada conjunto, los separa por repulsión, con un tope de iteraciones.
4. Si al final ninguno se enciman y ninguno se movió más que **50 m** en el terreno (convertidos a
   píxeles de ese zoom), quedan sueltos en su posición corrida. Si no, el conjunto entero pasa a
   ser un grupo en su centro, con su cuenta.

`MapaDeFondo` escucha `OnCameraIdle`: si el zoom entero cambió, pide el acomodo de ese zoom y le
hace `setGeoJson` a la fuente. Los features llevan `cuenta` cuando son grupo, y las capas de
grupo filtran por esa propiedad en vez de `point_count`.

**Por qué**:
- MapLibre no tiene nada que corra un símbolo y **después** agrupe. `text-variable-anchor` corre
  etiquetas para evitar choques, pero la que no entra en ninguna posición **se esconde**, y una
  patente que desaparece del mapa es exactamente el problema que el TODO reporta.
- Calcular por zoom entero y no de continuo cumple FR-023c: desplazar el mapa no cambia el zoom,
  así que no cambia nada de lo dibujado.
- Es una función pura sobre números: se prueba en JVM, que es lo que la constitución pide para la
  lógica no trivial.
- Con cien patentes y ocho zooms útiles, el costo es despreciable.

**Costo aceptado**: durante un pellizco el acomodo es el del zoom anterior, y cambia de golpe al
soltar. Es el comportamiento de las agrupaciones en las apps de mapas, y la alternativa —acomodar
en cada cuadro— rompería FR-023c.

**Números** (calibrables, FR-023a):

| Zoom | m por dp (lat −34,6) | 50 m en dp | Qué se puede separar |
|---|---|---|---|
| 15 | 1,97 | 25 | pares que apenas se tocan |
| 16 (calle) | 0,98 | 51 | grupos chicos de la misma cuadra |
| 17 | 0,49 | 102 | casi todo lo que no está en la misma puerta |

**Línea de corte**: si en el teléfono el salto al soltar el pellizco molesta, o el mapa pierde
fluidez, se vuelve a `withCluster` con el radio de hoy y la US5 queda como pedido abierto. Nada
de las US1 a US4 depende de esto: la que toca ya está fuera de los grupos por D1, en su propia
fuente.

**Descartado**:
- Achicar `withClusterRadius` y dejar que los pines se encimen. Agrupa menos, pero no separa:
  FR-023 pide que se corran, no que se tapen.
- Filtrar por zoom dentro de la capa (`["==", ["get","z"], ["floor", ["zoom"]]]`) con todos los
  zooms precalculados en una sola fuente. Evitaría el `setGeoJson` al cambiar de zoom, pero el
  soporte de `zoom` en filtros de MapLibre Native no está documentado con claridad. Si en la
  implementación el `setGeoJson` se nota, se prueba esto antes de cortar.
- Una línea desde el pin corrido hasta su punto real. La spec dice que no hace falta.

## D14 — La confianza que arranca en cero ya está hecha

**Decisión**: FR-024 no genera trabajo nuevo. `Probabilidad.INICIAL = 0`, la columna
`probabilidadInicial` y la migración v6 → v7 entraron el 2026-09-28 con su prueba
(`ProbabilidadTest.conserva sus votos solo si la dejaban por encima de 5`). Las tareas de esta
feature solo lo verifican.
