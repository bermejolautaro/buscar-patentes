# Implementation Plan: Patentes desde la que toca

**Branch**: `009-patentes-desde-la-que-toca` | **Date**: 2026-10-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/009-patentes-desde-la-que-toca/spec.md`

## Summary

El botón de patentes de la pantalla principal pasa de dos vistas a tres: **mayor o igual** (la de
arranque), **todas** y **ninguna**. Es un cambio de la pantalla principal y nada más: los datos, la
base y el mapa quedan como están.

**Enfoque**:
- Un `enum` con las tres vistas, el ciclo y la regla de qué número se muestra (D1). Es lógica con
  ramas, así que lleva su prueba.
- La pantalla le manda al mapa menos marcadores, como ya hace con el botón de esconder y con la lupa
  (D4 de la 005). El mapa no se entera.
- La leyenda dice la vista, y un ícono nuevo para mayor o igual (D2, D3).

## Technical Context

**Language/Version**: Kotlin 2.4.20 (Multiplatform), igual que la 008.

**Primary Dependencies**: Compose Multiplatform 1.12.1. **Ninguna nueva.**

**Storage**: ninguno. La vista no se guarda (FR-003), la base sigue en la 8.

**Testing**: `kotlin.test` en `commonTest`, en la JVM con `:shared:testAndroidHostTest` y en el
simulador del iPhone en la nube.

**Target Platform**: Android 8.0+ e iOS 15.5+, con el mismo código común.

**Project Type**: app móvil (Kotlin Multiplatform, módulo `shared` más `app/` e `iosApp/`).

**Performance Goals**: filtrar unos cientos de marcadores con una comparación cada uno, al cambiar la
vista o el número. Nada que medir.

**Constraints**: la fila de botones no suma ninguno; el de patentes cambia de ciclo (FR-033a de la
004, Principio I).

**Scale/Scope**: un botón, una leyenda, un ícono.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo cumple |
|---|---|
| I. Captura sin fricción | No toca la carga ni suma botones. El campo de número queda igual |
| II. La evidencia es inmutable | No escribe nada. Se retira el dibujo, no el dato (FR-007) |
| III. Local-first | Sin red |
| IV. YAGNI | Un `enum` que reemplaza un `Boolean`, sin dependencias. La regla vive en el `enum` porque la usan el mapa y la leyenda |
| Regla de la 006 | Todo es código común |
| Flujo: lógica no trivial con prueba | `VistaDePatentesTest` cubre el ciclo y qué número se ve en cada vista, con y sin lupa |

**Resultado**: pasa, antes y después del diseño.

## Project Structure

### Documentation (this feature)

```text
specs/009-patentes-desde-la-que-toca/
├── plan.md
├── research.md
├── quickstart.md
└── tasks.md             # lo arma /speckit-tasks
```

Sin `data-model.md` ni `contracts/`: no hay datos nuevos ni interfaces hacia afuera.

### Source Code (repository root)

```text
shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/
├── domain/VistaDePatentes.kt     # nuevo: las tres vistas, el ciclo, qué número se ve
└── ui/
    ├── PantallaPrincipal.kt      # el Boolean pasa a VistaDePatentes; el botón cicla
    └── LeyendaDelMapa.kt         # "Desde la 313" y "Patentes ocultas"

shared/src/commonMain/composeResources/drawable/
└── ic_patentes_desde.xml         # nuevo: el pin con un ≥

shared/src/commonTest/kotlin/ar/lauta/buscarpatentes/domain/
└── VistaDePatentesTest.kt        # nuevo
```

**Structure Decision**: todo en `shared/commonMain`. `app/` e `iosApp/` no cambian.

## Complexity Tracking

Sin violaciones de la constitución.
