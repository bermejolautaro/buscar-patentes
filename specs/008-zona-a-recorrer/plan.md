# Implementation Plan: Zona a recorrer

**Branch**: `008-zona-a-recorrer` | **Date**: 2026-09-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/008-zona-a-recorrer/spec.md`

## Summary

El jugador dibuja una zona tocando esquinas. La app le pide a OpenStreetMap, una sola vez, las
calles del área, las parte en **cuadras** de esquina a esquina y se las guarda. Desde ahí, todo es
local:
- el porcentaje sale de comparar esas cuadras con los caminos ajustados que la 007 ya guarda;
- una cuadra está recorrida cuando el 80% de su largo queda a 10 m o menos de alguna salida que
  cuenta.

**Enfoque**:
- **Las cuadras** salen de Overpass (D1). Se arman con los nodos: una cuadra corre entre dos
  esquinas, y las dos manos de una avenida con bulevar cuentan como una (D2, D3). Se mide con datos
  reales de una zona pública: 2.000 cuadras llegan en 6 s.
- **La cuenta** es una función pura, con una grilla para que 2.000 cuadras se calculen en
  milisegundos (D5). Con la respuesta real del servicio de ajuste, las cuadras caminadas dan 100% y
  las cruzadas en la esquina, 14% o menos.
- **Se calcula en vivo** mientras la zona está activa, y **se congela** al completarse o al cerrarse
  (D7). Borrar una salida o cambiar "desde cuándo cuenta" se refleja solo.
- **Dos tablas nuevas**, base 8, con la migración en común (D7). El respaldo las lleva sin cambiar
  de formato.
- **El mapa** es el mismo `MapaDeFondo`, con el borde, las cuadras, un toque suelto para las esquinas
  y un toque sobre una cuadra para quitarla (D9).
- **Una pantalla nueva**, Zonas, con la lista, el dibujo y el detalle, y un botón para llegar desde
  la pantalla principal.

## Technical Context

**Language/Version**: Kotlin 2.4.20 (Multiplatform), igual que la 007.

**Primary Dependencies**: las de siempre, **sin ninguna nueva**:
- Compose Multiplatform 1.12.1 con Material 3, para la lista, el `DatePicker` y los diálogos;
- maplibre-compose 0.18.0, para el dibujo y los toques;
- Room 2.8.4, para las dos tablas;
- kotlinx-serialization-json, para leer la respuesta de Overpass;
- kotlinx-datetime, para "desde cuándo cuenta";
- Overpass API (`overpass-api.de`) como servicio nuevo. Es libre y sin clave, igual que el Valhalla
  de la 003.

**Storage**: Room.
- Tablas nuevas: `zona` y `cuadra` ([data-model.md](./data-model.md)).
- La base pasa de la versión 7 a la **8**.
- La migración 7→8 está en `commonMain`: la usan el builder del Android y el del iPhone.

**Testing**: `kotlin.test` en `commonTest`. Corre en la JVM con `:shared:testAndroidHostTest` y en el
simulador del iPhone en la nube. Las pruebas usan dos respuestas reales de zonas públicas: una de
Overpass recortada, y la de Valhalla que ya existe.

**Target Platform**: Android 8.0+ e iOS 15.5+, con el mismo código común.

**Project Type**: app móvil (Kotlin Multiplatform, módulo `shared` más `app/` e `iosApp/`).

**Performance Goals**:
- La pantalla de una zona de 2.000 cuadras abre en menos de 1 s, también sin conexión (SC-003).
- Quitar una cuadra actualiza el porcentaje en menos de 1 s (SC-004).
- La búsqueda de cuadras tarda menos de 30 s con conexión (SC-001). La medida es de 6 s para el
  máximo.

**Constraints**:
- Las salidas se leen y nunca se escriben (Principio II, FR-019).
- Nada de esto está en el camino de captura (Principios I y III).
- Sin conexión solo se frena la búsqueda de cuadras de una zona nueva; todo lo demás anda.
- La pantalla principal no muestra ningún número de la zona (FR-010, FR-020a de la 004).

**Scale/Scope**:
- Un jugador.
- Unas pocas zonas, de hasta 2.000 cuadras cada una.
- Unas decenas de salidas.
- Un pedido a Overpass por zona, en toda su vida.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo cumple |
|---|---|
| I. Captura sin fricción | No toca la carga. La pantalla principal suma un botón en la fila de abajo, al lado de Salidas y Ajustes, fuera del camino de captura |
| II. La evidencia es inmutable | La zona solo lee `iniciadoEn`, `estado` y `caminoAjustado`. Una prueba verifica que ninguna función de la zona escribe en `recorrido` ni en `punto_de_trayecto` (FR-019). Las zonas no son evidencia: se borran y se editan |
| III. Local-first | La red se usa una vez por zona, para las cuadras. Sin red, la zona espera en "Buscando las calles" y todo lo demás funciona. La captura no depende de nada de esto |
| IV. YAGNI | Sin dependencias nuevas. Un servicio externo nuevo, justificado en el D1: no hay otra forma de saber qué calles tiene un área. Dos tablas, porque las cuadras no se pueden recalcular sin red. Lo recorrido no se guarda mientras se puede calcular (D7). Los parámetros nuevos de `MapaDeFondo` los usan dos pantallas |
| Regla de la 006 (Android e iPhone iguales) | Todo es código común. Lo único por plataforma es el `User-Agent` de `postear`, una línea en cada `actual` |

**Resultado**: pasa, antes y después del diseño. Sin violaciones que justificar.

## Project Structure

### Documentation (this feature)

```text
specs/008-zona-a-recorrer/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── cuadras.md
└── tasks.md             # lo arma /speckit-tasks
```

### Source Code (repository root)

```text
shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/
├── data/
│   ├── Entidades.kt          # + Zona, Cuadra, EstadoZona
│   ├── Daos.kt               # + ZonaDao
│   ├── BaseDeDatos.kt        # versión 8, entidades nuevas
│   ├── Migraciones.kt        # nuevo: MIGRACION_7_8, común a los dos teléfonos
│   └── Zonas.kt              # nuevo: revisar() para completar; cerrar, quitar, borrar
├── domain/
│   ├── Borde.kt              # nuevo: validez del dibujo, adentro/afuera, el rectángulo con margen
│   ├── Cuadras.kt            # nuevo: de las vías a las cuadras, gemelas (Z2)
│   └── CoberturaDeZona.kt    # nuevo: la cuenta (Z3), con la grilla
├── ubicacion/
│   └── BuscarCuadras.kt      # nuevo: el pedido a Overpass y la cola de zonas en BUSCANDO (D8)
├── respaldo/
│   └── Respaldo.kt           # VERSION_BASE = 8
├── mapa/
│   ├── Colecciones.kt        # + las colecciones de borde y cuadras
│   └── MapaDeFondo.kt        # + zonas, onTocarMapa, onTocarCuadra (D9)
└── ui/
    ├── AppBuscarPatentes.kt  # + Destino.ZONAS
    ├── PantallaPrincipal.kt  # + el botón, las zonas en el mapa, revisar() después del ajuste
    ├── PantallaZonas.kt      # nuevo: lista, dibujo, detalle
    └── Formatos.kt           # + la duración

shared/src/androidMain/…/data/ConstruirBase.kt   # + MIGRACION_7_8
shared/src/iosMain/…/data/ConstruirBase.ios.kt   # + MIGRACION_7_8
shared/src/androidMain/…/plataforma/Plataforma.android.kt   # User-Agent en postear
shared/src/iosMain/…/plataforma/Plataforma.ios.kt           # User-Agent en postear

shared/src/commonTest/kotlin/ar/lauta/buscarpatentes/
├── domain/BordeTest.kt              # nuevo
├── domain/CuadrasTest.kt            # nuevo, con una respuesta de Overpass recortada
├── domain/CoberturaDeZonaTest.kt    # nuevo, con la respuesta de Valhalla de la 007
├── domain/RespuestaDeOverpass.kt    # nuevo: el fixture
├── respaldo/RespaldoTest.kt         # un respaldo de la 7 en la base 8
└── ui/FormatosTest.kt               # + la duración
```

**Structure Decision**: casi todo vive en `shared/commonMain`, donde ya están el ajuste, el dibujo,
la base y las pantallas. Fuera de ahí se tocan dos cosas: el builder de cada teléfono, que suma la
migración, y `postear`, que suma una cabecera. `app/` e `iosApp/` no cambian.

## Complexity Tracking

Sin violaciones de la constitución.
