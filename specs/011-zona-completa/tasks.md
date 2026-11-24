# Tasks: Zona completa

**Input**: Design documents from `/specs/011-zona-completa/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [quickstart.md](./quickstart.md)

**Tests**: cada prueba va antes que su código.

## Format: `[ID] [P?] [Story] Description`

---

## Phase 1: Setup

Nada.

## Phase 2: Foundational

Nada que bloquee.

---

## Phase 3: User Story 1 - Una cuadra caminada cuenta aunque el ajuste se equivoque (Priority: P1) 🎯 MVP

**Independent Test**: [quickstart](./quickstart.md) §1 y §2.

- [X] T001 [P] [US1] En `shared/src/commonTest/kotlin/ar/lauta/buscarpatentes/domain/CoberturaDeZonaTest.kt`, el pasaje pegado a la diagonal: una cuadra recta de 35 m que lo ajustado cubre solo en las puntas, y lo medido entera. Con los dos tramos en la misma salida, la cuadra está recorrida.
- [X] T002 [P] [US1] En `shared/src/commonTest/kotlin/ar/lauta/buscarpatentes/data/ZonasTest.kt`, `salidasParaContar(recorridos, puntosPorRecorrido)`:
  - una salida ajustada lleva sus tramos ajustados más los medidos;
  - una sin ajustar lleva solo los medidos;
  - un salto de más de 250 m parte lo medido;
  - una en curso no cuenta.
- [X] T003 [US1] En `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/data/Zonas.kt`, `salidasParaContar` suma `Geo.tramos` de los puntos de cada salida terminada, ajustada o no (D1). La versión `suspend` lee los puntos. Actualizar el KDoc del objeto y de la función. T001 y T002 pasan.
- [X] T004 [US1] En `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt`, pasarle a `salidasParaContar` los puntos que ya lee.

## Phase 4: User Story 2 - Felicitaciones al completar una zona (Priority: P2)

**Independent Test**: [quickstart](./quickstart.md) §3.

- [X] T005 [US2] `expect fun notificar(id: Int, titulo: String, texto: String)` en `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/plataforma/Plataforma.kt`, con su `actual` (D2):
  - en `shared/src/androidMain/kotlin/ar/lauta/buscarpatentes/plataforma/Plataforma.android.kt`, con el canal "Zonas";
  - en `shared/src/iosMain/kotlin/ar/lauta/buscarpatentes/plataforma/Plataforma.ios.kt`, sin disparador;
  - el delegado del iPhone que muestra banner y sonido con la app abierta, puesto en `shared/src/iosMain/kotlin/ar/lauta/buscarpatentes/MainViewController.kt`.
- [X] T006 [US2] En `Zonas.revisar()`, por cada zona que completa: `notificar(zona.id, "¡Zona completada!", "Recorriste todas las cuadras de <nombre> en <duración>.")`. Usa `duracion` de `Formatos`, y "en el día" si fue el mismo día (FR-005, FR-007).
- [X] T007 [US2] En `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt`, al pasar la salida en curso a null: esperar hasta 5 s a que la base la cierre, ajustar lo pendiente y revisar las zonas (D3, FR-004).

## Phase 5: Polish

- [X] T008 Correr `./gradlew :shared:testAndroidHostTest :app:assembleDebug`. Todo verde, sin warnings nuevos.
- [X] T009 Verificar con el respaldo del jugador, fuera del repo, que la cuadra del pasaje queda recorrida y ninguna otra se pierde (SC-001, SC-002).
- [X] T010 [P] `README.md`: la 011 en la lista de specs, y en la sección de Zonas que lo medido también cuenta y la felicitación. `specs/008-zona-a-recorrer/spec.md`: anotar en el supuesto de las salidas ajustadas que la 011 lo cambia.
- [ ] T011 Armar lo que el jugador instala: el `.ipa` (`gh workflow run ios.yml --ref 011-zona-completa`) a `Downloads\buscar-patentes-ios\zona-completa`, y el APK si el Android está enchufado.
- [ ] T012 El jugador corre el [quickstart](./quickstart.md) §3.
- [ ] T013 Antes de cada push, revisar el diff: ni coordenadas, ni nombres de calles o de la zona, ni el respaldo.

## Dependencies & Execution Order

- T001 y T002 antes que T003; T004 después de T003.
- US2 (T005 a T007) después de US1: la notificación sale de `revisar`, que usa la cuenta nueva.
- T007 toca el mismo archivo que T004.
- La rama sale de la 010, que todavía no está en `main`: el iPhone ya tiene la base en la 9.

## Implementation Strategy

US1 sola ya completa la zona del jugador. US2 la celebra.
