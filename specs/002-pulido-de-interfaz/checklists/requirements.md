# Specification Quality Checklist: Pulido de interfaz

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-08-28
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

- **16/16.** Los dos marcadores abiertos eran choques con principios NO NEGOCIABLES de la
  constitución, y se resolvieron con el usuario el 2026-08-29:

  1. **US4 / FR-019 — teclado minimizado contra el Principio I.** El principio fijaba la
     carga rápida en 3 interacciones, con el teclado ya arriba al abrir. Se eligió subir el
     presupuesto a 4 y **enmendar la constitución**, en lugar de dejar el principio
     diciendo una cosa y el código haciendo otra. Enmienda aplicada: constitución 1.0.0 ->
     1.1.0, con el Sync Impact Report actualizado. El FR-021 de esta spec deja registrado
     que la enmienda va antes que la implementación, como pide la Governance.

  2. **US3 / FR-014 — "editarlas" contra el Principio II.** Se resolvió que corregir
     significa el número y el texto de la patente, nunca la evidencia. El Principio II
     queda intacto: el número es lo que el jugador **leyó**, no lo que el teléfono midió.
     El SC-007 lo verifica comparando cada registro antes y después de usar la corrección.

- FR-020 declara explícitamente que esta especificación supera al FR-016 de la 001. Esa
  anotación en la 001 es trabajo de esta feature, no se da por hecha.

- El pedido de "acercar el mapa a mi ubicación" (US2) está **parcialmente construido**: la
  tarea T084 de la especificación 001, cerrada el mismo día que se escribió esta spec,
  activó el punto de posición y el seguimiento con zoom de calle. Lo que esta
  especificación agrega encima es el control de recentrado (FR-007), el comportamiento al
  arrastrar (FR-006) y el caso sin permiso (FR-008). Conviene verificar en el dispositivo
  qué quedó cubierto antes de dimensionar la historia.

- **Corrección del 2026-08-29**: la primera versión de D2 en `research.md` afirmaba que el
  usuario no tenía instalado el último build. Es falso, verificado en el dispositivo. Lo
  que sí es cierto y cambia el trabajo: el reporte del botón "Volver" se hizo sobre una
  versión que **ya tenía** el inset aplicado, así que la causa no son los insets sino la
  falta de una barra superior con jerarquía. Eso mueve trabajo dentro de la User Story 1,
  no lo elimina.
