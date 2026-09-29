# Buscar patentes

App Android personal para el juego de patentes en secuencia. Se anota una patente vista en
la calle con el mínimo roce posible —abrir, tipear tres dígitos, tocar `+`— y la app
resuelve sola la ubicación, la precisión y el momento. Cuando el juego llega a un número ya
anotado, el teléfono avisa al pasar cerca del lugar.

Todo local: sin cuentas, sin servidor propio, sin red en el camino de captura.

- Qué hace y por qué: [`specs/001-captura-patentes/spec.md`](specs/001-captura-patentes/spec.md)
- Cómo está construido: [`specs/001-captura-patentes/plan.md`](specs/001-captura-patentes/plan.md)
- El pulido de interfaz: [`specs/002-pulido-de-interfaz/spec.md`](specs/002-pulido-de-interfaz/spec.md)
- Rumbo y aspecto, lo que destapó el primer uso real: [`specs/003-rumbo-y-aspecto/spec.md`](specs/003-rumbo-y-aspecto/spec.md)
- Buscar la que toca, la barra y el recorrido por varias: [`specs/005-buscar-la-que-toca/spec.md`](specs/005-buscar-la-que-toca/spec.md)
- Reglas que no se negocian: [`.specify/memory/constitution.md`](.specify/memory/constitution.md)

## Requisitos

- JDK 17 o superior (la compilación apunta a bytecode 17)
- SDK de Android con `platforms/android-36`, vía Android Studio o `cmdline-tools`
- `local.properties` con `sdk.dir` apuntando al SDK
- Para las User Stories 4 y 5, un **teléfono físico** con Google Play Services y Android 8.0
  o superior. El emulador no dispara geofences de forma confiable ni reproduce el consumo
  de batería

## Compilar, instalar, probar

```bash
./gradlew assembleDebug          # APK en app/build/outputs/apk/debug/
./gradlew installDebug           # instala en el dispositivo conectado
./gradlew test                   # lógica de dominio, en JVM, sin dispositivo
./gradlew connectedAndroidTest   # consultas de Room, requiere dispositivo
```

En Windows, `gradlew.bat` en lugar de `./gradlew`.

## Qué cubren las pruebas automáticas

`./gradlew test` corre sobre `app/src/test/`, y cubre las funciones puras que la constitución
obliga a probar, más una prueba de ausencia:

| Prueba | Qué protege |
|---|---|
| `PatenteTest` | Extracción del número de 3 dígitos, formato viejo y Mercosur (FR-010) |
| `CoberturaTest` | El texto de descubiertas del número actual (FR-003 de la 005) |
| `AvisosTest` | Qué merece geofence y qué merece notificación (D4, FR-028, FR-029, FR-041) |
| `InmutabilidadTest` | Que **no exista** ninguna forma de editar la evidencia de un registro (Principio II) |
| `ColeccionTest` | El GeoJSON de los marcadores: un feature por registro, con su id, su número y su probabilidad; la que toca en su propia colección, fuera de los grupos; un pin corrido en su posición de dibujo y un grupo con solo su cuenta (FR-010, D1 y D13 de la 005) |
| `ColeccionTrazosTest` | El GeoJSON de los recorridos: un `LineString` por tramo, el hueco punteado de cada desconexión, que un camino ajustado no se corte, el escalón de antigüedad de cada salida, y que la colección conserve el orden de entrada — que es lo que hace que la salida reciente quede dibujada encima de la vieja (FR-006, FR-009a, FR-031, FR-013) |
| `GeoTest` | Distancia y rumbo hasta una patente, los bordes del punto cardinal —el cruce por 0° y los límites de sector— y el corte del trazo donde se cortó la señal (FR-003, FR-009) |
| `PolilineaTest` | El decodificador de la polilínea que devuelve el servicio de ajuste a calles, incluidas las piernas separadas (FR-031) |
| `ProbabilidadTest` | La cuenta de los votos sobre si una patente sigue estando: arranca en 0, se acumula de a uno y no se pasa de los extremos (FR-037); y qué patentes conservan sus votos al migrar a la v7 |
| `PrioridadTest` | El orden de la lista de la barra —distancia pesada por confianza, empate por la captura más reciente— y el orden de caminar las paradas de un recorrido (FR-012, FR-015 de la 005) |
| `IrTest` | Qué hace "Abrir en Google Maps" según cuántas paradas quedaron, y la URL del recorrido a pie con punto decimal aunque el teléfono esté en español (FR-019 de la 005) |
| `AcomodoTest` | Los pines que se hacen lugar antes de agruparse: se separan a zoom de calle sin irse de la cuadra, se agrupan de lejos, una cadena no termina en un solo grupo, nadie se pierde y la misma entrada da lo mismo (FR-023 de la 005) |
| `AntiguedadTest` | Los escalones de hace cuánto se caminó una calle, con los bordes exactos en 3 y 14 días, la salida en curso y el reloj corrido; y el tiempo dicho como lo diría una persona (FR-014, FR-019) |

Todo lo demás —el aviso de proximidad, el recorrido con la pantalla apagada, el consumo de
batería, la carga rápida con cronómetro— se valida caminando, y está escrito paso a paso en
[`specs/001-captura-patentes/quickstart.md`](specs/001-captura-patentes/quickstart.md), en
[`specs/002-pulido-de-interfaz/quickstart.md`](specs/002-pulido-de-interfaz/quickstart.md)
—modo oscuro, mapa y ficha de un registro— y en
[`specs/003-rumbo-y-aspecto/quickstart.md`](specs/003-rumbo-y-aspecto/quickstart.md), que
agrega ir hasta una patente y el recorrido dibujado. La
[`004`](specs/004-planear-recorridos/quickstart.md) suma lo que hay que mirar al sol: los tres
modos del mapa, la escala de antigüedad, los pines nuevos, el botón que esconde las patentes y
la confirmación de una patente que ya estaba anotada en el lugar. La
[`005`](specs/005-buscar-la-que-toca/quickstart.md) recorre la barra, el filtro por número, la
lista, el recorrido por varias y los pines que se hacen lugar.

## Antes de reinstalar: respaldar

Reinstalar la app —no actualizarla, reinstalarla— borra la base y las fotos, y desde la
primera salida real lo que se perdería son datos de calle que no se pueden volver a generar
sin volver al lugar.

```powershell
./scripts/respaldo.ps1 respaldar      # a respaldos/<fecha-hora>/
./scripts/respaldo.ps1 restaurar      # de vuelta al teléfono
```

Verificar que el archivo `-wal` del respaldo pesa cientos de KB: la base sola puede venir
casi vacía porque los datos todavía están en el write-ahead log.

## Mapa

El fondo usa [OpenFreeMap](https://openfreemap.org), un servicio gratuito operado por un
tercero. La URL del estilo vive en un solo lugar, `ConfigMapa.ESTILO_URL` en
`app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt`: si el servicio desaparece se
cambia esa constante y nada más. La captura de patentes nunca depende de que el mapa cargue.
