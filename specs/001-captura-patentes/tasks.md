---

description: "Task list for Captura de patentes"
---

# Tasks: Captura de patentes

**Input**: Design documents from `/specs/001-captura-patentes/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/os-and-share.md](./contracts/os-and-share.md)

**Tests**: incluidos, pero acotados. La constitución exige que toda lógica no trivial deje
al menos una prueba ejecutable que falle si la lógica se rompe. Esa obligación cubre
exactamente las cinco funciones puras identificadas en D8 y en Derivados del data model.
No hay suite de UI ni pruebas por función: sería exactamente lo que el Principio IV
prohíbe.

**Organization**: agrupadas por user story para que cada una se implemente y se pruebe
sola.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede correr en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: a qué user story pertenece (US1 a US5)

## Path Conventions

Un solo módulo Android (`app/`), según Structure Decision de plan.md. Raíz de código:
`app/src/main/java/ar/lauta/buscarpatentes/`. Pruebas JVM en `app/src/test/`,
instrumentadas en `app/src/androidTest/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: proyecto Android que compila y corre vacío.

- [X] T001 Crear proyecto Gradle con módulo `app/` en `settings.gradle.kts` y `app/build.gradle.kts`, package `ar.lauta.buscarpatentes`, minSdk 26, compileSdk 36, JDK 17
- [X] T002 Declarar dependencias en `app/build.gradle.kts`: Compose BOM, Room con KSP, `play-services-location`, MapLibre Native Android
- [X] T003 [P] Escribir `AndroidManifest.xml` base en `app/src/main/AndroidManifest.xml` con `Application` y actividad única
- [X] T004 [P] Crear contenedor manual de dependencias en `app/src/main/java/ar/lauta/buscarpatentes/App.kt` (D7: sin Hilt, sin Koin)
- [X] T005 [P] Configurar formato y linting en `app/build.gradle.kts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: persistencia, fotos, parseo y lectura de ubicación. Todo lo que cualquier
user story necesita antes de existir.

**⚠️ CRITICAL**: ninguna user story arranca hasta que esta fase esté completa.

- [X] T006 Definir entidad `RegistroDeCaptura` en `app/src/main/java/ar/lauta/buscarpatentes/data/Entidades.kt` con los campos de data-model.md
- [X] T007 Definir `RegistroDao` en `app/src/main/java/ar/lauta/buscarpatentes/data/Daos.kt` exponiendo **solo** `insertar`, `adjuntarFoto` y `marcarCompartida` — sin ningún update que toque latitud, longitud, precisión ni timestamp (Principio II, FR-005, FR-006)
- [X] T008 Crear la base Room en `app/src/main/java/ar/lauta/buscarpatentes/data/BaseDeDatos.kt`
- [X] T009 [P] Test que verifica que no existe forma de modificar los campos de evidencia de un registro ya creado, en `app/src/test/java/ar/lauta/buscarpatentes/InmutabilidadTest.kt` (Principio II, SC-006). Implementado como test JVM sobre el fuente del DAO, no instrumentado: las anotaciones de Room tienen retención BINARY, así que un test por reflexión pasa en vacío. Verificado verde-rojo-verde inyectando una query ofensora.
- [X] T010 [P] Implementar `AlmacenFotos` sobre almacenamiento privado en `app/src/main/java/ar/lauta/buscarpatentes/data/AlmacenFotos.kt` (D5)
- [X] T011 [P] Implementar extracción del número de 3 dígitos para formato viejo `AAA 123` y Mercosur `AB 123 CD` en `app/src/main/java/ar/lauta/buscarpatentes/domain/Patente.kt` (FR-010)
- [X] T012 [P] Test de parseo de ambos formatos, incluyendo entradas inválidas, en `app/src/test/java/ar/lauta/buscarpatentes/PatenteTest.kt` (FR-010)
- [X] T013 Implementar `LectorUbicacion` con `getCurrentLocation()` y `PRIORITY_HIGH_ACCURACY` en `app/src/main/java/ar/lauta/buscarpatentes/ubicacion/LectorUbicacion.kt` — nunca `getLastLocation()` (D3, Principio II)
- [X] T014 [P] Implementar helper de solicitud y estado de permisos en `app/src/main/java/ar/lauta/buscarpatentes/ubicacion/Permisos.kt` (C4)
- [X] T015 [P] Configurar `FileProvider` en `app/src/main/AndroidManifest.xml` y `app/src/main/res/xml/file_paths.xml` (C1)

**Checkpoint**: base lista. Las user stories pueden empezar.

---

## Phase 3: User Story 1 - Carga rápida en la calle (Priority: P1) 🎯 MVP

**Goal**: abrir la app, tipear tres dígitos, tocar `+`, y tener el registro guardado con
ubicación, precisión y timestamp propios. En 3 interacciones y menos de 10 segundos.

**Independent Test**: cerrar la app, abrirla con cronómetro, tipear un número, tocar `+`.
El campo ya estaba enfocado con el teclado arriba. El registro queda guardado. Repetir en
modo avión: se guarda igual.

- [X] T016 [US1] Crear `PantallaPrincipal` con un único campo de entrada que acepta solo el número en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (FR-017)
- [X] T017 [US1] Enfocar el campo y levantar el teclado numérico en la primera composición con `FocusRequester`, sin esperar al mapa, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (FR-016, D6)
- [X] T018 [P] [US1] Crear la fila inferior con los tres botones `+`, cámara y "Empezar recorrido" en `app/src/main/java/ar/lauta/buscarpatentes/ui/BotonesAccion.kt` (FR-036)
- [X] T019 [US1] Implementar el guardado del botón `+`: leer ubicación, armar el registro sin foto e insertarlo, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (FR-002, FR-003, FR-004, FR-018, FR-019)
- [X] T020 [US1] Marcar `precisionDegradada` cuando la precisión reportada supera el umbral, conservando el valor real sin redondear, en `app/src/main/java/ar/lauta/buscarpatentes/data/Entidades.kt` (FR-009)
- [X] T021 [US1] Propagar el fallo de persistencia como error visible, nunca como éxito silencioso, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (FR-008, Principio III)
- [X] T022 [US1] Pedir `ACCESS_FINE_LOCATION` antes de habilitar el guardado, explicando para qué se usa, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (FR-014, C4)
- [X] T023 [P] [US1] Integrar el mapa MapLibre como fondo vía `AndroidView` en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt` (FR-034, D6)
- [X] T024 [P] [US1] Definir la URL del estilo de OpenFreeMap como único punto de configuración en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt` (C5, riesgo declarado en D2)
- [X] T025 [US1] Verificar que el campo y el teclado se componen en el primer frame y que la carga del mapa nunca bloquea ni demora el guardado, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (FR-038)
- [X] T026 [P] [US1] Configurar el caché ambiente de MapLibre con tamaño máximo y desalojo LRU en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt` (FR-042, FR-043, FR-044)
- [X] T027 [US1] Permitir bajar el teclado para ver el mapa completo y volver al campo sin salir de la pantalla, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (FR-037)
- [X] T028 [US1] Verificar el camino completo de captura sin conexión siguiendo `specs/001-captura-patentes/quickstart.md` (FR-007, SC-003)
  - **Validado en dispositivo el 2026-08-28** (Xiaomi 24115RA8EG, HyperOS V816 / Android 15, sin SIM, wifi apagado, `airplane_mode_on=0`). Registro guardado: `numero=314`, `lat/lon` con siete decimales, `precisionMetros=13.104`, `precisionDegradada=0`, `capturadoEn=2026-08-28 22:20:02`. Leído directo de la base con `run-as`. Sin red de ningún tipo.
  - **Dos hallazgos del camino**, ambos ya arreglados: el teclado tapaba el campo y los botones en Android 15 (edge-to-edge forzado desde targetSdk 35 deja de achicar la ventana con `adjustResize`), y el `SnackbarHost` quedaba abajo del teclado, así que los errores de FR-008 existían pero eran invisibles.
  - **Precondición no obvia**: con el proveedor de ubicación por red deshabilitado y sin SIM, la primera captura depende de que el GNSS ya tenga efemérides. En frío no engancha en 5 s. De ahí salió el precalentado de `LectorUbicacion`.

**Checkpoint**: 🎯 **MVP funcionando.** Es un cuaderno de patentes con evidencia
automática. Ya sirve solo, sin ninguna de las historias que siguen.

---

## Phase 4: User Story 2 - Adjuntar la foto de la patente (Priority: P2)

**Goal**: el mismo número, confirmado con el botón de cámara en vez de `+`, queda
guardado con la foto adentro.

**Independent Test**: tipear un número, tocar cámara, sacar la foto. El registro queda con
foto y con la ubicación y el timestamp de la captura. Cancelar la cámara vuelve al campo
con el número tipeado y sin registro creado.

- [X] T029 [US2] Lanzar la captura de foto desde el botón de cámara en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (FR-018)
- [X] T030 [US2] Guardar la foto en almacenamiento privado y asociar su ruta al registro en `app/src/main/java/ar/lauta/buscarpatentes/data/AlmacenFotos.kt` (D5)
- [X] T031 [US2] Hacer que la foto herede la ubicación y el timestamp del registro sin redefinirlos, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (Principio II)
- [X] T032 [US2] Manejar la cancelación de la cámara: volver con el número todavía tipeado y sin crear registro, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (US2 escenario 3)
- [X] T033 [US2] Permitir adjuntar una foto a un registro ya guardado sin tocar su ubicación ni su timestamp, en `app/src/main/java/ar/lauta/buscarpatentes/data/Daos.kt` (FR-019, US2 escenario 2)
  - **Cerrado**: `PantallaBusqueda` muestra "Agregar foto" en los registros sin foto, pide el permiso de cámara al primer uso y llama a `Captura.adjuntarFotoA`, que solo toca `fotoRuta`. El escenario 2 de US2 ya es alcanzable.
- [X] T034 [P] [US2] Pedir permiso `CAMERA` al primer uso, dejando el botón `+` funcionando si se niega, en `app/src/main/java/ar/lauta/buscarpatentes/ubicacion/Permisos.kt` (C4)

**Checkpoint**: US1 y US2 funcionan de forma independiente.

---

## Phase 5: User Story 3 - Encontrar y usar la patente que toca (Priority: P3)

**Goal**: la app sabe en qué número va el juego, responde sola "¿tengo el 313?", y deja
compartir el registro al grupo.

**Independent Test**: fijar el contador en 313, cargar registros para 313, 314 y 315.
Al abrir la app se ve que el actual está cubierto y que hay 3 consecutivos adelantados,
sin buscar nada. Compartir el del 313 lo marca como compartida.

- [X] T035 [P] [US3] Definir la entidad `EstadoDelJuego` de fila única en `app/src/main/java/ar/lauta/buscarpatentes/data/Entidades.kt` (FR-020)
- [X] T036 [P] [US3] Definir `EstadoDelJuegoDao` en `app/src/main/java/ar/lauta/buscarpatentes/data/Daos.kt` (FR-020)
- [X] T037 [P] [US3] Implementar el cálculo de números consecutivos cubiertos desde el actual en `app/src/main/java/ar/lauta/buscarpatentes/domain/Cobertura.kt` (FR-022)
- [X] T038 [P] [US3] Test de consecutivos cubiertos, incluyendo huecos y archivo vacío, en `app/src/test/java/ar/lauta/buscarpatentes/CoberturaTest.kt` (FR-022)
- [X] T039 [US3] Mostrar en la pantalla principal si ya tiene un registro para el número actual, sin que el jugador busque, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (FR-021, SC-008)
- [X] T040 [US3] Mostrar cuántos números consecutivos tiene cubiertos hacia adelante en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (FR-022)
- [X] T041 [P] [US3] Distinguir en el mapa los registros pendientes de los compartidos, y los que ya tocan de los que no, en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt` (FR-035)
- [X] T042 [P] [US3] Crear la pantalla de búsqueda por número en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaBusqueda.kt` (FR-011)
- [X] T043 [US3] Mostrar un estado vacío explícito cuando no hay coincidencias, en lugar de una lista vacía, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaBusqueda.kt` (US3 escenario 6)
- [X] T044 [US3] Implementar el compartir por `ACTION_SEND` con y sin foto, usando FileProvider, en `app/src/main/java/ar/lauta/buscarpatentes/ui/Compartir.kt` (FR-012, C1)
- [X] T045 [US3] Marcar el registro como `COMPARTIDA` al lanzar el chooser en `app/src/main/java/ar/lauta/buscarpatentes/ui/Compartir.kt` (FR-015, C1)
- [X] T046 [P] [US3] Crear la pantalla de ajustes con edición del número actual del juego en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaAjustes.kt` (FR-020)
- [X] T047 [US3] Recalcular la cobertura al avanzar el contador en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaAjustes.kt` (US3 escenario 4)
- [X] T048 [P] [US3] Mostrar el espacio ocupado por el fondo de mapa guardado y permitir borrarlo, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaAjustes.kt` (FR-045)

**Checkpoint**: US1, US2 y US3 funcionan de forma independiente.

---

## Phase 6: User Story 4 - Aviso de proximidad (Priority: P4)

**Goal**: con la app cerrada y sin recorrido activo, el teléfono avisa al pasar cerca de
una patente guardada cuyo número ya toca.

**Independent Test**: guardar un registro con `+` en una ubicación conocida, poner el
contador en ese número, cerrar la app por completo, alejarse 300 m y volver caminando. La
notificación llega. Repetir después de reiniciar el teléfono: tiene que seguir llegando.

- [X] T049 [P] [US4] Implementar la selección del conjunto de geofences a registrar, dados los registros pendientes y el número actual, en `app/src/main/java/ar/lauta/buscarpatentes/domain/Avisos.kt` (D4)
- [X] T050 [P] [US4] Implementar la decisión de si un aviso corresponde, según número, estado y repetición previa, en `app/src/main/java/ar/lauta/buscarpatentes/domain/Avisos.kt` (FR-028, FR-029, FR-041)
- [X] T051 [P] [US4] Test de selección de geofences y de decisión de aviso, cubriendo los casos negativos, en `app/src/test/java/ar/lauta/buscarpatentes/AvisosTest.kt` (FR-028, FR-029, FR-041)
- [X] T052 [US4] Registrar geofences con `GeofencingClient` a 150 m, `GEOFENCE_TRANSITION_ENTER`, `NEVER_EXPIRE` e `INITIAL_TRIGGER_ENTER`, en `app/src/main/java/ar/lauta/buscarpatentes/ubicacion/Geofences.kt` (C2, FR-028)
- [X] T053 [US4] Reconciliar el conjunto de geofences al cambiar el contador, al crear un registro del número actual y al compartir uno, en `app/src/main/java/ar/lauta/buscarpatentes/ubicacion/Geofences.kt` (D4, C2)
- [X] T054 [US4] Truncar de forma determinista si el conjunto superara el límite de 100 geofences del sistema, en lugar de fallar en silencio, en `app/src/main/java/ar/lauta/buscarpatentes/ubicacion/Geofences.kt` (C2)
- [X] T055 [US4] Declarar `GeofenceReceiver` en el manifiesto, no registrarlo en código, en `app/src/main/AndroidManifest.xml` y `app/src/main/java/ar/lauta/buscarpatentes/ubicacion/GeofenceReceiver.kt` (C2, FR-039)
- [X] T056 [US4] Emitir la notificación de aviso indicando qué patente es y dónde está, en `app/src/main/java/ar/lauta/buscarpatentes/ubicacion/GeofenceReceiver.kt` (FR-028)
- [X] T057 [US4] Deduplicar los avisos: uno por registro por salida, en `app/src/main/java/ar/lauta/buscarpatentes/ubicacion/GeofenceReceiver.kt` (FR-041)
- [X] T058 [US4] Volver a registrar los geofences al arrancar la app en `app/src/main/java/ar/lauta/buscarpatentes/App.kt` (C2)
- [X] T059 [US4] Crear un receiver de `BOOT_COMPLETED` que re-registra los geofences tras reiniciar el teléfono, en `app/src/main/AndroidManifest.xml` y `app/src/main/java/ar/lauta/buscarpatentes/ubicacion/BootReceiver.kt` — sin esto el aviso deja de llegar sin ningún error visible (C2)
- [X] T060 [US4] Pedir `ACCESS_BACKGROUND_LOCATION` y `POST_NOTIFICATIONS` al activar los avisos, explicando para qué, en `app/src/main/java/ar/lauta/buscarpatentes/ubicacion/Permisos.kt` (FR-030, C4)
- [X] T061 [US4] Degradar sin romper cuando se niega el permiso de segundo plano: avisos apagados, resto de la app intacto, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaAjustes.kt` (FR-030, C4)
- [X] T062 [P] [US4] Agregar el interruptor para apagar los avisos sin perder el resto de la app, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaAjustes.kt` (FR-040)

**Checkpoint**: el ciclo del producto cierra. Guardás dónde vive una patente futura sin
foto, y la app te lleva de vuelta cuando el número llega.

---

## Phase 7: User Story 5 - Recorrido grabado (Priority: P5)

**Goal**: registrar por dónde se pasó buscando, con muestreo espaciado, y mostrar de forma
acumulada qué calles ya se exploraron.

**Independent Test**: iniciar recorrido, caminar unas cuadras con la pantalla apagada,
cargar una patente en el medio, terminar. Las calles quedan marcadas y la patente queda
asociada al recorrido. Repetir en otra zona: el mapa muestra el acumulado de las dos.

- [X] T063 [P] [US5] Definir las entidades `Recorrido` y `PuntoDeTrayecto` en `app/src/main/java/ar/lauta/buscarpatentes/data/Entidades.kt` (data-model.md)
- [X] T064 [P] [US5] Definir `RecorridoDao` y `PuntoDeTrayectoDao` en `app/src/main/java/ar/lauta/buscarpatentes/data/Daos.kt`
- [X] T065 [P] [US5] Implementar la agregación de puntos de trayecto a cobertura de calles sobre una grilla, sin geocodificación inversa, en `app/src/main/java/ar/lauta/buscarpatentes/domain/Cobertura.kt` (FR-032, Principio III)
- [X] T066 [P] [US5] Test de cobertura acumulada entre varios recorridos en `app/src/test/java/ar/lauta/buscarpatentes/CoberturaTest.kt` (FR-032, SC-015)
- [X] T067 [US5] Crear `ServicioRecorrido` como foreground service con `foregroundServiceType="location"` en `app/src/main/java/ar/lauta/buscarpatentes/ubicacion/ServicioRecorrido.kt` y `app/src/main/AndroidManifest.xml` (FR-026, C3)
- [X] T068 [US5] Configurar el muestreo en `PRIORITY_BALANCED_POWER_ACCURACY` cada 30 s o 50 m, nunca con la precisión de captura, en `app/src/main/java/ar/lauta/buscarpatentes/ubicacion/ServicioRecorrido.kt` (FR-024, FR-025, C3)
- [X] T069 [US5] Escribir cada punto a Room apenas se recibe, sin acumular en memoria, en `app/src/main/java/ar/lauta/buscarpatentes/ubicacion/ServicioRecorrido.kt` (FR-033, C3)
- [X] T070 [US5] Mostrar la notificación persistente mientras hay un recorrido en curso, en `app/src/main/java/ar/lauta/buscarpatentes/ubicacion/ServicioRecorrido.kt` (FR-023, C3)
- [X] T071 [US5] Iniciar y finalizar el recorrido desde el botón "Empezar recorrido" en `app/src/main/java/ar/lauta/buscarpatentes/ui/BotonesAccion.kt` (FR-023)
- [X] T072 [US5] Asociar al recorrido en curso las capturas hechas mientras dura, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (FR-027)
- [X] T073 [US5] Cerrar como `TERMINADO` un recorrido `EN_CURSO` cuyo servicio ya no existe al reabrir la app, conservando sus puntos, en `app/src/main/java/ar/lauta/buscarpatentes/App.kt` (FR-033, C3)
- [X] T074 [US5] Garantizar el invariante de un solo recorrido `EN_CURSO` a la vez en `app/src/main/java/ar/lauta/buscarpatentes/data/Daos.kt` (data-model.md)
- [X] T075 [P] [US5] Crear la pantalla de recorridos con las calles y patentes de cada salida en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaRecorridos.kt` (FR-031)
- [X] T076 [US5] Dibujar la cobertura acumulada de calles sobre el mapa principal en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt` (FR-032, SC-015)

**Checkpoint**: las cinco user stories funcionan de forma independiente.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T081 [P] Escribir `README.md` en la raíz con cómo compilar, instalar y correr las pruebas
- [X] T082 Auditar `app/src/main/java/ar/lauta/buscarpatentes/data/Daos.kt` y `app/src/main/java/ar/lauta/buscarpatentes/ui/` buscando cualquier ruta que permita editar ubicación, precisión o timestamp de un registro existente, y eliminarla (Principio II, SC-006)
  - **Resultado**: nada que eliminar. Los únicos `UPDATE` sobre `registro_de_captura` son `fotoRuta` y `estado`; no hay `@Update`, y ninguna pantalla arma un `copy()` de un registro guardado. `recorridoId` se fija en el insert y nunca después (FR-027). `borrar` sigue siendo la única salida para un registro equivocado, que es lo que el DAO documenta: se borra y se crea otro con la ubicación real.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias
- **Foundational (Phase 2)**: depende de Setup. **BLOQUEA todas las user stories**
- **US1 (Phase 3)**: depende de Foundational. Sin dependencias de otras historias
- **US2 (Phase 4)**: depende de Foundational. Comparte pantalla con US1 pero se prueba sola
- **US3 (Phase 5)**: depende de Foundational. Necesita registros para ser útil, no para funcionar
- **US4 (Phase 6)**: depende de Foundational y de **US3** — sin `EstadoDelJuego` no hay "número actual" contra el cual decidir un aviso. Es la única dependencia real entre historias
- **US5 (Phase 7)**: depende de Foundational. Independiente de US3 y US4
- **Polish (Phase 8)**: depende de las historias que se hayan implementado

### Dependencia cruzada, declarada

**US4 necesita US3.** El resto de las historias son independientes entre sí. Si se
implementa US4 sin US3, la selección de geofences de T049 no tiene número actual contra
el cual filtrar.

### Within Each User Story

- Entidades antes que DAOs, DAOs antes que pantallas
- Lógica de dominio y su test antes que el código de Android que la consume
- La pantalla existe antes de que se le cuelguen funciones

### Parallel Opportunities

- Setup: T003, T004 y T005 en paralelo
- Foundational: T009, T010, T011, T012, T014 y T015 en paralelo tras T008
- US1: T018, T023, T024 y T026 en paralelo — la fila de botones y el mapa no se pisan
- US3: T035 a T038, T041, T042, T046 y T048 en paralelo
- US4: T049, T050 y T051 en paralelo, todos en `domain/` antes de tocar Android
- US5: T063 a T066 en paralelo
- Con más de una persona: US2, US3 y US5 se pueden atacar en paralelo apenas termina Foundational. US4 espera a US3

---

## Parallel Example: User Story 4

```bash
# Toda la lógica pura primero, sin Android de por medio:
Task: "Selección de geofences en domain/Avisos.kt"
Task: "Decisión de aviso en domain/Avisos.kt"
Task: "Test de ambas en test/AvisosTest.kt"

# Después, en serie, el cableado con el sistema:
# T052 → T053 → T055 → T056 → T057 → T058 → T059
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup
2. Phase 2: Foundational (bloquea todo)
3. Phase 3: User Story 1
4. **PARAR Y VALIDAR**: cronómetro en mano, en la calle, y una vez en modo avión
5. A esta altura ya tenés una app usable: un cuaderno de patentes con evidencia automática

### Incremental Delivery

1. Setup + Foundational → base lista
2. US1 → probar sola → **MVP usable**
3. US2 → ahora los registros tienen foto y se pueden mandar al grupo
4. US3 → la app sabe en qué número va el juego y deja compartir
5. US4 → **acá el producto cierra**: la app te avisa al pasar cerca de la que toca
6. US5 → cobertura de calles para planificar dónde buscar

### Dónde recortar si hace falta

**US5 es la primera candidata.** Es la parte más cara —foreground service, dos entidades,
permiso de segundo plano— y es la única que no toca el objetivo declarado de encontrar
las patentes de vuelta. Está registrada como tal en Complexity Tracking de plan.md.

El corte natural del proyecto es **después de US4**: ahí el ciclo completo funciona.

---

## Notes

- `[P]` = archivos distintos, sin dependencias pendientes
- Las pruebas de T012, T038, T051 y T066 son las que exige la constitución, y cubren las
  cinco funciones puras de D8. No se agregan más salvo que una regresión real aparezca dos
  veces
- T009 es distinto: no prueba una función, prueba una **ausencia**. Que no exista forma de
  editar la evidencia es el Principio II
- T059 protege la falla más silenciosa del sistema: sin re-registro tras reiniciar, el
  aviso simplemente deja de llegar y nada muestra error
- Commitear por tarea o por grupo lógico, con Conventional Commits según la constitución
- Parar en cualquier checkpoint y validar la historia sola antes de seguir

---

## Phase 9: Convergence

Trabajo que la spec, el plan o la constitución piden y el código todavía no cubre.
Detectado evaluando el estado actual del código contra `spec.md`, `plan.md` y la
constitución, no comparando cambios.

- [X] T083 **CRITICAL** Eliminar `proximoQueFalta` de `app/src/main/java/ar/lauta/buscarpatentes/domain/Cobertura.kt`, o darle el uso real que la justifique, per Constitución IV (unrequested)
  - "Toda abstracción MUST tener al menos dos usos reales en el código antes de existir." Tiene cero: ningún archivo de `main/` la llama, solo su propia prueba. Es exactamente la abstracción especulativa que el principio prohíbe. Si no aparece un uso concreto, se borra junto con su test.

- [X] T084 Mostrar la posición actual del jugador en el mapa y encuadrar la cámara donde está, en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt`, per FR-034 (partial)
  - FR-034 pide un mapa "que muestre la posición actual del jugador y la ubicación de sus registros guardados". Los registros se dibujan; la posición no. No hay `locationComponent` ni `CameraPosition` en el archivo, así que el mapa abre en vista mundial: verificado en dispositivo el 2026-08-28, el screenshot de arranque mostraba África y Europa.
  - Encuadrar sin bloquear la captura: FR-038 sigue mandando, el mapa no puede demorar el guardado.

- [X] T085 Distinguir visualmente las zonas del mapa para las que no hay fondo guardado, en `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt`, per FR-043 y US1/AC8 (missing)
  - El caché ambiente cubre la primera mitad de FR-043 —offline se ven las calles de las zonas ya visitadas— pero no la segunda: "MUST distinguir visualmente las zonas para las que no tiene fondo". Hoy una zona sin cachear se ve como un vacío ambiguo, que es literalmente lo que US1/AC8 dice que no debe pasar.

- [X] T086 Pedir `ACCESS_BACKGROUND_LOCATION` y explicarlo antes de habilitar el recorrido, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt`, per FR-030 y US5/AC1 (partial)
  - FR-030 exige pedirlo antes de habilitar "los avisos de proximidad **o los recorridos**". La pantalla de ajustes cubre los avisos (T060, T061); el botón de recorrido solo pide `POST_NOTIFICATIONS`. Técnicamente el foreground service anda sin el permiso de segundo plano, y esa es la razón por la que quedó así, pero la spec lo pide igual y US5/AC1 lo da por otorgado.
  - Si se decide que la spec está de más acá, la salida correcta es enmendar FR-030, no dejar el hueco callado.

- [X] T087 Mostrar cuáles calles se recorrieron en una salida terminada, no solo cuántas, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaRecorridos.kt`, per FR-031 y US5/AC4 (partial)
  - US5/AC4 dice que el jugador "ve las calles que recorrió en esa salida junto con las patentes que capturó en ella". Hoy ve un número —"12 cuadras recorridas"— y la lista de patentes. El conteo no responde cuáles. La cobertura acumulada de FR-032 ya está en el mapa principal; falta la vista por salida.

- [X] T088 Acotar el espacio que ocupan las fotos, o declarar su techo de forma visible, en `app/src/main/java/ar/lauta/buscarpatentes/data/AlmacenFotos.kt` y `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaAjustes.kt`, per SC-014 (partial)
  - SC-014 pide que el espacio total "se mantenga por debajo de un límite conocido y visible, sin crecer sin control con el uso", y las Assumptions dimensionan ese límite contra las fotos y el fondo de mapa. El fondo de mapa tiene techo (`ConfigMapa.CACHE_MAXIMO_BYTES`) y las fotos no: hoy solo se muestran, y crecen sin límite.
  - Ojo con el Principio IV antes de construir purga automática: puede alcanzar con declarar el techo y avisar al llegar.

- [X] T089 Revisar `RegistroDao.borrar` en `app/src/main/java/ar/lauta/buscarpatentes/data/Daos.kt`: darle entrada desde la interfaz o quitarlo (unrequested)
  - Ninguna FR pide borrar un registro, y ninguna pantalla lo llama. El KDoc del DAO lo justifica por escrito como la única salida para un registro equivocado —"se borra y se crea otro con la ubicación real"—, que es la contracara del Principio II. Pero desde la app no se puede llegar, así que esa salida hoy no existe para el jugador.

---

## Backlog: validación de campo

Estas cuatro salieron de la Phase 8 cuando la especificación se dio por cerrada. No son
trabajo pendiente de código: son mediciones que exigen un teléfono en la mano, la calle y
tiempo de reloj —cronómetro, reporte de batería del sistema, diálogos de permisos reales,
un reinicio y una caminata de 300 metros—. Lo que cada una ya dejó medido está anotado
abajo, y el quickstart dice cómo correr el resto.

Quedan sin marcar a propósito. Darlas por hechas sin haberlas hecho sería exactamente el
éxito silencioso que el Principio III prohíbe.

- [ ] T077 [P] Medir SC-001 y SC-002 con cronómetro siguiendo `specs/001-captura-patentes/quickstart.md`
  - **Medido una vez el 2026-08-28, al filo.** Sin cronómetro: `ActivityTaskManager: Displayed ... +1s441ms` a las 22:23:57.731 contra el `capturadoEn` del registro id=3, 22:24:07.049. Del tap al registro guardado, **10,76 s**; de actividad visible a guardado, 9,32 s. SC-001 da 10.
  - Arranque en frío aparte, 3 corridas de `am start -W`: 1406 / 1421 / 1405 ms.
  - **No se da por pasada**: es una sola corrida, hecha leyendo instrucciones en pantalla en vez de tipeando de memoria, y el `capturadoEn` se sella recién cuando vuelve la lectura de GPS, así que la espera del GNSS está incluida. Faltan dos o tres corridas a velocidad natural.
  - **SC-002 pasa**: abrir, tipear, tocar `+`. Tres interacciones, las que la propia spec enumera.
- [ ] T078 [P] Medir el consumo de batería de SC-010 y SC-012 siguiendo `specs/001-captura-patentes/quickstart.md`
- [ ] T079 Validar C4 negando cada permiso uno por uno siguiendo `specs/001-captura-patentes/quickstart.md`
  - **Fila "ubicación en segundo plano" validada** el 2026-08-28. Con el permiso en `foreground` (`appops`: `COARSE_LOCATION: foreground`, `FINE_LOCATION: foreground`), `Geofences.reconciliar()` llamó a `quitarTodos()`, `shared_prefs/geofences.xml` quedó sin ids, y el resto de la app siguió funcionando: se guardó un registro en ese mismo estado. Negar degrada la función, no rompe la app.
  - **Faltan tres filas**: ubicación precisa, notificaciones y cámara. En este dispositivo `pm revoke` está bloqueado por HyperOS, así que hay que negarlas a mano en Ajustes del sistema.
- [ ] T080 Validar el caso de reinicio del teléfono siguiendo `specs/001-captura-patentes/quickstart.md` (C2)
  - **Encontró un bug real, ya arreglado.** `Geofences.reconciliar()` comparaba contra la lista de ids guardada en `SharedPreferences`, que sobrevive al reinicio; como Android borra los geofences al arrancar, la comparación no veía diferencia, `plan.sinCambios` era true y **no se registraba nada**. El `BootReceiver` cuyo KDoc dice existir para tapar la falla más silenciosa del sistema quedaba anulado por esa optimización. Arreglado con `Avisos.previosSegunElSistema` y la bandera `elSistemaLosOlvido`, con test de regresión verificado en rojo.
  - **Verificado en dispositivo tras `adb reboot`** el 2026-08-28: el proceso `ar.lauta.buscarpatentes` arrancó solo ~90 s después del arranque, sin que nadie abriera la app (`pidof` devuelve pid, no hay `Displayed` en logcat, el foco estaba en otra app). El camino de reconciliación forzada corrió.
  - **Queda abierta igual**: falta lo que el quickstart pide de verdad —alejarse 300 m y volver caminando para ver llegar la notificación—. El estado de geofences de Play Services no se puede leer desde afuera, así que que el proceso despierte es condición necesaria, no suficiente.
  - **Dato de comportamiento**: `BOOT_COMPLETED` no llega hasta el primer desbloqueo, porque la app no es direct-boot. Un teléfono reiniciado y dejado bloqueado no avisa nada hasta que alguien lo desbloquea.
