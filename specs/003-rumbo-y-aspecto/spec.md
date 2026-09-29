# Feature Specification: Rumbo y aspecto

**Feature Branch**: `003-rumbo-y-aspecto`

**Created**: 2026-08-29

**Status**: Draft

**Input**: User description: observaciones del primer uso real en la calle, después de mergear la
002:

> - El recorrido al menos visualmente está roto, se dibujan rectángulos violetas en vez del
>   recorrido.
> - El guardado y agrupación de patentes funciona perfecto.
> - Una feature esencial que no está es la de mostrarme el recorrido hacia la patente. Ya sea
>   en la app o redirigiendo a google maps.
> - La pantalla de buscar, no pone como recomendado al que haría match al que estás buscando,
>   ni muestra los más recientes. Además el input está arriba de todo y es incómodo.
> - La UI al menos los colores violáceos son feos. Al menos ir por algo más estándar con
>   colores negros, blancos, etc. El input con fondo transparente también es feo. La parte de
>   abajo con acciones debería ser más chica y tener un fondo para ver correctamente los
>   botones.
> - La barra de acciones nativa de Android por alguna razón es blanca con botones blancos, en
>   otros apps se ajusta correctamente para hacer contraste.

## Contexto

Esta es la primera especificación escrita **después de usar la app caminando**. Las dos
anteriores se validaron mirando el teléfono en la mano; esta nace de una salida real.

Eso cambia el peso de lo que pide. La 002 fue pulido elegido desde el escritorio; la 003 es
lo que la calle devolvió. Un solo punto de la lista es un defecto de código; el resto son
cosas que se construyeron como se especificaron y que, en uso, resultaron no ser lo que hacía
falta. Vale la pena decirlo explícitamente porque cambia el tono de la feature: no se está
arreglando trabajo mal hecho, se está corrigiendo el rumbo con información que antes no
existía.

## Clarifications

### Session 2026-08-29

- Q: ¿Cuántos recorridos se dibujan sobre el mapa al mismo tiempo? → A: Todos los recorridos
  guardados, con el en curso —o el más reciente si no hay ninguno— destacado sobre los demás.
- Q: ¿Qué tiene que pasar entre dos posiciones consecutivas para que el trazo se corte en vez
  de unirlas? → A: Nada: el trazo nunca se corta. Las posiciones consecutivas de un recorrido
  se unen siempre, y la línea recta sobre un tramo sin señal se acepta.
- Q: ¿La distancia y la dirección hasta una patente se muestran siempre, o solo cuando no hay
  conexión? → A: Siempre, junto a los datos de la patente. Quedar sin conexión deja de ser un
  camino aparte.
- Q: ¿Dónde se dibujan los recorridos: en el mapa de la pantalla principal, o en la pantalla
  de Salidas? → A: En los dos. El histórico completo en la principal, y Salidas gana un mapa
  para revisar una salida puntual.

### Session 2026-08-29 (segunda salida a la calle)

La primera versión implementada se probó caminando. Lo que devolvió:

- Q: El trazo unió con una recta un corte de señal, y "de pronto me teletransportó hasta mi
  casa". ¿Se sigue uniendo siempre? → A: **No. Se revierte la decisión de la sesión
  anterior.** El trazo se corta en un salto grande y los dos tramos se unen con una línea
  punteada, que dice "acá hubo una desconexión y hay un camino posible en el medio" sin
  afirmar por dónde fue.
- Q: ¿Se puede pegar el trazo a las calles? → A: **No en esta feature.** Requiere map matching
  contra un servicio de ruteo: red obligatoria y una dependencia nueva, las dos rechazadas
  por el Principio III y el IV. La línea punteada es lo honesto sin red.
- Q: La paleta neutra quedó gris y apagada. → A: Se rehace tomando la escala de shadcn/ui:
  neutros fríos, bordes visibles, esquinas redondeadas, poca elevación.
- Q: ¿El mapa tiene un estilo más minimalista o uno oscuro? → A: Sí, los dos. Se elige según
  el modo del sistema.

### Session 2026-08-29 (tercera vuelta)

- Q: `positron` dibuja las calles menores casi invisibles: en la calle solo se veían las
  avenidas. → A: Se revierte a `liberty` en modo claro. Para el modo oscuro se usa `fiord`,
  cuya capa `highway_minor` sí las dibuja con contraste. **Sobrio no puede costar
  información**: el jugador camina por calles, no por avenidas.
- Q: ¿Se puede agregar el servicio de map matching, aunque necesite red? → A: **Sí, y
  revierte el FR-009b.** El recorrido termina en casa, con wifi, así que la red está
  disponible justo cuando hace falta. Sin conexión al terminar, la salida queda pendiente
  hasta que la app abra con red.
- Q: La franja inferior sigue siendo dos filas y el campo queda enorme. → A: Una sola fila:
  `+`, cámara, un botón circular de play para el recorrido, y el campo llevándose el resto.
- Q: ¿Qué muestra el campo al abrir? → A: El número que el juego está pidiendo, leído del
  estado que administra Ajustes, y **seleccionado entero** para que el primer dígito lo
  reemplace. *(Corregido en la vuelta siguiente: ver abajo.)*

### Session 2026-08-29 (cuarta vuelta, detalles)

- Q: El número precargado tenía que ser una sugerencia, no un valor. → A: El campo arranca
  **vacío**, y el placeholder son los tres puntos. Se descarta precargar el número del juego:
  en la práctica era algo para borrar casi siempre.
- Q: Con el teclado abierto queda una franja muerta entre el panel inferior y el teclado. →
  A: Es el inset de la barra de navegación sumado al del teclado. Se toma el mayor de los
  dos, no la suma.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ir hasta una patente que ya tengo guardada (Priority: P1)

El juego llega al número que el jugador capturó hace tres días. Tiene el registro, tiene la
foto, tiene las coordenadas — y no tiene forma de volver al lugar. Hoy la app le muestra un
punto en el mapa y ahí se termina: para llegar hay que mirar el punto, deducir la esquina y
salir a buscar de memoria.

Esta historia cierra el círculo que la 001 abrió cuando decidió guardar la ubicación. Guardar
dónde estaba el auto solo sirve si después se puede volver.

**Why this priority**: es la razón por la que el registro guarda coordenadas. Sin esto, la
ubicación es un dato bonito que no se usa para nada, y el jugador que quiere compartir la
patente del día tiene que resolver a mano lo único que la app tenía que resolverle. Es
también lo único de esta lista que el usuario llamó "esencial".

**Independent Test**: con una patente guardada en una esquina conocida, pedirle a la app que
lleve hasta ella y verificar que da una indicación que alcanza para caminar hasta el lugar.
No necesita ninguna otra historia de esta feature.

**Acceptance Scenarios**:

1. **Given** una patente guardada y el teléfono con conexión, **When** el jugador pide ir
   hasta ella, **Then** el sistema le ofrece la ruta hasta esa ubicación sin que tenga que
   copiar coordenadas a mano.
2. **Given** una patente guardada y el teléfono **sin conexión**, **When** el jugador mira su
   información, **Then** la distancia y la dirección ya están en pantalla, iguales que con
   conexión: no aparece ninguna pantalla de error ni ningún camino distinto.
3. **Given** el jugador parado sobre la ubicación guardada, **When** mira la indicación,
   **Then** la distancia refleja que ya llegó, y no lo manda a seguir caminando.
4. **Given** una patente guardada con precisión degradada, **When** el jugador pide ir hasta
   ella, **Then** el sistema le advierte que la ubicación era poco exacta antes de mandarlo.
5. **Given** el jugador salió a la app de mapas para navegar, **When** vuelve a esta app,
   **Then** la encuentra como la dejó y puede cargar una patente sin volver a empezar nada.

---

### User Story 2 - El recorrido se ve como el camino que caminé (Priority: P2)

El jugador termina una salida, abre el mapa y espera ver por dónde anduvo. Lo que ve son
rectángulos: bloques de aproximadamente una cuadra de lado, pintados de un color fuerte, que
cubren la zona por la que pasó sin parecerse en nada a un recorrido.

Los rectángulos no son un error de dibujo: son exactamente lo que la 001 pidió, una grilla que
responde "¿pasé por acá?". Lo que la salida real mostró es que esa pregunta no era la que el
jugador se hace cuando vuelve a casa. La que se hace es "¿por dónde fui?", y esa se contesta
con el trazo del camino.

**Why this priority**: es la única cosa de la lista que el jugador percibió como rota, y una
funcionalidad que se ve rota erosiona la confianza en el resto de la app. Va después de la
US1 porque no le impide hacer nada: es información que mira después de caminar, no una
herramienta que use mientras camina.

**Independent Test**: hacer una salida de diez minutos por calles conocidas, terminarla y
verificar que el mapa de la pantalla principal dibuja el camino recorrido de forma reconocible
contra las calles reales, y que abriendo esa salida desde la pantalla de Salidas se ve sola.
No necesita ninguna otra historia.

**Acceptance Scenarios**:

1. **Given** un recorrido terminado, **When** el jugador mira el mapa de la pantalla
   principal, **Then** ve el camino que hizo trazado sobre las calles, reconocible como un
   recorrido y no como un área.
2. **Given** varias salidas hechas en días distintos, **When** el jugador mira el mapa de la
   pantalla principal,
   **Then** las ve todas dibujadas, y distingue la que está caminando ahora —o la última, si
   no está caminando— del resto del histórico.
3. **Given** un recorrido en curso, **When** el jugador mira el mapa de la pantalla principal
   mientras camina, **Then** ve el camino que lleva hecho hasta ese momento.
4. **Given** varias salidas guardadas, **When** el jugador abre una desde la pantalla de
   Salidas, **Then** ve **solo** el camino de esa salida, encuadrado y entero.
5. **Given** un recorrido con un salto de señal en el medio —un túnel, un subsuelo—, **When**
   se dibuja el camino, **Then** el trazo sigue siendo continuo: se acepta la línea recta
   entre la última posición antes del corte y la primera de la vuelta.
6. **Given** cualquier zona del mapa, **When** el jugador la mira, **Then** ya no hay
   rectángulos de cobertura dibujados encima.

---

### User Story 3 - Buscar sirve para encontrar, no solo para filtrar (Priority: P3)

El jugador entra a Buscar porque el juego llegó a un número y quiere saber si lo tiene. Hoy la
pantalla no le dice nada hasta que tipea los tres dígitos completos: antes de eso muestra
"Escribí los 3 dígitos" y una pantalla vacía. No sugiere el número que el juego está pidiendo
ahora mismo, no muestra lo último que capturó, y el campo donde hay que escribir está pegado
al borde de arriba, que es donde el pulgar no llega.

**Why this priority**: la pregunta que el jugador trae al abrir esta pantalla es casi siempre
la misma —"¿tengo la que toca?"— y la app ya sabe la respuesta antes de que escriba nada. Va
en P3 porque el indicador de la pantalla principal ya contesta esa pregunta: esto la hace
accionable, no la descubre.

**Independent Test**: abrir Buscar sin escribir nada y verificar que la pantalla ya ofrece
algo útil. Después escribir con una sola mano, sin recolocar el teléfono.

**Acceptance Scenarios**:

1. **Given** el jugador abre Buscar sin escribir nada, **When** mira la pantalla, **Then** ve
   destacada la patente que corresponde al número actual del juego, si la tiene guardada.
2. **Given** el jugador abre Buscar sin escribir nada, **When** mira la pantalla, **Then** ve
   sus capturas más recientes, en lugar de una pantalla vacía.
3. **Given** el jugador empieza a escribir un número incompleto, **When** lleva uno o dos
   dígitos, **Then** la pantalla ya va acotando lo que muestra en lugar de esperar al tercero.
4. **Given** el jugador sostiene el teléfono con una sola mano, **When** quiere escribir en el
   campo de búsqueda, **Then** llega con el pulgar sin recolocar el teléfono.

---

### User Story 4 - La app se ve como una herramienta y los controles se leen (Priority: P4)

La paleta actual tiñe de violeta buena parte de la interfaz: los botones de navegación, las
tarjetas, los fondos de los controles. El campo donde se escribe el número es transparente
sobre el mapa, así que lo que se lee detrás cambia según por dónde ande el jugador. La franja
inferior de acciones ocupa más alto del necesario y no tiene fondo propio, así que los botones
compiten con las calles. Y la barra de navegación del sistema queda blanca con iconos blancos:
literalmente invisible.

**Why this priority**: nada de esto impide hacer algo, salvo la barra del sistema, que sí deja
controles sin ver. Va última por la misma razón que la historia de aspecto de las dos specs
anteriores: es lo primero que se recorta si hay que recortar.

**Independent Test**: mostrar la pantalla principal en modo claro y en modo oscuro, y
verificar que todos los controles se leen contra cualquier zona del mapa, incluida la barra
del sistema.

**Acceptance Scenarios**:

1. **Given** el teléfono en modo claro, **When** el jugador mira la barra de navegación del
   sistema, **Then** sus controles se distinguen del fondo. Lo mismo en modo oscuro.
2. **Given** la app abierta sobre cualquier zona del mapa, **When** el jugador mira el campo
   de número, **Then** lo que escribió se lee sin que el fondo del mapa interfiera.
3. **Given** la pantalla principal, **When** el jugador mira la franja de acciones de abajo,
   **Then** los botones se distinguen del mapa y la franja ocupa menos alto que hoy, sin que
   ningún botón se vuelva más difícil de acertar.
4. **Given** cualquier pantalla de la app, **When** el jugador la mira, **Then** los colores
   de fondo y de los controles son neutros, y el color fuerte queda reservado para lo que
   tiene significado.
5. **Given** el mapa con patentes guardadas, **When** el jugador lo mira, **Then** sigue
   distinguiendo pendiente, ya toca y compartida, como fija el FR-011 de la 002.

---

### Edge Cases

- ¿Qué pasa si el jugador pide ir hasta una patente y el teléfono no tiene ninguna app de
  mapas instalada? El sistema tiene que decirlo, no quedarse mudo ni cerrarse.
- ¿Qué pasa si pide ir hasta una patente sin permiso de ubicación otorgado? La ubicación del
  registro se conoce igual; lo que falta es de dónde sale el jugador, así que la distancia se
  declara desconocida y el destino se puede abrir lo mismo (FR-003a).
- ¿Qué pasa con un recorrido de un solo punto —empezado y terminado sin moverse? No hay camino
  que trazar.
- ¿Qué pasa cuando el jugador se para diez minutos en un lugar? No genera ningún hueco: el
  trazo es continuo por definición (FR-009), así que la pausa no se dibuja de ninguna forma
  especial.
- ¿Qué pasa con un recorrido muy largo, de horas? El camino tiene que seguir dibujándose sin
  que el mapa se vuelva lento al desplazarlo.
- ¿Qué pasa cuando dos recorridos pasan por la misma calle? Los dos tienen que seguir
  distinguiéndose.
- ¿Qué pasa al abrir desde Salidas una salida que quedó sin puntos —empezada y cortada sin
  moverse? Se dice que no hay camino que mostrar (FR-010b).
- ¿Qué pasa en Buscar cuando el jugador todavía no capturó nada? Los "más recientes" no
  existen, y el vacío se dice.
- ¿Qué pasa si el número actual del juego no está guardado? La sugerencia tiene que decir que
  falta, no desaparecer sin explicación.

## Requirements *(mandatory)*

### Functional Requirements

#### Llegar hasta una patente guardada

- **FR-001**: Los usuarios MUST poder pedir, desde la información de una patente guardada, que
  el sistema los lleve hasta el lugar donde fue capturada. El trayecto lo MUST resolver la app
  de mapas que el jugador ya tenga en el teléfono: esta app entrega el destino y no calcula
  rutas por su cuenta.
- **FR-001a**: Si el teléfono no tiene ninguna app capaz de recibir el destino, el sistema
  MUST decirlo. No hace falta ningún respaldo adicional: la distancia y la dirección del
  FR-003 ya están a la vista.
- **FR-002**: El sistema MUST ofrecer esa acción tanto desde el mapa como desde los resultados
  de búsqueda, que son los dos lugares donde el jugador se encuentra con una patente.
- **FR-003**: La información de una patente guardada MUST mostrar **siempre** a qué distancia
  y en qué dirección está respecto de la posición actual del jugador — no solo cuando falla
  algo. Con conexión sirve para decidir si vale la pena ir; sin conexión es lo único que hay,
  y ya estaba en pantalla.
- **FR-003a**: Si no se conoce la posición actual del jugador —sin permiso de ubicación, o sin
  lectura todavía—, el sistema MUST decir que no puede calcular la distancia, en lugar de
  mostrar un número inventado. La ubicación de la patente se sigue conociendo y el destino se
  sigue pudiendo abrir.
- **FR-004**: Cuando la ubicación guardada tiene precisión degradada, el sistema MUST
  advertirlo antes de mandar al jugador hasta ahí.
- **FR-005**: Esta funcionalidad MUST NOT introducir ninguna dependencia con el camino de
  captura ni demorarlo, en línea con el Principio III y con el FR-009 de la 002.

#### El recorrido dibujado

- **FR-006**: El sistema MUST dibujar cada recorrido como el trazo del camino efectivamente
  recorrido, siguiendo las calles por donde pasó el jugador.
- **FR-006a**: El mapa de la pantalla principal MUST mostrar el histórico completo, en el
  mismo lugar donde se dibujaba la grilla que el FR-021 retira.
- **FR-007**: El mapa MUST dibujar **todos** los recorridos guardados, no solo el último: al
  retirarse la grilla de cobertura, el trazo queda como la única respuesta a "¿por dónde ya
  anduve?".
- **FR-007a**: El recorrido en curso —o el más reciente, si no hay ninguno en curso— MUST
  destacarse sobre el resto del histórico, de modo que el jugador distinga lo que está
  caminando ahora de lo que ya caminó.
- **FR-008**: El recorrido en curso MUST dibujarse mientras se camina, no solo al terminarlo.
- **FR-009**: Cuando dos posiciones consecutivas están separadas por un salto que no puede
  explicarse caminando, el trazo MUST cortarse en dos tramos en lugar de unirlas con una
  recta continua.

  **Reescrito después de la segunda salida.** La versión anterior de este requisito decía lo
  contrario —unir siempre, aceptar la recta— y se eligió para no tener que fijar un umbral.
  En uso real el resultado fue que un corte de señal "teletransportó" el recorrido hasta la
  casa del jugador, cruzando en línea recta manzanas por las que nunca pasó. El error no era
  cosmético: el trazo afirmaba un camino falso.
- **FR-009a**: Los dos tramos a cada lado de un corte MUST unirse con una línea **punteada**,
  que indica que hubo una desconexión y que existe algún camino entre los dos extremos, sin
  afirmar cuál fue.
- **FR-009b**: ~~El sistema MUST NOT intentar pegar el trazo a las calles.~~ **Superado por
  el FR-031.** El argumento de este requisito era que el map matching exige red y una
  dependencia nueva. Lo primero es cierto y resultó aceptable —el recorrido termina en casa,
  con wifi— y lo segundo no lo es: el servicio se consume con el cliente HTTP de la
  plataforma, sin sumar nada. El corte y la línea punteada siguen vigentes para el trazo
  crudo, que es lo que se dibuja mientras el ajuste está pendiente.

#### El camino ajustado a las calles

- **FR-030**: El campo de número MUST arrancar **vacío**, con un placeholder que ocupe el
  lugar de los tres dígitos sin ser un valor.

  **Reescrito.** La primera versión precargaba el número que el juego está pidiendo, con la
  selección puesta. Probado en uso, el número precargado resultó ser algo para borrar casi
  siempre: la mayoría de las patentes que el jugador captura no son la que el juego pide.
- **FR-031**: El sistema MUST poder ajustar el camino de una salida terminada a las calles
  por las que se pudo haber ido, y MUST dibujar ese camino ajustado en lugar del crudo cuando
  esté disponible.
- **FR-032**: Cuando no hay conexión al terminar una salida, el ajuste MUST quedar pendiente y
  MUST reintentarse solo cuando la app vuelva a tener red, sin que el jugador tenga que pedir
  nada.
- **FR-033**: Mientras una salida está pendiente de ajuste, su camino crudo MUST seguir
  dibujándose. Un recorrido sin ajustar no puede verse como un recorrido que no existe.
- **FR-034**: El ajuste MUST NOT modificar ni descartar los puntos de trayecto guardados. Son
  lo que el teléfono midió; el camino ajustado es una interpretación de esos puntos, vive
  aparte y se puede recalcular.
- **FR-035**: El ajuste MUST NOT bloquear ni demorar ninguna pantalla. Si el servicio no
  responde, la app se comporta como si nunca se hubiera intentado.

#### El muestreo del recorrido

- **FR-036**: El registro del trayecto MUST usar la precisión máxima del teléfono y un
  muestreo lo bastante fino como para que una esquina caiga entre dos puntos: en el orden de
  un punto cada 5 segundos o cada 10 metros. El sistema MUST descartar los puntos que llegan
  con una precisión peor que la de un cuarto de manzana, en vez de guardarlos y dibujarlos.

  **Encontrado validando la US2 en la calle.** Con el muestreo anterior —prioridad
  balanceada, 30 segundos, 50 metros, sin filtro de precisión— el trazo en vivo salió lleno
  de diagonales que no correspondían a ninguna calle, y desplazado: la prioridad balanceada
  resuelve por antenas y wifi con decenas de metros de error, y una manzana mide alrededor de
  cien. El ajuste a calles del FR-031 mejoró el dibujo pero no lo arregló, y no podía: sobre
  puntos desplazados y separados, el servicio elige el camino peatonal más barato entre uno y
  el siguiente, así que una calle por la que sí se pasó puede no quedar dibujada. El map
  matching corrige el ruido de una medición buena; no adivina la medición que falta.
- **FR-036a**: El ajuste a calles MUST decirle al servicio cuánta confianza merece cada
  punto, usando la precisión que se guardó con él. Un punto medido con 5 metros de error y
  uno con 25 no pueden pegarse a la calle con la misma seguridad.

#### La confianza en que la patente siga estando

- **FR-041**: Desde la ficha de una patente, los usuarios MUST poder decir si la volvieron a
  ver en el lugar o si pasaron y ya no estaba. Cada opinión MUST guardarse aparte del
  registro: es lo que el jugador **opina** después, no lo que el teléfono midió, y el
  Principio II protege lo segundo. Una opinión emitida MUST NOT editarse ni retirarse — si el
  jugador se arrepiente, opina al revés y la cuenta lo absorbe.
- **FR-041a**: De esas opiniones el sistema MUST derivar una probabilidad de que la patente
  siga estando, en una escala acotada. Una patente sobre la que nadie opinó MUST quedar en el
  valor neutro: verla una vez no dice nada sobre su permanencia. Cada opinión MUST moverla un
  escalón y recortarse **en cada paso**, no al final: diez "sigue estando" seguidos de un "ya
  no estaba" tienen que bajar un escalón, no quedarse arriba hasta que el jugador opine nueve
  veces en contra. Lo último que se vio pesa, que es para lo que sirve.
- **FR-041b**: Borrar una patente MUST llevarse sus opiniones. Son sobre un registro que ya no
  existe.

  **Escritos después, y por eso van fuera de orden.** Los tres requisitos de arriba
  describen algo que se construyó sin haberse pedido: el FR-037 fue escrito dando por sentado
  que la probabilidad "ya se calcula a partir de los votos", cuando en realidad la forma de
  votar, la escala y su persistencia nacieron en la misma vuelta. La convergencia lo encontró
  como trabajo sin requisito. Se documenta en vez de retirarse porque el FR-037 y el FR-037c
  dependen de él, la migración ya está instalada en el teléfono de pruebas, y lo que faltaba
  era la spec, no el código.

- **FR-037**: El mapa MUST mostrar en cada marcador qué tan probable es que esa patente siga
  estando donde se la vio, con color y sin que el jugador tenga que abrir la ficha. La
  probabilidad sale de las opiniones del FR-041a; hasta este requisito solo se leía de a una,
  abriendo la ficha.
- **FR-037a**: Ese color MUST NOT reemplazar al que distingue los tres estados del FR-020
  —pendiente, ya toca, compartida—, que responden una pregunta distinta y anterior.
- **FR-037b**: ~~Una patente sobre la que nadie votó MUST verse igual que antes de existir
  este requisito.~~ **Reescrito después de verlo en el teléfono.** El color MUST usar tres
  escalones netos —bajo, medio, alto— y no un degradado continuo. El degradado no se leía: el
  color de un marcador solo se puede juzgar contra otro que esté a la vista, y entre valores
  vecinos la diferencia era un tono. La probabilidad neutra cae en el escalón del medio, junto
  con los votos que se contradicen: "todavía no se sabe" y "hay opiniones cruzadas" dicen lo
  mismo al que decide si caminar hasta ahí.
- **FR-037c**: Votar desde la ficha MUST actualizar el marcador del mapa que está detrás. El
  número de la ficha y el color del marcador salen del mismo dato y no pueden discrepar.

#### El mapa se maneja con una sola mano

- **FR-038**: Un grupo de patentes MUST poder abrirse acercando el mapa. Un grupo que no se
  parte a ningún zoom esconde registros sin ninguna forma de llegar a ellos.
- **FR-039**: El mapa MUST tener un techo de acercamiento. Pasado el detalle que el estilo
  publica, seguir acercando no agrega información y el mapa se vuelve pesado al desplazarlo.
- **FR-040**: El mapa MUST NOT rotarse. El norte arriba es información en esta app —el rumbo
  a una patente se dice en puntos cardinales, FR-003— y la rotación se disparaba sola con el
  gesto de zoom, dejando el mapa torcido en la mano del que camina.
- **FR-010**: El dibujo del recorrido MUST NOT degradar la fluidez del mapa al desplazarlo o
  hacer zoom.
- **FR-010a**: Desde la pantalla de Salidas, los usuarios MUST poder abrir una salida y ver
  **solo ese** recorrido sobre un mapa, encuadrado de modo que se vea entero sin tener que
  buscarlo.
- **FR-010b**: La pantalla de Salidas MUST seguir siendo utilizable cuando una salida no tiene
  ningún punto guardado: se dice que no hay camino que mostrar, no se abre un mapa vacío.

#### Borrar una salida

- **FR-024**: Los usuarios MUST poder borrar una salida desde la pantalla de Salidas, con
  confirmación previa.
- **FR-025**: Borrar una salida MUST llevarse su trayecto, y MUST NOT borrar ninguna patente
  capturada durante ella. La salida es un registro de actividad; la patente es evidencia, y el
  Principio II la protege aunque se borre el recorrido que la rodeaba.
- **FR-026**: Si la salida borrada estaba en curso, el sistema MUST detenerla antes de
  borrarla, en lugar de dejar un servicio grabando contra un recorrido que ya no existe.

#### El fondo del mapa

- **FR-027**: El fondo del mapa MUST acompañar el modo claro u oscuro del sistema, igual que
  el resto de la app.
- **FR-028**: El fondo MUST ser visualmente sobrio, de modo que los marcadores y el trazo se
  lean encima. El mapa es contexto; lo que el jugador tiene que ver son sus patentes.
- **FR-029**: Los colores con significado —los tres estados de una patente, el trazo, la zona
  sin fondo guardado— MUST distinguirse sobre los dos fondos.

#### Buscar

- **FR-011**: Al abrir la pantalla de búsqueda sin escribir nada, el sistema MUST destacar la
  patente que corresponde al número actual del juego, si el jugador la tiene guardada, e
  indicar explícitamente que falta si no la tiene.
- **FR-012**: Al abrir la pantalla de búsqueda sin escribir nada, el sistema MUST mostrar las
  capturas más recientes.
- **FR-013**: El sistema MUST ir acotando los resultados a medida que el jugador escribe, sin
  esperar a que complete los tres dígitos.
- **FR-014**: El campo de búsqueda MUST quedar al alcance del pulgar sosteniendo el teléfono
  con una sola mano, igual que exige el FR-026 de la 002 para la pantalla principal.

#### Aspecto y contraste

- **FR-015**: La barra de navegación del sistema MUST mostrar sus controles con contraste
  suficiente contra el fondo de la app, en modo claro y en modo oscuro.
- **FR-016**: Los fondos y los controles de la app MUST usar una paleta neutra. El color
  saturado MUST quedar reservado para lo que tiene significado: el estado de una patente en el
  mapa y la acción principal.
- **FR-016a**: "Neutra" MUST NOT significar apagada. La primera implementación quedó gris y
  sin definición, que es un problema distinto del violeta pero igual de visible. La paleta
  MUST tener contraste suficiente entre superficies contiguas, bordes que se vean, y una
  jerarquía clara entre lo que se toca y lo que se lee.
- **FR-017**: El campo de número de la pantalla principal MUST tener fondo propio, de modo que
  lo escrito se lea contra cualquier zona del mapa.
- **FR-017a**: El campo MUST ocupar el espacio que tres dígitos necesitan y no más. Ni a lo
  ancho de la pantalla, ni con la altura de un campo de formulario largo.
- **FR-018**: La franja de acciones inferior MUST ocupar menos alto que hoy y MUST tener fondo
  propio que la separe del mapa.
- **FR-018a**: Los controles de captura y el campo de número MUST vivir en **una sola fila**:
  guardar, guardar con foto, empezar o terminar el recorrido, y el campo llevándose el ancho
  que sobra. El control del recorrido MUST ser un botón de icono, no un botón ancho con
  texto: se usa dos veces por salida y ocupaba media franja.
- **FR-019**: Achicar la franja de acciones MUST NOT hacer que ningún botón sea más difícil de
  acertar con el pulgar.
- **FR-018b**: Con el teclado abierto, el sistema MUST NOT dejar espacio muerto entre la
  franja de acciones y el teclado. El inset del teclado se mide desde el borde de la pantalla
  y ya contiene al de la barra de navegación: corresponde el mayor de los dos, nunca la suma.
- **FR-019a**: Los iconos de los botones de acción MUST escalar con el botón que los contiene.
  Al achicar la franja, el icono de la cámara quedó desproporcionadamente chico dentro de su
  botón: el botón cambió de tamaño y el icono no.
- **FR-020**: Los tres estados de una patente en el mapa —pendiente, ya toca, compartida—
  MUST seguir distinguiéndose, conservando lo que fija el FR-011 de la 002.

#### Continuidad con lo ya especificado

- **FR-021**: El trazo del recorrido MUST reemplazar a la grilla de cobertura de calles: la
  grilla MUST dejar de dibujarse sobre el mapa.
- **FR-021a**: El FR-032 y el SC-015 de la especificación 001 MUST quedar anotados como
  superados por esta especificación, del mismo modo en que la 002 anotó su FR-016 y su
  SC-002. La pregunta que la grilla contestaba —"¿pasé por acá?"— la contesta mejor el
  trazo, que además dice por dónde.
- **FR-022**: Ningún cambio de esta especificación MUST alterar la evidencia de un registro
  ya guardado, según el Principio II.

### Key Entities

Esta especificación no crea entidades nuevas. Trabaja sobre las que definió la 001:

- **Registro de captura**: gana un uso nuevo —ser destino de un trayecto— sin ganar ningún
  campo. La ubicación que ya guarda es todo lo que hace falta.
- **Recorrido** y **Punto de trayecto**: dejan de leerse solo como insumo de la grilla de
  cobertura y pasan a leerse como lo que siempre fueron, la sucesión de posiciones de una
  salida. La agrupación por recorrido y el momento de cada punto ya están guardados.
- **Estado del juego**: su número actual pasa a alimentar también la sugerencia de la pantalla
  de búsqueda.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Desde que el jugador ve una patente guardada hasta que tiene una indicación
  utilizable para llegar hasta ella, no pasan más de 2 interacciones.
- **SC-002**: Con el teléfono sin conexión, el 100% de las patentes guardadas siguen dando una
  orientación utilizable —distancia y dirección— en lugar de un error.
- **SC-003**: Después de una salida de 10 minutos por calles conocidas, el jugador reconoce en
  el mapa el camino que hizo, y puede señalar por qué calles pasó.
- **SC-004**: Con tres salidas guardadas y una cuarta en curso, el jugador señala en el mapa
  cuál es la que está caminando sin consultar ninguna otra pantalla.
- **SC-005**: El mapa con un recorrido de una hora dibujado se desplaza y hace zoom sin tirones
  perceptibles.
- **SC-005a**: Desde la pantalla de Salidas, el jugador ve el camino de una salida puntual en
  1 interacción, y lo ve entero sin desplazar ni hacer zoom a mano.
- **SC-006**: Al abrir la pantalla de búsqueda, el jugador sabe si tiene la patente del número
  actual sin escribir un solo dígito.
- **SC-007**: El jugador completa una búsqueda sosteniendo el teléfono con una sola mano, sin
  recolocarlo.
- **SC-008**: Los controles de la barra de navegación del sistema se distinguen del fondo en
  los dos modos, verificado mirando el teléfono.
- **SC-009**: Todos los controles de la pantalla principal se leen sobre cualquier zona del
  mapa —parque, avenida, zona sin fondo guardado— en los dos modos.
- **SC-010**: La franja de acciones inferior ocupa menos alto que en la 002, y una carga
  rápida completa sigue entrando en 4 interacciones y 10 segundos.

## Assumptions

- **El trayecto lo resuelve la app de mapas del teléfono.** Decidido explícitamente: esta app
  entrega el destino y no calcula rutas. Dibujar el camino adentro exigiría un servicio de
  ruteo —red obligatoria y una dependencia nueva— para hacer peor lo que Maps ya hace bien, y
  el Principio IV manda mirar primero lo que ya está instalado.
- **El jugador tiene una app de mapas instalada.** Es el caso normal en un teléfono Android
  con Play Services, que la 001 ya asumió para la ubicación. El caso contrario se cubre
  avisando y cayendo en la orientación propia, no construyendo un motor de ruteo.
- **La grilla de cobertura se retira, no se conserva apagada.** Decidido explícitamente. Se
  descartó dejarla debajo del trazo —vuelve el problema de legibilidad que motivó esta
  spec— y se descartó hacerla apagable desde Ajustes, que es la clase de opción que nadie
  pidió y que la 002 ya rechazó para el tema.
- **La ruta hasta una patente es una función accesoria, no del camino de captura.** El
  Principio III permite red para funciones accesorias; lo que no se permite es que la captura
  dependa de ella. Por eso FR-003 exige que sin conexión quede algo utilizable.
- **La orientación es en línea recta.** Distancia y dirección desde la posición actual, sin
  calles. Es lo honesto sin red, y en una zona conocida alcanza para caminar. Se muestra
  siempre y no solo al fallar algo, para no construir un camino de excepción que sería el
  menos probado de la app.
- **Los puntos de trayecto ya guardados sirven para dibujar el camino.** La 001 los guarda
  agrupados por recorrido y con el momento de cada uno, así que esta feature no necesita
  capturar nada nuevo ni migrar nada.
- **"Neutro" significa fondos y superficies sin tinte de color**, con el color reservado para
  el estado de una patente y para la acción principal. No implica monocromo: el mapa aporta su
  propio color y los tres estados tienen que seguir distinguiéndose.
- **No se agrega un selector de tema ni de paleta.** Se sigue el modo del sistema, como decidió
  la 002 contra el Principio IV.
- **La app pasa a tener dos superficies de mapa**, la principal y la de una salida puntual.
  El Principio IV pide dos usos reales antes de que exista una abstracción compartida, y acá
  los hay: es justamente el caso que el principio admite, no el que descarta.
- **El trazo no interpreta los datos, los une.** No hay umbral de corte, ni relleno, ni
  suavizado: se dibuja la sucesión de posiciones tal como quedó guardada. Es la lectura más
  honesta de lo que la app efectivamente midió, y la que no exige ninguna constante que haya
  que calibrar. La contrapartida —una recta sobre un tramo sin señal— está aceptada en el
  FR-009.
- **El juego lo sigue llevando el jugador a mano.** Nada de esta feature toca el número actual
  ni intenta deducirlo.
