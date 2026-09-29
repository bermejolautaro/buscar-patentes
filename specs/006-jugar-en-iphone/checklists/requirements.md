# Specification Quality Checklist: Jugar en el iPhone

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

- Una pregunta resuelta en la sesión del 2026-09-28: **el repositorio en GitHub es público**, limpio
  antes de subirlo (FR-005, FR-005a, SC-011). Hoy hay coordenadas de la zona en cuatro archivos de
  prueba (`AcomodoTest`, `GeoTest`, `PrioridadTest` y `ColeccionTest`), la captura
  `specs/005-buscar-la-que-toca/antes-grupos.png`, y dos emails personales en los commits.
- **GitHub, Sideloadly, el Apple ID gratis, WhatsApp y Google Maps** aparecen nombrados en los
  requisitos. No son detalles de implementación que se colaron: son el pedido del jugador, igual
  que Google Maps en la 005.
- **El Principio IV decide la mudanza**: el iPhone reemplaza al Android, y restaurar reemplaza, no
  fusiona (FR-017). Usar los dos teléfonos a la par pediría enmendar la constitución primero.
- **La constitución nombra la referencia de ubicación solo para Android** ("Restricciones
  técnicas"). El plan tiene que sumar la del iPhone con una enmienda, antes de las tareas.
- **Riesgo que el plan tiene que despejar primero**: que los avisos con la app cerrada y la salida
  con la pantalla apagada funcionen con un Apple ID gratis, en el iPhone del jugador. Si no
  funcionan, la US5 y la US6 cambian de forma, y conviene saberlo antes de portar el resto.
- El único cambio visible en el Android es el respaldo desde la app (FR-031).
