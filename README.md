# Buscar patentes

App personal para Android y iPhone para el juego de patentes en secuencia. Se anota una patente
vista en la calle con el mínimo roce posible —abrir, tipear tres dígitos, tocar `+`— y la app
resuelve sola la ubicación, la precisión y el momento. Después ayuda a encontrar la que toca:
el mapa con las anotadas, la barra con la lista, el recorrido a pie por varias y las salidas
grabadas.

Todo local: sin cuentas, sin servidor propio, sin red en el camino de captura. Los dos teléfonos
corren el mismo código de Kotlin Multiplatform y Compose Multiplatform.

- Qué hace y por qué: [`specs/001-captura-patentes/spec.md`](specs/001-captura-patentes/spec.md)
- Cómo está construido: [`specs/001-captura-patentes/plan.md`](specs/001-captura-patentes/plan.md)
- El pulido de interfaz: [`specs/002-pulido-de-interfaz/spec.md`](specs/002-pulido-de-interfaz/spec.md)
- Rumbo y aspecto, lo que destapó el primer uso real: [`specs/003-rumbo-y-aspecto/spec.md`](specs/003-rumbo-y-aspecto/spec.md)
- Buscar la que toca, la barra y el recorrido por varias: [`specs/005-buscar-la-que-toca/spec.md`](specs/005-buscar-la-que-toca/spec.md)
- Jugar en el iPhone: [`specs/006-jugar-en-iphone/spec.md`](specs/006-jugar-en-iphone/spec.md), y
  cómo se prueba en [`quickstart.md`](specs/006-jugar-en-iphone/quickstart.md)
- Reglas que no se negocian: [`.specify/memory/constitution.md`](.specify/memory/constitution.md)

## Estructura

| Módulo | Qué tiene |
|---|---|
| `shared/src/commonMain` | Todo lo que es igual en los dos: dominio, base (Room), pantallas, mapa y respaldo |
| `shared/src/androidMain` | Lo del Android: ubicación, el servicio de la salida, permisos, cámara, compartir |
| `shared/src/iosMain` | Lo mismo para el iPhone, sobre Core Location y UIKit |
| `app/` | La app Android: `App`, `MainActivity` y el manifiesto |
| `iosApp/` | La app del iPhone: el `AppDelegate` en Swift y el `project.yml` de XcodeGen |

## Android

### Requisitos

- JDK 17 o superior (la compilación apunta a bytecode 17)
- SDK de Android con `platforms/android-37`, vía Android Studio o `cmdline-tools`
- `local.properties` con `sdk.dir` apuntando al SDK
- Para la salida con la pantalla apagada y el consumo de batería, un **teléfono físico** con
  Google Play Services y Android 8.0 o superior

### Compilar, instalar, probar

```bash
./gradlew assembleDebug                    # APK en app/build/outputs/apk/debug/
./gradlew installDebug                     # instala en el dispositivo conectado
./gradlew :shared:testAndroidHostTest      # las pruebas, en la JVM, sin dispositivo
```

En Windows, `gradlew.bat` en lugar de `./gradlew`.

## iPhone

Sin Mac: la app se compila en GitHub Actions y se instala desde Windows con
[Sideloadly](https://sideloadly.io) y un Apple ID gratis.

### Una sola vez

1. **iTunes de la web, no el de la Microsoft Store.** Sideloadly lo pide, y es también el que
   muestra los archivos compartidos de la app, que es por donde viajan los respaldos.
2. **Sideloadly.**
3. **iOS 15.5 o más.** Con iOS 16 o más, activar el modo de desarrollador: Ajustes → Privacidad y
   seguridad → Modo de desarrollador.
4. **Siempre el mismo Apple ID.** Con otro, el iPhone la trata como otra app y no conserva los
   datos.

### Compilar e instalar

```powershell
gh workflow run ios.yml --ref <rama>
gh run watch
gh run download --name buscar-patentes-ipa --dir $env:USERPROFILE\Downloads\buscar-patentes-ios
```

En Sideloadly: el iPhone enchufado, el `.ipa`, el Apple ID y **Start**. La primera vez, en el
iPhone: Ajustes → General → VPN y gestión de dispositivos → confiar en el perfil. La nube no
guarda ninguna credencial de Apple: el `.ipa` sale sin firmar y lo firma Sideloadly.

### Cada semana

Con un Apple ID gratis, la instalación dura 7 días. La fecha está en Ajustes de la app. Dos días
antes aparece una línea en la pantalla principal, y el día anterior llega una notificación.
Reinstalar el mismo `.ipa` o uno nuevo, con el mismo Apple ID, conserva todos los datos.

## Respaldo

En Ajustes → **Respaldo**, igual en los dos teléfonos:

- **Sacar respaldo** arma un solo archivo `buscar-patentes-AAAAMMDD-HHMM.respaldo` con todas las
  patentes, salidas, votos y fotos, y abre la hoja de compartir para mandarlo afuera.
- **Restaurar respaldo** lo valida y **reemplaza** todo lo del teléfono, sin mezclar. Si el
  teléfono tenía patentes, antes pide confirmación y guarda un respaldo automático de lo que había.

Es también la forma de pasar todo del Android al iPhone y al revés. En el iPhone los respaldos
quedan en Archivos → En mi iPhone → Buscar patentes, y en iTunes, en los archivos compartidos de la
app. El formato está en [`contracts/respaldo.md`](specs/006-jugar-en-iphone/contracts/respaldo.md).

`scripts/respaldo.ps1` queda para el Android de desarrollo: copia la base por `adb` antes de una
reinstalación que borre los datos.

```powershell
./scripts/respaldo.ps1 respaldar      # a respaldos/<fecha-hora>/
./scripts/respaldo.ps1 restaurar      # de vuelta al teléfono
```

Verificar que el archivo `-wal` de ese respaldo pesa cientos de KB: la base sola puede venir casi
vacía porque los datos todavía están en el write-ahead log.

## Qué cubren las pruebas automáticas

Las pruebas comunes viven en `shared/src/commonTest` y corren en dos lados: en la JVM de la PC, con
`:shared:testAndroidHostTest`, y en el simulador del iPhone, en la nube, antes de cada `.ipa`.
`InmutabilidadTest` lee los fuentes, así que corre solo en la JVM (`androidHostTest`).

| Prueba | Qué protege |
|---|---|
| `PatenteTest` | Extracción del número de 3 dígitos, formato viejo y Mercosur (FR-010) |
| `CoberturaTest` | El texto de descubiertas del número actual (FR-003 de la 005) |
| `InmutabilidadTest` | Que **no exista** ninguna forma de editar la evidencia de un registro (Principio II) |
| `ColeccionTest` | El GeoJSON de los marcadores y de los recorridos: un feature por registro, la que toca en su propia colección, los pines corridos y los grupos; un `LineString` por tramo, el hueco punteado de cada desconexión y el escalón de antigüedad de cada salida (FR-010, D1 y D13 de la 005; FR-006, FR-009a, FR-031) |
| `GeoTest` | Distancia y rumbo hasta una patente, los bordes del punto cardinal y el corte del trazo donde se cortó la señal (FR-003, FR-009) |
| `PolilineaTest` | El decodificador de la polilínea que devuelve el servicio de ajuste a calles (FR-031) |
| `AjustarACallesTest` | El pedido al servicio de ajuste a calles: el radio de cada punto sale de su precisión, con piso y techo |
| `ProbabilidadTest` | La cuenta de los votos sobre si una patente sigue estando (FR-037), y qué patentes conservan sus votos al migrar a la v7 |
| `PrioridadTest` | El orden de la lista de la barra y el orden de caminar las paradas de un recorrido (FR-012, FR-015 de la 005) |
| `IrTest` | Qué hace "Abrir en Google Maps" según cuántas paradas quedaron, y la URL con punto decimal (FR-019 de la 005) |
| `AcomodoTest` | Los pines que se hacen lugar antes de agruparse (FR-023 de la 005) |
| `AntiguedadTest` | Los escalones de hace cuánto se caminó una calle, con los bordes en 3 y 14 días (FR-014, FR-019) |
| `FormatosTest` | Fechas y números iguales en los dos teléfonos, sin depender de cómo cada sistema los escribe (D16 de la 006) |
| `AlmacenFotosTest` | Que la foto se busque por su nombre, así una ruta del Android sirve en el iPhone (D7 de la 006) |
| `RespaldoTest` | El respaldo: sale con sus cuentas sin tocar la base, vuelve idéntico bit por bit, rechaza lo que no es un respaldo con su motivo y deshace un cambio que falla a mitad (contrato R de la 006) |
| `VencimientoTest` | La fecha de vencimiento del perfil del iPhone, y el aviso con 48 horas o menos (D15 de la 006) |

El SQLite que usa `RespaldoTest` en la PC es la biblioteca de escritorio de `sqlite-bundled`: la
tarea `nativoDeSqlite` de `shared/build.gradle.kts` la extrae y se la pasa a la JVM.

Todo lo demás —la salida con la pantalla apagada, el consumo de batería, la carga rápida con
cronómetro, el mapa al sol— se valida caminando, y está escrito paso a paso en el quickstart de
cada feature: [`001`](specs/001-captura-patentes/quickstart.md),
[`002`](specs/002-pulido-de-interfaz/quickstart.md),
[`003`](specs/003-rumbo-y-aspecto/quickstart.md),
[`004`](specs/004-planear-recorridos/quickstart.md),
[`005`](specs/005-buscar-la-que-toca/quickstart.md) y
[`006`](specs/006-jugar-en-iphone/quickstart.md).

## Mapa

El fondo usa [OpenFreeMap](https://openfreemap.org), un servicio gratuito operado por un
tercero: `liberty` con el teléfono en modo claro y `fiord` en oscuro, con las avenidas resaltadas
encima. Las URL viven en un solo lugar, `ConfigMapa.estiloUrl` en
`shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/mapa/MapaDeFondo.kt`: si el servicio
desaparece se cambia esa función y nada más. La captura de patentes nunca depende de que el mapa
cargue.
