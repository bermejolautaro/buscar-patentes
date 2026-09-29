# Feature Specification: Ir a buscar la que toca

**Feature Branch**: `005-buscar-la-que-toca`

**Created**: 2026-09-28

**Status**: Draft

**Input**: User description:

> Quiero que escribas una feature basado en el TODO, también si ves que un item del todo es una
> tarea muy grande podemos partirlo en otro spec dedicado.

Los ítems del `TODO.md` que esta spec toma, en palabras del jugador:

> - El filtro de buscar estaría bueno que si yo busco por ejemplo "318", me muestre en el mapa
>   solo las 318. Con eso dicho creo que el filtro debería funcionar a nivel home en vez de otra
>   pantalla, o dividir la funcionalidad.
> - Los grupos son muy sensibles, estaría bueno que haya un threshold donde las patentes puedan
>   empujarse entre sí un poco para mostrarse sin agruparse y después de ese threshold ya
>   agruparse. Mientras la patente quede sobre la misma cuadra ya es suficiente para entender
>   dónde está.
> - En la pantalla de patentes no se muestra la confianza.
> - En la pantalla de patentes, al buscar por una patente no se ordenan de más cercana a más
>   lejana (y también habría que darle prioridad a más confianza, ahí debería ser más o menos
>   weighted porque capaz una tiene confianza más o menos pero está tan cerca que vale la pena
>   chequearla).
> - La app dice "6 números seguidos cubiertos" y no tengo idea qué significa. A mí me
>   interesaría saber cuántas instancias hay de la patente que estoy buscando: "Ninguna
>   descubierta", "2 descubiertas", etc.
> - Ahora en el mapa veo que hay 318, me gustaría una forma fácil de armar un recorrido que pase
>   por las 3. Quizás agregar las 3 a Google Maps en un solo recorrido. También pienso que alguna
>   patente quizás está muy lejos, así que se debería poder elegir cuáles quiero agregar y cuáles
>   no. También cambiar el orden en que las busco, pero debería ser cómodo.
> - Que vuelva el fondo verde para la patente buscada.
> - Que la patente buscada se vea incluso si está dentro de un grupo en todo momento.

## Contexto

La app ya sabe dónde están las patentes del número que el juego está pidiendo. Lo que no hace
es **llevar al jugador hasta ellas**. Hoy, con el juego en 318, el camino es este:

1. En el mapa, las 318 son tres pines entre casi noventa. Se distinguen solo por el tamaño, y si
   alguna cae cerca de otras queda **adentro de un grupo**, invisible.
2. La barra de arriba dice "Tenés la 318" y abajo "6 números seguidos cubiertos", una cifra que
   el jugador no sabe leer. Tocarla no hace nada: no lleva a la patente ni a ningún lado.
3. La pantalla de búsqueda, en otra pantalla, lista las 318 sin la confianza y en orden de
   captura, no de cercanía: la que conviene revisar primero puede estar última.
4. "Ir" abre de a una patente. Para pasar por las tres hay que volver a la app entre parada y
   parada.

Cada paso tiene el dato y lo pierde en el camino. Esta feature es una sola línea: **ver** dónde
están las que tocan, **elegir** cuáles vale la pena revisar, e **ir** a buscarlas. Y la línea
pasa entera por la pantalla principal: la barra de arriba, que hoy ocupa lugar sin hacer nada,
pasa a ser el lugar donde se elige qué número mirar.

### Qué pasa con cada ítem del TODO

| Ítem del TODO | Dónde queda |
|---|---|
| Botón de esconder patentes | Ya resuelto por la 004 (US5, FR-033 a FR-035b). Sale del TODO. |
| Filtrar el mapa por número | **US2** |
| Grupos menos sensibles | **US5** |
| Diagonales en los caminos ajustados | **Spec aparte**: es un defecto del ajuste a calles, no de las patentes |
| Ver el camino crudo o el ajustado | **Spec aparte**, junto con el anterior: es la herramienta para diagnosticarlo |
| Confianza en la pantalla de patentes | **US3** |
| Ordenar por cercanía y confianza | **US3** |
| "N números seguidos cubiertos" | **US1** |
| Recorrido por varias patentes | **US4** |
| Fondo verde para la buscada | **US1** |
| La buscada visible aunque esté en un grupo | **US1** |
| Confianza por turno (mañana, tarde, noche) | **Spec aparte**: redefine el modelo de confianza |
| Zonas objetivo con porcentaje recorrido | **Spec aparte**: es la feature más grande del TODO, y contradice una decisión explícita de la 004 |

**Por qué se separan tres.** Los caminos con diagonales son un defecto del camino ajustado a
calles: se diagnostican comparando contra el camino crudo, y por eso el interruptor entre los dos
viaja con el arreglo. No tocan nada de esta feature. La confianza por turno cambia **qué es** la
confianza —un número por patente pasa a ser tres, o uno que depende de la hora—, y ese cambio
arrastra el borde del pin, la ficha y el orden de la US3; tiene decisiones propias que tomar. Las
zonas objetivo son una entidad nueva, una herramienta de dibujo y una cuenta de cobertura contra
la red de calles; además piden un porcentaje, y la 004 descartó **explícitamente** todo contador
de cobertura (su FR-020a y su Out of Scope). Esa decisión se revisa en su propia spec, no de
costado en esta.

## Clarifications

### Session 2026-09-28

- Q: ¿Dónde vive el filtro del mapa? → A: En la pantalla principal, y la pantalla de búsqueda
  deja de existir. El campo para escribir el número **no está siempre abierto**: *"que el campo de
  texto solo se abra cuando tocás en la lupita para no sobrecargar la home"*. Y va en la barra de
  arriba, que el jugador describió así: *"ya de por sí es bastante inútil esa barra, además si la
  clickeás ni te lleva a la patente ni nada"*.
- Q: ¿Qué pasa con la segunda línea de la barra? → A: Se va. *"Poco me interesan los números
  seguidos cubiertos."* La reemplaza la cuenta de descubiertas.
- Q: Cuando se toca la lupa, ¿el teclado sube de entrada o recién al tocar el campo? → A: Sube de
  entrada. La lupa filtra por el número actual y deja el campo listo para escribir otro, con el
  teclado arriba; para mirar el mapa entero, se baja el teclado y el filtro sigue.
- Q: Al filtrar, ¿el mapa se mueve solo para mostrar las patentes del número? → A: Sí, y encuadra
  también la posición del jugador: se ven todas las del número y dónde está parado respecto de
  ellas.
- Q: ¿Una patente ya compartida cuenta en "descubiertas" y aparece en la lista y en el filtro? →
  A: Sí, todas cuentan. Descubiertas, lista, filtro y fondo verde usan el mismo conjunto que el
  mapa, que ya no distingue compartidas. Los avisos siguen solo para las no compartidas.
- Q: ¿"Últimas capturas" puede desaparecer con la pantalla de búsqueda? → A: Sí, sin reemplazo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver de un vistazo cuántas hay y dónde (Priority: P1)

El juego está en 318. El jugador abre la app y, sin tocar nada, lee arriba "318 · 3
descubiertas". En el mapa, las tres 318 tienen fondo verde y se ven aunque estén en la zona más
cargada, a cualquier zoom: ninguna queda tragada por un grupo.

**Why this priority**: es la pregunta que el jugador trae cada vez que abre la app, y hoy la
respuesta existe pero está escondida: en una cifra que no se entiende y en pines que se pierden
entre los grupos. Sin esto el resto de la feature no tiene punto de partida.

**Independent Test**: con el número actual cargado y varias patentes de ese número, alguna de
ellas rodeada de otras, verificar que la barra dice cuántas son y que todas se ven en el mapa con
fondo verde a cualquier zoom.

**Acceptance Scenarios**:

1. **Given** tres patentes guardadas del número actual, **When** el jugador abre la app,
   **Then** la barra dice "3 descubiertas".
2. **Given** ninguna patente del número actual, **When** el jugador abre la app, **Then** la
   barra dice "Ninguna descubierta".
3. **Given** una sola patente del número actual, **When** el jugador abre la app, **Then** la
   barra dice "1 descubierta".
4. **Given** cualquier situación, **When** el jugador mira la barra, **Then** no aparece ninguna
   cuenta de "números seguidos cubiertos".
5. **Given** una patente del número actual con otras cinco alrededor, **When** el jugador aleja
   el mapa hasta que esas cinco se agrupan, **Then** la del número actual sigue dibujada como pin
   propio, con fondo verde, encima del grupo.
6. **Given** una patente del número actual, **When** se dibuja, **Then** su borde sigue diciendo
   la confianza, igual que el de cualquier otro pin.

---

### User Story 2 - Ver en el mapa solo las de un número (Priority: P1)

El jugador toca la lupa de la barra. El mapa pasa a mostrar solo las del número actual, y se abre
un campo para escribir otro si quiere. Escribe 245 y el mapa queda con las 245, los recorridos y
nada más. Cuando termina, cierra el filtro y vuelve el mapa completo.

**Why this priority**: es el ítem que el jugador escribió primero y el que más cambia el uso del
mapa. Con noventa pines, "¿dónde están las 318?" se contesta leyendo números; con el filtro, se
contesta mirando.

**Independent Test**: con patentes de varios números en el mapa, tocar la lupa y verificar que
quedan solo las del número actual; escribir otro número y verificar que quedan solo las de ese;
cerrar el filtro y verificar que vuelven todas.

**Acceptance Scenarios**:

1. **Given** el mapa completo, **When** el jugador toca la lupa, **Then** el mapa muestra solo
   las patentes del número actual, y se abre el campo con ese número ya escrito, listo para
   reemplazarse y con el teclado arriba.
2. **Given** el filtro recién abierto con el teclado arriba, **When** el jugador baja el
   teclado, **Then** el mapa sigue filtrado y la barra sigue mostrando el número del filtro.
3. **Given** el campo abierto, **When** el jugador escribe 245, **Then** al completar el tercer
   dígito el mapa muestra solo las 245, y la barra dice cuántas 245 hay.
4. **Given** el filtro activo, **When** el jugador mira el mapa, **Then** los recorridos siguen
   dibujados como estaban.
5. **Given** tres 318 repartidas por el barrio, fuera de la parte del mapa que se estaba viendo,
   **When** el jugador filtra por 318, **Then** el mapa se mueve y ajusta el zoom hasta mostrar
   las tres y la posición del jugador a la vez.
6. **Given** el filtro activo, **When** el jugador mira la leyenda del mapa, **Then** dice qué
   número se está mostrando: un mapa filtrado no puede confundirse con un mapa con pocas patentes
   guardadas.
7. **Given** un número del que no hay ninguna patente, **When** el jugador filtra por él,
   **Then** la barra dice "Ninguna descubierta" y el mapa no queda vacío sin explicación.
8. **Given** el filtro activo, **When** el jugador lo cierra, **Then** vuelven todas las patentes
   sin recargar ni esperar, y la barra vuelve al número actual.
9. **Given** el filtro activo, **When** el jugador cierra la app y la vuelve a abrir, **Then** el
   mapa muestra todas las patentes.
10. **Given** la pantalla principal sin filtro, **When** el jugador la mira, **Then** no hay ningún
   campo de filtro abierto: solo la lupa.

---

### User Story 3 - Saber cuál revisar primero (Priority: P2)

El jugador toca la barra y se despliega la lista de las patentes de ese número. Arriba van las que
conviene ir a ver: las más cercanas, pero con la confianza metida en la cuenta. Una patente a
media cuadra se revisa aunque tenga confianza baja, porque cuesta nada pasar; una a diez cuadras
con confianza baja queda detrás de una a doce con confianza alta. Cada fila dice su distancia y su
confianza, y tocarla lleva a esa patente en el mapa.

**Why this priority**: una vez que el jugador sabe cuántas hay y dónde (US1, US2), lo siguiente es
decidir por cuál empezar. Hoy esa lista vive en otra pantalla, sin la confianza y ordenada por
fecha de captura, que no es una pregunta que el jugador se haga en la calle.

**Independent Test**: con varias patentes del mismo número a distintas distancias y con distinta
confianza, tocar la barra y verificar el orden de la lista contra los ejemplos de abajo, que cada
fila muestra su confianza y que tocar una fila lleva a esa patente.

**Acceptance Scenarios**:

1. **Given** patentes del número de la barra, **When** el jugador toca la barra, **Then** se
   despliega la lista de esas patentes, y cada fila muestra distancia, rumbo y confianza.
2. **Given** la lista desplegada, **When** el jugador toca una fila, **Then** el mapa se centra
   en esa patente y se abre su ficha.
3. **Given** dos patentes a 100 m y a 600 m, las dos con la misma confianza, **When** se listan,
   **Then** la de 100 m va primero.
4. **Given** una a 50 m con confianza 0 y otra a 240 m con confianza 10, **When** se listan,
   **Then** va primero la de 50 m: está tan cerca que vale la pena pasar.
5. **Given** una a 300 m con confianza 0 y otra a 500 m con confianza 10, **When** se listan,
   **Then** va primero la de 500 m: la diferencia de confianza compensa la de distancia.
6. **Given** que no hay posición disponible, **When** se despliega la lista, **Then** se ordena
   por confianza, y dice que no está ordenada por distancia.
7. **Given** ninguna patente del número de la barra, **When** el jugador la toca, **Then** no se
   despliega una lista vacía: la barra ya dijo "Ninguna descubierta".

---

### User Story 4 - Armar un recorrido por varias (Priority: P2)

Desde la lista de las tres 318, el jugador arma un recorrido: la app le propone un orden que
empieza por la más cercana a donde está parado, él saca la que está muy lejos, cambia el orden de
las otras dos si prefiere, y lo abre en Google Maps como un solo recorrido con todas las paradas.

**Why this priority**: es el paso que cierra la línea —ver, elegir, ir—, y hoy exige volver a la
app entre parada y parada. Va después de la US3 porque arranca desde su lista y usa la misma
información (distancia y confianza) para decidir qué incluir.

**Independent Test**: con tres patentes de un número, armar un recorrido desde la lista, excluir
una, invertir el orden de las otras dos y verificar que Google Maps abre con esas dos paradas en
ese orden.

**Acceptance Scenarios**:

1. **Given** la lista de tres patentes de un número, **When** el jugador arma un recorrido,
   **Then** las tres aparecen incluidas, en un orden propuesto que arranca por la más cercana a su
   posición y sigue cada vez por la más cercana a la anterior.
2. **Given** el recorrido armado, **When** el jugador saca una parada, **Then** deja de estar en
   el recorrido y el resto conserva su orden.
3. **Given** el recorrido armado, **When** el jugador cambia el orden de dos paradas, **Then** el
   recorrido respeta el orden nuevo, sin tipear nada.
4. **Given** el recorrido listo, **When** el jugador lo abre, **Then** Google Maps muestra un
   solo recorrido que sale de su posición y pasa por todas las paradas en ese orden.
5. **Given** cada parada del recorrido, **When** el jugador la mira antes de abrirlo, **Then** ve
   a qué distancia está y con qué confianza, que es lo que necesita para decidir si la saca.
6. **Given** un recorrido con una sola parada, **When** el jugador lo abre, **Then** se comporta
   igual que "Ir" hacia esa patente.

---

### User Story 5 - Pines que se hacen lugar antes de agruparse (Priority: P3)

En una cuadra con cuatro patentes, hoy el mapa las junta en un grupo apenas se tocan. Con esta
feature, primero se corren un poco para dejarse ver cada una, y solo se agrupan cuando para
mostrarse tendrían que alejarse de su cuadra.

**Why this priority**: mejora la lectura del mapa en toda la app, pero la US1 ya resuelve el caso
que más importa —que la que toca nunca se esconda— y la US2 resuelve el resto cuando se busca un
número puntual. Esto es para el mapa completo.

**Independent Test**: con varias patentes a pocos metros entre sí, verificar que a zoom de calle
se ven como pines separados, y que al alejar el mapa se agrupan recién cuando ya no entran cerca
de su lugar.

**Acceptance Scenarios**:

1. **Given** cuatro patentes en la misma cuadra, **When** el jugador mira el mapa a zoom de
   calle, **Then** se ven cuatro pines separados, cada uno a no más de media cuadra de su lugar
   real.
2. **Given** esas cuatro patentes, **When** el jugador aleja el mapa hasta que no entran sin
   pasarse de media cuadra, **Then** se agrupan como hoy.
3. **Given** un pin corrido de su lugar, **When** el jugador lo toca, **Then** la ficha muestra
   la ubicación real, y "Ir" lleva a la ubicación real.
4. **Given** el mapa quieto en un zoom, **When** el jugador lo desplaza sin acercar ni alejar,
   **Then** ningún pin salta de lugar.

---

### Edge Cases

- **Dos patentes del número actual a pocos metros entre sí.** Las dos se dibujan como pines,
  aunque se encimen. Ninguna se agrupa (FR-002); la regla de la US5 puede separarlas, pero si no
  entran, se encima una sobre la otra antes que esconderse.
- **Filtrar por el número actual.** Es el caso más común, y el que la lupa resuelve de un toque.
  Los pines siguen con fondo verde: el filtro saca a las demás, no le quita el destaque a las que
  quedan.
- **Filtrar por otro número.** Sus pines no van en verde: el verde dice "la que toca", y esa sigue
  siendo la del número actual.
- **Filtrar y salir a Salidas o a Ajustes.** Al volver, el filtro está cerrado: vive en la
  pantalla principal y se va con ella, igual que las patentes escondidas de la 004. Por eso
  cambiar el número actual del juego —que se hace en Ajustes— siempre encuentra el mapa sin
  filtro, y al volver el verde ya está en el número nuevo.
- **Una patente del número filtrado muy lejos** (en otro barrio). El encuadre la incluye igual y
  el mapa se aleja mucho: es la respuesta honesta a "¿dónde están todas?". Para mirar el barrio,
  el jugador acerca el mapa; el filtro sigue.
- **Escribir uno o dos dígitos.** El mapa no cambia hasta el tercero. Un filtro por prefijo
  mostraría decenas de números a la vez, que es casi el mapa completo.
- **Filtro activo y patentes escondidas (botón de la 004) a la vez.** No se ve ningún pin, y la
  leyenda dice las dos cosas. Esconder gana: es la decisión más general.
- **Filtrar mientras se camina con un recorrido en curso.** El trazo sigue creciendo. El filtro es
  sobre las patentes, no sobre los recorridos.
- **Campo de filtro abierto y el jugador quiere cargar una patente.** El campo de carga sigue
  donde está y funciona igual. Los dos campos nunca se confunden: el de filtro vive en la barra de
  arriba y solo existe mientras el jugador lo abrió.
- **Cargar una patente con el filtro activo.** Se guarda igual y el aviso lo confirma. Si es de
  otro número, no aparece en el mapa filtrado, y el aviso alcanza para saber que se guardó.
- **Armar un recorrido sin posición disponible.** No hay de dónde calcular el orden propuesto ni
  punto de partida. Se arma igual, en el orden de la lista, y Google Maps parte de donde él
  resuelva la posición; la pantalla dice que el orden no está calculado desde donde está parado.
- **Armar un recorrido con más paradas de las que Google Maps acepta.** Se avisa cuántas entran y
  se pide sacar las que sobran. No se abre un recorrido cortado en silencio.
- **Google Maps no está instalado.** El recorrido se abre en el navegador con las mismas paradas.
  Si tampoco se puede, se avisa, igual que el FR-001a de la 003 con "Ir".
- **Excluir todas las paradas.** No hay recorrido que abrir; la acción queda deshabilitada.
- **Una patente con confianza 0 muy lejos.** Queda al final de la lista (US3) y, en el recorrido,
  es la candidata obvia a sacar, pero no se saca sola: decide el jugador.
- **Patentes a la misma distancia y con la misma confianza.** Va primero la capturada más
  recientemente.
- **Una cuadra con veinte patentes.** No hay forma de separarlas sin pasarse de media cuadra; se
  agrupan como hoy. La US5 mejora el caso de pocas patentes cercanas, no hace magia con muchas.
- **Número actual sin cargar todavía** (primera apertura). No hay "la que toca": la barra dice lo
  mismo que hoy en ese caso, ningún pin va en verde, y la lupa abre el campo vacío.

## Requirements *(mandatory)*

### La que toca, siempre a la vista

- **FR-001**: El pin de cada patente del número actual del juego MUST dibujarse con **fondo
  verde**, y MUST conservar el tamaño mayor que le dio la 004. **Esto revierte en parte el FR-030d
  de la 004**, que pedía destacarla "sin color": en la calle, el tamaño solo no alcanzó, y el verde
  es lo que el jugador reconoce como "la que toca".
- **FR-001a**: El borde de ese pin MUST seguir diciendo la confianza (FR-030 de la 004). El fondo
  verde se suma; no reemplaza al borde. Los tres colores de borde MUST distinguirse sobre el fondo
  verde.
- **FR-002**: Las patentes del número actual MUST NOT quedar dentro de ningún grupo, a ningún
  zoom. Siempre se dibujan como pin propio, encima de los grupos y de los otros pines.
- **FR-002a**: La cuenta de un grupo MUST NOT incluir las patentes del número actual, porque no
  están adentro.

### La barra de arriba

- **FR-003**: La barra de arriba de la pantalla principal MUST decir qué número está mirando —el
  actual del juego, o el del filtro si hay uno— y cuántas patentes de ese número hay guardadas:
  "Ninguna descubierta", "1 descubierta" o "N descubiertas".
- **FR-003a**: La barra MUST NOT mostrar la cuenta de números seguidos cubiertos. **Esto retira
  de la pantalla el FR-022 de la 001.**
- **FR-003b**: La cuenta MUST incluir **todas** las patentes guardadas del número, compartidas o
  no. El mismo conjunto MUST usarse para la lista (FR-004), el filtro (FR-006) y el fondo verde
  (FR-001): es el que el mapa ya dibuja sin distinguir compartidas (FR-030a de la 004), y la barra
  no puede contar algo distinto de lo que se ve.
- **FR-003c**: Los avisos de proximidad MUST seguir como están: solo para las no compartidas
  (FR-030b de la 004). Esta feature cambia qué se cuenta y se muestra, no a qué se avisa.
- **FR-004**: Tocar la barra MUST desplegar la lista de las patentes del número que está mirando
  (US3). Hoy tocarla no hace nada.

### Filtrar el mapa por número

- **FR-005**: La barra MUST tener una **lupa**. Tocarla MUST filtrar el mapa por el número actual
  y abrir un campo para escribir otro número, con el actual ya escrito, listo para reemplazarse y
  **con el teclado arriba**.
- **FR-005c**: Bajar el teclado MUST NOT cerrar el filtro: el mapa sigue filtrado, y el campo
  vuelve a subir el teclado si se lo toca. Cerrar el filtro es una acción aparte (FR-008).
- **FR-005a**: El campo de filtro MUST existir solo mientras el jugador lo abrió. Sin filtro, la
  pantalla principal muestra la lupa y nada más: la pantalla no se recarga con un segundo campo
  numérico que casi nunca se usa.
- **FR-005b**: El filtro MUST aplicarse al completar el tercer dígito. Con menos dígitos el mapa
  no cambia.
- **FR-006**: Con el filtro activo, el mapa MUST dejar de dibujar las patentes de otros números, y
  MUST NOT cambiar nada más: los recorridos, su modo y el botón de esconder patentes siguen como
  estaban.
- **FR-006a**: Al aplicarse el filtro, el mapa MUST moverse y ajustar el zoom para mostrar a la
  vez **todas** las patentes del número y la **posición del jugador**. Sin posición disponible,
  encuadra solo las patentes; sin ninguna patente del número, no se mueve.
- **FR-006b**: Ese encuadre MUST dejar de seguir al jugador, igual que cuando él arrastra el mapa:
  si el mapa volviera a centrarse en su posición al siguiente paso, deshacería el encuadre. Volver
  a seguirlo es el mismo botón de recentrar de siempre.
- **FR-006c**: Cerrar el filtro MUST NOT mover el mapa: queda donde está.
- **FR-007**: Mientras el filtro está activo, la leyenda del mapa MUST decir por escrito qué número
  se está mostrando, igual que dice cuándo las patentes están escondidas (FR-035 de la 004).
- **FR-008**: Cerrar el filtro MUST ser una sola acción, y las patentes MUST volver sin recargar el
  mapa. La barra vuelve al número actual.
- **FR-009**: El filtro MUST NOT recordarse entre aperturas de la app, por la misma razón que el
  FR-035a de la 004: un arranque con menos pines de los que hay se confunde con una pérdida de
  datos. Tampoco sobrevive a ir a otra pantalla de la app y volver.
- **FR-010**: El filtro MUST NOT afectar lo que se guarda ni lo que se avisa. Es un filtro del
  dibujo, no de los datos.

### La lista: confianza y orden

- **FR-011**: Cada fila de la lista MUST mostrar la distancia y el rumbo desde la posición actual
  (FR-003 de la 003) y la confianza, en la misma escala que la ficha ("N de 10").
- **FR-011a**: Tocar una fila MUST centrar el mapa en esa patente y abrir su ficha.
- **FR-012**: La lista MUST ordenarse combinando **distancia a la posición actual** y
  **confianza**: más cerca va antes, y más confianza va antes, con un peso que hace que una patente
  muy cercana se revise primero aunque tenga confianza baja. Los escenarios 3, 4 y 5 de la US3 son
  la referencia de cómo se comporta.
- **FR-012a**: El peso de la confianza frente a la distancia MUST ser calibrable en un solo lugar.
- **FR-012b**: Sin posición disponible, la lista MUST ordenarse por confianza y MUST decir que no
  está ordenada por distancia.
- **FR-012c**: Con distancia y confianza iguales, MUST ir primero la capturada más recientemente.

### La pantalla de búsqueda se retira

Con el filtro y la lista en la pantalla principal, la pantalla de búsqueda queda sin trabajo
propio. Lo que hacía se reparte así:

| Lo que hacía la pantalla de búsqueda | Dónde queda |
|---|---|
| Buscar las patentes de un número | La lupa de la barra (FR-005) |
| Listarlas con distancia y rumbo | La lista de la barra (FR-004, FR-011) |
| "Tenés la N" / "Te falta la N" | La barra, con la cuenta de descubiertas (FR-003) |
| Ir, Compartir, Borrar | Ya estaban en la ficha, a un toque de cada fila (FR-011a) |
| Agregar foto a una patente guardada con `+` | Pasa a la ficha (FR-013a) |
| Últimas capturas | Se retira (FR-013b) |

- **FR-013**: La pantalla de búsqueda y su botón MUST retirarse. **Esto retira los FR-011 a
  FR-014 de la 003**, cuyo trabajo pasa a la barra de la pantalla principal.
- **FR-013a**: La ficha de una patente sin foto MUST ofrecer agregarle una, con las mismas
  garantías que hoy tiene la pantalla de búsqueda: se toca solo la foto, nunca la ubicación ni el
  momento (FR-019 de la 001, Principio II). Es la única acción de la pantalla de búsqueda que la
  ficha no tenía.
- **FR-013b**: La lista de "últimas capturas" MUST retirarse sin reemplazo. Contestaba "¿qué anoté
  hace poco?", y eso lo contesta el mapa y el detalle de cada salida (FR-021 de la 004).
- **FR-013c**: Ningún texto de la app MUST seguir mandando al jugador a la pantalla de búsqueda.

### Armar un recorrido por varias

- **FR-014**: Desde la lista de la barra, el jugador MUST poder armar un recorrido que pase por
  varias de esas patentes.
- **FR-015**: Al armarlo, todas las patentes de la lista MUST aparecer incluidas, en un orden
  propuesto que arranca por la más cercana a la posición del jugador y sigue cada vez por la más
  cercana a la parada anterior.
- **FR-016**: El jugador MUST poder sacar cualquier parada y volver a ponerla.
- **FR-017**: El jugador MUST poder cambiar el orden de las paradas con una mano y sin tipear.
- **FR-018**: Cada parada MUST mostrar su distancia a la posición actual y su confianza.
- **FR-019**: Abrir el recorrido MUST mandarlo a **Google Maps** como un solo recorrido, saliendo
  de la posición del jugador y pasando por todas las paradas incluidas en el orden elegido. **Esto
  se aparta de la decisión de la 003** de entregar el destino a cualquier app de mapas: la forma
  estándar de abrir un lugar no admite varias paradas, y el pedido nombra Google Maps.
- **FR-019a**: Si Google Maps no está instalado, el recorrido MUST abrirse en el navegador con las
  mismas paradas; si tampoco se puede, MUST avisarse.
- **FR-019b**: Si hay más paradas incluidas de las que Google Maps acepta en un recorrido, MUST
  avisarse cuántas entran, y MUST NOT abrirse un recorrido cortado en silencio.
- **FR-020**: Un recorrido con una sola parada MUST comportarse igual que "Ir" (FR-001 de la 003).
- **FR-021**: Armar o abrir un recorrido MUST NOT iniciar una salida ni el registro del trayecto.
  Planear a dónde ir y grabar por dónde se fue son cosas separadas.
- **FR-022**: El recorrido armado MUST NOT guardarse. Es de un solo uso: la próxima vez las
  patentes, la posición y la confianza pueden ser otras.

### Pines que se hacen lugar

- **FR-023**: Cuando dos o más pines se encimarían, el mapa MUST intentar primero separarlos,
  dibujando cada uno corrido de su lugar real **como mucho media cuadra** en el terreno. Solo si no
  entran sin pasarse de ese límite, se agrupan.
- **FR-023a**: El límite MUST ser calibrable en un solo lugar.
- **FR-023b**: Correr un pin MUST ser solo del dibujo. La ficha, la distancia, el rumbo, "Ir" y el
  recorrido MUST usar siempre la ubicación real (Principio II).
- **FR-023c**: Desplazar el mapa sin cambiar el zoom MUST NOT cambiar la posición de ningún pin
  respecto del mapa.
- **FR-023d**: Las patentes del número actual MAY correrse como cualquier otra, pero MUST NOT
  agruparse (FR-002).

### La confianza arranca en cero

Se decidió y se implementó el 2026-09-28, antes de esta spec. Se registra acá porque la US3 y la
US4 ordenan por confianza, y porque cambia un caso borde que la 004 dejó escrito.

- **FR-024**: Toda patente nueva MUST arrancar con confianza **0**. Volver a anotarla a diez metros
  o menos sube su confianza en uno (FR-036 y FR-037 de la 004). La motivación: bajar la confianza
  exige demostrar que un auto **no** está, que es difícil e incómodo; subirla es anotar la
  patente otra vez.
- **FR-024a**: Al actualizar, las patentes que tenían confianza 5 o menos MUST quedar en 0, y las
  que tenían más de 5 MUST conservar su valor.
- **FR-024b**: Los cortes de color del borde MUST quedar como están (0 a 3, 4 a 7, 8 a 10). Una
  patente recién anotada se ve con el borde bajo. **Esto reemplaza el caso borde de la 004** que
  decía que una patente sin votos caía en el escalón medio.

### Lo que no cambia

- **FR-025**: Esta feature MUST NOT agregar pasos, demoras ni pantallas al camino de carga
  rápida, que sigue en 4 interacciones (Principio I). El campo de filtro no está en ese camino:
  vive en la barra de arriba y solo existe si el jugador lo abre.
- **FR-026**: Esta feature MUST NOT modificar ningún registro, voto ni punto de trayecto, salvo
  adjuntar una foto a un registro que no tenía (FR-013a), que ya estaba permitido. Cambia cómo se
  ven, se ordenan y se usan los datos, no los datos (Principio II).
- **FR-027**: Todo MUST funcionar sin conexión, salvo abrir el recorrido en Google Maps, que es la
  app externa la que lo necesita (Principio III).

### Key Entities

- **Número actual del juego**: ya existe. Esta feature lo usa para cuatro cosas nuevas: el fondo
  verde, la exclusión de los grupos, lo que la barra muestra sin filtro y el número con que abre
  la lupa.
- **Filtro del mapa**: qué número está mostrando el mapa, o ninguno. Es estado de pantalla y **no**
  se persiste.
- **Recorrido planeado**: una lista ordenada de paradas, cada una una patente guardada, con una
  marca de incluida o no. Vive mientras el jugador lo arma y **no** se guarda. No confundir con la
  **salida** (recorrido grabado) de la 003 y la 004, que es lo que efectivamente se caminó.
- **Prioridad de revisión**: lectura derivada de distancia y confianza que ordena la lista. No se
  guarda: cambia con cada paso del jugador.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con el número actual cargado y más de ochenta patentes en el mapa, el jugador señala
  todas las del número actual en menos de 3 segundos, a cualquier zoom y sin tocar ningún grupo.
- **SC-002**: El jugador lee cuántas patentes tiene del número actual sin tocar nada y sin
  necesitar explicación.
- **SC-003**: Filtrar el mapa por el número actual toma **un toque**, y verlo sin el teclado
  encima, uno más; filtrar por otro número toma un toque más los tres dígitos. Volver al mapa
  completo toma un toque.
- **SC-004**: Desde la pantalla principal, el jugador llega a la ficha de cualquier patente del
  número que está mirando en dos toques: la barra y la fila.
- **SC-005**: Con posición disponible, ninguna patente aparece en la lista después de otra que esté
  a más del doble de distancia y tenga igual o menor confianza.
- **SC-006**: Armar un recorrido por tres patentes y abrirlo en Google Maps toma menos de 30
  segundos si el orden propuesto sirve, y menos de un minuto si hay que sacar una y reordenar.
- **SC-007**: En la zona con más patentes, a zoom de calle, hay menos grupos que antes de esta
  feature, y ningún pin se dibuja a más de media cuadra de su lugar real.
- **SC-008**: La carga rápida sigue entrando en 4 interacciones.

## Assumptions

- **"La patente buscada" del TODO es la del número actual del juego.** Es la que el jugador sale a
  buscar, y es la única que va en verde. Si filtra por otro número, el filtro ya deja solo las de
  ese número y no hace falta un segundo destaque.
- **El filtro es por número completo, no por prefijo.** El pedido es "solo las 318". La búsqueda
  por prefijo de la 003 tenía sentido en una lista; en el mapa, "31" mostraría diez números a la
  vez.
- **El peso de la confianza se calibra en la calle.** Los escenarios de la US3 fijan el
  comportamiento esperado en tres casos; el valor exacto sale de usarlo. Con la confianza
  arrancando en 0 (FR-024), hoy casi todas las patentes empatan en confianza, así que al principio
  el orden va a ser casi solo por distancia.
- **El orden propuesto del recorrido es "la más cercana cada vez", no el óptimo.** Con las pocas
  paradas que tiene un número, la diferencia con el recorrido más corto posible es chica, y el
  jugador lo corrige a mano si no le sirve.
- **Media cuadra son unos 50 metros.** Es el mismo largo que la app ya usa como umbral de precisión
  degradada, y alcanza para que un pin corrido se siga leyendo sobre la cuadra correcta.
- **Un pin corrido no necesita una línea que lo una a su lugar real.** El jugador dijo que con que
  quede en la misma cuadra alcanza.
- **"Últimas capturas" se retira sin reemplazo, confirmado por el jugador.** Era lo único de la
  pantalla de búsqueda sin otro lugar adonde mudarse; las patentes anotadas fuera de una salida
  quedan visibles solo en el mapa. Si en la calle hace falta, vuelve como feature propia.
- **Google Maps es la app de mapas del jugador.** Lo nombró él. El navegador queda como respaldo.
- **Un usuario, un teléfono** (Principio IV). Nada de esto se comparte ni se sincroniza.

## Out of Scope

- **Diagonales en los caminos ajustados y el interruptor crudo/ajustado.** Spec aparte (ver
  Contexto).
- **Confianza por turno o por día.** Spec aparte. Esta feature usa la confianza como es hoy: un
  número por patente.
- **Zonas objetivo y porcentaje de calles recorridas.** Spec aparte, que además tiene que revisar el
  FR-020a de la 004.
- **Rutear dentro de la app.** La app arma la lista de paradas y se la entrega a Google Maps; el
  ruteo y la navegación los sigue haciendo él (D2 de la 003).
- **Guardar recorridos planeados** o reusarlos. Nadie lo pidió.
- **Filtrar por más de un número a la vez**, por rangos o por prefijo.
- **Filtrar la lista de salidas o el mapa del detalle de una salida.** El filtro es del mapa
  principal.
