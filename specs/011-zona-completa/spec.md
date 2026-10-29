# Feature Specification: Zona completa

**Feature Branch**: `011-zona-completa`

**Created**: 2026-10-28

**Status**: Draft

**Input**: User description: "Tengo una cuadra que no importa cuánto la camine no me la cuenta, es la última que me falta. Sin el ajustar calles se puede ver que la caminé un gran porcentaje." Y después: "Hacé el arreglo y agregá una notificación de felicitaciones al llegar 100%."

## Contexto

A la zona del jugador le faltaba una sola cuadra: un tramo de 35 m de un pasaje que corre pegado a
una diagonal, formando un triangulito con ella. El jugador la caminó varias veces.

- **Lo que midió el teléfono** pasa a 0,3–2 m de la cuadra en todo su largo: la cubre al 100%.
- **El camino ajustado a las calles**, que es lo único que cuenta desde la 008, pasa por las dos
  puntas pero en el medio se va hasta 17 m, por la diagonal. Cubre el 57%, y hacen falta el 80%.

El servicio de ajuste prefirió la diagonal, que es la calle más grande, y lo va a seguir haciendo:
la cuadra no se puede completar caminando.

Probado con las 40 salidas reales del jugador, una por una: si un pedazo de cuadra cuenta cuando le
pasa cerca **lo ajustado o lo medido**, la regla suma 7 cuadras en total, en 40 salidas (una es la
del pasaje; las otras ya estaban recorridas por otras salidas), y no baja ninguna. Lo ajustado sigue
siendo lo principal: en muchas salidas cuenta cuadras que lo medido no ve, porque el GPS se corre a la
vereda o más allá.

Y una zona terminada al 100% hoy no lo celebra: pasa a "Completada" en la lista, y nada más.

## Clarifications

### Session 2026-10-28

- Q: ¿Cómo se arregla la cuadra que el ajuste no ve? → A: Un pedazo de cuadra cuenta si le pasa a
  10 m o menos el camino ajustado **o** lo que midió el teléfono, cortado en los huecos de señal.
- Q: ¿Qué pasa al completar una zona? → A: Una notificación de felicitaciones.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Una cuadra caminada cuenta aunque el ajuste se equivoque (Priority: P1)

El jugador camina una cuadra que el ajuste a calles pega a la calle de al lado. La cuadra cuenta
igual, porque lo que midió el teléfono pasa por ella.

**Why this priority**: es lo que hoy no deja completar la zona del jugador.

**Independent Test**: con el respaldo del jugador, la cuadra del pasaje queda recorrida y la zona
completa, con la fecha de la salida en que la caminó.

**Acceptance Scenarios**:

1. **Given** una cuadra que lo medido cubre entera y lo ajustado cubre al 57%, **When** se cuenta la
   zona, **Then** la cuadra está recorrida.
2. **Given** una salida terminada que todavía no se ajustó (sin conexión), **When** se cuenta la
   zona, **Then** lo que midió ya cuenta. Cuando se ajuste, suma lo ajustado.
3. **Given** un hueco de señal de más de 250 m en lo medido, **When** se cuenta la zona, **Then** la
   recta del hueco no cubre nada, igual que en el mapa (FR-009a de la 004).
4. **Given** una cuadra que solo se cruzó en la esquina, **When** se cuenta, **Then** sigue sin
   estar recorrida: lo medido también tiene que cubrir el 80%.

---

### User Story 2 - Felicitaciones al completar una zona (Priority: P2)

Cuando una zona llega al 100%, el teléfono muestra una notificación que lo celebra, con el nombre de
la zona y en cuánto tiempo se completó.

**Why this priority**: es el cierre de meses de caminata, y hoy pasa en silencio.

**Independent Test**: caminar la última cuadra de una zona, terminar la salida y mirar el teléfono.

**Acceptance Scenarios**:

1. **Given** una zona a la que le falta una cuadra, **When** el jugador la camina y termina la salida,
   **Then** aparece la notificación, con la app abierta o cerrada: "¡Zona completada! Recorriste
   todas las cuadras de Mi barrio en 3 meses y 4 días."
2. **Given** una zona a la que le falta una cuadra, **When** el jugador la quita, **Then** la zona se
   completa y aparece la misma notificación.
3. **Given** una zona completada, **When** pasan más salidas, **Then** no vuelve a felicitar: una
   zona completada no cambia (FR-016 de la 008).
4. **Given** una zona que el jugador cierra a mano, **When** se cierra, **Then** no felicita: no
   llegó al 100%.

---

### Edge Cases

- **Sin permiso de notificaciones**: no hay notificación. La zona queda "Completada" en la lista,
  como hoy.
- **La zona del jugador**, que con la regla nueva ya está al 100%: se completa y felicita la
  primera vez que la app revisa las zonas, al abrirla.
- **Dos zonas completadas a la vez**: una notificación por zona.
- **Calles casi pegadas**, a menos de 10 m: una salida por una puede contar la otra, si pasa a menos
  de 10 m de ella en el 80% de su largo. Es el mismo riesgo que ya tiene lo ajustado.

## Requirements *(mandatory)*

### Functional Requirements

**Lo medido también cuenta**

- **FR-001**: Un pedazo de cuadra MUST contar como cubierto por una salida si le pasa a 10 m o menos
  su camino ajustado **o** lo que midió el teléfono. La regla del 80% de la cuadra no cambia (contrato
  Z3 de la 008).
- **FR-002**: Lo medido MUST cortarse en los mismos huecos que el mapa (saltos de más de 250 m): la
  recta de un hueco no cubre nada.
- **FR-003**: Una salida terminada MUST contar con lo medido aunque no se haya ajustado todavía, o
  aunque el ajuste no haya podido. Cambia el supuesto de la 008 "solo cuentan las salidas ajustadas".
- **FR-004**: Al terminar una salida, la app MUST revisar las zonas enseguida, sin esperar a que el
  jugador vuelva a entrar a la pantalla principal.

**Felicitaciones**

- **FR-005**: Cuando una zona se completa al 100%, la app MUST mostrar una notificación de
  felicitaciones con el nombre de la zona y en cuánto tiempo se completó, contado desde "desde cuándo
  cuenta".
- **FR-006**: La notificación MUST verse también con la app abierta.
- **FR-007**: Cada zona MUST felicitar una sola vez: cuando pasa a completada. Cerrar una zona a mano
  MUST NOT felicitar.
- **FR-008**: Todo esto MUST funcionar igual en el Android y en el iPhone (regla de la 006).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con el respaldo del jugador, la cuadra del pasaje queda recorrida y la zona completa al
  100%.
- **SC-002**: Con las mismas 40 salidas, la regla nueva no deja sin recorrer ninguna cuadra que hoy
  está recorrida.
- **SC-003**: Al terminar la salida que completa una zona, la notificación aparece en menos de 10 s.

## Assumptions

- Lo medido usa los mismos 10 m que lo ajustado: los puntos que llegan ya descartaron lo que el
  teléfono declaró impreciso (más de 30 m).
- Las zonas terminadas no se recalculan: la regla nueva vale para las activas (FR-016 de la 008).

## Out of Scope

- **Arreglar el ajuste a calles.** El servicio prefiere la calle más grande, y no se controla desde
  la app. Lo ajustado se sigue usando para dibujar.
- **Felicitar otros logros** (rachas, km, patentes). Es otra feature.
- **Mostrar el porcentaje subir durante la salida.** Sigue fuera, como en la 008.
