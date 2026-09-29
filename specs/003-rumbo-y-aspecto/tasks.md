---

description: "Task list for Rumbo y aspecto"
---

# Tasks: Rumbo y aspecto

**Input**: Design documents from `/specs/003-rumbo-y-aspecto/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/ui-y-mapa.md](./contracts/ui-y-mapa.md)

**Tests**: **una sola, y es una decisión**. La geometría de D3 —distancia, rumbo y punto
cardinal— es la única lógica pura que esta feature agrega, y se rompe en bordes que no se ven
mirando el teléfono: el cruce por 0°, el redondeo entre sectores. La constitución la obliga.
Todo lo demás que se toca es tema, capas de mapa, un intent y disposición de pantalla, y para
eso el Principio IV descartó la suite de interfaz desde la 001.

El saldo de pruebas de la feature es **una nueva contra cinco borradas**: las de la grilla se
van con la grilla.

**Organization**: agrupadas por user story. Las cuatro se validan caminando, no corriendo algo.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede correr en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: a qué user story pertenece (US1 a US4)

## Path Conventions

Un solo módulo Android (`app/`), sin cambios de estructura respecto de la 002. Raíz de código:
`app/src/main/java/ar/lauta/buscarpatentes/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: no perder los datos de calle, y tener contra qué validar.

**Esta fase pesa más que en las features anteriores.** Hasta ahora la base tenía datos de
prueba cargados en el escritorio; ahora tiene una salida real y patentes capturadas en la
vereda, que no se pueden volver a generar sin volver al lugar.

- [X] T001 Respaldar antes de instalar nada con `./scripts/respaldo.ps1 respaldar`, y verificar que el archivo `-wal` trae cientos de KB. Un respaldo con `-wal` vacío no sirve
- [X] T002 Confirmar contra qué versión se prueba: `adb shell dumpsys package ar.lauta.buscarpatentes | grep -E "firstInstallTime|lastUpdateTime"`. Si son iguales, fue instalación limpia y hay que restaurar antes de validar nada
- [X] T003 Asegurar los datos que la validación necesita: **una salida grabada de 10 minutos o más doblando al menos dos esquinas**, y tres patentes guardadas, una de ellas a varias cuadras. Sin la salida, US2 no se puede juzgar; sin la patente lejana, US1 mide una distancia de cero
  - **De sobra.** La salida real del 29/08 dejó **32 patentes y un recorrido de 71 minutos con 27 puntos**, repartidos sobre ~1,5 km. Los 27 puntos en 71 minutos confirman además la aritmética de D9 por el lado bueno: la estimación era del orden de cien por hora

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: la paleta y la retirada de la grilla. Es lo que bloquea a todas las historias.

**⚠️ CRITICAL**: la paleta tiñe cada pantalla que se toque después, y la grilla tapa
literalmente lo que la US2 viene a dibujar. Hacer cualquiera de las dos tarde obliga a revisar
dos veces todo lo que ya se miró.

T004 no bloquea a nadie y está acá igual, a propósito: es un defecto visible de una línea, y
su requisito vive en la US4 —la primera candidata a recortar—. Ponerlo acá es lo que impide
que se recorte junto con el pulido.

- [X] T004 Llamar a `enableEdgeToEdge()` en `onCreate` de `app/src/main/java/ar/lauta/buscarpatentes/ui/MainActivity.kt` (D7, FR-015). Sin esto nadie fija `isAppearanceLightNavigationBars` y en modo claro los iconos del sistema quedan claros sobre fondo claro: invisibles. `androidx.activity` 1.12.4 ya está en el classpath
- [X] T005 Definir el esquema de color **completo**, claro y oscuro, en `app/src/main/java/ar/lauta/buscarpatentes/ui/Tema.kt` (D6, C1, FR-016). Hoy solo están `primary` y `secondary`: **todos los demás roles —`secondaryContainer`, `surfaceVariant`, `primaryContainer`, `surfaceContainer`— se quedan en la base de Material 3, que es violeta**, y de ahí salen los botones tonales y las tarjetas lilas. Superficies neutras, un solo acento para la acción principal
- [X] T006 Retirar la fuente y la capa de cobertura de `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt`, junto con el parámetro `celdas` de `MapaDeFondo` y `coleccionCobertura` (D4, FR-021)
- [X] T007 Quitar el cálculo y el paso de `celdas` en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt`, y el campo `celdas` de `ResumenDeSalida` en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaRecorridos.kt` (D4)
  - **Hecha antes que T006, no después.** La nota original tenía el orden invertido: `celdas` tiene valor por defecto en `MapaDeFondo`, así que quitar el argumento en los llamadores compila, y quitar el parámetro primero no. Se hizo llamadores → callee.
  - `ResumenDeSalida.celdas` no se borró: se **reemplazó** por `puntos`, que es lo que T024 necesita. Borrarlo para volver a agregarlo hubiera sido churn. El texto de la fila pasó de "12 cuadras recorridas" a "Tocá para ver el camino", que ya es el texto final de FR-010b
- [X] T008 Borrar de `app/src/main/java/ar/lauta/buscarpatentes/domain/Cobertura.kt` lo que existía solo para la grilla: `Celda`, `Limites`, `celdaDe`, `celdasExploradas`, `limitesDe` y `PASO_GRADOS`. **Conservar `tieneElActual` y `consecutivosCubiertos`**, que alimentan el indicador del juego. Borrar también el color `COBERTURA` de `Tema.kt` (D4)
  - `PuntoExplorado` se fue también: existía solo como entrada de `celdasExploradas`
- [X] T009 Borrar las cinco pruebas de la grilla de `app/src/test/java/ar/lauta/buscarpatentes/CoberturaTest.kt` —las que cubren `celdasExploradas`, `celdaDe` y `limitesDe`— y confirmar que las ocho del indicador siguen en verde (D4)
- [X] T010 Anotar el FR-032 y el SC-015 de `specs/001-captura-patentes/spec.md` como superados por esta especificación, del mismo modo en que la 002 anotó su FR-016 y su SC-002 (FR-021a)

**Checkpoint**: no queda un solo rectángulo violeta, ni un violeta que nadie haya escrito. El
mapa quedó limpio para que la US2 dibuje encima.

---

## Phase 3: User Story 1 - Ir hasta una patente guardada (Priority: P1) 🎯 MVP

**Goal**: que guardar la ubicación sirva para volver. Es el círculo que la 001 abrió y nunca
cerró.

**Independent Test**: con una patente guardada a varias cuadras, abrir su ficha y verificar que
dice a qué distancia y en qué dirección está antes de tocar nada, y que un toque más abre la
app de mapas con el punto marcado. No necesita ninguna otra historia.

### Prueba de esta historia

- [X] T011 [P] [US1] Escribir `app/src/test/java/ar/lauta/buscarpatentes/domain/GeoTest.kt` cubriendo distancia entre dos posiciones conocidas, rumbo, y sobre todo **los bordes de la conversión a punto cardinal**: 0°, 360°, y los límites de sector en 22.5° y 337.5°. Es la única prueba nueva de la feature (D3)
  - **13 pruebas, todas en verde.** El caso que justifica el archivo entero es `el norte es el sector partido y no se rompe cruzando el cero`: el sector norte va de 337.5° a 22.5° pasando por 0, así que es el único que no es un intervalo contiguo

### Implementación

- [X] T012 [P] [US1] Crear `app/src/main/java/ar/lauta/buscarpatentes/domain/Geo.kt` con distancia haversine, rumbo inicial y rumbo a punto cardinal en 8 sectores (D3, FR-003). Sin Android adentro: `Location.distanceBetween` es un estático de la plataforma que en JVM tira `not mocked`, y probarlo costaría sumar Robolectric
  - Salió además `distanciaLegible`: redondea a decenas bajo el kilómetro, porque decir "127 m" insinuaría una exactitud que el GPS de un teléfono no tiene
- [X] T013 [US1] Crear `app/src/main/java/ar/lauta/buscarpatentes/ui/Ir.kt` con el intent `ACTION_VIEW` sobre `geo:0,0?q=<lat>,<lng>(<numero>)`, capturando `ActivityNotFoundException` para avisar cuando no hay app de mapas (D2, FR-001, FR-001a). `geo:` y no `google.navigation:`: el segundo ata la app a Google Maps
  - El aviso sin app de mapas quedó como `AlertDialog` en Buscar y como snackbar en la principal, reusando lo que cada pantalla ya tenía. Agregarle un `Scaffold` con `Snackbar` a Buscar para un caso que pasa una vez cada nunca habría sido de más
- [X] T014 [US1] Mostrar distancia y punto cardinal **siempre** en `app/src/main/java/ar/lauta/buscarpatentes/ui/FichaDeRegistro.kt`, junto a los datos del registro y no como respaldo que aparece al fallar algo (FR-003, C4). Sin posición actual: "distancia desconocida", **nunca un número inventado** (FR-003a)
- [X] T015 [US1] Colgar de la ficha la acción de ir hasta la patente, con la advertencia previa cuando la precisión guardada es degradada (FR-001, FR-004, C4). La distancia **no se guarda**: depende de dónde está parado el jugador ahora, y persistirla metería en la evidencia un dato de otro día
- [X] T016 [US1] Pasar la posición actual del jugador a la ficha desde `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt`, que ya la tiene por el mapa
- [X] T017 [US1] Agregar la acción de ir en los resultados de `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaBusqueda.kt` (FR-002). **Toca el mismo archivo que la US3**: ver la dependencia cruzada declarada más abajo
- [ ] T018 [US1] Validar en la calle siguiendo la sección US1 de `specs/003-rumbo-y-aspecto/quickstart.md`, incluidos el paso en modo avión y el de permiso revocado (SC-001, SC-002)

**Checkpoint**: 🎯 **la ubicación guardada deja de ser un dato bonito.**

---

## Phase 4: User Story 2 - El recorrido se ve como el camino que caminé (Priority: P2)

**Goal**: reemplazar los rectángulos por el trazo real, en la pantalla principal y en una
salida puntual.

**Independent Test**: caminar diez minutos por calles conocidas doblando dos esquinas,
terminar, y poder señalar por qué calles se pasó mirando el mapa. Después abrir esa salida
desde Salidas y verla sola.

- [X] T019 [US2] Ordenar la consulta de todos los puntos de trayecto por recorrido y por momento en `app/src/main/java/ar/lauta/buscarpatentes/data/Daos.kt` (data-model). Hoy no declara orden: el trazo saldría con los vértices en el orden que la base quiera darlos, o sea un garabato
- [X] T020 [US2] Dibujar los recorridos en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt`: una fuente GeoJSON con **un `LineString` por recorrido** —nunca uno solo con todos los puntos, que uniría el final de una salida con el principio de la siguiente— y una capa de línea que cambia color y grosor según una propiedad booleana de destacado (D1, C2, FR-006, FR-007a)
  - Colores nuevos en `ColoresDeMapa`: `RECORRIDO` naranja para lo de hoy, `RECORRIDO_VIEJO` apagado para el histórico. Naranja porque los tres colores de patente ya ocupan azul, verde y gris
  - Los recorridos de menos de dos puntos se saltean: con un vértice no hay línea que dibujar
- [X] T021 [US2] Leer los recorridos y sus puntos en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` y pasarlos al mapa, marcando como destacado el que esté `EN_CURSO` o, si no hay ninguno, el de `iniciadoEn` más reciente (FR-006a, FR-007a, FR-008)
- [X] T022 [US2] Verificar que el trazo se dibuja **debajo** de los marcadores de patentes en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt` (C2). MapLibre dibuja en orden de inserción: las patentes son el dato, el recorrido es el contexto
- [X] T023 [US2] Parametrizar `MapaDeFondo` en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt` para servir también a una salida puntual: seguimiento del jugador apagado y encuadre sobre los puntos recibidos (D5, C3). Dos usos reales, así que el Principio IV admite la generalización
  - `encuadrarRegistros` pasó a llamarse `encuadrar` y ahora encuadra patentes **y** camino. Encuadrar solo las patentes dejaba afuera el tramo de la salida donde no se capturó ninguna, que suele ser la mitad
- [X] T024 [US2] Mostrar el mapa de una salida dentro de la fila que `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaRecorridos.kt` **ya expande**, con alto fijo y solo el trazo y las patentes de esa salida (FR-010a, C3). No es una pantalla nueva: un quinto destino de navegación para esto no se justifica
  - **Media tarea ya existía**: la 001 había puesto un mapa en esa fila para mostrar la grilla. Solo hubo que cambiar qué recibe
- [X] T025 [US2] Cubrir la salida sin puntos en `PantallaRecorridos.kt`: se dice que no hay camino que mostrar, **no se abre un mapa vacío** (FR-010b)
  - Hecha junto con T007, cuando `celdas` se reemplazó por `puntos`
- [ ] T026 [US2] Validar en la calle siguiendo la sección US2 de `specs/003-rumbo-y-aspecto/quickstart.md`, con atención al paso 3 —señalar por qué calles se pasó— y al 9, desplazar y hacer zoom con la salida más larga (SC-003, SC-004, SC-005, SC-005a)

**Checkpoint**: el mapa muestra por dónde anduviste, no en qué manzanas estuviste.

---

## Phase 5: User Story 3 - Buscar sirve para encontrar (Priority: P3)

**Goal**: que la pantalla conteste la pregunta que el jugador trae antes de que escriba nada.

**Independent Test**: abrir Buscar sin escribir nada y verificar que ya dice si tenés la
patente del número actual. Después buscar sosteniendo el teléfono con una sola mano.

- [X] T027 [US3] Destacar la patente del número actual del juego al abrir `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaBusqueda.kt` sin escribir nada, e **indicar explícitamente que falta cuando no la hay** (FR-011, C5). El número sale de `EstadoDelJuego`, que la pantalla principal ya lee
- [X] T028 [US3] Mostrar las capturas más recientes en el estado vacío de `PantallaBusqueda.kt` (FR-012). Sin consulta nueva: `todosUnaVez()` ya devuelve todo ordenado por fecha descendente, y los recientes son los primeros de esa lista (D8)
- [X] T029 [US3] Acotar los resultados desde el primer dígito en `PantallaBusqueda.kt`, sin esperar al tercero (FR-013). El filtrado por prefijo se hace en memoria sobre la misma lista: es un jugador y un teléfono, y la escala lo permite con holgura
- [X] T030 [US3] Bajar el campo de búsqueda de `PantallaBusqueda.kt` al alcance del pulgar, igual que el FR-026 de la 002 hizo con la pantalla principal (FR-014)
- [X] T031 [US3] Corregir el comentario obsoleto de `PantallaBusqueda.kt` que dice que "borrar es la única corrección posible": desde la 002 existe `corregirNumero`, y la ficha ya le dice al jugador que corrija en vez de borrar. Un comentario que afirma lo contrario del código es peor que no tener comentario
- [ ] T032 [US3] Validar siguiendo la sección US3 de `specs/003-rumbo-y-aspecto/quickstart.md`, incluida la búsqueda completa con una sola mano (SC-006, SC-007)

**Checkpoint**: la pantalla dejó de ser un filtro que exige tres dígitos para contestar algo.

---

## Phase 6: User Story 4 - Se ve como una herramienta (Priority: P4)

**Goal**: lo que queda del aspecto una vez que la paleta y la barra del sistema ya se
arreglaron en la Phase 2.

**Independent Test**: mirar la pantalla principal sobre distintas zonas del mapa, en los dos
modos, y verificar que todos los controles se leen.

- [X] T033 [US4] Dar fondo propio al campo de número en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt`, de modo que lo escrito se lea sobre cualquier zona del mapa (FR-017, C1)
- [X] T034 [US4] Bajar el alto de la franja de acciones y darle fondo propio en `app/src/main/java/ar/lauta/buscarpatentes/ui/BotonesAccion.kt` (FR-018). **El objetivo táctil no puede achicarse con ella**: más chico visualmente no puede significar más difícil de acertar caminando (FR-019)
  - 64 dp → 52 dp, márgenes 16 → 12/8. **52 sigue por encima del mínimo de 48 dp que recomienda Android**, que es el piso que el FR-019 protege. T036 lo confirma caminando
- [X] T035 [US4] Revisar el indicador del juego y los controles superpuestos al mapa en `PantallaPrincipal.kt` contra la paleta nueva de T005, y que los tres estados de una patente sigan distinguiéndose (FR-020, C1)
  - Revisado, sin cambios necesarios: el indicador usa `Card` y los controles de navegación `FilledTonalIconButton`, y los dos roles quedaron neutros en T005. Los tres colores de patente no se tocaron. Confirmarlo en pantalla es T036
- [ ] T036 [US4] Validar siguiendo la sección US4 de `specs/003-rumbo-y-aspecto/quickstart.md`, incluido el paso 6: tocar los tres botones de la franja diez veces cada uno caminando (SC-008, SC-009)

**Checkpoint**: las cuatro historias terminadas.

---

## Phase 6b: Segunda vuelta, después de caminar (2026-08-29)

**Purpose**: lo que la segunda salida devolvió. Un defecto de verdad, dos features nuevas y
tres correcciones de aspecto. Las respuestas están en la sesión de clarificación del mismo
día en `spec.md`.

- [X] T041 Cortar el trazo donde hubo una desconexión, con `Geo.tramos` y `Geo.huecos` en `app/src/main/java/ar/lauta/buscarpatentes/domain/Geo.kt` (FR-009, FR-009a)
  - **Revierte la decisión de la primera clarificación.** "Unir siempre" se eligió para no fijar un umbral; en la calle eso hizo que un corte de señal *teletransportara* el recorrido hasta la casa del jugador, cruzando manzanas por las que nunca pasó. El trazo afirmaba un camino falso, que es peor que no afirmar ninguno
  - Umbral por **distancia** (250 m) y no por tiempo: un umbral temporal cortaría en falso cada vez que el jugador se para diez minutos a tomar algo. Lo que falta en un corte no es tiempo, es conocimiento del camino
  - **No se pega el trazo a las calles** (FR-009b): eso es map matching contra un servicio de ruteo, o sea red obligatoria y una dependencia nueva. La línea punteada es lo honesto sin red
- [X] T042 [P] Ampliar `app/src/test/java/ar/lauta/buscarpatentes/domain/GeoTest.kt` con los cortes: recorrido continuo, vacío, un punto, uno y dos cortes, pausa larga sin moverse, y el umbral por arriba y por abajo
  - 22 pruebas en total. La de la pausa larga es la que documenta por qué el umbral no es temporal
- [X] T043 Dibujar los tramos y la línea punteada de los huecos en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt` (FR-009a)
- [X] T044 Rehacer la paleta de `app/src/main/java/ar/lauta/buscarpatentes/ui/Tema.kt` sobre la escala zinc de shadcn/ui (FR-016a)
  - El primer intento sacó el violeta y trajo otro problema: neutros tibios, contraste bajo, sin bordes. Zinc es frío, con primer plano casi negro y bordes con valor propio, que es lo que separa una tarjeta del fondo sin sombra
  - Se agregó `Shapes` con un radio único de 8 dp. Material 3 usa de 4 a 28 dp según el componente, y esa variedad es la mitad de lo que hace que una app "se vea Material" en vez de verse como una herramienta
- [X] T045 [P] Cambiar `Card` por `OutlinedCard` en Buscar, Salidas y el indicador del juego. La jerarquía la hace el borde, no la elevación
- [X] T046 Achicar el campo de número en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (FR-017a)
  - Era un `OutlinedTextField`: etiqueta flotante, contorno y 56 dp de alto mínimo, todo pensado para un formulario. Pasa a `BasicTextField` con su propia caja, que ocupa lo que tres dígitos necesitan
- [X] T047 Arreglar la proporción de los iconos de `app/src/main/java/ar/lauta/buscarpatentes/ui/BotonesAccion.kt` (FR-019a)
  - Eran dos causas, no una: al bajar el botón de 64 a 52 dp el icono se quedó en los 24 dp por defecto, **y** el `contentPadding` de `Button` —pensado para un botón ancho con texto— se comía el resto. `contentPadding` en cero e icono de 26 dp
- [X] T048 Agregar `borrarConSuTrayecto` a `app/src/main/java/ar/lauta/buscarpatentes/data/Daos.kt` (FR-024, FR-025)
  - Borra el recorrido y sus puntos. **No toca ninguna patente**: la salida es actividad, la patente es evidencia. Los registros quedan con un `recorridoId` que no apunta a nada, y limpiarlo exigiría escribir sobre evidencia, cosa que esta feature no hace por ninguna razón. `AUTOINCREMENT` garantiza que ningún recorrido futuro herede ese id
- [X] T049 Colgar el borrado de la fila abierta de `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaRecorridos.kt`, con confirmación que dice explícitamente que las patentes se quedan (FR-024, FR-026)
  - El botón aparece solo con la fila abierta: tocarlo sin querer recorriendo la lista sería el mismo error que esto viene a deshacer
  - Si la salida está en curso se detiene **antes** de borrarla. Al revés dejaría el servicio grabando puntos contra un recorrido que ya no existe
- [X] T050 Elegir el fondo del mapa por modo del sistema en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt` (FR-027, FR-028)
  - `positron` en claro y `dark` en oscuro, en lugar de `liberty`. **Verificado contra el JSON de cada estilo**: los tres publican Noto Sans, así que el `textFont` obligatorio de la capa de números sigue resolviéndose y no vuelve el bug de los marcadores invisibles
  - `GRUPO` pasó de casi negro a gris medio y `SIN_FONDO` ganó variante oscura: sobre el fondo `dark` el primero desaparecía y el segundo era un cartel (FR-029)
- [ ] T051 Validar en la calle la segunda vuelta: el corte punteado en el lugar donde se cortó la señal, la paleta nueva en las cinco pantallas, el fondo del mapa en los dos modos, borrar una salida sin perder sus patentes, y que el campo y los botones se sientan bien caminando

---

## Phase 6c: Tercera vuelta (2026-08-29)

**Purpose**: la franja en una sola fila, el fondo del mapa que se veía mal, y el map matching
—que revierte el FR-009b de la vuelta anterior.

- [X] T052 Revertir el estilo del mapa en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt` (FR-027, FR-028)
  - `positron` dibujaba las calles menores tan claras que en la calle solo se veían las avenidas. **Sobrio no puede costar información**: se camina por calles. Vuelve `liberty` en modo claro
  - En oscuro entra `fiord` en lugar de `dark`: su capa `highway_minor` arranca en zoom 8 y las pinta en gris azulado sobre fondo azul oscuro. Verificado leyendo el JSON del estilo, no probando a ojo
- [X] T053 [P] Crear `ic_play.xml` e `ic_detener.xml` en `app/src/main/res/drawable/`
- [X] T054 Pasar `BotonesAccion` a extensión de `RowScope` y convertir el control de recorrido en botón circular en `app/src/main/java/ar/lauta/buscarpatentes/ui/BotonesAccion.kt` (FR-018a)
  - Era un botón ancho con texto que se comía media franja para una acción que se usa dos veces por salida. El icono cambia entre play y stop, y en curso va teñido con el color de error: dice que está grabando sin necesitar texto
- [X] T055 Unificar campo y botones en una sola fila en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (FR-018a)
  - Dos filas de controles sobre un mapa era el doble de alto para la misma cantidad de acciones, y el campo quedaba enorme por estar solo en su renglón. Ahora se lleva el ancho sobrante con `weight`
- [X] T056 Precargar el campo con el número actual del juego, seleccionado entero (FR-030)
  - `TextFieldValue` en lugar de `String`, por la selección. **Sin ella el cambio sería un retroceso**: en la salida del 29/08 se capturaron 32 números distintos y ninguno era el actual, así que casi siempre habría que borrar tres dígitos antes de escribir. Seleccionado, el primer dígito los reemplaza
- [X] T057 [P] Crear `app/src/main/java/ar/lauta/buscarpatentes/domain/Polilinea.kt` con el decodificador de polilínea (FR-031)
  - **Valhalla codifica a 1e6, no a 1e5.** Con el factor equivocado el camino cae en el lugar correcto pero diez veces más chico: un garabato apretado sobre una manzana, sin ninguna excepción de por medio
- [X] T058 [P] Escribir `app/src/test/java/ar/lauta/buscarpatentes/domain/PolilineaTest.kt` contra la respuesta real del servicio
  - 7 pruebas sobre la polilínea que Valhalla devolvió para los primeros cinco puntos de la salida del 29/08. Una compara los dos factores de precisión, que es el error que no se ve
- [X] T059 Agregar `caminoAjustado` a `Recorrido` y la migración v3→v4 en `app/src/main/java/ar/lauta/buscarpatentes/data/` (FR-031, FR-034)
  - Columna nullable: los recorridos ya guardados arrancan sin ajustar y se ajustan solos la próxima vez que la app abra con conexión. Ninguna fila se toca
  - **No reemplaza a los puntos de trayecto** (FR-034): esos son lo que el teléfono midió. El camino ajustado es una interpretación, vive aparte y se puede recalcular
- [X] T060 Crear `app/src/main/java/ar/lauta/buscarpatentes/ubicacion/AjustarACalles.kt` (FR-031, FR-032, FR-035)
  - Valhalla de FOSSGIS, libre y sin clave, con costeo `pedestrian` y `shape_match: map_snap`. **Verificado antes de escribir el cliente**: devolvió calles reales de la zona para los puntos de la salida
  - `HttpURLConnection` y `org.json`, los dos de la plataforma. Retrofit y un serializador para una llamada a un endpoint es lo que el Principio IV manda no hacer
  - **El standby no tiene cola ni `WorkManager`**: la cola es la consulta `sinAjustar()`. Un recorrido terminado con `caminoAjustado` en null está pendiente, y cada apertura con red los intenta todos
  - Se consulta `NET_CAPABILITY_VALIDATED` y no solo "hay red": una wifi que todavía pide login está conectada y no llega a internet, y preguntarlo evita gastar el timeout para descubrirlo
- [X] T061 Dibujar el camino ajustado en lugar del crudo cuando existe, en `Mapa.kt`, `PantallaPrincipal.kt` y `PantallaRecorridos.kt` (FR-031, FR-033)
  - Nunca los dos a la vez: serían dos líneas casi iguales encimadas
  - **Un camino ajustado no se corta ni se puntea.** Cortar tenía sentido sobre el crudo, donde un salto significaba "no sé qué pasó en el medio"; acá el servicio ya contestó qué pasó
- [ ] T062 Validar la tercera vuelta: que el recorrido del 29/08 con sus tres saltos aparezca ajustado a las calles al abrir la app con wifi, la fila única caminando, el campo precargado, y las calles visibles en los dos modos

---

## Phase 6d: Detalles después de la tercera vuelta (2026-08-29)

- [X] T063 Sacar la precarga del número en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt`: el campo arranca vacío y los tres puntos son el placeholder (FR-030)
  - Revierte T056. La precarga funcionaba como se especificó y **la especificación estaba mal**: el número del juego es una sugerencia, no un valor, y precargado era algo para borrar casi siempre
  - `TextFieldValue` vuelve a ser `String` y `campoConElActual` se borra: existían solo para dejar el número precargado seleccionado, y sin precarga no hay nada que seleccionar. El cambio saca código
- [X] T064 Quitar el hueco entre la franja de acciones y el teclado en `PantallaPrincipal.kt` (FR-018b)
  - Eran el relleno del `Scaffold` —que incluye la barra de navegación— **más** `imePadding`. El inset del teclado ya se mide desde el borde de la pantalla, o sea que ya contiene a la barra: corresponde `union` de los dos, que toma el mayor, y no la suma

- [X] T065 Sacar el ajuste a calles del efecto que carga marcadores y trazos, a su propio `LaunchedEffect` en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (FR-035)
  - **Defecto encontrado en la validación final, no caminando.** Estaba adentro del efecto que se dispara con cada guardado, así que hacía dos cosas malas: salía a la red con cada patente cargada, y —peor— el resto del efecto esperaba la llamada, de modo que después de guardar el marcador nuevo podía tardar hasta los 20 segundos del timeout en aparecer
  - El FR-035 dice que el ajuste no puede demorar ninguna pantalla, y ahí la estaba demorando. Ahora corre una vez por entrada a la pantalla, sin que nadie lo espere, y pide la relectura solo si ajustó algo

---

## Phase 6e: El muestreo, después de la cuarta salida (2026-08-30)

Validando la T026 en la calle: el trazo en vivo salió con diagonales que no correspondían a
ninguna calle, y desplazado. Con wifi el ajuste lo acercó, pero una calle por la que sí se
pasó no quedó dibujada. La causa no está en el dibujo ni en el ajuste: está en los puntos.

- [X] T071 Subir el muestreo a `PRIORITY_HIGH_ACCURACY`, 5 s y 10 m, y descartar los puntos con precisión peor que 30 m en `app/src/main/java/ar/lauta/buscarpatentes/ubicacion/ServicioRecorrido.kt` (FR-036)
  - La prioridad balanceada resuelve por antenas y wifi con decenas de metros de error, y una manzana mide alrededor de cien: alcanzaba para poner el recorrido entero sobre la calle de al lado
  - Los 50 m de distancia mínima venían de la grilla de celdas, donde bastaba saber en qué cuadra se estuvo. Retirada la grilla en la T006, quedó un punto por cuadra dibujado como camino: por eso las esquinas salían cortadas en diagonal
- [X] T072 Pasarle a Valhalla el `radius` de cada punto desde su precisión guardada en `app/src/main/java/ar/lauta/buscarpatentes/ubicacion/AjustarACalles.kt` (FR-036a)
  - Sin esto el servicio pega con la misma confianza un punto de 5 m y uno de 25, y así es como elige la calle paralela
- [X] T073 Anotar el FR-024 y el FR-025 de `specs/001-captura-patentes/spec.md` como superados, y escribir el FR-036 y el FR-036a en `specs/003-rumbo-y-aspecto/spec.md`
- [ ] T074 Validar la cuarta vuelta: repetir el recorrido de la salida fallida y comparar. El trazo en vivo tiene que doblar en las esquinas en vez de cortarlas, quedar sobre las calles caminadas, y el ajuste con wifi no puede saltearse ninguna calle por la que se pasó
  - Mirar también la batería: es el costo que este cambio paga, y el que puede obligar a aflojar el intervalo

---

## Phase 6f: La confianza en el mapa (2026-08-30)

Pedido del jugador. La probabilidad de permanencia existe desde que hay votos, pero vivía
solo adentro de la ficha: para saber si valía la pena caminar hasta una patente había que
abrirla de a una.

- [X] T075 Agregar `todos()` a `VotoDao` en `app/src/main/java/ar/lauta/buscarpatentes/data/Daos.kt` (FR-037). Una consulta para todos los votos, agrupada en memoria: de a una sería una consulta por marcador
- [X] T076 Dibujar la probabilidad en el **anillo** del marcador en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt`, con la escala divergente rojo/blanco/verde de `ColoresDeMapa` (FR-037, FR-037a, FR-037b)
  - El anillo y no el relleno: el relleno lleva los tres estados del FR-020 y el número blanco de adentro necesita fondo oscuro para leerse. El anillo estaba blanco y fijo, o sea que no decía nada
  - Divergente y no un degradado de un tono: el medio de esta escala **es** un valor con significado —nadie votó— y tiene que seguir viéndose blanco (FR-037b)
  - Anillo de 3 px y no 2: con información adentro tiene que poder mirarse caminando
- [X] T077 Pasar la probabilidad a los marcadores desde `PantallaPrincipal.kt` y `PantallaRecorridos.kt`, y releer los marcadores al votar (FR-037, FR-037c)
- [X] T078 [P] Cubrir la propiedad nueva en `app/src/test/java/ar/lauta/buscarpatentes/mapa/ColeccionTest.kt`: que viaje en el feature y que sin votos sea la neutra y no cero. Un cero pintaría de rojo todas las patentes sobre las que nadie votó
- [X] T080 Pasar el anillo a tres escalones —0 a 3 rojo, 4 a 7 amarillo, 8 a 10 verde— en `Mapa.kt` y `Tema.kt` (FR-037b). El degradado no se leía en el teléfono: el color de un marcador solo se juzga contra otro que esté a la vista
- [X] T081 Arreglar el grupo que no se partía a ningún zoom en `opcionesDeAgrupacion` de `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt` (FR-038)
  - `clusterMaxZoom` estaba en 20 y el `maxZoom` de la fuente quedó en su valor por defecto, 18. La fuente nunca indexaba un nivel sin agrupar, así que pasado el 18 el mapa estiraba las teselas del 18 —con el grupo adentro— y el grupo sobrevivía a cualquier zoom
  - Ahora los dos salen de `ZOOM_MAXIMO`: la fuente indexa hasta el techo del mapa y deja de agrupar un nivel antes
- [X] T082 Poner techo de zoom en 19 con `setMaxZoomPreference` en `Mapa.kt` (FR-039)
- [X] T083 Apagar la rotación y la brújula del mapa en `Mapa.kt` (FR-040)
- [ ] T079 Validar en la calle: que el anillo se lea de un vistazo caminando, que votar "ya no estaba" en la ficha se vea en el marcador al cerrarla, que el grupo se abra al acercar, y que el mapa no se sienta pesado ni se tuerza

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T037 Correr la sección de regresión de `specs/003-rumbo-y-aspecto/quickstart.md`: guardar en modo avión, guardar con el mapa en cualquier estado, el indicador, compartir, y el recorrido con la pantalla apagada
- [ ] T038 Medir la carga rápida completa desde la app cerrada: 4 interacciones o menos y 10 segundos o menos (SC-010). **Es la misma medición que la 002 dejó pendiente en su T034**, así que esta feature la salda
- [X] T039 Revisar que la fila con mapa de `PantallaRecorridos.kt` no dejó controles debajo de las barras del sistema (C6)
  - La fila vive dentro del `Column` que ya tiene `safeDrawingPadding`, así que hereda el contrato. No hizo falta tocar nada
- [X] T040 [P] Actualizar `README.md`: la tabla de pruebas cambia —entra `GeoTest`, `CoberturaTest` adelgaza— y se agrega el enlace a la spec y al quickstart de la 003
  - Se agregó además una sección de respaldo. Con datos de calle en el teléfono, "respaldar antes de reinstalar" dejó de ser una nota al pie del quickstart

---

## Estado al 2026-08-29

**Todo el código de la feature está escrito, compila y las pruebas pasan.** Quedan ocho
tareas, y las ocho son de caminar con el teléfono en la mano.

La Phase 6b salió de la segunda salida: un defecto real —el trazo que teletransportaba—, dos
features nuevas y tres correcciones de aspecto. La 6c salió de mirar el resultado: el fondo
del mapa se había vuelto ilegible, la franja seguía siendo dos filas, y el map matching pasó
de descartado a implementado.

**El recorrido del 29/08 es el caso de prueba de todo esto**: tiene tres saltos de más de
250 m —338, 485 y 606— así que ejercita el corte, el punteado y el ajuste a calles con los
mismos datos.

| Tarea | Qué falta |
|---|---|
| T018 | US1 en la calle: distancia, rumbo, abrir la app de mapas, modo avión, permiso revocado |
| T026 | US2 en la calle: reconocer el camino, la salida puntual, el zoom con el trazo largo |
| T032 | US3: que Buscar conteste sin escribir nada, y buscar con una sola mano |
| T036 | US4: barra del sistema en los dos modos, y los botones diez veces caminando |
| T037 | Regresión: modo avión, mapa que no demora, indicador, compartir, pantalla apagada |
| T038 | Carga rápida: 4 interacciones, 10 segundos. Salda también la T034 de la 002 |

El APK con todo esto ya está en `/sdcard/Download/buscar-patentes-debug.apk`, y los datos de
la salida real quedaron respaldados en `respaldos/2026-08-29_143507/` antes de tocar nada.

**Lo que más conviene mirar primero**, porque es lo que se hizo a ciegas: la paleta nueva
(T005) sobre las cinco pantallas, y el trazo sobre las calles reales (T020). Lo demás son
cambios cuyo resultado se puede razonar; esos dos hay que verlos.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias. T001 es prerrequisito real de todo lo demás: hay datos de calle en el teléfono que no se pueden regenerar
- **Foundational (Phase 2)**: depende de Setup. **BLOQUEA todas las user stories** — la paleta tiñe todo lo que se toque después, y la grilla tapa lo que la US2 dibuja
- **US1 (Phase 3)**: depende de Foundational
- **US2 (Phase 4)**: depende de Foundational, en particular de T006 a T008. Independiente de US1
- **US3 (Phase 5)**: depende de Foundational. Independiente de US1 y US2, salvo por el archivo que comparte con la US1
- **US4 (Phase 6)**: depende de T005. Es lo que queda del aspecto después de la Phase 2
- **Polish (Phase 7)**: depende de las historias implementadas

### Dependencia cruzada, declarada

**US1 y US3 comparten `PantallaBusqueda.kt`.** T017 le agrega la acción de ir a los resultados;
T027 a T031 reescriben el estado vacío y el campo. Es la única dependencia real entre historias
de esta feature, y se resuelve por orden: **si se hace la US1 primero, la US3 conserva la
acción al reescribir la pantalla**. Al revés también funciona, pero hay que acordarse.

Si se corta la feature después de la US1, T017 queda como la única cosa de esa pantalla que
cambió, y es coherente por sí sola.

### Within Each User Story

- La geometría antes de la ficha que la muestra
- El orden de la consulta antes del trazo que lo consume: sin T019, T020 dibuja un garabato
- La generalización del mapa antes de la segunda pantalla que lo usa
- La validación en dispositivo al final de cada historia, nunca antes

### Parallel Opportunities

- Setup: T001 primero y solo; T002 y T003 después
- Foundational: T004 en paralelo con todo lo demás de la fase — archivo distinto, sin
  dependencias. T005 a T009 en serie, porque se pisan entre archivos
- US1: T011 y T012 en paralelo — la prueba y la función viven en archivos distintos, y
  escribirlas juntas es lo que hace que la prueba sirva
- US2 y US3 pueden ir en paralelo si no se toca `PantallaBusqueda.kt` al mismo tiempo

---

## Parallel Example: User Story 1

```bash
# La geometría y su prueba, juntas:
Task: "Geo.kt con haversine, rumbo y punto cardinal"
Task: "GeoTest.kt con los bordes de sector: 0°, 360°, 22.5°, 337.5°"

# Después, en serie, porque todo cuelga de la ficha:
# T013 (el intent) → T014 (distancia siempre visible) → T015 (la acción) → T016 → T017
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1: Setup — respaldar, sobre todo
2. Phase 2: Foundational — la paleta y la grilla, que bloquean todo
3. Phase 3: User Story 1
4. **PARAR Y VALIDAR**: caminar hasta una patente guardada usando la app
5. A esta altura la ubicación guardada sirve para volver, que es lo que la 001 prometió sin
   terminar de cumplir

### Incremental Delivery

1. Setup + Foundational → sin violeta y sin rectángulos. **Ya se ve distinto**
2. US1 → **la app te lleva hasta la patente**
3. US2 → el mapa muestra por dónde anduviste
4. US3 → Buscar contesta antes de que escribas
5. US4 → el acabado

### Dónde recortar si hace falta

**US4 es la primera candidata**, igual que la historia de aspecto lo fue en la 001 y en la 002.
Y esta vez el recorte es más barato que nunca: **lo que más molestaba de la interfaz —el
violeta y la barra del sistema— ya se arregló en la Phase 2**, que no se recorta porque bloquea
todo. Lo que queda en la US4 es el campo, la franja y una revisión.

**US1 es la que más valor agrega**, y la spec la puso P1 por eso. Si hay que elegir entre US1 y
cualquier otra, no hay discusión.

El corte natural del proyecto es **después de US2**: ahí la app te lleva hasta una patente y te
muestra por dónde anduviste, que son las dos cosas que la salida real destapó como faltantes.

---

## Notes

- `[P]` = archivos distintos, sin dependencias pendientes
- **Una prueba nueva, cinco borradas.** No es un descuido: está justificado arriba y en la nota
  de `research.md`
- Esta feature **no escribe nada en la base**. Si aparece una sentencia de escritura en algún
  diff, algo se salió del plan
- El orden de las fases no es el orden de prioridad de la spec. La US1 es P1 y va tercera
  porque la Phase 2 bloquea a todos. Si hay que recortar, se recorta por prioridad de la spec,
  no por este orden
- Respaldar con `./scripts/respaldo.ps1 respaldar` antes de cada reinstalación. Reinstalar en
  vez de actualizar borra la base, y ahora lo que se perdería son datos de calle
- Commitear por tarea o por grupo lógico, con Conventional Commits según la constitución

---

## Phase 8: Convergence

Lo que quedó entre lo que la spec pide y lo que el código hace, después de la 6d. Ningún
requisito está sin construir: son un defecto de refresco, dos comentarios que afirman lo
contrario del código y dos huecos de cobertura.

- [X] T066 Refrescar el trazo del recorrido en curso mientras el jugador camina en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` per FR-008, US2/AC3 (partial). Hoy el efecto que carga los trazos depende de `guardando`, `recorrido` y `recarga`, y `ServicioRecorrido.enCurso` solo cambia al empezar y al terminar: con la pantalla abierta caminando, los puntos que el servicio va guardando no se vuelven a leer nunca. Lo más barato es que el servicio publique un contador que se incremente con cada punto guardado y que ese contador sea una clave más del efecto — sin sondeo y sin tocar el camino de captura (FR-005)
- [X] T067 Reescribir el KDoc de `coleccionTrazos` en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt` per FR-009, FR-009a (contradicts). Todavía dice "se une todo sin cortar" y "un tramo sin señal se dibuja como recta y se acepta", que es el FR-009 anterior a la segunda salida; el código de abajo corta con `tramosDe`/`Geo.tramos` y dibuja el hueco punteado. Es el mismo defecto que la T031 arregló en Buscar: un comentario que afirma lo contrario del código es peor que no tener comentario
- [X] T068 Cubrir `coleccionTrazos` y `coleccionHuecos` en `app/src/test/java/ar/lauta/buscarpatentes/mapa/ColeccionTest.kt` per Constitución, "Flujo de desarrollo" (partial). `Geo.tramos` ya tiene prueba; lo que no la tiene es el armado del GeoJSON: un `LineString` por tramo y no uno por recorrido, el tramo de un solo punto que se saltea, el orden lng/lat —que es exactamente el bug por el que este archivo existe— la propiedad `destacado`, y que un trazo ajustado no genere huecos
- [X] T069 Sacar las referencias a la grilla retirada en `app/src/main/java/ar/lauta/buscarpatentes/data/Daos.kt` y `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` per FR-021 (contradicts). `Daos.kt` llama a `todos()` "la cobertura acumulada de FR-032" —un requisito que la FR-021a anotó como superado, y cuyo número en la 003 significa otra cosa— y el comentario del `onCorregir` dice que refresca "marcadores, cobertura e indicador"
- [X] T070 [P] Agregar `PolilineaTest` a la tabla de pruebas del `README.md` per tasks T040 (partial). La tabla se actualizó en la T040 y la prueba llegó después, en la T058

---

## Phase 9: Convergence

**Segunda pasada de convergencia, después de las vueltas 6b a 6f.** Ninguna de las cuatro
user stories tiene requisitos sin construir: el código cubre los 56 requisitos funcionales
vigentes. Lo que queda son un hueco real en la P1, un costo de refresco que las vueltas
posteriores multiplicaron por seis sin que nadie volviera a mirarlo, tres comentarios y dos
documentos de diseño que afirman lo contrario del código, un subsistema entero que ningún
requisito pidió, y dos detalles.

Las diez tareas sin marcar de las fases anteriores siguen siendo lo que eran: caminar con el
teléfono en la mano. Esta fase no las toca.

- [X] T084 Mostrar distancia y rumbo en cada resultado de `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaBusqueda.kt` per FR-003, US1/AC2, FR-001a (partial). El FR-003 pide la distancia y la dirección en **la información de una patente guardada**, y el FR-002 puso la acción de ir en los dos lugares donde el jugador se encuentra con una: la ficha y los resultados de búsqueda. En la ficha están; en `FilaRegistro` no. El propio código lo admite: el diálogo de "sin app de mapas" de esta pantalla dice *"Acá no hay distancia en pantalla como en la ficha"* y manda al jugador al mapa — pero el FR-001a dice que no hace falta respaldo **porque** la distancia y el rumbo ya están a la vista, y acá no lo están. Falta leer la posición actual una vez al entrar a la pantalla —`contenedor.ubicacion.leerAhora()`, como hace `PantallaPrincipal` al abrir la ficha— y pasarla a la fila; sin lectura, "distancia desconocida" y nunca un número inventado (FR-003a)
- [X] T085 Dejar de releer la base entera con cada punto grabado en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` per FR-010, SC-005, FR-036 (partial). El efecto de marcadores y trazos tiene a `puntosGrabados` entre sus claves desde la T066, y su comentario justifica el costo diciendo que *"llega uno cada 30 s y 50 m"*. La T071 subió el muestreo a 5 s y 10 m: ahora cada punto dispara seis consultas —estado, pendientes, todos los votos, todos los registros, todas las salidas y **todos los puntos de trayecto de todas las salidas**—, vuelve a decodificar cada polilínea guardada y reconstruye el GeoJSON completo, que `MapaDeFondo` sube con `setGeoJson`. Caminando, eso pasa cada 5 segundos, que es exactamente lo que el FR-010 y el SC-005 prohíben. Lo barato es que la clave `puntosGrabados` recargue **solo el trazo de la salida en curso** —`puntos.deRecorrido(id)`, que ya existe— y deje los marcadores, los votos y el histórico donde estaban
- [X] T086 Corregir el KDoc de `Geo.SALTO_MAXIMO_M` en `app/src/main/java/ar/lauta/buscarpatentes/domain/Geo.kt` per FR-036 (contradicts). Dice *"El servicio de recorrido guarda una posición cada 30 segundos y cada 50 metros"* y de ahí deduce que 250 m son *"al menos cuatro posiciones"* perdidas. Con el muestreo del FR-036 —5 s y 10 m— son alrededor de veinticinco, así que el número sigue siendo defendible pero su razón ya no dice nada. Es el mismo defecto que la T067 arregló en `coleccionTrazos` y la T031 en Buscar
- [X] T087 Corregir el KDoc de `ColoresDeMapa` en `app/src/main/java/ar/lauta/buscarpatentes/ui/Tema.kt` per FR-027, FR-029 (contradicts). Afirma que los colores del mapa no cambian con el modo del sistema porque *"el estilo del mapa no se oscurece"* y los tiles son *"claros en modo claro y claros en modo oscuro"*. Desde la T050 y la T052 el estilo **sí** se oscurece: `fiord` en oscuro, `liberty` en claro. La decisión de dejar los colores fijos puede seguir siendo la correcta, pero su argumento escrito ya es falso, y el FR-029 exige que los colores con significado se distingan **sobre los dos fondos**. Hay que reescribir la justificación contra los dos estilos reales, no contra uno que no existe
- [X] T088 Decodificar cada pierna por separado en `app/src/main/java/ar/lauta/buscarpatentes/ubicacion/AjustarACalles.kt` per FR-031 (contradicts). `pedirCamino` concatena las cadenas `shape` de `trip.legs` con `buildString`, y una polilínea codificada es un flujo de **diferencias acumuladas desde cero**: la pierna 2 arranca de nuevo en cero, así que decodificar la concatenación pone sus puntos en la suma de las dos coordenadas —fuera del planeta— en lugar de a continuación de la pierna 1. Con `shape_match: map_snap` y sin `type` por punto Valhalla suele devolver una sola pierna, así que hoy no se ve; el bucle que el código escribió existe justamente para el caso en que devuelva más. Decodificar cada `shape` con `Polilinea` y concatenar los **puntos**, o dejar asentado por qué una sola pierna está garantizada
- [X] T089 Justificar por escrito el subsistema de votos, o retirarlo per Principio IV, FR-037 (unrequested). La entidad `Voto`, `VotoDao`, la migración v4→v5, `domain/Probabilidad.kt`, `ProbabilidadTest` y los dos botones de votar de `FichaDeRegistro.kt` no salen de ningún requisito: no aparecen en la 001, no aparecen en la 002, y en la 003 el FR-037 los da por existentes —*"La probabilidad ya se calcula a partir de los votos"*— cuando en el árbol de trabajo son código nuevo, con la base pasando de v3 a v5. El FR-037 pidió **dibujar** una probabilidad que ya se leía; construir de dónde sale nunca se pidió. Converge no borra: lo que corresponde es escribir el requisito que le falta —en la 003 o en una spec propia— o sacarlo. Ojo con el orden si se saca: la migración ya está publicada en el teléfono de pruebas
- [X] T090 Actualizar `specs/003-rumbo-y-aspecto/contracts/ui-y-mapa.md` y `specs/003-rumbo-y-aspecto/data-model.md` per FR-009, FR-009a, FR-031, FR-034 (contradicts). El C2 todavía dice *"Continuidad | Sin cortes. Un tramo sin señal se dibuja como recta y se acepta (FR-009)"* y *"Unidad de dibujo | Un trazo por salida"*: las dos son la decisión que la segunda salida revirtió, y el código hace lo contrario desde la T041 y la T043. El `data-model.md` abre diciendo que la feature *"no agrega entidades, no agrega campos, no agrega migración y no agrega ni una sentencia de escritura"*, y desde la T059 y la T048 agrega la columna `caminoAjustado`, la migración v3→v4, `guardarCaminoAjustado` y `borrarConSuTrayecto`. Un contrato que describe lo que se descartó no gobierna nada. **El `plan.md` arrastra la misma afirmación en su Constitution Check del Principio II y queda afuera de esta tarea a propósito**: converge no lo edita, y corregirlo es una enmienda del plan
- [X] T091 [P] Agregar `ProbabilidadTest` a la tabla de pruebas del `README.md` per tasks T040, T070 (partial). La tabla se actualizó en la T040, la T070 le sumó `PolilineaTest`, y la prueba de la probabilidad llegó después con la Phase 6f sin que nadie la anotara. Es la misma tarea que la T070, un archivo más tarde
- [X] T092 [P] Corregir el KDoc de `ResumenDeSalida` en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaRecorridos.kt` per FR-024, FR-025 (contradicts). El bloque que encabeza la `data class` describe *"El diálogo de confirmación de borrado"*: quedó separado del código que documenta y ahora explica un componente que está cien líneas más arriba
