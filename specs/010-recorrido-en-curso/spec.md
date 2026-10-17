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
- **Cerrar la app la corta.** En el iPhone, sacar la app deslizándola desde el selector de apps
  mata la grabación. Al abrirla otra vez, la salida se cierra con lo que tenía y la pantalla dice
  "La salida se cortó" (FR-028 de la 006). Lo que se camine después es otra salida, si el jugador
  se acuerda de empezarla.

### Lo que se averiguó sobre seguir grabando con la app cerrada

- **iPhone, la app cerrada por el jugador**: no se puede. Cuando el jugador la saca deslizándola,
  el sistema no la vuelve a abrir para darle ubicaciones hasta que el jugador la abre a mano. Es una
  regla del sistema, y desarrolladores reportan que en iOS 26 se cumple también para los avisos de
  ubicación que antes sí la reabrían.
- **iPhone, la app cerrada por el sistema** (para liberar memoria, con la pantalla apagada): sí se
  puede. Con las APIs de ubicación de iOS 17 en adelante, el sistema la vuelve a abrir en segundo
  plano y la grabación sigue, si la app la retoma apenas arranca. La grabación de hoy usa la API
  vieja, que no la reabre.
- **Android**: la grabación es un servicio en primer plano con su notificación, y sacar la app de
  recientes no lo detiene en un Android de fábrica. Algunas capas de fabricante, como HyperOS, matan
  todo igual si la app no tiene permiso de inicio automático o tiene el ahorro de batería activo.
  Eso se verifica en el teléfono de pruebas.

Lo que sí se puede en los dos: **que cerrar la app no corte la salida**. Al volver a abrirla, la
salida sigue siendo la misma, con un hueco donde no hubo grabación.

## Clarifications

### Session 2026-10-17

- Q: ¿De dónde sale el peso para las calorías? → A: El jugador lo carga en Ajustes. Mientras no lo
  cargue, la app no muestra calorías: ningún número inventado.
- Q: ¿La barra de arriba muestra también las calorías en vivo? → A: Sí: tiempo, km y calorías.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - No perder la salida al cerrar la app sin querer (Priority: P1)

El jugador está en una salida y cierra la app sin querer. Cuando la vuelve a abrir, la salida sigue
abierta y grabando: es la misma, no una nueva, y lo que pasó con la app cerrada queda como un hueco.

**Why this priority**: es lo único de la lista que hoy pierde datos. Lo demás es comodidad.

**Independent Test**: empezar una salida, caminar una cuadra, cerrar la app desde el selector de
apps, caminar otra cuadra, abrirla y caminar una tercera. Terminar la salida.

**Acceptance Scenarios**:

1. **Given** una salida en curso, **When** el jugador cierra la app desde el selector de apps y la
   vuelve a abrir, **Then** la salida sigue en curso, el botón de recorrido sigue en "terminar" y
   la grabación sigue.
2. **Given** esa salida retomada, **When** el jugador la termina, **Then** queda una sola salida
   con las tres cuadras: la primera y la tercera grabadas, y un hueco punteado entre las dos (FR-009a
   de la 004).
3. **Given** la app retomó una salida, **When** se abre la pantalla principal, **Then** dice una vez
   que siguió la misma salida y que lo de la app cerrada no se grabó.
4. **Given** una salida en curso en el iPhone y la pantalla apagada, **When** el sistema cierra la
   app para liberar memoria, **Then** la grabación sigue sola, sin que el jugador abra la app.
5. **Given** una salida en curso en el Android, **When** el jugador saca la app de recientes,
   **Then** la notificación de la salida sigue y la grabación no se corta.

---

### User Story 2 - No terminar la salida sin querer (Priority: P1)

El jugador toca el botón de recorrido con una salida en curso. Antes de terminarla, la app pregunta.

**Why this priority**: es el otro modo de perder una salida, y el más fácil de arreglar.

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
5. **Given** una salida en curso, **When** el jugador la termina desde la pantalla de Salidas,
   **Then** también pregunta.

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
   "0:42 · 2,1 km · ~150 kcal".
3. **Given** la misma salida sin el peso cargado, **When** se mira la barra, **Then** dice "0:42 ·
   2,1 km", sin calorías.
4. **Given** la salida en curso, **When** pasa un minuto, **Then** el tiempo sube solo, sin tocar
   nada.
5. **Given** una salida retomada después de cerrar la app (US1), **When** se mira la barra,
   **Then** el tiempo cuenta desde que empezó la salida, y los km no suman nada del hueco.

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
3. **Given** la salida en curso, **When** se mira la confirmación de terminar (US2), **Then** dice
   también los km y las calorías.
4. **Given** el peso del jugador, **When** cambia, **Then** las calorías de todas las salidas se
   recalculan con el peso nuevo.
5. **Given** sin peso cargado, **When** se mira una salida, **Then** dice sus km y no dice calorías.
   El detalle de la salida dice que se ven cargando el peso en Ajustes.

---

### Edge Cases

- **La app cerrada mucho tiempo**: si el jugador vuelve a abrirla al otro día, la salida sigue abierta
  igual. Una salida olvidada se termina a mano, y la confirmación dice cuánto lleva, que es lo que
  delata que quedó abierta.
- **El teléfono se reinicia o se apaga** con una salida en curso: es lo mismo que cerrar la app. Al
  abrirla, sigue la misma salida.
- **Sin permiso de ubicación al reabrir**: la salida sigue abierta pero no graba, y la pantalla lo
  dice, como hoy al empezar sin permiso.
- **Una salida sin puntos o con uno solo**: 0,0 km y las calorías del tiempo nada más.
- **Saltos del GPS**: un punto que cae lejos y vuelve no suma km. Lo que la precisión ya descarta
  (FR-036 de la 003) no cuenta.
- **El hueco de una desconexión o de la app cerrada** no suma km: no se sabe por dónde se fue.
- **Las salidas viejas**, de antes de esta feature, también muestran sus km y calorías: salen de los
  puntos que ya tienen.

## Requirements *(mandatory)*

### Functional Requirements

**Cerrar la app no corta la salida**

- **FR-001**: Si al abrir la app hay una salida en curso, la app MUST retomarla: la misma salida,
  grabando otra vez. Reemplaza al FR-028 de la 006, que la cerraba.
- **FR-002**: El tiempo con la app cerrada MUST quedar como un hueco de la salida, igual que un corte
  de señal: no se dibuja como caminado ni suma km.
- **FR-003**: Al retomar una salida, la pantalla principal MUST decirlo una vez: que sigue la misma
  salida y que lo de la app cerrada no se grabó.
- **FR-004**: En el iPhone, si el sistema cierra la app con una salida en curso, la grabación MUST
  seguir sola, sin que el jugador la abra.
- **FR-005**: En el iPhone, si el jugador cierra la app con una salida en curso y el sistema le da
  tiempo a la app de enterarse, la app MUST dejar una notificación que diga que la grabación quedó
  frenada y que abrirla la sigue.
- **FR-006**: En el Android, sacar la app de recientes MUST NOT terminar la salida ni frenar la
  grabación. Si el teléfono mata el proceso igual, vale el FR-001.

**Terminar con confirmación**

- **FR-007**: Terminar una salida en curso MUST pedir confirmación, desde la pantalla principal y
  desde Salidas. La confirmación MUST decir cuánto lleva la salida: tiempo, km y calorías.
- **FR-008**: Mientras la confirmación está abierta, la salida MUST seguir grabando. Cancelar o tocar
  afuera MUST dejarla como estaba.
- **FR-009**: Empezar una salida MUST NOT pedir confirmación.

**La salida en la barra de arriba**

- **FR-010**: Con una salida en curso, la barra de arriba de la pantalla principal MUST mostrar el
  tiempo que lleva (horas y minutos) y los km caminados, con un decimal. Sin salida en curso, la
  barra MUST verse como hoy.
- **FR-011**: El tiempo MUST actualizarse solo, al menos una vez por minuto, y contar desde que
  empezó la salida, también si se retomó.
- **FR-012**: Lo que la barra ya muestra (el número, la cuenta, la lupa, la lista) MUST seguir igual.

**Km y calorías**

- **FR-013**: Cada salida MUST mostrar los km caminados en la lista de Salidas, en su detalle y en la
  confirmación de terminar. Los km MUST salir de los puntos grabados, sin contar los huecos ni los
  puntos que la precisión descarta.
- **FR-014**: Con el peso cargado, cada salida MUST mostrar un aproximado de las calorías gastadas
  en los mismos lugares, dicho como aproximado. Salen de los km, el tiempo y el peso del jugador.
- **FR-015**: El peso del jugador MUST poder cargarse, cambiarse y borrarse en Ajustes. Sin peso
  cargado, la app MUST NOT mostrar calorías en ningún lado, y el detalle de una salida MUST decir que
  se ven cargando el peso.
- **FR-016**: Los km y las calorías MUST calcularse en el momento a partir de lo guardado, así las
  salidas viejas también los tienen y cambiar el peso las recalcula a todas.
- **FR-017**: Con el peso cargado, la barra de arriba MUST mostrar también las calorías
  aproximadas de la salida en curso, al lado del tiempo y los km (FR-010).

**Para todo**

- **FR-018**: Nada de esto MUST modificar los puntos ni las patentes de una salida (Principio II).
  Retomar una salida le agrega puntos; no reescribe los que tenía.
- **FR-019**: Todo esto MUST funcionar igual en el Android y en el iPhone, salvo lo que el FR-004 al
  FR-006 dicen por teléfono (regla de la 006).

### Key Entities

- **Salida** (ya existe): suma lo que se calcula sobre ella, sin guardarlo: km, calorías y tiempo.
- **Peso del jugador**: un número en kg, opcional, una preferencia más como el modo del mapa.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cerrar la app en medio de una salida y volver a abrirla deja **una** salida, no dos, y
  sigue grabando sin ningún toque extra.
- **SC-002**: Ninguna salida termina con un solo toque.
- **SC-003**: Con una salida en curso, el tiempo, los km y las calorías se leen en la pantalla
  principal sin abrir nada.
- **SC-004**: En una caminata de 2 km medidos en un mapa, la app dice entre 1,8 y 2,3 km.
- **SC-005**: En el iPhone, con la pantalla apagada, una salida de una hora no se corta aunque el
  sistema cierre la app.

## Assumptions

- **El teléfono de juego es el iPhone** (006). El Android sigue siendo el de pruebas.
- **Las salidas se caminan.** Las calorías son las de caminar: una fórmula estándar que suma lo que
  cuesta moverse por cada km y lo que cuesta estar de pie por cada minuto, por kg de peso. Se dicen
  como aproximado ("~150 kcal") y no pretenden más.
- **Los km salen de los puntos medidos**, no del camino ajustado a las calles de la 007: lo medido
  existe desde el primer punto, también en una salida en curso.
- Retomar una salida no pide permisos nuevos: usa los que ya pidió al empezarla.
- El iPhone de juego tiene iOS 17 o más, que es lo que piden las APIs del FR-004. Se verifica en el
  plan; si no, el FR-004 queda afuera y el resto sigue.

## Out of Scope

- **Seguir grabando en el iPhone después de que el jugador cierra la app.** El sistema no lo permite.
  Lo que se hace es retomar al abrirla (FR-001) y avisar (FR-005).
- **Pausar una salida.** Cerrar la app no la pausa: deja un hueco.
- **Ritmo, velocidad, pasos o metas de km.** Nadie lo pidió.
- **Mandar los datos a Salud o a Google Fit.**
- **Terminar sola una salida olvidada.**
