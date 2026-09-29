# Contrato R — El archivo de respaldo

Es el único formato que cruza de un teléfono al otro. Lo escribe y lo lee el mismo código de
`commonMain`, en el Android y en el iPhone (FR-015). Las decisiones que lo explican están en
[research.md](../research.md), D8.

## R1 — Qué es

- **Un archivo SQLite 3**, con extensión `.respaldo`.
- **Nombre**: `buscar-patentes-AAAAMMDD-HHMM.respaldo`, con la hora local en que se sacó. El
  automático, el que se saca antes de restaurar, lleva un sufijo: `…-antes-de-restaurar.respaldo`.
- **Contenido**: una copia exacta de la base de la app, hecha con `VACUUM INTO`, más dos tablas
  propias del respaldo.

## R2 — Tablas

| Tabla | Viene de | Qué tiene |
|---|---|---|
| `registro_de_captura` | la base | Tal cual. Ningún valor se convierte |
| `estado_del_juego` | la base | Tal cual: número actual, avisos activos, modo del mapa |
| `recorrido` | la base | Tal cual, con `caminoAjustado` |
| `punto_de_trayecto` | la base | Tal cual |
| `voto` | la base | Tal cual |
| `room_master_table` | la base | El identity hash del esquema |
| `respaldo_info` | se agrega | `clave TEXT PRIMARY KEY, valor TEXT NOT NULL` |
| `respaldo_foto` | se agrega | `nombre TEXT PRIMARY KEY, datos BLOB NOT NULL` |

`PRAGMA user_version` es la versión de la base (hoy 7): lo fija Room y lo copia `VACUUM INTO`.

### `respaldo_info`

| Clave | Valor | Ejemplo |
|---|---|---|
| `formato` | Versión de **este contrato**. Sube solo si cambian R2 o R3 | `1` |
| `sacadoEn` | Epoch millis | `1790000000000` |
| `origen` | `android` o `ios` | `android` |
| `patentes` | Filas de `registro_de_captura` | `87` |
| `salidas` | Filas de `recorrido` | `12` |
| `fotos` | Filas de `respaldo_foto` | `41` |
| `fotosFaltantes` | Registros con `fotoRuta` cuya foto no estaba en el teléfono al respaldar | `0` |
| `ultimaCaptura` | `capturadoEn` máximo en epoch millis, o vacío si no hay patentes | `1789990000000` |

Las cuentas están para mostrarlas **antes** de restaurar (FR-016) sin leer las tablas.

### `respaldo_foto`

- `nombre` es el nombre de archivo solo, sin carpeta: `1789990000000.jpg`.
- Hay una fila por cada foto que algún registro nombra en `fotoRuta` y que existe en el teléfono.
- `datos` son los bytes del JPEG, sin tocar.

## R3 — Sacar un respaldo

1. `VACUUM INTO '<temporal>'` sobre la conexión de escritura de Room. Así la copia es consistente
   aunque haya una salida grabando, porque incluye lo que todavía está en el WAL.
2. Abrir la copia con `BundledSQLiteDriver`, crear las dos tablas y llenarlas.
3. Cerrar la copia y moverla a su destino:
   - **iPhone**: `Documents/respaldos/`.
   - **Android**: la carpeta `respaldos` de la app (`filesDir/respaldos`), y desde ahí a la hoja de
     compartir.
4. Guardar `ultimoRespaldoEn` en las preferencias (FR-020). El respaldo automático también cuenta.

Si falla cualquier paso, se borra el temporal y se muestra el error. **La base no se toca nunca**
para sacar un respaldo.

## R4 — Validar antes de restaurar

Se abre el archivo elegido, **una copia**, con `BundledSQLiteDriver`. Cualquier "no" corta el
proceso, con su mensaje, y sin tocar nada (FR-018):

| Chequeo | Si falla, la app dice |
|---|---|
| Abre como SQLite | "Ese archivo no es un respaldo de la app." |
| Tiene `respaldo_info` con `formato` | "Ese archivo no es un respaldo de la app." |
| `formato` ≤ 1 | "Ese respaldo es de una versión más nueva de la app. Actualizala primero." |
| `user_version` ≤ la versión de la base de esta app | Igual que el anterior |
| `PRAGMA integrity_check` da `ok` | "El respaldo está dañado." |
| Están las cinco tablas de la base | "El respaldo está dañado." |

## R5 — Confirmar

Si el teléfono tiene cero patentes, se restaura sin preguntar: no hay nada que perder.

Si tiene alguna, se muestra la comparación y se pide confirmación (FR-016):

> **En este teléfono**: 12 patentes, la última del 3 de octubre.
> **En el respaldo**: 87 patentes, la última del 28 de septiembre.
> Restaurar reemplaza todo lo de este teléfono. Antes se guarda un respaldo de lo que hay.
> [Cancelar] [Restaurar]

## R6 — Restaurar

1. **Si el teléfono tiene patentes**: se saca el respaldo automático (R3). Si falla, no se sigue.
2. **Se prepara todo en una carpeta aparte**, sin tocar la base:
   - se extraen las fotos de `respaldo_foto` a `preparado/fotos/`;
   - se borran `respaldo_info` y `respaldo_foto` de la copia y se hace `VACUUM`.
3. **El cambio**, que es lo único que toca los datos:
   - se cierra Room;
   - se renombran la base a `.anterior` y la carpeta de fotos a `fotos.anterior`;
   - se mueven `preparado/` a su lugar y se borran los `-wal` y `-shm` viejos.
   Si falla a mitad, se deshacen los renombres.
4. **Se vuelve a abrir Room.** Si el respaldo era de una versión vieja, Room lo migra en ese
   momento. Si la apertura falla, se deshace el paso 3.
5. Se borran `.anterior` y `fotos.anterior`.
6. Se recalcula lo que depende de los datos: los avisos vigilados y la pantalla principal.
7. **Resultado visible**: "Listo: 87 patentes, 12 salidas, 41 fotos." Y si faltaron fotos:
   "Faltaron 2 fotos: esas patentes quedan sin foto." (FR-019)

**Lo que no se toca**: `fotoRuta` queda como vino. La foto se busca por nombre (D7), así que una
ruta del Android sirve en el iPhone. Si el nombre no está en la carpeta, el registro se ve sin foto.

## R7 — Lo que el respaldo no lleva

- Qué avisos ya se dieron en la salida en curso. Es de la salida, no del juego.
- Las regiones o geofences registradas. Se reconstruyen desde los datos (R6, paso 6).
- La caché de teselas del mapa.
- La fecha del último respaldo.

## R8 — Pruebas (`RespaldoTest`, en la JVM)

- **Ida y vuelta**: una base con registros de coordenadas con muchos decimales, precisión
  fraccionaria, degradados, fotos, salidas con camino ajustado y votos. Se respalda, se restaura en
  una base vacía, y cada columna de cada fila tiene que ser **idéntica**, comparando los bits de
  cada `REAL`.
- **Rechazos**: un archivo de texto, un SQLite sin `respaldo_info`, `formato` 2, `user_version` 99
  y un archivo truncado. Todos se rechazan con su motivo, y la base de destino queda como estaba.
- **Foto faltante**: un registro con `fotoRuta` sin archivo. El respaldo cuenta 1 en
  `fotosFaltantes`, y al restaurar el registro llega igual.
- **Ruta de otro teléfono**: un `fotoRuta` con ruta del Android resuelve a la carpeta de fotos
  local (D7).
