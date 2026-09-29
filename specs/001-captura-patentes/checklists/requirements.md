# Specification Quality Checklist: Captura de patentes

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

- **Regresión cerrada** (2026-08-28): FR-042 a FR-045 no tenían escenarios de
  aceptación. Se agregaron cuatro: tres a la User Story 1 (mapa offline en zona ya
  recorrida, zona sin fondo guardado, límite de caché alcanzado) y uno a la User Story 3
  (ver y borrar el espacio del fondo de mapa en ajustes). La ubicación sigue lo que fijó
  `plan.md`.

- 16/16 items passing. Los 2 marcadores abiertos se resolvieron con las respuestas del
  usuario: foto opcional con dos botones de confirmación (FR-016 a FR-019) y contador
  del juego mantenido a mano (FR-020 a FR-022).
- Los botones `+` y cámara se consideran comportamiento de interfaz especificado por el
  usuario, no detalle de implementación: la spec no dice con qué se construyen.
- El BRIEF listaba 7 puntos abiertos. Cinco se resolvieron con defaults razonables
  documentados en Assumptions: multiusuario (Principio IV: no), GPS degradado (FR-009:
  se guarda marcado), formato de patente (FR-010: ambos, 3 dígitos), salida a WhatsApp
  (share nativo), retención (se archiva, no se borra). La tensión con la regla del
  grupo se declaró fuera de alcance del software.
- Sesión de `/speckit-clarify` del 2026-08-28: 5 preguntas, 5 respuestas. Agregaron el
  recorrido grabado, el aviso de proximidad siempre activo, el mapa de fondo con
  cacheo automático de zonas visitadas, y el muestreo espaciado del trayecto. La spec
  pasó de 22 a 45 requisitos funcionales y de 3 a 5 user stories.
- Quedan 14 edge cases sin decisión tomada (contador retrocedible, captura de número ya
  pasado, cámara sin número tipeado, reloj del teléfono, almacenamiento lleno,
  recorrido sin cerrar, interrupción del sistema operativo, avisos repetidos, avisos
  manejando, dos patentes cerca a la vez, señal perdida, límite de mapa alcanzado,
  mudanza o viaje, zona nueva sin conexión). No bloquean el plan: son decisiones de
  comportamiento local que `/speckit-plan` resuelve al diseñar cada flujo.
