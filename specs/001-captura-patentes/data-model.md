# Phase 1 Data Model: Captura de patentes

**Fecha**: 2026-08-28
**Spec**: [spec.md](./spec.md) · **Research**: [research.md](./research.md)

Persistencia local en Room (D5). Las fotos viven como archivos en almacenamiento
privado; la base guarda la ruta.

---

## Registro de captura

La entidad central. Una patente vista y anotada.

| Campo | Tipo | Reglas |
|---|---|---|
| `id` | Long | Clave primaria autogenerada |
| `numero` | Int | 0 a 999. Derivado de `patenteTexto` (FR-010). Indexado: se busca por acá |
| `patenteTexto` | String? | Texto completo de la patente si el jugador lo cargó. Opcional |
| `formato` | Enum | `VIEJO`, `MERCOSUR`, `DESCONOCIDO` |
| `latitud` | Double | **Inmutable** tras la creación (FR-005) |
| `longitud` | Double | **Inmutable** tras la creación (FR-005) |
| `precisionMetros` | Float | Precisión reportada por el proveedor (FR-003). **Inmutable** |
| `precisionDegradada` | Boolean | `true` si `precisionMetros` supera el umbral aceptable (FR-009) |
| `capturadoEn` | Long | Epoch millis del momento de la captura (FR-004). **Inmutable** |
| `estado` | Enum | `PENDIENTE`, `COMPARTIDA` (FR-015) |
| `fotoRuta` | String? | Ruta al archivo en almacenamiento privado. `null` si se guardó con `+` |
| `recorridoId` | Long? | FK a Recorrido si la captura ocurrió durante uno (FR-027) |

### Invariantes

- `latitud`, `longitud`, `precisionMetros` y `capturadoEn` **NUNCA** se actualizan. El
  DAO no expone ningún update que los toque. Es el Principio II hecho esquema.
- `numero` se deriva de `patenteTexto` en el momento de crear, o se ingresa directo en
  carga rápida. Nunca queda vacío.
- Un registro se puede crear sin foto y recibirla después (FR-019). Ese es el **único**
  campo que un update puede tocar, junto con `estado`.

### Transiciones de estado

```
PENDIENTE ──compartir──> COMPARTIDA
```

Sin vuelta atrás automática. `COMPARTIDA` deja de generar avisos (FR-029).

### Operaciones permitidas

| Operación | Toca |
|---|---|
| `insertar` | Todos los campos, una vez |
| `adjuntarFoto(id, ruta)` | Solo `fotoRuta` |
| `marcarCompartida(id)` | Solo `estado` |

No hay `update` general. No hay `editarUbicacion`. Su ausencia es el requisito.

---

## Estado del juego

Una sola fila. En qué número va el grupo (FR-020).

| Campo | Tipo | Reglas |
|---|---|---|
| `id` | Int | Constante `1`. Fila única |
| `numeroActual` | Int | 0 a 999. Editable a mano por el jugador |
| `avisosActivos` | Boolean | FR-039: el jugador puede apagar los avisos |

### Efecto de cambiar `numeroActual`

Escribir este campo **dispara reconciliación de geofences** (D4): se desregistran los
del número viejo y se registran los de los registros pendientes del número nuevo. Es el
único punto del sistema donde el conjunto de geofences cambia por decisión del usuario.

---

## Recorrido

Una salida a buscar patentes (FR-023).

| Campo | Tipo | Reglas |
|---|---|---|
| `id` | Long | Clave primaria autogenerada |
| `iniciadoEn` | Long | Epoch millis |
| `finalizadoEn` | Long? | `null` mientras está en curso |
| `estado` | Enum | `EN_CURSO`, `TERMINADO` |

### Invariantes

- Como máximo **un** recorrido en `EN_CURSO` a la vez.
- Un recorrido interrumpido por el sistema operativo conserva sus puntos (FR-033). Al
  reabrir la app, un recorrido `EN_CURSO` cuyo servicio ya no existe se cierra y pasa a
  `TERMINADO` con los puntos que alcanzó a juntar.

---

## Punto de trayecto

Una posición registrada durante un recorrido, con muestreo espaciado (FR-024).

| Campo | Tipo | Reglas |
|---|---|---|
| `id` | Long | Clave primaria autogenerada |
| `recorridoId` | Long | FK a Recorrido. Indexado |
| `latitud` | Double | |
| `longitud` | Double | |
| `precisionMetros` | Float | Menor exigencia que un registro de captura (FR-025) |
| `registradoEn` | Long | Epoch millis |

### Volumen

Con muestreo cada 30 s o 50 m, un recorrido de 2 horas son unos 240 puntos, del orden
de 8 KB. Cien recorridos no llegan a 1 MB. Por eso no hay limpieza automática: el
Principio IV la prohíbe hasta que un problema medido aparezca.

---

## Relaciones

```
EstadoDelJuego (1 fila)
        │
        │ numeroActual determina qué registros generan geofence
        ▼
RegistroDeCaptura ──── 0..1 ────> Foto (archivo en disco, referenciado por fotoRuta)
        │
        │ recorridoId (opcional)
        ▼
   Recorrido ──── 1..N ────> PuntoDeTrayecto
```

Una captura puede existir sin recorrido: la carga rápida en la vereda es el caso normal
y no requiere haber tocado "Empezar recorrido".

---

## Derivados, no almacenados

Estas tres cosas se calculan; no son columnas. Cada una es una función pura sobre los
datos de arriba, y cada una es una de las pruebas que exige la constitución (D8).

| Derivado | Entrada | Requisito |
|---|---|---|
| **Números consecutivos cubiertos** | Registros pendientes + `numeroActual` | FR-022 |
| **Conjunto de geofences a registrar** | Registros pendientes con `numero == numeroActual` | D4 |
| **Calles exploradas acumuladas** | Todos los puntos de trayecto de todos los recorridos | FR-032 |

El último merece una nota: "calles exploradas" no se guarda como lista de calles. Se
deriva agregando puntos de trayecto sobre una grilla y pintándola en el mapa. Guardar
nombres de calles requeriría geocodificación inversa contra la red, lo que rompería el
Principio III y agregaría una dependencia externa que ningún requisito pide.

---

## Reglas de validación

| Regla | Requisito |
|---|---|
| `numero` entre 0 y 999 | Assumptions de la spec |
| No se acepta captura sin lectura de ubicación | FR-002, Principio II |
| Precisión sobre el umbral marca `precisionDegradada`, no rechaza | FR-009 |
| Insert fallido propaga error visible, nunca éxito silencioso | FR-008, Principio III |
| Un solo recorrido `EN_CURSO` | Invariante de Recorrido |
| Geofences activos = registros pendientes del número actual | D4, FR-028, FR-029 |
