---

description: "Task list for 006-jugar-en-iphone"
---

# Tasks: Jugar en el iPhone

**Input**: Design documents from `/specs/006-jugar-en-iphone/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/respaldo.md](./contracts/respaldo.md),
[contracts/plataforma.md](./contracts/plataforma.md),
[contracts/compilacion.md](./contracts/compilacion.md), [quickstart.md](./quickstart.md)

**Tests**: la spec no pide TDD. Las pruebas de esta lista son las que obliga la constitución, más
las que se rompen por el cambio:
- **Nuevas**: `RespaldoTest`, `VencimientoTest` y `AlmacenFotosTest`.
- **Cambian**: `AvisosTest` (límite como parámetro), `ColeccionTest` (maplibre-compose) y las
  coordenadas de cuatro pruebas (D22).
- **Se mudan**: todas pasan a `commonTest`, con `kotlin.test` (D17).

**Organization**: el orden sale de D20. Las fases 1 y 2 son grandes a propósito:
- **Fase 1**: publicar el repositorio y correr la prueba piloto en el iPhone.
- **Fase 2**: pasar el Android a multiplataforma **sin que cambie nada** y compartir el mapa.

Recién ahí empiezan las stories, que son el pegamento del iPhone más lo nuevo (respaldo y
vencimiento). Cada fase cierra con el Android validado en el teléfono.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede hacer en paralelo (archivo distinto, sin depender de nada incompleto)
- **[Story]**: a qué user story pertenece (US1 a US6)
- Cada tarea nombra el archivo exacto y cita la decisión (D*) o el contrato (R*, P*, C*) que la
  gobierna

## Path Conventions

Tres raíces, todas con el paquete `ar/lauta/buscarpatentes`:

| Abreviatura | Ruta |
|---|---|
| `common/…` | `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/…` |
| `commonTest/…` | `shared/src/commonTest/kotlin/ar/lauta/buscarpatentes/…` |
| `android/…` | `shared/src/androidMain/kotlin/ar/lauta/buscarpatentes/…` |
| `hostTest/…` | `shared/src/androidHostTest/kotlin/ar/lauta/buscarpatentes/…` |
| `ios/…` | `shared/src/iosMain/kotlin/ar/lauta/buscarpatentes/…` |
| `app/…` | `app/src/main/java/ar/lauta/buscarpatentes/…` (lo que queda de la app Android) |

- **La app de iPhone**: `iosApp/`.
- **El workflow**: `.github/workflows/ios.yml`.
- **Mover** es siempre `git mv`, para no perder el rastro del archivo.

**Verificación del Android** (la llamamos **"verificar Android"** abajo):
1. `.\gradlew.bat test assembleDebug` en verde.
2. El APK va a `/sdcard/Download/buscar-patentes.apk`, pisando el anterior y borrando otros
   `buscar-patentes*.apk`.
3. El usuario lo instala **encima**.
4. Se compara el hash del APK instalado con el compilado.
5. Al abrir están las mismas patentes, salidas y fotos que antes.

**Compilación del iPhone** (**"compilar iPhone"** abajo):
1. `gh workflow run ios.yml --ref <rama>` y después `gh run watch`.
2. `gh run download --name buscar-patentes-ipa`.
3. El usuario instala con Sideloadly (quickstart, "Compilar e instalar").

---

## Phase 1: Setup — línea de base, repositorio público y prueba piloto

**Purpose**:
- asegurar los datos del Android antes de tocar nada;
- publicar el repositorio limpio (D22);
- saber en un día lo que depende de Apple (D21).

- [X] T001 Línea de base: `.\gradlew.bat testDebugUnitTest` en verde en la rama `006-jugar-en-iphone`, y respaldo del teléfono Android con `./scripts/respaldo.ps1 respaldar`. Verificar que el `-wal` pesa cientos de KB. Es la red de seguridad de toda la feature: el paso a multiplataforma toca la base
- [X] T002 [P] **Hecha**: además de las cuatro previstas, `ui/IrTest.kt` tenía coordenadas de la zona y `domain/PolilineaTest.kt` una polilínea real de Valhalla con los primeros puntos de una salida. Se reemplazó por una sintética de cinco puntos alrededor del Obelisco, y `GeoTest` dejó de nombrar el barrio. Corrimiento usado: −0,0232° de latitud y +0,1572° de longitud. Siguen pasando 119 pruebas. Pasar las coordenadas de `app/src/test/java/ar/lauta/buscarpatentes/domain/AcomodoTest.kt`, `domain/GeoTest.kt`, `domain/PrioridadTest.kt` y `mapa/ColeccionTest.kt` a una zona neutra, el Obelisco (`-34.6037, -58.3816`), corriendo cada punto lo mismo en grados. Recalcular los esperados que dependan de la latitud: los metros por grado de longitud, el comentario de `AcomodoTest` sobre metros por dp y las distancias y rumbos de `GeoTest`. `testDebugUnitTest` en verde (D22, FR-005a)
- [X] T003 [P] **Hecha**: además de la T002 de la 005, se limpiaron las coordenadas de una captura real en `specs/001-captura-patentes/tasks.md` (línea 90) y los nombres de calles en `specs/003-rumbo-y-aspecto/tasks.md` (línea 228). Borrar `specs/005-buscar-la-que-toca/antes-grupos.png`. Reescribir la T002 de `specs/005-buscar-la-que-toca/tasks.md` sin nombres de calles ni la ruta de la imagen: "la zona con más patentes, a zoom de calle, con 4 grupos (2, 2, 2 y 3) y 2 pines sueltos…". Revisar `specs/` y `README.md` con `git grep -n` por nombres de calles o barrios de la zona, y neutralizarlos (D22, SC-011)
- [X] T004 [P] En `.gitignore`, sumar `*.respaldo`, `iosApp/*.xcodeproj/`, `iosApp/build/` y `.kotlin/` (C4)
- [X] T005 Commitear en `006-jugar-en-iphone` la spec, el plan, la enmienda de la constitución 1.2.0 y T002 a T004, en commits separados por tema (docs, test, chore)
- [X] T006 **Hecha el 2026-09-28**, después de T011 y con el OK del usuario: `github.com/bermejolautaro/buscar-patentes`, público. El chequeo de §8 dio limpio. Crear la historia pública (D22):
  1. `git config user.email <id>+bermejolautaro@users.noreply.github.com`, con el id de `gh api user --jq .id`.
  2. `git branch -m main historial` y `git branch -m 006-jugar-en-iphone historial-006`.
  3. `git checkout --orphan main` y un commit `chore: primer commit público`.
  4. `git checkout -b 006-jugar-en-iphone`.
  5. Correr los chequeos de quickstart §8 sobre `main` y mostrar el resultado.
  6. **Pedirle confirmación al usuario antes de publicar.** Recién entonces `gh repo create buscar-patentes --public --source . --remote origin` y `git push -u origin main 006-jugar-en-iphone`.
- [X] T007 **Hecha**: Kotlin 2.4.20 con KSP 2.3.12 (no hay KSP para 2.4 todavía; en Android, Room compila igual). Subir Kotlin a 2.4.20 y KSP a la versión compatible en `gradle/libs.versions.toml`, sin tocar nada más. **Verificar Android** (D2)
- [X] T008 **Hecha**: maplibre-compose obliga a `compileSdk = 37` (`targetSdk` queda en 36). Con Compose Multiplatform, el Android pasa a Compose UI 1.12.1 y Material3 1.5.0-alpha22: a mirar en T031. Crear el módulo `shared/`:
  - **`settings.gradle.kts`**: `include(":shared")`.
  - **`gradle/libs.versions.toml`**: Compose Multiplatform 1.12.1 (plugin `org.jetbrains.compose`), `com.android.kotlin.multiplatform.library`, maplibre-compose 0.18.0 (y el runtime OpenGL de Android), `androidx.sqlite:sqlite-bundled`, `kotlinx-datetime`, `kotlinx-serialization-json` y `kotlinx-io-core`. **No** se suma el lifecycle de JetBrains: `androidx.lifecycle` solo lo usa `Mapa.kt`, y sale de `:app` con el mapa viejo (T036).
  - **`shared/build.gradle.kts`**:
    - plugins de Kotlin Multiplatform, la biblioteca Android multiplataforma, Compose Multiplatform, `kotlin.plugin.compose` y KSP;
    - `androidLibrary { namespace = "ar.lauta.buscarpatentes.shared"; compileSdk = 36; minSdk = 26 }`, con `withHostTest {}`;
    - `iosArm64()` e `iosSimulatorArm64()`, con framework `Shared` estático;
    - dependencias de `commonMain` y `commonTest` (`kotlin("test")`).
  - **`:app`**: `implementation(project(":shared"))`.
  - `.\gradlew.bat assembleDebug` en verde (D1, D3)
- [X] T009 **Hecha**, con dos pruebas más que las previstas: una base de Room (reinstalar tiene que conservarla, y además prueba KSP en iOS con Kotlin 2.4) y un mapa de maplibre-compose. Prueba piloto, en `ios/piloto/Piloto.kt`:
  - **Arranque**: `fun iniciarPiloto()`, que llama el `AppDelegate`, crea el único `CLLocationManager` y su delegado.
  - **Pantalla**: `fun MainViewController(): UIViewController` con `ComposeUIViewController { PantallaPiloto() }`.
  - **Botones de `PantallaPiloto`**:
    - "Escribir marca": escribe la hora en `Library/Application Support/marca.txt` y muestra la marca que había.
    - "Vigilar acá": pide "siempre" y registra un `CLCircularRegion` de 150 m en la posición actual.
    - "Grabar": `startUpdatingLocation` en segundo plano, y cuenta puntos.
  - **Datos que muestra**: la versión de iOS (`UIDevice.currentDevice.systemVersion`), la `ExpirationDate` leída de `embedded.mobileprovision` y el estado de cada permiso.
  - **Al entrar a la región**: una notificación local.
  - Es temporal: se borra en T041 (D21)
- [X] T010 [P] Crear `iosApp/project.yml` (XcodeGen), `iosApp/iosApp/iOSApp.swift` (`@main` con `@UIApplicationDelegateAdaptor`, cuyo `didFinishLaunching` llama a `PilotoKt.iniciarPiloto()`) e `iosApp/iosApp/ContentView.swift` (un `UIViewControllerRepresentable` que muestra `MainViewController()`). Bundle id, deployment target, flags de linker, framework e `Info.plist`, exactamente como en C2
- [X] T011 [P] **Hecha**: el workflow entra en la `main` pública desde el primer commit, porque T006 se corrió después de T011. Crear `.github/workflows/ios.yml` según C1: `workflow_dispatch` y `push` a `main`, `macos-latest`, sin secretos, los pasos 1 a 7 y el artefacto `buscar-patentes-ipa` con retención de 14 días. Mergear a `main` y hacer push: `workflow_dispatch` solo aparece si el workflow está en la rama por defecto
- [ ] T012 **Compilar iPhone** hasta que salga el `.ipa`, arreglando lo que falle en el workflow o en `project.yml`. El usuario instala y corre el bloque 0 del quickstart. Anotar en `research.md`, en la tabla de D21, el resultado de cada fila, la versión de iOS y la fecha:
  - **Si falla "reinstalar conserva la marca"**: se para la feature y se habla con el usuario.
  - **Si fallan los avisos o la grabación**: marcar la US5 o la US6 como "solo Android" y seguir.

**Checkpoint**: el repositorio es público y está limpio, el iPhone instala algo compilado en la nube,
y se sabe qué funciona con el Apple ID gratis.

---

## Phase 2: Foundational — el Android multiplataforma, igual que hoy

**Purpose**: todo el código compartido existe y el Android lo usa sin ningún cambio visible. Ninguna
story del iPhone puede empezar sin esto.

**⚠️ CRITICAL**: cada tarea deja `.\gradlew.bat test assembleDebug` en verde. Hay dos puntos donde
además se **verifica el Android** en el teléfono: T031 y T036.

### Dominio y pruebas

- [X] T013 **Hecha**: además de `TimeUnit`, había `String.format` en `Geo.distanciaLegible` y `Math.toRadians`/`toDegrees` en `Geo` y `Acomodo`; se reemplazaron por equivalentes comunes que dan lo mismo. Mover `app/…/domain/*.kt` a `common/domain/`. En `Antiguedad.kt`, reemplazar `java.util.concurrent.TimeUnit` por constantes en milisegundos o `kotlin.time`. Sin otros cambios de lógica (D1)
- [X] T014 **Hecha**: 94 pruebas en común y 25 que siguen en la app (IrTest, ColeccionTest, InmutabilidadTest), 119 en total. Pasan también en el simulador de iPhone en la nube. Mover a `commonTest/` y pasar de JUnit a `kotlin.test` las pruebas de `app/src/test/java/…`: `PatenteTest`, `CoberturaTest`, `AvisosTest`, `domain/PolilineaTest`, `domain/AntiguedadTest`, `domain/GeoTest`, `domain/AcomodoTest`, `domain/PrioridadTest` y `domain/ProbabilidadTest`. **Dar vuelta el mensaje**: en JUnit va primero y en `kotlin.test` va último. `@Test` pasa a `kotlin.test.Test`. `ProbabilidadTest` va a `commonTest`, como las demás. `.\gradlew.bat :shared:testAndroidHostTest` en verde, con **el mismo número de pruebas** que antes (D17)
- [X] T015 [P] En `common/domain/Avisos.kt`, cambiar `aRegistrar(registros, numeroActual)` por `aRegistrar(registros, numeroActual, limite: Int)`, conservar `LIMITE_GEOFENCES = 100` como el valor del Android y actualizar el KDoc. En `commonTest/AvisosTest.kt`, sumar un caso con 25 candidatos y límite 20: quedan los 20 de id más bajo.

  Además, pasar a `Avisos` la regla de deduplicación que hoy vive en `GeofenceReceiver.kt:60`: `fun yaAvisado(ahora: Long, ultimoAviso: Long): Boolean = ahora - ultimoAviso < VENTANA_SALIDA_MS`, con la constante movida desde el receiver. `GeofenceReceiver` la usa, y `AvisosTest` prueba los bordes: justo antes y justo en el límite de la ventana, y `ultimoAviso = 0`. Así el iPhone no reescribe la regla (T063) (D11, FR-025, data-model)

### Base, fotos y contenedor

- [X] T016 **Hecha**: identity hash `b85b6209…`, igual al de la base del teléfono. KSP en iOS con Kotlin 2.4 funciona (la prueba piloto lo confirmó en la nube). Mover `app/…/data/Entidades.kt` y `data/Daos.kt` a `common/data/`. Mover `data/BaseDeDatos.kt` a `common/data/` **sin las migraciones**: `@Database` igual (v7, `exportSchema = false`), más `@ConstructedBy(BaseDeDatosConstructor::class)` y `expect object BaseDeDatosConstructor : RoomDatabaseConstructor<BaseDeDatos>`. Las migraciones 1→7 y el builder van a `android/data/ConstruirBase.kt`, como `fun construirBase(context): BaseDeDatos` con el **mismo nombre de archivo** y el mismo `addMigrations`. KSP: `kspAndroid`, `kspIosArm64` y `kspIosSimulatorArm64` con `room-compiler`. `Convertidores` va con las entidades (D6, FR-030)
- [X] T017 [P] Crear `ios/data/ConstruirBase.kt`: `Room.databaseBuilder<BaseDeDatos>(name = "<Application Support>/buscar-patentes.db")`, `.setDriver(BundledSQLiteDriver())` y `.setQueryCoroutineContext(Dispatchers.IO)`. Crea la carpeta si no existe (D6, P1)
- [X] T018 **Hecha**, con `Carpetas` (P1) adelantado de T019. Mover `app/…/data/AlmacenFotos.kt` a `common/data/`, con `kotlinx-io` en lugar de `java.io.File`. Recibe la carpeta de fotos como `Path`, y `archivo(ruta)` resuelve **el nombre del archivo** de `ruta` dentro de la carpeta (D7). `archivoNuevo`, `existe`, `borrar`, `espacioOcupado` y `superoElTecho` siguen haciendo lo mismo. Crear `commonTest/data/AlmacenFotosTest.kt`: una ruta del Android (`/data/user/0/…/files/fotos/123.jpg`) resuelve a `<carpeta>/123.jpg`, y `existe` da `true` si ese archivo está. Corre en la JVM sobre una carpeta temporal
- [X] T019 **Hecha, recortada a lo que el código común usa hoy**: `Ubicacion`, `Grabacion`, `Vigilancia`, `abrirPunto`, `abrirRecorrido`, `abrirAjustesDelSistema`, `compartir`, `hayConexion`, `postear`, `versionInstalada` y `ahora()`. `Preferencias`, `perfilDeAprovisionamiento` y `programarAvisoDeVencimiento` los declara la story que los usa (US2, US5), y `ubicacionExacta` la T040. En el iPhone, `Vigilancia.reconciliar` no hace nada y `hayConexion` da `false` hasta US5 y US6: las pantallas los llaman solas, y la app tiene que abrir igual. El resto tira `NotImplementedError` con su story. Crear `common/plataforma/Plataforma.kt` con los `expect` de P1 (`Carpetas`), P2 (`leerUbicacion`, `ubicacionExacta`), P3 (`Grabacion`), P4 (`Vigilancia`), P6 (`abrirUrl`, `abrirAjustesDelSistema`, `postear`, `Preferencias`, `perfilDeAprovisionamiento`, `programarAvisoDeVencimiento`) y `data class Lectura`. Crear `android/plataforma/Plataforma.android.kt` con los `actual` que **delegan en el código de hoy**: `LectorUbicacion`, `ServicioRecorrido`, `Geofences`, el `Intent` de `Ir` y `HttpURLConnection`. `ubicacionExacta` y `perfilDeAprovisionamiento` dan `true` y `null`, y `programarAvisoDeVencimiento` no hace nada. Crear `ios/plataforma/Plataforma.ios.kt` con `actual` que tiran `NotImplementedError("US<n>")`, cada uno marcado con la story que lo implementa (D9, contracts/plataforma.md)
- [X] T020 **Hecha**, sin `LocalContenedor`: `contenedor` es una propiedad global común que fija `iniciarContenedor`, porque el servicio del recorrido y los receivers la usan sin ninguna pantalla abierta. Mover `Contenedor` de `app/…/App.kt` a `common/Contenedor.kt`: recibe `BaseDeDatos` y `AlmacenFotos` ya construidos y expone los mismos DAO. Sumar `val LocalContenedor = staticCompositionLocalOf<Contenedor> { error("sin contenedor") }`. `App.onCreate` arma el contenedor con `construirBase(this)` y conserva `cerrarLosAbiertos` y `reconciliar` como hoy. El `Context.contenedor` de hoy se queda para el código de `androidMain`

### Ubicación

- [X] T021 **Hecha**. `Permisos.kt` del Android se fue: lo reemplaza `tienePermiso` común. Mover `app/…/ubicacion/LectorUbicacion.kt`, `ServicioRecorrido.kt`, `Geofences.kt`, `GeofenceReceiver.kt`, `BootReceiver.kt` y `Permisos.kt` a `android/ubicacion/`, sin cambios salvo los imports. En `Geofences.kt`, `Avisos.aRegistrar(…, Avisos.LIMITE_GEOFENCES)` (T015). `app/src/main/AndroidManifest.xml` no cambia: las clases conservan paquete y nombre
- [X] T022 **Hecha**. El pedido sale igual salvo cómo se escriben los números: `radius` viaja como `Float` (`12.34`) y no como el `double` de `org.json` (`12.34000015258789`); el valor es el mismo. `AjustarACallesTest` lo cubre. Mover `app/…/ubicacion/AjustarACalles.kt` a `common/ubicacion/`: `org.json` pasa a `kotlinx.serialization.json` (`buildJsonObject`, `Json.parseToJsonElement`) y `HttpURLConnection` a `postear(url, cuerpo)` (P6). El pedido JSON tiene que salir **igual byte a byte** que hoy, salvo el orden de las claves si el servicio no lo exige. `PolilineaTest` sigue en verde (D16)

### UI

- [X] T023 **Hecha**, moviéndolos: ninguno lo usaba el manifiesto ni una notificación. Compose Multiplatform no lee `@android:color/...`, así que pasaron a hexadecimal. `Res` vive en `ar.lauta.buscarpatentes.recursos`. [P] Copiar los `ic_*.xml` de `app/src/main/res/drawable/` a `shared/src/commonMain/composeResources/drawable/`, y en cada pantalla reemplazar `painterResource(R.drawable.ic_x)` por `painterResource(Res.drawable.ic_x)`. Se quedan en `app/` solo los que use el manifiesto o una notificación del Android
- [X] T024 **Hecha** como `common/ui/Formatos.kt`, con las fechas, los decimales con coma y `tresCifras`. Los esperados de `FormatosTest` salen de `SimpleDateFormat` con `Locale("es", "AR")` en la JVM 21. [P] Crear `common/ui/Fechas.kt` sobre `kotlinx-datetime`, con los formatos que hoy salen de `SimpleDateFormat` en `FichaDeRegistro.kt`, `PantallaAjustes.kt` y `PantallaRecorridos.kt`, y los meses en castellano en una lista propia. Crear `commonTest/ui/FechasTest.kt`: para dos instantes fijos, en la zona `America/Argentina/Buenos_Aires`, el texto de cada formato es **igual al que da hoy** `SimpleDateFormat` con `Locale("es", "AR")` (anotar el esperado corriendo el código viejo antes de borrarlo) (D16, FR-022)
- [X] T025 **Hecha**, recortada: `rememberSacarFoto` es composable; compartir es una función común (`compartir`), porque en ninguno de los dos sistemas hace falta un launcher. Pedir permisos ya estaba, en `plataforma/Permisos.kt`. Los dos de respaldo los declaran US2 y US3. Crear `common/plataforma/Pantallas.kt` con los composables `expect` de P5: `rememberSacarFoto`, `rememberPedirPermisos`, `rememberCompartir`, `rememberElegirRespaldo` y `rememberGuardarRespaldo`. Los `actual` de `android/plataforma/Pantallas.android.kt` **se mudan** de los launchers de `PantallaPrincipal.kt`, `FichaDeRegistro.kt` y `PantallaAjustes.kt` y del `Intent` de `Compartir.kt`. Los de `ios/plataforma/Pantallas.ios.kt` tiran `NotImplementedError` con su story. Los dos de respaldo quedan declarados y se implementan en US2 y US3 (P5)
- [X] T026 **Hecha**. Mover `app/…/ui/*.kt` a `common/ui/`, **salvo `MainActivity.kt`**:
  - `LocalContext.current.contenedor` → `LocalContenedor.current` (T020);
  - `SimpleDateFormat`, `Date` y `Locale` → `Fechas` (T024);
  - los launchers → los `expect` de T025;
  - `Intent`, `Uri` y `Build` → `abrirUrl` y `abrirAjustesDelSistema` (P6);
  - `androidx.activity.compose.BackHandler` → el `BackHandler` de Compose Multiplatform.
  Sin cambios de comportamiento ni de aspecto. `IrTest` va a `commonTest/ui/`, y su parte de `Locale.setDefault` se reemplaza por la garantía de que la URL no depende del locale (D9, D16)
- [X] T027 **Hecha**, sin `registroAAbrir`: el aviso del Android abre el lugar en la app de mapas, no la ficha. Crear `common/ui/AppBuscarPatentes.kt` con lo que hoy hace el `setContent` de `MainActivity`: `TemaBuscarPatentes`, `enum Destino`, el `BackHandler` y el `when`. Sumar un parámetro `registroAAbrir: Long?` para el toque en un aviso. `MainActivity` queda en `app/…/ui/MainActivity.kt` con `enableEdgeToEdge()` y `setContent { CompositionLocalProvider(LocalContenedor provides contenedor) { AppBuscarPatentes(…) } }`. Si hoy el `PendingIntent` del aviso lleva un extra de registro, se pasa por `registroAAbrir` (D9)
- [X] T028 **Hecha**: las tres tienen `BarraSuperior` con volver. [P] Verificar que las pantallas de salidas y de ajustes, y el detalle de una salida en `common/ui/PantallaRecorridos.kt` (hoy `BarraSuperior(fechaLarga(…), onVolver = { abierta = null })`), muestran `BarraSuperior` con volver. Hoy lo cumplen. Si apareciera una sin flecha, se muestra **solo en el iPhone**, con un `expect val` (D18, FR-023, FR-031)
- [X] T029 **Hecha**. `:app` queda con `activity-compose`, el runtime de Compose Multiplatform y Room, y sin KSP. Pasar `:app` de la BOM de Compose de Android a las dependencias de Compose Multiplatform 1.12.1, para no tener dos versiones de Compose en el mismo APK. Borrar de `gradle/libs.versions.toml` lo que quede sin uso
- [X] T030 **Hecha**, en `shared/src/androidHostTest/`. Mover `app/src/test/java/ar/lauta/buscarpatentes/InmutabilidadTest.kt` a `hostTest/InmutabilidadTest.kt`, apuntando a los DAO en su nuevo lugar. Tiene que seguir fallando si alguien agrega una forma de editar la evidencia (Principio II)
- [ ] T031 **Verificar Android**, más un recorrido corto por los quickstart de la 004 y la 005: carga rápida, mapa, barra con filtro, ficha, votar, compartir, ir, salida y ajustes. **El Android tiene que ser indistinguible del de antes de T007.** Commit

### Mapa compartido (D5)

- [X] T032 **Hecha, antes que T019–T031**: se reordenó para validar el mapa nuevo en el Android con la UI de siempre, en vez de armar un `expect` provisorio del mapa viejo. Mover a `common/mapa/Colecciones.kt` las funciones puras de GeoJSON de `app/…/mapa/Mapa.kt`, reescritas sobre los tipos GeoJSON de maplibre-compose: `coleccion`, `featureDe`, `coleccionAcomodada`, `coleccionesPatentes`, las de trazos y sus constantes `PROP_*`. `ColeccionTest` va a `commonTest/mapa/`, con las mismas afirmaciones sobre los tipos nuevos (D5, D17)
- [X] T033 Crear `common/mapa/Pines.kt`, que dibuja los pines con Compose (`Painter` o `ImageBitmap`) con el mismo tamaño, el borde por confianza, el relleno blanco o `ColoresDeMapa.TOCA` y el texto que hoy sale de `bitmapDePin` con `android.graphics.Canvas`. Los nombres de imagen (`PIN_*`, `PIN_TOCA_*`) no cambian
- [X] T034 **Hecha**: el punto azul es `LocationIndicatorLayer` con `rememberLocationState`, y el seguimiento `LocationTrackingEffect`. El caché va a `Carpetas.base/maplibre-cache.db` (el de la caché del sistema lo podía borrar Android) y el viejo `mbgl-offline.db` se borra: las zonas vistas sin conexión se vuelven a guardar al pasar. Sin brújula ni escala. Crear `common/mapa/MapaDeFondo.kt` con maplibre-compose, con la misma API de composable que tiene hoy `MapaDeFondo`. Tiene que reproducir `Mapa.kt`:
  - **Estilo y fuentes**: el estilo `ConfigMapa.ESTILO_URL`, las fuentes `FUENTE` y `FUENTE_TOCA` y la de trazos, con las mismas capas en el mismo orden (C2 de la 004).
  - **Acomodo**: recalcula `coleccionesPatentes` solo cuando cambia el zoom entero, al quedar quieta la cámara (FR-023c de la 005).
  - **Toques**: el clic consulta `CAPA` y `CAPA_TOCA` y abre la ficha.
  - **Posición y cámara**: `LocationIndicatorLayer` para la posición, encuadre con límites y `ZOOM_MAXIMO`.
  - **Caché de teselas**: se fija con el `OfflineManager` de la biblioteca, igual que hoy (SC-013 de la 001).
  - **Motor en el Android**: el runtime OpenGL.
  Si algo de la API de hoy no tiene equivalente, anotarlo en el commit y en `research.md` (D5)
- [X] T035 **Hecha**, sin `MapaViejo`: el `Mapa.kt` viejo salió del todo (queda en el historial para la línea de corte), y con él `android-sdk` y `androidx.lifecycle`. Apuntar la UI común a `common/mapa/MapaDeFondo.kt` y mover `app/…/mapa/Mapa.kt` a `android/mapa/MapaViejo.kt`, sin borrarlo todavía. Sacar `org.maplibre.gl:android-sdk` de `:app` solo si nada lo usa
- [ ] T036 **Verificar Android** con los quickstart **enteros** de la [004](../004-planear-recorridos/quickstart.md) y la [005](../005-buscar-la-que-toca/quickstart.md) en el teléfono: modos del mapa, pines, la que toca en verde, grupos y pines que se hacen lugar, trazos por antigüedad, punto azul con rumbo y el mapa sin conexión en una zona ya vista.
  - **Si pasa**: borrar `android/mapa/MapaViejo.kt` y las dependencias `android-sdk`, `lifecycle-runtime-ktx` y `lifecycle-runtime-compose` de `:app`. Solo las usaba el mapa viejo.
  - **Si no pasa y no se arregla**: aplicar la línea de corte de D5 (`expect fun MapaDeFondo` con `MapaViejo` en el Android), anotarlo en `research.md` y seguir.
  Commit

**Checkpoint**: el Android corre sobre `:shared`, igual que antes, y el iPhone compila el mismo código
con `actual` pendientes. De acá en adelante, cada story del iPhone es pegamento más lo nuevo.

---

## Phase 3: User Story 1 - Anotar y encontrar patentes en el iPhone (Priority: P1) 🎯 MVP

**Goal**: en el iPhone se anota con ubicación, hora y precisión, también sin señal, y se ve el mapa,
la barra, la lista y la ficha como en el Android.

**Independent Test**: compilar iPhone e instalar. Anotar tres patentes, una en modo avión. Verlas en
el mapa y en la lista, abrir una ficha y votar (quickstart §2).

- [X] T037 **Hecha** con T018 (`Carpetas.ios.kt`). `Preferencias` queda para la story que la use: la vigilancia del iPhone lee las regiones del sistema y no necesita anotarlas. [P] [US1] En `ios/plataforma/Plataforma.ios.kt`, implementar `Carpetas`:
  - `base` y `fotos` en `NSApplicationSupportDirectory`, `respaldos` en `NSDocumentDirectory/respaldos` y `temporal` en `NSTemporaryDirectory()`;
  - crear las carpetas al primer uso;
  - `Preferencias` sobre `NSUserDefaults.standardUserDefaults`.
  (P1, P6)
- [X] T038 **Hecha** en `ios/plataforma/Ubicacion.ios.kt`, con un cambio: en vez de `requestLocation()`, `startUpdatingLocation()` hasta que llega una posición de ahora (edad de 15 s o menos), y corta. `requestLocation()` puede entregar una guardada. Tiene su propio `CLLocationManager`, así apagarlo no toca la salida ni los avisos. [P] [US1] Crear `ios/ubicacion/Ubicacion.ios.kt`:
  - **El manager**: el único `CLLocationManager` de la app, con su delegado, creado en `Plataforma.iniciar()`.
  - **`leerUbicacion()`**: `requestLocation()` con `kCLLocationAccuracyBest`, el mismo tiempo de espera que `LectorUbicacion`, y `horizontalAccuracy` a `precisionMetros`. Devuelve `null` si vence la espera.
  - **`ubicacionExacta()`**: `accuracyAuthorization == CLAccuracyAuthorizationFullAccuracy`.
  (D10, P2)
- [X] T039 **Hecha**, también para la cámara. "Siempre" se pide sin esperar la respuesta: iOS puede postergar el cartel. `abrirAjustesDelSistema` quedó en `Plataforma.ios.kt`. [P] [US1] En `ios/plataforma/Pantallas.ios.kt`, implementar `rememberPedirPermisos` para ubicación "mientras se usa" (`requestWhenInUseAuthorization`) y notificaciones (`UNUserNotificationCenter.requestAuthorization`), y `abrirAjustesDelSistema()` con `UIApplicationOpenSettingsURLString` (D13, P5, P6)
- [X] T040 **Hecha**. [US1] En `common/ui/PantallaPrincipal.kt`, si `ubicacionExacta()` da `false`, mostrar una línea que se puede tocar, "La ubicación exacta está apagada", que llama a `abrirAjustesDelSistema()`. No entra en el camino de carga, y en el Android nunca aparece. La captura se guarda con la precisión que llegó, y queda degradada por el umbral de siempre (FR-024, Principio II)
- [X] T041 **Hecha**, sin `LocalContenedor` (ver T020). `iniciar()` además configura el caché del mapa como el Android y deja el último fallo de Kotlin en `Documents/fallo.txt`, visible en Archivos: sin Mac no hay otra consola. [US1] Crear `ios/MainViewController.kt` con `fun MainViewController() = ComposeUIViewController { CompositionLocalProvider(LocalContenedor provides …) { AppBuscarPatentes(…) } }` y `fun iniciar()`, que llama a `Plataforma.iniciar()` y arma el `Contenedor` con `construirBase()` de iOS. En `iosApp/iosApp/iOSApp.swift`, `didFinishLaunching` llama a `MainViewControllerKt.iniciar()`. Borrar `ios/piloto/` (T009)
- [X] T042 **Hecha** (2026-09-29): el usuario la instaló y anotó una patente, que se guardó bien. Encontró texto negro sobre fondo negro en Ajustes con el iPhone en oscuro: el tema no fijaba `LocalContentColor` y Ajustes y Salidas no tienen `Surface`. Se corrigió en `Tema.kt`, y arregla también el Android en modo oscuro. [US1] **Compilar iPhone** y correr el quickstart §2: carga en 4 toques y 10 segundos o menos, modo avión, ubicación exacta apagada, ficha sin edición y volver desde cada pantalla. Anotar los tiempos en esta tarea. **Verificar Android** (solo pruebas y APK: esta story no toca el Android)

**Checkpoint**: el iPhone anota y muestra. Todavía sin fotos, sin avisos y sin salidas.

---

## Phase 4: User Story 2 - Reinstalar cada semana sin perder nada (Priority: P1)

**Goal**:
- reinstalar conserva todo;
- la app muestra y avisa el vencimiento;
- se puede sacar un respaldo a un lugar fuera de la app.

**Independent Test**: con datos, reinstalar el mismo `.ipa` y uno nuevo, y comparar. Mirar el
vencimiento en ajustes. Sacar un respaldo y verlo en Archivos y en iTunes (quickstart §3).

- [X] T043 **Hecha**. Las fechas van en epoch millis, como el resto de la app, y `Vencimiento.deEstaInstalacion` lee el perfil una sola vez. [P] [US2] Crear `common/respaldo/Vencimiento.kt` y `commonTest/respaldo/VencimientoTest.kt`:
  - **`fun leer(perfil: ByteArray?): Instant?`**: busca `<key>ExpirationDate</key>` seguido de `<date>…</date>` en el texto ISO-8859-1 del perfil y lo parsea como ISO 8601.
  - **`fun mostrarAviso(ahora: Instant, vence: Instant?): Boolean`**: da `true` si `vence != null` y `vence - ahora <= 48.hours`.
  - **Pruebas**:
    - un perfil de muestra con bytes binarios antes y después del plist;
    - `null`, un perfil sin la clave y una fecha rota;
    - los bordes exactos de 48 horas, 47:59 y 48:01, y una fecha ya pasada.
  (D15, P7)
- [X] T044 **Hecha**, con dos cambios: el disparo es `UNTimeIntervalNotificationTrigger`, que da lo mismo sin armar componentes de calendario, y el permiso de notificaciones se pide ahí la primera vez, porque el aviso tiene que llegar aunque los avisos de patentes estén apagados. [P] [US2] En `ios/plataforma/Plataforma.ios.kt`:
  - **`perfilDeAprovisionamiento()`**: lee `NSBundle.mainBundle.pathForResource("embedded", "mobileprovision")`.
  - **`programarAvisoDeVencimiento(en)`**: una `UNNotificationRequest` con identificador fijo `vencimiento`, trigger `UNCalendarNotificationTrigger` en `en - 24h`, y el texto "La app vence mañana a las HH:MM. Reinstalala desde la PC.". Si `en - 24h` ya pasó, no programa nada.
  (D15, FR-012)
- [X] T045 **Hecha**. [US2] En la UI común:
  - **`PantallaAjustes.kt`**: una fila "La instalación vence el …", con `Fechas`, visible solo si `Vencimiento.leer(perfilDeAprovisionamiento())` no es `null` (FR-010).
  - **`PantallaPrincipal.kt`**: una línea que no se toca, "Vence el …", cuando `mostrarAviso` da `true` (FR-011). No agrega pasos a la carga (Principio I).
  - **Al arrancar**: `AppBuscarPatentes` llama a `programarAvisoDeVencimiento(vence)` (FR-012).
- [X] T046 **Hecha**, sin `Preferencias`: la fecha del último respaldo sale del nombre de los archivos de `Carpetas.respaldos`, que ya la dice. Si el jugador los borra desde Archivos, vuelve a decir "nunca". El paso 2 pasa la copia a `journal_mode = DELETE` para que viaje un solo archivo. [US2] Crear `common/respaldo/Respaldo.kt` con `suspend fun sacar(…): Resultado`, según R3:
  1. `VACUUM INTO` sobre la conexión de escritura de Room (`useWriterConnection`, fuera de transacción) en `Carpetas.temporal`.
  2. Abrir la copia con `BundledSQLiteDriver`, crear `respaldo_info` y `respaldo_foto` y llenarlas (R2).
  3. Mover la copia a `Carpetas.respaldos` como `buscar-patentes-AAAAMMDD-HHMM.respaldo`.
  4. Guardar `ultimoRespaldoEn` en `Preferencias`.
  Si algo falla, borrar el temporal y devolver el error. Nunca escribe la base (R3)
- [X] T047 **Hecha**, en `commonTest`. Corre en la JVM de la PC con la biblioteca nativa de escritorio de `sqlite-bundled`, que la tarea `nativoDeSqlite` de `shared/build.gradle.kts` extrae y le pasa a la JVM, y en el simulador del iPhone en la nube. El `VACUUM INTO` se hace a mano sobre una base con el esquema de Room. [P] [US2] Crear `commonTest/respaldo/RespaldoTest.kt`, que corre en la JVM y cubre la parte de sacar:
  - una base con registros, fotos (algunas faltantes), salidas y votos da `respaldo_info` con las cuentas correctas (`patentes`, `salidas`, `fotos`, `fotosFaltantes`, `ultimaCaptura`) y `user_version` 7;
  - la base de origen queda sin cambios.
  Si `useWriterConnection` no se puede probar en la JVM, se prueba el paso 2 en adelante sobre una copia hecha a mano (R8)
- [X] T048 **Hecha**, sin mirar la API del Android: con menos de 30, `VACUUM INTO` falla y la pantalla muestra el error, sin cerrar nada. Todos los teléfonos del juego tienen más. [US2] En `common/ui/PantallaAjustes.kt`, sumar la sección **Respaldo**:
  - el botón "Sacar respaldo" llama a `Respaldo.sacar` y después a `rememberGuardarRespaldo()` con la ruta;
  - debajo, "Último respaldo: …" o "Nunca" (FR-013, FR-020);
  - **en el Android**, la sección solo aparece con API 30 o más (D8).
  Es el único cambio visible en el Android (FR-031)
- [X] T049 **Hecha** como función común `mandarRespaldo(ruta)`, igual que `compartir` (T025): no necesita estado de Compose. [P] [US2] Implementar `rememberGuardarRespaldo`:
  - **Android**, en `android/plataforma/Pantallas.android.kt`: `ACTION_SEND` con `FileProvider` y el tipo `application/octet-stream`; sumar `<files-path name="respaldos" path="respaldos/" />` a `app/src/main/res/xml/file_paths.xml`. La carpeta es `filesDir/respaldos`, no la caché: ahí queda también el respaldo automático previo a restaurar, y el sistema no la vacía (P1).
  - **iPhone**, en `ios/plataforma/Pantallas.ios.kt`: `UIActivityViewController` con la URL del archivo, presentado desde el controlador raíz.
  (P5)
- [ ] T050 [US2] **Compilar iPhone** y correr el quickstart §3: reinstalar el mismo `.ipa` y uno nuevo cronometrando, fecha en ajustes, sacar respaldo, verlo en Archivos y en iTunes y la fecha del último respaldo. Los avisos de 48 y 24 horas se validan en la semana real: dejar esta tarea abierta hasta verlos. **Verificar Android** con la sección Respaldo: sacar uno y mandarlo a Descargas

**Checkpoint**: el iPhone se puede reinstalar sin miedo, y hay un respaldo fuera de la app.

---

## Phase 5: User Story 3 - Traer todo del Android (Priority: P1)

**Goal**: un respaldo del Android se restaura en el iPhone con todo idéntico, y al revés.

**Independent Test**: restaurar un respaldo real del Android en el iPhone y comparar cuentas y cinco
registros campo por campo. Rechazar un archivo inválido (quickstart §4).

- [X] T051 **Hecha**. Antes de abrir mira la cabecera `SQLite format 3`: un archivo que la tiene y después no se lee es un respaldo dañado, no "otra cosa". [US3] En `common/respaldo/Respaldo.kt`, sumar `suspend fun validar(archivo): Validacion`, según R4. Abre **una copia** con `BundledSQLiteDriver` y devuelve `Valido(cuentas)` o `Rechazado(motivo)` con los textos exactos de R4
- [X] T052 **Hecha**. Los archivos de al lado de la base (`-wal`, `-shm`, `-journal`) se apartan con `.anterior` junto con la base, en vez de borrarse: si hay que deshacer, vuelven. `cambiar` deshace solo lo que alcanzó a mover. [US3] En `common/respaldo/Respaldo.kt`, sumar `suspend fun restaurar(archivo, conRespaldoPrevio: Boolean): Resultado`, según R6:
  1. el respaldo automático si corresponde; si falla, no se sigue;
  2. preparar en `Carpetas.temporal/preparado/`: extraer las fotos, borrar las dos tablas y hacer `VACUUM`;
  3. el cambio de nombres (base y fotos a `.anterior`, los nuevos a su lugar, borrar `-wal` y `-shm`), deshaciéndolo si algo falla;
  4. se devuelve la base nueva para reabrir;
  5. se borran los `.anterior`.
  El resultado lleva las cuentas y las fotos faltantes (R6.7). **Nunca** fusiona (FR-017)
- [X] T053 **Hecha**, junto con T047. Suma un caso: deshacer después de que la base nueva no abrió, con el `-wal` que habría dejado Room. [P] [US3] Completar `commonTest/respaldo/RespaldoTest.kt` con R8:
  - **ida y vuelta**: `latitud`, `longitud`, `precisionMetros` y `capturadoEn` comparados con `toRawBits()` en cada fila;
  - **rechazos**: texto, SQLite sin `respaldo_info`, `formato` 2, `user_version` 99 y archivo truncado, y la base de destino intacta en cada caso;
  - **foto faltante**: el registro llega igual;
  - **falla a mitad**: simular un error después de preparar y antes del cambio de nombres, y comprobar que nada cambió.
- [X] T054 **Hecha** con `reabrirContenedor()`: `iniciarContenedor` recibe cómo construir la base y la guarda. El `construirBase` del Android dejó de guardar su instancia. Alcanza con cambiar el global porque se restaura desde Ajustes y las pantallas con datos arrancan de cero al volver. [US3] Hacer reabrible el contenedor:
  - en `common/Contenedor.kt`, un `mutableStateOf<Contenedor>` en la raíz de `AppBuscarPatentes`, o un `Contenedor.reabrir()` que cierra Room y construye uno nuevo;
  - la UI que depende de los datos se recompone desde cero;
  - después se llama a `Vigilancia.reconciliar(...)` (R6.6).
  El Android usa el mismo camino, y la `App` de Android conserva la referencia nueva
- [X] T055 **Hecha**. El indicador es un diálogo que no se cierra ni con el gesto de volver: con la base cerrada, cualquier toque que escriba cerraría la app. No deja restaurar con una salida grabando. [US3] En `common/ui/PantallaAjustes.kt`, sección Respaldo, el botón "Restaurar respaldo":
  - llama a `rememberElegirRespaldo` y después a `validar`;
  - si `Rechazado`, muestra el motivo;
  - si el teléfono tiene patentes, muestra el diálogo de R5 con las dos cuentas y las dos fechas;
  - si no, o al confirmar, llama a `restaurar`;
  - después reabre (T054) y muestra el resultado de R6.7.
  Mientras dura, un indicador que no deja volver a tocar (FR-016, FR-018, FR-019)
- [X] T056 **Hecha**. [P] [US3] Implementar `rememberElegirRespaldo`:
  - **Android**, en `android/plataforma/Pantallas.android.kt`: `ActivityResultContracts.OpenDocument()` con `arrayOf("*/*")`, y copiar el `Uri` a `Carpetas.temporal`.
  - **iPhone**, en `ios/plataforma/Pantallas.ios.kt`: `UIDocumentPickerViewController(forOpeningContentTypes = [UTTypeData], asCopy = true)` y entregar la URL copiada.
  (P5)
- [ ] T057 Parcial (2026-09-29): el usuario sacó el respaldo en el Android y lo restauró en el iPhone, y dijo que funcionó perfecto. Faltan los rechazos, la vuelta al Android y la comparación campo por campo. [US3] Mudanza de prueba, **sin reemplazar todavía el Android**, con el quickstart §4 entero:
  1. respaldo del Android;
  2. PC por cable, iTunes y restaurar en el iPhone;
  3. cuentas y cinco registros comparados campo por campo contra las fichas del Android;
  4. los dos rechazos;
  5. restaurar con datos en el iPhone, que deja el `…-antes-de-restaurar.respaldo`;
  6. la vuelta: en el iPhone, sacar un respaldo **con los datos que vinieron del Android**, sin anotar nada nuevo. Restaurarlo en el Android y comparar las cuentas. El Android termina con lo mismo que tenía, y el respaldo automático de R6 queda como red;
  7. la comparación lado a lado del quickstart §2, con los mismos datos en los dos teléfonos (SC-005).
  Cronometrar y anotar acá. **Verificar Android**

**Checkpoint**: los datos cruzan en los dos sentidos. Todavía no es la mudanza real: esa la decide el
usuario cuando la US6 esté (o cuando decida no esperarla).

---

## Phase 6: User Story 4 - La foto, el WhatsApp y el camino hasta la patente (Priority: P2)

**Goal**: en el iPhone se saca foto desde la app, se comparte a WhatsApp con foto y texto, y "Ir" y
el recorrido abren Google Maps.

**Independent Test**: foto desde la ficha, compartir a un chat, "Ir" y un recorrido de tres paradas
(quickstart §5).

- [X] T058 **Hecha** en `Plataforma.ios.kt`, junto al resto de la costura. El permiso de cámara ya lo pedía la pantalla (T039). [P] [US4] En `ios/plataforma/Pantallas.ios.kt`, implementar `rememberSacarFoto(alTerminar)`: `UIImagePickerController` con `sourceType = Camera`. La `UIImage` va a JPEG con `UIImageJPEGRepresentation(imagen, 0.85)` y se escribe en el `destino` que da `AlmacenFotos.archivoNuevo` (mismo nombre `<capturadoEn>.jpg`). Pide cámara con `AVCaptureDevice.requestAccessForMediaType` si hace falta (D14, P5)
- [X] T059 **Hecha** como la función común `compartir(texto, foto)` (ver T025), con la foto como `UIImage`. [P] [US4] En `ios/plataforma/Pantallas.ios.kt`, implementar `rememberCompartir()`: `UIActivityViewController` con `[texto, UIImage(contentsOfFile: foto)]`, presentado desde el controlador raíz. La marca de compartida la pone el código común al abrir la hoja, igual que en el Android (D14)
- [X] T060 **Hecha** con links de Google Maps: `abrirPunto` busca las coordenadas y `abrirRecorrido` abre la URL de siempre. Como en el Android, sin la app instalada y con más paradas de las que entran en el navegador no abre; para saberlo, `comgooglemaps` va en `LSApplicationQueriesSchemes`. [P] [US4] En `ios/plataforma/Plataforma.ios.kt`, implementar `abrirUrl(url)` con `UIApplication.sharedApplication.openURL(NSURL(string = url), emptyMap<Any?, Any>(), null)`. Es la misma URL que arma `Ir.urlDeRecorrido` (D14, P6)
- [X] T061 **Hecha** (2026-09-29): el usuario probó foto, compartir e "Ir" en el iPhone y dijo que funciona todo perfecto. [US4] **Compilar iPhone** y correr el quickstart §5. Anotar si Google Maps abre la app o Safari. **Verificar Android** (solo pruebas y APK)

---

## Phase 7: User Story 5 - Avisos con la app cerrada (Priority: P2)

**Goal**: con la app cerrada, y después de reiniciar, el iPhone avisa a 150 m de una patente del
número actual sin compartir, y tocar el aviso abre el mapa en ella.

**Independent Test**: el quickstart §6 con una patente real. **Si T012 marcó los avisos como "solo
Android", esta fase se reduce a T066**, que solo actualiza ajustes.

- [ ] T062 [P] [US5] Crear `ios/ubicacion/Vigilancia.ios.kt` con `actual object Vigilancia`:
  - **`limite = 20`**.
  - **`reconciliar(deseados)`**: los ids vigilados salen de `manager.monitoredRegions` (identificador `registro-<id>`). Qué quitar y qué agregar lo decide **`Avisos.reconciliar(...)`**, igual que en `Geofences` del Android: no se escribe otra comparación. Después, `stopMonitoringForRegion` para las que sobran, y `CLCircularRegion(center, radius = Avisos.RADIO_METROS, identifier)` con `notifyOnEntry = true` y `notifyOnExit = false` para las que faltan.
  - Los deseados salen de `Avisos.aRegistrar(…, limite)`.
  (D11, P4, FR-025)
- [ ] T063 [US5] En el delegado de `ios/ubicacion/Ubicacion.ios.kt`, `locationManager(_:didEnterRegion:)`:
  1. saca el id del identificador;
  2. lee el registro y el estado del juego de Room;
  3. aplica `Avisos.corresponde(...)`, con `yaAvisadoEnEstaSalida = Avisos.yaAvisado(ahora, ultimoAviso)` (T015). `ultimoAviso` se lee y se guarda en `NSUserDefaults` con las mismas claves que `GeofenceReceiver` (`ultimo_aviso_<id>`);
  4. si corresponde, publica una `UNNotificationRequest` con el mismo título y texto que el Android y `userInfo = ["registroId": id]`.
  Tiene que funcionar con la app lanzada en segundo plano por el sistema: el manager y el contenedor ya existen desde `iniciar()` (D11, FR-026)
- [ ] T064 [US5] En `ios/MainViewController.kt`, sumar un `UNUserNotificationCenterDelegate`:
  - al tocar una notificación con `registroId`, lo entrega a `AppBuscarPatentes(registroAAbrir = …)` (T027), que centra el mapa y abre la ficha, como en el Android;
  - con la app adelante, muestra la notificación igual (`willPresentNotification` con banner).
- [ ] T065 [US5] Llamar a `Vigilancia.reconciliar` en los mismos momentos en que el Android llama a `Geofences.reconciliar`: al arrancar, al cambiar el número actual, al compartir, al activar o desactivar los avisos y después de restaurar. Los llamados que hoy están en `App.kt` y en la UI pasan por `Vigilancia` en código común, así el Android sigue igual
- [ ] T066 [US5] En `common/ui/PantallaAjustes.kt`, si los avisos están activos y falta el permiso "siempre", mostrar "Los avisos no llegan con la app cerrada: falta el permiso de ubicación 'siempre'", con un botón a `abrirAjustesDelSistema()`. Al activar los avisos en el iPhone, pedir `requestAlwaysAuthorization` (D13, FR-027). Si T012 encontró que después de cerrar la app deslizándola no llegan, sumar debajo: "Si cerrás la app deslizándola, los avisos se cortan hasta que la abras." (D11)
- [ ] T067 [US5] **Compilar iPhone** y correr el quickstart §6: app cerrada, después de reiniciar, tocar el aviso, un solo aviso por salida y permiso "mientras se usa". **Verificar Android**, y en el Android caminar hasta una patente con la app cerrada: el aviso tiene que llegar como antes

---

## Phase 8: User Story 6 - Salidas con la pantalla apagada (Priority: P3)

**Goal**: en el iPhone, una salida graba con la pantalla bloqueada, se ajusta a calles y se ve por
antigüedad. Si se corta, se conserva y la app lo dice.

**Independent Test**: el quickstart §7. **Si T012 marcó la grabación como "solo Android", esta fase
se reduce a ocultar el botón de salida en el iPhone**, con una línea en ajustes que lo explique.

- [X] T068 **Hecha** en `ios/plataforma/Grabacion.ios.kt`, sin `activityType` (con las pausas automáticas apagadas no cambia nada) y sin pedir "siempre" adentro: ya lo pide la pantalla principal antes de empezar, igual que en el Android. Con "mientras se usa" alcanza, porque la grabación arranca con la app adelante. Los puntos llevan la hora del GPS y descartan precisión peor que 30 m, como el Android. [P] [US6] Crear `ios/ubicacion/Grabacion.ios.kt` con `actual object Grabacion`:
  - **`iniciar(recorridoId)`**: pide "siempre" si falta, y configura el manager con `allowsBackgroundLocationUpdates = true`, `pausesLocationUpdatesAutomatically = false`, `showsBackgroundLocationIndicator = true`, `distanceFilter = 10.0`, `desiredAccuracy = kCLLocationAccuracyBest` y `activityType = CLActivityTypeFitness`. Después, `startUpdatingLocation`.
  - **`didUpdateLocations`**: inserta cada punto en `punto_de_trayecto`, con precisión y hora del proveedor.
  - **`terminar()`**: `stopUpdatingLocation`.
  - **`activa`**: `true` solo mientras grabe este proceso.
  (D12, P3)
- [X] T069 **Hecha**. `iniciar()` cierra las salidas abiertas antes de la primera pantalla (con `runBlocking`, para no cerrar una salida que el jugador empiece enseguida) y prende `salidaCortada`, que la pantalla principal muestra una vez. [US6] En `ios/MainViewController.kt` `iniciar()`, si hay un recorrido `EN_CURSO` y `Grabacion.activa` es `false`, llamar a `cerrarLosAbiertos(ahora)` y guardar una marca, y que `AppBuscarPatentes` muestre una vez "La salida se cortó. Lo grabado hasta ahí quedó guardado.". Solo en el iPhone: en el Android el servicio sobrevive (D12, FR-028)
- [X] T070 **Hecha**. `hayConexion()` da siempre `true` en el iPhone: saberlo es asíncrono, y sin red `postear` falla en el acto sin gastar la espera. [P] [US6] En `ios/plataforma/Plataforma.ios.kt`, implementar `postear(url, cuerpo)` con `NSURLSession.sharedSession.dataTaskWithRequest`: POST, `Content-Type: application/json`, 20 s de espera y el cuerpo en UTF-8. Devuelve el texto de la respuesta o lanza una excepción con el código HTTP. El ajuste a calles corre desde el código común (D16, P6)
- [X] T071 **Hecha** (2026-09-29): el usuario salió con el iPhone y dijo que funcionó todo perfecto: grabó con la pantalla bloqueada y encontró la 338. Batería: en 1 h 05 min bajó de 42% a 28%, o sea 14%, unos 13% por hora, mirando el mapa por ratos. Con eso SC-007 se revisó de 5% a 30% en 2 horas, que el jugador considera realista. [US6] **Compilar iPhone** y correr el quickstart §7: salida de 2 horas bloqueada con la batería anotada (SC-007), salida cortada deslizando la app, ajuste a calles y modo antigüedad. Anotar el consumo de batería acá. **Verificar Android**, con una salida corta: graba como antes

**Checkpoint**: el iPhone hace todo lo que hace el Android. Es el momento de la mudanza real, que la
decide el usuario: quickstart §4 con los datos de ese día.

---

## Phase 9: Polish & Cross-Cutting Concerns

- [ ] T072 [P] `README.md`:
  - la primera línea deja de decir "App Android": ahora es Android y iPhone;
  - sección **iPhone**: requisitos (iTunes de la web, Sideloadly, modo de desarrollador, mismo Apple ID), compilar (`gh workflow run`), bajar e instalar, reinstalación semanal y vencimiento;
  - sección **Respaldo** junto a la de `respaldo.ps1`: el de la app sirve en los dos teléfonos, y el script queda para el Android de desarrollo;
  - comandos de prueba: `.\gradlew.bat :shared:testAndroidHostTest`;
  - tabla de pruebas: `RespaldoTest`, `VencimientoTest`, `AlmacenFotosTest` y `FechasTest`, y dónde corren (JVM y simulador);
  - enlace a la spec y al quickstart de la 006.
- [ ] T073 [P] En `specs/006-jugar-en-iphone/research.md`, completar D21 con los resultados de T012, y D5 con cómo terminó el mapa (compartido o con línea de corte)
- [ ] T074 Correr el quickstart §8 (SC-010, SC-011) sobre todo lo que se va a subir. Revisar en GitHub que **Secrets** esté vacío
- [ ] T075 Revisar las `NotImplementedError` que queden en `ios/`: solo pueden quedar las de stories marcadas "solo Android" en T012, y cada una tiene que tener detrás un camino de UI que no la llame
- [ ] T076 Pasada final del quickstart entero en los dos teléfonos (§1 a §7). Marcar SC-001 a SC-011 en `spec.md` con lo medido

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1**: T001 primero, T002 a T004 en paralelo, T005, T006, y después T007 a T012 en orden.
  **T012 es una compuerta**: si reinstalar pierde datos, no se sigue.
- **Phase 2**: depende de T012 y **bloquea todas las stories**. El orden interno es dominio →
  base → ubicación → UI → Android verificado (T031) → mapa → Android verificado (T036).
- **US1 (Phase 3)**: depende de Phase 2. **Todas las demás stories dependen de US1**, porque sin la
  app arrancando en el iPhone no hay nada que probar.
- **US2 y US3**: US3 usa `Respaldo.sacar` de US2 (el respaldo automático) y la sección Respaldo de
  ajustes. Van en ese orden.
- **US4, US5 y US6**: dependen solo de US1. Entre ellas son independientes.
- **Polish**: al final. T074 corre además **antes de cada push**.

### User Story Dependencies

| Story | Depende de | Se puede probar sola |
|---|---|---|
| US1 | Phase 2 | Sí |
| US2 | US1 | Sí |
| US3 | US2 (`sacar` y la sección de ajustes) | Sí |
| US4 | US1 | Sí |
| US5 | US1 | Sí |
| US6 | US1 | Sí |

### Within Each User Story

- Los `actual` de iOS de una story ([P], archivos distintos) primero, después la UI común que los
  usa, y al final compilar iPhone y verificar el Android.
- Las pruebas nuevas (`VencimientoTest`, `RespaldoTest`) van junto a su función: son JVM y no
  esperan a la nube.

### Parallel Opportunities

- **Phase 1**: T002, T003 y T004. T010 y T011 después de T009.
- **Phase 2**: T015 junto a T016, T017 junto a T018. T023 y T024 antes de T026. T028 después de
  T026.
- **Stories**: los `actual` de iOS de cada una (T037 a T039, T043 y T044, T058 a T060, T062, T068 y
  T070) están en archivos distintos. Una vez terminada US1, **US4, US5 y US6 pueden ir juntas**, y
  conviene juntarlas en una sola compilación de iPhone, porque cada vuelta cuesta 10 a 15 minutos.

---

## Parallel Example: User Story 1

```text
Task: "T037 Carpetas y Preferencias en ios/plataforma/Plataforma.ios.kt"
Task: "T038 CLLocationManager, leerUbicacion y ubicacionExacta en ios/ubicacion/Ubicacion.ios.kt"
Task: "T039 rememberPedirPermisos y abrirAjustesDelSistema en ios/plataforma/Pantallas.ios.kt"
```

## Parallel Example: US4 + US5 + US6 en una sola compilación

```text
Task: "T058 rememberSacarFoto en ios/plataforma/Pantallas.ios.kt"
Task: "T062 Vigilancia en ios/ubicacion/Vigilancia.ios.kt"
Task: "T068 Grabacion en ios/ubicacion/Grabacion.ios.kt"
Task: "T070 postear en ios/plataforma/Plataforma.ios.kt"
```

Después, una sola vuelta de "compilar iPhone" para T061, T067 y T071.

---

## Implementation Strategy

### MVP First

1. **Phase 1**, hasta la compuerta de T012. Si Apple no deja reinstalar sin perder datos, se sabe
   acá, sin haber portado nada.
2. **Phase 2**: el Android multiplataforma, verificado dos veces en el teléfono.
3. **US1**: el iPhone anota y muestra. **STOP and VALIDATE**, con el quickstart §2.

### Incremental Delivery

1. **US2**: se puede reinstalar sin miedo. Desde acá, el iPhone sirve para probar en la calle.
2. **US3**: los datos cruzan en los dos sentidos.
3. **US4, US5 y US6**, juntas si se puede. Con las tres, el iPhone es el teléfono de juego, y la
   mudanza real la decide el usuario.
4. Cada paso termina con el **Android verificado**: es el teléfono de respaldo durante toda la
   feature.

### Líneas de corte

| Dónde | Si falla | Qué pasa |
|---|---|---|
| T012 | Reinstalar pierde datos | Se para la feature |
| T012 | Avisos o grabación con el Apple ID gratis | La US5 o la US6 quedan "solo Android", y lo dice ajustes |
| T036 | El mapa compartido en el Android | El Android conserva `MapaViejo` detrás de un `expect` |

---

## Notes

- [P] = archivo distinto, sin depender de nada incompleto.
- Commits con Conventional Commits, uno por tarea o por grupo lógico. Antes de cada push, quickstart
  §8.
- **No publicar** (T006) ni hacer la mudanza real (checkpoint de la US6) sin la confirmación del
  usuario en el chat.
- El teléfono Android no tiene SIM: adb solo lee. El APK siempre va a
  `/sdcard/Download/buscar-patentes.apk`, y el usuario lo instala a mano.
