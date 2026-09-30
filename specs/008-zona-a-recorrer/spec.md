# Feature Specification: Zona a recorrer

**Feature Branch**: `008-zona-a-recorrer`

**Created**: 2026-09-29

**Status**: Draft

**Input**: User description: "Zona a recorrer, del TODO: 'Me gustaria dibujar una zona y ponerme como objetivo recorrer todas las calles. Ver cuanto tiempo me lleva (podrian ser meses) desde que se creo la zona y que me muestre un porcentaje de cuanto recorri. Tambien la posibilidad de cerrar el objetivo en algun momento. Tambien poder quitar calles (pueden ser peligrosas).' Alcance completo: dibujar la zona, porcentaje recorrido, tiempo desde que se creó, cerrar el objetivo y quitar calles."

## Contexto

La 004 contestó "¿qué calles me faltan?" con el mapa en modo cobertura, y descartó a propósito
cualquier contador: calles, cuadras, kilómetros o porcentajes (su FR-020a). El motivo era que, para
todo el mapa, un número al lado del dibujo es una segunda respuesta, y peor, a la misma pregunta.

Una zona cambia la pregunta. Ya no es "¿por dónde pasé?" sobre un mapa sin bordes, sino "¿cuánto me
falta para terminar **esto**?" sobre un pedazo con borde que el jugador eligió. Esa pregunta sí tiene
un número por respuesta, y el número tiene un final: el 100%. La 005 dejó esta feature para una spec
propia justamente para revisar ahí la decisión de la 004. La revisión es esta: **el porcentaje vive
solo adentro de una zona**, y la pantalla principal sigue sin contadores.

La 007 dejó el mapa pintando solo calles, con rectas que doblan en las esquinas. Eso es lo que cuenta
para una zona: una cuadra está recorrida si el camino pintado la recorre.

## Clarifications

### Session 2026-09-29

- Q: ¿Las salidas hechas antes de crear la zona cuentan para el porcentaje? → A: Cuentan las salidas
  desde una fecha que elige el jugador, "desde cuándo cuenta". Arranca en el día en que se crea la
  zona, así el objetivo empieza en 0%, pero se puede correr para atrás para no perder el progreso
  que ya hizo. El tiempo del objetivo se mide desde esa misma fecha.
- Q: En la pantalla principal, mientras camina, ¿qué ve el jugador de una zona activa? → A: El borde
  y las cuadras que faltan, resaltadas, sin ningún número. Así ve adónde ir sin abrir la zona; el
  porcentaje sigue viviendo solo adentro de la zona.
- Q: ¿Cómo se dibuja el borde de la zona? → A: Tocando las esquinas una por una, unidas con rectas,
  con un botón para deshacer la última. No a mano alzada.

### Session 2026-09-30

Después de la primera prueba en el teléfono, el jugador pidió tres cosas: esquinas más grandes,
poder arrastrarlas y poder editar una zona ya creada. Entran en el FR-001 y el FR-022, y el cambio
de borde sale de Out of Scope.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Dibujar una zona y ver cuánto falta (Priority: P1)

El jugador se propone recorrer todas las calles de un pedazo del mapa. Marca las esquinas de la
zona sobre el mapa, le pone un nombre y elige desde cuándo cuenta. La app busca las cuadras que
quedan adentro y, a partir de ahí, le dice qué porcentaje recorrió, cuántas cuadras le faltan y
cuántos días lleva. En el mapa de la zona se distinguen las cuadras recorridas de las que faltan.

**Why this priority**: es la feature. Sin zona y sin porcentaje no hay objetivo que quitar ni
cerrar.

**Independent Test**: dibujar una zona chica, de unas diez manzanas, alrededor de calles ya
caminadas; poner "desde cuándo cuenta" antes de esas salidas y ver que las cuadras caminadas
aparecen recorridas y el porcentaje coincide con la cuenta a mano. Caminar después una cuadra que
faltaba, de esquina a esquina, y ver que la zona la suma cuando la salida queda ajustada.

**Acceptance Scenarios**:

1. **Given** la pantalla de zonas, **When** el jugador marca tres esquinas o más sobre el mapa y
   confirma, **Then** la zona queda guardada con su nombre, su fecha de creación y "desde cuándo
   cuenta" en el día de hoy.
2. **Given** una zona recién creada con conexión, **When** la app termina de buscar sus cuadras,
   **Then** muestra el porcentaje recorrido, las cuadras que faltan y los días desde "desde cuándo
   cuenta".
3. **Given** una zona con "desde cuándo cuenta" en el día de hoy, **When** el jugador la mira,
   **Then** está en 0%, aunque tenga salidas viejas adentro.
4. **Given** una zona con salidas viejas adentro, **When** el jugador corre "desde cuándo cuenta"
   a una fecha anterior a esas salidas, **Then** las cuadras que caminó desde esa fecha pasan a
   recorridas, y los días se cuentan desde esa fecha.
5. **Given** una zona activa, **When** termina una salida que pasa por una cuadra que faltaba, de
   esquina a esquina, y esa salida queda ajustada a las calles, **Then** la cuadra pasa a recorrida
   y el porcentaje sube, sin que el jugador haga nada.
6. **Given** una salida que solo cruza una calle de la zona en una esquina, **When** se ajusta,
   **Then** esa calle no suma ninguna cuadra.
7. **Given** una o varias zonas activas, **When** el jugador mira la pantalla principal, **Then**
   ve el borde de cada zona y sus cuadras que faltan resaltadas, y ningún porcentaje ni contador.
8. **Given** la pantalla principal con una zona activa, **When** una salida recorre una cuadra que
   faltaba y queda ajustada, **Then** esa cuadra deja de estar resaltada.

---

### User Story 2 - Quitar las calles que no se van a caminar (Priority: P2)

Algunas cuadras de la zona no se quieren caminar: son peligrosas, son privadas o no existen aunque
el mapa las tenga. El jugador toca una cuadra en el mapa de la zona y la quita, o quita toda esa
calle dentro de la zona. Lo quitado deja de contar para el porcentaje, se sigue viendo distinto en
el mapa y se puede volver a sumar.

**Why this priority**: sin esto, una zona con una cuadra peligrosa nunca llega al 100%. Pero la US1
entrega valor sola: el porcentaje y el mapa de lo que falta ya sirven para planear.

**Independent Test**: en una zona con cuadras que faltan, quitar una cuadra y ver que el total de
cuadras baja en uno y el porcentaje se recalcula. Quitar una calle entera y ver que se van todas sus
cuadras de adentro de la zona. Volver a sumar una y ver que vuelve a contar.

**Acceptance Scenarios**:

1. **Given** el mapa de una zona activa, **When** el jugador toca una cuadra, **Then** ve el nombre
   de la calle y puede quitar esa cuadra o toda la calle dentro de la zona.
2. **Given** una cuadra quitada, **When** se recalcula el porcentaje, **Then** esa cuadra no cuenta
   ni en las recorridas ni en el total, esté recorrida o no.
3. **Given** una cuadra quitada, **When** el jugador mira el mapa de la zona, **Then** se ve distinta
   de las recorridas y de las que faltan.
4. **Given** una cuadra quitada, **When** el jugador la toca, **Then** puede volver a sumarla, y
   vuelve a contar como recorrida o como pendiente según corresponda.

---

### User Story 3 - Cerrar el objetivo (Priority: P3)

El objetivo termina de una de dos formas. Si el jugador recorre todas las cuadras que cuentan, la
zona se completa sola y guarda cuánto tardó. Si en algún momento decide que ya está, la cierra a
mano y queda guardado hasta dónde llegó. Una zona terminada no cambia más, y queda en la lista con
su resultado.

**Why this priority**: le da final al objetivo y a la medida del tiempo, que el jugador pidió ("ver
cuánto tiempo me lleva"). Mientras no esté, una zona activa ya muestra los días que lleva.

**Independent Test**: en una zona chica, quitar todas las cuadras que faltan menos una, caminarla y
ver que la zona se completa sola con los días que tardó. En otra zona, cerrarla a mano y ver que
queda en la lista con su porcentaje y sus días, y que una salida posterior ya no la cambia.

**Acceptance Scenarios**:

1. **Given** una zona activa a la que le falta una cuadra, **When** una salida la recorre y queda
   ajustada, **Then** la zona queda completada, con la fecha de esa salida y el tiempo desde "desde
   cuándo cuenta".
2. **Given** una zona activa, **When** el jugador la cierra a mano y confirma, **Then** la zona
   queda cerrada con el porcentaje de ese momento, la fecha de cierre y el tiempo que llevaba.
3. **Given** una zona completada o cerrada, **When** termina una salida adentro, o se borra una
   salida que contaba, **Then** su resultado no cambia.
4. **Given** la pantalla de zonas, **When** el jugador la abre, **Then** ve primero las zonas
   activas y después las terminadas, cada una con su resultado.
5. **Given** cualquier zona, **When** el jugador la borra y confirma, **Then** la zona desaparece y
   las salidas quedan exactamente como estaban.

---

### Edge Cases

- **Sin conexión al crear la zona**: la zona se guarda igual, y sus cuadras se buscan cuando vuelve
  la conexión, sin que el jugador haga nada. Mientras tanto la zona dice "Buscando las calles" y no
  muestra porcentaje. Una vez que tiene sus cuadras, todo funciona sin red.
- **Un dibujo que no es una zona**: menos de tres esquinas, o bordes que se cruzan. La app no deja
  confirmarlo y dice por qué.
- **Una zona demasiado grande**: más cuadras que el máximo del FR-004. La app dice que la zona es
  demasiado grande y no la crea.
- **Una zona sin cuadras**: un parque, el río. La app dice que no encontró calles adentro y no la
  crea.
- **Una cuadra en el borde de la zona**: pertenece a la zona si la mitad o más de su largo queda
  adentro.
- **Una avenida que el mapa dibuja como dos calles paralelas**, una por mano: cada cuadra de la
  avenida cuenta una vez, y la recorre caminar por cualquiera de los dos lados.
- **Senderos de plazas, autopistas, vías del tren y calles internas de barrios cerrados**: no son
  cuadras de la zona. Lo que se cuele igual se quita (US2).
- **Una salida pendiente de ajuste o que no se pudo ajustar**: no suma hasta que queda ajustada, y
  si no se pudo, no suma. Una salida en curso suma cuando termina y se ajusta.
- **Una salida que empezó antes de "desde cuándo cuenta"**: no cuenta. Vale la fecha en que empezó.
- **Zonas que se pisan**: una cuadra caminada suma en cada zona activa que la contenga.
- **Se borra una salida que contaba**: una zona activa se recalcula y puede bajar. Una terminada no
  cambia.
- **"Desde cuándo cuenta" en el futuro**: no se puede elegir. Puede ser cualquier día hasta hoy.
- **Respaldo**: las zonas, sus cuadras y lo quitado viajan en el respaldo de la 006. Un respaldo
  sacado antes de esta feature no trae zonas, y al restaurarlo el teléfono queda sin zonas.

## Requirements *(mandatory)*

### Functional Requirements

**Crear y ver una zona**

- **FR-001**: El jugador MUST poder crear una zona tocando sus esquinas sobre el mapa, una por una;
  la app las une con rectas. Hacen falta tres o más esquinas y bordes que no se crucen. Entre toque y
  toque el jugador MUST poder mover el mapa y cambiar el zoom. Cada esquina MUST verse grande y
  poder arrastrarse con el dedo sin mover el mapa. El jugador MUST poder deshacer los cambios de a
  uno, una esquina nueva o un arrastre, antes de confirmar.
- **FR-002**: Cada zona MUST tener un nombre, que el jugador puede escribir; si no escribe ninguno,
  la app le pone uno con la fecha de creación.
- **FR-003**: Cada zona MUST tener un "desde cuándo cuenta": un día que arranca en el de la creación
  y que el jugador puede correr a cualquier día anterior mientras la zona esté activa. No puede ser
  posterior a hoy.
- **FR-004**: Al crear una zona, la app MUST buscar las **cuadras** que quedan adentro: los tramos
  de calle entre dos esquinas por los que se puede caminar (calles, avenidas y pasajes), sin
  autopistas, vías, senderos de plazas ni calles internas de barrios cerrados. Una cuadra del borde
  pertenece a la zona si la mitad o más de su largo queda adentro. Una zona MUST tener entre 1 y
  2.000 cuadras.
- **FR-005**: Las cuadras de una zona MUST quedar guardadas en el teléfono al crearla, y no cambiar
  después aunque cambie el mapa de calles. Sin conexión, la búsqueda MUST quedar pendiente y
  reintentarse sola.
- **FR-006**: Una cuadra MUST contar como **recorrida** cuando una salida terminada, ajustada a las
  calles y empezada desde "desde cuándo cuenta", la recorre de esquina a esquina: el camino pintado
  de la vista por defecto (007) sigue la mayor parte de su largo. Cruzarla en una esquina, o
  meterse unos metros, MUST NOT contar.
- **FR-007**: Para una zona activa, la app MUST mostrar el porcentaje de cuadras recorridas sobre
  las que cuentan, redondeado hacia abajo (una zona no dice 100% mientras le falte una cuadra), las
  cuadras que faltan y los días desde "desde cuándo cuenta".
- **FR-008**: El mapa de una zona MUST distinguir a simple vista tres clases de cuadra: recorrida,
  pendiente y quitada.
- **FR-009**: El porcentaje MUST actualizarse solo cuando una salida queda ajustada, cuando se
  borra una salida, cuando se quita o se suma una cuadra, y cuando cambia "desde cuándo cuenta".
- **FR-010**: La pantalla principal MUST mostrar el borde de cada zona activa y resaltar sus cuadras
  que faltan, en los modos cobertura y antigüedad; con los recorridos apagados, tampoco se ven las
  zonas. MUST NOT mostrar porcentajes ni contadores: el FR-020a de la 004 sigue valiendo para el
  mapa entero. Las cuadras quitadas no se resaltan.
- **FR-011**: Puede haber varias zonas activas a la vez, y MAY pisarse. Cada una lleva su propia
  cuenta.

**Quitar cuadras**

- **FR-012**: Tocando una cuadra de una zona activa, el jugador MUST ver el nombre de su calle y
  poder quitar esa cuadra o todas las cuadras de esa calle dentro de la zona.
- **FR-013**: Una cuadra quitada MUST NOT contar ni en las recorridas ni en el total. MUST poder
  volver a sumarse, y entonces cuenta según las salidas, como cualquier otra.

**Terminar el objetivo**

- **FR-014**: Cuando las cuadras que cuentan están todas recorridas, la zona MUST quedar
  **completada** sola, con la fecha de la salida que la completó y el tiempo desde "desde cuándo
  cuenta".
- **FR-015**: El jugador MUST poder **cerrar** una zona activa a mano, con confirmación. Queda
  guardado el porcentaje de ese momento, la fecha de cierre y el tiempo que llevaba.
- **FR-016**: Una zona completada o cerrada MUST NOT cambiar más: ni por salidas nuevas, ni por
  salidas borradas, ni por cuadras quitadas. No se reabre.
- **FR-017**: La pantalla de zonas MUST listar primero las activas, con su porcentaje y sus días, y
  después las terminadas, con su resultado: completada en tanto tiempo, o cerrada con tanto por
  ciento después de tanto tiempo.
- **FR-018**: El jugador MUST poder borrar cualquier zona, con confirmación. Borrar una zona MUST
  NOT tocar ninguna salida.

**Para todo**

- **FR-019**: Ni las zonas ni su cuenta MUST modificar los puntos medidos ni los caminos ajustados
  de las salidas (Principio II). La zona solo los lee.
- **FR-020**: Las zonas, sus cuadras, lo quitado y su resultado MUST viajar en el respaldo de la 006.
- **FR-021**: Todo esto MUST funcionar igual en el Android y en el iPhone (regla de la 006).

**Editar**

- **FR-022**: El jugador MUST poder editar una zona que no terminó: su nombre, su "desde cuándo
  cuenta" y su borde, que se dibuja igual que en el FR-001 empezando por el que tenía. Si el borde
  cambia, la zona MUST volver a buscar sus cuadras como en el FR-004. Las cuadras que estaban
  quitadas MUST seguir quitadas en la búsqueda nueva: se reconocen por sus dos puntas, a 5 m o
  menos. Una zona terminada no se edita (FR-016).

### Key Entities

- **Zona**: un pedazo del mapa que el jugador se propone recorrer entero. Tiene su borde, su nombre,
  la fecha de creación, "desde cuándo cuenta", su estado —buscando calles, activa, completada o
  cerrada— y, si terminó, la fecha y el porcentaje con que terminó.
- **Cuadra de la zona**: un tramo de calle entre dos esquinas, con el nombre de su calle. Pertenece
  a una zona, se guarda al crearla y puede estar quitada.
- **Salida**: la de siempre. La zona lee su fecha de inicio y su camino ajustado para decidir qué
  cuadras recorrió.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El jugador dibuja y confirma una zona de unas diez manzanas en menos de un minuto, y
  con conexión ve su porcentaje en menos de 30 segundos.
- **SC-002**: En una caminata de prueba por diez cuadras de una zona, de esquina a esquina, la zona
  suma exactamente esas diez: ninguna de menos, y ninguna de las calles que solo cruzó en las
  esquinas.
- **SC-003**: La pantalla de una zona de 2.000 cuadras muestra el porcentaje y el mapa en menos de un
  segundo, también sin conexión.
- **SC-004**: Quitar una cuadra o una calle lleva dos toques, y el porcentaje nuevo se ve en menos de
  un segundo.
- **SC-005**: Después de pasar un respaldo de un teléfono al otro, cada zona tiene las mismas
  cuadras, lo mismo quitado, las mismas fechas y el mismo porcentaje.
- **SC-006**: Mirando el mapa de una zona, el jugador dice cuáles cuadras le faltan sin tocar nada
  más.

## Assumptions

- **Las cuadras salen del mismo mapa de calles abierto que usa el ajuste**, y se buscan con red una
  sola vez por zona. Cuál servicio y cómo se le pide queda para el plan. Si el mapa trae una calle
  que no existe, se quita (US2).
- **Recorrida quiere decir recorrida a pie de esquina a esquina**, por cualquier vereda. El umbral
  exacto de "la mayor parte de su largo" se calibra en el plan contra salidas reales, con la regla
  del FR-006: caminarla cuenta, cruzarla no.
- **El porcentaje cuenta cuadras, no metros**: "te faltan 37 cuadras" es la medida con la que se
  camina.
- **El máximo de 2.000 cuadras** alcanza para varios barrios juntos, y acota el tiempo de la cuenta
  en el teléfono.
- **Solo cuentan las salidas ajustadas.** Es el mismo camino que pinta el mapa por defecto desde la
  007; los puntos medidos sin ajustar no dicen qué calle se caminó.
- **Las zonas no son evidencia.** Se pueden borrar y cambiar sin tocar el Principio II, que protege
  las patentes y los puntos medidos.

## Out of Scope

- **Un porcentaje o contador en la pantalla principal.** El FR-020a de la 004 sigue: el número vive
  adentro de la zona.
- **Sugerir por dónde caminar para completar la zona.** Pide un motor de ruteo, que la 003 y la 004
  ya dejaron afuera. El mapa de la zona muestra lo que falta; el camino lo elige el jugador.
- **Agregar cuadras que el mapa no tiene.**
- **Reabrir una zona terminada.**
- **Ver el porcentaje subir durante la salida.** Suma cuando la salida termina y se ajusta.
- **Avisos o notificaciones sobre la zona.** La 006 sacó los avisos por pedido del jugador.
- **Compartir o exportar una zona.** Nadie lo pidió.
