# Implementation Plan: Recorrido en curso

**Branch**: `010-recorrido-en-curso` | **Date**: 2026-10-17 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/010-recorrido-en-curso/spec.md`

## Summary

Cinco cosas alrededor de una salida, sin tocar cómo se graba:

- **Confirmar antes de terminar**, en la pantalla principal (US1).
- **Avisar si se cortó**: en el iPhone, una notificación que la app tiene que seguir postergando
  mientras graba; si la app muere, suena sola (US2, D4). En el Android el servicio sobrevive a sacar
  la app de recientes, y se verifica (D5).
- **La barra de arriba** dice tiempo, km y calorías de la salida en curso (US3).
- **Km y calorías** de cada salida, en la lista y el detalle de Salidas (US4). Los km salen de los
  mismos tramos que dibuja el mapa (D1); las calorías son las activas de caminar, con el peso que el
  jugador carga en Ajustes (D2, D3).
- **Estadísticas** por día, semana y mes, con barras de km, desde Salidas (US5, D7).

Una sola cuenta arma el resumen de una salida, y la usan todos (D6). Lo único que se guarda de nuevo
es el peso: la base pasa a la 9.

## Technical Context

**Language/Version**: Kotlin 2.4.20 (Multiplatform), igual que la 009.

**Primary Dependencies**: Compose Multiplatform 1.12.1, Room 2.8.4, kotlinx-datetime. **Ninguna
nueva.** Las notificaciones del iPhone usan `UserNotifications`, que ya usa el aviso de vencimiento.

**Storage**: Room. Una columna nueva, `estado_del_juego.pesoKg`, y la base pasa de la 8 a la 9 con
la migración en común ([data-model.md](./data-model.md)). El formato del respaldo no cambia.

**Testing**: `kotlin.test` en `commonTest`, en la JVM con `:shared:testAndroidHostTest` y en el
simulador del iPhone en la nube. Las cuentas son funciones puras con su contrato
([contracts/cuentas.md](./contracts/cuentas.md)).

**Target Platform**: Android 8.0+ e iOS 15.5+, con el mismo código común. Lo único por teléfono es
el aviso de la salida cortada.

**Project Type**: app móvil (Kotlin Multiplatform, módulo `shared` más `app/` e `iosApp/`).

**Performance Goals**:
- Las estadísticas abren en menos de 1 s con un año de salidas, unos 100.000 puntos (SC-007).
- La barra recalcula los km con cada punto nuevo, unos cientos de puntos por salida.

**Constraints**:
- La fila de botones de la pantalla principal no cambia: la confirmación es un diálogo y la barra
  suma una línea (Principio I).
- Los puntos y las patentes no se escriben (Principio II, FR-021).
- Nada usa red (Principio III).
- La lista de Salidas sigue sin totales acumulados arriba (FR-020a de la 004): los km son de cada
  salida, y los totales viven en Estadísticas.

**Scale/Scope**: un jugador, unas pocas salidas por semana, de una a dos horas.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo cumple |
|---|---|
| I. Captura sin fricción | La carga no cambia. Terminar una salida suma un toque, y empezarla no. La barra suma una línea sin achicar el campo |
| II. La evidencia es inmutable | Todo se calcula leyendo puntos y patentes. Lo único que se escribe es el peso, que no es evidencia |
| III. Local-first | Sin red. El aviso del iPhone es una notificación local |
| IV. YAGNI | Sin dependencias nuevas: las barras son `Box` de Compose. El peso va en la tabla de preferencias que ya existe. Una sola cuenta para cinco lugares (D6). El aviso del Android se suma solo si la verificación lo pide (D5) |
| Regla de la 006 | Todo es código común salvo el aviso, que el iPhone necesita y el Android no (D4, D5) |
| Flujo: lógica no trivial con prueba | `CaminataTest` (metros, calorías), `EstadisticasTest` (períodos, sumas, barras), `FormatosTest` (textos) y `MigracionTest` (de la 8 a la 9) |

**Resultado**: pasa, antes y después del diseño.

## Project Structure

### Documentation (this feature)

```text
specs/010-recorrido-en-curso/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── cuentas.md
└── tasks.md             # lo arma /speckit-tasks
```

### Source Code (repository root)

```text
shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/
├── data/
│   ├── Entidades.kt              # + EstadoDelJuego.pesoKg
│   ├── Daos.kt                   # + fijarPeso
│   ├── BaseDeDatos.kt            # versión 9
│   └── Migraciones.kt            # + SQL_8_9
├── domain/
│   ├── Caminata.kt               # nuevo: metros, calorías, el resumen de una salida (D1, D2, D6)
│   └── Estadisticas.kt           # nuevo: períodos, totales, barras (D7)
├── respaldo/Respaldo.kt          # VERSION_BASE = 9
└── ui/
    ├── Formatos.kt               # + horasYMinutos, caminata
    ├── BarraDelJuego.kt          # + la línea de la salida en curso
    ├── PantallaPrincipal.kt      # + la confirmación, el ciclo del tiempo, los km en vivo
    ├── PantallaRecorridos.kt     # + km y calorías en la lista y el detalle, el botón de Estadísticas
    ├── PantallaEstadisticas.kt   # nuevo: pestañas, flechas, totales y barras
    └── PantallaAjustes.kt        # + el peso

shared/src/iosMain/…/plataforma/Grabacion.ios.kt     # + el aviso que se reprograma (D4)
shared/src/androidMain/…/data/ConstruirBase.kt       # + MIGRACION_8_9
shared/src/iosMain/…/data/ConstruirBase.ios.kt       # + MIGRACION_8_9

shared/src/commonTest/kotlin/ar/lauta/buscarpatentes/
├── domain/CaminataTest.kt        # nuevo
├── domain/EstadisticasTest.kt    # nuevo
├── ui/FormatosTest.kt            # + los textos del contrato C3
├── data/MigracionTest.kt         # + de la 8 a la 9
└── respaldo/RespaldoTest.kt      # un respaldo de la 8 en la base 9
```

**Structure Decision**: casi todo en `shared/commonMain`. Fuera de ahí, la migración en cada
`ConstruirBase` y el aviso en la grabación del iPhone. `app/` e `iosApp/` no cambian.

## Complexity Tracking

Sin violaciones de la constitución.
