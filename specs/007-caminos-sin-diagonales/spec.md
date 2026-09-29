# Feature Specification: Caminos sin diagonales

**Feature Branch**: `007-caminos-sin-diagonales`

**Created**: 2026-09-29

**Status**: Draft

**Input**: User description: "Caminos sin diagonales, a partir del TODO.md: (1) En los caminos de las salidas siguen apareciendo ciertas diagonales que no coinciden con la calle y se ven como un glitch. El dibujo a partir de los puntos de GPS crudos sale perfecto sobre el camino, así que es raro que aparezcan; sospecha: vienen del camino ajustado a calles. (2) En la pantalla de Salidas (el detalle de un recorrido), un interruptor para ver el camino dibujado con los puntos reales o con el camino ajustado a calles."

## Contexto

Desde la 003, una salida terminada se dibuja con su **camino ajustado a las calles** cuando lo
tiene, y con los puntos que midió el teléfono mientras el ajuste está pendiente (FR-031 a FR-035
de la 003). Desde que el muestreo se afinó (FR-036 de la 003), los puntos medidos caen sobre la
calle por la que se caminó: el jugador dice que ese dibujo "sale perfecto".

El camino ajustado, en cambio, muestra de vez en cuando **rectas que cruzan manzanas**: tramos que
no siguen ninguna calle y que se ven como un error de dibujo. Es el mismo problema que la 003
resolvió para el trazo crudo con el corte y la línea punteada (FR-009 y FR-009a de la 003): una
recta continua afirma un camino que nadie caminó. El trazo crudo quedó protegido; el ajustado no.

Esta feature tiene dos partes: que el camino ajustado deje de afirmar caminos falsos, y que el
jugador pueda comparar, en el detalle de una salida, lo que midió el teléfono contra lo que
interpretó el ajuste.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El camino de una salida sigue las calles de punta a punta (Priority: P1)

El jugador mira el mapa, en cobertura o en antigüedad, para decidir por dónde salir. Todas las
salidas se dibujan sobre calles: ninguna línea atraviesa una manzana en diagonal. Donde el ajuste
no supo pegar el camino a una calle, se ve lo que midió el teléfono en ese tramo, que sí sigue la
calle. Donde se cortó la señal, se ve la línea punteada de siempre.

**Why this priority**: el mapa de cobertura existe para responder "por dónde ya pasé". Una
diagonal dice que se pasó por el medio de una manzana, o tapa una calle que no se caminó. Es el
mismo error que la 003 corrigió para el trazo crudo, y hoy lo reintroduce el ajuste.

**Independent Test**: con las salidas que ya están guardadas en el teléfono, abrir el mapa en
cobertura y recorrer con la vista cada salida en la zona de juego: no hay ninguna recta que cruce
una manzana. Repetir con una salida nueva, ajustada con conexión al volver.

**Acceptance Scenarios**:

1. **Given** una salida terminada y ajustada, **When** el jugador la mira en el mapa, **Then**
   cada tramo continuo sigue una calle o un sendero peatonal; no hay rectas entre dos puntos que
   no estén unidos por una calle.
2. **Given** una salida donde el ajuste no pudo pegar un tramo a ninguna calle, **When** se
   dibuja, **Then** ese tramo se dibuja con los puntos que midió el teléfono, y el resto con el
   camino ajustado, sin rectas de unión entre las partes.
3. **Given** una salida con un corte de señal (dos puntos seguidos demasiado lejos entre sí, como
   define el FR-009 de la 003), **When** se dibuja ajustada, **Then** los dos lados del corte se
   unen con la línea punteada, igual que en el trazo crudo, y no con una línea continua.
4. **Given** una salida que ya tenía un camino ajustado guardado antes de esta feature, **When**
   la app abre con conexión, **Then** esa salida se vuelve a ajustar con el criterio nuevo sin que
   el jugador haga nada, y mientras tanto se dibuja con los puntos medidos.
5. **Given** cualquier salida, **When** se ajusta o se vuelve a ajustar, **Then** los puntos que
   midió el teléfono quedan exactamente como estaban.

---

### User Story 2 - Ver lo que midió el teléfono o lo que interpretó el ajuste (Priority: P2)

En el detalle de una salida, el jugador puede alternar entre dos vistas del mismo camino: **el
real**, con los puntos que midió el teléfono, y **el ajustado**, pegado a las calles. Le sirve para
ver si el ajuste inventó o se comió alguna calle, y para quedarse con la vista que le resulte más
fiel.

**Why this priority**: es la herramienta para confiar en la US1, o para detectar lo que la US1 no
arregló. No cambia el mapa principal, así que sin ella la US1 ya entrega su valor.

**Independent Test**: abrir el detalle de una salida ajustada, tocar el interruptor y ver que el
camino cambia entre las dos versiones. Abrir una salida pendiente de ajuste y ver que el
interruptor explica por qué no hay versión ajustada.

**Acceptance Scenarios**:

1. **Given** el detalle de una salida ajustada, **When** se abre, **Then** muestra el camino
   ajustado y un interruptor que dice cuál de las dos vistas se está mirando.
2. **Given** el detalle con la vista ajustada, **When** el jugador toca el interruptor, **Then**
   el mapa muestra los puntos medidos, con sus cortes punteados, sin mover la cámara.
3. **Given** una salida sin camino ajustado —pendiente, sin conexión, o que el ajuste no pudo
   resolver—, **When** se abre el detalle, **Then** se ve el camino real y el interruptor no deja
   cambiar, con una línea que dice por qué: "Todavía sin ajustar" o "No se pudo ajustar".
4. **Given** el jugador cambió a la vista real y volvió a la lista, **When** abre otra salida o la
   misma, **Then** el detalle arranca otra vez en la vista ajustada.

---

### Edge Cases

- **Salida en curso**: no se ajusta hasta terminar (FR-031 de la 003). Su detalle muestra el
  camino real y el interruptor dice "Todavía sin ajustar".
- **Salida con uno o ningún punto**: no hay camino que ajustar ni que alternar. El detalle se ve
  como hoy.
- **Un tramo por una plaza o un terreno sin calles mapeadas**: el ajuste no tiene dónde pegarlo, y
  ese tramo se dibuja con los puntos medidos (escenario 2 de la US1). La diagonal medida es real y
  se muestra; lo que no se muestra es una diagonal inventada.
- **Todo el camino queda sin pegar**: la salida se dibuja entera con los puntos medidos, y cuenta
  como "No se pudo ajustar" en el detalle.
- **El servicio de ajuste no responde o no hay conexión**: igual que hoy, queda pendiente y se
  reintenta sola (FR-032 de la 003). Un reajuste de una salida vieja que falla deja la salida
  dibujada con los puntos medidos hasta el próximo intento.
- **Respaldo**: una salida con su camino ajustado viaja en el respaldo como hoy. Si llega a un
  teléfono una salida ajustada con el criterio viejo —por ejemplo, un respaldo sacado antes de
  esta feature—, se vuelve a ajustar como cualquier otra (escenario 4 de la US1).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Un camino ajustado MUST NOT dibujar una recta entre dos puntos que el ajuste no unió
  por una calle o un sendero. Todo tramo continuo del camino ajustado MUST seguir la red de calles.
- **FR-002**: Donde el ajuste no pudo pegar un tramo a una calle, el sistema MUST dibujar ese
  tramo con los puntos que midió el teléfono, y el resto con el camino ajustado. Las partes MUST
  empalmar sin rectas de unión.
- **FR-003**: Los cortes de señal del FR-009 de la 003 MUST respetarse también en el camino
  ajustado: los dos lados de un corte se unen con la línea punteada del FR-009a de la 003, nunca
  con una línea continua ni con un camino inventado por el ajuste.
- **FR-004**: Las salidas que ya tienen un camino ajustado con el criterio anterior MUST volver a
  ajustarse solas, una vez, la primera vez que la app tenga conexión, sin que el jugador lo pida.
  Mientras no se reajusten, MUST dibujarse con los puntos medidos.
- **FR-005**: Ni el ajuste ni el reajuste MUST modificar o descartar los puntos medidos (FR-034 de
  la 003, Principio II). El camino ajustado es una interpretación y se puede recalcular.
- **FR-006**: El detalle de una salida MUST ofrecer un interruptor entre la vista **real** —los
  puntos medidos, con sus cortes punteados— y la vista **ajustada**, y MUST decir cuál se está
  viendo.
- **FR-007**: El detalle MUST arrancar en la vista ajustada cuando la salida tiene camino ajustado.
  Si no lo tiene, MUST mostrar la vista real, no dejar cambiar, y decir por qué: "Todavía sin
  ajustar" o "No se pudo ajustar".
- **FR-008**: Cambiar de vista MUST NOT mover la cámara ni cambiar el zoom, para que las dos se
  puedan comparar en el mismo lugar.
- **FR-009**: El interruptor MUST afectar solo al detalle de esa salida. El mapa principal sigue
  dibujando el camino ajustado cuando existe (FR-031 de la 003) y la elección no se recuerda entre
  una visita al detalle y la siguiente.
- **FR-010**: Todo esto MUST funcionar igual en el Android y en el iPhone (regla de la 006).

### Key Entities

- **Punto medido**: una posición de la salida con su precisión y su hora, tal como la midió el
  teléfono. No cambia nunca.
- **Camino ajustado**: la interpretación de los puntos medidos sobre la red de calles. Puede tener
  **tramos sin pegar**, que se dibujan con los puntos medidos, y **cortes**, que se dibujan
  punteados. Se puede recalcular.
- **Estado del ajuste de una salida**: pendiente, ajustada, ajustada con el criterio anterior (a
  reajustar) o no se pudo ajustar. Es lo que decide qué se dibuja y qué dice el interruptor.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En todas las salidas guardadas en el teléfono del jugador al momento de probar, el
  mapa no muestra **ninguna** recta que cruce una manzana, ni en cobertura ni en antigüedad ni en el
  detalle. Se verifica mirando cada salida.
- **SC-002**: Después de actualizar la app, la primera vez que abre con conexión, el 100% de las
  salidas ajustadas con el criterio anterior quedan reajustadas o dibujadas con sus puntos medidos,
  sin que el jugador toque nada.
- **SC-003**: Cambiar de vista en el detalle lleva **un toque**, y el camino nuevo se ve en menos de
  un segundo.
- **SC-004**: Los puntos medidos de cada salida son idénticos antes y después del reajuste,
  comparados uno por uno.
- **SC-005**: Al mirar las dos vistas de sus salidas, el jugador no encuentra ninguna calle que
  caminó y que la vista ajustada no muestre, salvo en los tramos que el ajuste marcó como sin pegar.

## Assumptions

- **Las diagonales vienen del ajuste, no de la medición.** El jugador ve el camino crudo "perfecto"
  y las diagonales solo en el ajustado. Si en el plan aparece una diagonal en los puntos medidos
  que no sea un corte de señal, esa es otra causa y entra en esta feature.
- **En un tramo sin pegar se muestran los puntos medidos, no una línea punteada.** La línea
  punteada significa "hubo un corte y no se sabe por dónde se fue"; en un tramo sin pegar sí se
  sabe, porque el teléfono lo midió. Si el jugador prefiere distinguirlo, se decide en
  `/speckit-clarify`.
- **Se sigue usando el mismo servicio de ajuste**, gratis y sin clave. Cambiar de servicio no es
  parte de esta feature.
- **La vista por defecto del detalle es la ajustada** y no se recuerda la elección: la vista real
  es para comparar, no para quedarse. Si el jugador la prefiere, recordarla es un cambio chico.
- **El reajuste de las salidas viejas es una sola vez** y en segundo plano, con el mismo
  mecanismo de reintento que ya existe para los ajustes pendientes.
- **El formato del respaldo no cambia** si se puede evitar: lo que viaja es la salida con sus
  puntos y su camino ajustado, como hoy.
