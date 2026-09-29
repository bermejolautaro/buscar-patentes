# Phase 1: modelo de datos

Esta feature casi no toca la base. Una columna nueva, una migración aditiva, y todo lo demás son
lecturas derivadas de datos que ya están guardados.

---

## Lo que cambia en la base

### `estado_del_juego` — una columna

| Campo | Tipo | Nulo | Default | Para qué |
|---|---|---|---|---|
| `modoMapa` | TEXT | NO | `'COBERTURA'` | Cuál de las tres preguntas está contestando el mapa (FR-003) |

```kotlin
enum class ModoMapa { COBERTURA, ANTIGUEDAD, APAGADO }
```

Se guarda como texto con los dos convertidores que `Convertidores` ya tiene por patrón, igual
que `EstadoRegistro` y `EstadoRecorrido`.

**Por qué vive acá y no en un almacén aparte**: la tabla ya guarda `avisosActivos`, que es una
preferencia del jugador de exactamente la misma naturaleza. Es una fila única, se lee en el
mismo efecto de la pantalla principal que ya la lee, y no agrega ningún mecanismo de
persistencia nuevo (D6).

**Escritura**: un `@Query` acotado a esa columna, siguiendo la forma de `fijarAvisos`:

```kotlin
@Query("UPDATE estado_del_juego SET modoMapa = :modo WHERE id = :id")
suspend fun fijarModoMapa(modo: String, id: Int = EstadoDelJuego.ID_UNICO)
```

Acotado y no un `@Update` general: es la misma disciplina que sostiene el Principio II en
`RegistroDao`, aplicada por costumbre aunque esta tabla no guarde evidencia.

### Migración v5 → v6

```sql
ALTER TABLE estado_del_juego ADD COLUMN modoMapa TEXT NOT NULL DEFAULT 'COBERTURA'
```

Aditiva y no destructiva, como las cuatro anteriores. La app ya está instalada en el teléfono de
pruebas y tiene patentes guardadas: el `DEFAULT` hace que quien actualiza abra en cobertura, sin
que ninguna fila se toque ni se pierda.

---

## Lo que NO cambia

Vale enumerarlo porque el grueso de la feature parece pedir datos nuevos y no pide ninguno.

| Entidad | Estado |
|---|---|
| `registro_de_captura` | **Intacta.** Ni una columna nueva, ni una escritura nueva |
| `punto_de_trayecto` | **Intacta** |
| `recorrido` | **Intacta.** La antigüedad se calcula de `finalizadoEn`/`iniciadoEn`, que ya están |
| `voto` | **Intacta.** La probabilidad se sigue derivando igual |

**La columna `registro_de_captura.estado` se conserva.** El FR-030a la saca del mapa; el FR-030b
acota ese retiro al mapa. La columna se sigue escribiendo al compartir y sigue haciendo su
trabajo en dos lugares que nadie cuestionó: que una patente ya compartida no cuente como cubierta
(`RegistroDao.pendientes`) y que no dispare aviso al pasar cerca (`Geofences`, `GeofenceReceiver`
vía `RegistroParaAviso`). Ninguno de esos tres usos se toca.

**Ninguna consulta nueva.** Todo lo que la feature muestra ya se lee hoy: `recorridos.todosUnaVez()`,
`puntos.todos()`, `registros.deRecorrido()`, `votos.todos()`.

---

## Lecturas derivadas

Ninguna se persiste. Se calculan al leer, y por eso no hay nada que mantener sincronizado.

### Escalón de antigüedad — `domain/Antiguedad.kt`

Función pura: de un recorrido y el momento actual, a uno de tres escalones.

```kotlin
enum class Escalon { RECIENTE, MEDIO, VIEJO }   // hasta 3 d · 4 a 14 d · más de 14 d
```

| Entrada | Regla |
|---|---|
| Fecha de referencia | `finalizadoEn ?: iniciadoEn`. Una salida en curso no tiene fin, y su antigüedad es cero |
| Corte 1 | Hasta **3 días** cumplidos → `RECIENTE` |
| Corte 2 | Más de 3 y hasta **14 días** → `MEDIO` |
| Resto | Más de 14 días → `VIEJO` |
| Fecha futura (reloj corrido) | `RECIENTE`. No es un error que valga la pena tratar aparte |

Es la única lógica no trivial de la feature, y por eso es lo que deja prueba ejecutable en JVM
(`AntiguedadTest`), como pide el flujo de desarrollo de la constitución.

### "Hace cuánto fue la última salida" — pantalla de Salidas

De `recorridos.todosUnaVez()`, la fecha más reciente, expresada como tiempo transcurrido
(FR-019, FR-020). Si no hay ninguna salida, la pantalla dice cómo empezar una, como ya hace hoy.

**Lo que explícitamente no se calcula**: ningún total acumulado —calles, cuadras, kilómetros ni
porcentaje— (FR-020a). No hay campo, no hay función, no hay consulta.

---

## Lo que cambia en las estructuras de dibujo

No son entidades de base pero sí son el modelo que el mapa consume, y los tres cambios son parte
del contrato.

### `Trazo`

| Campo | Cambio |
|---|---|
| `destacado: Boolean` | **Se borra.** Revierte el FR-007a de la 003: ya no hay un recorrido que valga más que otro |
| `escalon: Escalon` | **Nuevo.** Lo calcula quien arma el trazo, no `Mapa.kt` |
| `recorridoId`, `puntos`, `ajustado` | Sin cambios |

Los trazos llegan al mapa **ordenados de la salida más vieja a la más nueva** (D5). Ese orden es
parte del contrato, no un detalle de quien los arma: es lo que hace que la más reciente quede
dibujada encima donde se superponen (FR-013).

### `Marcador`

| Campo | Cambio |
|---|---|
| `compartida: Boolean` | **Se borra.** Ya no lo consume nadie: era la entrada de `claseDe` (FR-030a) |
| `toca: Boolean` | **Se conserva**, pero cambia de sentido: ya no elige color, elige tamaño (FR-030d) |
| `probabilidad: Int` | Sin cambios. Pasa de pintar el anillo del círculo a pintar el borde del pin |
| `id`, `numero`, `latitud`, `longitud` | Sin cambios |

### `ModoMapa` en la superficie del mapa

`MapaDeFondo` recibe el modo como parámetro. No es estado del mapa: es un valor que baja de la
pantalla, que a su vez lo leyó de la base. El mapa no lo persiste ni lo decide.

### Las patentes escondidas

`MapaDeFondo` recibe la lista de marcadores. Esconderlas es **no mandarlas**: la pantalla le pasa
una lista vacía, que el mapa ya sabe dibujar porque es el estado del primer cuadro. No hay
parámetro nuevo, no hay capa que prender ni apagar, y los marcadores siguen leídos y contados en
la pantalla, así que volver a mostrarlos no cuesta una consulta.

El interruptor es estado de pantalla —`remember`— y **no** una columna de `estado_del_juego`
(FR-035a). No se persiste a propósito: ver [spec.md](./spec.md), sesión de clarificación del
2026-09-02.

## Lo que la confirmación de duplicados NO cambia en la base

La US6 **no agrega ninguna tabla, ninguna columna y ninguna migración**. Todo lo que necesita ya
existe:

| Qué necesita | Con qué se resuelve |
|---|---|
| Encontrar candidatos | `RegistroDao.porNumero`, que ya existe y ya tiene índice por `numero` |
| Decidir si está cerca | `Geo.distanciaMetros`, función pura, sin Room de por medio |
| Subir la confianza | Una fila en `voto`, la misma que escribe el botón de la ficha |
| Adjuntar la foto | `RegistroDao.adjuntarFoto`, el update acotado que ya existía (FR-019) |

Que no haga falta nada nuevo **es** la señal de que la 003 modeló bien: "volví a ver esta
patente" ya tenía su forma en la base, y lo único que faltaba era dispararla desde la carga y no
solo desde la ficha.

Lo que sí queda fijado: la confirmación **no escribe sobre el registro** salvo la foto que le
faltaba. La ubicación, la precisión y el momento del registro confirmado siguen siendo los de la
primera captura (FR-039a, Principio II). El dato nuevo entra como voto, con su propia hora, que
es lo que permite leer después cuándo se volvió a ver.
