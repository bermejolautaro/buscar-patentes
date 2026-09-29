# Feature Specification: Pulido de interfaz

**Feature Branch**: `002-pulido-de-interfaz`

**Created**: 2026-08-28

**Status**: Draft

**Input**: User description: "La feature base estan. Ahora necesito una fase de pulido con varias cosas para mejorar: Lo de que se abra el teclado apenas inicias la app creo que no suma. Que aparezca minimizado. La UI deberia ser mas comoda y moderna. Toma inspiracion de apps como google maps, el icono de la camara es horrible y todo se ve muy pobre. Ni hablar de las pantalla de ajustes, salida o buscar que por alguna razon se ven en modo oscuro todo roto y encima el boton de Volver esta tan arriba que ni se ve y esta casi tapado por la barra de notificaciones. La app deberia acercar el mapa a mi ubicacion tal cual como lo hace google maps. Deberia ser capaz de clickear en las patentes subidas, ver su info y poder editarlas o borrarlas. Ademas ya a simple vista en vez de un punto estaria bueno que sea como un globito con el numero adentro, nunca van a ser mas de 3 digitos asi que es manejable."

## Contexto

La especificación [001](../001-captura-patentes/spec.md) construyó el producto: capturar
patentes con evidencia automática, encontrarlas, compartirlas, recibir avisos y grabar
recorridos. Todo eso funciona y está validado en dispositivo.

Esta especificación **no agrega producto**: hace que el que hay se pueda usar sin pelearse
con él. El jugador ya no está evaluando si la app sirve; la está usando y tropezando con
cosas concretas. Dos de esas cosas están rotas, no son cuestión de gusto.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Las pantallas de adentro se leen y se pueden dejar (Priority: P1)

El jugador entra a Buscar, a Salidas o a Ajustes. Hoy encuentra dos problemas que hacen
que esas pantallas se sientan a medio terminar: con el teléfono en modo oscuro el texto y
el fondo quedan en combinaciones ilegibles, y el botón "Volver" está tan pegado al borde
superior que la barra de estado del sistema lo tapa casi por completo. Salir de la
pantalla se vuelve adivinanza.

Después de esta historia, cualquiera de las tres pantallas se lee igual de bien en modo
claro y en modo oscuro, y el control para volver está donde la mano lo espera y no debajo
del reloj.

**Why this priority**: es lo único de esta especificación que está **roto**, no
mejorable. Una pantalla ilegible o de la que no se puede salir es peor que no tenerla. Y
afecta a tres de las cinco pantallas de la app.

**Independent Test**: poner el teléfono en modo oscuro, entrar a Buscar, a Salidas y a
Ajustes, y verificar que todo el texto se lee y que el control de volver está completo y
alcanzable con el pulgar. Repetir en modo claro. No hace falta ninguna otra historia de
esta especificación para probarlo.

**Acceptance Scenarios**:

1. **Given** el teléfono en modo oscuro, **When** el jugador abre Buscar, Salidas o
   Ajustes, **Then** todo el texto tiene contraste suficiente contra su fondo y ningún
   elemento queda invisible.
2. **Given** el teléfono en modo claro, **When** el jugador abre las mismas tres
   pantallas, **Then** se ven correctamente y de forma coherente con el modo oscuro.
3. **Given** cualquiera de las pantallas secundarias abierta, **When** el jugador mira la
   parte superior, **Then** el control para volver se ve completo, sin quedar debajo de la
   barra de estado ni de la muesca de la cámara.
4. **Given** cualquiera de las pantallas secundarias abierta, **When** el jugador quiere
   salir, **Then** puede hacerlo con el gesto o el control que el sistema ya usa en el
   resto del teléfono, sin buscarlo.
5. **Given** el teléfono cambiado de modo claro a oscuro con la app abierta, **When** el
   jugador vuelve a la app, **Then** la app acompaña el cambio sin quedar a medio camino.

---

### User Story 2 - El mapa abre donde estoy parado (Priority: P2)

El jugador abre la app en la vereda. Hoy el mapa aparece en vista mundial y él tiene que
arrastrar y hacer zoom hasta encontrar su barrio, o simplemente ignora el mapa. Lo que
espera —porque es lo que hace cualquier app de mapas que usa— es que el mapa ya esté
centrado en él, a una altura donde se distinguen las cuadras, y que si se aleja mirando
otra zona haya una forma de un toque para volver a su posición.

**Why this priority**: el mapa es el fondo permanente de la pantalla principal. Si no
muestra dónde está el jugador, ocupa toda la pantalla sin aportar nada, y las patentes
guardadas quedan como puntos sobre un planeta.

**Independent Test**: abrir la app en la calle y verificar que el mapa queda centrado en
la posición del jugador a altura de cuadra, sin tocar nada. Después arrastrar el mapa a
otra zona y verificar que un solo control lo devuelve a la posición actual.

**Acceptance Scenarios**:

1. **Given** el permiso de ubicación otorgado, **When** el jugador abre la app, **Then**
   el mapa queda centrado en su posición actual a una altura donde se distinguen las
   calles, sin que él arrastre ni haga zoom.
2. **Given** el mapa centrado en el jugador, **When** el jugador se mueve, **Then** el
   mapa acompaña su posición.
3. **Given** el jugador que arrastró el mapa a otra zona, **When** toca el control de
   volver a su ubicación, **Then** el mapa regresa a su posición actual.
4. **Given** el permiso de ubicación denegado, **When** el jugador abre la app, **Then**
   el mapa no queda en vista mundial sin explicación: muestra la zona de sus registros
   guardados, o dice por qué no puede centrarse.
5. **Given** que el sistema todavía no entregó ninguna posición, **When** el mapa se
   dibuja, **Then** no bloquea ni demora la captura de una patente.

---

### User Story 3 - Tocar una patente del mapa y hacer algo con ella (Priority: P3)

El jugador ve sus patentes sobre el mapa. Hoy son puntos de color: sabe que hay algo
guardado ahí, pero no cuál. Quiere ver el número de un vistazo, sin tocar nada, y quiere
poder tocar una para ver su ficha —número, cuándo, con qué precisión, si tiene foto, si ya
la compartió— y desde ahí corregirla o borrarla.

**Why this priority**: convierte el mapa de decoración en la forma principal de navegar el
archivo. Hoy para llegar a un registro hay que ir a Buscar y escribir el número, aunque el
registro esté a la vista en la pantalla.

**Independent Test**: guardar tres patentes con números distintos, mirar el mapa y
verificar que se lee el número de cada una sin tocarlas. Tocar una, ver su ficha completa,
corregir lo que la especificación permita corregir y borrar otra, verificando que
desaparece del mapa.

**Acceptance Scenarios**:

1. **Given** varias patentes guardadas, **When** el jugador mira el mapa, **Then** cada
   una se ve como un marcador con su número de 3 dígitos legible adentro, no como un punto
   sin identificar.
2. **Given** los marcadores con número, **When** el jugador los mira, **Then** sigue
   distinguiendo a simple vista los pendientes de los compartidos, y los que ya tocan por
   número de los que todavía no.
3. **Given** una patente en el mapa, **When** el jugador la toca, **Then** ve su
   información: número, momento de la captura, precisión, si tiene foto y si ya fue
   compartida.
4. **Given** la ficha de una patente abierta, **When** el jugador elige borrarla,
   **Then** se le pide confirmación y, al confirmar, el registro y su foto desaparecen y
   el marcador se va del mapa.
5. **Given** la ficha de una patente abierta, **When** el jugador elige corregirla,
   **Then** puede cambiar el número y el texto de la patente, y la ubicación, la precisión
   y el timestamp quedan exactamente como estaban.
6. **Given** varios marcadores muy juntos en el mapa, **When** el jugador mira esa zona,
   **Then** puede distinguirlos o separarlos, en lugar de ver un amontonamiento ilegible.

---

### User Story 4 - El teclado aparece cuando lo pido (Priority: P4)

El jugador abre la app y hoy el teclado numérico ocupa media pantalla desde el primer
instante, tape lo que tape. Muchas veces la abre para mirar el mapa, no para cargar: para
ver dónde tiene patentes cerca, o qué calles ya recorrió. En esos casos el teclado es un
estorbo que hay que bajar antes de poder ver nada.

Quiere que la app abra con el mapa a la vista y el teclado guardado, y que aparezca cuando
efectivamente vaya a escribir.

**Why this priority**: es una molestia real y frecuente, pero cambia el camino de captura,
que es lo que la constitución protege con más fuerza. Va después de lo que está roto y de
lo que no toca ningún principio.

**Independent Test**: abrir la app y verificar que el mapa se ve completo, sin teclado
encima. Después contar cuántas interacciones hacen falta para guardar una patente y
comparar contra el presupuesto que fije la resolución de la pregunta abierta.

**Acceptance Scenarios**:

1. **Given** la app cerrada, **When** el jugador la abre, **Then** el mapa se ve sin que
   el teclado lo tape.
2. **Given** la app recién abierta, **When** el jugador va a escribir un número,
   **Then** el teclado numérico aparece y el campo recibe lo que tipea.
3. **Given** el teclado abierto, **When** el jugador termina de cargar, **Then** la app
   vuelve a un estado donde el mapa se ve completo.
4. **Given** el cambio de comportamiento, **When** se mide la carga rápida completa desde
   la app cerrada, **Then** requiere 4 interacciones o menos: abrir, tocar el campo,
   tipear el número y confirmar.

---

### User Story 5 - La pantalla principal se ve como una app terminada (Priority: P5)

El jugador mira la pantalla principal y le parece pobre. El botón de la cámara es un
emoji, los controles no se distinguen del fondo del mapa, y el conjunto no transmite que
sea una herramienta terminada. Quiere algo que se parezca a las apps de mapas que usa
todos los días: controles con forma reconocible, iconografía real y una jerarquía visual
clara entre lo principal —cargar una patente— y lo secundario.

Y quiere poder usarla con el pulgar de una mano. Hoy los tres controles de navegación
están arriba de todo, que en un teléfono alto es donde el pulgar no llega: para ir a
Buscar hay que recolocar el teléfono o usar la otra mano. El campo de número, además,
ocupa mucho más espacio del que tres dígitos necesitan, y ese espacio se lo saca al mapa.

Esto no es un gusto nuevo: el Principio I de la constitución dice que la app se usa
**"parado en la vereda, con una mano"**. El diseño actual lo incumple, y esta historia es
donde se corrige.

**Why this priority**: es lo más subjetivo de la especificación y lo único que no arregla
un problema concreto de uso. Va última a propósito: si hay que recortar, se recorta acá.

**Independent Test**: mostrarle la pantalla principal a alguien que no conoce la app y
verificar que identifica sin ayuda cuál es el botón de cargar, cuál el de la foto y cuál
el de empezar un recorrido.

**Acceptance Scenarios**:

1. **Given** la pantalla principal, **When** el jugador mira la fila de acciones,
   **Then** cada botón usa un icono real y reconocible, no un emoji.
2. **Given** el mapa de fondo con cualquier contenido debajo, **When** el jugador mira los
   controles, **Then** todos se distinguen del fondo sin importar si abajo hay calles,
   agua o zona sin fondo guardado.
3. **Given** la pantalla principal, **When** alguien que nunca usó la app la mira,
   **Then** identifica cuál es la acción principal sin que se la expliquen.
4. **Given** el indicador del estado del juego, **When** el jugador lo mira, **Then**
   sigue respondiendo "¿tengo el número actual?" de un vistazo, sin perder claridad
   respecto de lo que ya hacía.
5. **Given** el jugador sosteniendo el teléfono con una sola mano, **When** quiere hacer
   cualquiera de las acciones principales —cargar, sacar la foto, empezar un recorrido,
   ir a buscar, ir a ajustes—, **Then** llega con el pulgar sin recolocar el teléfono.
6. **Given** la pantalla principal, **When** el jugador la mira, **Then** el campo de
   número ocupa lo que necesita para escribir tres dígitos y no más, dejando el resto de
   la pantalla al mapa.

---

### Edge Cases

- ¿Qué pasa con el marcador de una patente cuyo número tiene menos de 3 dígitos? ¿Se
  muestra con ceros adelante o tal como se guardó?
- ¿Qué pasa cuando hay muchas patentes guardadas en pocas cuadras y los marcadores se
  superponen?
- ¿Qué pasa si el jugador toca el mapa en un punto donde hay dos marcadores encimados?
- ¿Qué pasa con la ficha de una patente abierta si el jugador borra ese registro desde
  otra pantalla?
- ¿Qué pasa con el mapa centrado en el jugador cuando la señal de ubicación se pierde?
- ¿Qué pasa si el jugador arrastra el mapa mientras la app lo está siguiendo? ¿Deja de
  seguirlo, o pelea contra el dedo?
- ¿Qué pasa con la corrección de un número si el registro ya fue compartido al grupo?
- ¿Qué pasa si el jugador corrige el número de una patente y con eso cambia si ya toca o
  no por el contador del juego?
- ¿Qué pasa con el teclado si el jugador vuelve a la pantalla principal desde Buscar o
  Ajustes: aparece o queda guardado?

## Requirements *(mandatory)*

### Functional Requirements

#### Legibilidad y navegación de las pantallas secundarias

- **FR-001**: Todas las pantallas MUST ser legibles en modo claro y en modo oscuro, con
  contraste suficiente entre texto y fondo en ambos.
- **FR-002**: El sistema MUST acompañar el modo claro u oscuro que el jugador tenga
  configurado en el teléfono, sin exigirle configurar nada dentro de la app.
- **FR-003**: Ninguna pantalla MUST dejar contenido o controles debajo de la barra de
  estado, de la barra de navegación ni de la muesca de la cámara.
- **FR-004**: Cada pantalla secundaria MUST ofrecer una forma visible y alcanzable de
  volver a la pantalla principal, y MUST responder también al gesto de retroceso del
  sistema.

#### El mapa y la ubicación

- **FR-005**: Al abrir la app con permiso de ubicación otorgado, el mapa MUST quedar
  centrado en la posición actual del jugador, a una altura donde se distingan las calles.
- **FR-006**: El mapa MUST seguir la posición del jugador mientras se mueve, y MUST dejar
  de seguirlo cuando el jugador arrastra el mapa a mano.
- **FR-007**: El sistema MUST ofrecer un control de un solo toque para volver a centrar el
  mapa en la posición actual.
- **FR-008**: Sin permiso de ubicación o sin lectura disponible, el mapa MUST NOT quedar
  en vista mundial sin explicación: MUST encuadrar los registros guardados o indicar por
  qué no puede centrarse.
- **FR-009**: El comportamiento del mapa MUST NOT bloquear ni demorar la captura, en línea
  con el FR-038 de la especificación 001.

#### Marcadores y ficha de una patente

- **FR-010**: Cada registro guardado MUST mostrarse en el mapa como un marcador que
  contiene su número visible, en lugar de un punto sin identificar.
- **FR-011**: Los marcadores MUST seguir distinguiendo visualmente los registros
  pendientes de los compartidos, y los que ya tocan por número de los que todavía no,
  conservando lo que fija el FR-035 de la especificación 001.
- **FR-012**: Los usuarios MUST poder tocar un marcador y ver la información del registro:
  número, momento de la captura, precisión, si tiene foto y su estado de uso.
- **FR-013**: Desde la información de un registro, los usuarios MUST poder borrarlo, con
  confirmación previa, y MUST desaparecer del mapa al confirmar.
- **FR-014**: Desde la información de un registro, los usuarios MUST poder corregir su
  número. El sistema MUST NOT permitir editar ubicación, precisión ni timestamp, según el
  Principio II de la constitución.
- **FR-015**: Al corregir el número de un registro, el sistema MUST recalcular qué tiene
  cubierto el jugador y qué avisos corresponden, igual que si el registro se hubiera
  guardado con ese número.
- **FR-016**: Cuando varios marcadores caen muy juntos, el sistema MUST permitir
  distinguirlos, en lugar de superponerlos de forma ilegible.

#### Teclado y camino de captura

- **FR-017**: La app MUST abrir con el mapa visible y el teclado numérico guardado.
- **FR-018**: El teclado numérico MUST aparecer cuando el jugador indica que va a escribir
  un número, y el campo MUST recibir lo que tipea.
- **FR-019**: La carga rápida completa, desde la app cerrada hasta el registro guardado,
  MUST entrar en 4 interacciones o menos: abrir, tocar el campo, tipear el número y
  confirmar.
- **FR-020**: Este cambio MUST reemplazar explícitamente al FR-016 de la especificación
  001, que exige el teclado visible al abrir. La especificación 001 MUST quedar anotada
  como superada en ese punto.
- **FR-021**: El Principio I de la constitución MUST quedar enmendado a 4 interacciones
  antes de que se implemente este cambio. La Governance de la constitución lo exige: si la
  constitución es la que está mal, se enmienda primero y después se avanza.

#### Aspecto de la pantalla principal

- **FR-022**: Los botones de acción MUST usar iconografía real y reconocible, y MUST NOT
  depender de emojis.
- **FR-023**: Los controles superpuestos al mapa MUST distinguirse del fondo cualquiera
  sea el contenido del mapa debajo.
- **FR-024**: La pantalla principal MUST mantener una jerarquía visual donde la acción de
  cargar una patente es la más prominente.
- **FR-025**: El indicador del estado del juego MUST seguir respondiendo si el número
  actual está cubierto y cuántos consecutivos hay, sin perder legibilidad respecto de lo
  que ya hacía.
- **FR-026**: Todas las acciones principales de la pantalla principal MUST quedar al
  alcance del pulgar sosteniendo el teléfono con una sola mano, sin recolocarlo. Esto
  incluye los controles de navegación a Buscar, Salidas y Ajustes, que hoy están en el
  borde superior.
- **FR-027**: El campo de número MUST ocupar solo el espacio que necesita para tres
  dígitos, dejando el resto de la pantalla al mapa.

### Key Entities

Esta especificación no crea entidades nuevas. Trabaja sobre las que definió la
especificación 001: **Registro de captura**, **Estado del juego**, **Recorrido** y **Punto
de trayecto**.

La única modificación de datos que introduce es la corrección del número de un registro
existente (FR-014), que toca exclusivamente el campo del número y el texto de la patente,
y nunca los campos de evidencia.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% del texto de todas las pantallas es legible en modo oscuro y en modo
  claro, verificable revisando cada pantalla en los dos modos.
- **SC-002**: Ningún control queda parcial o totalmente tapado por elementos del sistema,
  en ninguna pantalla y en ninguna orientación.
- **SC-003**: Al abrir la app en la calle, el jugador ve su posición en el mapa sin tocar
  nada, en el 100% de las aperturas con permiso otorgado y ubicación disponible.
- **SC-004**: El jugador vuelve a centrar el mapa en su posición con un solo toque desde
  cualquier zona a la que lo haya arrastrado.
- **SC-005**: El jugador identifica el número de una patente guardada mirando el mapa, sin
  tocar el marcador ni abrir otra pantalla.
- **SC-006**: El jugador llega a la información de un registro que ve en el mapa en 1
  interacción, contra las 3 que hoy exige pasar por la pantalla de búsqueda.
- **SC-007**: Ningún registro cambia su ubicación, precisión ni timestamp como
  consecuencia de esta especificación, verificable comparando cada registro antes y
  después de usar la corrección.
- **SC-008**: Al abrir la app, el mapa se ve completo sin que el jugador tenga que bajar
  el teclado.
- **SC-009**: La carga rápida sigue completándose en 10 segundos o menos desde la app
  cerrada, sin regresión respecto del SC-001 de la especificación 001, y en 4
  interacciones o menos.
- **SC-010**: Una persona que nunca usó la app identifica la acción principal de la
  pantalla principal sin que se la expliquen.
- **SC-011**: El jugador completa una carga rápida sosteniendo el teléfono con una sola
  mano, sin recolocarlo ni usar la otra.

## Assumptions

- El alcance es la interfaz y la interacción. Esta especificación no agrega capacidades
  nuevas al producto ni toca la captura, los avisos ni los recorridos, salvo donde el
  cambio de interfaz lo obliga.
- "Como Google Maps" se toma como referencia de comportamiento y de comodidad —mapa
  centrado, control de recentrado, marcadores con contenido, controles con forma
  reconocible— y no como pedido de copiar su estética ni sus funciones.
- El modo claro y el modo oscuro siguen lo que el jugador ya configuró en el teléfono. No
  se agrega un selector de tema dentro de la app: sería configuración que nadie pidió, y el
  Principio IV la desaconseja.
- El número que se muestra dentro del marcador es el de 3 dígitos que usa el juego, que
  por las Assumptions de la especificación 001 nunca pasa de 999.
- Borrar un registro desde el mapa es la misma operación que ya existe en la pantalla de
  búsqueda, no una nueva.
- La corrección de un registro existe porque el Principio II prohíbe editar la evidencia:
  si el número salió mal, hasta ahora la única salida era borrar y volver a capturar, lo
  que exige estar de nuevo en el lugar. Corregir el número no toca ninguna garantía,
  porque el número es lo que el jugador leyó, no lo que el teléfono midió.
- El volumen de marcadores en pantalla es de decenas, no de miles: son los registros de un
  jugador a lo largo de la vida del juego, concentrados en su barrio.
- Las pantallas se prueban en el teléfono del jugador, un dispositivo con Android 15 y
  barra de estado y navegación por gestos.
