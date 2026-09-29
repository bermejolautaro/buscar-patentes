# Phase 0: investigación y decisiones

Doce decisiones. Las que tocan el dibujo del mapa arrancan mirando qué hace hoy `Mapa.kt`,
porque la mitad de esta feature es cambiarle el sentido a mecanismos que ya funcionan.

---

## D1 — Cómo se dibuja un pin en MapLibre

**Decisión**: **tres bitmaps prerrenderizados con `android.graphics.Canvas`**, registrados en
el estilo al cargar, y una `SymbolLayer` con `iconImage` elegido por expresión según el escalón
de probabilidad. El pin lleva el relleno blanco y el borde de color **ya dibujados adentro**.

**Rationale**: MapLibre no tiene forma de dibujar un pin sin una imagen. Con tres imágenes
—una por escalón, que son tres y no cambian nunca— el borde queda bajo control total: grosor,
color y forma se deciden en el `Canvas` y no dependen de cómo el renderizador interprete nada.
Tres bitmaps de ~40×52 px es memoria despreciable, y se dibujan una sola vez por carga de
estilo. La `SymbolLayer` reemplaza a la `CircleLayer` actual sin sumar capas.

**Alternatives considered**:

- **Una imagen SDF teñida con `iconColor`.** Es el camino "elegante": una sola imagen, el color
  por expresión. Pero un SDF admite **un** color por feature, y el pin necesita dos —relleno
  blanco y borde de color—, así que hacen falta dos capas apiladas: el pin grande teñido con la
  probabilidad y encima el mismo pin más chico en blanco. Además `addImage(..., sdf = true)`
  espera un campo de distancia de verdad; pasarle una silueta alfa funciona pero deja el borde
  blando, y el borde es justamente el dato. Dos capas más y un riesgo de nitidez para ahorrar
  dos bitmaps: no.
- **Un bitmap por número de patente, con el número dibujado adentro.** Ya lo rechazó la 002 en
  su D4, y por la misma razón: son mil imágenes posibles. El número sigue yendo en su capa de
  texto.
- **Seguir con `CircleLayer`.** Es lo más barato de todo, pero el FR-028 pide pin. El pedido no
  es cosmético: la punta señala el lugar exacto, y la forma —no el color— es lo que separa una
  patente de un camino ahora que el color del marcador ya no dice estado.

**Riesgo vigilado**: si el pin dibujado a mano no se lee bien encima del trazo, el arreglo es el
grosor del borde en el `Canvas`, no una capa más. `ponytail: las medidas del pin son
calibrables contra la pantalla real, no constantes sagradas.`

---

## D2 — Dónde va el número adentro del pin

**Decisión**: `iconAnchor(BOTTOM)` para que la punta caiga sobre la coordenada, y el número en
su capa de texto con `textAnchor(CENTER)` más un `textOffset` **en ems** hacia arriba, que lo
sube hasta la cabeza del pin.

**Rationale**: `textOffset` se mide en ems, o sea relativo a `textSize`. Eso hace que el offset
siga siendo correcto cuando el tamaño del texto cambia, que es exactamente lo que pasa con el
pin de la patente que toca (D3). Un offset en píxeles habría que recalcularlo por caso.

Se conserva de la 003 lo que ya está aprendido y no se toca: `textFont("Noto Sans Regular")` es
obligatorio —sin él el pedido de glifos no se resuelve nunca contra OpenFreeMap Liberty y se cae
el teselado de la fuente entera, que fue el bug de los marcadores invisibles— y
`textAllowOverlap` + `textIgnorePlacement` en true, porque un marcador que desaparece es peor
que uno amontonado.

**Cambia respecto de hoy**: `textColor` pasa de `#FFFFFF` a negro (FR-029).

---

## D3 — Cómo se destaca la patente del número actual

**Decisión**: **por tamaño**. Una propiedad booleana por feature, y `iconSize` + `textSize`
resueltos con una expresión `switchCase` sobre ella. Se le suma `symbolSortKey` para que ese
pin se dibuje **encima** de los demás cuando se superponen.

**Rationale**: es lo que pide el FR-030d —destacar sin color— con cero capas nuevas y cero
imágenes nuevas. El `textOffset` en ems de D2 escala solo, así que el número queda centrado en
la cabeza del pin grande sin ninguna cuenta aparte. El `symbolSortKey` es una propiedad y
resuelve el caso que arruinaría el efecto: que el pin importante quede debajo de uno cualquiera.

**Alternatives considered**:

- **Un halo**: una capa más con la misma imagen, más grande y en un tono claro, filtrada a la
  que toca. Se ve bien pero es una capa y una expresión de filtro para decir lo que el tamaño ya
  dice.
- **Relleno verde, como excepción al pin blanco.** Es la señal más fuerte de todas y hay cero o
  un pin así en pantalla. Se descartó por decisión explícita del jugador en la clarificación: el
  color del marcador queda diciendo una sola cosa, la probabilidad.

---

## D4 — Cómo cambia el mapa de modo

**Decisión**: **una sola capa de líneas**, como hoy. El escalón de antigüedad viaja como
propiedad de cada feature; el **modo** no viaja en la feature, se aplica como propiedad de
pintura de la capa:

| Modo | Qué se le hace a la capa |
|---|---|
| Cobertura | `lineColor` = literal azul de ruta, `lineWidth` y `lineOpacity` únicos |
| Antigüedad | `lineColor` = `Expression.step` sobre la propiedad del escalón |
| Apagado | `visibility = NONE` en las capas de trazo y de huecos |

**Rationale**: el modo es un valor global, no un atributo de cada camino. Meterlo en cada
feature obligaría a reconstruir la `FeatureCollection` entera en cada toque del interruptor;
así, cambiar de modo son tres `setProperties` sobre capas que ya existen, y la fuente ni se
entera. Apagar con `visibility` deja la fuente intacta, así que volver a prender no recalcula
nada.

**Alternatives considered**:

- **Tres capas, una por modo, mostrando la que corresponde.** Triplica lo que hay que mantener
  en espejo, que es el problema que el comentario de `pintarTrazos` ya nombra desde la 003.
- **Alimentar la fuente con una colección vacía para apagar.** Funciona, pero tira el trabajo de
  armar la colección y lo rehace al prender. `visibility` es una propiedad.

---

## D5 — Que la salida más reciente gane donde se superponen (FR-013)

**Decisión**: ordenar los trazos **de la más vieja a la más nueva** al armar la
`FeatureCollection`. MapLibre dibuja las features de una capa de líneas en el orden de la
fuente, así que la más reciente queda encima sin ninguna cuenta de geometría.

**Rationale**: es una línea (`sortedBy`) contra calcular intersecciones de polilíneas en el
teléfono para un resultado que se ve igual. La spec ya lo asumió explícitamente.

**Alternatives considered**:

- **Partir los caminos en tramos y quedarse con el más reciente de cada uno.** Es la respuesta
  "correcta" y cuesta geometría real: buffers, intersecciones y un umbral de tolerancia que
  habría que calibrar. Para un mapa donde las salidas se pisan en avenidas, la diferencia visible
  es ninguna.

**Riesgo vigilado, con salida nombrada**: si en el teléfono el orden de dibujo dentro de la capa
no resulta ser el de la fuente, el plan B es **una capa por escalón** —tres— agregadas de la más
vieja a la más nueva. Cuesta dos capas y ninguna lógica nueva. Se decide mirando el mapa, no
discutiéndolo. `ColeccionTest` cubre el orden de la colección, que es lo que sí se puede probar
en JVM.

---

## D6 — Dónde se guarda el modo elegido

**Decisión**: una columna `modoMapa` en `estado_del_juego`, con enum `ModoMapa` y los dos
convertidores que la clase `Convertidores` ya tiene por patrón. Migración v5 → v6, aditiva, con
`DEFAULT 'COBERTURA'`.

**Rationale**: esa tabla ya guarda una preferencia del jugador de exactamente la misma
naturaleza (`avisosActivos`). Sumar una columna a una fila única es la operación más barata que
existe acá, y el `DEFAULT` resuelve solo el caso de la app ya instalada: quien actualiza abre en
cobertura, que es el modo que la feature quiere mostrar primero.

**Alternatives considered**:

- **DataStore o SharedPreferences.** Sería un segundo mecanismo de persistencia conviviendo con
  Room para guardar un valor, cuando la tabla de preferencias ya está y ya se lee en el mismo
  efecto de la pantalla principal. Principio IV.
- **No persistirlo, dejarlo en memoria.** Lo prohíbe el FR-003, y con razón: el jugador que
  apagó los recorridos no quiere volver a apagarlos cada vez que abre.

---

## D7 — Dónde vive el interruptor

**Decisión**: un cuarto `FilledTonalIconButton` en la **franja de acciones que ya existe**,
junto a Buscar, Salidas y Ajustes.

**Rationale**: el FR-026 de la 003 dice que todo lo que se toca vive en esa franja, y lo dice
por experiencia propia: la primera versión del botón de recentrar flotaba con
`align(BottomEnd)` y un margen absoluto, quedaba encima del campo de número y **no recibía el
toque**. Dentro de la columna el layout garantiza que nada se pise.

**Alternatives considered**:

- **Un botón flotante sobre el mapa.** Es el error ya cometido, documentado en el código.
- **Una opción en Ajustes.** Lo prohíbe el FR-001: cambiar de modo es algo que se hace mirando
  el mapa, no entrando a otra pantalla.

**Consecuencia vigilada**: son cuatro botones más el de recentrar en la misma fila que el campo
de número. El contrato C4 le pone techo explícito.

---

## D8 — Cómo se dice en qué modo está el mapa (FR-004) y la leyenda (FR-014a)

**Decisión**: **una sola línea** apoyada arriba de la franja de acciones. Siempre presente,
contenido según el modo:

| Modo | Qué dice la línea |
|---|---|
| Cobertura | "Recorridos: todo lo caminado" |
| Antigüedad | Los tres escalones con su color y su plazo: hasta 3 días · 4 a 14 · más de 14 |
| Apagado | "Recorridos ocultos" |

**Rationale**: los dos requisitos —decir el modo y explicar la escala— son la misma línea de
interfaz vista en dos modos, no dos componentes. Y hace falta que sea texto: con el mapa sin
salidas guardadas, "apagado" y "no caminé nada" se ven idénticos, así que el icono del botón no
alcanza para distinguirlos.

**Alternatives considered**:

- **Que la línea aparezca unos segundos al cambiar de modo y se desvanezca.** Ocupa menos
  pantalla, cuesta un temporizador y un estado más, y deja al jugador sin la leyenda justo
  cuando está mirando el mapa un rato largo, que es cuando la necesita.
- **Solo el icono del botón.** No cubre el caso del mapa vacío.

---

## D9 — De qué fecha sale el escalón de antigüedad

**Decisión**: `finalizadoEn ?: iniciadoEn`, comparado contra el momento actual. Función pura en
`domain/Antiguedad.kt`, con los cortes en **3 y 14 días** (FR-014).

**Rationale**: una salida en curso no tiene `finalizadoEn`, y su antigüedad es cero: caer en
`iniciadoEn` da el escalón más reciente, que es lo correcto y lo que pide el caso borde de la
spec. Que sea una función pura es lo que la hace probable en JVM, que es lo que la constitución
exige para toda lógica no trivial. Es la única prueba nueva de la feature.

**Casos que la prueba cubre**: los dos bordes exactos (3 días y 14 días), una salida en curso,
una salida de hace un año, y una fecha futura por reloj corrido —que cae en el escalón más
reciente y no rompe—.

---

## D10 — El detalle de una salida

**Decisión**: **una sub-pantalla dentro de `PantallaRecorridos`**, gobernada por el estado
`abierta: Long?` que ya existe, más su propio `BackHandler`. Cuando hay una salida abierta la
pantalla muestra el detalle en lugar de la lista.

**Rationale**: el estado ya está —hoy decide qué fila se despliega—; lo que cambia es que en vez
de expandir una fila reemplaza la lista. Eso resuelve el FR-017 y el FR-021 de una: el mapa deja
de estar adentro de un `LazyColumn` y pasa a tener la pantalla entera.

**Alternatives considered**:

- **Un quinto valor en el enum `Destino` de `MainActivity`.** El enum no lleva argumentos, y el
  detalle necesita saber qué salida abrir. Habría que agregarle un campo al destino o una
  variable suelta al lado, que es peor que tener el estado en la pantalla que lo usa.
- **Navigation-Compose.** El comentario de `MainActivity` dice que un destino con argumentos
  sería el momento de considerarla. Se considera y se descarta: esto no es un destino par de las
  otras cuatro pantallas, es el adentro de una de ellas, y una dependencia con grafo, rutas y
  serialización de argumentos para eso es exactamente lo que el Principio IV prohíbe.

---

## D11 — Los colores

**Decisión**:

| Rol | Color | Nota |
|---|---|---|
| Trazo en cobertura | Azul de ruta, `#1A73E8` aprox. | El color que ya significa "camino" para cualquiera que usó un GPS (FR-008a) |
| Antigüedad, hasta 3 días | Gris apagado | Es lo que **no** hay que revisar: se corre al fondo |
| Antigüedad, 4 a 14 días | Violeta medio | |
| Antigüedad, más de 14 días | Magenta fuerte | Lo viejo es lo que resalta (FR-012) |
| Borde del pin | Los tres de confianza que ya existen: rojo, ámbar, verde | Sin cambios (FR-030) |

**Rationale**: la familia violeta/magenta está libre en el mapa y no choca ni con el azul de
cobertura —lo prohíbe el FR-012b— ni con el rojo/ámbar/verde del borde del pin, que dice otra
cosa. Elegir rojo para "viejo" habría puesto líneas rojas y anillos rojos significando cosas
distintas en la misma pantalla.

Que el violeta vuelva al mapa no contradice a la 002: lo que esa spec sacó fue el violeta que
**Material 3 ponía por default sin que nadie lo escribiera** en fondos y superficies. Los colores
del mapa son una paleta aparte y declarada, como dice el contrato C1 de la 003.

**Se borran**: `PENDIENTE`, `TOCA`, `COMPARTIDA` y `RECORRIDO_VIEJO`. Los tres primeros porque
el marcador deja de codificar estado (FR-030a); el cuarto porque el histórico apagado es
justamente lo que esta feature vino a revertir.

**Riesgo vigilado**: los valores exactos se calibran mirando el teléfono al sol, en tema claro y
oscuro. `ponytail: los hex son un punto de partida, no una decisión cerrada.`

---

## D12 — Los grupos de patentes

**Decisión**: **quedan como están**: círculo gris con la cuenta en blanco. No se convierten en
pines.

**Rationale**: un grupo no tiene un número de patente que mostrar ni una probabilidad propia, y
ahora que los sueltos son pines, la **forma** los separa —círculo contra pin— mejor de lo que el
color los separaba antes. Es la respuesta al caso borde de la spec, y no cuesta ni una línea:
las dos capas de grupo no se tocan.

Lo que sí se toca es la agrupación misma, y solo para no romperla: `clusterRadius` está fijado en
28 px porque es el diámetro del marcador actual, "así que dos registros se agrupan justo cuando
se taparían entre sí". El pin no mide lo mismo que el círculo, así que ese número se recalibra
contra el ancho real del pin. Es un número, no una decisión.
