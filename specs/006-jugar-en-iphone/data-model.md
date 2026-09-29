# Data Model: Jugar en el iPhone

**La base no cambia.** Sigue en la v7 del 2026-09-28: ni tablas, ni columnas, ni migración. Lo que
cambia es **dónde vive** cada cosa, y aparecen dos que no son de la base: el archivo de respaldo y
la fecha de vencimiento.

## Entidades que ya existen

Se mudan de `app/src/main/java/.../data/` a `shared/src/commonMain/kotlin/.../data/`, **con el
mismo paquete, los mismos nombres de tabla y columna y los mismos tipos**. El identity hash de Room
no cambia, y por eso el Android abre su base de siempre (D6).

| Entidad | Tabla | Cambia |
|---|---|---|
| `RegistroDeCaptura` | `registro_de_captura` | Nada en la tabla. **`fotoRuta` se interpreta distinto** (abajo) |
| `EstadoDelJuego` | `estado_del_juego` | Nada |
| `Recorrido` | `recorrido` | Nada |
| `PuntoDeTrayecto` | `punto_de_trayecto` | Nada |
| `Voto` | `voto` | Nada |

### `fotoRuta`: de ruta a nombre

| | Antes | Ahora |
|---|---|---|
| Qué se guarda | La ruta absoluta del Android | Lo mismo en el Android. En el iPhone, la ruta local |
| Cómo se busca la foto | `File(fotoRuta)` | **`Carpetas.fotos` + el nombre del archivo** de `fotoRuta` |
| Migración | — | Ninguna. Ningún valor guardado se reescribe |

**Por qué**: la ruta del Android no existe en el iPhone, y la del iPhone puede cambiar en la
próxima reinstalación (D7). El nombre sale del momento de la captura (`<capturadoEn>.jpg`) y es el
mismo en los dos teléfonos.

**Principio II**: `fotoRuta` no es evidencia. Ubicación, precisión y hora no se tocan. Aun así no
se reescribe: interpretarla distinto alcanza.

## Dónde vive cada cosa

| Qué | Android | iPhone | Sobrevive a una reinstalación |
|---|---|---|---|
| La base | `databases/buscar-patentes.db` | `Library/Application Support/buscar-patentes.db` | Sí, en los dos (FR-007) |
| Las fotos | `files/fotos/` | `Library/Application Support/fotos/` | Sí |
| Respaldos que saca la app | `files/respaldos/` (se quedan; el sistema no borra el respaldo previo a restaurar) | `Documents/respaldos/` (se quedan, a la vista de Archivos y de iTunes) | Sí |
| Avisos dados en la salida en curso | `SharedPreferences` `avisos` | `NSUserDefaults`, las mismas claves | Sí |
| Lugares vigilados | `SharedPreferences` `geofences` + Play Services | Core Location (los conserva el sistema) | Sí |
| `ultimoRespaldoEn` (**nuevo**) | `SharedPreferences` `respaldo` | `NSUserDefaults` | Sí |

**Una reinstalación en el iPhone con el mismo Apple ID y el mismo identificador conserva el
contenedor de la app entero** (`Library`, `Documents`). Solo lo borra desinstalar la app. Es la
suposición que la prueba piloto verifica primero (D21).

## Nuevo: el respaldo

Un archivo, no una tabla de la app. El formato está en
[contracts/respaldo.md](./contracts/respaldo.md).

| Atributo | De dónde sale |
|---|---|
| Formato | `respaldo_info.formato`, hoy `1` |
| Versión de la base | `PRAGMA user_version`, hoy `7` |
| Cuándo se sacó | `respaldo_info.sacadoEn` |
| De qué teléfono | `respaldo_info.origen`: `android` o `ios` |
| Cuentas | `patentes`, `salidas`, `fotos`, `fotosFaltantes`, `ultimaCaptura` |

### Validación

| Regla | Requisito |
|---|---|
| Abre como SQLite y tiene `respaldo_info` | FR-018 |
| `formato` ≤ el que entiende la app | FR-018 |
| `user_version` ≤ la versión de la base de la app | FR-018 |
| `integrity_check` = `ok` y están las cinco tablas | FR-018 |
| Cada registro llega con los mismos bits en `latitud`, `longitud`, `precisionMetros` y `capturadoEn` | FR-014, Principio II |

### Estados de una restauración

```text
Elegido ──validar──▶ Válido ──(teléfono con patentes)──▶ Confirmando ──sí──▶ Respaldando lo actual
   │                   │                                    │                      │
   │ no válido         │ teléfono vacío                     │ cancelar             ▼
   ▼                   └───────────────────────────────────────────────────▶ Preparando
Rechazado (mensaje,                                                               │
 nada cambió)                                                                     ▼
                                                                          Intercambiando ──falla──▶ Deshecho
                                                                                  │                (nada cambió)
                                                                                  ▼
                                                                            Reabriendo ──falla──▶ Deshecho
                                                                                  │
                                                                                  ▼
                                                                    Listo (cuentas, fotos faltantes)
```

- **Rechazado** y **Deshecho** dejan la base y las fotos **exactamente como estaban** (FR-018).
- **Cancelar** en Confirmando no deja nada: todavía no se escribió ni el respaldo automático.
- Si falla **Respaldando lo actual**, no se sigue: sin respaldo previo no se reemplaza nada
  (FR-016).

## Nuevo: el vencimiento de la instalación

**No se guarda.** Se lee de `embedded.mobileprovision` cada vez que la app abre (D15).

| Atributo | Regla |
|---|---|
| `vence: Instant?` | `null` en el Android y en un `.ipa` sin firmar. Así nunca se muestra nada |
| Se muestra en ajustes | siempre que no sea `null` (FR-010) |
| Se muestra en la pantalla principal | `vence - ahora ≤ 48 h` (FR-011) |
| Notificación | programada para `vence - 24 h` en cada apertura, con identificador fijo (FR-012) |
| Ya vencida | no se puede mostrar: la app no abre. Por eso existen los dos avisos de antes |

## Cambio de firma: `Avisos.aRegistrar`

```text
antes:  aRegistrar(registros, numeroActual)          // recorta a LIMITE_GEOFENCES = 100
ahora:  aRegistrar(registros, numeroActual, limite)  // 100 en el Android, 20 en el iPhone
```

El criterio no cambia: solo las patentes no compartidas del número actual, ordenadas por id, hasta
el límite (FR-025). `AvisosTest` suma el caso con límite 20.
