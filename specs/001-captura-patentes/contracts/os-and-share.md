# Contracts: superficie externa de la app

**Fecha**: 2026-08-28
**Spec**: [../spec.md](../spec.md) · **Research**: [../research.md](../research.md)

La app no expone API ni red: **FR-013** dice sin cuentas y sin servidor propio. Su
superficie externa son cuatro contratos con el sistema operativo y con otras apps del
teléfono. Cada uno es un lugar donde algo fuera de nuestro control puede cambiar de
comportamiento, y por eso están documentados acá.

---

## C1 — Salida: compartir un registro hacia otra app

**Requisito**: FR-012. **Consumidor**: WhatsApp y cualquier app que acepte el intent.

`ACTION_SEND`, elegido con `createChooser` para que el jugador elija destino.

| Caso | MIME | Extras |
|---|---|---|
| Registro con foto | `image/jpeg` | `EXTRA_STREAM` con URI de la foto vía FileProvider, `EXTRA_TEXT` con la línea de texto |
| Registro sin foto | `text/plain` | `EXTRA_TEXT` con la línea de texto |

**Línea de texto**: el número de patente. Es lo que el juego pide y nada más. No se
adjunta ubicación ni timestamp al mensaje: son evidencia privada del jugador, no parte
de la jugada.

**FileProvider**: la foto vive en almacenamiento privado (D5), así que compartirla exige
un `content://` URI con permiso de lectura temporal. Un `file://` lanza
`FileUriExposedException` desde Android 7.

**Efecto de vuelta**: compartir marca el registro como `COMPARTIDA` (FR-015), lo que lo
saca del conjunto de geofences (FR-029). El cambio de estado ocurre al lanzar el
chooser, no al confirmar el envío: Android no informa si el usuario completó el envío.
Es una imprecisión aceptada y consciente.

---

## C2 — Geofencing: aviso de proximidad con la app cerrada

**Requisito**: FR-028, FR-029, FR-039, FR-041. **Proveedor**: `GeofencingClient` de
Google Play Services.

**Contrato de registro**:

| Parámetro | Valor | Origen |
|---|---|---|
| Radio | 150 m | FR-028 |
| Transición | `GEOFENCE_TRANSITION_ENTER` | Avisar al llegar, no al salir |
| Vencimiento | `NEVER_EXPIRE` | El conjunto se gestiona por contador, no por tiempo |
| `initialTrigger` | `INITIAL_TRIGGER_ENTER` | Avisar si ya está adentro al registrar |
| Identificador | `id` del registro de captura | Permite resolver el registro en el callback |

**Conjunto activo**: registros con `estado == PENDIENTE` y `numero == numeroActual`
(D4). Se reconcilia cuando cambia `numeroActual`, cuando se crea un registro de ese
número, y cuando uno se comparte.

**Límite del sistema**: 100 geofences por app. El conjunto activo es de unos pocos, así
que el límite no se alcanza. Aun así, la reconciliación MUST truncar de forma
determinista si alguna vez se superara, en lugar de fallar en silencio.

**Recepción**: `BroadcastReceiver` registrado en el manifiesto, no en código. Un receiver
registrado en código muere con el proceso y rompería FR-039.

**Deduplicación**: FR-041 dice un aviso por registro por salida. El receiver consulta la
marca de último aviso antes de notificar.

**Pérdidas conocidas del sistema, no bugs de la app**: los geofences se borran al
reiniciar el teléfono y al desactivar la ubicación. La app MUST volver a registrarlos al
arrancar y ante `BOOT_COMPLETED`. Sin eso el aviso deja de llegar sin ningún error
visible.

---

## C3 — Foreground service: registro de trayecto

**Requisito**: FR-024, FR-025, FR-026, FR-033. **Proveedor**: `FusedLocationProviderClient`.

| Parámetro | Valor | Origen |
|---|---|---|
| Prioridad | `PRIORITY_BALANCED_POWER_ACCURACY` | FR-025, SC-010 |
| Intervalo | 30 000 ms | FR-024 |
| Distancia mínima | 50 m | D3: a 40 km/h el intervalo puro saltea cuadras |
| Tipo de servicio | `foregroundServiceType="location"` | Obligatorio desde Android 14 |

**Notificación persistente**: obligatoria por el sistema, y además satisface FR-023
(mostrar de forma visible que hay un recorrido en curso). Un requisito y una imposición
que coinciden.

**Ciclo de vida**: el servicio escribe cada punto a Room apenas lo recibe, no acumula en
memoria. Si el sistema mata el proceso, lo ya escrito sobrevive, que es exactamente lo
que pide FR-033.

**Al reabrir la app**: un recorrido `EN_CURSO` sin servicio vivo se cierra como
`TERMINADO` conservando sus puntos.

---

## C4 — Permisos

Tres niveles, pedidos en momentos distintos. Pedirlos todos al arrancar es la forma más
rápida de que el jugador los niegue todos.

| Permiso | Cuándo se pide | Si se niega |
|---|---|---|
| `ACCESS_FINE_LOCATION` | Antes de la primera captura (FR-014) | La captura no puede cumplir el Principio II: se bloquea guardar y se explica por qué |
| `ACCESS_BACKGROUND_LOCATION` | Al activar avisos o iniciar el primer recorrido (FR-030) | Avisos y recorridos quedan apagados; **el resto de la app funciona igual** |
| `POST_NOTIFICATIONS` | Junto con los avisos, Android 13+ | Sin notificación no hay aviso: se explica y se ofrece abrir ajustes |
| `CAMERA` | Al primer uso del botón de cámara | Carga rápida con `+` sigue funcionando (FR-019) |

**Regla transversal**: negar un permiso degrada una función, nunca rompe la app. Es
FR-030 generalizado, y es lo que sostiene que la User Story 1 sea independiente de las
demás.

---

## C5 — Tiles de mapa

**Requisito**: FR-034, FR-042 a FR-045. **Proveedor**: OpenFreeMap sobre MapLibre Native.

| Aspecto | Contrato |
|---|---|
| Autenticación | Ninguna. Sin API key, sin cuenta (Principio IV) |
| Estilo | URL configurable en **un solo lugar** del código |
| Caché | Ambiente de MapLibre, automático sobre lo que se muestra (FR-042) |
| Tamaño máximo | `setMaximumAmbientCacheSize`, desalojo LRU (FR-044) |
| Borrado | `clearAmbientCache` (FR-045) |

**Dependencia de terceros declarada**: OpenFreeMap es gratuito y operado por un tercero.
Si deja de estar disponible, la app pierde el fondo de mapa pero **NO** pierde la
captura: FR-038 exige que el estado del mapa nunca bloquee guardar. Esa es la mitigación
real, y ya está en la spec.
