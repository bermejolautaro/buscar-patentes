# Implementation Plan: Pulido de interfaz

**Branch**: `002-pulido-de-interfaz` | **Date**: 2026-08-29 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-pulido-de-interfaz/spec.md`

## Summary

La especificación 001 dejó el producto funcionando y validado en dispositivo. Esta hace que
se pueda usar sin pelearse con él: arregla el modo oscuro —que está roto por tres causas
encadenadas, no por gusto—, pone el mapa donde está el jugador con un control para volver,
convierte los puntos del mapa en marcadores con el número adentro que se pueden tocar para
ver, corregir y borrar, y saca el teclado de encima al abrir.

Enfoque técnico: no entra ninguna dependencia nueva. Todo se construye sobre lo que ya está
instalado —Material 3 trae el tema y la hoja inferior, MapLibre trae el texto en capas, la
agrupación de marcadores y el seguimiento de posición— y los pocos iconos que faltan se
dibujan como vectores propios. La única escritura nueva en la base es una sentencia acotada
que toca el número y nada más.

El detalle y las alternativas descartadas están en [research.md](./research.md).

## Technical Context

**Language/Version**: Kotlin 2.3.21, JDK 17. Sin cambios respecto de la 001.

**Primary Dependencies**: las mismas de la 001 — Compose con Material 3, Room, Play
Services Location, MapLibre Native Android. **Ninguna nueva**: D3 rechaza
`material-icons-extended` explícitamente contra el Principio IV.

**Storage**: Room, sin entidades ni migración nuevas. Se agrega una sentencia de
actualización acotada al número (D8).

**Testing**: JUnit sobre dominio en JVM. Esta feature no agrega lógica pura, así que no
agrega pruebas: la única escritura nueva ya queda cubierta por la prueba de inmutabilidad
existente, que lee el fuente del DAO.

**Target Platform**: Android 8.0 (API 26) o superior. El dispositivo de validación corre
Android 15 con navegación por gestos, que es donde aparecieron los problemas de insets.

**Project Type**: mobile-app, un solo módulo Android. Sin cambios de estructura.

**Performance Goals**: la carga rápida sigue entrando en 10 segundos (SC-009). El mapa con
marcadores, texto y agrupación no puede introducir tirones al desplazarlo.

**Constraints**: el estado del mapa nunca bloquea la captura (FR-009, hereda FR-038 de la
001). La evidencia sigue siendo inmutable. Nada de esto necesita red.

**Scale/Scope**: cinco pantallas, decenas de marcadores en pantalla. Un usuario, un
teléfono.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitución **1.1.0**, enmendada el 2026-08-29 por pedido de esta misma feature.

### Principio I — Captura sin fricción (NO NEGOCIABLE)

**Gate**: la carga rápida llega de app cerrada a registro guardado en 4 interacciones o
menos.

| Momento | Resultado |
|---|---|
| Pre-Phase 0 | **PASS con enmienda previa** — el presupuesto era 3 y esta feature lo sube a 4. La Governance exige enmendar antes de avanzar, y se hizo: 1.0.0 → 1.1.0, MINOR, con el Sync Impact Report actualizado |
| Post-Phase 1 | **PASS** — el teclado guardado cuesta exactamente un toque: abrir, tocar el campo, tipear, confirmar. Cuatro, que es el techo nuevo |

**Lo que la enmienda NO relajó**, y este diseño respeta: ubicación y timestamp se siguen
resolviendo solos, no hay pantalla intermedia ni selección de modo, y la ficha del registro
(D7) vive fuera del camino de captura — se llega a ella tocando un marcador, nunca al
guardar.

**Riesgo vigilado**: el techo de 4 no es un punto de partida. La enmienda lo dice
explícitamente. Cualquier tarea de esta feature que agregue un toque más al camino de
captura no entra.

### Principio II — La evidencia es inmutable (NO NEGOCIABLE)

**Gate**: ubicación, precisión y timestamp no se pueden editar después de creados.

| Momento | Resultado |
|---|---|
| Pre-Phase 0 | **En riesgo** — el pedido decía "poder editarlas", sin acotar qué |
| Post-Phase 1 | **PASS** — se acotó con el usuario: corregir es el número y el texto de la patente. D8 lo hace estructural con una sentencia que no nombra ningún campo de evidencia |

La distinción que sostiene esto: el número es lo que el jugador **leyó**; la ubicación y la
hora son lo que el teléfono **midió**. El principio protege la medición.

Y una trampa que D8 descarta por escrito: "borrar y volver a crear con la misma ubicación"
parece respetuoso del principio y es exactamente la carga retroactiva de metadata que
prohíbe.

**Verificación**: la prueba de inmutabilidad existente lee el fuente del DAO y falla si
alguna sentencia escribe sobre latitud, longitud, precisión o timestamp. La operación nueva
queda cubierta sin escribir una prueba nueva.

### Principio III — Local-first, sin red

**Gate**: toda captura se completa sin conexión; fallar es ruidoso.

**PASS** — nada de esta feature toca el camino de captura ni necesita red. El tema, los
iconos vectoriales y las capas del mapa son locales. FR-009 repite explícitamente que el
comportamiento del mapa no puede demorar el guardado, que es lo único que esta feature
podría romper.

### Principio IV — Alcance personal (YAGNI)

**Gate**: sin cuentas ni backend; toda abstracción con dos usos reales; toda dependencia
justificada contra lo ya instalado.

**PASS** — y este principio es el que más forma le dio al plan:

- **Ninguna dependencia nueva.** D3 rechaza `material-icons-extended` y dibuja tres
  vectores propios. D5 usa la agrupación que MapLibre ya trae en lugar de escribir
  separación de marcadores. D7 usa la hoja inferior que Material 3 ya trae.
- **Sin selector de tema dentro de la app.** Se sigue el del sistema. Un ajuste que nadie
  pidió es configuración de más.
- **Sin suite de interfaz.** Se sostiene la decisión de la 001.
- **D2 evitó construir dos veces** —después de corregirse a sí mismo—: el usuario **sí**
  tiene el último build. Lo que cambió el plan es más fino: el botón "Volver" ya tenía el
  inset aplicado cuando lo reportó, así que el arreglo no es de insets sino de estructura
  —una barra superior de verdad—, y el centrado del mapa puede estar resuelto por T084.

### Flujo de desarrollo

**Gate**: toda lógica no trivial deja una prueba ejecutable.

**PASS** — esta feature no agrega lógica pura. El único candidato, la corrección del
número, ya está cubierto por la prueba de inmutabilidad. La nota al pie de research.md lo
deja registrado para que no se lea como un descuido.

### Resultado del gate

**Los cuatro principios pasan.** Complexity Tracking queda vacío: no hay ninguna violación
que justificar.

## Project Structure

### Documentation (this feature)

```text
specs/002-pulido-de-interfaz/
├── plan.md              # Este archivo
├── spec.md              # Qué se construye
├── research.md          # Phase 0: D1 a D9, con los dos diagnósticos primero
├── data-model.md        # Phase 1: la operación nueva y lo que el mapa necesita saber
├── quickstart.md        # Phase 1: cómo validar cada user story
├── contracts/
│   └── ui-y-mapa.md     # Phase 1: C1 a C4, la superficie de interfaz
├── checklists/
│   └── requirements.md  # Calidad de la spec — 16/16
└── tasks.md             # Phase 2 (/speckit-tasks — todavía no existe)
```

### Source Code (repository root)

Archivos que esta feature toca. La estructura de la 001 no cambia: sigue siendo un solo
módulo con `domain/` aislado.

```text
app/src/main/
├── res/
│   ├── values/themes.xml          # D1: el tema de ventana deja de ser oscuro fijo
│   └── drawable/                  # D3: los vectores propios, nuevo directorio
├── java/ar/lauta/buscarpatentes/
│   ├── ui/
│   │   ├── Tema.kt                # D1: esquemas claro y oscuro — archivo nuevo
│   │   ├── MainActivity.kt        # D1: usa el tema propio en vez de MaterialTheme pelado
│   │   ├── PantallaPrincipal.kt   # D1 Surface, teclado guardado, control de recentrado
│   │   ├── PantallaBusqueda.kt    # D1: Surface
│   │   ├── PantallaAjustes.kt     # D1: Surface
│   │   ├── PantallaRecorridos.kt  # D1: Surface
│   │   ├── BotonesAccion.kt       # D3: iconos reales en lugar del emoji
│   │   ├── FichaDeRegistro.kt     # D7: la hoja inferior — archivo nuevo
│   │   └── Captura.kt             # D8: la corrección del número
│   ├── data/Daos.kt               # D8: la sentencia acotada al número
│   └── mapa/Mapa.kt               # D4, D5, D6, D9: texto, agrupación, toque, recentrado
```

**Structure Decision**: sin cambios. Un solo módulo `app/`, con `domain/` como única
división —lógica pura sin Android, testeable en JVM—. Esta feature casi no toca `domain/`:
es interfaz, y por eso concentra el trabajo en `ui/` y `mapa/`.

Dos archivos nuevos, los dos justificados contra un requisito concreto: `Tema.kt` por
FR-001 y FR-002, y `FichaDeRegistro.kt` por FR-012. Ninguno es andamiaje especulativo.

## Orden de ataque sugerido

No es dependencia técnica estricta, pero importa:

1. **Mirar el mapa en el dispositivo y recargar registros de prueba** (D2). T084 pudo haber
   resuelto la mayor parte de la User Story 2, y la base quedó vacía tras la reinstalación
   limpia del 2026-08-29, así que sin registros no se puede validar nada de la User Story 3.
2. **US1, el tema y la barra superior** (D1, D2). Toca las cinco pantallas, así que todo lo
   demás se construye encima. Hacerlo después obligaría a revisar cada pantalla dos veces.
3. **US5, iconos y contraste** (D3). Va pegado a US1 por la misma razón: comparte archivos.
4. **US2, mapa y recentrado** (D9). Independiente del resto.
5. **US3, marcadores y ficha** (D4 a D8). Es la más grande, y la única que toca la base.
6. **US4, el teclado** (una línea). Va última a propósito: es la que toca el principio NO
   NEGOCIABLE, y conviene medirla con todo lo demás ya en su lugar.

## Complexity Tracking

> Se completa solo si el Constitution Check tiene violaciones que justificar.

**Vacío.** Ninguna violación. La única tensión de esta feature —el presupuesto de
interacciones del Principio I— se resolvió enmendando la constitución antes de planificar,
que es lo que su Governance manda, y no arrastrando una excepción.
