---

description: "Task list for Pulido de interfaz"
---

# Tasks: Pulido de interfaz

**Input**: Design documents from `/specs/002-pulido-de-interfaz/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/ui-y-mapa.md](./contracts/ui-y-mapa.md)

**Tests**: ninguno nuevo, y es una decisión, no un olvido. Esta feature no agrega lógica
pura: es tema, capas de mapa, interfaz y una sentencia de escritura acotada. El único
candidato a prueba —corregir el número— ya queda cubierto por `InmutabilidadTest`, que lee
el fuente del DAO y falla si alguna sentencia escribe sobre un campo de evidencia. Agregar
pruebas de interfaz sería exactamente lo que el Principio IV descartó en la 001.

**Organization**: agrupadas por user story. La 002 es una feature de pulido, así que cada
historia se valida mirando el teléfono, no corriendo algo.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede correr en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: a qué user story pertenece (US1 a US5)

## Path Conventions

Un solo módulo Android (`app/`), sin cambios de estructura respecto de la 001. Raíz de
código: `app/src/main/java/ar/lauta/buscarpatentes/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: saber contra qué se está trabajando, y no volver a perder los datos de prueba.

La primera versión de D2 en `research.md` fue un diagnóstico invertido por suponer el
estado del dispositivo en vez de mirarlo. Esta fase existe para que eso no se repita.

- [X] T001 Verificar que el APK instalado es el último y anotar el estado inicial, siguiendo el Paso 0 de `specs/002-pulido-de-interfaz/quickstart.md`: comparar tamaño y `lastUpdateTime` de `dumpsys package` contra el archivo de `/sdcard/Download/`
- [ ] T002 Cargar tres registros de prueba con números distintos y compartir uno, para poder validar US3 y US5. La base quedó vacía tras la reinstalación limpia del 2026-08-29
  - **Mitad hecha.** El 2026-08-29 a las 00:56 se cargaron 313, 314 y 315, con precisiones de 18,4 / 13,0 / 9,1 m. Los tres siguen `PENDIENTE`: falta compartir uno, que es lo que hace falta para ver el color de "compartida" en el mapa. No bloquea US1 ni US2; sí es prerrequisito de T030.
- [X] T003 [P] Respaldar los datos cargados con `./scripts/respaldo.ps1 respaldar`, y repetirlo antes de cada reinstalación

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: la maquinaria del tema. Es lo único que bloquea a todas las historias: la ficha
de US3 y el contraste de US5 se construyen encima de estos colores.

**⚠️ CRITICAL**: sin esto, cada pantalla que se toque después hay que revisarla dos veces.

D1 identificó **tres causas encadenadas** del modo oscuro roto. Estas tareas cubren las dos
primeras; la tercera es por pantalla y vive en US1.

- [X] T004 Crear `app/src/main/java/ar/lauta/buscarpatentes/ui/Tema.kt` con un esquema claro y uno oscuro, eligiendo según `isSystemInDarkTheme()`. Los tres colores con significado del mapa —pendiente, ya toca, compartida— se definen acá y deben distinguirse en ambos esquemas (C1)
- [X] T005 Corregir `app/src/main/res/values/themes.xml`: el padre es hoy `android:Theme.Material.NoActionBar`, que es la variante **oscura fija** de la plataforma. Cambiarlo a una variante DayNight (D1, causa 1)
- [X] T006 Envolver el contenido de `app/src/main/java/ar/lauta/buscarpatentes/ui/MainActivity.kt` con el tema de T004 en lugar de `MaterialTheme` sin argumentos, que usa el esquema claro fijo (D1, causa 2)

**Checkpoint**: el tema existe y sigue al sistema. Las pantallas todavía no lo aprovechan.

---

## Phase 3: User Story 1 - Las pantallas de adentro se leen y se pueden dejar (Priority: P1) 🎯 MVP

**Goal**: que Buscar, Salidas y Ajustes se lean igual de bien en modo claro y oscuro, y que
el control para volver esté donde la mano lo espera.

**Independent Test**: poner el teléfono en modo oscuro, entrar a las tres pantallas,
verificar que todo el texto se lee y que el control de volver se ve completo y alcanzable.
Repetir en claro. No necesita ninguna otra historia.

- [X] T007 [P] [US1] Envolver el contenido de `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaBusqueda.kt` en un `Surface` que pinte el fondo del tema (D1, causa 3)
- [X] T008 [P] [US1] Envolver el contenido de `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaAjustes.kt` en un `Surface` (D1, causa 3)
- [X] T009 [P] [US1] Envolver el contenido de `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaRecorridos.kt` en un `Surface` (D1, causa 3)
- [X] T010 [US1] Crear una barra superior compartida en `app/src/main/java/ar/lauta/buscarpatentes/ui/BarraSuperior.kt` con título y control de volver, usando el componente de Material 3 (D2, re-diagnóstico). Tres usos reales, así que el Principio IV la admite
- [X] T011 [US1] Reemplazar la fila de título y `TextButton("Volver")` por la barra de T010 en `PantallaBusqueda.kt`, `PantallaAjustes.kt` y `PantallaRecorridos.kt` (FR-004)
- [X] T012 [US1] Hacer que las tres pantallas respondan al gesto de retroceso del sistema, no solo al control visible, en `app/src/main/java/ar/lauta/buscarpatentes/ui/MainActivity.kt` (FR-004)
- [X] T013 [US1] Verificar en el dispositivo, en los dos modos y cambiando el modo con la app abierta, siguiendo la sección US1 de `specs/002-pulido-de-interfaz/quickstart.md` (SC-001, SC-002)

**Checkpoint**: 🎯 **lo que estaba roto dejó de estarlo.** Es la única historia de esta
feature que arregla un defecto en lugar de mejorar algo.

---

## Phase 4: User Story 2 - El mapa abre donde estoy parado (Priority: P2)

**Goal**: que el mapa arranque centrado en el jugador y que haya un toque para volver.

**Independent Test**: abrir la app en la calle y verificar que queda centrado sin tocar
nada. Arrastrar a otra zona y verificar que un solo control lo devuelve.

**Estado de partida**: el centrado y el seguimiento **ya funcionan** (T084 de la 001, más el
arreglo de la transición de cámara del 2026-08-29). Lo que sigue es lo que falta encima.

- [X] T014 [US2] Apagar el seguimiento cuando el jugador arrastra el mapa, escuchando el gesto en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt`. Sin esto el mapa pelea contra el dedo: llega la siguiente lectura y vuelve solo (FR-006, C4)
- [X] T015 [US2] Agregar un control flotante de recentrado sobre el mapa en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt`, que reactive el seguimiento y vuelva a la posición actual (FR-007)
- [X] T016 [US2] Encuadrar los registros guardados cuando no hay permiso o todavía no llegó ninguna lectura, en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt`. Nunca vista mundial muda (FR-008)
- [X] T017 [US2] Verificar que nada de lo anterior demora la captura: abrir en modo avión y guardar una patente con el mapa en cualquier estado (FR-009, hereda FR-038 de la 001)
- [X] T018 [US2] Validar en la calle siguiendo la sección US2 de `specs/002-pulido-de-interfaz/quickstart.md` (SC-003, SC-004)

**Checkpoint**: el mapa se comporta como el jugador espera de una app de mapas.

---

## Phase 5: User Story 3 - Tocar una patente del mapa y hacer algo con ella (Priority: P3)

**Goal**: ver el número de cada patente sin tocar nada, y poder abrir su ficha para
corregirla, compartirla o borrarla.

**Independent Test**: con tres registros cargados, leer sus números en el mapa sin tocarlos.
Tocar uno, ver su ficha, corregir el número de uno y borrar otro.

**Es la historia más grande de la feature, y la única que toca la base.**

- [X] T019 [P] [US3] Agregar `id` y `numero` a la vista de marcador de `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt`, y propagarlos como propiedades de cada feature GeoJSON (data-model)
- [X] T020 [P] [US3] Poblar esos campos donde se arman los marcadores en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt`, con el número en 3 dígitos y ceros a la izquierda
- [X] T021 [US3] Dibujar el número adentro del marcador con una capa de símbolo de solo texto sobre la capa de círculos existente, en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt` (FR-010, D4). La capa **debe** declarar `textFont` con una tipografía que el estilo publique —OpenFreeMap Liberty solo sirve Noto Sans—: sin eso MapLibre pide su Open Sans por defecto, el pedido no se resuelve nunca y arrastra el teselado de toda la fuente, con lo que los círculos tampoco se dibujan y no hay ningún error en logcat
- [X] T022 [US3] Verificar que los tres colores de FR-011 siguen distinguiéndose con el número encima, en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt`
- [X] T023 [US3] Activar la agrupación por cercanía de la fuente GeoJSON y dibujar los grupos con su cuenta, en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt` (FR-016, D5). **Reabierta**: se implementó y se revirtió durante el diagnóstico de los marcadores invisibles, por sospecha equivocada. La causa real era la capa de símbolo sin `textFont` (ver T021), así que la agrupación probablemente nunca estuvo rota y se puede reintentar tal cual, sola y con su propia validación. Los datos de prueba la piden: los seis registros caen dentro de ~2 metros
  - **Hecha.** La fuente se crea con `withCluster(true)`, radio 28 px —el diámetro del
    marcador, así que dos registros se agrupan justo cuando se taparían— y techo de zoom 20,
    por encima del zoom de calle para que los ~2 metros de los datos de prueba se puedan
    abrir. Cuatro capas filtradas por `point_count`: círculo y número del registro suelto,
    círculo y cuenta del grupo. La capa de la cuenta lleva el mismo `textFont` obligatorio
    que T021. **Tocar un grupo no hace nada**: se separa acercando el mapa. Abrir la ficha
    desde un grupo sin acercar no lo pide ningún requisito
- [X] T024 [US3] Detectar el toque sobre un marcador consultando las features dibujadas en ese punto, filtrando por la capa de patentes, en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt` (FR-012, D6)
- [X] T025 [US3] Agregar `corregirNumero` a `app/src/main/java/ar/lauta/buscarpatentes/data/Daos.kt` como sentencia que escribe **solo** `numero`, `patenteTexto` y `formato`. Ningún campo de evidencia puede aparecer en el `SET` (FR-014, D8, Principio II)
- [X] T026 [US3] Confirmar que `app/src/test/java/ar/lauta/buscarpatentes/InmutabilidadTest.kt` sigue en verde y que **falla** si se agrega a mano un campo de evidencia a la sentencia de T025. Es la prueba que cubre esta feature sin escribir una nueva
- [X] T027 [US3] Implementar la corrección en `app/src/main/java/ar/lauta/buscarpatentes/ui/Captura.kt`: parsear la entrada con `Patente`, rechazar lo que no dé 3 dígitos, y reconciliar geofences y cobertura al terminar (FR-015)
- [X] T028 [US3] Crear la ficha en `app/src/main/java/ar/lauta/buscarpatentes/ui/FichaDeRegistro.kt` como hoja inferior, mostrando número, momento, precisión, foto y estado. La ubicación y la hora se muestran **como dato, nunca como campo editable** (FR-012, C3, D7)
- [X] T029 [US3] Colgar de la ficha las tres acciones —corregir, compartir y borrar con confirmación— reutilizando `Compartir` y `Captura.borrar` que ya existen, en `app/src/main/java/ar/lauta/buscarpatentes/ui/FichaDeRegistro.kt` (FR-013, FR-014)
- [X] T030 [US3] Validar siguiendo la sección US3 de `specs/002-pulido-de-interfaz/quickstart.md`, con especial atención al paso 6: anotar ubicación y hora antes de corregir y comprobar que no cambiaron (SC-005, SC-006, SC-007)

**Checkpoint**: el mapa deja de ser decoración y pasa a ser la forma principal de navegar el
archivo.

---

## Phase 6: User Story 4 - El teclado aparece cuando lo pido (Priority: P4)

**Goal**: que la app abra con el mapa a la vista y el teclado guardado.

**Independent Test**: abrir la app y verificar que el mapa se ve completo. Contar las
interacciones de una carga rápida completa: tienen que ser 4 o menos.

- [X] T031 [US4] Cambiar `android:windowSoftInputMode` de `stateAlwaysVisible` a `stateHidden` en `app/src/main/AndroidManifest.xml` (FR-017)
  - **Adelantada el 2026-08-29.** Eran dos causas, no una: además del foco pedido en código, `stateAlwaysVisible` levanta el teclado al arrancar la actividad aunque nadie pida foco. Arreglar solo una de las dos no habría cambiado nada.
- [X] T032 [US4] Quitar `foco.requestFocus()` de la primera composición y borrar el `FocusRequester` que queda sin uso, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (FR-017, FR-018)
  - **Adelantada el 2026-08-29**, junto con T031. El `FocusRequester` y sus dos imports se eliminaron por la misma regla del Principio IV que cerró T083 de la 001.
- [X] T033 [US4] Anotar en `specs/001-captura-patentes/spec.md` que su FR-016 queda superado por el FR-017 de esta especificación (FR-020)
- [ ] T034 [US4] Medir la carga rápida completa desde la app cerrada y confirmar 4 interacciones o menos y 10 segundos o menos, siguiendo la sección US4 de `specs/002-pulido-de-interfaz/quickstart.md` (FR-019, SC-008, SC-009)

**Checkpoint**: el camino de captura cuesta un toque más y está dentro del techo que la
constitución 1.1.0 fijó. **4 es el techo, no un punto de partida.**

---

## Phase 7: User Story 5 - La pantalla principal se ve como una app terminada (Priority: P5)

**Goal**: iconografía real y controles que se distingan del mapa.

**Independent Test**: mostrarle la pantalla principal a alguien que no conoce la app y
verificar que identifica cuál es el botón de cargar una patente.

- [X] T035 [P] [US5] Crear los vector drawables que faltan en `app/src/main/res/drawable/` —cámara, recentrado y lo que pida T015—. Sin agregar `material-icons-extended`: el Principio IV lo descarta por traer miles de iconos para usar tres (D3)
  - **Empezada.** `ic_volver.xml` se creó durante T010, porque `Icons` no está en el classpath: Material 3 ya no arrastra `material-icons-core`. Confirma D3 antes de tiempo — la alternativa era agregar una dependencia para una flecha. Sin `android:tint`: el color lo pone `Icon` desde el tema, y `?attr/colorControlNormal` no compila con temas de plataforma.
- [X] T036 [US5] Reemplazar el emoji `📷` y ajustar los tres botones para que usen iconos reales, en `app/src/main/java/ar/lauta/buscarpatentes/ui/BotonesAccion.kt` (FR-022)
- [X] T037 [US5] Dar a los controles superpuestos al mapa un fondo o elevación que los separe de cualquier contenido debajo, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (FR-023)
- [X] T038 [US5] Revisar que el indicador del estado del juego no perdió legibilidad con el tema nuevo, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (FR-025)
- [X] T038a [US5] Bajar los controles de navegación —Buscar, Salidas, Ajustes— del borde superior a donde llega el pulgar, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (FR-026)
  - El Principio I dice que la app se usa "con una mano". Los tres `TextButton` del indicador están hoy arriba de todo, que en un teléfono alto es exactamente donde el pulgar no llega. El indicador puede quedarse arriba —se lee, no se toca—; lo que tiene que bajar son los controles.
- [X] T038b [US5] Achicar el campo de número a lo que necesitan tres dígitos, devolviéndole el espacio sobrante al mapa, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (FR-027)
  - Hoy ocupa el ancho completo con texto de 32sp. Al achicarlo hay que cuidar que el objetivo táctil siga siendo cómodo: más chico visualmente no puede significar más difícil de acertar.
- [ ] T039 [US5] Validar con alguien que nunca usó la app siguiendo la sección US5 de `specs/002-pulido-de-interfaz/quickstart.md` (SC-010), y hacer una carga rápida completa sosteniendo el teléfono con una sola mano, sin recolocarlo (SC-011)

**Checkpoint**: las cinco historias terminadas.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [ ] T040 Correr la sección de regresión de `specs/002-pulido-de-interfaz/quickstart.md`: guardar en modo avión, el mapa que no demora el guardado, el indicador del juego y el compartir
- [X] T041 [P] Actualizar `README.md` si alguna de las pantallas nuevas cambia cómo se corre o se valida el proyecto
  - Compilar e instalar no cambió. Sí cambió qué se valida: se agregó la fila de
    `ColeccionTest` a la tabla de pruebas, el enlace a la spec de la 002 y el enlace a su
    quickstart
- [X] T042 Revisar que ninguna pantalla nueva dejó controles debajo de las barras del sistema ni del teclado, incluida la ficha de T028 (C2, FR-003)
  - Las tres pantallas secundarias siguen con `safeDrawingPadding`, y la principal toma el
    relleno del `Scaffold` más `imePadding`. **La ficha era el agujero, exactamente el que
    C2 anticipó**: `ModalBottomSheet` se dibuja sobre la barra de gestos y no se corre
    cuando sube el teclado, así que "Borrar" quedaba debajo de la barra y el campo de
    corregir, debajo del teclado. Se le agregaron `navigationBarsPadding` e `imePadding`.
    Falta la confirmación en el dispositivo, que va con T040

---

## Estado al 2026-08-29

**Todo el código de la feature está escrito.** Lo que queda no se implementa: se camina.

| Tarea | Qué falta | Por qué no se puede acá |
|---|---|---|
| T002 | Compartir uno de los tres registros cargados | El teléfono no acepta toques por `adb` |
| T034 | Cronometrar la carga rápida: ≤4 interacciones, ≤10 s | Se mide con el teléfono en la mano |
| T039 | Alguien que nunca usó la app, y una carga con una sola mano | Hace falta otra persona |
| T040 | Regresión: modo avión, mapa que no demora, indicador, compartir | Se camina |

El APK con todo esto ya está en `/sdcard/Download/buscar-patentes-debug.apk`, y los datos
del teléfono quedaron respaldados en `respaldos/2026-08-29_114639/` antes de tocarlo.

**T040 también confirma el arreglo de insets de la ficha (T042)**, que es lo único de esta
tanda que se hizo a ciegas: el problema se dedujo del comportamiento de `ModalBottomSheet`,
no se vio en pantalla.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias. T002 es prerrequisito real de US3 y US5: sin registros no hay nada que mirar
- **Foundational (Phase 2)**: depende de Setup. **BLOQUEA todas las user stories** — cualquier pantalla tocada antes del tema hay que revisarla de nuevo
- **US1 (Phase 3)**: depende de Foundational
- **US2 (Phase 4)**: depende de Foundational. Independiente de US1
- **US3 (Phase 5)**: depende de Foundational. Se apoya en el mapa, pero no necesita US2 terminada
- **US4 (Phase 6)**: **ya casi lista**. T031 y T032 están hechas; quedan la anotación y la medición
- **US5 (Phase 7)**: depende de Foundational, y conviene después de US2 porque T035 comparte los iconos con el control de recentrado de T015
- **Polish (Phase 8)**: depende de las historias implementadas

### Dependencia cruzada, declarada

**US5 comparte un icono con US2.** T035 dibuja el vector de recentrado que T015 consume. Si
US5 va antes, T015 lo encuentra hecho; si va después, T015 usa un icono provisorio y T036 lo
reemplaza. Es la única dependencia real entre historias.

### Within Each User Story

- El tema antes que cualquier pantalla
- Las propiedades del marcador antes de dibujarlas y antes de poder consultarlas
- La sentencia del DAO antes de la ficha que la usa
- La validación en dispositivo al final de cada historia, nunca antes

### Parallel Opportunities

- Setup: T003 en paralelo con lo demás
- US1: T007, T008 y T009 en paralelo — tres archivos distintos, el mismo cambio
- US3: T019 y T020 en paralelo antes de que T021 los consuma
- US5: T035 en paralelo con todo lo demás de la historia

---

## Parallel Example: User Story 1

```bash
# Los tres Surface son el mismo cambio en archivos distintos:
Task: "Surface en PantallaBusqueda.kt"
Task: "Surface en PantallaAjustes.kt"
Task: "Surface en PantallaRecorridos.kt"

# Después, en serie, porque tocan los mismos tres archivos:
# T010 (crear la barra) → T011 (usarla en los tres) → T012 → T013
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1: Setup — saber contra qué se trabaja y no volver a perder los datos
2. Phase 2: Foundational — el tema, que bloquea todo
3. Phase 3: User Story 1
4. **PARAR Y VALIDAR**: las tres pantallas en modo oscuro y en claro
5. A esta altura lo que estaba roto dejó de estarlo, que es el piso de esta feature

### Incremental Delivery

1. Setup + Foundational → el tema listo
2. US1 → **lo roto arreglado**
3. US4 → ya está casi hecha; cerrarla cuesta una anotación y una medición
4. US2 → el mapa se comporta como corresponde
5. US3 → **acá la feature paga**: el mapa pasa a ser la forma de navegar el archivo
6. US5 → el acabado

### Dónde recortar si hace falta

**US5 es la primera candidata**, igual que lo fue US5 en la 001. Es la única historia que no
arregla un problema concreto de uso, y la propia spec la puso última por eso.

**US3 es la más cara y la que más valor agrega.** Si hay que elegir entre US3 y US5, no hay
discusión.

El corte natural del proyecto es **después de US3**: ahí el pulido resolvió todo lo que
molestaba de verdad.

---

## Notes

- `[P]` = archivos distintos, sin dependencias pendientes
- **Esta feature no agrega pruebas automáticas**, y está justificado arriba. T026 es la
  excepción aparente: no escribe una prueba, verifica que la que existe cubre lo nuevo
- T031 y T032 ya están hechas: se adelantaron el 2026-08-29 porque eran una línea cada una
  y el ciclo de APK estaba abierto. Quedan marcadas para no volver a hacerlas
- El Principio I permite 4 interacciones desde la constitución 1.1.0, enmendada para esta
  feature. T034 lo mide. Si da 5, algo se agregó al camino de captura que no corresponde
- Respaldar con `./scripts/respaldo.ps1 respaldar` antes de cada reinstalación. Reinstalar
  en vez de actualizar borra la base, y ya pasó una vez
- Commitear por tarea o por grupo lógico, con Conventional Commits según la constitución
