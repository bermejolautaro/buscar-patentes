# Specification Quality Checklist: Ir a buscar la que toca

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-09-28
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

- Una pregunta resuelta en la sesión del 2026-09-28: **dónde vive el filtro** (FR-004, FR-005).
  Va en la barra de arriba de la pantalla principal, detrás de una lupa, y la pantalla de
  búsqueda se retira (FR-013).
- **Google Maps** aparece nombrado en los requisitos (FR-019). No es un detalle de
  implementación que se coló: es el pedido del jugador, y se aparta a propósito de la D2 de la
  003, que entregaba el destino a cualquier app de mapas.
- Requisitos de specs anteriores que esta revierte o retira a propósito, y así están anotados:
  - **FR-030d de la 004** (destacar la que toca sin color): vuelve el fondo verde.
  - **FR-022 de la 001** (números seguidos cubiertos): sale de la pantalla.
  - **FR-011 a FR-014 de la 003** (pantalla de búsqueda): su trabajo pasa a la barra.
  - **Caso borde de la 004** "una patente sin votos cae en el escalón medio": desde el
    2026-09-28 arranca en 0 (FR-024).
- **FR-024** registra un cambio que se implementó antes de esta spec (la confianza arranca en 0).
  No genera tareas nuevas salvo verificar que el resto de la feature ordena con ese modelo.
- Cuatro ítems del `TODO.md` quedan fuera y piden spec propia o ya están resueltos: esconder
  patentes (hecho en la 004), diagonales + camino crudo/ajustado, confianza por turno, y zonas
  objetivo.
- La US5 (pines que se corren) es la de más riesgo técnico: vale revisar en `/speckit-plan` si
  el motor del mapa lo permite sin costo de fluidez, y si no, bajarla o separarla.
- Spec lista para `/speckit-plan`.
