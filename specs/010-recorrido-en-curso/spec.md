# Feature Specification: Recorrido en curso

**Feature Branch**: `010-recorrido-en-curso`

**Created**: 2026-10-17

**Status**: Draft

**Input**: User description: "Crea un nuevo spec donde se muestre el tiempo del recorrido comenzado en la barra superior. Que se muestre un mensaje de confirmación al terminar un recorrido para no terminarlo sin querer. Que se muestre un aproximado de calorías quemadas en un recorrido y cuántos km se recorrió. Investigar si es posible que la app siga funcionando en el background incluso cuando está cerrada (eliminada desde lo que creo que se llama el administrador de tareas) si hay un recorrido abierto. Actualmente si estás en un recorrido y la cerrás sin querer se corta el recorrido."

## Contexto

Una salida se graba desde que el jugador toca el botón de recorrido hasta que lo vuelve a tocar.
Hoy pasan tres cosas que le juegan en contra en la calle:

- **Un toque de más la termina.** El mismo botón empieza y termina, y está en la fila de la carga
  rápida, donde se toca todo el tiempo.
- **No se ve cuánto lleva.** Ni el tiempo, ni la distancia, ni nada que diga "esto está grabando"
  fuera del ícono del botón.
- **Una salida cortada no avisa.** Si la app se cierra, la salida deja de grabar en silencio, y el
  jugador se entera recién cuando vuelve a abrirla.

La 004 dejó la pantalla de Salidas sin ningún total acumulado, ni de distancia (su FR-020a): "qué
calles recorrí" lo contesta el mapa, y un contador sería una respuesta más pobre. Esta feature
contesta otras preguntas, "cuánto caminé" y "cuánto gasté", y las contesta así: cada fila de la lista
dice los km **de esa salida**, y los totales por día, semana y mes viven en una pantalla aparte, la
de estadísticas. La lista sigue sin totales arriba.

### Lo que se averiguó sobre seguir grabando con la app cerrada

El jugador pidió averiguar si una salida puede seguir grabando cuando la app se cierra desde el
selector de apps. Hoy se corta: al abrirla otra vez, la salida se cierra con lo que tenía y la
pantalla dice "La salida se cortó" (FR-028 de la 006).

- **iPhone, la app cerrada por el jugador**: no se puede. Cuando el jugador la saca deslizándola,
  el sistema no la vuelve a abrir para darle ubicaciones hasta que el jugador la abre a mano. Es una
  regla del sistema, y desarrolladores reportan que en iOS 26 se cumple también para los avisos de
  ubicación que antes sí la reabrían.
- **iPhone, la app cerrada por el sistema** (para liberar memoria, con la pantalla apagada): se
  podría. Con las APIs de ubicación de iOS 17 en adelante, el sistema la vuelve a abrir en segundo
  plano, y la grabación sigue si la app la retoma apenas arranca.
- **Android**: la grabación es un servicio en primer plano con su notificación, y sacar la app de
  recientes no lo detiene en un Android de fábrica. Algunas capas de fabricante, como HyperOS, matan
  todo igual.

Lo único que resolvería el caso del jugador sería retomar la misma salida al volver a abrir la app.
El jugador decidió que no vale la pena: si se cierra, la salida se corta como hasta ahora, y empieza
otra. Lo que sí pide es **enterarse en el momento**, con una notificación, para empezar otra enseguida
y no caminar media hora sin grabar.

## Clarifications

### Session 2026-10-17

- Q: ¿De dónde sale el peso para las calorías? → A: El jugador lo carga en Ajustes. Mientras no lo
  cargue, la app no muestra calorías: ningún número inventado.
- Q: ¿La barra de arriba muestra también las calorías en vivo? → A: Sí: tiempo, km y calorías.
- Q: ¿Qué hace la app si se cierra con una salida en curso? → A: Lo mismo que hoy: la salida se
  corta, y el jugador empieza otra. No hay forma buena de evitarlo en el iPhone, y retomarla no le
  interesa. Sale del alcance todo lo de seguir grabando o retomar.
- Q: ¿Se avisa cuando la salida se corta sin que el jugador la termine? → A: Sí, con una
  notificación, por ejemplo al cerrar la app deslizándola.
- Q: ¿Las calorías son solo las de caminar o el total, con lo que el cuerpo gasta en reposo? → A:
  Solo las activas, las de caminar. Salen de los km y el peso: estar quieto no suma.
- Q: ¿Las salidas ya terminadas van a tener calorías? → A: Sí, todas, también las de antes de esta
  feature: los km y las calorías se calculan con los puntos guardados (FR-010).
- Q: ¿Cómo se ven las estadísticas por día, semana y mes? → A: Los totales del período elegido, con
  flechas para ir al anterior, y abajo un gráfico de barras de km de los últimos 7 días, 8 semanas
  o 12 meses, según la pestaña.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - No terminar la salida sin querer (Priority: P1)

El jugador toca el botón de recorrido con una salida en curso. Antes de terminarla, la app pregunta.

**Why this priority**: es el único modo de perder una salida que esta feature puede evitar, y el más
fácil de arreglar.

**Independent Test**: con una salida en curso, tocar el botón y cancelar; tocarlo otra vez y
confirmar.

**Acceptance Scenarios**:

1. **Given** una salida en curso, **When** el jugador toca el botón de recorrido, **Then** aparece
   una confirmación con cuánto lleva la salida, y la salida sigue grabando mientras está abierta.
2. **Given** la confirmación abierta, **When** el jugador cancela o toca afuera, **Then** la salida
   sigue en curso.
3. **Given** la confirmación abierta, **When** el jugador confirma, **Then** la salida termina como
   hasta ahora.
4. **Given** sin salida en curso, **When** el jugador toca el botón, **Then** la salida empieza sin
   preguntar nada.
5. **Given** una salida en curso, **When** el jugador la borra desde Salidas, **Then** pregunta
   como hoy: borrar ya tiene su confirmación, y en Salidas no hay otra forma de terminarla.

---

### User Story 2 - Enterarse de que la salida se cortó (Priority: P1)

El jugador cierra la app sin querer en medio de una salida. Al rato, el teléfono muestra una
notificación: la salida se cortó, y abriendo la app se empieza otra.

**Why this priority**: es lo que evita caminar sin grabar, ahora que una salida cortada no se retoma.

**Independent Test**: empezar una salida, cerrar la app desde el selector de apps y esperar con el
teléfono bloqueado.

**Acceptance Scenarios**:

1. **Given** una salida en curso, **When** el jugador cierra la app deslizándola, **Then** en menos
   de 2 minutos aparece una notificación que dice que la salida se cortó y que abriendo la app se
   empieza otra.
2. **Given** esa notificación, **When** el jugador la toca, **Then** se abre la app en la pantalla
   principal, con la salida cortada guardada y lista para empezar otra.
3. **Given** una salida en curso, **When** el sistema cierra la app por su cuenta, **Then** aparece
   la misma notificación.
4. **Given** una salida en curso, **When** el jugador la termina con el botón, **Then** no aparece
   ninguna notificación.
5. **Given** una salida en curso con el jugador quieto, mirando el mapa o parado en una esquina,
   **When** pasan 10 minutos, **Then** no aparece ninguna notificación: la salida no se cortó.

---

### User Story 3 - Ver cuánto lleva la salida (Priority: P2)

Mientras hay una salida en curso, la barra de arriba de la pantalla principal muestra cuánto tiempo
lleva, cuántos km se caminaron y, con el peso cargado, las calorías aproximadas. Se actualiza sola.

**Why this priority**: dice de un vistazo que se está grabando, y es lo que el jugador mira para
decidir si sigue o vuelve.

**Independent Test**: empezar una salida y mirar la barra durante unos minutos de caminata.

**Acceptance Scenarios**:

1. **Given** sin salida en curso, **When** se mira la barra, **Then** se ve como hoy.
2. **Given** una salida empezada hace 42 minutos y el peso cargado, **When** se mira la barra,
   **Then** dice "0:42", los km caminados con un decimal y las calorías aproximadas, por ejemplo
   "0:42 · 2,1 km · ~75 kcal".
3. **Given** la misma salida sin el peso cargado, **When** se mira la barra, **Then** dice "0:42 ·
   2,1 km", sin calorías.
4. **Given** la salida en curso, **When** pasa un minuto, **Then** el tiempo sube solo, sin tocar
   nada.

---

### User Story 4 - Saber cuánto se caminó y cuánto se gastó (Priority: P3)

Cada salida dice cuántos km se caminaron y, con el peso cargado en Ajustes, un aproximado de las
calorías gastadas, en la lista de Salidas y en su detalle.

**Why this priority**: es un dato lindo de tener, pero no cambia cómo se juega.

**Independent Test**: terminar una salida de distancia conocida y mirarla en Salidas.

**Acceptance Scenarios**:

1. **Given** una salida terminada y el peso cargado, **When** se mira en la lista de Salidas,
   **Then** dice su duración, sus km, sus calorías aproximadas y sus patentes.
2. **Given** una caminata de 2 km medidos en un mapa, **When** se mira la salida, **Then** dice
   entre 1,8 y 2,3 km.
3. **Given** la salida en curso, **When** se mira la confirmación de terminar (US1), **Then** dice
   también los km y las calorías.
4. **Given** el peso del jugador, **When** cambia, **Then** las calorías de todas las salidas se
   recalculan con el peso nuevo.
5. **Given** sin peso cargado, **When** se mira una salida, **Then** dice sus km y no dice calorías.
   El detalle de la salida dice que se ven cargando el peso en Ajustes.

---

### User Story 5 - Ver cuánto se caminó por día, semana y mes (Priority: P3)

Desde Salidas, el jugador abre las estadísticas. Elige día, semana o mes, y ve los totales del
período: cuántas salidas hizo, cuánto tiempo caminó, cuántos km, cuántas calorías y cuántas patentes
anotó. Con las flechas va al período anterior. Abajo, un gráfico de barras compara los km de ese
período con los anteriores.

**Why this priority**: como los km y las calorías, es un dato lindo que no cambia cómo se juega. Se
apoya en lo de la US4.

**Independent Test**: con salidas de varios días guardadas, abrir las estadísticas y recorrer las
tres pestañas y las flechas.

**Acceptance Scenarios**:

1. **Given** dos salidas hoy, de 1,5 y 2 km, **When** el jugador abre las estadísticas en "día",
   **Then** ve 2 salidas, el tiempo sumado de las dos, 3,5 km, las calorías (con el peso cargado) y
   las patentes anotadas hoy.
2. **Given** la pestaña "semana", **When** se mira, **Then** suma de lunes a domingo de la semana
   actual, y la flecha lleva a la semana anterior.
3. **Given** la pestaña "mes", **When** se mira, **Then** suma el mes calendario actual.
4. **Given** cualquier pestaña, **When** se mira el gráfico, **Then** hay una barra de km por cada
   uno de los últimos 7 días, 8 semanas o 12 meses, terminando en el período elegido, que se ve
   resaltado.
5. **Given** un período sin salidas, **When** se mira, **Then** muestra todo en cero, sin error.
6. **Given** el período actual, **When** el jugador busca la flecha hacia adelante, **Then** no
   puede ir a un período que todavía no pasó.

---

### Edge Cases

- **Una salida sin puntos o con uno solo**: 0,0 km y 0 kcal.
- **Una salida larga y quieta**, de mucho tiempo y pocos metros, tiene pocas calorías: las activas no
  suman por estar parado.
- **Saltos del GPS**: un punto que cae lejos y vuelve no suma km. Lo que la precisión ya descarta
  (FR-036 de la 003) no cuenta.
- **El hueco de una desconexión** no suma km: no se sabe por dónde se fue (FR-009a de la 004).
- **Una salida cortada** porque se cerró la app tiene los km y las calorías de lo que alcanzó a
  grabar, hasta su último punto.
- **Las salidas viejas**, de antes de esta feature, también muestran sus km y calorías: salen de los
  puntos que ya tienen.
- **Un peso fuera de lo razonable** (0, negativo, o más de 300 kg) no se acepta.
- **Sin permiso de notificaciones**: no hay aviso, y al abrir la app la pantalla principal dice que la
  salida se cortó, como hoy.
- **En el Android** sacar la app de recientes no suele cortar la salida (Contexto). Si el teléfono
  mata el proceso igual, el aviso es el mismo que en el iPhone.
- **Una salida que cruza la medianoche** cuenta entera en el día en que empezó, y lo mismo para la
  semana y el mes.
- **La salida en curso** cuenta en las estadísticas con lo que lleva hasta ahora.
- **Las patentes anotadas fuera de una salida** también cuentan en su día: son patentes del jugador,
  estén o no en una salida.
- **El teléfono se apaga o se queda sin batería** con una salida en curso: no hay forma de avisar en
  el momento. Al abrir la app, la pantalla principal lo dice, como hoy.

## Requirements *(mandatory)*

### Functional Requirements

**Terminar con confirmación**

- **FR-001**: Terminar una salida en curso desde la pantalla principal MUST pedir confirmación. La
  confirmación MUST decir cuánto lleva la salida: el tiempo, los km y, con el peso cargado, las
  calorías. Borrarla desde Salidas sigue con la confirmación que ya tiene.
- **FR-002**: Mientras la confirmación está abierta, la salida MUST seguir grabando. Cancelar o tocar
  afuera MUST dejarla como estaba.
- **FR-003**: Empezar una salida MUST NOT pedir confirmación.

**La salida en la barra de arriba**

- **FR-004**: Con una salida en curso, la barra de arriba de la pantalla principal MUST mostrar el
  tiempo que lleva (horas y minutos), los km caminados con un decimal y, con el peso cargado, las
  calorías aproximadas. Sin salida en curso, la barra MUST verse como hoy.
- **FR-005**: El tiempo MUST actualizarse solo, al menos una vez por minuto, y los km y las calorías
  con cada punto que se graba.
- **FR-006**: Lo que la barra ya muestra (el número, la cuenta, la lupa, la lista) MUST seguir igual.

**Km y calorías**

- **FR-007**: Cada salida MUST mostrar los km caminados en la lista de Salidas, en su detalle y en la
  confirmación de terminar. Son los km de esa salida: la lista MUST seguir sin totales acumulados
  arriba (FR-020a de la 004), que viven en las estadísticas. Los km MUST salir de los puntos grabados, sin contar los huecos ni los
  puntos que la precisión descarta.
- **FR-008**: Con el peso cargado, cada salida MUST mostrar un aproximado de las calorías gastadas
  en los mismos lugares, dicho como aproximado. Son las calorías activas, las de caminar: salen de
  los km y del peso del jugador, y el tiempo quieto no suma.
- **FR-009**: El peso del jugador MUST poder cargarse, cambiarse y borrarse en Ajustes. Sin peso
  cargado, la app MUST NOT mostrar calorías en ningún lado, y el detalle de una salida MUST decir que
  se ven cargando el peso.
- **FR-010**: Los km y las calorías MUST calcularse en el momento a partir de lo guardado, así las
  salidas viejas también los tienen y cambiar el peso las recalcula a todas.

**El aviso de una salida cortada**

- **FR-011**: Si una salida en curso deja de grabar sin que el jugador la termine, porque la app se
  cerró de cualquier forma, el teléfono MUST mostrar una notificación en menos de 2 minutos. Dice que
  la salida se cortó y que abriendo la app se empieza otra.
- **FR-012**: Tocar esa notificación MUST abrir la app en la pantalla principal.
- **FR-013**: Terminar la salida con el botón MUST NOT dejar ninguna notificación pendiente, y una
  salida que sigue grabando MUST NOT avisar nada, aunque el jugador esté quieto.
- **FR-014**: Sin permiso de notificaciones, el aviso MUST quedar en lo de hoy: la pantalla
  principal lo dice al volver a abrir la app.

**Estadísticas**

- **FR-015**: La app MUST tener una pantalla de estadísticas, que se abre desde Salidas, con tres
  pestañas: día, semana y mes.
- **FR-016**: Para el período elegido, la pantalla MUST mostrar la cantidad de salidas, el tiempo
  caminado (la suma de sus duraciones), los km, las calorías (solo con el peso cargado, FR-009) y
  las patentes anotadas.
- **FR-017**: La pantalla MUST arrancar en el período actual (hoy, esta semana, este mes) y dejar ir
  a los anteriores con una flecha, y volver hacia adelante hasta el actual. La semana MUST ir de
  lunes a domingo, y el mes MUST ser el calendario, en la hora del teléfono.
- **FR-018**: Debajo de los totales, la pantalla MUST mostrar un gráfico de barras de km: una barra
  por cada uno de los últimos 7 días, 8 semanas o 12 meses, según la pestaña, terminando en el
  período elegido y con ese resaltado.
- **FR-019**: Una salida MUST contar en el período en que empezó. Una patente MUST contar en el
  período en que se anotó.
- **FR-020**: Las estadísticas MUST calcularse con lo guardado, igual que el FR-010: no se guarda
  nada nuevo.

**Para todo**

- **FR-021**: Nada de esto MUST modificar los puntos ni las patentes de una salida (Principio II).
- **FR-022**: Cerrar la app con una salida en curso MUST seguir cortándola con lo grabado, como hoy
  (FR-028 de la 006). No se retoma.
- **FR-023**: Todo esto MUST funcionar igual en el Android y en el iPhone (regla de la 006). En el
  Android, una salida que sigue grabando después de sacar la app de recientes no se cortó, y no
  avisa.

### Key Entities

- **Salida** (ya existe): suma lo que se calcula sobre ella, sin guardarlo: km y calorías.
- **Peso del jugador**: un número en kg, opcional, una preferencia más como el modo del mapa.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Ninguna salida termina con un solo toque.
- **SC-002**: Con una salida en curso, el tiempo, los km y las calorías se leen en la pantalla
  principal sin abrir nada.
- **SC-003**: En una caminata de 2 km medidos en un mapa, la app dice entre 1,8 y 2,3 km.
- **SC-004**: Cerrar la app con una salida en curso deja una notificación en menos de 2 minutos.
- **SC-005**: En una salida de una hora que termina con el botón, con ratos quieto, no aparece
  ninguna notificación.
- **SC-006**: Los km de una semana en las estadísticas son la suma exacta de los km de sus salidas en
  la lista de Salidas.
- **SC-007**: Con un año de salidas guardadas, las estadísticas abren en menos de un segundo.

## Assumptions

- **El teléfono de juego es el iPhone** (006). El Android sigue siendo el de pruebas.
- **Las salidas se caminan.** Las calorías son las activas de caminar en llano, con una fórmula
  estándar por km y kg de peso. Se dicen como aproximado ("~75 kcal") y no pretenden más.
- **Los km salen de los puntos medidos**, no del camino ajustado a las calles de la 007: lo medido
  existe desde el primer punto, también en una salida en curso.

## Out of Scope

- **Seguir grabando o retomar la salida después de que se cierra la app.** Se averiguó y el jugador
  lo descartó (Contexto). Lo que queda es el aviso (US2).
- **Pausar una salida.**
- **Ritmo, velocidad, pasos o metas de km.** Nadie lo pidió.
- **Estadísticas por zona, por año o con rangos elegidos a mano.** Día, semana y mes alcanzan.
- **Gráficos de otra cosa que km.** El resto se lee en los totales.
- **Mandar los datos a Salud o a Google Fit.**
- **Terminar sola una salida olvidada.**
