# Research: Jugar en el iPhone

Decisiones de Phase 0. Cada una dice qué se eligió, por qué, y qué se descartó. Las fuentes
externas están al final.

Dos hechos ordenan casi todo lo demás:

1. **No hay Mac ni simulador.** Todo lo que sea del iPhone se compila en la nube y se prueba
   instalándolo en el teléfono. Una vuelta cuesta 10 a 15 minutos, contra 2 del Android. Por eso
   el código compartido se desarrolla **y se valida primero en el Android**, y del lado iPhone
   queda solo el pegamento con el sistema.
2. **El Android no se puede romper.** Tiene los datos y es el teléfono de respaldo. Cada paso que
   lo toca termina con las pruebas pasando y el APK actualizado sobre el anterior, sin reinstalar.

## D1 — Kotlin Multiplatform: un módulo compartido y dos apps

**Decisión**: un módulo nuevo `:shared` (Kotlin Multiplatform) con `commonMain`, `androidMain` e
`iosMain`. `:app` sigue siendo la app Android, con el mismo `applicationId`, y depende de `:shared`.
La app de iPhone vive en `iosApp/`: un punto de entrada en Swift de pocas líneas que muestra la UI
de Compose. Los paquetes de Kotlin no cambian (`ar.lauta.buscarpatentes.*`): los archivos se mudan
de carpeta, no de nombre.

**Por qué**: `domain/` (unas 970 líneas) ya es Kotlin puro, y la UI (unas 3.400) es Compose, que
corre en el iPhone con Compose Multiplatform. Lo que es de cada sistema (ubicación, avisos,
cámara, compartir) se aísla detrás de funciones `expect`/`actual` (D9). Así hay **un solo juego de
reglas**, que es lo que pide el FR-022: con los mismos datos, los dos teléfonos dicen lo mismo.

**Por qué dos módulos y no uno**: AGP 9 no deja combinar `com.android.application` con el plugin
multiplataforma en el mismo módulo. La biblioteca compartida usa
`com.android.kotlin.multiplatform.library`.

**Descartado**:
- **Reescribir el iPhone en SwiftUI**: duplica unas 6.000 líneas, y cada feature futura se haría
  dos veces. Además, sin Mac no se puede ni previsualizar una pantalla de SwiftUI.
- **Flutter o React Native**: reescribir todo, incluido el Android que ya funciona.
- **Convertir `:app` en multiplataforma**: no lo permite AGP 9 (arriba).

## D2 — Versiones: Kotlin 2.4.20 y Compose Multiplatform 1.12.1

**Decisión**:

| Pieza | Hoy | Pasa a |
|---|---|---|
| Kotlin | 2.3.21 | **2.4.20** |
| Compose (Android) | BOM 2026.06.01 | **Compose Multiplatform 1.12.1** (Jetpack Compose 1.12) |
| Room | 2.8.4 | 2.8.4 (ya es multiplataforma) |
| KSP | 2.3.11 | la que corresponda a Kotlin 2.4.20 |
| AGP | 9.3.2 | 9.3.2 |
| MapLibre | Android SDK 13.6.0 | **maplibre-compose 0.18.0** (D5) |

**Por qué 2.4.20**: maplibre-compose 0.18.0 está compilado con Kotlin 2.4.20, y en iOS un
compilador más viejo no lee bibliotecas de uno más nuevo. Compose Multiplatform 1.12.1 es la
última estable y pide Kotlin 2.1 o más.

**Riesgo**: subir Kotlin y Compose cambia el compilador del Android. Va en su propio paso, antes de
tocar nada más, con las 119 pruebas y el APK en el teléfono como verificación (D20).

## D3 — Targets de iPhone: `iosArm64`, y el simulador solo para pruebas

**Decisión**: la app se compila para `iosArm64`, el iPhone real. Se suma `iosSimulatorArm64` **solo
para correr las pruebas comunes en la nube** (D17). Nada para Intel.

**Por qué**: sin Mac nadie usa el simulador para mirar la app. Pero correr las mismas pruebas en el
motor del iPhone es la única prueba automática del FR-022: el texto de una fecha o de una
distancia puede salir distinto en cada sistema, y eso lo ve una prueba, no una lectura del código.

## D4 — Compilar en GitHub Actions, firmar en la PC

**Decisión**: un workflow `.github/workflows/ios.yml` en un runner de macOS, que se lanza a mano
desde la PC (`gh workflow run ios.yml`) y además con cada push a `main`. El workflow:

1. corre las pruebas comunes en el simulador (D17);
2. compila el framework de Kotlin para `iosArm64`;
3. genera el proyecto de Xcode con **XcodeGen** a partir de `iosApp/project.yml`;
4. compila con `xcodebuild`, con la firma apagada (`CODE_SIGNING_ALLOWED=NO`);
5. empaqueta `Payload/BuscarPatentes.app` como `buscar-patentes.ipa`, y lo sube como artefacto.

En la PC, `gh run download` baja el `.ipa`, y Sideloadly lo firma con el Apple ID y lo instala.

**Por qué**:
- **FR-003**: el workflow no necesita ninguna credencial de Apple. El `.ipa` sale sin firmar, y
  Sideloadly firma en la PC. Según su propia documentación, el Apple ID solo viaja a Apple.
- **FR-004**: el identificador (`ar.lauta.buscarpatentes`) está fijo en `project.yml`. Sideloadly
  solo cambia el identificador cuando choca con una app de la App Store, y este no choca.
- **XcodeGen**: un proyecto de Xcode (`.pbxproj`) no se escribe a mano. XcodeGen lo genera en el
  runner desde un YAML corto que sí se puede editar desde Windows, y no queda en el repositorio.
- **Repositorio público** (clarificación de la spec): los minutos de macOS no tienen tope.
- `gh` ya está instalado en la PC, con sesión iniciada en la cuenta del jugador.

**Descartado**:
- **Firmar en la nube**: pide subir credenciales de Apple a GitHub, y el FR-003 lo prohíbe.
- **Codemagic, Bitrise y otros**: servicios y cuentas nuevas, cuando GitHub alcanza.
- **Compilar klibs de iOS en Windows** (`kotlin.native.enableKlibsCrossCompilation`): serviría para
  revisar tipos de `iosMain` sin esperar a la nube. Queda como ayuda opcional, no como camino: KSP
  no corre sobre esos targets (issue 2267 de KSP), así que Room no compilaría. La nube manda.

## D5 — El mapa: maplibre-compose en los dos teléfonos

**Decisión**: el mapa pasa a **maplibre-compose 0.18.0** en `commonMain`, y reemplaza a `Mapa.kt`,
que hoy usa el MapLibre Android SDK directo. El Android usa el runtime de OpenGL, que es el motor
que usa hoy. Los pines, que hoy se dibujan con `android.graphics.Canvas`, pasan a dibujarse con
Compose. La caché de teselas, que hoy fija `OfflineManager`, sigue con el `OfflineManager` de la
biblioteca (SC-013 de la 001).

**Por qué**:
- Es la única forma de tener **un solo mapa**. Con dos, cada cambio futuro se hace dos veces, y la
  próxima feature grande del `TODO.md` (zonas objetivo) dibuja sobre el mapa.
- En iOS no pide configuración nativa: ni CocoaPods, ni Swift Package Manager, ni `cinterop`. Solo
  flags de linker en el proyecto de Xcode. Esto importa: un `cinterop` propio solo se procesa en una
  Mac.
- Tiene lo que usa el mapa hoy: fuentes GeoJSON, capas de círculo, línea y símbolo con imagen y
  texto, expresiones, estado de cámara, clic con consulta de features, indicador de ubicación
  (`LocationIndicatorLayer`) y caché ambiente.
- El acomodo de la 005 (D13) es código propio en `domain/`, no del motor. Pasa entero.

**Riesgo**: la biblioteca está en **beta** en Android y en iOS, y puede romper API entre versiones
menores. Se fija la versión exacta.

**Línea de corte**: el mapa compartido se valida **primero en el Android**, con los quickstart de la
004 y la 005. Si no llega, el Android conserva su `Mapa.kt` en `androidMain` detrás de un
`expect fun MapaDeFondo(...)`, y solo el iPhone usa el compartido. Se pierde el mapa único, no la
feature.

**Descartado**:
- **Un mapa por sistema, con el MapLibre iOS SDK por `cinterop`**: solo compila en una Mac, y
  duplica 1.160 líneas.
- **Google Maps o Apple Maps**: otro motor, otros estilos, y se pierde OpenFreeMap.

## D6 — La base: Room multiplataforma, y el Android con el mismo archivo

**Decisión**: `BaseDeDatos`, las entidades y los DAO pasan a `commonMain`, con `@ConstructedBy`.
Cada sistema arma la base con su propio builder:

| | Android | iPhone |
|---|---|---|
| Driver | El de siempre (modo de compatibilidad de Room, sin driver nuevo) | `BundledSQLiteDriver` |
| Archivo | `buscar-patentes.db`, el mismo de hoy | `Library/Application Support/buscar-patentes.db` |
| Migraciones | 1→7 como están, en `androidMain` | Ninguna: toda base de iPhone nace en v7 |

**Por qué**:
- **FR-030**: el Android abre el mismo archivo, con la misma versión y las mismas migraciones. Room
  valida el esquema con el mismo identity hash, porque las entidades no cambian. Es una
  actualización, no una reinstalación.
- Las migraciones 1→7 usan `SupportSQLiteDatabase`, que solo existe en Android. **No hace falta
  portarlas**: el iPhone nunca ve una base anterior a la 7. La crea de cero en la 7, o la recibe por
  respaldo de un Android que ya está en la 7.
- Las migraciones que vengan, de la 8 en adelante, se escriben en `commonMain` sobre
  `SQLiteConnection` y sirven a los dos (FR-009).
- Los DAO ya son todos `suspend` o `Flow`, que es lo que pide Room multiplataforma.
- **La base en `Application Support`, no en `Documents`**: `Documents` queda visible desde la app
  Archivos (D8), y la base no tiene que estar al alcance de un dedo.

## D7 — Las fotos se buscan por nombre, no por ruta

**Decisión**: `AlmacenFotos.archivo(ruta)` resuelve el nombre del archivo dentro de la carpeta de
fotos de la app, e ignora el resto de la ruta guardada. El dato guardado **no se reescribe**.

**Por qué**: hoy `fotoRuta` guarda la ruta absoluta del Android (`/data/user/0/…/files/fotos/…`).
En el iPhone esa ruta no existe. Además, **iOS puede cambiar la ruta del contenedor de la app al
actualizarla**: guardar rutas absolutas rompería las fotos en la primera reinstalación semanal
(FR-007). Buscar por nombre arregla las dos cosas sin migrar nada, y el Android sigue funcionando
igual, porque el nombre es el mismo.

## D8 — El respaldo es un archivo SQLite

**Decisión**: el respaldo es **una copia de la base**, hecha con `VACUUM INTO`, más dos tablas que se
le agregan: `respaldo_info` (formato, versión, fecha, origen, cuentas) y `respaldo_foto` (nombre y
bytes de cada foto). Es un solo archivo con extensión `.respaldo`. El formato está en
[contracts/respaldo.md](./contracts/respaldo.md).

Se lee y se escribe con `BundledSQLiteDriver` en `commonMain`: **una sola implementación** para los
dos teléfonos (FR-015), que además corre en la JVM, así que se prueba en Windows.

**Por qué**:
- **Principio II y FR-014**: una copia de SQLite no convierte ningún número. Latitud, longitud y
  precisión viajan en el mismo `REAL` de 8 bytes en el que se guardaron. Un JSON pasaría cada double
  por texto y de vuelta; Kotlin lo hace sin pérdida, pero así ni siquiera hay que demostrarlo.
- **Un solo archivo sin biblioteca de zip**: Kotlin/Native no trae zip, y las fotos ya son JPEG, así
  que comprimirlas no gana nada. Los BLOB de SQLite son el contenedor.
- **Restaurar es casi gratis**: quitar las dos tablas extra y poner el archivo en el lugar de la
  base. Si el respaldo es de una versión vieja, Room lo migra al abrirlo, como a cualquier base.

**Dónde queda y cómo viaja**:
- **iPhone**: se escribe en `Documents/respaldos/`, que se ve desde la app Archivos y desde el iTunes
  de la PC (`UIFileSharingEnabled`). Después se ofrece la hoja de compartir. Para restaurar, el
  selector de documentos del sistema.
- **Android**: se escribe en `filesDir/respaldos`, no en la caché, porque la caché la vacía el
  sistema y ahí queda el respaldo previo a restaurar. Después se ofrece la hoja de compartir
  (Drive, cable, lo que haya).
  Para restaurar, el selector de documentos del sistema.
- **Mudanza sin nube**: Android → PC por cable (el archivo queda en Descargas), PC → iPhone con el
  iTunes, en la sección de archivos compartidos de la app. El quickstart tiene los pasos.

**Restaurar sin dejar nada a medias (FR-018)**: todo se prepara en una carpeta aparte: se valida,
se extraen las fotos y se limpia la copia. Recién entonces se cierra Room, se renombra la base
actual a `.anterior`, se pone la nueva y se mudan las fotos. Si algo falla antes de ese cambio de
nombres, no cambió nada. Si falla durante, se vuelve a poner la `.anterior`. Antes de todo esto, si
el teléfono tenía patentes, se saca un respaldo automático a `respaldos/` (FR-016).

**Riesgo**: `VACUUM INTO` pide SQLite 3.27, que Android trae desde la versión 11. El teléfono del
jugador tiene HyperOS, que es Android 14 o más, así que alcanza. En un Android 8 a 10 el botón de
respaldo no aparece (`ponytail:` techo declarado; si hiciera falta, se copia el archivo después de
un `wal_checkpoint`).

**Descartado**:
- **Zip con la base y las fotos**: pide una biblioteca de zip en iOS.
- **JSON**: hay que escribir y mantener un serializador por tabla, y cada cambio de esquema lo
  rompe.
- **Seguir con `scripts/respaldo.ps1`**: usa `adb run-as`, que en el iPhone no existe. El script
  queda para el Android de desarrollo.

## D9 — La costura con cada sistema: `expect`/`actual`, y nada más

**Decisión**: lo que es de cada sistema se declara una vez en `commonMain` como `expect`, y tiene su
`actual` en `androidMain` y en `iosMain`. Lo que pide una pantalla del sistema (cámara, permisos,
compartir, elegir un archivo) es un **composable** `expect`, como `rememberSacarFoto(...)`, porque
en el Android se apoya en los launchers de Activity Result. La lista completa está en
[contracts/plataforma.md](./contracts/plataforma.md).

**Por qué**: cada `expect` tiene exactamente dos `actual`, así que cumple el gate de abstracciones
del Principio IV. No hay interfaces con una sola implementación ni inyección de dependencias. El
código de Android que hoy funciona (servicio de salida, geofences, receiver de arranque) se muda a
`androidMain` **sin reescribirse**.

## D10 — Ubicación en el iPhone: un solo `CLLocationManager`

**Decisión**: Core Location, llamado directo desde Kotlin (`platform.CoreLocation`, que Kotlin/Native
trae de fábrica). Hay **un solo** `CLLocationManager` en toda la app, que se crea al arrancar, desde
el `AppDelegate`, antes que cualquier pantalla (ver D11).

- **Carga rápida**: `requestLocation()` con `kCLLocationAccuracyBest`. Una lectura, con el mismo
  tiempo de espera que en el Android. `horizontalAccuracy` va a `precisionMetros`, y el umbral de
  degradada es el mismo.
- **Ubicación exacta apagada** (FR-024): si `accuracyAuthorization` es `reducedAccuracy`, la lectura
  se guarda como llegó (Principio II) y la app muestra un aviso con un botón a Ajustes.

## D11 — Avisos en el iPhone: vigilancia de regiones

**Decisión**: `startMonitoringForRegion` con un `CLCircularRegion` de 150 metros por patente, que
avisa al entrar. La notificación es local (`UNUserNotificationCenter`), y la deduplicación por
salida se guarda en `NSUserDefaults`, igual que hoy en las preferencias del Android.

- **El límite**: `Avisos.LIMITE_GEOFENCES` deja de ser una constante y pasa a ser un parámetro de
  `aRegistrar`. Vale 100 en el Android y 20 en el iPhone. El recorte sigue siendo el mismo (FR-025).
- **Con la app cerrada**: iOS relanza la app en segundo plano cuando se entra a una región. Por eso
  el `CLLocationManager` y su delegado se crean al arrancar: si no, el evento llega y nadie lo
  atiende.
- **Después de reiniciar**: iOS conserva las regiones vigiladas. No hace falta el receiver de
  arranque.
- **Tocar el aviso** abre la app con el id del registro en el `userInfo`, y la app centra el mapa
  en él.

**Riesgo abierto**: la documentación de Apple no deja claro si iOS relanza la app por una región
**después de que el jugador la cerró deslizándola** en el multitarea. Lo resuelve la prueba piloto
(D21). Si no la relanza, la spec ya lo contempla: el aviso llega con la app cerrada normalmente, no
después de matarla a mano. Eso se dice en ajustes.

**Descartado**: `CLMonitor`, la API nueva de iOS 17. Es solo para Swift, y la vigilancia de regiones
clásica sigue funcionando.

## D12 — Salida con la pantalla apagada en el iPhone

**Decisión**: `startUpdatingLocation` con `allowsBackgroundLocationUpdates = true`,
`pausesLocationUpdatesAutomatically = false`, `showsBackgroundLocationIndicator = true`, y el mismo
filtro de distancia que el Android (`DISTANCIA_MINIMA_M`). Se declara `UIBackgroundModes: location`
en el `Info.plist`. Cada punto va a Room, como en el Android.

- **Si la salida se corta** (FR-028): al arrancar, si hay una salida `EN_CURSO` y no hay grabación
  activa en este proceso, se cierra con `cerrarLosAbiertos` —que ya existe— y la app muestra "La
  salida se cortó". El mensaje es solo del iPhone: en el Android, el servicio sobrevive a que se
  cierre la app, y el FR-031 no deja sumar cambios visibles.
- **`UIBackgroundModes` es del `Info.plist`, no un entitlement**: no depende de la cuenta de Apple.
  Igual se confirma en la prueba piloto, porque es la suposición de la que depende la US6.

## D13 — Permisos en el iPhone

**Decisión**: al primer uso se pide ubicación "mientras se usa la app", que alcanza para anotar. El
permiso "siempre" se pide al activar los avisos o al empezar una salida: iOS muestra la mejora una
sola vez. Si no está (FR-027), la app lo dice en ajustes y ofrece un botón que abre los Ajustes del
sistema (`UIApplicationOpenSettingsURLString`). Los textos de cada permiso van en el `Info.plist`.

## D14 — Cámara, compartir y Google Maps en el iPhone

**Decisión**:
- **Cámara**: `UIImagePickerController` con la cámara. El JPEG va a la misma carpeta y con el mismo
  nombre que en el Android.
- **Compartir**: `UIActivityViewController` con el texto y la imagen. La patente se marca
  compartida **en el mismo momento que en el Android**, al abrir la hoja de compartir. Así los dos
  teléfonos se comportan igual (FR-022).
- **Google Maps**: se abre la misma URL `https://www.google.com/maps/dir/?api=1…` que arma `Ir`.
  Con Google Maps instalado, iOS la abre ahí; si no, en Safari (FR-021, caso borde de la spec).

## D15 — La fecha de vencimiento se lee del perfil de aprovisionamiento

**Decisión**: al firmar, Sideloadly mete `embedded.mobileprovision` en la app. Adentro hay un plist
en texto plano con `<key>ExpirationDate</key><date>…</date>`. Una función pura en `commonMain` lo
busca en los bytes del archivo y devuelve la fecha, o `null` si no lo encuentra. En el Android, o en
un `.ipa` sin firmar, no hay archivo, y la app no muestra nada de vencimiento.

- **En ajustes**: la fecha y la hora (FR-010).
- **En la pantalla principal**: una línea que no se toca, cuando faltan 2 días o menos (FR-011). No
  agrega ningún paso a la carga rápida.
- **La notificación del día anterior** (FR-012): cada vez que la app abre, reprograma una
  notificación local para 24 horas antes del vencimiento, con un identificador fijo que reemplaza la
  anterior.

**Descartado**: calcular el vencimiento como "fecha de instalación + 7 días". Sideloadly puede
refrescar la firma por Wi-Fi sin que la app se entere de que la reinstalaron. El perfil dice la
verdad.

## D16 — Fechas, JSON y red en código común

**Decisión**:
- **Fechas**: `kotlinx-datetime` reemplaza a `java.text.SimpleDateFormat` y `java.util.Date` en la
  UI. Los nombres de los meses van como lista propia en castellano, y así salen iguales en los dos
  teléfonos.
- **JSON**: `kotlinx-serialization-json`, solo la biblioteca (`buildJsonObject`,
  `parseToJsonElement`), sin plugin de compilador. Reemplaza a `org.json` en `AjustarACalles`, que
  no existe en iOS.
- **Red**: una sola función `expect suspend fun postear(url, cuerpo): String`. En el Android es el
  `HttpURLConnection` de hoy, y en el iPhone `NSURLSession`.
- **Archivos**: `kotlinx-io-core` (`SystemFileSystem`) reemplaza a `java.io.File` en `AlmacenFotos` y
  en el respaldo. Esas dos piezas necesitan saber si existe, borrar, medir, listar, mover
  atómicamente, leer y escribir bytes, y en código común no hay `File`.

**Descartado**:
- **Ktor**: es un cliente HTTP completo para una sola llamada POST.
- **Un `expect` por cada operación de archivos**: son unas diez operaciones con dos
  implementaciones cada una, justo lo que `kotlinx-io` ya resuelve.

## D17 — Las pruebas pasan a código común

**Decisión**: las pruebas de `domain/` y de `mapa/` se mudan a `shared/src/commonTest`, con
`kotlin.test` en lugar de JUnit. Corren en la JVM en la PC (`./gradlew :shared:testAndroidHostTest`)
y en el simulador de iPhone en la nube (`:shared:iosSimulatorArm64Test`). Eso último es la prueba
automática del FR-022.

- `InmutabilidadTest` usa reflexión de Java, así que se queda en las pruebas de la JVM.
- `ColeccionTest` se reescribe contra los tipos GeoJSON de maplibre-compose.
- **Pruebas nuevas**:
  - `RespaldoTest`, en la JVM con `BundledSQLiteDriver`: ida y vuelta idéntica bit a bit, rechazo
    de archivos inválidos y de versiones más nuevas, foto faltante.
  - `VencimientoTest`, sobre la lectura del perfil.
  - `AvisosTest` suma el caso del límite como parámetro.

**Cuidado al convertir**: `assertEquals` de JUnit recibe el mensaje **primero**, y el de
`kotlin.test` lo recibe **último**. Hay que dar vuelta cada prueba que lo use.

## D18 — Volver atrás en el iPhone

**Decisión**: no hace falta nada nuevo. `BarraSuperior` tiene `navigationIcon`, y las pantallas de
salidas y de ajustes y el detalle de una salida ya lo muestran (`PantallaRecorridos.kt`: el detalle
usa `BarraSuperior(fechaLarga(…), onVolver = { abierta = null })`). El `BackHandler` pasa a ser el de
Compose Multiplatform, así el botón atrás del Android sigue andando. El Android no cambia de aspecto
(FR-031).

Si alguna pantalla nueva llegara a necesitar una flecha que el Android no tiene, se muestra solo en
el iPhone, con un `expect val` de una línea.

## D19 — Enmienda de la constitución: 1.1.0 → 1.2.0

**Decisión**: una enmienda MINOR, antes de las tareas.
- **Restricciones técnicas**: la referencia de ubicación suma Core Location en el iPhone.
- **Principio IV**: aclara que "un teléfono" quiere decir uno a la vez. Pasar al iPhone es una
  mudanza: el respaldo reemplaza, no sincroniza.

**Por qué MINOR**: expande una guía, no redefine un principio.

## D20 — Orden de trabajo

Cada paso deja el Android andando y validado en el teléfono antes del siguiente:

1. **Repositorio público y compilación en la nube**, con la prueba piloto de D21.
2. **El Android pasa a multiplataforma sin cambiar nada**: sube Kotlin y Compose, se mudan los
   archivos a `:shared`, pasan todas las pruebas y el APK se actualiza sin perder datos.
3. **El mapa compartido, en el Android** (D5, con línea de corte).
4. **El iPhone anota y muestra** (US1).
5. **Reinstalar, vencimiento y respaldo** (US2), y **mudanza** (US3).
6. **Foto, compartir, Google Maps** (US4), **avisos** (US5), **salidas** (US6).

## D21 — Prueba piloto antes de portar

**Decisión**: antes de mover una sola línea del Android, una app mínima de iPhone: una pantalla de
Compose, una región vigilada, ubicación en segundo plano y una notificación local, compilada en la
nube e instalada con Sideloadly. Tiene que confirmar, en el iPhone del jugador y con el Apple ID
gratis:

| Qué | Si falla |
|---|---|
| Se instala y abre | Se revisa la cadena de compilación. No se sigue |
| La versión de iOS es 15.5 o más (lo pide maplibre-compose) | Otra biblioteca de mapa, o el iPhone no sirve |
| Ubicación con la pantalla bloqueada | La US6 queda solo en el Android |
| El aviso de región con la app cerrada, y después de reiniciar | La US5 queda solo en el Android |
| El aviso después de cerrar la app deslizándola | Se documenta en ajustes (D11) |
| La app lee su fecha de vencimiento | FR-010 a FR-012 sin fecha: se avisa por fecha de instalación |
| Reinstalar encima conserva un archivo escrito antes | **Bloqueante**: sin esto el iPhone no sirve (condición del jugador) |

Así, lo que depende de Apple se sabe en un día y no después de portar 6.000 líneas.

**Resultados (2026-09-29)**, con el `.ipa` de la corrida 36583702009 y Sideloadly:

| Qué | Resultado |
|---|---|
| Se instala y abre | Sí, después de un arreglo: el primer `.ipa` se cerraba al abrir con `MissingResourceException`, porque el `.app` no llevaba los recursos de Compose que usa el mapa. Xcode pasó a compilar con `embedAndSignAppleFrameworkForXcode`, que los copia, y la nube falla si faltan |
| Reinstalar encima conserva un archivo escrito antes | **Sí**: la marca escrita en Room sigue después de reinstalar con Sideloadly. La compuerta pasa |
| El mapa de maplibre-compose | Dibuja el centro con el estilo de OpenFreeMap |
| Ubicación con la app abierta | Llega. La primera vez "Vigilar acá" no tenía posición todavía; al rato, sí |
| La versión de iOS | Pendiente |
| La app lee su fecha de vencimiento | Pendiente |
| Ubicación con la pantalla bloqueada | Pendiente |
| El aviso de región con la app cerrada, y después de reiniciar | Pendiente |
| El aviso después de cerrar la app deslizándola | Pendiente |

## D22 — Repositorio público, limpio (FR-005a, SC-011)

**Decisión**:
1. Las coordenadas de las pruebas (`AcomodoTest`, `GeoTest`, `PrioridadTest`, `ColeccionTest`) pasan
   a una zona neutra, el Obelisco, recalculando los esperados que dependan de la latitud.
2. Sale `specs/005-buscar-la-que-toca/antes-grupos.png`, y se ajusta la línea de la 005 que lo
   nombra. Esa línea, la T002 de las tareas de la 005, nombra además las calles de la zona: se
   reescribe sin nombres de calles. Lo mismo con cualquier otro nombre de calle o de barrio que
   aparezca en `specs/`.
3. `git config user.email` del repositorio pasa a la dirección `noreply` de GitHub del jugador.
4. La `main` de hoy se renombra `historial`, queda solo en la PC y no se sube nunca. Una `main`
   nueva nace huérfana (`git checkout --orphan`) desde el árbol limpio, con un primer commit, y es
   la que se sube.
5. Antes de subir: se busca en todo lo que se va a publicar que no quede ninguna coordenada de la
   zona, ninguna imagen del mapa y ningún email personal (SC-011).

**Por qué**: reescribir el historial viejo con `filter-repo` cambia los 33 commits y exige una
herramienta más. Una historia nueva es un comando, y la vieja no se pierde.

**Efecto**: las ramas `001`–`005` quedan en la PC colgando de `historial`. Desde acá se trabaja
sobre la `main` nueva.

## Fuentes

- [Compatibilidad de Compose Multiplatform](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html)
- [maplibre-compose, primeros pasos](https://maplibre.org/maplibre-compose/getting-started/) y
  [repositorio](https://github.com/maplibre/maplibre-compose)
- [Versiones que usa maplibre-compose](https://raw.githubusercontent.com/maplibre/maplibre-compose/main/gradle/libs.versions.toml)
- [maplibre-compose, OfflineManager](https://maplibre.org/maplibre-compose/api/lib/maplibre-compose/org.maplibre.compose.offline/-offline-manager/index.html)
  y [ubicación](https://maplibre.org/maplibre-compose/location/)
- [Kotlin/Native: targets y hosts](https://kotlinlang.org/docs/native-target-support.html) y
  [KSP issue 2267](https://github.com/google/ksp/issues/2267)
- [Preguntas frecuentes de Sideloadly](https://sideloadly.io/faq.html)
- [`UIBackgroundModes` es del Info.plist, no un entitlement](https://developer.apple.com/forums/thread/791736)
- [Compilar un .ipa sin firmar para Sideloadly](https://dev.to/oivoodoo/build-unsigned-ios-ipa-to-install-via-sideloadly-236f)
