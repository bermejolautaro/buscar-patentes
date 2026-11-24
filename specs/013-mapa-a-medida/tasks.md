# Tasks: Mapa a medida

**Input**: [plan.md](./plan.md), [spec.md](./spec.md)

## Phase 2: Foundational

- [X] T001 [P] `MigracionTest`: de la 9 a la 10 suma `zonaEnElMapa` y `zoomSinGrupos` en null sin tocar nada más.
- [X] T002 La base 10: las dos columnas en `EstadoDelJuego`, `fijarZonaEnElMapa` y `fijarZoomSinGrupos` en el DAO, `SQL_9_10`, `MIGRACION_9_10` en los dos `ConstruirBase`, `VERSION_BASE = 10` y el comentario de `RespaldoTest`. T001 pasa.

## Phase 3: US1 - El botón de ubicación lleva a donde estoy (P1)

- [X] T003 [US1] En `mapa/MapaDeFondo.kt`, `EstadoDelMapa` guarda la `LocationState`, y `jugador` es su `lastLocation`. `LocationTrackingEffect` ya no la copia (D1).

## Phase 4: US2 - Elegir qué zona se sigue en el mapa (P2)

- [X] T004 [P] [US2] `ZonasTest`: `enElMapa` da la elegida si existe; si no, o si se borró, la última creada; sin zonas, null.
- [X] T005 [US2] `Zonas.enElMapa(zonas, elegida)` en `data/Zonas.kt`. T004 pasa.
- [X] T006 [US2] `ui/PantallaPrincipal.kt`: una sola zona, la de `enElMapa`, con sus cuadras que faltan según su estado (FR-003).
- [X] T007 [US2] `ui/PantallaZonas.kt`: en el detalle, "Se ve en el mapa principal" o el botón "Seguir en el mapa".

## Phase 5: US3 - Sin grupos desde un acercamiento (P3)

- [X] T008 [P] [US3] `AcomodoTest`: con `zoomSinGrupos`, desde ese zoom todas sueltas en su lugar; antes, como hoy.
- [X] T009 [US3] `domain/Acomodo.kt`, `mapa/Colecciones.kt` y `mapa/MapaDeFondo.kt` reciben `zoomSinGrupos`; `ui/PantallaPrincipal.kt` lo pasa. T008 pasa.
- [X] T010 [US3] `ui/PantallaAjustes.kt`: "Patentes sueltas desde": Nunca, Barrio, Calle, Cuadra, Muy cerca.

## Phase 6: Polish

- [X] T011 `./gradlew :shared:testAndroidHostTest :app:assembleDebug`, sin warnings nuevos.
- [X] T012 [P] `README.md` con la 013.
- [ ] T013 El `.ipa` a `Downloads\buscar-patentes-ios\mapa-a-medida`. La base pasa a la 10: respaldo antes de instalar.
- [ ] T014 El jugador lo prueba: el botón después de arrastrar y caminar, elegir zona, y "Calle".
- [ ] T015 Antes de cada push, revisar el diff: nada de la zona de juego ni respaldos.
