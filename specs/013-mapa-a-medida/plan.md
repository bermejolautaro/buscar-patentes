# Implementation Plan: Mapa a medida

**Branch**: `013-mapa-a-medida` | **Date**: 2026-11-24 | **Spec**: [spec.md](./spec.md)

## Summary

Tres arreglos de la pantalla principal:

- **D1. El botón de ubicación** lee la última posición de la `LocationState` de la librería, la misma
  que dibuja el punto azul. Se va la copia `EstadoDelMapa.jugador`, que se actualizaba adentro de
  `LocationTrackingEffect` y dejaba de hacerlo al arrastrar el mapa.
- **D2. La zona del mapa principal**: `Zonas.enElMapa(zonas, elegida)` decide cuál, pura y probada.
  La elegida se guarda en `estado_del_juego.zonaEnElMapa`.
- **D3. Sin grupos desde un zoom**: `Acomodo.para` recibe `zoomSinGrupos`; desde ese zoom devuelve
  todas sueltas en su lugar. Se guarda en `estado_del_juego.zoomSinGrupos`.

La base pasa a la **10**: dos columnas que pueden ir vacías, con `SQL_9_10` en común. El respaldo las
lleva sin cambiar de formato.

## Technical Context

**Language/Version**: Kotlin 2.4.20 (Multiplatform). **Dependencias nuevas**: ninguna.

**Storage**: Room, base 10: `estado_del_juego.zonaEnElMapa` (Long?) y `zoomSinGrupos` (Int?).

**Testing**: `ZonasTest` (qué zona se ve), `AcomodoTest` (sin grupos desde un zoom), `MigracionTest`
(de la 9 a la 10). El botón se prueba en el teléfono.

## Constitution Check

| Principio | Cómo lo cumple |
|---|---|
| I. Captura sin fricción | No toca la carga |
| II. La evidencia es inmutable | No toca salidas ni patentes |
| III. Local-first | Sin red |
| IV. YAGNI | Sin dependencias. Las dos preferencias van en la tabla que ya las guarda. El botón usa lo que la librería ya tiene, y se borra la copia |
| Regla de la 006 | Todo común |
| Flujo: lógica con prueba | `ZonasTest`, `AcomodoTest`, `MigracionTest` |

**Resultado**: pasa.

## Project Structure

```text
shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/
├── data/Entidades.kt, Daos.kt, BaseDeDatos.kt, Migraciones.kt   # base 10
├── data/Zonas.kt                 # + enElMapa
├── domain/Acomodo.kt             # + zoomSinGrupos
├── mapa/Colecciones.kt           # pasa zoomSinGrupos
├── mapa/MapaDeFondo.kt           # recentrar con la LocationState; + zoomSinGrupos
├── respaldo/Respaldo.kt          # VERSION_BASE = 10
└── ui/PantallaPrincipal.kt, PantallaZonas.kt, PantallaAjustes.kt
shared/src/androidMain/…/ConstruirBase.kt, shared/src/iosMain/…/ConstruirBase.ios.kt   # MIGRACION_9_10
```
