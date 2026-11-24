# Feature Specification: Cuadras de las teselas

**Feature Branch**: `012-cuadras-de-las-teselas`

**Created**: 2026-11-16

**Status**: Draft

**Input**: User description: "Creé un recorrido y se quedó permanentemente en 'Buscando calles...'. ¿Qué sucedió? [...] Hace varios meses se creó y todavía no funciona. [...] Pasemos a las teselas porque la región es más chica que la que ya existe en la otra zona."

## Contexto

Las cuadras de una zona salen hoy de Overpass, la consulta pública de OpenStreetMap (D1 de la 008).
Una zona nueva del jugador lleva meses en "Buscando las calles…", y es más chica que la que sí
funcionó: el problema no es su tamaño.

Medido el 2026-11-16 desde la PC:

- **Overpass**, el servidor principal: tarda entre 28 y 40 s en contestar una franja chica, y a un
  área de 2×2 km le contesta error 502. Su página de estado también da error.
- **Las copias de Overpass** de otros servidores: ninguna contestó el área de 2×2 km. Las que no
  dieron error tardaron más de 60 s.
- **ohsome**, otra API pública de OpenStreetMap: no deja bajar las formas de las calles (error 403).

La app le da 20 s a Overpass. Cada intento corta, y la zona se queda esperando sin decir nada.

**Las teselas del mapa de fondo** (OpenFreeMap) contestan en 0,1 s. Son las que la app ya descarga
para dibujar el mapa, y traen cada calle con su tipo, su sentido, si se puede caminar, el id de su
vía de OpenStreetMap y, en otra capa, su nombre. No traen las esquinas como nodos, pero en una
tesela del centro 811 de 838 puntas de calle caen exactamente sobre otra calle, 10 a menos de 1 m de
otra, y 7 son calles sin salida. Las 208 restantes son el borde de la tesela, y se unen con la de al
lado.

## Clarifications

### Session 2026-11-16

- Q: ¿De dónde salen las calles de una zona? → A: De las teselas del mapa de fondo, no de Overpass.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Las calles de una zona nueva llegan en segundos (Priority: P1)

El jugador dibuja una zona. En unos segundos, con conexión, la zona tiene sus cuadras y su
porcentaje, sin depender de un servicio que puede estar caído.

**Why this priority**: es la que tiene hoy al jugador con una zona colgada.

**Independent Test**: crear una zona con conexión y mirar cuánto tarda en dejar de decir "Buscando
las calles…".

**Acceptance Scenarios**:

1. **Given** conexión, **When** el jugador crea una zona del tamaño de un barrio, **Then** en menos
   de 10 s la zona tiene sus cuadras.
2. **Given** la zona que lleva meses buscando sus calles, **When** el jugador abre la app con la
   versión nueva, **Then** la zona encuentra sus calles sola, sin borrarla ni crearla de nuevo.
3. **Given** sin conexión, **When** se crea una zona, **Then** espera como hoy, y las busca la
   próxima vez que hay conexión (D8 de la 008).
4. **Given** una zona que se edita y cambia de borde (FR-022 de la 008), **When** se vuelven a buscar
   sus calles, **Then** salen de las teselas, y lo quitado sigue quitado.

---

### User Story 2 - Las mismas cuadras que antes (Priority: P1)

Las cuadras que salen de las teselas son las mismas que salían de Overpass: cortadas en cada esquina,
con su nombre, sin las de menos de 30 m, y las dos manos de una avenida contadas una vez (contrato Z2
de la 008).

**Why this priority**: si las cuadras cambian, el porcentaje deja de significar lo mismo.

**Independent Test**: armar con las teselas las cuadras de la zona del jugador que ya existe, y
compararlas con las que salieron de Overpass.

**Acceptance Scenarios**:

1. **Given** el borde de la zona completada del jugador, **When** se arman sus cuadras con las
   teselas, **Then** al menos el 95% coinciden con las de Overpass por sus dos puntas.
2. **Given** una calle que cruza el borde entre dos teselas, **When** se arman las cuadras, **Then**
   la cuadra sale entera, no partida en el borde.
3. **Given** una calle que termina sobre otra sin compartir un punto, **When** se arman las cuadras,
   **Then** la otra se corta ahí: es una esquina.
4. **Given** una calle marcada como no caminable o de paso prohibido, **When** se arman las cuadras,
   **Then** no entra, como con Overpass.

---

### Edge Cases

- **Una zona que cae en el borde entre varias teselas**: se bajan todas las que la tocan, con el
  mismo margen de 200 m que tenía la consulta a Overpass.
- **El servicio de teselas no contesta**: la zona espera como hoy y se reintenta.
- **Una calle sin nombre en las teselas**: la cuadra queda sin nombre, como con Overpass. No tiene
  gemela ni "toda la calle".
- **Un puente sobre una calle**: no es una esquina, si las teselas dicen que van en distinto nivel.
- **Las zonas que ya tienen cuadras** no cambian: la búsqueda corre solo para las que esperan.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Las calles de una zona MUST salir de las teselas del mapa de fondo, las mismas que la
  app usa para dibujar. La app MUST NOT consultar más a Overpass.
- **FR-002**: Las calles que entran MUST ser las mismas que con Overpass: avenidas, calles y
  peatonales, sin las de servicio, las de paso prohibido ni las no caminables.
- **FR-003**: Una esquina MUST ser un punto donde se tocan dos calles del mismo nivel: compartiendo
  un punto, o una terminando sobre la otra a menos de 1 m.
- **FR-004**: Una calle cortada por el borde de una tesela MUST unirse con su continuación en la
  tesela de al lado.
- **FR-005**: Cada cuadra MUST llevar el nombre de su calle, cuando las teselas lo tienen, y el
  sentido único, para reconocer las dos manos de una avenida.
- **FR-006**: El resto de las reglas de las cuadras MUST seguir igual: el largo mínimo, cuáles son de
  la zona, las gemelas y el máximo de 2.000 (contrato Z2 y FR-004 de la 008).
- **FR-007**: Todo esto MUST funcionar igual en el Android y en el iPhone (regla de la 006).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Una zona del tamaño de un barrio tiene sus cuadras en menos de 10 s con conexión.
- **SC-002**: Con el borde de la zona completada del jugador, al menos el 95% de las cuadras que
  salen de las teselas coinciden con las de Overpass.
- **SC-003**: La zona que lleva meses buscando sus calles las encuentra al abrir la app.

## Assumptions

- OpenFreeMap sigue siendo el fondo del mapa (D2 de la 001). Su URL vive en `ConfigMapa`, y las
  teselas se piden a la misma dirección.
- Las teselas se actualizan cada semana con OpenStreetMap: las calles están al día como con Overpass.
- Se usa el zoom 14, el más detallado que publica OpenFreeMap.

## Out of Scope

- **Recalcular las cuadras de las zonas que ya existen.** Se arman de nuevo solo al editar el borde.
- **Cuadras de calles que no están en las teselas.**
- **Avisar en la zona por qué no encuentra sus calles.** Con un servicio que contesta en 0,1 s deja
  de hacer falta; si vuelve a pasar, es otra feature.
