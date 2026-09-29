---

description: "Task list for 004-planear-recorridos"
---

# Tasks: Planear el próximo recorrido

**Input**: Design documents from `/specs/004-planear-recorridos/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/mapa-y-salidas.md](./contracts/mapa-y-salidas.md)

**Tests**: la spec no pide TDD. Las únicas pruebas de esta lista son las dos que la
constitución obliga —la lógica no trivial deja prueba ejecutable en JVM— más la actualización
de las que ya existen y quedan rotas por el cambio de forma de `Trazo`.

**Organization**: las tareas van agrupadas por user story. Cada story se puede terminar,
instalar en el teléfono y juzgar sola.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede hacer en paralelo (archivo distinto, sin depender de nada incompleto)
- **[Story]**: a qué user story pertenece (US1, US2, US3, US4)
- Cada tarea nombra el archivo exacto

## Path Conventions

Un solo módulo Android. Código en `app/src/main/java/ar/lauta/buscarpatentes/`, pruebas en
`app/src/test/java/ar/lauta/buscarpatentes/`, recursos en `app/src/main/res/`.

---

## Phase 1: Setup

**Purpose**: red de seguridad antes de tocar la base, y línea de base en verde.

- [X] T001 Correr `scripts/respaldo.ps1` para respaldar la base del teléfono antes de la migración v5 → v6. Es la primera migración de esta feature y corre contra patentes reales
- [X] T002 Correr `./gradlew test` y anotar que pasa. Todo lo que se rompa después de acá lo rompió esta feature

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: la preferencia persistida, la función de antigüedad y el cambio de forma de
`Trazo`. US1 y US2 dependen de las tres.

**⚠️ CRITICAL**: ninguna user story puede empezar hasta que esta fase esté completa.

- [X] T003 Agregar `enum class ModoMapa { COBERTURA, ANTIGUEDAD, APAGADO }`, el campo `modoMapa: ModoMapa = ModoMapa.COBERTURA` en `EstadoDelJuego` y sus dos convertidores en `app/src/main/java/ar/lauta/buscarpatentes/data/Entidades.kt`, siguiendo el patrón de `EstadoRegistro` y `EstadoRecorrido`
- [X] T004 Agregar `MIGRACION_5_6` con `ALTER TABLE estado_del_juego ADD COLUMN modoMapa TEXT NOT NULL DEFAULT 'COBERTURA'`, subir `version` a 6 y registrarla en `addMigrations` en `app/src/main/java/ar/lauta/buscarpatentes/data/BaseDeDatos.kt` (depende de T003)
- [X] T005 [P] Agregar `fijarModoMapa(modo: String, id: Int = EstadoDelJuego.ID_UNICO)` como `@Query` acotado a esa columna en `EstadoDelJuegoDao`, en `app/src/main/java/ar/lauta/buscarpatentes/data/Daos.kt`, con la forma de `fijarAvisos`
- [X] T006 [P] Crear `app/src/main/java/ar/lauta/buscarpatentes/domain/Antiguedad.kt` con `enum class Escalon { RECIENTE, MEDIO, VIEJO }` y la función pura que va de `(finalizadoEn, iniciadoEn, ahora)` al escalón, con los cortes en 3 y 14 días (FR-014, D9). Sin dependencias de Android: se prueba en JVM
- [X] T007 [P] Crear `app/src/test/java/ar/lauta/buscarpatentes/domain/AntiguedadTest.kt` cubriendo los dos bordes exactos (3 días y 14 días), una salida en curso —sin `finalizadoEn`—, una de hace un año, y una fecha futura por reloj corrido, que cae en `RECIENTE` y no rompe
- [X] T008 En `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt`: borrar `Trazo.destacado`, agregar `Trazo.escalon: Escalon`, y que `coleccionTrazos` y `coleccionHuecos` emitan `PROP_ESCALON` en lugar de `PROP_DESTACADO`. `pintarTrazos` queda pintando un color literal para que el proyecto compile; el color y los modos llegan en T010
- [X] T009 Actualizar `app/src/test/java/ar/lauta/buscarpatentes/mapa/ColeccionTest.kt`: reemplazar `destacado` por `escalon` en las diez construcciones de `Trazo`, y cambiar la prueba `el destacado viaja en el feature` por una que verifique que el escalón viaja (depende de T008)

**Checkpoint**: `./gradlew test` en verde, la app compila, la migración corre. Nada cambió a la vista todavía.

---

## Phase 3: User Story 1 - Ver de un vistazo qué calles me faltan (Priority: P1) 🎯 MVP

**Goal**: el histórico entero pintado igual y fuerte, en azul de ruta, con un interruptor que
lo apaga y lo prende y recuerda la elección.

**Independent Test**: con dos o más salidas guardadas, las dos se dibujan idénticas; el
interruptor las quita y las devuelve; cerrar y abrir la app conserva el estado.

- [X] T010 [US1] En `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt`: `MapaDeFondo` recibe `modo: ModoMapa`; `pintarTrazos` aplica color literal azul en cobertura y `visibility = NONE` en apagado sobre las capas de trazo y de huecos; `actualizarCapas` reaplica al cambiar de modo sin reconstruir la fuente (D4). La rama de antigüedad llega en T019
- [X] T011 [P] [US1] En `app/src/main/java/ar/lauta/buscarpatentes/ui/Tema.kt`: `ColoresDeMapa.RECORRIDO` pasa al azul de ruta (`#1A73E8` como punto de partida, calibrable) y se borra `RECORRIDO_VIEJO`
- [X] T012 [P] [US1] Agregar los iconos del interruptor en `app/src/main/res/drawable/`, uno por estado, con el formato de los `ic_*.xml` que ya están
- [X] T013 [US1] En `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt`: armar los `Trazo` sin `destacado`, con su `escalon` calculado por `Antiguedad`, y **ordenados de la salida más vieja a la más nueva** (D5, FR-013). Borrar el cálculo de `aDestacar`
- [X] T014 [US1] En `PantallaPrincipal.kt`: leer `modoMapa` del estado del juego al entrar y pasárselo a `MapaDeFondo`
- [X] T015 [US1] En `PantallaPrincipal.kt`: agregar el botón del interruptor a la franja de acciones, junto a Buscar / Salidas / Ajustes (D7). Cicla cobertura → apagado → cobertura por ahora, y persiste con `fijarModoMapa`. **Sin achicar el campo de número** (contrato C4)
- [X] T016 [US1] Crear `app/src/main/java/ar/lauta/buscarpatentes/ui/LeyendaDelMapa.kt`: la línea que se apoya arriba de la franja de acciones y nombra el modo. Cobertura y apagado en esta story; los escalones llegan en T021
- [X] T017 [US1] Agregar a `app/src/test/java/ar/lauta/buscarpatentes/mapa/ColeccionTest.kt` una prueba de que `coleccionTrazos` conserva el orden de entrada. Es lo único de D5 que se puede probar en JVM; el resto se mira en el teléfono

**Checkpoint**: el mapa muestra todo el histórico en azul, sin destacar nada, y el interruptor lo apaga y lo prende. La feature ya sirve para planear.

---

## Phase 4: User Story 2 - Saber hace cuánto no paso por una calle (Priority: P2)

**Goal**: un tercer estado del interruptor que pinta los caminos según hace cuánto se
caminaron, con lo viejo resaltando y una leyenda que dice los plazos.

**Independent Test**: con salidas de hace 1, 10 y 60 días, las tres caen en escalones
distintos y la más vieja es la que más resalta; dos de hace 5 y 12 días se ven iguales.

- [X] T018 [P] [US2] En `app/src/main/java/ar/lauta/buscarpatentes/ui/Tema.kt`: agregar los tres colores de la escala de antigüedad —gris apagado, violeta medio, magenta fuerte (D11)—. Ninguno puede ser el azul de cobertura (FR-012b)
- [X] T019 [US2] En `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt`: agregar la rama de antigüedad al color de la capa de trazos, como `Expression.step` sobre `PROP_ESCALON`. Tres escalones netos, nunca un gradiente (FR-015)
- [X] T020 [US2] En `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt`: el ciclo del interruptor pasa a tres estados —cobertura → antigüedad → apagado— (FR-002)
- [X] T021 [US2] En `app/src/main/java/ar/lauta/buscarpatentes/ui/LeyendaDelMapa.kt`: en modo antigüedad la línea muestra los tres escalones, cada uno con su color y su plazo escrito: hasta 3 días · 4 a 14 días · más de 14 días (FR-014a, contrato C2)

**Checkpoint**: tres toques recorren los tres modos, y en antigüedad el mapa dice qué hace más que no se pisa.

---

## Phase 5: User Story 3 - Una pantalla de Salidas que se pueda recorrer (Priority: P2)

**Goal**: una lista de filas compactas con el "hace cuánto" arriba, y el detalle de una salida
en su propia pantalla con el mapa a tamaño real.

**Independent Test**: con diez salidas, la lista entra en menos de dos pantallas de alto; abrir
una salida muestra su mapa completo y el gesto de retroceso vuelve a la lista.

- [X] T022 [US3] Agregar a `app/src/main/java/ar/lauta/buscarpatentes/domain/Antiguedad.kt` la función que formatea un lapso como tiempo transcurrido —"hace 3 días"— (FR-019), y cubrirla en `AntiguedadTest.kt` junto a los escalones: mismo dominio, misma prueba
- [X] T023 [US3] En `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaRecorridos.kt`: `FilaDeSalida` pasa a ser compacta y de altura pareja —cuándo fue, cuánto duró, cuántas patentes—. **Sacar de la fila el `MapaDeFondo` embebido y la línea de patentes separadas por comas** (FR-017, FR-018; retira el FR-010a de la 003)
- [X] T024 [US3] En `PantallaRecorridos.kt`: encabezado que dice hace cuánto fue la última salida, visible sin tocar nada (FR-020). **Sin ningún total acumulado** (FR-020a)
- [X] T025 [US3] En `PantallaRecorridos.kt`: el estado `abierta: Long?` deja de expandir una fila y pasa a conmutar entre la lista y la sub-pantalla de detalle, con su propio `BackHandler` que vuelve a la lista (D10, contrato C5)
- [X] T026 [US3] En `PantallaRecorridos.kt`: el detalle dibuja el camino de esa salida en un mapa a pantalla, con `seguirAlJugador = false` y encuadre del camino completo. Una salida sin puntos lo dice y no abre mapa vacío (FR-021, FR-022, conserva el FR-010b de la 003)
- [X] T027 [US3] En `PantallaRecorridos.kt`: la lista de patentes del detalle se puede recorrer y cada una abre `FichaDeRegistro`, en lugar de volcarse como texto separado por comas (FR-023)
- [X] T028 [US3] En `PantallaRecorridos.kt`: mover la acción de borrar salida al detalle. Conserva la confirmación, no toca ninguna patente, y al confirmar vuelve a la lista (FR-022)

**Checkpoint**: Salidas se puede recorrer, y el mapa de una salida se ve entero.

---

## Phase 6: User Story 4 - Un mapa donde el color dice una sola cosa (Priority: P3)

**Goal**: pines blancos con el número en negro, el borde diciendo la probabilidad, la que toca
destacada por tamaño, y el color de estado retirado del mapa.

**Independent Test**: una patente compartida y una pendiente se ven iguales; tres
probabilidades distintas se distinguen por el borde; el número se lee encima del trazo en claro
y en oscuro.

- [X] T029 [P] [US4] En `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt`: función que dibuja con `android.graphics.Canvas` el bitmap de un pin —silueta con punta, relleno blanco, borde del color que se le pase— y devuelve las tres variantes, una por escalón de probabilidad (D1). Sin dependencias nuevas
- [X] T030 [US4] En `Mapa.kt`: registrar las tres imágenes en el estilo con `style.addImage` al cargar, junto al resto del armado de capas (depende de T029)
- [X] T031 [US4] En `Mapa.kt`: reemplazar la `CircleLayer` de marcadores sueltos por una `SymbolLayer` con `iconImage` elegido por expresión según el escalón de probabilidad, `iconAnchor(BOTTOM)` para que la punta caiga sobre la coordenada, y el filtro `not(esGrupo())` que ya está (FR-028, FR-030)
- [X] T032 [US4] En `Mapa.kt`: el número pasa a negro con `textOffset` **en ems** hacia arriba para caer en la cabeza del pin (D2). Conservar `textFont("Noto Sans Regular")`, `textAllowOverlap` y `textIgnorePlacement`: sin el primero se cae el teselado de la fuente entera
- [X] T033 [US4] En `Mapa.kt`: la patente del número actual se destaca por `iconSize` y `textSize` mayores vía expresión sobre `toca`, más `symbolSortKey` para que quede dibujada encima de las que se le superpongan (FR-030d, D3). **Sin color**
- [X] T034 [US4] En `Mapa.kt`: borrar `claseDe`, `colorPorClase`, la constante `PROP_CLASE` y el campo `Marcador.compartida` (FR-030a)
- [X] T035 [P] [US4] En `app/src/main/java/ar/lauta/buscarpatentes/ui/Tema.kt`: borrar `ColoresDeMapa.PENDIENTE`, `TOCA` y `COMPARTIDA`. Los tres de confianza se quedan: ahora pintan el borde del pin
- [X] T036 [US4] Sacar el argumento `compartida =` de las dos construcciones de `Marcador`, en `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` y `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaRecorridos.kt` (depende de T034)
- [X] T037 [US4] En `Mapa.kt`: recalibrar `clusterRadius` al ancho real del pin. Estaba en 28 px porque era el diámetro del círculo, para que dos registros se agrupen justo cuando se taparían (D12). Los grupos siguen siendo círculos y no se tocan
- [X] T038 [US4] En `app/src/test/java/ar/lauta/buscarpatentes/mapa/ColeccionTest.kt`: borrar las aserciones sobre la propiedad `clase`, que quedan sin objeto. La prueba de la probabilidad se queda

**Checkpoint**: el mapa tiene un solo código de color por elemento.

---

## Phase 7b: User Story 5 - Mirar el mapa sin las patentes encima (Priority: P2)

**Goal**: un botón que esconde y trae los marcadores, con la leyenda diciéndolo.

**Independent Test**: un toque deja el trazo solo, otro devuelve los pines, y reabrir la app los
muestra.

- [X] T044 [P] [US5] Crear `app/src/main/res/drawable/ic_patentes.xml` e `ic_patentes_ocultas.xml`: un pin al trazo blanco y el mismo con una barra encima, en el lenguaje de `ic_cobertura` / `ic_recorridos_ocultos`
- [X] T045 [US5] En `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt`: estado `patentesVisibles` con `remember` —**sin persistir**, FR-035a— y el botón en la franja, a la derecha del interruptor de recorridos (FR-033, FR-033a)
- [X] T046 [US5] En `PantallaPrincipal.kt`: `MapaDeFondo` recibe `emptyList()` cuando están escondidas. Sin tocar `Mapa.kt`: la lista vacía ya es un estado que sabe dibujar (FR-033)
- [X] T047 [US5] En `app/src/main/java/ar/lauta/buscarpatentes/ui/LeyendaDelMapa.kt`: parámetro `patentesVisibles` y la línea "· Patentes ocultas" mientras lo están (FR-035)

**Checkpoint**: el mapa se puede mirar con los recorridos solos, y la leyenda nunca deja dudas de
por qué está vacío.

---

## Phase 7c: User Story 6 - La misma patente vista dos veces no es dos patentes (Priority: P1)

**Goal**: cargar un número ya anotado a menos de diez metros sube su confianza en vez de apilar
un pin.

**Independent Test**: dos cargas del mismo número sin moverse dejan un registro y una confianza
más alta.

- [X] T048 [P] [US6] En `app/src/main/java/ar/lauta/buscarpatentes/domain/Geo.kt`: `masCercano` —el candidato más próximo dentro de un radio, o null—. Genérico con selector de posición para que el dominio no importe las entidades de la base, como ya hace `Avisos` (FR-036, FR-036a)
- [X] T049 [P] [US6] En `app/src/test/java/ar/lauta/buscarpatentes/domain/GeoTest.kt`: los cuatro casos de `masCercano` —dentro del radio, a unas cuadras, dos candidatos gana el más cercano, lista vacía—. Es la prueba ejecutable que la constitución exige para lógica no trivial
- [X] T050 [US6] En `app/src/main/java/ar/lauta/buscarpatentes/ui/Captura.kt`: la constante `RADIO_MISMA_PATENTE_M = 10.0`, en un solo lugar y comentada como calibración (FR-039c)
- [X] T051 [US6] En `Captura.kt`: antes de insertar, buscar con `registros.porNumero` + `Geo.masCercano`. Si hay uno, no se inserta (FR-036, FR-037, FR-038)
- [X] T052 [US6] En `Captura.kt`: `confirmar` —voto de +1 con la hora de la carga, foto adjuntada solo si el registro no tenía, y mensaje con la confianza resultante (FR-037a, FR-039, FR-039b)
- [X] T053 [US6] Verificar que `confirmar` no reconcilia geofences: el conjunto de registros no cambió, y reconciliar sería trabajo tirado en el camino de los diez segundos

**Checkpoint**: el mapa deja de acumular pines encimados, y la probabilidad del borde vuelve a
decir la verdad.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T039 [P] Verificar que `app/src/test/java/ar/lauta/buscarpatentes/InmutabilidadTest.kt` sigue en verde sin cambios: esta feature no escribe sobre evidencia y esa prueba lo tiene que seguir demostrando
- [X] T040 [P] Actualizar `README.md` si describe el aspecto del mapa o los estados del marcador
- [X] T041 Verificar el techo del Principio I: la carga rápida sigue entrando en cuatro interacciones y el campo de número no se achicó por el botón nuevo (contrato C4). Si no entran los cuatro botones, **sale el interruptor de la franja, no el ancho del campo**
- [X] T042 Correr `./gradlew test` completo y comparar contra la línea de base de T002
- [X] T042b Verificar que `InmutabilidadTest` sigue en verde después de la US6: `confirmar` no agrega ninguna escritura sobre campos de evidencia
- [ ] T043 Recorrer [quickstart.md](./quickstart.md) entero en el teléfono, incluidos los bloques 4, 5, 6b y 6c, que son los que se juzgan al sol y no en el escritorio

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias
- **Foundational (Phase 2)**: depende de Setup. **Bloquea a US1 y US2**; US3 y US4 no la necesitan
- **US1 (Phase 3)**: depende de Foundational
- **US2 (Phase 4)**: depende de Foundational y de US1 — reusa la capa y el interruptor que US1 construye
- **US3 (Phase 5)**: **independiente**. Solo toca `PantallaRecorridos.kt` y `Antiguedad.kt`
- **US4 (Phase 6)**: **independiente** de US1 y US2 en lo funcional, pero conviene después: es lo que hace que el pin no compita con el azul nuevo
- **Polish (Phase 7)**: depende de las stories que se hayan hecho

### User Story Dependencies

- **US1 (P1)**: la única que depende de la base y el dominio nuevos. Es el MVP
- **US2 (P2)**: **depende de US1**. Es un tercer estado del interruptor que US1 construye, no una pieza aparte. Es la única dependencia entre stories de esta feature
- **US3 (P2)**: independiente de todas. Se puede hacer primero si se quiere
- **US4 (P3)**: independiente. Toca solo las capas de marcadores de `Mapa.kt` y dos call sites

### Parallel Opportunities

- **Phase 2**: T005, T006 y T007 en paralelo entre sí. T003 antes que T004
- **Phase 3**: T011 y T012 en paralelo con T010
- **Phase 6**: T029 y T035 en paralelo
- **Entre stories**: US3 y US4 se pueden hacer en paralelo con US1/US2 — tocan archivos distintos. La única colisión es `PantallaRecorridos.kt`, que US3 reescribe y T036 de US4 toca en una línea: hacer T036 después de T023

---

## Parallel Example: Phase 2

```bash
# Después de T003 y T004, estas tres van juntas:
Task: "fijarModoMapa en data/Daos.kt"
Task: "domain/Antiguedad.kt con Escalon y la función de escalones"
Task: "test/domain/AntiguedadTest.kt con los dos bordes y los casos raros"
```

---

## Implementation Strategy

### MVP (solo US1)

1. Phase 1: Setup — respaldo y línea de base
2. Phase 2: Foundational — bloquea todo lo demás
3. Phase 3: US1
4. **PARAR Y VALIDAR**: bloques 1, 2, 3 y 7 del quickstart, en el teléfono
5. Instalar y usarlo caminando

Ahí la feature ya contesta la pregunta que la motivó: *¿a dónde no fui todavía?*

### Entrega incremental

1. Setup + Foundational → base lista, nada cambió a la vista
2. US1 → **el MVP**. Se instala y se usa
3. US3 → Salidas usable. Independiente, se puede adelantar si molesta más que el mapa
4. US2 → el modo antigüedad. Recién muerde cuando la cobertura crece; no urge
5. US4 → los pines. Es lo más grande de las cuatro y lo menos urgente

### Orden alternativo, si al usar el MVP algo molesta más

US3 y US4 son independientes: si la pantalla de Salidas resulta más molesta que la falta del
modo antigüedad, se adelanta y no rompe nada.

---

## Notes

- **US2 depende de US1**, y es la única dependencia entre stories. Está dicho arriba porque
  contradice la suposición por defecto de que las stories son independientes
- El proyecto tiene que compilar y `./gradlew test` pasar en cada checkpoint. T008 deja
  `pintarTrazos` con un color literal justamente para eso
- Commit por tarea o por grupo lógico, con Conventional Commits
- Los valores de color de T011 y T018, y el radio de T037, son **calibración**: se deciden
  mirando el teléfono, no discutiéndolos. Punto de partida en research.md
- El radio de T050 es calibración también, y de la más cara de juzgar en el escritorio: el
  síntoma de que quedó corto son pines duplicados, y el de que quedó largo son dos autos
  fusionados. Solo se ve caminando
- **US5 y US6 son independientes de todo lo anterior y entre sí.** Salieron de usar la feature
  en la calle, después de que las cuatro stories originales estuvieran hechas; se agregaron acá
  en vez de abrir una 005 porque las dos viven en el mismo mapa que esta feature construyó
- Si el orden de dibujo de T013 no funciona en el teléfono —una calle compartida se ve con el
  color de la salida vieja—, el plan B está nombrado en D5: una capa por escalón, de la más
  vieja a la más nueva
