# Research: Patentes desde la que toca

No había nada que investigar afuera: todo sale del código de la 004 y la 005. Quedan las decisiones.

## D1. Un `enum` con la regla adentro

**Decision**: `VistaDePatentes { DESDE_LA_QUE_TOCA, TODAS, NINGUNA }` en `domain`, con
`siguiente()` y `muestra(numero, queToca, filtro)`. Reemplaza al `patentesVisibles: Boolean` de la
pantalla principal.

**Rationale**: el ciclo vive en el tipo por lo mismo que el de `ModoMapa`: agregar una vista
obligaría a acordarse de un `when` suelto en un `onClick`. La regla queda en una función pura, que es
lo que se prueba. Con la lupa, `filtro` gana en las dos vistas que muestran (FR-006).

**Alternatives considered**: un filtro por expresión en la capa del mapa. Se descartó por el D4 de
la 005: la fuente seguiría agrupando las ocultas con las visibles.

## D2. La leyenda

**Decision**: en mayor o igual dice "· Desde la 313"; en ninguna, "· Patentes ocultas", como hoy.
Con la lupa, "· Solo la 120" reemplaza a "Desde la 313", porque lo que se ve es ese número.

**Rationale**: FR-035 de la 004 y FR-004 de esta: un mapa con menos pines tiene que decirlo.

## D3. El ícono

**Decision**: `ic_patentes_desde`, el pin de `ic_patentes` más chico y un "≥" al lado. Todas sigue
con `ic_patentes` y ninguna con `ic_patentes_ocultas`.

**Rationale**: el ícono muestra el estado del mapa, igual que el de recorridos (FR-005).

## D4. No se guarda

**Decision**: `remember`, no la base. Cada arranque abre en mayor o igual.

**Rationale**: FR-035a de la 004, que esta spec mantiene (FR-003).
