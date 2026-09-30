---

description: "Tareas de la 007: caminos sin diagonales"
---

# Tasks: Caminos sin diagonales

**Input**: Design documents from `/specs/007-caminos-sin-diagonales/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/camino-ajustado.md](./contracts/camino-ajustado.md)

**Tests**: sí. La constitución pide prueba para la lógica pura que se rompe en silencio, y el
[quickstart](./quickstart.md) §1 las lista. Van antes de la implementación que prueban.

**Organization**: por user story. Todo el código vive en `shared/src/commonMain` y las pruebas en
`shared/src/commonTest`, bajo `kotlin/ar/lauta/buscarpatentes/`. Abajo se abrevian como `common/` y
`commonTest/`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)

---

## Phase 1: Setup

**Purpose**: la respuesta real del servicio que usan las pruebas.

- [X] T001 **Hecha**, con un cambio: la respuesta es la de la caminata entera **en un solo pedido**, como pedía la app antes de la 007. Pedidos por separado, cada tramo trae puntos sin emparejar pero ningún corte entre pedazos, y la prueba necesita las dos cosas. Que la caminata se parta en dos tramos de señal se prueba sin respuesta. Crear `commonTest/ubicacion/RespuestaDeAjuste.kt` con dos constantes:
  - **La caminata**: los puntos de la caminata inventada de D1 ([research.md](./research.md)), cerca del Obelisco. Son dos tramos en línea recta, de 13 y 11 puntos, con un salto de unos 680 m entre ellos.
  - **La respuesta**: la respuesta del servicio a **cada tramo por separado**, recortada a `shape`, `edges[].begin_shape_index`, `edges[].end_shape_index`, `matched_points[].type` y `matched_points[].edge_index`.
  Se genera corriendo el pedido de A2 ([contrato](./contracts/camino-ajustado.md)) desde la PC. **Solo coordenadas cercanas al Obelisco**: nada del jugador. Si una de las respuestas no trae un corte de pedazos ni puntos `unmatched`, correr los puntos hasta que los traiga: la prueba de T008 necesita las dos cosas.

---

## Phase 2: Foundational

**Purpose**: leer y escribir el formato nuevo, y dibujar por tramos. Lo usan las dos stories.

**⚠️ CRITICAL**: T004 cambia `Trazo`, que usan el mapa principal y el detalle.

- [X] T002 **Hecha**. [P] En `common/domain/Polilinea.kt`, sumar `fun codificar(puntos: List<Pair<Double, Double>>, precision: Double = PRECISION_VALHALLA): String`, el inverso de `decodificarUna`: diferencias acumuladas desde cero, zigzag y trozos de 5 bits. En `commonTest/domain/PolilineaTest.kt`, sumar la ida y vuelta: codificar y decodificar dan las mismas posiciones redondeadas a 1e6, con coordenadas negativas y una lista vacía.
- [X] T003 **Hecha**. `CaminoGuardado` suma `dibujo(medidos)`: los tramos guardados si está ajustado, y si no `Geo.tramos(medidos)`. Lo usan las dos pantallas. Crear `common/domain/CaminoAjustado.kt` con `sealed interface CaminoGuardado` y sus tres casos: `Pendiente`, `NoSePudo` y `Ajustado(tramos: List<List<Pair<Double, Double>>>)`. Suma dos funciones:
  - **`leer(texto: String?)`**: `null` o sin prefijo `2:` da `Pendiente`; `2:` solo da `NoSePudo`; `2:` con tramos da `Ajustado`, con cada tramo decodificado por su lado y descartando los de menos de 2 posiciones.
  - **`escribir(tramos)`**: `2:` más los tramos codificados con `Polilinea.codificar` y unidos con `;`. Una lista vacía da `2:`.
  Crear `commonTest/domain/CaminoAjustadoTest.kt` con la lectura de cada caso del contrato A1 (un camino viejo de una pierna y otro de dos se leen `Pendiente`) y la ida y vuelta de `escribir` y `leer`. Depende de T002.
- [X] T004 **Hecha**. También la salida en curso de la pantalla principal, que al crecer reemplaza sus tramos con `Geo.tramos`. Pasar `Trazo`, en `common/mapa/Colecciones.kt`, a `Trazo(recorridoId, tramos: List<List<Pair<Double, Double>>>, escalon)`.
  - **Se va**: `puntos`, `ajustado` y `tramosDe`.
  - **El dibujo**: `coleccionTrazos` dibuja una línea por tramo, y `coleccionHuecos` un segmento punteado del final de cada tramo al principio del siguiente, con `Geo.huecos(trazo.tramos)`.
  - **Los que lo arman**:
    - en `common/ui/PantallaPrincipal.kt`, los tramos salen de `CaminoGuardado.leer(salida.caminoAjustado)`: si es `Ajustado`, sus tramos; si no, `Geo.tramos(puntos medidos)`;
    - `common/ui/PantallaRecorridos.kt` (`DetalleDeSalida`) hace lo mismo por ahora;
    - el encuadre de `common/mapa/MapaDeFondo.kt` usa `tramos.flatten()`.
  - **Pruebas**: actualizar `commonTest/mapa/ColeccionTest.kt`. Los casos de `ajustado = true` pasan a trazos de varios tramos: una línea por tramo y un hueco entre cada par, sin importar de dónde vinieron.
  Depende de T003.

**Checkpoint**: compila y pasan las pruebas. El mapa ya no dibuja caminos viejos: sin ningún `2:` en la base, todo se ve con los puntos medidos. Es el estado "mientras no se reajusten" de FR-004.

---

## Phase 3: User Story 1 - El camino de una salida sigue las calles de punta a punta (Priority: P1) 🎯 MVP

**Goal**: el camino ajustado sin rectas inventadas, los cortes de señal punteados en las dos vistas, y las salidas viejas reajustadas solas.

**Independent Test**: quickstart §2 y §3: actualizar, abrir con conexión, y recorrer con la vista cada salida del mapa sin encontrar ninguna recta que cruce una manzana.

### Tests for User Story 1

- [X] T005 **Hecha**. Los casos se comparan contra la lista exacta que tiene que salir; el chequeo de la garantía de A4 va sobre la respuesta real, en T006. [P] [US1] En `commonTest/domain/CaminoAjustadoTest.kt`, sumar las pruebas de `CaminoAjustado.armar` (contrato A4), con casos armados a mano:
  - dos pedazos con puntos sueltos en el medio;
  - dos pedazos **sin** puntos sueltos en el medio;
  - puntos sueltos al principio y al final;
  - ningún punto emparejado, que devuelve los medidos tal cual;
  - un punto `interpolated`, que cuenta como emparejado.
  En todos, un chequeo común de la garantía de A4: cada par de posiciones seguidas del tramo armado es de un mismo pedazo de `shape` o son dos puntos medidos consecutivos.
- [X] T006 **Hecha**. Un empalme entre un punto medido y su pedazo mide como mucho 30 m, el radio máximo del pedido; nada más largo que eso une dos pedazos. [P] [US1] En `commonTest/ubicacion/AjustarACallesTest.kt`, sumar la lectura de las respuestas de T001 con la función de T008. Con el armado de cada tramo:
  - se cumple la garantía de A4;
  - el tramo arranca en el primer punto medido o en su emparejado, y termina en el último punto medido o en su emparejado: lo que no emparejó no desaparece.
  Con la caminata entera: `Geo.tramos` da dos tramos de señal, y el camino guardado tiene dos tramos, con un hueco entre ellos.

### Implementation for User Story 1

- [X] T007 **Hecha**. Los extremos sueltos incluyen al punto emparejado de al lado, así el empalme con el pedazo es siempre el de un punto con su lugar en la calle. [US1] En `common/domain/CaminoAjustado.kt`, implementar `fun armar(medidos: List<Pair<Double, Double>>, forma: List<Pair<Double, Double>>, aristas: List<IntRange>, emparejados: List<Int?>): List<Pair<Double, Double>>` según el contrato A4:
  - `aristas[i]` va de `begin_shape_index` a `end_shape_index`;
  - `emparejados[i]` es el `edge_index` del punto medido `i`, o `null` si vino `unmatched`;
  - `forma` se parte en pedazos donde una arista no empieza en el índice donde terminó la anterior;
  - los empalmes entre pedazos van con los puntos medidos, del último del pedazo anterior al primero del siguiente, inclusive.
  Tienen que pasar T005.
- [X] T008 **Hecha**, con un matiz sobre A2: si el servicio **rechaza** un tramo (no contesta 200, o contesta algo que no se entiende), ese tramo va con sus puntos medidos y el resto se guarda igual. Un tramo corto en una plaza no puede dejar sin ajustar la salida entera. Si rechaza todos, se reintenta, porque lo más probable es que esté caído. Sin red, se reintenta como siempre. [US1] En `common/ubicacion/AjustarACalles.kt`:
  - **Los pedidos**: `pendientes()` corta los puntos de cada salida con `Geo.tramos` y hace **un pedido por tramo de señal** de 2 puntos o más.
  - **La respuesta**: una función `internal` lee cada respuesta: `shape` con `Polilinea.decodificar`, `edges` y `matched_points` con kotlinx-serialization. Si un campo no está o no se entiende, la respuesta no sirve.
  - **Lo que se guarda**:
    - con **una** respuesta que falle o no sirva, no se guarda nada de esa salida y queda para la próxima (FR-032 y FR-035 de la 003);
    - si **ningún** punto de ningún tramo emparejó, se guarda `2:` (`NoSePudo`);
    - si no, se guarda `CaminoGuardado.escribir(tramos armados)`.
  - **Se va**: `pedirCamino`, y con él la lectura de `shape` sola. El cuerpo de cada pedido sigue siendo `cuerpo(puntos)`.
  Tiene que pasar T006.
- [X] T009 **Hecha**. `guardarCaminoAjustado` sigue siendo el único que escribe la columna. [US1] En `common/data/Daos.kt`, `sinAjustar()` pasa a `SELECT * FROM recorrido WHERE estado = 'TERMINADO' AND (caminoAjustado IS NULL OR caminoAjustado NOT LIKE '2:%')`, con el comentario de que un camino viejo vuelve a la cola sin nada que lo administre (D3). Revisar que `guardarCaminoAjustado` siga siendo el único que escribe la columna.
- [ ] T010 [US1] **Verificar**:
  - `./gradlew.bat :shared:testAndroidHostTest assembleDebug`;
  - compilar el iPhone en la nube (`gh workflow run ios.yml --ref 007-caminos-sin-diagonales`);
  - APK a `Download/buscar-patentes.apk`, pisando el anterior, e `.ipa` al usuario;
  - el usuario corre el quickstart §2 y §3 en los dos teléfonos. Anotar acá cuántas salidas se reajustaron y si quedó alguna diagonal.

**Checkpoint**: el mapa sin diagonales en los dos teléfonos. Es entregable sin la US2.

---

## Phase 4: User Story 2 - Ver lo que midió el teléfono o lo que interpretó el ajuste (Priority: P2)

**Goal**: en el detalle de una salida, un interruptor entre el camino ajustado y los puntos medidos.

**Independent Test**: quickstart §4: abrir una salida ajustada, alternar sin que se mueva la cámara, y abrir una en curso.

### Implementation for User Story 2

- [X] T011 **Hecha**, antes de verificar la US1: así la compilación del iPhone y la prueba en la calle cubren las dos stories de una vez. [US2] En `DetalleDeSalida`, en `common/ui/PantallaRecorridos.kt`, sumar sobre el mapa una fila con un texto y un `Switch` (D5 de [research.md](./research.md)):
  - `val camino = CaminoGuardado.leer(recorrido.caminoAjustado)`, y `var verAjustado by remember(recorrido.id) { mutableStateOf(true) }`;
  - **con `Ajustado`**: el `Switch` prendido dice "Ajustado a las calles" y dibuja los tramos guardados; apagado dice "Lo que midió el teléfono" y dibuja `Geo.tramos(puntos medidos)`;
  - **con `Pendiente`**, que incluye una salida en curso: el `Switch` apagado y deshabilitado, con "Todavía sin ajustar";
  - **con `NoSePudo`**: lo mismo, con "No se pudo ajustar";
  - **la cámara no se mueve** al cambiar (FR-008): `MapaDeFondo` encuadra una sola vez. Si en el teléfono se mueve, no volver a encuadrar cuando cambian los trazos;
  - la fila no aparece si la salida no tiene puntos. Ahí sigue el texto de hoy.
- [ ] T012 [US2] **Verificar**: pruebas y APK; compilar el iPhone; el usuario corre el quickstart §4 en los dos teléfonos.

**Checkpoint**: las dos stories andando en los dos teléfonos.

---

## Phase 5: Polish & Cross-Cutting Concerns

- [X] T013 **Hecha**. [P] En `TODO.md`, tachar los dos ítems con "(hecho en la 007)": el de las diagonales y el del interruptor entre los puntos reales y el camino ajustado.
- [X] T014 **Hecha**. [P] En `README.md`, sumar `CaminoAjustadoTest` a la tabla de pruebas: el armado del camino ajustado sin rectas inventadas y el formato guardado (contrato A de la 007). Actualizar la fila de `ColeccionTest`: los trazos por tramos, vengan de donde vengan.
- [ ] T015 Cierre:
  - correr el quickstart §5 de la 007: un respaldo del Android restaurado en el iPhone reajusta las salidas viejas;
  - revisar con el quickstart §8 de la 006 que no se suba nada del jugador (coordenadas, imágenes, emails);
  - abrir el PR a `main` con el resumen. El merge lo hace el usuario.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (T001)**: sin dependencias. Solo lo necesita T006.
- **Foundational (T002 → T003 → T004)**: en ese orden. Bloquea las dos stories.
- **US1 (T005 a T010)**: después de Foundational.
- **US2 (T011, T012)**: después de Foundational. No necesita la US1 para funcionar: sin caminos `2:` todo dice "Todavía sin ajustar", pero para verla con datos conviene hacerla después.
- **Polish (T013 a T015)**: al final.

### Within User Story 1

- T005 antes de T007; T006 antes de T008 (las pruebas primero).
- T007 antes de T008: el ajuste usa `armar`.
- T009 es independiente de T007 y T008 (otro archivo), pero sin T008 la cola vuelve a pedir lo que ya estaba.

### Parallel Opportunities

- T001 y T002 en paralelo.
- T005 y T006, pruebas en archivos distintos.
- T013 y T014.

---

## Parallel Example: User Story 1

```text
Task: "T005 Pruebas de CaminoAjustado.armar en commonTest/domain/CaminoAjustadoTest.kt"
Task: "T006 Lectura de la respuesta real en commonTest/ubicacion/AjustarACallesTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. T001 a T004: el formato y el dibujo por tramos.
2. T005 a T009: el ajuste sin diagonales y el reajuste de lo viejo.
3. **STOP and VALIDATE** con T010: el mapa sin diagonales en los dos teléfonos.

### Incremental Delivery

1. Foundational: el mapa se ve con los puntos medidos, que ya salen "perfectos".
2. US1: el ajustado vuelve, sin diagonales. Se puede entregar acá.
3. US2: el interruptor del detalle.

---

## Notes

- Cada tarea termina con `./gradlew.bat :shared:testAndroidHostTest assembleDebug` en verde y un commit.
- El iPhone se compila en la nube solo en los puntos de verificación (T010, T012), porque cada compilación tarda unos 20 minutos.
- Nada de datos del jugador en el repositorio: las pruebas usan solo coordenadas del Obelisco.
