# Feature Specification: Planear el próximo recorrido

**Feature Branch**: `004-planear-recorridos`

**Created**: 2026-08-30

**Status**: Draft

**Input**: User description:

> Quiero un brainstorming para lo siguiente. Tengo la necesidad de que yo pueda planear mi
> recorrido en base a los recorridos ya hechos. Actualmente el último recorrido se ve en un
> naranja fuerte mientras que los anteriores en un naranja clarito. La verdad que a mí lo que
> más me interesa es en total las calles recorridas y de alguna manera hace cuánto no hago un
> trayecto. En mis siguientes caminatas voy a intentar recorrer las calles que no recorrí y
> para eso necesito a simple vista ver cuáles sí recorrí, el naranja clarito no ayuda en ese
> caso. Pero es verdad que va a llegar un punto donde quizás esté todo pintado de naranja y yo
> quiera revisar las calles ya recorridas para chequear las patentes. Esta feature debería ser
> sobre mejorar la funcionalidad de recorridos, incluyendo el menú que hoy en día está bastante
> pobre. Ver todas las patentes por recorrido puede ocupar mucho espacio, a simple vista el
> mapa no se muestra y es simplemente feo la pantalla. Estoy pensando en un botón para poder
> apagar/prender el recorrido en el mapa global o ideas así.

## Contexto

La 003 puso el camino recorrido en el mapa y lo pintó con la regla que parecía obvia: el
recorrido de hoy fuerte, los anteriores apagados. Esa regla contesta **"¿cuál fue la última
salida?"**, y esa pregunta se hace una sola vez, mientras se camina, cuando el jugador ya sabe
la respuesta porque acaba de caminarla.

La pregunta que se hace **antes de salir** es otra: *"¿a dónde no fui todavía?"*. Y para
contestarla el histórico no es contexto de segunda: **es el dato**. Pintarlo apagado esconde
justo lo que hay que mirar. La jerarquía actual está invertida respecto del uso real.

Pero el histórico no responde una sola pregunta, responde dos, y son opuestas:

| Pregunta | Cuándo | Qué necesita ver |
|---|---|---|
| ¿Qué calles me faltan? | Al planear, mientras el mapa está mayormente vacío | Todo lo caminado **igual y fuerte**: lo que importa es el hueco |
| ¿Hace cuánto no paso por acá? | Cuando ya está casi todo pintado | Lo caminado **distinto entre sí** según la antigüedad |

Un solo dibujo no puede contestar las dos: la primera exige aplanar la diferencia entre
salidas, la segunda exige exhibirla. De ahí sale la forma de esta feature: **el mapa no cambia
de datos, cambia de pregunta**, y el jugador elige cuál con un interruptor.

El resto es la pantalla de Salidas, que hoy es una lista que vuelca todo lo que sabe —las
patentes como texto corrido, un mapa de 220 dp que empuja el resto fuera de la vista— y por
eso no se puede recorrer. Tiene los datos; le falta orden.

## Clarifications

### Session 2026-08-30

- Q: ¿Cuántos estados tiene el interruptor del mapa? → A: Tres, en un solo botón que cicla:
  cobertura, antigüedad y apagado. La US1 y la US2 comparten control en vez de que la antigüedad
  quede para después.
- Q: ¿El total de la pantalla de Salidas mide distancia caminada o cuenta calles distintas? → A:
  **Ninguna de las dos: no hay total.** "Qué calles recorrí" lo contesta el mapa del modo
  cobertura, y con eso la pregunta ya está respondida. Un contador al lado sería una segunda
  respuesta, peor. Esto también saca la distancia de cada fila de la lista, que existía solo como
  aproximación a esa misma pregunta.
- Q: ¿En qué escalones de tiempo se corta la escala de antigüedad del mapa? → A: Tres cortos:
  hasta 3 días, de 4 a 14 días, más de 14 días.
- Q: ¿Con qué se codifica la antigüedad — colores distintos o intensidades del mismo tono? → A:
  Tres colores distintos, no tres intensidades. Y además: el recorrido deja de ser naranja y pasa
  a ser el azul de ruta que usan las apps de mapas. Para que no se confunda con el trazo, los
  marcadores de patentes cambian de forma y de color: pin o globo de texto, número en negro sobre
  fondo blanco, en lugar del círculo de color actual.
- Q: Con el pin blanco, ¿dónde van el estado del registro y la probabilidad? → A: El borde del
  pin lleva la **probabilidad**. El estado no va a ninguna parte: *"ni sabía que existía el
  estado. Solo me importa la probabilidad. Si está compartida o no está compartida no es de mi
  interés y no debería ser algo necesario para la app. Por cuál patente vamos se trackea desde
  ajustes."*
- Q: ¿Hasta dónde llega ese retiro del estado? → A: Solo del mapa. El dato sigue existiendo y
  sigue haciendo su trabajo en los avisos y en la cuenta de números cubiertos; lo que se retira
  es el color del marcador, que era lo que no se leía.
- Q: ¿La patente del número actual se sigue destacando en el mapa? → A: Sí, pero **sin color**:
  con tamaño o halo. La paleta del marcador queda limpia y el color sigue diciendo una sola cosa,
  la probabilidad.

### Session 2026-09-02

Las dos salieron de usar la 004 en la calle, no de la mesa de diseño.

- Q: ¿Qué pasa cuando se carga una patente que ya está anotada en ese mismo lugar? → A: No se
  guarda una segunda vez. *"Esencial agregar que cuando yo agregue una patente, busque en un
  radio de 10 metros si ya existe una patente con ese número, si existe, actualizar la confianza
  de esa patente. Si no existe, crear una nueva patente."*
- Q: ¿Qué es "actualizar la confianza"? → A: Un voto de "sigue estando" sobre el registro que ya
  existe. Es el mecanismo de la 003 (FR-037), y encaja sin inventar nada: volver a ver la misma
  patente en el mismo lugar **es** una observación de permanencia, que es justo lo que el voto
  registra.
- Q: ¿Diez metros medidos contra qué? → A: Contra la posición de la captura nueva, en línea
  recta. Es poco más que el largo de un auto: alcanza para absorber el temblor del GPS urbano y
  no tanto como para fusionar dos autos distintos de la misma cuadra.
- Q: ¿El mapa puede quedarse sin las patentes? → A: Sí. *"Botón de esconder patentes."* Va al
  lado del interruptor de recorridos: son las dos mitades de lo que el mapa dibuja.
- Q: ¿Ese botón se recuerda entre aperturas, como el modo de recorridos? → A: **No.** Un mapa que
  abre sin patentes se ve igual que un mapa sin nada guardado, y el jugador que lo dejó apagado
  hace tres días no se acuerda: el arranque parece pérdida de datos. El modo de recorridos sí
  puede recordarse, porque su leyenda está siempre a la vista diciendo en qué estado está.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver de un vistazo qué calles me faltan (Priority: P1)

Antes de salir, el jugador abre la app y mira el mapa. Todo lo que caminó alguna vez está
pintado del mismo color fuerte, sin importar cuándo fue. Lo que no está pintado es lo que le
falta, y salta a la vista sin que tenga que comparar tonos ni recordar qué salida fue cuál.
Con eso decide para qué lado arranca.

Si en algún momento el trazo le tapa las patentes que quiere mirar, un interruptor sobre el
mapa lo apaga y deja el mapa limpio.

**Why this priority**: es la necesidad que abre el pedido y la que hoy está mal servida. Sin
esto la feature no existe: el resto son modos y pantallas que giran alrededor de esta.

**Independent Test**: con dos o más salidas guardadas, verificar que todas se dibujan con el
mismo peso visual y que el interruptor las quita y las devuelve. Entrega valor sola: el jugador
ya puede planear.

**Acceptance Scenarios**:

1. **Given** tres salidas guardadas en fechas distintas, **When** el jugador abre el mapa
   principal en modo cobertura, **Then** las tres se dibujan con el mismo color, ancho y
   opacidad, sin ninguna destacada sobre las otras.
2. **Given** el mapa con recorridos dibujados, **When** el jugador toca el interruptor hasta
   apagarlos, **Then** no queda ningún trazo en el mapa y las patentes siguen visibles.
3. **Given** los recorridos apagados, **When** el jugador cierra la app y la vuelve a abrir,
   **Then** siguen apagados: la elección se recuerda.
4. **Given** un recorrido en curso, **When** el jugador camina en modo cobertura, **Then** el
   trazo sigue creciendo en vivo, con el mismo aspecto que el resto.

---

### User Story 2 - Saber hace cuánto no paso por una calle (Priority: P2)

Con el barrio ya bastante pintado, la pregunta del jugador cambia: no es dónde no fue nunca,
sino **dónde hace más tiempo que no pasa**. Ahí es donde las patentes cambiaron: autos que se
fueron, autos nuevos que aparecieron. Cambia el modo del mapa y el color pasa a decir
antigüedad: lo caminado hace mucho **resalta**, lo caminado hace poco se apaga.

**Why this priority**: es el segundo tiempo del mismo problema y el jugador ya lo anticipó,
pero recién muerde cuando la cobertura crece. Se construye sobre el mismo dibujo de la US1.

**Independent Test**: con salidas de fechas separadas, verificar que el mismo tramo cambia de
aspecto según cuándo se caminó, y que una leyenda deja leer la escala sin adivinar.

**Acceptance Scenarios**:

1. **Given** salidas de hace 1 día, de hace 10 días y de hace 2 meses, **When** el jugador pasa
   a modo antigüedad, **Then** las tres caen en escalones distintos, se distinguen entre sí, y
   la más vieja es la que más resalta.
2. **Given** el modo antigüedad activo, **When** el jugador mira el mapa, **Then** hay una
   referencia visible con los tres escalones y sus plazos escritos: hasta 3 días, de 4 a 14
   días, más de 14 días.
3. **Given** dos salidas de hace 5 y de hace 12 días, **When** se dibujan, **Then** se ven
   iguales: las dos caen en el mismo escalón, y la escala no promete un detalle que no tiene.
4. **Given** dos salidas que pisan la misma calle en fechas distintas, **When** se dibujan,
   **Then** esa calle se ve con la antigüedad de **la más reciente de las dos**: haber pasado
   ayer no se borra por haber pasado también hace un año.
5. **Given** una única salida guardada, **When** el jugador entra en modo antigüedad, **Then**
   el mapa se sigue entendiendo y no exige una escala que no hay de qué sacar.

---

### User Story 3 - Una pantalla de Salidas que se pueda recorrer (Priority: P2)

El jugador entra a Salidas para dos cosas: ver cuánto hace que no sale, y abrir una salida
puntual. La lista le muestra una fila por salida, corta y pareja —cuándo fue, cuánto duró,
cuántas patentes—, y arriba, sin que toque nada, hace cuánto fue la última vez.

Cuando quiere el detalle, abre la salida y ahí sí ve el mapa grande y sus patentes, con lugar
para mostrarlos.

**Why this priority**: es la mitad del pedido y hoy la pantalla es inservible para recorrer,
pero no bloquea el planeamiento, que pasa por el mapa principal.

**Independent Test**: con varias salidas cargadas, verificar que la lista entra en pantalla sin
desplazamiento desmedido y que el detalle de una salida muestra su mapa sin recortarlo.

**Acceptance Scenarios**:

1. **Given** cinco salidas guardadas, **When** el jugador abre Salidas, **Then** ve las cinco
   filas sin que ninguna despliegue mapas ni listas de patentes.
2. **Given** la lista de salidas, **When** el jugador la mira sin tocar nada, **Then** lee cuánto
   hace que fue la última salida, y no ve ningún total acumulado de calles ni de distancia.
3. **Given** una salida de la lista, **When** el jugador la abre, **Then** ve su camino en un
   mapa que ocupa el espacio necesario para entenderse, y sus patentes.
4. **Given** una salida sin puntos registrados, **When** el jugador la abre, **Then** se le
   dice que no hay camino que mostrar, sin abrir un mapa vacío (se conserva el FR-010b de la
   003).
5. **Given** el detalle de una salida abierto, **When** el jugador la borra, **Then** vuelve a
   la lista y la salida ya no está, con la confirmación y las garantías que fijó la 003.

---

### User Story 4 - Un mapa donde el color dice una sola cosa (Priority: P3)

El jugador mira el mapa y no tiene que interpretar cuatro códigos de color a la vez. El camino
es azul, como en cualquier app de mapas. Las patentes son pines blancos con el número en negro,
que se leen sobre cualquier fondo. El único color que llevan es el del borde, y dice una sola
cosa: qué tan probable es que la patente siga estando ahí. La que toca ahora se distingue por
tamaño, no por color.

**Why this priority**: nació como consecuencia de la US1 —dos códigos de color naranja peleando
por el mismo mapa— pero terminó tocando algo aparte: un color que el jugador nunca supo que
existía y que estaba ocupando el canal más visible del marcador.

**Independent Test**: con patentes de los tres niveles de probabilidad sobre un tramo caminado,
verificar que el número se lee, que el borde dice la probabilidad y que ningún color del
marcador se confunde con el del camino.

**Acceptance Scenarios**:

1. **Given** patentes sobre una calle ya caminada, **When** el jugador mira el mapa, **Then** el
   número de cada una se lee sin acercar, en modo claro y en modo oscuro.
2. **Given** tres patentes con probabilidad baja, media y alta, **When** se dibujan, **Then** se
   distinguen entre sí por el borde del pin.
3. **Given** una patente ya compartida y una pendiente, **When** se dibujan, **Then** se ven
   iguales en el mapa: esa distinción dejó de ocupar color.
4. **Given** el número actual del juego cargado y una patente con ese número, **When** el jugador
   mira el mapa, **Then** ese pin se distingue de los demás por tamaño o halo, sin usar color.
5. **Given** cualquiera de los tres modos del mapa activo, **When** el jugador mira un marcador,
   **Then** ningún color del marcador se confunde con el del trazo.

---

### User Story 5 - Mirar el mapa sin las patentes encima (Priority: P2)

El jugador está planeando por dónde caminar y las patentes le tapan el trazo. Un toque las
esconde y el mapa queda con los recorridos solos; otro toque las trae de vuelta.

**Why this priority**: es la contracara de la US1. Con el histórico dibujado y cincuenta pines
encima, el hueco —que es el dato— queda debajo de los marcadores justo en la zona más caminada,
que es donde más patentes hay. El FR-005 ya decía que apagar los recorridos no puede esconder
las patentes; esto es la operación inversa, y por eso es otro botón y no un cuarto estado del
mismo.

**Independent Test**: con patentes y recorridos dibujados, un toque deja el mapa con el trazo
solo y otro lo deja como estaba.

**Acceptance Scenarios**:

1. **Given** el mapa con patentes y recorridos, **When** el jugador toca el botón de esconder,
   **Then** los marcadores desaparecen y el trazo se sigue viendo igual.
2. **Given** las patentes escondidas, **When** el jugador mira la leyenda, **Then** dice que
   están ocultas: un mapa sin pines y un mapa sin nada guardado no pueden verse igual.
3. **Given** las patentes escondidas, **When** el jugador vuelve a tocar el botón, **Then**
   reaparecen sin recargar el mapa ni esperar.
4. **Given** las patentes escondidas, **When** el jugador cierra la app y la vuelve a abrir,
   **Then** las patentes están a la vista otra vez.
5. **Given** las patentes escondidas, **When** el jugador carga una patente nueva, **Then** se
   guarda igual: esconder es dejar de dibujar, no dejar de registrar.

---

### User Story 6 - La misma patente vista dos veces no es dos patentes (Priority: P1)

El jugador vuelve a pasar por donde ya anotó un auto y lo carga de nuevo, porque desde la
vereda no se acuerda de cuál cargó la semana pasada. La app reconoce que es la misma —mismo
número, mismo lugar— y en vez de apilar un segundo pin encima del primero, sube la confianza
del que ya estaba.

**Why this priority**: P1 porque los duplicados **corrompen el mapa**, que es lo que las otras
cinco stories construyen. Dos registros encimados se leen como dos autos, cada uno con su
confianza a medias, y la probabilidad —que la 003 puso en el borde del pin justamente para
decidir si vale la pena caminar hasta ahí— pasa a mentir en los dos.

**Independent Test**: cargar dos veces el mismo número sin moverse; tiene que quedar un solo
registro, con la confianza más alta que después de la primera carga.

**Acceptance Scenarios**:

1. **Given** una patente ya anotada, **When** el jugador carga el mismo número a menos de diez
   metros, **Then** no se crea un registro nuevo y la confianza del que estaba sube.
2. **Given** esa misma situación, **When** termina la carga, **Then** el aviso dice que ya
   estaba y con qué confianza quedó: el jugador no puede quedarse creyendo que guardó algo
   nuevo.
3. **Given** el mismo número anotado a varias cuadras, **When** el jugador lo carga acá,
   **Then** se crea un registro nuevo: son dos autos distintos con el mismo número.
4. **Given** dos patentes de ese número dentro del radio, **When** el jugador carga, **Then** la
   confianza sube en la **más cercana**, no en la primera que aparezca.
5. **Given** una captura con foto sobre una patente que ya estaba y no tenía foto, **When** se
   confirma, **Then** la foto se adjunta al registro que ya existía.
6. **Given** una patente confirmada así, **When** el jugador abre su ficha, **Then** la
   ubicación y la fecha siguen siendo las de la primera vez que la vio.

---

### Edge Cases

- **No hay ninguna salida guardada.** El interruptor no tiene qué prender: se muestra en su
  estado apagado sin prometer nada, y la pantalla de Salidas sigue diciendo cómo empezar una.
- **Todas las salidas son del mismo día.** El modo antigüedad no tiene rango que mostrar. Se
  dibuja todo en el extremo "reciente" de la escala; no se inventa una diferencia que no hay.
- **Una salida sin camino ajustado a calles.** Se dibuja con su camino crudo, como fija el
  FR-033 de la 003. Los modos nuevos no cambian esa regla: cambian el color, no el origen del
  trazo.
- **El recorrido en curso en modo antigüedad.** Tiene antigüedad cero por definición y se dibuja
  como lo más reciente de la escala, sin ser un caso aparte.
- **El interruptor apagado mientras se camina.** El servicio sigue grabando: apagar el dibujo
  no apaga el registro. Nada de esta feature toca lo que se guarda.
- **Muchas salidas encimadas sobre la misma avenida.** El color no se acumula ni se satura:
  diez pasadas por la misma calle se ven igual que una, porque la pregunta es si pasó, no
  cuántas veces.
- **La última salida, borrada.** El "hace cuánto" del encabezado pasa a hablar de la que quedó
  como más reciente; no queda un número que ya no corresponde a ninguna salida.
- **Un grupo de patentes amontonadas.** Sigue siendo un grupo con su cuenta, y no un pin: un
  grupo no tiene un número de patente que mostrar ni una probabilidad propia. Se distingue de un
  pin por la forma, como antes se distinguía por el color.
- **Una patente sin ningún voto.** Cae en el escalón medio del borde —"todavía no se sabe"—,
  igual que hoy (FR-037b de la 003). El pin blanco no la deja sin borde.
- **Un pin sobre el trazo azul.** Es el caso normal: casi todas las patentes están sobre una
  calle caminada. El pin blanco con borde de color tiene que leerse encima del trazo, no solo
  sobre el fondo del mapa.
- **Las patentes escondidas y los recorridos apagados a la vez.** El mapa queda vacío a
  propósito, y la leyenda lo dice con las dos cosas escritas. Es el único estado en que el mapa
  no muestra nada, y tiene que verse elegido, no roto.
- **Cargar una patente con las patentes escondidas.** Se guarda igual y el aviso lo confirma. Lo
  que no pasa es que aparezca un pin: esconder es una decisión del jugador, y una carga no la
  revierte sola.
- **Una patente a once metros de otra con el mismo número.** Se crea un registro nuevo. El radio
  es un corte, y todo corte tiene un lado de afuera: la alternativa —preguntarle al jugador— pone
  una pregunta en el camino de los diez segundos del Principio I.
- **Volver a cargar una patente con el GPS degradado.** La comparación usa la posición medida,
  con su error incluido. Con una lectura de cincuenta metros de precisión puede no reconocer la
  patente que sí estaba: el resultado es un duplicado, que es el error barato. El caro sería
  fusionar dos autos distintos.
- **Confirmar una patente que ya tenía foto, sacando otra.** La foto nueva se descarta y el
  archivo se borra. Adjuntarla exigiría reemplazar la que estaba, que es reescribir evidencia.
- **Confirmar la patente del número actual.** No cambia el conjunto de avisos: el registro ya
  existía y ya tenía su geofence. No hace falta reconciliar nada.


## Requirements *(mandatory)*

### El interruptor del mapa

- **FR-001**: El mapa de la pantalla principal MUST tener un control visible que cambia cómo se
  dibujan los recorridos, sin salir del mapa ni entrar a Ajustes.
- **FR-002**: Ese control MUST ciclar por **tres** estados con un solo toque cada vez:
  **cobertura** (todo con el mismo peso, US1), **antigüedad** (color por hace cuánto, US2) y
  **apagado**. Un control y no dos: el mapa ya tiene su barra de acciones abajo, y un selector
  aparte agregaría un segundo elemento encima del mapa para elegir entre tres cosas.
- **FR-003**: El estado elegido MUST recordarse entre aperturas de la app. Es una preferencia de
  cómo se mira el mapa, no un estado de sesión.
- **FR-004**: El control MUST decir en cuál de los tres estados está sin que el jugador tenga que
  deducirlo del mapa. Es lo que hace viable ciclar en vez de elegir: con el mapa vacío de
  recorridos, "apagado" y "no caminé nada" se ven igual, y con pocas salidas cargadas cobertura y
  antigüedad pueden llegar a verse parecidas.
- **FR-005**: Apagar los recorridos MUST NOT ocultar las patentes ni ningún otro elemento del
  mapa. Apaga el camino, nada más.
- **FR-006**: Apagar los recorridos MUST NOT afectar el registro del trayecto. El servicio sigue
  grabando exactamente igual, y el trazo reaparece completo al volver a prenderlo.

### Modo cobertura: qué me falta

- **FR-007**: En modo cobertura, todos los recorridos guardados MUST dibujarse con el **mismo**
  color, ancho y opacidad, incluido el que está en curso. **Esto revierte el FR-007a de la
  003**: destacar el más reciente contesta una pregunta que el jugador no se hace al planear, y
  al hacerlo apaga el histórico, que es lo que sí necesita ver.
- **FR-008**: Ese aspecto único MUST tener contraste suficiente contra el fondo del mapa —en
  claro y en oscuro— para que el hueco, y no el trazo, sea lo que se lee.
- **FR-008a**: El trazo de cobertura MUST dibujarse en el **azul de ruta** que usan las apps de
  mapas, y no en el naranja de la 003. Es el color que ya significa "camino" para cualquiera que
  usó un GPS, y deja el naranja libre para que no compita con nada.
- **FR-009**: El recorrido en curso MUST seguir creciendo en vivo mientras se camina (FR-008 de
  la 003), ahora sin distinguirse del resto.
- **FR-010**: Los huecos por desconexión MUST seguir dibujándose punteados y distinguibles de un
  tramo caminado (FR-009a de la 003). Un hueco no es cobertura: es exactamente lo que no se
  sabe, y aplanar los colores no puede convertirlo en calle caminada.

### Modo antigüedad: hace cuánto no paso

- **FR-011**: El sistema MUST poder dibujar cada tramo del histórico según **hace cuánto** se
  caminó, derivándolo de la fecha de la salida a la que pertenece.
- **FR-012**: En ese modo, lo caminado hace **más** tiempo MUST resaltar por sobre lo caminado
  hace poco. La jerarquía es al revés que en la 003, y a propósito: lo viejo es lo que hay que
  ir a revisar.
- **FR-012a**: Los tres escalones MUST distinguirse por **colores distintos**, no por tres
  intensidades del mismo tono. Dos intensidades vecinas obligan a comparar, y comparar es
  exactamente lo que esta feature vino a sacar del medio.
- **FR-012b**: Ninguno de los tres colores de la escala MUST ser el azul de cobertura del
  FR-008a. Así el modo se reconoce mirando el mapa dos segundos, sin leer la etiqueta del
  control.
- **FR-013**: Cuando dos salidas pisan el mismo tramo, MUST prevalecer visualmente la **más
  reciente**: la antigüedad de una calle es la de la última vez que se pasó.
- **FR-014**: La escala de antigüedad MUST tener exactamente **tres escalones**, cortados en
  **3 días** y en **14 días**: hasta 3 días, de 4 a 14 días, y más de 14 días. Son cortos a
  propósito: las patentes rotan en días, no en meses, así que una calle de hace dos semanas ya
  merece revisarse. El corte de 3 días separa además lo caminado en el fin de semana de lo de la
  semana anterior.
- **FR-014a**: El mapa MUST mostrar una referencia visible de qué significa cada escalón, con
  esos plazos escritos. Un degradado sin leyenda no es un dato, es una decoración.
- **FR-015**: La escala MUST dibujarse como esos tres escalones y MUST NOT ser un gradiente
  continuo. La pregunta es "¿hace mucho o hace poco?", y una rampa de tonos no la contesta mejor
  que tres colores que se distinguen sin comparar.
- **FR-016**: Con una sola salida, o con todas del mismo día, el modo MUST seguir siendo
  utilizable y MUST NOT inventar diferencias de antigüedad que no existen.

### Los marcadores de patentes

Cambiar el color del trazo arrastra a los marcadores: hoy son círculos de color con el número en
blanco, y el color es lo que dice el estado. Con el camino en azul y la escala de antigüedad en
otros tres colores, el mapa pasa a tener demasiadas cosas hablando por color a la vez.

- **FR-028**: Los marcadores de patentes MUST dibujarse con forma de **pin o globo de texto con
  punta**, no como círculo. La punta señala el lugar exacto, y la forma —no el color— es lo que
  separa "una patente" de "un camino" a simple vista.
- **FR-029**: El número MUST leerse en **negro sobre fondo blanco**. El blanco no compite con
  ningún trazo del mapa en ninguno de los modos, y el número es lo que el jugador va a buscar.
- **FR-030**: El **borde** del pin MUST llevar la probabilidad de que la patente siga estando,
  con los mismos tres escalones netos —baja, media, alta— que fijó el FR-037b de la 003. Es el
  único dato de color que le queda al marcador, y es el que decide si vale la pena caminar hasta
  ahí.
- **FR-030a**: El marcador MUST NOT seguir codificando con color el **estado** del registro
  —pendiente, ya toca, compartida—. **Esto retira del mapa el FR-020 de la 001 y el FR-011 de la
  002.** El motivo no es de diseño sino de producto: el jugador no sabía que ese color decía
  algo, y "compartida o no compartida" no es una distinción que use. Un color que nadie lee es
  ruido compitiendo con el que sí importa.
- **FR-030b**: Ese retiro MUST alcanzar **solo al mapa**. El estado sigue guardándose, sigue
  marcándose al compartir, y sigue haciendo su trabajo donde nadie se quejó: que una patente ya
  mandada al grupo no cuente como cubierta (FR-021 de la 001) y no dispare aviso al pasar cerca.
  Se retira el color que nadie leía, no el concepto que sostiene dos funciones que sí se usan.
- **FR-030c**: Las fichas y los resultados de búsqueda MAY seguir diciendo con texto si una
  patente ya se compartió. Es donde el dato sí se lee, porque hay lugar para escribirlo.
- **FR-030d**: La patente cuyo número es el actual del juego MUST seguir destacándose en el
  mapa, y MUST hacerlo **sin color**: con tamaño, halo o ambos. Es lo que la app existe para
  ayudar a encontrar, así que no puede verse igual que las otras cincuenta; y separarla por
  tamaño deja el color diciendo una sola cosa en todo el marcador, que es la probabilidad.
- **FR-032**: Tocar un marcador, agrupar por acercamiento y abrir un grupo MUST seguir
  funcionando igual (FR-038 y FR-039 de la 003). Esta feature cambia cómo se ve el marcador, no
  qué hace.

### La pantalla de Salidas

- **FR-017**: La lista de Salidas MUST mostrar una fila **compacta y de altura pareja** por
  salida. Ninguna fila MUST desplegar un mapa ni la lista completa de patentes dentro de la
  lista. **Esto retira el mapa embebido del FR-010a de la 003**, que se construyó como se pidió
  y en uso resultó empujar el resto de la lista fuera de la pantalla.
- **FR-018**: Cada fila MUST decir cuándo fue la salida, cuánto duró y cuántas patentes salieron
  de ella. **No lleva ninguna medida de cuánto se caminó**: qué calles se recorrieron lo contesta
  el mapa, y un número al lado sería una segunda respuesta peor a la misma pregunta.
- **FR-019**: La fecha de cada salida MUST poder leerse como tiempo transcurrido ("hace 3
  días"), que es la forma en que el jugador se hace la pregunta.
- **FR-020**: La pantalla MUST mostrar, sin que el jugador toque nada, **hace cuánto fue la
  última salida**. Es la única de las dos preguntas que trae al entrar acá que el mapa no
  contesta.
- **FR-020a**: La pantalla MUST NOT mostrar ningún total acumulado —ni de calles, ni de
  distancia, ni de cuadras—. "Qué calles recorrí" ya lo contesta el mapa del FR-007, y un
  contador al lado sería una respuesta más pobre a una pregunta ya respondida.
- **FR-021**: Los usuarios MUST poder abrir una salida y ver su camino en un mapa con espacio
  suficiente para entenderlo, junto con las patentes capturadas en ella.
- **FR-022**: Ese detalle MUST conservar lo que la 003 ya garantiza: el mapa encuadra el camino
  completo sin seguir al jugador, una salida sin puntos avisa en vez de abrir un mapa vacío
  (FR-010b), y borrar la salida pide confirmación y **no toca ninguna patente** (FR-024 de la
  003).
- **FR-023**: La lista de patentes de una salida MUST poder recorrerse y MUST llevar a la ficha
  de cada patente, en lugar de volcarse como una línea de texto separada por comas.

### Esconder las patentes

El FR-005 dice que apagar los recorridos no puede esconder las patentes. Esta es la operación
inversa, y es un control aparte por la misma razón: son dos capas distintas, y cada una se prende
y se apaga sola.

- **FR-033**: El mapa de la pantalla principal MUST tener un control que esconde y vuelve a
  mostrar los marcadores de patentes, con **un solo toque** cada vez.
- **FR-033a**: Ese control MUST vivir en la misma franja que el interruptor de recorridos, y
  MUST NOT achicar el campo de número ni agregar pasos a la carga rápida (contrato C4,
  Principio I).
- **FR-034**: Esconder las patentes MUST NOT apagar los recorridos, y apagar los recorridos MUST
  NOT esconder las patentes. Las dos capas son independientes.
- **FR-035**: Mientras las patentes están escondidas, el mapa MUST decirlo por escrito en la
  misma leyenda que dice el estado de los recorridos. Un mapa sin pines y un mapa sin patentes
  guardadas se ven idénticos, y esa confusión es lo que hace peligroso al interruptor.
- **FR-035a**: El estado del control MUST NOT recordarse entre aperturas de la app: cada arranque
  muestra las patentes. Es la diferencia deliberada con el FR-003 —el modo de recorridos sí se
  recuerda— y el motivo es que un arranque sin patentes se confunde con una pérdida de datos.
- **FR-035b**: Esconder las patentes MUST NOT afectar en nada lo que se guarda ni lo que se
  avisa. La carga sigue funcionando, los avisos siguen llegando y la cuenta de números cubiertos
  no cambia. Se retira el dibujo, no el dato.

### Una patente vista dos veces

Volver a pasar por donde ya se anotó un auto y cargarlo de nuevo es lo normal, no un error del
jugador: desde la vereda no hay forma de acordarse de cuál de las cincuenta ya está. Hoy eso deja
dos pines encimados que se leen como dos autos.

- **FR-036**: Al guardar una patente, el sistema MUST buscar si ya hay un registro con el
  **mismo número** a **diez metros o menos** de la posición recién medida.
- **FR-036a**: Si hay más de uno dentro del radio, MUST elegirse el **más cercano**.
- **FR-037**: Si existe, el sistema MUST NOT crear un registro nuevo, y MUST subir la confianza
  del que ya existe registrando un voto de "sigue estando" (FR-037 de la 003).
- **FR-037a**: Ese voto MUST llevar la hora de la carga. Es lo que hace que la confirmación
  cuente como una observación con fecha y no como una corrección del pasado.
- **FR-038**: Si no existe, el sistema MUST crear un registro nuevo, exactamente como hasta
  ahora.
- **FR-039**: La confirmación MUST decírsele al jugador, distinguiéndola de un guardado nuevo, y
  MUST incluir la confianza en la que quedó la patente. Un mensaje igual al de siempre lo dejaría
  creyendo que cargó algo que no cargó.
- **FR-039a**: Confirmar una patente MUST NOT tocar ningún campo de evidencia del registro que ya
  existía —ubicación, precisión y momento siguen siendo los de la primera vez que se la vio
  (Principio II)—. Lo nuevo viaja en el voto, que trae su propia hora.
- **FR-039b**: Si la carga traía foto y el registro confirmado no tenía ninguna, la foto MUST
  adjuntarse a ese registro. Si ya tenía, la foto nueva MUST descartarse y su archivo borrarse:
  reemplazarla sería reescribir evidencia, y dejarla suelta sería un archivo huérfano.
- **FR-039c**: El radio de diez metros MUST ser un valor calibrable en un solo lugar del código,
  no un número repartido por el camino de carga.

### Lo que no cambia

- **FR-024**: Esta feature MUST NOT modificar ni descartar ningún punto de trayecto, camino
  ajustado, registro de captura ni voto. Cambia cómo se mira lo guardado, no lo guardado
  (Principio II).
- **FR-025**: Esta feature MUST NOT agregar pasos, demoras ni pantallas al camino de carga
  rápida, que sigue entrando en 4 interacciones (Principio I).
- **FR-026**: Ningún modo del mapa MUST requerir conexión para dibujarse. Todo sale de lo que ya
  está en el teléfono (Principio III).
- **FR-027**: Los modos MUST NOT degradar la fluidez del mapa al desplazarlo o acercarlo
  (FR-010 de la 003).

### Key Entities

- **Salida (recorrido)**: ya existe. Esta feature le suma una sola lectura derivada
  —**antigüedad**, hace cuánto terminó— que sale de la fecha ya guardada y no se persiste como
  campo nuevo.
- **Modo de vista del mapa**: cuál de las preguntas está contestando el mapa ahora. Es una
  preferencia del jugador, una sola, que sobrevive al cierre de la app. No es un atributo de
  ninguna salida.
- **Patentes visibles**: si el mapa está dibujando los marcadores. Es estado de pantalla y
  **no** se persiste (FR-035a), a diferencia del modo de vista.
- **Voto**: ya existe, de la 003. Esta feature le suma un segundo origen —la carga de una
  patente que ya estaba— además del botón de la ficha. No cambia de forma ni de significado.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Frente al mapa principal en modo cobertura, el jugador puede señalar una calle que
  no caminó nunca en menos de 5 segundos, sin acercar ni comparar tonos.
- **SC-002**: Todas las salidas guardadas se ven con el mismo peso visual en modo cobertura:
  ninguna se lee como "más importante" que otra.
- **SC-003**: Cambiar de modo o apagar los recorridos toma **un solo toque** desde el mapa, sin
  entrar a ninguna otra pantalla, y volver al modo de partida toma como mucho tres.
- **SC-004**: El estado del interruptor se conserva al cerrar y volver a abrir la app en el
  100% de los casos.
- **SC-005**: En modo antigüedad, el jugador puede decir cuál de dos calles pintadas hace más
  que no pisa, sin abrir la pantalla de Salidas.
- **SC-006**: Con diez salidas guardadas, la lista de Salidas entra en menos de dos pantallas
  de alto, contra las diez o más de hoy.
- **SC-007**: "Hace cuánto fue la última salida" se lee al entrar a Salidas sin tocar nada ni
  desplazar.
- **SC-008**: El mapa mantiene su fluidez con todo el histórico dibujado en cualquiera de los
  modos.
- **SC-009**: El número de una patente se lee sin acercar el mapa, en tema claro y oscuro, y con
  el pin encima del trazo.
- **SC-010**: El jugador puede decir la probabilidad de una patente mirando solo el mapa, sin
  abrir la ficha.
- **SC-011**: Con el número actual cargado, el jugador ubica su patente en el mapa en menos de 3
  segundos sin leer números uno por uno.
- **SC-012**: Ningún color del marcador se confunde con el del trazo en ninguno de los tres
  modos.
- **SC-013**: Esconder las patentes y volver a mostrarlas toma **un toque** cada vez, y el mapa
  responde sin recargar.
- **SC-014**: Cargar dos veces el mismo número sin moverse deja **un** registro en la base, no
  dos, y la confianza del que queda es más alta que después de la primera carga.
- **SC-015**: Después de una confirmación, la ficha de esa patente sigue mostrando la ubicación y
  la fecha de la **primera** vez que se la vio.

## Assumptions

- **El histórico es el dato, no el contexto.** Es la inversión que motiva la feature y hay que
  decirla explícita porque contradice al FR-007a de la 003: al planear, la salida de ayer no
  vale más que la de hace un mes; lo que vale es la unión de todas contra el mapa vacío.
- **Un mismo dibujo no puede contestar las dos preguntas**, y por eso hay modos en vez de un
  color más astuto. Se descartó buscar una paleta única que sirviera para las dos: aplanar y
  diferenciar son requisitos opuestos, y cualquier compromiso las contesta mal a las dos.
- **La antigüedad se mide por salida, no por tramo de calle.** Cada camino guardado se colorea
  según su propia fecha, y dibujando las salidas de la más vieja a la más nueva, lo reciente
  queda arriba de lo viejo donde se superponen. Eso satisface el FR-013 sin calcular
  intersecciones de geometría, que sería trabajo pesado en el teléfono para un resultado que se
  ve igual.
- **"Qué calles recorrí" es una pregunta del mapa, no un número.** Decidido explícitamente: se
  contesta mirando el trazo en modo cobertura. Se descartaron las dos formas de ponerlo en un
  contador —distancia caminada, y contar nombres de calle pidiéndoselos al servicio de ajuste—
  porque las dos son respuestas peores a una pregunta que el mapa ya contesta bien, y la segunda
  además trae red y datos nuevos.
- **No se cuentan cuadras ni celdas.** La 003 retiró la grilla (su FR-021) por el mismo motivo:
  la pregunta no era "¿cuántas cuadras?" sino "¿por dónde fui?". Esta feature no la trae de
  vuelta con otro nombre.
- **La preferencia del modo vive donde ya vive la de los avisos.** El juego ya guarda una
  preferencia del jugador de la misma naturaleza; esta es otra igual y no justifica ninguna
  estructura nueva (Principio IV).
- **El color del mapa dice una sola cosa por elemento.** El trazo dice camino —azul, o el
  escalón de antigüedad—, y el marcador dice probabilidad. Es la regla que hizo caer el color de
  estado: con cuatro códigos encima del mismo mapa, el que nadie sabía leer era el que había que
  sacar, no el que había que explicar mejor.
- **Que el jugador no supiera que el color del marcador decía algo es el dato**, no una anécdota.
  El FR-020 de la 001 y el FR-011 de la 002 se construyeron como se especificaron; lo que la
  calle devolvió es que la distinción que codificaban no es una que se use. Se retira del mapa por
  eso, no porque estuviera mal hecha.
- **El estado se retira del mapa, no de la app.** Sigue sosteniendo dos funciones que nadie
  cuestionó: que una patente ya compartida no cuente como cubierta, y que no dispare aviso al
  pasar cerca. Retirar el concepto entero es otra feature con otro alcance.
- **El corte por desconexión sobrevive a todos los modos.** Aplanar colores no puede convertir
  "no sé qué pasó acá" en "caminé esto": es la lección que la 003 aprendió en la calle y esta
  feature no la revisa.
- **Un usuario, un teléfono.** Nada de esto necesita cuentas, sincronización ni compartir
  cobertura con nadie (Principio IV).

## Out of Scope

- **Prender y apagar salidas de a una.** El pedido menciona un interruptor global; filtrar salida
  por salida es una segunda función que nadie usó todavía. Si al usar esto aparece la necesidad
  real de esconder una salida puntual, entra sola.
- **Sugerir por dónde caminar.** El mapa muestra el hueco; elegir el camino lo sigue haciendo el
  jugador. Proponer rutas exige un motor de ruteo, que la 003 ya decidió no construir.
- **Cualquier contador de cobertura**: calles, cuadras, kilómetros o porcentajes. Descartado
  explícitamente en la sesión de clarificación, no diferido. Ver el FR-020a.
- **Avisar "hace mucho que no pasás por acá".** Los avisos por geofence existen y son para
  patentes. Extenderlos a zonas es otra feature, con su propio costo de batería.
- **Exportar o compartir la cobertura.** Nadie lo pidió.
- **Retirar el concepto de "compartida" de la app.** El jugador dijo que no le interesa, y por eso
  se va del mapa (FR-030a). Sacarlo del todo toca compartir, los avisos y la cuenta de números
  cubiertos, que son tres cosas que no molestaron. Si al usarlo sigue sin servir, entra como
  feature propia.
