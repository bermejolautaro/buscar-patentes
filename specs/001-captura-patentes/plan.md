# Implementation Plan: Captura de patentes

**Branch**: `001-captura-patentes` | **Date**: 2026-08-28 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-captura-patentes/spec.md`

## Summary

App Android personal para el juego de patentes en secuencia. El jugador anota patentes
que ve en la calle con el mínimo roce posible —abrir, tipear tres dígitos, tocar `+`— y
la app resuelve sola ubicación y timestamp. Cuando el juego llega a un número que el
jugador ya tiene anotado, el teléfono le avisa al pasar cerca del lugar para que vaya y
la fotografíe en vivo.

Enfoque técnico: Android nativo en Kotlin, todo local, sin servidor ni cuentas. Tres
mecanismos de ubicación distintos según el uso —lectura precisa bajo demanda para la
captura, muestreo espaciado en foreground service para el recorrido, y la Geofencing
API del sistema para el aviso con la app cerrada. Mapa MapLibre con caché ambiente, que
cubre el guardado offline de zonas visitadas sin código propio.

El detalle y las alternativas descartadas están en [research.md](./research.md).

## Technical Context

**Language/Version**: Kotlin 2.3.21, JDK 17 como target. La máquina compila con JDK 21,
que produce bytecode 17 sin problema.

**Build**: Gradle 9.7.1, AGP 9.3.2. **AGP 9 trae Kotlin incorporado**: el plugin
`org.jetbrains.kotlin.android` ya no va y el build falla si está declarado.

**Primary Dependencies**: Jetpack Compose (UI), Room (persistencia), Google Play Services
Location (FusedLocationProviderClient + GeofencingClient), MapLibre Native Android (mapa)

**Storage**: Room sobre SQLite para registros, estado del juego, recorridos y puntos de
trayecto. Fotos como archivos en almacenamiento privado de la app. Caché de tiles
gestionado por MapLibre.

**Testing**: JUnit sobre la lógica de dominio en JVM. Tests instrumentados de Room para
las consultas. Sin suite de UI (ver D8).

**Target Platform**: Android 8.0 (API 26) o superior, con Google Play Services.
compileSdk 36. `platforms/android-37` existe pero solo en canal preview, así que las
dependencias quedaron fijadas a las últimas que compilan contra 36: Compose BOM
2026.06.01, core-ktx 1.18.0, lifecycle 2.10.0, activity-compose 1.12.4. Las versiones más
nuevas de cada una exigen compileSdk 37.

**Project Type**: mobile-app, un solo módulo Android. Sin backend.

**Performance Goals**: app cerrada a registro guardado en ≤10 s y ≤3 interacciones
(SC-001, SC-002). Aviso de proximidad a ≤150 m con la app cerrada (SC-009). Recorrido de
2 h ≤5% de batería (SC-010). Avisos activos en reposo ≤3% de batería por día (SC-012).

**Constraints**: captura funciona sin conexión (Principio III). Ubicación, precisión y
timestamp inmutables tras la creación (Principio II). Sin cuentas, sin login, sin
servidor propio (FR-013). El estado del mapa nunca bloquea la captura (FR-037).

**Scale/Scope**: un usuario, un teléfono. Cientos de registros de captura a lo largo de
la vida del juego. ~100 recorridos, bajo 1 MB de puntos de trayecto. El espacio lo
dominan las fotos y el caché de mapa, no los datos.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principio I — Captura sin fricción (NO NEGOCIABLE)

**Gate**: la carga rápida llega de app cerrada a registro guardado en ≤3 interacciones.

| Momento | Resultado |
|---|---|
| Pre-Phase 0 | PASS — FR-016 a FR-019 lo fijan en la spec |
| Post-Phase 1 | **PASS** — D6 lo resuelve: `MainActivity` compone el campo y pide foco con `FocusRequester` en la primera composición; el mapa se infla por `AndroidView` detrás, asincrónico. FR-038 queda garantizado por construcción, no por disciplina |

**Riesgo vigilado**: la inicialización de MapLibre es lo único que puede demorar el
primer frame. La mitigación es estructural (el teclado no espera al mapa), y el
quickstart la verifica con cronómetro.

### Principio II — La evidencia es inmutable (NO NEGOCIABLE)

**Gate**: ubicación, precisión y timestamp no se pueden editar después de creados.

| Momento | Resultado |
|---|---|
| Pre-Phase 0 | PASS — FR-005, FR-006 |
| Post-Phase 1 | **PASS** — el data model lo hace estructural: el DAO expone `insertar`, `adjuntarFoto` y `marcarCompartida`, y nada más. No existe un update que toque los campos de evidencia. La ausencia de esa operación **es** el cumplimiento |

D3 refuerza: la captura usa `getCurrentLocation()`, no `getLastLocation()`. Guardar una
posición de hace minutos como si fuera del momento violaría este principio en silencio.

### Principio III — Local-first, sin red

**Gate**: toda captura se completa y persiste sin conexión; fallar es ruidoso.

| Momento | Resultado |
|---|---|
| Pre-Phase 0 | PASS — FR-007, FR-008 |
| Post-Phase 1 | **PASS** — Room es local. La red solo aparece en tiles de mapa, y C5 más FR-038 garantizan que su ausencia no toca el camino de captura. Un insert fallido propaga error visible |

Nota de diseño derivada: "calles exploradas" se calcula agregando puntos sobre una
grilla, **no** con geocodificación inversa. Resolver nombres de calles exigiría red y
rompería este principio.

### Principio IV — Alcance personal (YAGNI)

**Gate**: sin cuentas ni backend; toda abstracción con dos usos reales; toda dependencia
justificada contra lo ya instalado y la biblioteca estándar.

| Momento | Resultado |
|---|---|
| Pre-Phase 0 | PASS — FR-013 |
| Post-Phase 1 | **PASS con una salvedad registrada** — ver Complexity Tracking |

Decisiones que este principio forzó, y que sin él habrían ido para el otro lado:

- **Sin framework de DI** (D7). Contenedor manual en `Application`. Hilt es lo estándar
  en apps grandes; esta no lo es.
- **Sin limpieza automática de recorridos**. La cuenta da menos de 1 MB en cien
  recorridos. Construir purga o simplificación de trayectos para eso es exactamente lo
  que el principio prohíbe.
- **Sin suite de UI** (D8). Las pruebas van sobre la lógica pura, que es donde puede
  romperse algo silenciosamente.
- **Sin Kotlin Multiplatform** (D1). No hay iOS que compartir.

Cada dependencia queda anclada a un requisito: Play Services Location a FR-002 y FR-039,
MapLibre a FR-034 y FR-042, Room a FR-007, Compose a FR-016.

### Flujo de desarrollo

**Gate**: toda lógica no trivial deja una prueba ejecutable.

**PASS** — D8 identifica las cinco funciones puras que la constitución obliga a cubrir, y
el data model las repite en Derivados. Son las mismas cinco, lo que confirma que el
diseño aisló la lógica testeable de Android.

### Resultado del gate

**Los cuatro principios pasan.** Una entrada en Complexity Tracking, justificada por
pedido explícito del usuario.

## Project Structure

### Documentation (this feature)

```text
specs/001-captura-patentes/
├── plan.md              # Este archivo
├── spec.md              # Qué se construye
├── research.md          # Phase 0: D1 a D8, con alternativas descartadas
├── data-model.md        # Phase 1: entidades, invariantes, derivados
├── quickstart.md        # Phase 1: cómo validar cada user story
├── contracts/
│   └── os-and-share.md  # Phase 1: C1 a C5, la superficie externa
├── checklists/
│   └── requirements.md  # Calidad de la spec
└── tasks.md             # Phase 2 (/speckit-tasks — todavía no existe)
```

### Source Code (repository root)

```text
app/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── AndroidManifest.xml        # permisos, foreground service, receiver de geofence
    │   ├── java/ar/lauta/buscarpatentes/
    │   │   ├── App.kt                 # contenedor manual de dependencias (D7)
    │   │   ├── domain/                # lógica pura, sin Android. Lo que D8 testea
    │   │   │   ├── Patente.kt         # parseo de formato viejo y Mercosur (FR-010)
    │   │   │   ├── Cobertura.kt       # consecutivos cubiertos (FR-022), calles exploradas (FR-032)
    │   │   │   └── Avisos.kt          # selección de geofences (D4), decisión de aviso (FR-028/029/041)
    │   │   ├── data/
    │   │   │   ├── Entidades.kt       # Room entities según data-model.md
    │   │   │   ├── Daos.kt            # sin update de campos de evidencia (Principio II)
    │   │   │   ├── BaseDeDatos.kt
    │   │   │   └── AlmacenFotos.kt    # archivos en almacenamiento privado
    │   │   ├── ubicacion/
    │   │   │   ├── LectorUbicacion.kt # getCurrentLocation para captura (D3)
    │   │   │   ├── Geofences.kt       # registro y reconciliación (C2)
    │   │   │   ├── GeofenceReceiver.kt
    │   │   │   └── ServicioRecorrido.kt # foreground service (C3)
    │   │   ├── mapa/
    │   │   │   └── Mapa.kt            # MapLibre, caché ambiente (C5)
    │   │   └── ui/
    │   │       ├── PantallaPrincipal.kt  # mapa + campo + tres botones (FR-016, FR-036)
    │   │       ├── PantallaBusqueda.kt   # buscar por número (FR-011)
    │   │       ├── PantallaRecorridos.kt # recorridos y cobertura (FR-031, FR-032)
    │   │       └── PantallaAjustes.kt    # contador, avisos, espacio de mapa (FR-020, FR-040, FR-045)
    │   └── res/
    ├── test/                          # JUnit sobre domain/ — las cinco funciones de D8
    └── androidTest/                   # consultas de Room
```

**Structure Decision**: un solo módulo Android (`app/`). No hay backend, no hay
frontend separado, no hay librería compartida — el Principio IV descarta partir esto en
módulos que hoy nadie consume.

La única división que sí se sostiene es `domain/`: lógica pura sin dependencias de
Android. No es una abstracción especulativa, es la condición para que las cinco pruebas
de D8 corran en JVM sin emulador. Y si algún día aparece iOS, es lo único que valdría la
pena portar.

## Resolución del hueco abierto en el checklist

`/speckit-clarify` dejó marcado que **FR-042 a FR-045** (caché de mapa, vista offline,
límite de espacio, borrado) no colgaban de ninguna user story.

Este plan los ubica sin inventar una historia nueva:

| Requisito | Dónde vive |
|---|---|
| FR-042, FR-043 — cachear zonas visitadas, verlas offline | `mapa/Mapa.kt`, comportamiento del mapa de la User Story 1. Sin interfaz propia: es automático |
| FR-044 — acotar espacio, desalojo LRU | Configuración de MapLibre al inicializar. Sin interfaz |
| FR-045 — ver espacio y borrarlo | `PantallaAjustes.kt`, junto al contador del juego y el interruptor de avisos |

Nada de esto justifica una user story separada: FR-042 a FR-044 no tienen interfaz —
ocurren solos— y FR-045 son controles en una pantalla de ajustes que ya
existe por FR-020 y FR-040. `/speckit-tasks` debe generar sus tareas bajo la User Story
1 y bajo la pantalla de ajustes, no como bloque aparte.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| **User Story 5 (recorrido grabado) arrastra un foreground service, dos entidades y el permiso de ubicación en segundo plano** — es la parte más cara del sistema, y el propio usuario la llamó "un plus" | Pedida explícitamente. FR-023 a FR-026 y FR-031 a FR-033 la fijan. Responde una pregunta real: qué calles ya recorrí buscando, y por dónde ir la próxima vez | Se evaluó y se rechazó dejarla afuera: el objetivo declarado de *encontrar las patentes de vuelta* ya lo resuelve la User Story 4 sin grabar nada, pero la cobertura de calles es un objetivo distinto que ninguna otra historia cubre. El costo se acotó llevándola a P5, bajando el muestreo a 30 s / 50 m (FR-024) y prohibiendo explícitamente usar precisión de captura para el trayecto (FR-025). **Si hay que recortar alcance, esta es la primera candidata** |

Ninguna otra dependencia ni estructura del diseño necesita justificación: cada una está
anclada a un requisito concreto de la spec, y las alternativas descartadas están
documentadas en research.md.
