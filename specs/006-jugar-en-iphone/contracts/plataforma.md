# Contrato P — La costura con cada sistema

Todo lo que es de un sistema se declara una vez en `commonMain` y tiene exactamente dos
implementaciones: `androidMain` e `iosMain` (D9). Una pantalla de Compose no importa nada de
`android.*` ni de `platform.*`. Si necesita algo del sistema, pasa por esta lista.

La columna **Android** dice de dónde sale hoy el código: en casi todos los casos se muda, no se
reescribe.

## P1 — Arranque y carpetas

| Declaración | Android | iPhone |
|---|---|---|
| `Plataforma.iniciar(...)` | `App.onCreate`, con el `applicationContext` | `AppDelegate.didFinishLaunching`, **antes** de la UI (D11) |
| `Carpetas.base` | `getDatabasePath` | `Library/Application Support/` |
| `Carpetas.fotos` | `filesDir/fotos` | `Library/Application Support/fotos` |
| `Carpetas.respaldos` | `filesDir/respaldos`. No la caché: ahí queda el respaldo previo a restaurar, y la caché la vacía el sistema | `Documents/respaldos` (visible en Archivos y en iTunes) |
| `Carpetas.temporal` | `cacheDir` | `NSTemporaryDirectory()` |
| `construirBase()` | `Room.databaseBuilder` + migraciones 1→7 | `Room.databaseBuilder` + `BundledSQLiteDriver` |

## P2 — Ubicación

| Declaración | Contrato | Android | iPhone |
|---|---|---|---|
| `suspend fun leerUbicacion(): Lectura?` | Una lectura de alta precisión, con tiempo de espera. `null` si no hubo ninguna | `LectorUbicacion` | `CLLocationManager.requestLocation()` |
| `fun ubicacionExacta(): Boolean` | `false` solo si el usuario apagó la ubicación exacta | siempre `true` | `accuracyAuthorization` (FR-024) |

`Lectura` es común: latitud, longitud, precisión en metros y epoch millis del proveedor.

## P3 — Salida con la pantalla apagada

| Declaración | Contrato | Android | iPhone |
|---|---|---|---|
| `Grabacion.iniciar(recorridoId)` | Empieza a guardar puntos en `punto_de_trayecto` | `ServicioRecorrido` | `startUpdatingLocation` en segundo plano (D12) |
| `Grabacion.terminar()` | Deja de guardar | ídem | `stopUpdatingLocation` |
| `Grabacion.activa: Boolean` | `true` si **este proceso** está grabando | ídem | ídem |

Los puntos usan el mismo filtro en los dos: 10 metros como mínimo entre uno y otro
(`DISTANCIA_MINIMA_M`).

## P4 — Avisos

| Declaración | Contrato | Android | iPhone |
|---|---|---|---|
| `Vigilancia.limite: Int` | Lugares que el sistema deja vigilar | 100 | 20 |
| `Vigilancia.reconciliar(deseados)` | Deja vigiladas exactamente esas patentes | `Geofences` | `startMonitoringForRegion` / `stopMonitoring…` |
| Evento de entrada | Aplica `Avisos.corresponde(...)` y la deduplicación, y notifica | `GeofenceReceiver` | delegado del `CLLocationManager` + `UNUserNotificationCenter` |
| Tocar la notificación | Abre la app con el id del registro y centra el mapa | `PendingIntent` | `userInfo` → callback común |

`Avisos.aRegistrar(registros, numeroActual, limite)` recibe el límite como parámetro (D11). Es la
misma función pura en los dos, con su prueba.

## P5 — Pantallas del sistema (composables `expect`)

| Declaración | Contrato | Android | iPhone |
|---|---|---|---|
| `rememberSacarFoto(alTerminar)` → `(destino) -> Unit` | Abre la cámara y escribe el JPEG en `destino` | `TakePicture` | `UIImagePickerController` |
| `rememberPedirPermisos(...)` | Pide ubicación, "siempre", cámara o notificaciones, según el caso | `RequestPermission(s)` | Core Location, `AVCaptureDevice`, `UNUserNotificationCenter` (D13) |
| `rememberCompartir()` → `(texto, foto?) -> Unit` | Abre la hoja de compartir del sistema | `ACTION_SEND` + `FileProvider` | `UIActivityViewController` |
| `rememberElegirRespaldo(alElegir)` | Deja elegir un archivo y entrega una copia local | `OpenDocument` | `UIDocumentPickerViewController` |
| `rememberGuardarRespaldo()` → `(ruta) -> Unit` | Ofrece mandar el archivo a otro lado | `ACTION_SEND` | `UIActivityViewController` |

La marca de **compartida** se pone al abrir la hoja, en los dos (D14).

## P6 — Todo lo demás

| Declaración | Contrato | Android | iPhone |
|---|---|---|---|
| `abrirUrl(url)` | Abre la URL; si hay una app que la toma, la abre ahí | `Intent` con Google Maps y el navegador de respaldo (`Ir`) | `UIApplication.openURL` |
| `abrirAjustesDelSistema()` | Los ajustes de la app en el sistema | `ACTION_APPLICATION_DETAILS_SETTINGS` | `UIApplicationOpenSettingsURLString` |
| `suspend fun postear(url, cuerpo): String` | POST de JSON, con 20 s de espera | `HttpURLConnection` | `NSURLSession` |
| `Preferencias` | Clave → `Long`, `String` o conjunto de `String` | `SharedPreferences` | `NSUserDefaults` |
| `perfilDeAprovisionamiento(): ByteArray?` | Los bytes de `embedded.mobileprovision` | siempre `null` | `NSBundle.mainBundle` |
| `programarAvisoDeVencimiento(en)` | Una notificación local en ese momento, que reemplaza la anterior | nada | `UNCalendarNotificationTrigger` |

## P7 — Lo común que usa esta costura

Sin `expect`, y con prueba:

- `Vencimiento.leer(bytes): Instant?` busca `ExpirationDate` en el perfil (D15). Prueba:
  `VencimientoTest`.
- `Vencimiento.mostrarAviso(ahora, vence): Boolean`, que da `true` cuando faltan 48 horas o menos.
  Prueba: `VencimientoTest`, con los bordes.
- `Respaldo.sacar`, `Respaldo.validar` y `Respaldo.restaurar` implementan el contrato R. Prueba:
  `RespaldoTest`.
- `AlmacenFotos.archivo(ruta)` resuelve por nombre de archivo (D7). Prueba: `RespaldoTest`, caso
  de la ruta de otro teléfono.
