# Implementation Plan: Zona completa

**Branch**: `011-zona-completa` | **Date**: 2026-10-28 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/011-zona-completa/spec.md`

## Summary

Dos cambios chicos sobre las zonas de la 008:

- **Lo medido también cuenta.** Cada salida le pasa a la cuenta de la zona sus tramos ajustados
  **y** los tramos de lo que midió el teléfono, cortados en los huecos de 250 m. La cuenta
  (`CoberturaDeZona`) no cambia: ya mira cualquier tramo que pase a 10 m (D1).
- **Felicitaciones.** `Zonas.revisar()`, que es el único lugar donde una zona pasa a completada, manda
  una notificación por cada zona que completa. Una función nueva por teléfono, `notificar`, y en el
  iPhone un delegado para que se vea con la app abierta (D2). Al terminar una salida, la pantalla
  principal revisa las zonas enseguida (D3).

## Technical Context

**Language/Version**: Kotlin 2.4.20 (Multiplatform), igual que la 010.

**Primary Dependencies**: las de siempre. `NotificationCompat` en el Android y `UserNotifications` en
el iPhone, que ya se usan. **Ninguna nueva.**

**Storage**: sin cambios. La base sigue en la 9.

**Testing**: `kotlin.test` en `commonTest`, en la JVM y en el simulador del iPhone en la nube.

**Target Platform**: Android 8.0+ e iOS 15.5+.

**Project Type**: app móvil (Kotlin Multiplatform, módulo `shared` más `app/` e `iosApp/`).

**Performance Goals**: la cuenta de una zona suma los ~12.000 puntos medidos a la grilla de
segmentos: sigue en milisegundos (D5 de la 008).

**Constraints**: las salidas se leen y nunca se escriben (Principio II). Sin red (Principio III).

**Scale/Scope**: una zona activa, unas decenas de salidas.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo cumple |
|---|---|
| I. Captura sin fricción | No toca la carga |
| II. La evidencia es inmutable | La cuenta lee puntos y caminos; no escribe nada de las salidas |
| III. Local-first | Sin red: lo medido cuenta aunque la salida no se haya ajustado |
| IV. YAGNI | Sin dependencias. `CoberturaDeZona` no cambia: se le pasan más tramos. `notificar` tiene dos usos de entrada, uno por teléfono, y una sola llamada |
| Regla de la 006 | Lo común es común; la notificación es una función por teléfono |
| Flujo: lógica no trivial con prueba | `ZonasTest` (qué tramos le pasa cada salida a la cuenta) y `CoberturaDeZonaTest` (el pasaje pegado a la diagonal) |

**Resultado**: pasa.

## Decisiones

### D1. Lo medido entra como tramos de la salida

`Zonas.salidasParaContar(recorridos, puntosPorRecorrido)` arma cada `SalidaParaContar` con los tramos
ajustados, si hay, más `Geo.tramos` de sus puntos medidos. Cuenta toda salida terminada, ajustada o no
(FR-003). La pantalla principal ya lee todos los puntos para dibujar; la versión `suspend` los lee
para `revisar` y la pantalla de zonas.

**Alternatives considered**: bajar el 80% para las cuadras cortas (arregla este caso y no el de un
pasaje largo), o pedirle al servicio de ajuste un radio de búsqueda más chico (la 007 lo sacó porque
un tercio de los puntos no emparejaba).

### D2. `notificar` por teléfono

- `expect fun notificar(id: Int, titulo: String, texto: String)` en `plataforma/Plataforma.kt`.
- **Android**: un canal "Zonas" de importancia normal y `NotificationManagerCompat`. Sin permiso de
  notificaciones no hace nada.
- **iPhone**: una notificación sin disparador, que se entrega enseguida. Para que se vea con la app
  abierta, un `UNUserNotificationCenterDelegate` que pide banner y sonido, puesto al arrancar en
  `MainViewController`.
- El id es el de la zona: dos zonas, dos notificaciones.

### D3. Revisar al terminar una salida

La pantalla principal, cuando la salida en curso pasa a null, espera hasta 5 s a que la base la cierre
(cada teléfono la cierra por su lado), ajusta a calles lo pendiente y revisa las zonas. Así la
felicitación llega al terminar la salida que completa la zona (SC-003), y no la próxima vez que se
abre la pantalla.

## Project Structure

```text
specs/011-zona-completa/
├── plan.md
├── quickstart.md
└── tasks.md

shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/
├── data/Zonas.kt                 # salidasParaContar con lo medido; revisar felicita
├── plataforma/Plataforma.kt      # + expect notificar
└── ui/PantallaPrincipal.kt       # revisar al terminar una salida

shared/src/androidMain/…/plataforma/Plataforma.android.kt   # actual notificar
shared/src/iosMain/…/plataforma/Plataforma.ios.kt           # actual notificar y el delegado
shared/src/iosMain/…/MainViewController.kt                  # pone el delegado

shared/src/commonTest/kotlin/ar/lauta/buscarpatentes/
├── data/ZonasTest.kt             # + los tramos de cada salida
└── domain/CoberturaDeZonaTest.kt # + el pasaje pegado a la diagonal
```

## Complexity Tracking

Sin violaciones de la constitución.
