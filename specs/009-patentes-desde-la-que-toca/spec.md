# Feature Specification: Patentes desde la que toca

**Feature Branch**: `009-patentes-desde-la-que-toca`

**Created**: 2026-10-15

**Status**: Draft

**Input**: User description: "Podés cambiar la visibilidad de las patentes para que por defecto solo se muestren todas las que sean mayor o igual que la patente buscada? Entonces tendrías: Mayor o igual (defecto), Todas, Ninguna."

## Contexto

La 004 puso un botón para esconder las patentes del mapa (FR-033 a FR-035b), con dos estados:
todas o ninguna. La 005 agregó la lupa, que deja el mapa con un solo número.

En la calle, las patentes de números que ya pasaron no sirven para buscar: el grupo ya va más
adelante, y esos pines tapan los que importan. Lo que el jugador quiere ver casi siempre es **la que
toca y las que vienen**. Esta feature hace de eso el estado del mapa al abrir la app, y deja las
otras dos vistas a un toque.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver solo lo que falta buscar (Priority: P1)

El jugador abre la app en la calle. El mapa muestra la patente que toca y las de números más altos,
y no las de números que ya pasaron. Un toque en el botón de patentes muestra todas; otro toque las
esconde; otro vuelve a la vista de arranque.

**Why this priority**: es todo el pedido. Sin esto no hay feature.

**Independent Test**: con patentes guardadas de números menores, iguales y mayores que el que toca,
abrir la app y mirar el mapa; después tocar el botón tres veces.

**Acceptance Scenarios**:

1. **Given** el número que toca es 313 y hay patentes 120, 313 y 500 guardadas, **When** el
   jugador abre la app, **Then** el mapa muestra la 313 y la 500, y no la 120.
2. **Given** el mapa en "mayor o igual", **When** el jugador toca el botón de patentes, **Then**
   el mapa muestra las tres.
3. **Given** el mapa con todas, **When** toca el botón otra vez, **Then** el mapa no muestra
   ninguna.
4. **Given** el mapa sin ninguna, **When** toca el botón otra vez, **Then** vuelve a mostrar la
   313 y la 500.
5. **Given** el jugador cambia el número que toca a 400, **When** vuelve a la pantalla principal
   en "mayor o igual", **Then** el mapa muestra la 500 y ya no la 313.
6. **Given** el mapa en "todas" o en "ninguna", **When** el jugador cierra la app y la vuelve a
   abrir, **Then** el mapa arranca otra vez en "mayor o igual".

---

### Edge Cases

- **La lupa de la 005**: con un número buscado con la lupa, el mapa muestra ese número aunque sea
  menor que el que toca. Buscar un número es pedirlo explícitamente, y gana sobre la vista. En
  "ninguna", la lupa no muestra nada, igual que hoy con las patentes escondidas.
- **Ninguna patente desde la que toca**: el mapa queda sin pines y la leyenda dice desde qué número
  muestra, así no se confunde con un mapa sin nada guardado.
- **El que toca es 0**: "mayor o igual" muestra todas.
- **La barra de arriba** (la cuenta y la lista del número que toca, de la 005) no cambia: cuenta lo
  del número que toca, que siempre está en la vista.
- **El mapa de una salida** muestra sus patentes como hasta ahora. La vista vive solo en la pantalla
  principal.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El botón de patentes de la pantalla principal MUST tener tres vistas y pasar de una a
  la siguiente con **un toque**, en este orden: **mayor o igual**, **todas**, **ninguna**, y otra
  vez mayor o igual. Reemplaza al FR-033 de la 004, que tenía dos.
- **FR-002**: En **mayor o igual**, el mapa MUST mostrar solo las patentes con número mayor o igual
  al que toca. En **todas**, todas. En **ninguna**, ninguna.
- **FR-003**: Cada arranque de la app MUST abrir en **mayor o igual**. La vista MUST NOT recordarse
  entre aperturas, por la misma razón que el FR-035a de la 004.
- **FR-004**: La leyenda del mapa MUST decir la vista cuando no es **todas**: desde qué número
  muestra en mayor o igual, y que están ocultas en ninguna (FR-035 de la 004).
- **FR-005**: El ícono del botón MUST mostrar la vista en la que está el mapa, uno distinto para
  cada una, como hoy.
- **FR-006**: Con un número buscado con la lupa (FR-004 a FR-010 de la 005), el mapa MUST mostrar
  ese número en **mayor o igual** y en **todas**, sea mayor o menor que el que toca, y nada en
  **ninguna**.
- **FR-007**: Cambiar la vista MUST NOT afectar lo que se guarda, la barra de arriba, los recorridos
  ni las zonas (FR-034 y FR-035b de la 004). Se retira el dibujo, no el dato.
- **FR-008**: Todo esto MUST funcionar igual en el Android y en el iPhone (regla de la 006).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Al abrir la app, ningún pin del mapa tiene un número menor que el que toca.
- **SC-002**: Desde cualquier vista se llega a cualquier otra con dos toques o menos.
- **SC-003**: En las vistas que esconden patentes, la leyenda lo dice: un mapa con menos pines nunca
  se ve igual que un mapa con todos.

## Assumptions

- "La patente buscada" es **el número que toca**, el que el jugador mantiene en Ajustes.
- Los números van de 0 a 999 y no dan la vuelta: el grupo los busca en orden y el jugador cambia el
  que toca a mano. Por eso alcanza con comparar.
- La patente que toca sigue en verde y encima de las demás (FR-001 y FR-002 de la 005). Está
  siempre en la vista de arranque.

## Out of Scope

- **Recordar la vista entre aperturas.** El FR-035a de la 004 sigue: un arranque con menos pines
  de los que hay se confunde con una pérdida de datos.
- **Elegir un rango de números a mano.** Para un número está la lupa.
- **Cambiar el mapa de una salida.**
