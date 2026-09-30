# Implementation Plan: Caminos sin diagonales

**Branch**: `007-caminos-sin-diagonales` | **Date**: 2026-09-29 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/007-caminos-sin-diagonales/spec.md`

## Summary

Las diagonales del camino ajustado no son ruido del GPS: salen de cómo la app lee la respuesta del
servicio de ajuste. Probado con una caminata inventada (D1 de [research.md](./research.md)), hay
tres causas:
- el servicio devuelve los pedazos de un camino cortado **pegados en una sola línea**;
- un corte de señal se **rellena con calles que nadie caminó**;
- lo que no pudo emparejar **desaparece**.

**Enfoque**:
- El camino ajustado pasa a ser una lista de **tramos**. Cada tramo de señal (el mismo corte de
  250 m del trazo crudo) se pide por separado.
- Cada respuesta se arma con lo que dicen sus aristas y sus puntos emparejados: los pedazos se unen
  por los puntos medidos, nunca por una recta (D2).
- Se guarda en el mismo campo de texto con un prefijo `2:`, que distingue lo nuevo de lo viejo sin
  tocar el esquema ni el respaldo. Lo viejo vuelve solo a la cola de ajuste (D3).
- El dibujo trata igual los dos caminos: tramos continuos y huecos punteados (D4).
- El detalle de una salida suma un interruptor entre lo ajustado y lo medido (D5).

## Technical Context

**Language/Version**: Kotlin 2.4.20 (Multiplatform), igual que la 006.

**Primary Dependencies**: las de siempre, sin ninguna nueva:
- Compose Multiplatform 1.12.1 con Material 3, para el `Switch`;
- maplibre-compose 0.18.0;
- Room 2.8.4;
- kotlinx-serialization-json, para leer la respuesta;
- el servicio público de Valhalla (FOSSGIS), el mismo de la 003.

**Storage**: Room, tabla `recorrido`, columna `caminoAjustado` (texto). **Sin cambio de esquema**:
la base sigue en la versión 7 (D3).

**Testing**: `kotlin.test` en `commonTest`. Corre en la JVM con `:shared:testAndroidHostTest` y en el
simulador del iPhone en la nube.

**Target Platform**: Android 8.0+ e iOS 15.5+, con el mismo código común.

**Project Type**: app móvil (Kotlin Multiplatform, módulo `shared` más `app/` e `iosApp/`).

**Performance Goals**:
- el cambio de vista en el detalle se ve en menos de 1 s (SC-003);
- el reajuste de todas las salidas viejas corre una vez, en segundo plano, sin bloquear ninguna
  pantalla (FR-035 de la 003).

**Constraints**:
- los puntos medidos no se tocan (Principio II, FR-005);
- nada de esto en el camino de captura (Principios I y III);
- sin conexión todo sigue como hoy: pendiente y crudo.

**Scale/Scope**: un jugador, unas decenas de salidas de cientos de puntos cada una. Los pedidos al
servicio son uno por tramo de señal: unas decenas la primera vez, uno o dos por salida después.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo cumple |
|---|---|
| I. Captura sin fricción | No toca la carga: el ajuste corre después de terminar una salida y el interruptor está en el detalle |
| II. La evidencia es inmutable | Los puntos medidos se leen y nunca se escriben. `caminoAjustado` es una interpretación que se recalcula (FR-034 de la 003). Una prueba compara los puntos antes y después del reajuste |
| III. Local-first | El ajuste sigue siendo accesorio: sin red queda pendiente y se ve el crudo. La captura no depende de nada de esto |
| IV. YAGNI | Sin dependencias, sin columnas, sin migración, sin abstracciones nuevas. Un prefijo de dos caracteres en vez de un estado en la base. `CaminoGuardado.leer` existe porque lo usan dos pantallas |
| Regla de la 006 (Android e iPhone iguales) | Todo es código común; ningún `expect` nuevo |

**Resultado**: pasa, antes y después del diseño. Sin violaciones que justificar.

## Project Structure

### Documentation (this feature)

```text
specs/007-caminos-sin-diagonales/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── camino-ajustado.md
└── tasks.md             # lo arma /speckit-tasks
```

### Source Code (repository root)

```text
shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/
├── domain/
│   ├── Polilinea.kt          # + codificar(); decodificar por tramo
│   └── CaminoAjustado.kt     # nuevo: armar() (D2) y CaminoGuardado.leer()/escribir() (D3)
├── ubicacion/
│   └── AjustarACalles.kt     # un pedido por tramo de señal, lee edges y matched_points
├── data/
│   └── Daos.kt               # sinAjustar(): también lo que no empieza con "2:"
├── mapa/
│   ├── Colecciones.kt        # Trazo con tramos; sin la rama "el ajustado no se corta"
│   └── MapaDeFondo.kt        # el encuadre usa los tramos
└── ui/
    ├── PantallaPrincipal.kt  # arma los trazos con CaminoGuardado
    └── PantallaRecorridos.kt # DetalleDeSalida: el interruptor (D5)

shared/src/commonTest/kotlin/ar/lauta/buscarpatentes/
├── domain/CaminoAjustadoTest.kt   # nuevo
├── domain/PolilineaTest.kt        # + ida y vuelta de codificar
├── mapa/ColeccionTest.kt          # los trazos por tramos
└── ubicacion/AjustarACallesTest.kt # + la lectura de una respuesta real recortada
```

**Structure Decision**: todo vive en `shared/commonMain`, donde ya están el ajuste, el dibujo y las
pantallas. No se toca `androidMain`, `iosMain`, `app/` ni `iosApp/`.

## Complexity Tracking

Sin violaciones de la constitución.
