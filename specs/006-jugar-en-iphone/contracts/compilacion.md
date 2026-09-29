# Contrato C — Compilar en la nube e instalar desde Windows

Qué entra, qué sale y qué no puede pasar en el camino del código al iPhone. Las decisiones están en
[research.md](../research.md), D4 y D22.

## C1 — El workflow `ios.yml`

| | |
|---|---|
| **Disparadores** | `workflow_dispatch` (a mano: `gh workflow run ios.yml`) y `push` a `main` |
| **Runner** | `macos-latest` |
| **Secretos** | **Ninguno.** Si alguna vez hace falta uno de Apple, se viola el FR-003 |
| **Artefacto** | `buscar-patentes-ipa`, con un solo archivo: `buscar-patentes.ipa` |
| **Retención** | 14 días: alcanza para dos reinstalaciones semanales sin volver a compilar |

**Pasos, en orden. Si uno falla, no se publica artefacto:**

1. JDK 17 y la caché de Gradle.
2. `./gradlew :shared:iosSimulatorArm64Test`: las pruebas comunes en el motor del iPhone (D17).
3. `./gradlew :shared:linkReleaseFrameworkIosArm64`: el framework estático `Shared`.
4. `brew install xcodegen` y `xcodegen` en `iosApp/`.
5. `xcodebuild` en `Release` para `generic/platform=iOS`, con `CODE_SIGNING_ALLOWED=NO`.
6. `Payload/BuscarPatentes.app` → `buscar-patentes.ipa`.
7. Subir el artefacto.

## C2 — `iosApp/project.yml` (XcodeGen)

Lo que tiene que decir, y no puede cambiar entre compilaciones:

| Campo | Valor | Por qué |
|---|---|---|
| `PRODUCT_BUNDLE_IDENTIFIER` | `ar.lauta.buscarpatentes` | FR-004: el mismo en todas, para instalar encima |
| `deploymentTarget` | `15.5` | Lo pide maplibre-compose |
| `TARGETED_DEVICE_FAMILY` | `1` (iPhone) | Sin iPad (fuera de alcance) |
| `OTHER_LDFLAGS` | los que pide maplibre-compose (`-lc++ -lz` y frameworks) | Sin ellos no linkea |
| Framework | `Shared.framework` estático, desde `shared/build/bin/iosArm64/releaseFramework` | |

**`Info.plist`**:

| Clave | Valor |
|---|---|
| `UIBackgroundModes` | `location` (D12) |
| `NSLocationWhenInUseUsageDescription` | "Para guardar dónde viste cada patente." |
| `NSLocationAlwaysAndWhenInUseUsageDescription` | "Para avisarte cuando pasás cerca de la patente que toca, y para grabar la salida con la pantalla apagada." |
| `NSCameraUsageDescription` | "Para sacarle foto a la patente." |
| `UIFileSharingEnabled` | `true`, así los respaldos se ven desde iTunes (D8) |
| `LSSupportsOpeningDocumentsInPlace` | `true`, así se ven desde la app Archivos |
| `CFBundleShortVersionString` / `CFBundleVersion` | el `versionName` / `versionCode` del Android, para saber qué hay instalado |

## C3 — Bajar e instalar

Desde la PC, en la raíz del repositorio:

```powershell
gh run download --name buscar-patentes-ipa --dir $env:USERPROFILE\Downloads\buscar-patentes-ios
```

En Sideloadly: el iPhone enchufado, el `.ipa`, **siempre el mismo Apple ID**, y sin tocar las
opciones avanzadas de identificador. Instalar encima de la app que ya está.

## C4 — Qué no puede pasar

| Nunca | Lo cuida |
|---|---|
| Una credencial de Apple en GitHub | C1: ningún secreto |
| Un identificador distinto entre compilaciones | C2: fijo en `project.yml` |
| Datos del jugador en el repositorio | `.gitignore` (`respaldos/`, `*.respaldo`) y el chequeo de D22 antes de cada push |
| Coordenadas de la zona, capturas del mapa o un email personal en el historial público | D22: historia nueva, email `noreply`, chequeo antes de subir (SC-011) |
| El Android roto por un cambio del iPhone | Cada paso de D20 cierra con las pruebas en la JVM y el APK en el teléfono |
