# Tasks: Cuadras de las teselas

**Input**: Design documents from `/specs/012-cuadras-de-las-teselas/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [quickstart.md](./quickstart.md)

**Tests**: cada prueba va antes que su código.

## Format: `[ID] [P?] [Story] Description`

---

## Phase 1: Setup

- [ ] T001 Bajar las teselas de zoom 14 de la franja del centro `(-34.6055,-58.3905,-34.6020,-58.3800)` con su margen de 200 m, recortarlas a `transportation` y `transportation_name` y a los rasgos que tocan ese rectángulo, y guardarlas en base64 en `shared/src/commonTest/kotlin/ar/lauta/buscarpatentes/domain/RespuestaDeTeselas.kt` (D7). Sin geografía fuera de esa franja.

## Phase 2: Foundational

- [ ] T002 [P] `expect suspend fun bajar(url: String): ByteArray?` en `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/plataforma/Plataforma.kt`, con su `actual` en `Plataforma.android.kt` y `Plataforma.ios.kt`: un GET con 20 s de espera y el `User-Agent` de siempre. Los bytes, o null si no fue un 200 (D6).

---

## Phase 3: User Story 2 - Las mismas cuadras que antes (Priority: P1)

Va antes que US1: es lo que arma las cuadras.

**Independent Test**: [quickstart](./quickstart.md) §1 y §2.

- [ ] T003 [US2] Escribir `shared/src/commonTest/kotlin/ar/lauta/buscarpatentes/domain/TeselasTest.kt`, con teselas armadas en la prueba con un codificador mínimo:
  - decodificar una capa con un rasgo, sus propiedades y su línea;
  - qué calles entran (D3): `minor` sí, `service` no, `foot = no` no, `path` peatonal sí y otro `path` no, un polígono no;
  - una calle que cruza el borde entre dos teselas sale como una sola cadena;
  - una calle que termina sobre otra sin compartir un punto la parte ahí, y una que no toca nada queda suelta;
  - dos calles que se cruzan en distinto `layer` no se tocan;
  - el nombre llega de `transportation_name` y el sentido único de `oneway`;
  - `cubren` da las teselas de zoom 14 de un rectángulo.
- [ ] T004 [US2] Crear `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/domain/Teselas.kt`: `Tesela`, `cubren(rectangulo)` y `vias(teselas)`, con el decodificador, el recorte, los nodos, las esquinas en T y los nombres (D2 a D5). T003 pasa.
- [ ] T005 [US2] Pasar `CuadrasTest.kt` y `CoberturaDeZonaTest.kt` a las teselas reales de T001, y borrar `RespuestaDeOverpass.kt` y las pruebas de la lectura de Overpass. Las pruebas con datos reales siguen pasando; las cuentas que cambien se anotan.

## Phase 4: User Story 1 - Las calles de una zona nueva llegan en segundos (Priority: P1)

**Independent Test**: [quickstart](./quickstart.md) §3.

- [ ] T006 [US1] En `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/ubicacion/BuscarCuadras.kt`, `buscar` pide el TileJSON de `ConfigMapa`, baja las teselas que cubre el rectángulo de la zona con `bajar` y arma con `Teselas.vias` y `Cuadras.armar`. Si falta alguna tesela, la zona sigue esperando. Borrar `consulta`, `leerVias` y el servicio de Overpass, y actualizar el KDoc.
- [ ] T007 [US1] En `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/mapa/MapaDeFondo.kt`, la URL del TileJSON en `ConfigMapa`, al lado de las de los estilos.

## Phase 5: Polish

- [ ] T008 Correr `./gradlew :shared:testAndroidHostTest :app:assembleDebug`. Todo verde, sin warnings nuevos.
- [ ] T009 Comparar contra Overpass con la zona del jugador, fuera del repo (SC-002, quickstart §2).
- [ ] T010 [P] `README.md`: la 012 en la lista de specs, y la sección de Zonas dice que las cuadras salen de las teselas. `specs/008-zona-a-recorrer/research.md`: anotar en el D1 que la 012 lo reemplaza.
- [ ] T011 Armar lo que el jugador instala: el `.ipa` a `Downloads\buscar-patentes-ios\cuadras-de-las-teselas`, y el APK si el Android está enchufado.
- [ ] T012 El jugador corre el [quickstart](./quickstart.md) §3.
- [ ] T013 Antes de cada push, revisar el diff: ni coordenadas fuera de la franja del centro, ni nombres de calles o de zonas del jugador, ni respaldos.

## Dependencies & Execution Order

- T001 y T002 primero.
- T003 → T004 → T005; T006 necesita T002 y T004.
- La rama sale de la 011, que todavía no está en `main`.
