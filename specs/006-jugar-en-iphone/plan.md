# Implementation Plan: Jugar en el iPhone

**Branch**: `006-jugar-en-iphone` | **Date**: 2026-09-28 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/006-jugar-en-iphone/spec.md`

## Summary

La app es solo de Android, y el jugador quiere jugar con un iPhone sin tener Mac. El plan tiene
cuatro piezas:

1. **Una sola app para los dos teléfonos.** El código pasa a Kotlin Multiplatform: las reglas, la
   base y las pantallas se comparten, y lo que es de cada sistema (ubicación, avisos, cámara,
   compartir) queda detrás de una costura chica (D1, D9).
2. **El iPhone se compila en la nube y se instala desde Windows.** El `.ipa` sale sin firmar, y
   Sideloadly lo firma en la PC con el Apple ID gratis (D4).
3. **Nada se pierde al reinstalar.** Los datos viven en el contenedor de la app, que la
   reinstalación conserva. Las fotos se buscan por nombre, porque la ruta cambia (D7). La app sabe
   cuándo vence y avisa antes (D15).
4. **Un respaldo que es una copia de la base.** Un archivo SQLite con las fotos adentro, igual en
   los dos teléfonos: sirve para mudarse al iPhone y para volver (D8).

**Enfoque técnico**:
- **Base**: el Android sigue abriendo la misma base, con el mismo esquema y las mismas migraciones
  (D6).
- **Mapa**: pasa a maplibre-compose en los dos teléfonos. Se valida primero en el Android, con línea
  de corte (D5).
- **Riesgo**: lo que depende de Apple (avisos con la app cerrada, ubicación con la pantalla apagada,
  que reinstalar conserve los datos) se prueba **primero**, con una app mínima en el iPhone del
  jugador, antes de portar nada (D21).

El detalle y lo descartado están en [research.md](./research.md).

## Technical Context

**Language/Version**: Kotlin **2.3.21 → 2.4.20**, JDK 17. Swift solo para el punto de entrada del
iPhone (unas 30 líneas).

**Primary Dependencies**:

| | Estado |
|---|---|
| Compose Multiplatform 1.12.1 (reemplaza al BOM de Compose de Android) | nueva |
| Room 2.8.4 | igual |
| `androidx.sqlite:sqlite-bundled` | nueva |
| Play Services Location 21.4.0 | igual, solo Android |
| **maplibre-compose 0.18.0** (reemplaza al MapLibre Android SDK 13.6.0) | nueva |
| `kotlinx-datetime` | nueva |
| `kotlinx-serialization-json` (solo la biblioteca) | nueva |
| `kotlinx-io-core` | nueva |
| `androidx.lifecycle` | sale: solo lo usaba `Mapa.kt` para el ciclo de vida del `MapView` |

Cada dependencia nueva está justificada en Complexity Tracking.

**Storage**: Room, **sin cambios de esquema** (v7). El Android usa el mismo archivo y el mismo modo.
El iPhone usa `BundledSQLiteDriver` en `Library/Application Support`. El respaldo es un archivo
SQLite ([contracts/respaldo.md](./contracts/respaldo.md)).

**Testing**:
- **Pruebas comunes** (`kotlin.test`, en `shared/src/commonTest`): corren en la JVM en la PC y en el
  simulador de iPhone en la nube (D17).
- **Nuevas**: `RespaldoTest`, `VencimientoTest`, `AlmacenFotosTest` y `FechasTest`. **Cambian**: `AvisosTest` (el límite como
  parámetro), `ColeccionTest` (tipos de maplibre-compose) y las coordenadas de cuatro pruebas (D22).
- **Solo JVM**: `InmutabilidadTest`, por la reflexión.

**Target Platform**:
- Android 8.0 (API 26) o superior, sin cambios. El respaldo pide Android 11.
- **iPhone con iOS 15.5 o superior**, `iosArm64`, instalado con Sideloadly y un Apple ID gratis.
  Validación en el iPhone del jugador; la versión exacta se anota en la prueba piloto.

**Project Type**: mobile-app en dos plataformas:
- `:shared` — Kotlin Multiplatform;
- `:app` — la app Android;
- `iosApp/` — el punto de entrada del iPhone, con proyecto XcodeGen.

**Performance Goals**: las mismas de la 004 y la 005.
- El mapa se desplaza sin tirones.
- El acomodo corre solo al cambiar de zoom entero.
- La carga rápida en 10 segundos o menos.
- Una salida de 2 horas gasta 5% de batería o menos, en los dos teléfonos (SC-007).

**Constraints**:
- **Sin Mac**: una compilación de iPhone tarda de 10 a 15 minutos en la nube. Por eso el código
  compartido se valida en el Android.
- **Apple ID gratis**: 7 días, 3 apps y ningún entitlement pago.
- **Ninguna credencial de Apple en la nube** (FR-003).
- **Repositorio público sin datos del jugador** (FR-005a).
- Todo sin red salvo compilar, el ajuste a calles y el mapa, como hoy.

**Scale/Scope**:
- Unas 7.200 líneas de hoy:
  - `domain/`, unas 970, se mudan casi sin cambios;
  - la UI, unas 3.400, se muda con cambios chicos en los puntos de plataforma;
  - `Mapa.kt`, 1.160, se reescribe sobre maplibre-compose;
  - `ubicacion/` y `data/`, unas 1.500, se mudan a `androidMain` sin reescribirse.
- Código nuevo estimado:
  - unas 800 líneas en `iosMain`;
  - unas 300 de respaldo;
  - unas 30 de Swift.
- Cientos de registros, un usuario y un teléfono a la vez.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitución **1.1.0 → 1.2.0** (D19). La enmienda va antes de las tareas, y es parte de este plan.

### Principio I — Captura sin fricción (NO NEGOCIABLE)

**Gate**: de app cerrada a registro guardado en 4 interacciones o menos.

**PASS.** La carga rápida es la misma pantalla de Compose en los dos teléfonos. El aviso de
vencimiento es una línea que no se toca y no entra en el camino de carga (FR-011). El permiso de
ubicación "siempre" no se pide al anotar, sino al activar los avisos o una salida (D13). Así, anotar
por primera vez pide lo mismo que en el Android: un permiso.

**Vigilado**: el teclado numérico del iPhone no tiene tecla de confirmar. La confirmación es el
botón `+` de la app, igual que hoy, así que no cambia la cuenta. El quickstart la cronometra (SC-004).

### Principio II — La evidencia es inmutable (NO NEGOCIABLE)

**Gate**: ubicación, precisión y hora no se editan después de creados; una lectura degradada se
guarda como degradada.

**PASS.**
- **El respaldo es una copia de SQLite**: ningún número pasa por texto (D8), y `RespaldoTest`
  compara los bits.
- **Restaurar reemplaza la base entera**: no edita registros.
- **`fotoRuta` no se reescribe**: se interpreta por nombre (D7).
- **Ubicación exacta apagada**: la lectura se guarda con la precisión que dio el sistema, degradada
  si corresponde, y se avisa. No se corrige (FR-024).

### Principio III — Local-first, sin red

**Gate**: la captura no depende de la red; perder un registro en silencio es el peor resultado.

**PASS.**
- Anotar funciona sin conexión en los dos teléfonos.
- La red aparece en la compilación, que no es de la app, y en el mapa, el ajuste a calles y Google
  Maps, como hoy.
- El respaldo es local, y se lleva por cable o por donde elija el jugador.

**A favor**: el iPhone suma una forma de perder datos que el Android no tenía, el vencimiento y el
borrado por error. La respuesta es visible y ruidosa: fecha en ajustes, aviso a 48 horas,
notificación a 24 horas y fecha del último respaldo (FR-010 a FR-012 y FR-020).

### Principio IV — Alcance personal (YAGNI)

**Gate**: sin cuentas ni backend; toda abstracción con dos usos reales; toda dependencia
justificada.

**PASS con complejidad declarada** (abajo).
- **Ni cuentas ni backend.** GitHub compila, no guarda datos.
- **Un teléfono a la vez**: restaurar reemplaza, no fusiona (FR-017). La enmienda D19 lo escribe.
- **Cada `expect` tiene exactamente dos `actual`**, el Android y el iPhone, así que cumple el gate de
  abstracciones. No hay interfaces de un solo uso ni inyección de dependencias (D9).

| Se agrega | Se borra o se reemplaza |
|---|---|
| Módulo `:shared` y `iosApp/` | — (se mudan archivos, no se duplican) |
| `Respaldo` (contrato R) y su pantalla en ajustes | — |
| `Vencimiento` (una función pura y dos líneas de UI) | — |
| `iosMain`: ubicación, vigilancia, grabación, cámara, compartir, preferencias, red | — |
| El mapa sobre maplibre-compose | `Mapa.kt` sobre MapLibre Android SDK |
| `kotlinx-datetime` en la UI | `java.text.SimpleDateFormat` y `java.util.Date` |
| `kotlinx-serialization-json` en `AjustarACalles` | `org.json` |

### Restricciones técnicas

**PASS después de la enmienda.** Hoy la constitución nombra solo la ubicación del Android. La 1.2.0
suma Core Location para el iPhone (D19). Los formatos de patente no se tocan: el parser es de
`domain/` y pasa entero.

### Flujo de desarrollo

**PASS.** Cada lógica nueva tiene su prueba:
- el respaldo: ida y vuelta, rechazos, foto faltante y ruta de otro teléfono;
- el vencimiento: leer el perfil y el borde de 48 horas;
- el límite de avisos como parámetro.

Las pruebas corren **también en el motor del iPhone**, que es la única prueba automática del
FR-022. Lo que queda sin prueba automática es pegamento con el sistema (Core Location, cámara, hoja
de compartir, notificaciones), y se valida con el [quickstart](./quickstart.md), empezando por la
prueba piloto.

### Re-check post-Phase 1

| Gate | Resultado | Qué fijó Phase 1 |
|---|---|---|
| I — Captura sin fricción | **PASS** | La costura P5 deja los permisos fuera del camino de carga. El aviso de vencimiento no se toca |
| II — Evidencia inmutable | **PASS** | El contrato R copia tablas sin convertir valores, y R8 exige comparar bits. data-model deja `fotoRuta` sin reescribir |
| III — Local-first | **PASS** | La base en `Application Support`, fuera del alcance de Archivos. Restaurar nunca deja datos a medias (R6) |
| IV — Alcance personal | **PASS con complejidad declarada** | La costura P1 a P6 tiene dos implementaciones por declaración. Siete dependencias nuevas, justificadas abajo, y una que sale |
| Restricciones técnicas | **PASS con enmienda** | D19 va antes de las tareas |
| Flujo de desarrollo | **PASS** | `RespaldoTest` y `VencimientoTest` definidos en R8 y P7. Pruebas en el simulador en C1 |

**Deuda declarada**:
- **El respaldo pide Android 11** (`VACUUM INTO`, D8). El teléfono del jugador lo cumple.
- **maplibre-compose está en beta**, con versión fijada y línea de corte (D5).
- **Si iOS no relanza la app por una región después de cerrarla deslizándola**, se documenta y no
  se arregla (D11).

## Project Structure

### Documentation (this feature)

```text
specs/006-jugar-en-iphone/
├── plan.md              # Este archivo
├── research.md          # Phase 0: D1 a D22
├── data-model.md        # Phase 1: la base no cambia; respaldo y vencimiento
├── quickstart.md        # Phase 1: prueba piloto, instalación y validación por historia
├── contracts/
│   ├── respaldo.md      # R: el archivo de respaldo
│   ├── plataforma.md    # P: la costura expect/actual
│   └── compilacion.md   # C: workflow, proyecto de Xcode e instalación
└── tasks.md             # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
settings.gradle.kts                 # + include(":shared")
gradle/libs.versions.toml           # Kotlin 2.4.20, CMP 1.12.1, maplibre-compose, sqlite-bundled…
.github/workflows/ios.yml           # C1

shared/                             # NUEVO — Kotlin Multiplatform
├── build.gradle.kts                # android (kotlin.multiplatform.library), iosArm64, iosSimulatorArm64
└── src/
    ├── commonMain/kotlin/ar/lauta/buscarpatentes/
    │   ├── domain/                 # ← app/…/domain, entero (Avisos.aRegistrar suma el límite)
    │   ├── data/                   # ← Entidades, Daos, BaseDeDatos (@ConstructedBy), AlmacenFotos (por nombre)
    │   ├── respaldo/               # NUEVO: Respaldo (contrato R), Vencimiento (D15)
    │   ├── mapa/                   # Mapa sobre maplibre-compose (D5)
    │   ├── ui/                     # ← app/…/ui, sin android.* (P5 para lo del sistema)
    │   ├── ubicacion/              # AjustarACalles (JSON común, postear expect)
    │   └── plataforma/             # expect de P1 a P6
    ├── commonTest/kotlin/…         # ← app/src/test (kotlin.test) + RespaldoTest, VencimientoTest
    ├── androidMain/kotlin/…        # actual de Android: LectorUbicacion, ServicioRecorrido,
    │                               #   Geofences, GeofenceReceiver, BootReceiver, Permisos,
    │                               #   migraciones 1→7, launchers de Activity Result
    ├── androidHostTest/kotlin/…    # InmutabilidadTest (reflexión)
    └── iosMain/kotlin/…            # actual de iPhone: Core Location, regiones, notificaciones,
                                    #   cámara, compartir, documentos, NSUserDefaults, NSURLSession,
                                    #   MainViewController()

app/                                # La app Android: queda chica
├── build.gradle.kts                # depende de :shared; applicationId sin cambios
└── src/main/
    ├── AndroidManifest.xml         # sin cambios de permisos; receivers y servicio apuntan a :shared
    └── java/…/App.kt, ui/MainActivity.kt

iosApp/                             # NUEVO — el punto de entrada del iPhone
├── project.yml                     # XcodeGen (C2): bundle id fijo, Info.plist, linker flags
└── iosApp/
    ├── iOSApp.swift                # @main + AppDelegate que llama a Plataforma.iniciar()
    └── ContentView.swift           # muestra MainViewController()
```

**Structure Decision**: un módulo `:shared` de Kotlin Multiplatform con todo lo que se comparte y
las dos costuras de plataforma. `:app` queda como envoltorio Android, con el mismo `applicationId`,
el manifiesto, `App` y `MainActivity`. `iosApp/` es solo el punto de entrada de Swift y la
descripción del proyecto para XcodeGen: el `.xcodeproj` se genera en la nube y no se guarda.
Los paquetes de Kotlin no cambian, así que los imports y las referencias del manifiesto siguen
valiendo.

## Complexity Tracking

| Qué se agrega | Por qué hace falta | Lo más simple que se descartó, y por qué |
|---|---|---|
| **Kotlin Multiplatform** (módulo `:shared`) | Es la única forma de que el iPhone use el mismo código y dé los mismos resultados (FR-022) | Una app aparte en SwiftUI: duplica 6.000 líneas, y sin Mac no hay ni previsualización |
| **Compose Multiplatform** | La UI de hoy es Compose. Con esto corre en el iPhone sin reescribirse | Pantallas nativas del iPhone: rediseño y doble mantenimiento, fuera de alcance en la spec |
| **maplibre-compose** | Un solo mapa en los dos, sin `cinterop`, que solo compila en una Mac | Dos mapas: 1.160 líneas duplicadas, y en el iPhone igual haría falta `cinterop` |
| **sqlite-bundled** | Room en el iPhone lo necesita, y el respaldo lo usa en los dos | Ninguna: iOS no trae un driver de SQLite para Room |
| **kotlinx-datetime** | `java.text` no existe en el iPhone. Con esto las fechas salen iguales en los dos (FR-022) | Una función `expect` por formato: dos implementaciones que pueden dar textos distintos |
| **kotlinx-serialization-json** | `org.json` no existe en el iPhone, y el ajuste a calles arma y lee JSON | `NSJSONSerialization` en iOS y `org.json` en Android: dos parsers para la misma respuesta |
| **kotlinx-io-core** | Las fotos y el respaldo mueven, miden y copian archivos en código común, donde no hay `java.io.File` | Un `expect` por operación: diez funciones con dos implementaciones cada una |
| **XcodeGen** (solo en la nube) | Un `.pbxproj` no se escribe a mano desde Windows | Guardar un `.pbxproj` generado en una Mac: no hay Mac |
| **Workflow de GitHub Actions** | No hay Mac. Es el único lugar donde se compila el iPhone | — |
