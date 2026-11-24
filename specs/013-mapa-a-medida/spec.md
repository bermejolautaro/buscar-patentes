# Feature Specification: Mapa a medida

**Feature Branch**: `013-mapa-a-medida`

**Created**: 2026-11-24

**Status**: Draft

**Input**: User description: "Que en el mapa principal se muestre el trackeo de la última zona designada (o que yo pueda elegir qué mapa trackear). Que yo pueda configurar desde settings a partir de cuál valor se agrupan las patentes cuando hago zoom. La ubicación se desfasa y el botón de mostrar mi ubicación me lleva al lugar equivocado; lo raro es que guardar patente conoce mi ubicación y funciona bien."

## Clarifications

### Session 2026-11-24

- Q: ¿Qué zona se ve en el mapa principal? → A: La que el jugador elige en Zonas. Arranca en la
  última que creó, y puede ser una terminada.
- Q: ¿Qué se ajusta de cómo se agrupan las patentes? → A: El acercamiento desde el que no se agrupan
  nunca: de ahí para adentro, todas sueltas.
- Q: ¿Dónde y cómo falla la ubicación? → A: En el iPhone. El punto azul está bien; el botón de volver a
  la ubicación lleva a un lugar donde el jugador estuvo antes.

### Session 2026-11-24 (después de probarla)

- Q: ¿Qué se pinta de azul en el mapa principal? → A: Se puede cambiar entre "todos los tiempos" y
  "la zona elegida", que muestra solo lo caminado desde que cuenta esa zona. El total se sigue
  guardando. Puede haber varias zonas a la vez; en el mapa se ve una.
- Q: ¿Qué se ajusta del agrupado? → A: Dos números, para ir probando: la distancia que se corre una
  patente antes de agruparse (hoy 50 m) y el zoom desde el que no se agrupa ninguna. Las cuatro
  opciones con nombre no servían. Y tocar un grupo lo expande.

## Contexto

- **Las zonas en el mapa principal** son hoy todas las activas, y ninguna terminada (FR-010 de la
  008). El jugador completó su zona y creó otra que quedó buscando sus calles: el mapa principal no
  mostraba nada que seguir.
- **Las patentes se agrupan** en cada zoom: las cercanas se corren hasta 50 m, y las que no entran
  forman un grupo con su cuenta (FR-023 de la 005). Desde qué acercamiento conviene verlas todas
  sueltas depende de cuántas hay y de cómo se mira, y hoy no se puede cambiar.
- **El botón de volver a la ubicación** usa una copia de la posición que la app actualiza solo mientras
  el mapa sigue al jugador. Al arrastrar el mapa la copia se congela, y el botón lleva ahí. El punto
  azul lee la posición de la librería del mapa, que sí está al día.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El botón de ubicación lleva a donde estoy (Priority: P1)

El jugador arrastra el mapa, camina, y toca el botón de volver a su ubicación: el mapa va adonde está
el punto azul.

**Why this priority**: es un error, y lleva a un lugar falso.

**Independent Test**: arrastrar el mapa, caminar dos cuadras sin tocarlo y tocar el botón.

**Acceptance Scenarios**:

1. **Given** el mapa arrastrado lejos y el jugador caminando, **When** toca el botón, **Then** el mapa
   se centra en el punto azul, no en donde estaba cuando arrastró.
2. **Given** el botón tocado, **When** el jugador sigue caminando, **Then** el mapa lo sigue, como hoy.

---

### User Story 2 - Elegir qué zona se sigue en el mapa (Priority: P2)

En el detalle de una zona, el jugador la elige para el mapa principal. El mapa principal muestra el
borde de esa zona y sus cuadras que faltan. Si nunca eligió, se muestra la última zona que creó.

**Why this priority**: es lo que hace que el mapa principal sirva para salir a completar una zona.

**Independent Test**: con dos zonas, elegir una y mirar el mapa principal; elegir la otra y mirar.

**Acceptance Scenarios**:

1. **Given** varias zonas y ninguna elegida, **When** se abre el mapa principal, **Then** muestra la
   última creada.
2. **Given** una zona elegida, **When** se abre el mapa principal, **Then** muestra solo esa: su borde
   punteado y las cuadras que le faltan.
3. **Given** la zona elegida es una terminada, **When** se abre el mapa principal, **Then** muestra su
   borde y las cuadras que le faltaban al terminar (ninguna si se completó).
4. **Given** la zona elegida está buscando sus calles, **When** se abre el mapa principal, **Then**
   muestra su borde, sin cuadras.
5. **Given** el detalle de la zona que se ve en el mapa, **When** se mira, **Then** dice que es la que
   se ve en el mapa principal. En las otras, el botón la elige.
6. **Given** la zona elegida se borra, **When** se abre el mapa principal, **Then** vuelve a la última
   creada.

---

### User Story 3 - Elegir desde qué acercamiento no se agrupan las patentes (Priority: P3)

En Ajustes, el jugador elige desde qué acercamiento las patentes se ven siempre sueltas: nunca (como
ahora), barrio, calle, cuadra o muy cerca.

**Why this priority**: es una preferencia; el agrupado de hoy sigue funcionando.

**Independent Test**: elegir "calle", acercar el mapa a una calle con varias patentes juntas.

**Acceptance Scenarios**:

1. **Given** "Nunca", **When** se acerca el mapa, **Then** se agrupan como hoy.
2. **Given** "Calle", **When** el mapa está a zoom de calle o más cerca, **Then** ninguna patente está en
   un grupo: todas sueltas, en su lugar.
3. **Given** "Calle", **When** el mapa está más lejos, **Then** se agrupan como hoy.
4. **Given** el valor elegido, **When** se cierra y se abre la app, **Then** sigue elegido.

---

### Edge Cases

- **Sin zonas**: el mapa principal no muestra ninguna, como hoy.
- **Con el modo de recorridos apagado**: tampoco se muestra la zona, como hoy.
- **Muchas patentes en el mismo punto con "sin grupos"**: se dibujan encimadas; es lo que eligió el
  jugador.
- **Sin permiso de ubicación** el botón no aparece, como hoy.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El botón de volver a la ubicación MUST llevar a la última posición del punto azul, y
  retomar el seguimiento.
- **FR-002**: El mapa principal MUST mostrar una sola zona: la elegida, o la última creada si no hay
  elegida o se borró. Reemplaza al FR-010 de la 008, que mostraba todas las activas.
- **FR-003**: De esa zona MUST mostrar el borde punteado y sus cuadras que faltan: en vivo si está
  activa, las que faltaban al terminar si terminó, ninguna si busca sus calles.
- **FR-004**: El detalle de una zona MUST dejar elegirla para el mapa principal, y decir cuándo es la
  que se ve.
- **FR-005**: Ajustes MUST dejar escribir dos números: la distancia que se puede correr una patente
  antes de agruparse (vacío, 50 m) y el zoom desde el que no se agrupa ninguna (vacío, nunca). Desde
  ese zoom, cada patente MUST dibujarse suelta y en su lugar.
- **FR-006**: La zona elegida y el acercamiento MUST recordarse entre aperturas y viajar en el
  respaldo.
- **FR-007**: Todo esto MUST funcionar igual en el Android y en el iPhone (regla de la 006).
- **FR-008**: Con una zona elegida, el azul del mapa principal MUST poder cambiarse, tocando la
  leyenda, entre lo caminado en todos los tiempos y lo de esa zona: las salidas desde su "desde cuándo
  cuenta" y, si terminó, hasta que terminó. Arranca en la zona, y se recuerda.
- **FR-009**: Tocar un grupo de patentes MUST acercar el mapa hasta las patentes del grupo. Si están
  todas en el mismo lugar, lo más cerca que se puede.
- **FR-010**: La leyenda del mapa MUST mostrar el zoom, para elegir el del FR-005.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Después de arrastrar el mapa y caminar, el botón centra el mapa a menos de 20 m del punto
  azul.
- **SC-002**: El mapa principal muestra la zona elegida en el primer cuadro en que muestra zonas.
- **SC-003**: Con "calle" elegido, a zoom de calle no hay ningún grupo de patentes en pantalla.

## Assumptions

- Los acercamientos son los zooms 15 (barrio), 16 (calle), 17 (cuadra) y 18 (muy cerca). El mapa llega
  hasta 19.
- El punto azul ya está bien en los dos teléfonos: el arreglo es usar su posición.

## Out of Scope

- **Mostrar varias zonas a la vez.**
- **Ajustar cuánto se corren las patentes** antes de agruparse (los 50 m).
- **Cambiar el lector de ubicación** de la librería del mapa o el de guardar patente.
