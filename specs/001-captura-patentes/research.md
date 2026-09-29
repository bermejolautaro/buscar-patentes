# Phase 0 Research: Captura de patentes

**Fecha**: 2026-08-28
**Spec**: [spec.md](./spec.md)
**Constitución**: [../../.specify/memory/constitution.md](../../.specify/memory/constitution.md)

Cada decisión se justifica contra los requisitos de la spec y los cuatro principios.

---

## D1 — Plataforma: Android nativo, no multiplataforma ni web

**Decision**: aplicación Android nativa en Kotlin. minSdk 26 (Android 8.0), compileSdk 36.

**Rationale**: tres requisitos, juntos, eliminan todo lo demás.

- **FR-039** exige que el aviso de proximidad llegue con la app cerrada, delegando el
  seguimiento al sistema operativo. Eso es la Geofencing API del sistema. Una PWA no
  tiene acceso a nada parecido: el service worker no se despierta por ubicación.
- **FR-026** exige registro de trayecto con la pantalla apagada. Requiere un foreground
  service con tipo `location`, declarado en el manifiesto.
- **FR-003** exige guardar la precisión reportada por el proveedor de ubicación. La API
  web de geolocalización la expone, pero sin el control de prioridad que necesita D3.

El BRIEF ya apuntaba a Android y a FusedLocationProvider, y la constitución lo nombra
como referencia en Restricciones técnicas. La decisión confirma esa dirección en vez de
abrirla.

**Alternatives considered**:

- **PWA / web**: descartada. Sin geofencing en segundo plano, la User Story 4 —la que
  cierra el ciclo del producto— no se puede construir.
- **Flutter / React Native**: descartadas. Los plugins de geofencing y de foreground
  service con ubicación son las partes más frágiles de ambos ecosistemas, y son
  justamente el corazón de este producto. Envolver la API nativa para después pelearse
  con el envoltorio es más trabajo que llamarla directo.
- **Kotlin Multiplatform**: descartada por el Principio IV. Un solo usuario, un solo
  teléfono, y ese teléfono es Android. Compartir código con un iOS que no existe es
  infraestructura que nadie pidió.
- **iOS además de Android**: fuera de alcance. Si aparece, la lógica de dominio (parseo
  de patente, cobertura, selección de geofences) ya queda aislada de Android y es lo
  único que valdría la pena portar.

---

## D2 — Mapa: MapLibre Native con tiles de OpenFreeMap

**Decision**: MapLibre Native para Android, estilo vectorial servido por OpenFreeMap.
El caché ambiente de MapLibre cubre el guardado offline.

**Rationale**: **FR-042 a FR-045** describen, casi literalmente, el caché ambiente de
MapLibre:

| Requisito | Mecanismo |
|---|---|
| FR-042 — guardar solo el fondo de las zonas ya visitadas | El caché ambiente guarda automáticamente cada tile que se muestra. Cero código. |
| FR-043 — offline muestra lo guardado | El renderer sirve del caché cuando no hay red. |
| FR-044 — acotar espacio, liberar lo menos usado primero | `setMaximumAmbientCacheSize`, con desalojo LRU. |
| FR-045 — ver cuánto ocupa y poder borrarlo | `clearAmbientCache` más consulta de tamaño del archivo de caché. |

OpenFreeMap sirve tiles vectoriales de OpenStreetMap sin API key ni cuenta, lo que
respeta el Principio IV: la app no obliga a registrarse en ningún servicio.

**Alternatives considered**:

- **Google Maps SDK**: descartada. Sus términos prohíben cachear o exportar tiles para
  uso offline, así que FR-042 a FR-045 no se pueden construir encima. Es la razón
  técnica, no una preferencia.
- **osmdroid**: candidata seria y más simple, con caché SQLite propio y tiles raster
  sin dependencias de Google. Descartada por dos motivos: los tiles raster ocupan
  varias veces más que los vectoriales para la misma superficie, lo que empeora FR-044;
  y su desalojo de caché es por antigüedad, no por uso, mientras FR-044 pide liberar
  "las zonas menos visitadas". Queda como plan B si OpenFreeMap deja de estar
  disponible.
- **MapTiler / Stadia**: mismos tiles vectoriales, pero requieren cuenta y API key.
  Roza el Principio IV sin aportar nada que OpenFreeMap no dé.

**Riesgo declarado**: OpenFreeMap es un servicio gratuito operado por un tercero. Si
desaparece, la app necesita otra fuente de tiles. La mitigación es que la URL del estilo
sea un valor configurable en un solo lugar, no una constante dispersa por el código.

---

## D3 — Ubicación: tres mecanismos distintos, no uno

**Decision**: la app usa tres APIs de ubicación diferentes, una por caso de uso, con
presupuestos de precisión y batería deliberadamente distintos.

| Uso | Mecanismo | Prioridad | Frecuencia |
|---|---|---|---|
| Captura (FR-002, FR-003) | `FusedLocationProviderClient.getCurrentLocation()` | `PRIORITY_HIGH_ACCURACY` | Una sola lectura, bajo demanda |
| Trayecto de recorrido (FR-024, FR-025) | `requestLocationUpdates` en foreground service | `PRIORITY_BALANCED_POWER_ACCURACY` | Cada 30 s o 50 m |
| Aviso de proximidad (FR-028, FR-039) | `GeofencingClient` | Gestionado por el sistema | Evento, app cerrada |

**Rationale**: **FR-025** exige explícitamente que la precisión de captura no se use
para el trayecto. Esta tabla es esa separación hecha código.

`getCurrentLocation()` es la llamada correcta para la captura, no `getLastLocation()`:
la última posición conocida puede tener minutos de antigüedad, y guardar eso violaría el
Principio II, que dice que el timestamp y la ubicación son del momento de la captura.

Para el trayecto, el muestreo por distancia (`setMinUpdateDistanceMeters(50)`) además de
por tiempo resuelve un problema que el intervalo puro tiene: a 40 km/h, 30 segundos son
330 metros y se pierden cuadras enteras. El objetivo de FR-024 es saber qué calles se
recorrieron, así que la distancia es la variable que importa, no el reloj.

**Alternatives considered**:

- **Un solo `requestLocationUpdates` de alta precisión para todo**: descartada. Rompe
  FR-025 y hace imposible SC-010 (5% de batería en 2 horas).
- **Polling propio para el aviso de proximidad en lugar de Geofencing API**: descartada.
  Obliga a mantener la app despierta, rompe FR-039 y hace imposible SC-012 (3% de
  batería por día en reposo).

---

## D4 — Geofences: solo el número actual, no todo el archivo

**Decision**: la app registra geofences únicamente para los registros pendientes cuyo
número coincide con el número actual del juego. Cuando el contador avanza, se
desregistran los viejos y se registran los nuevos.

**Rationale**: Android limita a **100 geofences activos por aplicación**. Un archivo de
varios cientos de patentes no entra. Pero **FR-029** dice que no hay que avisar por
registros cuyo número todavía no toca — así que no hace falta que entren.

Con el juego en un número, los geofences activos son los registros pendientes de ese
número: típicamente entre cero y unos pocos. El límite de 100 deja de ser una
restricción y pasa a ser holgura enorme.

La consecuencia de diseño es que **avanzar el contador del juego dispara una
reconciliación de geofences**. Ese es el único momento en que el conjunto cambia, junto
con crear o compartir un registro del número actual.

**Alternatives considered**:

- **Registrar geofences para todos los registros y filtrar en el callback**: descartada.
  Choca con el límite de 100 apenas el archivo crece, y despierta la app por eventos
  que va a descartar, gastando batería contra SC-012.
- **Registrar los próximos N números por adelantado**: descartada por el Principio IV.
  Complejidad sin requisito que la pida.

---

## D5 — Persistencia: Room, fotos en archivos

**Decision**: Room (SQLite) para registros, estado del juego, recorridos y puntos de
trayecto. Las fotos se guardan como archivos en el almacenamiento privado de la app, y
la fila del registro guarda la ruta.

**Rationale**: Room es parte de AndroidX, es lo estándar en Android, y para cuatro
entidades con consultas por número y por estado escribe menos código que SQLite crudo.

Las fotos no van en la base: un BLOB de varios MB por fila degrada toda consulta que
toque la tabla. Archivo en disco más ruta en la fila es el patrón normal y correcto.

Almacenamiento privado de la app, no la galería del sistema: **FR-013** dice que no hay
cuentas ni servidor, y **Principio II** dice que la evidencia es inmutable. Una foto en
la galería la puede editar o borrar cualquier otra app.

**Alternatives considered**:

- **SQLite crudo**: descartada, más código para el mismo resultado.
- **DataStore para todo**: descartada. El estado del juego solo son dos valores y podría
  ir ahí, pero mantener dos mecanismos de persistencia para ahorrar una tabla de una
  fila es complejidad sin beneficio.
- **Fotos en la galería pública**: descartada por el Principio II.

---

## D6 — UI: Compose, con el mapa embebido por interoperabilidad

**Decision**: Jetpack Compose para toda la interfaz. El mapa de MapLibre se embebe con
`AndroidView`.

**Rationale**: Compose es el camino por defecto para una app Android nueva. MapLibre
Native expone una `View`, así que el mapa entra por `AndroidView` — el mecanismo previsto
para exactamente esto.

El punto crítico es **FR-016 más FR-038**: la app abre con el campo enfocado y el teclado
visible, y el estado de carga del mapa no puede bloquear ni demorar la captura. En
Compose esto se resuelve componiendo el campo y el teclado en el primer frame, y dejando
que el `AndroidView` del mapa se infle y cargue por su cuenta detrás. El teclado se pide
con un `FocusRequester` en un `LaunchedEffect` que corre en la primera composición, sin
esperar al mapa.

**Alternatives considered**:

- **Vistas XML**: descartada. Es el camino legacy y no simplifica nada acá.
- **Mapa como pantalla aparte para no pelear con la interoperabilidad**: descartada.
  El usuario pidió explícitamente el mapa de fondo en la pantalla principal.

---

## D7 — Sin framework de inyección de dependencias

**Decision**: construcción manual de dependencias en un contenedor simple a nivel de
`Application`. Sin Hilt, sin Koin.

**Rationale**: Principio IV. Una app de un módulo, con una base de datos, un cliente de
ubicación y un puñado de casos de uso, no justifica el peso de un framework de DI ni su
generación de código. Un objeto contenedor construido a mano en `Application` es menos
código y menos magia.

**Alternatives considered**:

- **Hilt**: descartada. Es lo estándar en apps Android grandes; esta no lo es. Si el
  proyecto crece hasta que la construcción manual duela de verdad, se agrega ahí.

---

## D8 — Pruebas: JUnit sobre la lógica de dominio, sin suite de UI

**Decision**: JUnit para la lógica pura, tests instrumentados de Room para las
consultas, sin Espresso ni pruebas de interfaz.

**Rationale**: la constitución dice que toda lógica no trivial deja al menos una prueba
ejecutable que falle si la lógica se rompe. La lógica no trivial de este proyecto es
identificable y está toda fuera de la interfaz:

- extracción del número de 3 dígitos de los dos formatos de patente (FR-010)
- cálculo de números consecutivos cubiertos desde el actual (FR-022)
- selección del conjunto de geofences a registrar ante un cambio de contador (D4)
- decisión de si un aviso corresponde, contra número actual, estado y repetición (FR-028,
  FR-029, FR-041)
- agregación de puntos de trayecto a calles exploradas acumuladas (FR-032)

Cada uno es una función pura sobre datos, testeable sin Android. Una suite de Espresso
para verificar que un botón está abajo cuesta más de lo que protege.

**Alternatives considered**:

- **Espresso o Compose UI testing**: descartada por ahora. Se agrega si alguna
  regresión real de interfaz aparece dos veces.
- **Sin pruebas**: descartada. La constitución lo prohíbe explícitamente.

---

## Sin NEEDS CLARIFICATION pendientes

Todos los desconocidos del Technical Context quedaron resueltos en D1 a D8. No queda
ningún marcador abierto para Phase 1.
