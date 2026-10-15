# Tasks: Patentes desde la que toca

**Input**: Design documents from `/specs/009-patentes-desde-la-que-toca/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [quickstart.md](./quickstart.md)

**Tests**: la constitución pide una prueba para la lógica con ramas: T001 va antes que T002.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1)

---

## Phase 1: Setup

Nada: sin dependencias, sin tablas, sin migración.

---

## Phase 2: Foundational

Nada que bloquee: la única historia arranca sola.

---

## Phase 3: User Story 1 - Ver solo lo que falta buscar (Priority: P1) 🎯 MVP

**Goal**: el mapa arranca mostrando la que toca y las que vienen, y el botón cicla entre mayor o
igual, todas y ninguna.

**Independent Test**: [quickstart](./quickstart.md) §2 a §5.

- [X] T001 [P] [US1] Escribir `shared/src/commonTest/kotlin/ar/lauta/buscarpatentes/domain/VistaDePatentesTest.kt`:
  - el ciclo: mayor o igual, todas, ninguna, mayor o igual;
  - en mayor o igual, con el que toca en 313: se ven la 313 y la 500, no la 120; con el que toca todavía sin leer, se ven todas;
  - con la lupa en la 120: la 120 se ve en mayor o igual y en todas, y la 500 no; en ninguna no se ve nada.
- [X] T002 [US1] Crear `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/domain/VistaDePatentes.kt`: el `enum` con `siguiente()` y `muestra(numero, queToca, filtro)` (D1). T001 pasa.
- [X] T003 [P] [US1] Crear `shared/src/commonMain/composeResources/drawable/ic_patentes_desde.xml`: el pin de `ic_patentes` más chico, a la izquierda, y un "≥" a la derecha (D3).
- [X] T004 [US1] En `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/ui/LeyendaDelMapa.kt`, cambiar `patentesVisibles` por la vista y el número que toca: "· Desde la 313" en mayor o igual sin lupa, "· Solo la 120" con lupa en las vistas que muestran, "· Patentes ocultas" en ninguna (D2).
- [X] T005 [US1] En `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt`:
  - `patentesVisibles` pasa a `vistaDePatentes`, en `remember` y arrancando en mayor o igual (D4); actualizar el comentario;
  - los marcadores del mapa se filtran con `muestra`;
  - el botón cicla con `siguiente()`, con el ícono y la descripción de cada vista.

**Checkpoint**: la historia completa.

---

## Phase 4: Polish & Cross-Cutting Concerns

- [X] T006 Correr `./gradlew :shared:testAndroidHostTest :app:assembleDebug`. Todo verde, sin warnings nuevos.
- [X] T007 [P] `README.md`: la 009 en la lista de specs y en los quickstarts, y `VistaDePatentesTest` en la tabla de pruebas.
- [ ] T008 Armar lo que el jugador instala: el APK a `/sdcard/Download/buscar-patentes.apk` (sin tocar `NavegadorCamiones.apk`, verificando el hash) y `gh workflow run ios.yml --ref 009-patentes-desde-la-que-toca`, con el `.ipa` a `Downloads\buscar-patentes-ios\patentes-desde`.
- [ ] T009 El jugador corre el [quickstart](./quickstart.md) en el teléfono.
- [ ] T010 Antes de cada push, revisar el diff: ni coordenadas de la zona de juego, ni nombres de calles o barrios de la lista de "Nunca subir", ni capturas del mapa, ni respaldos.

---

## Dependencies & Execution Order

- T001 antes que T002 (la prueba falla primero).
- T002 antes que T004 y T005. T003 suelto.
- T004 y T005 tocan archivos distintos, pero T005 llama a la leyenda nueva: T004 primero.
- La rama sale de la 008, que todavía no está en `main`: los teléfonos ya tienen la base en la 8, y
  un APK hecho desde `main` la tendría en la 7.

## Parallel Example: User Story 1

```text
T001 VistaDePatentesTest.kt   |   T003 ic_patentes_desde.xml
```

## Implementation Strategy

Una sola historia: es el MVP y la feature entera. Se valida con el quickstart y se cierra.
