# Implementation Plan: Cuadras de las teselas

**Branch**: `012-cuadras-de-las-teselas` | **Date**: 2026-11-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/012-cuadras-de-las-teselas/spec.md`

## Summary

Las calles de una zona dejan de salir de Overpass y salen de las teselas vectoriales de OpenFreeMap,
las del mapa de fondo. Un decodificador propio lee las dos capas de calles, une las calles en una
grilla común, detecta las esquinas por coordenadas y entrega las mismas `Via` que daba Overpass:
`Cuadras.armar` no cambia ([research.md](./research.md)).

## Technical Context

**Language/Version**: Kotlin 2.4.20 (Multiplatform), igual que la 011.

**Primary Dependencies**: **ninguna nueva**. El decodificador de Mapbox Vector Tiles es propio (D2).

**Storage**: sin cambios. La base sigue en la 9.

**Testing**: `kotlin.test` en `commonTest`. Teselas sintéticas armadas en la prueba para cada caso,
y las teselas reales de la franja del centro para el armado completo (D7).

**Target Platform**: Android 8.0+ e iOS 15.5+.

**Project Type**: app móvil (Kotlin Multiplatform, módulo `shared` más `app/` e `iosApp/`).

**Performance Goals**: una zona de barrio, de una a cuatro teselas de unos 500 KB, armada en menos
de 10 s con conexión (SC-001). El decodificado y las esquinas, en menos de 1 s en el teléfono.

**Constraints**: la red se usa una vez por zona, como antes (Principio III). Nada en el camino de
captura (Principio I).

**Scale/Scope**: zonas de hasta 2.000 cuadras.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo cumple |
|---|---|
| I. Captura sin fricción | No toca la carga |
| II. La evidencia es inmutable | No toca salidas ni patentes |
| III. Local-first | La red se usa una vez por zona, como con Overpass. Sin red, la zona espera |
| IV. YAGNI | Sin dependencias: el decodificador son unas 100 líneas y una librería de protobuf sería una dependencia para leer cuatro campos. Overpass se borra en vez de quedar de respaldo |
| Regla de la 006 | Todo es común salvo `bajar`, una función por teléfono como `postear` |
| Flujo: lógica no trivial con prueba | `TeselasTest` (decodificar, recortar, esquinas, bordes, nombres, filtro) y las pruebas con datos reales de `CuadrasTest` y `CoberturaDeZonaTest` |

**Resultado**: pasa.

## Project Structure

```text
specs/012-cuadras-de-las-teselas/
├── plan.md
├── research.md
├── quickstart.md
└── tasks.md

shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/
├── domain/Teselas.kt             # nuevo: decodificar, recortar, esquinas, nombres → Via (D2 a D5)
├── ubicacion/BuscarCuadras.kt    # TileJSON y teselas en vez de Overpass
├── mapa/MapaDeFondo.kt           # ConfigMapa: la URL del TileJSON
└── plataforma/Plataforma.kt      # + expect bajar

shared/src/androidMain/…/plataforma/Plataforma.android.kt   # actual bajar
shared/src/iosMain/…/plataforma/Plataforma.ios.kt           # actual bajar

shared/src/commonTest/kotlin/ar/lauta/buscarpatentes/domain/
├── TeselasTest.kt                # nuevo
├── RespuestaDeTeselas.kt         # nuevo: las teselas de la franja (D7)
├── RespuestaDeOverpass.kt        # se borra
├── CuadrasTest.kt                # los datos reales salen de las teselas
└── CoberturaDeZonaTest.kt        # ídem
```

## Complexity Tracking

Sin violaciones de la constitución.
