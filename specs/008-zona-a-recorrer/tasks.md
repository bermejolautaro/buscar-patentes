---

description: "Tareas de la 008: zona a recorrer"
---

# Tasks: Zona a recorrer

**Input**: Design documents from `/specs/008-zona-a-recorrer/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/cuadras.md](./contracts/cuadras.md)

**Tests**: sí. La constitución pide prueba para la lógica que se rompe en silencio, y el
[quickstart](./quickstart.md) §1 las lista. Van antes de la implementación que prueban.

**Organization**: por user story. Todo el código vive en `shared/src/commonMain` y las pruebas en
`shared/src/commonTest`, bajo `kotlin/ar/lauta/buscarpatentes/`. Abajo se abrevian como `common/` y
`commonTest/`. Lo de cada teléfono vive en `shared/src/androidMain/…` y `shared/src/iosMain/…`, con
la misma ruta de paquete.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)

---

## Phase 1: Setup

**Purpose**: la respuesta real de Overpass para las pruebas, y la identificación de la app ante los
servicios.

- [X] T001 [P] Crear `commonTest/domain/RespuestaDeOverpass.kt` con una constante `internal val VIAS: String`: la respuesta de Overpass al pedido del contrato Z1 ([cuadras.md](./contracts/cuadras.md)) para la franja `(-34.6055,-58.3905,-34.6020,-58.3800)`. Es la franja del primer tramo de la caminata `CAMINATA` de `commonTest/ubicacion/RespuestaDeAjuste.kt`.
  - **Cómo se genera**: desde la PC, con un script de Python en el scratchpad, con `User-Agent: buscar-patentes-prueba`. Se recorta a `type`, `id`, `nodes`, `geometry` (`lat` y `lon` a 7 decimales) y `tags` (`name`, `highway`, `oneway`), en JSON compacto.
  - **Tamaño**: un literal de Kotlin no puede pasar de 65.535 bytes. Si no entra, achicar la franja hacia el oeste hasta que entre, dejando por lo menos cuatro cuadras caminadas por el primer tramo.
  - **Privacidad**: solo la franja del centro. Antes de guardarlo, buscar en el archivo los nombres de la lista de "Nunca subir" de la memoria del proyecto; si aparece alguno, correr la franja.
  - **Comentario**: arriba de la constante, de dónde sale, la fecha y el recorte, como en `RespuestaDeAjuste.kt`.
- [X] T002 [P] En `postear`, sumar la cabecera `User-Agent: buscar-patentes` (D1 de [research.md](./research.md)).
  - **Android**: `setRequestProperty("User-Agent", "buscar-patentes")` en `androidMain/…/plataforma/Plataforma.android.kt`.
  - **iPhone**: `setValue("buscar-patentes", forHTTPHeaderField = "User-Agent")` en `iosMain/…/plataforma/Plataforma.ios.kt`.
  - **Comentario**: una línea que diga que Overpass rechaza con 406 los pedidos sin identificación propia, y que el servidor de Valhalla pide lo mismo.

---

## Phase 2: Foundational

**Purpose**: las dos tablas, la base 8 y la migración. Lo usan las tres stories.

**⚠️ CRITICAL**: la base cambia de versión. Hasta que T005 esté hecha, la app no abre una base de la 7.

- [X] T003 En `common/data/Entidades.kt`, sumar lo del [data-model.md](./data-model.md):
  - `enum class EstadoZona { BUSCANDO, ACTIVA, COMPLETADA, CERRADA }`.
  - `@Entity(tableName = "zona") data class Zona` con `id` autogenerado, `nombre`, `borde`, `creadaEn`, `cuentaDesde`, `estado`, `problema: String? = null`, `terminadaEn: Long? = null` y `porcentajeFinal: Int? = null`.
  - `@Entity(tableName = "cuadra", indices = [Index("zonaId")]) data class Cuadra` con `id` autogenerado, `zonaId`, `nombre: String?`, `forma`, `gemelaDe: Long? = null`, `quitada: Boolean = false` y `recorridaAlTerminar: Boolean = false`.
  - Un KDoc por clase y por campo, con qué guarda y qué FR cumple, al estilo de `Recorrido`.
- [X] T004 Crear `ZonaDao` en `common/data/Daos.kt`, abstracto como `RecorridoDao`. Todo con funciones `suspend` de una vez, sin `Flow`, igual que el resto.
  - **Lecturas**: `todas()`, ordenadas por `creadaEn` descendente; `buscando()`, las `BUSCANDO` sin `problema`; `activas()`; `porId(id)`; `cuadrasDe(zonaId)`; `cuadrasDeActivas()`.
  - **Escrituras**: `insertar(zona): Long`; `marcarProblema(id, problema)`; `cambiarCuentaDesde(id, desde)`; `marcarQuitadas(ids: List<Long>, quitada: Boolean)`.
  - **Transacciones**:
    - `activar(zonaId, principales: List<Cuadra>, gemelas: List<Pair<Int, Cuadra>>)`: inserta las principales, les resuelve el id, inserta cada gemela con `gemelaDe` = el id de la principal de su índice, y pasa la zona a `ACTIVA`.
    - `terminar(id, estado, en, porcentaje, recorridas: List<Long>)`: guarda el resultado y marca `recorridaAlTerminar`.
    - `borrarConSusCuadras(id)`.
  - Sumar `abstract fun zonas(): ZonaDao` en `common/data/BaseDeDatos.kt` y `val zonas by lazy { baseDeDatos.zonas() }` en `common/Contenedor.kt`.
  Depende de T003.
- [X] T005 La base 8 (D7 de [research.md](./research.md)).
  - **Versión**: en `common/data/BaseDeDatos.kt`, sumar `Zona::class` y `Cuadra::class` a `entities` y pasar `version` a 8. Actualizar el KDoc: la 8 es la primera versión con migración común.
  - **La migración, común**: crear `common/data/Migraciones.kt` con `val SQL_7_8: List<String>`. Son los `CREATE TABLE` de `zona` y de `cuadra` y el `CREATE INDEX` de `cuadra.zonaId`, **copiados textuales** del `createAllTables` que genera Room: está en `shared/build/generated/ksp/…/BaseDeDatos_Impl.kt` después de compilar. Room valida el esquema al abrir, y una columna distinta tira la app.
  - **Android**: en `androidMain/…/data/ConstruirBase.kt`, `MIGRACION_7_8` recorre `SQL_7_8` con `db.execSQL` y se suma a `addMigrations`.
  - **iPhone**: en `iosMain/…/data/ConstruirBase.ios.kt`, `MIGRACION_7_8` recorre `SQL_7_8` con `connection.execSQL` y se agrega con `addMigrations`. Actualizar el KDoc: desde la 8 también migra, al abrir un respaldo de la 7.
  - **Por qué dos objetos y una lista**: en el Android, Room abre la base con `SupportSQLiteDatabase` y llama a `migrate(db)`; en el iPhone, con el driver, llama a `migrate(connection)`. Lo común es el SQL.
  - **El respaldo**: en `common/respaldo/Respaldo.kt`, `VERSION_BASE = 8`.
  Depende de T003 y T004.
- [X] T006 [P] Crear `commonTest/data/MigracionTest.kt`.
  - **La migración**: arma una base de la 7 con los `CREATE TABLE` que ya tiene `RespaldoTest` (moverlos a una función `internal` compartida si hace falta). Corre `SQL_7_8` sobre una `BundledSQLiteDriver().open(...)`. Verifica que existen `zona` y `cuadra`, que se puede insertar una fila en cada una, y que las filas de `recorrido` y `punto_de_trayecto` no cambiaron.
  - **El respaldo**: en `commonTest/respaldo/RespaldoTest.kt`, sumar que un respaldo con `user_version` 7 se acepta y uno con 9 se rechaza como más nuevo.
  Depende de T005.
- [X] T007 [P] En `shared/src/androidHostTest/…/InmutabilidadTest.kt`, sumar una prueba que lea el bloque de `ZonaDao` en `Daos.kt` y falle si alguna `@Query`, `@Insert`, `@Update` o `@Delete` escribe en `recorrido` o en `punto_de_trayecto` (FR-019, Principio II). Depende de T004.

**Checkpoint**: compila en los dos targets y pasan todas las pruebas. La app abre la base de siempre, migrada a la 8, sin zonas.

---

## Phase 3: User Story 1 - Dibujar una zona y ver cuánto falta (Priority: P1) 🎯 MVP

**Goal**: el jugador dibuja una zona, la app busca sus cuadras y le muestra el porcentaje, lo que
falta y los días. La pantalla principal resalta lo que falta.

**Independent Test**: quickstart §3, §4 y §5.

### Tests for User Story 1

- [X] T008 [P] [US1] Crear `commonTest/domain/BordeTest.kt` para `Borde`:
  - con dos esquinas no es válido;
  - un moño, con los bordes cruzados, no es válido;
  - un cuadrado y un polígono cóncavo sí;
  - un punto adentro, uno afuera y uno adentro del hueco de un cóncavo;
  - el rectángulo con margen se agranda 200 m para cada lado, medidos con `Geo`;
  - la consulta de Overpass lleva punto decimal y 6 decimales.
- [X] T009 [P] [US1] Crear `commonTest/domain/CuadrasTest.kt` con vías **inventadas**, construidas en la prueba, para las garantías del contrato Z2:
  - una calle recta que cruzan cinco calles da seis cuadras;
  - una calle partida en dos vías sin esquina da una cuadra;
  - una calle sin salida termina en su último nodo;
  - una cuadra de 20 m se descarta;
  - una cuadra con 60% adentro del borde queda y una con 40% no;
  - las dos manos de una avenida, paralelas a 20 m y con el mismo nombre, son gemelas;
  - dos cuadras seguidas de la misma calle de una mano no lo son, y dos paralelas con distinto nombre tampoco.
  Sumar una prueba con `VIAS` (T001): se lee con `BuscarCuadras.leerVias` y arma más de cero cuadras, todas de 30 m o más.
- [X] T010 [P] [US1] Crear `commonTest/domain/CoberturaDeZonaTest.kt` para el contrato Z3.
  - **Con los datos reales**: cuadras armadas de `VIAS` (T001) con un borde que envuelve toda la franja, y los tramos de `RESPUESTA` (`commonTest/ubicacion/RespuestaDeAjuste.kt`) leídos con `AjustarACalles.leerRespuesta` y `CaminoAjustado.pedazos`. Las cuadras caminadas por el primer tramo están recorridas, y las que ese tramo cruza en una esquina, no. Imprimir en el mensaje de falla el porcentaje cubierto de cada cuadra.
  - **Con cuadras inventadas**:
    - media cuadra una salida y la otra media otra salida dan recorrida;
    - una salida anterior a `desde` no suma;
    - una cuadra quitada no cuenta ni arriba ni abajo;
    - una pareja de gemelas cuenta una vez y la recorre cualquiera de las dos;
    - 99 recorridas de 100 dan 99%;
    - `completadaEn` es la fecha de la salida que recorrió la última cuadra, y es nula mientras falte una.
- [X] T011 [P] [US1] En `commonTest/ui/FormatosTest.kt`, sumar la duración (D10):
  - 0 días da "Desde hoy";
  - 1 da "1 día";
  - 23 da "23 días";
  - 30 da "30 días";
  - 31 da "1 mes y 1 día";
  - 65 da "2 meses y 4 días";
  - 60, justo dos meses sin días sueltos, da "2 meses".
  Los meses son de calendario, contados con `kotlinx-datetime` en la zona del teléfono.

### Implementation for User Story 1

- [X] T012 [P] [US1] **Hecha**, con `consulta` en `BuscarCuadras` y no en `Borde`: es el pedido a Overpass, y su prueba está en `CuadrasTest`. Las cuentas en metros viven en `common/domain/Plano.kt`, que usan `Cuadras` y `CoberturaDeZona`. Crear `common/domain/Borde.kt`: `object Borde` con estas funciones:
  - `valido(esquinas)`: tres o más, y ningún par de lados no vecinos que se cruce;
  - `adentro(punto, esquinas)`: el método del rayo;
  - `rectangulo(esquinas, margenMetros = 200.0)`;
  - `consulta(rectangulo)`: el texto del contrato Z1, con el `decimales` de `ui/Formatos.kt` o un formato propio con punto;
  - `escribir(esquinas)` y `leer(texto)`, con `Polilinea` a 1e6.
  Los KDoc citan el D4 y el Z1. Hace pasar T008.
- [X] T013 [US1] Crear `common/domain/Cuadras.kt` con `data class Via(nodos: List<Long>, posiciones: List<Pair<Double, Double>>, nombre: String?, unaMano: Boolean)` y `data class CuadraArmada(nombre: String?, forma: List<Pair<Double, Double>>, gemelaDe: Int?)`, donde `gemelaDe` es el índice de la principal en la lista.
  - **`object Cuadras`** con `armar(vias, borde): List<CuadraArmada>`, que sigue los pasos 1 a 5 del contrato Z2 con las constantes `LARGO_MINIMO_M = 30.0`, `PASO_M = 5.0`, `PARTE_ADENTRO = 0.5` y las del D3.
  - **Marcas**: `ponytail:` en el corte de 30 m, igual que en el D2.
  - **Distancias**: las de `Geo` que ya existen; si falta distancia de punto a segmento, sumarla a `common/domain/Geo.kt`.
  Depende de T012. Hace pasar T009, salvo la parte de `VIAS`.
- [X] T014 [US1] **Hecha**, con el resultado llamado `CuentaDeZona`: `Cobertura` ya existía (la cuenta de descubiertas de la 005). Crear `common/domain/CoberturaDeZona.kt` con:
  - `data class CuadraParaContar(id, forma, gemelaDe: Long?, quitada)`;
  - `data class SalidaParaContar(iniciadoEn, tramos)`;
  - `data class Cobertura(recorridas: Set<Long>, total, porcentaje, faltan, completadaEn: Long?)`;
  - `object CoberturaDeZona` con `calcular(cuadras, salidas, desde): Cobertura`.

  **La cuenta**: los pasos 1 a 5 del contrato Z3.
  - Constantes: `PASO_M = 5.0`, `TOLERANCIA_M = 10.0`, `UMBRAL = 0.8`, con una marca `ponytail:`.
  - Los segmentos de las salidas van en una grilla de celdas de 50 m, en un `HashMap` de celda a lista, y cada muestra mira su celda y las 8 vecinas (D5).
  - Las salidas cuyo rectángulo no toca el de las cuadras se descartan antes.
  - `porcentaje` redondea hacia abajo.

  Depende de T013. Hace pasar T010.
- [X] T015 [US1] Crear `common/ubicacion/BuscarCuadras.kt`, con la forma y los comentarios de `AjustarACalles.kt`.
  - **`URL`**: la de Overpass, en una constante.
  - **`leerVias(texto): List<Via>?`**: lee el JSON del Z1 con `kotlinx-serialization-json`, sin plugin. Da `null` si no se entiende.
  - **`buscar(zona): Resultado?`**: pide `Borde.consulta(Borde.rectangulo(esquinas))` con `postear`, lee, arma con `Cuadras.armar` y cuenta las principales. `null` sin respuesta.
  - **`pendientes()`**: con `hayConexion()`, recorre `contenedor.zonas.buscando()`. Con 0 o más de 2.000 principales llama a `marcarProblema` con el texto del D8; si no, a `activar`. Devuelve cuántas zonas cambiaron.
  - **Excepciones**: se tragan como en `AjustarACalles`, para que se reintente en la próxima apertura.
  Depende de T004, T013 y T012. Hace pasar la parte de `VIAS` de T009.
- [X] T016 [US1] Crear `common/data/Zonas.kt` con `object Zonas`, que concentra lo que la pantalla le pide a la base.
  - **`crear(nombre, esquinas, cuentaDesde, ahora): Long`**: inserta en `BUSCANDO`. Si el nombre viene vacío, "Zona del dd/mm" con `fechaSinAnio`.
  - **`cambiarCuentaDesde(id, desde)`**: solo si la zona está activa y `desde` no es posterior a hoy.
  - **`salidasParaContar()`**: lee `recorridos.todosUnaVez()` y se queda con las terminadas y `CaminoGuardado.Ajustado`, pasadas a `SalidaParaContar`.
  - **`cobertura(zona, cuadras, salidas)`**: arma los `CuadraParaContar` y llama a `CoberturaDeZona.calcular` con `zona.cuentaDesde`.
  Depende de T004, T014 y T012.
- [X] T017 [P] [US1] En `common/ui/Formatos.kt`, sumar `fun duracion(desde: Long, hasta: Long, zona: TimeZone = TimeZone.currentSystemDefault()): String` (D10). Hace pasar T011.
- [X] T018 [US1] Sumar el dibujo de las zonas en `common/mapa/Colecciones.kt`, siguiendo el contrato Z4.
  - **Tipos**:
    - `enum class ClaseDeCuadra { RECORRIDA, PENDIENTE, QUITADA }`;
    - `data class CuadraDibujada(id, forma, clase)`;
    - `data class DibujoDeZona(borde: List<Pair<Double, Double>>, cuadras: List<CuadraDibujada>, bordeContinuo: Boolean)`.
  - **Colecciones**:
    - `coleccionBordes(zonas)`: un `LineString` cerrado por zona, con la propiedad de continuo o punteado;
    - `coleccionCuadras(zonas)`: un `LineString` por cuadra, con `PROP_ID` y `PROP_CLASE`.
  - **Pruebas**, en `commonTest/mapa/ColeccionTest.kt`: el borde cierra contra la primera esquina, y cada cuadra lleva su id y su clase.
- [X] T019 [US1] Dibujar y tocar las zonas en `common/mapa/MapaDeFondo.kt` (D9).
  - **Parámetros nuevos**: `zonas: List<DibujoDeZona> = emptyList()`, `onTocarMapa: ((Double, Double) -> Unit)? = null` y `onTocarCuadra: ((Long) -> Unit)? = null`.
  - **Capas**:
    - las cuadras, en una `LineLayer` **debajo** de `CapasDeTrazos`, con el color por clase;
    - el borde, en otra `LineLayer`, punteado o continuo según la propiedad.
    - Con `onTocarCuadra`, la capa de cuadras tiene `onClick`, que lee `PROP_ID` como `CapasDePatentes`.
  - **Toque suelto**: con `onTocarMapa`, las `MapInteractions` suman `click { onUnhandled { … } }`, que entrega `position.latitude` y `position.longitude`.
  - **Colores**: `ColoresDeMapa.PENDIENTE`, `RECORRIDA_ZONA`, `QUITADA` y `BORDE`, en `common/ui/Tema.kt`.
    - El pendiente tiene que distinguirse del azul de los recorridos y del resaltado de las avenidas, en los dos temas.
    - La quitada va gris y con menos opacidad.
    - Con la marca `ponytail:`, como el D9.
  - **Encuadre**: el existente también cuenta las esquinas de las zonas, así el mapa de una zona abre mostrándola entera.
  Depende de T018.
- [X] T020 [US1] La pantalla de zonas y cómo se llega.
  - **Navegación**: en `common/ui/AppBuscarPatentes.kt`, sumar `Destino.ZONAS`, con `BackHandler` como las otras.
  - **Ícono**: crear `shared/src/commonMain/composeResources/drawable/ic_zonas.xml`, un polígono con un recorrido adentro, del mismo tamaño y trazo que `ic_salidas.xml`.
  - **Botón**: en `common/ui/PantallaPrincipal.kt`, sumar un `FilledTonalIconButton` con `onZonas`, entre Salidas y Ajustes, con `contentDescription = "Zonas"`.
  - **La lista**: crear `common/ui/PantallaZonas.kt` con `PantallaZonas(onVolver)` y el patrón de `PantallaRecorridos`, es decir lista, detalle abierto por id y volver.
    - Arriba, **Nueva zona**.
    - Cada zona activa dice su nombre, el porcentaje, "faltan N cuadras" y la duración desde `cuentaDesde`.
    - Una `BUSCANDO` dice "Buscando las calles", o su `problema`.
    - La cuenta de cada zona sale de `Zonas.cobertura` con las salidas leídas una sola vez.
    - Al entrar, llama a `BuscarCuadras.pendientes()` y recarga si algo cambió.
  Depende de T016 y T017.
- [X] T021 [US1] El dibujo de una zona nueva, en `common/ui/PantallaZonas.kt` (FR-001 a FR-003).
  - **El mapa**: `MapaDeFondo` a pantalla completa, con `onTocarMapa` agregando esquinas y `zonas` mostrando el borde que se va armando. Lleva `seguirAlJugador = true` hasta el primer arrastre, como la pantalla principal.
  - **Botones**: **Deshacer**, **Cancelar** y **Listo**. **Listo** está habilitado solo con `Borde.valido`, y si no, una línea dice por qué: "Faltan esquinas" o "El borde se cruza".
  - **Con Listo**: se abre un diálogo con el campo de nombre, opcional, y "Desde cuándo cuenta: hoy". Al tocar la fecha se abre un `DatePickerDialog` de Material 3 con `selectableDates`, que no deja elegir días posteriores a hoy.
  - **Crear**: llama a `Zonas.crear` y a `BuscarCuadras.pendientes()`, y abre el detalle de la zona nueva.
  Depende de T019 y T020.
- [X] T022 [US1] El detalle de una zona en `common/ui/PantallaZonas.kt` (FR-007, FR-008).
  - **Arriba**: el nombre, el porcentaje grande, "faltan N cuadras de M", la duración y "Desde cuándo cuenta: dd/mm/aaaa". Tocar la fecha abre el mismo `DatePickerDialog` y llama a `Zonas.cambiarCuentaDesde`.
  - **Abajo**: el mapa con el borde continuo y las cuadras en sus tres clases.
  - **Mientras busca**: "Buscando las calles", o el `problema`, sin mapa de cuadras.
  - **Al volver a la lista**: la lista recarga.
  Depende de T020.
- [X] T023 [US1] Las zonas en la pantalla principal, en `common/ui/PantallaPrincipal.kt` (FR-010).
  - **Qué se carga**: en el `LaunchedEffect` que arma `caminos`, cargar también `zonas.activas()` y `cuadrasDeActivas()`. `CaminoDeSalida` suma `iniciadoEn`.
  - **Qué se dibuja**: `remember(caminos, zonas)` calcula con `Zonas.cobertura` los `DibujoDeZona` de cada activa: el borde punteado y **solo las cuadras pendientes**, sin las quitadas.
  - **Cuándo**: se pasan a `MapaDeFondo` solo en `COBERTURA` y `ANTIGUEDAD`; con los recorridos apagados van vacías.
  - **La búsqueda**: en el `LaunchedEffect(Unit)` que llama a `AjustarACalles.pendientes()`, llamar también a `BuscarCuadras.pendientes()` y sumar `recarga++` si alguno cambió algo.
  - Ningún número en esta pantalla.
  Depende de T016 y T019.

**Checkpoint**: la US1 anda de punta a punta. Quickstart §3, §4 y §5. El porcentaje se mueve solo cuando una salida se ajusta.

---

## Phase 4: User Story 2 - Quitar las calles que no se van a caminar (Priority: P2)

**Goal**: el jugador quita una cuadra o una calle entera de la zona, y la puede volver a sumar.

**Independent Test**: quickstart §6.

### Tests for User Story 2

- [X] T024 [P] [US2] **Hecha en `commonTest/data/ZonasTest.kt`**, porque la selección necesita el nombre de la calle, que vive en `Cuadra` y no en el dominio. En `commonTest/domain/CuadrasTest.kt`, sumar `Cuadras.seleccion(tocada, cuadras, todaLaCalle)`:
  - una cuadra sola da su id y el de su gemela;
  - "toda la calle" da todas las cuadras de la zona con el mismo nombre, con sus gemelas;
  - una cuadra sin nombre con "toda la calle" da solo ella y su gemela.

### Implementation for User Story 2

- [X] T025 [US2] **Hecha como `Zonas.seleccion` y `Zonas.quitar(zonaId, tocada, todaLaCalle, quitada)`**, una sola función para quitar y sumar. Hacer pasar T024 sumando `fun seleccion(tocada: Long, cuadras: List<CuadraParaContar>, nombres: Map<Long, String?>, todaLaCalle: Boolean): Set<Long>` en `common/domain/Cuadras.kt`, o la firma que resulte más simple con los tipos de T014. En `common/data/Zonas.kt`, sumar `quitar(zonaId, tocada, todaLaCalle)` y `sumar(zonaId, tocada, todaLaCalle)`: los dos usan `seleccion` y `marcarQuitadas`, y solo actúan si la zona está activa. Depende de T016.
- [X] T026 [US2] En el detalle de `common/ui/PantallaZonas.kt`, `onTocarCuadra` abre un diálogo, solo si la zona está activa (FR-012, FR-013).
  - **Arriba**: el nombre de la calle, o "Calle sin nombre".
  - **Cuadra que cuenta**: **Quitar esta cuadra** y **Quitar toda la calle en la zona**.
  - **Cuadra quitada**: **Volver a sumar** y **Volver a sumar toda la calle**.
  - **Al elegir**: recarga el detalle. El total y el porcentaje cambian en el momento.
  En la pantalla principal no hay nada que cambiar: T023 ya dibuja solo las pendientes, sin las quitadas. Verificarlo. Depende de T025 y T022.

**Checkpoint**: quickstart §6. Quitar son dos toques.

---

## Phase 5: User Story 3 - Cerrar el objetivo (Priority: P3)

**Goal**: la zona se completa sola al 100%, se puede cerrar a mano, y una zona terminada no cambia
más. Las zonas se pueden borrar.

**Independent Test**: quickstart §7.

### Implementation for User Story 3

La cuenta de la fecha de completada ya la prueba T010. Lo que agrega esta story es la escritura, y
la verifica el quickstart.

- [X] T027 [US3] En `common/data/Zonas.kt`, sumar `revisar()`, `cerrar(id, ahora)` y `borrar(id)` (FR-014 a FR-018, D7).
  - **`revisar()`**: para cada zona activa calcula la cobertura. Si `faltan == 0` y `total > 0`, llama a `zonas.terminar(id, COMPLETADA, completadaEn, 100, recorridas)`. Devuelve cuántas completó.
  - **`cerrar(id, ahora)`**: lo mismo, con `CERRADA`, `ahora` y el porcentaje del momento.
  - **`borrar(id)`**: `borrarConSusCuadras`.
  - **Cuándo corre `revisar()`**:
    - en el `LaunchedEffect(Unit)` de `common/ui/PantallaPrincipal.kt`, después de `AjustarACalles.pendientes()` y de `BuscarCuadras.pendientes()`;
    - en `Zonas.quitar` y en `Zonas.sumar`;
    - en `Zonas.cambiarCuentaDesde`.
  Depende de T016 y T025.
- [X] T028 [US3] En `common/ui/PantallaZonas.kt`, lo que falta de la lista y del detalle.
  - **Lista**: una sección "Terminadas" debajo de las activas. Cada una lleva su nombre y su resultado: "Completada en {duracion(cuentaDesde, terminadaEn)}", o "Cerrada con N% después de {duracion}".
  - **Detalle de una zona activa**: **Cerrar objetivo**, que confirma con un diálogo que dice que no se reabre y llama a `Zonas.cerrar`.
  - **Detalle de una zona terminada**:
    - dibuja las cuadras según `recorridaAlTerminar`, más las quitadas;
    - no tiene `onTocarCuadra`, ni fecha editable, ni Cerrar;
    - arriba, el resultado.
  - **Detalle de cualquier zona**: **Borrar zona**, que confirma diciendo que las salidas no se tocan, llama a `Zonas.borrar` y vuelve a la lista.
  Depende de T027 y T022.

**Checkpoint**: quickstart §7. Borrar una salida no cambia una zona terminada.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T029 [P] Correr `./gradlew :shared:testAndroidHostTest` y `:shared:compileKotlinIosSimulatorArm64`. Todo verde, sin warnings nuevos.
- [X] T030 [P] Actualizar `README.md`:
  - la tabla de pruebas, con `BordeTest`, `CuadrasTest`, `CoberturaDeZonaTest` y `MigracionTest`;
  - una sección corta "Zonas": qué hace, que las cuadras salen de OpenStreetMap vía Overpass una vez por zona, y dónde vive la URL;
  - la lista de specs, con la 008.
  En `TODO.md`, tachar el ítem de la zona con "(hecho en la 008)".
- [ ] T031 Armar lo que el jugador instala.
  - **Android**: el APK debug a `/sdcard/Download/buscar-patentes.apk`, pisando el anterior y borrando cualquier otro `buscar-patentes*.apk`, sin tocar `NavegadorCamiones.apk`. Verificar el hash.
  - **iPhone**: `gh workflow run ios.yml --ref 008-zona-a-recorrer`, y el `.ipa` a `Downloads\buscar-patentes-ios\zonas`.
  Recordarle al jugador que saque un respaldo antes de instalar: la base pasa a la 8.
- [ ] T032 El jugador corre el [quickstart](./quickstart.md) §2 a §8 en el teléfono de juego. Lo que falle vuelve como tarea nueva en esta fase.
- [ ] T033 Antes de cada push, revisar el diff: ni coordenadas de la zona de juego, ni nombres de calles o barrios de la lista de "Nunca subir", ni capturas del mapa, ni respaldos. La franja de T001 es del centro y es la única geografía que se sube.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: T001 y T002, independientes entre sí.
- **Foundational (Phase 2)**: después de Setup. Bloquea todas las stories.
- **US1 (Phase 3)**: después de Foundational. Es el MVP.
- **US2 (Phase 4)**: necesita el detalle de la US1 (T022) y `Zonas` (T016).
- **US3 (Phase 5)**: necesita `Zonas` (T016) y el detalle (T022). La completada automática usa la cuenta de T014. T027 depende de T025 solo porque `revisar()` también corre después de quitar; si la US2 no está, `revisar()` se engancha solo en la pantalla principal y en el cambio de fecha.
- **Polish (Phase 6)**: al final.

### Dentro de la US1

- Las pruebas T008 a T011 van antes y en paralelo.
- `Borde` (T012) va antes que `Cuadras` (T013), que va antes que `CoberturaDeZona` (T014).
- `BuscarCuadras` (T015) y `Zonas` (T016) van después de T012 a T014.
- El mapa: T018, después T019.
- Las pantallas: T020, después T021 y T022, después T023. Las cuatro tocan archivos de UI, así que van en secuencia.

### Parallel Opportunities

- T001 ∥ T002.
- T006 ∥ T007, después de T005.
- T008 ∥ T009 ∥ T010 ∥ T011, con T001 hecha para las partes con datos reales.
- T012 ∥ T017 ∥ T018: `Borde`, `Formatos` y `Colecciones` son archivos distintos sin dependencias entre sí.
- T029 ∥ T030.

## Parallel Example: User Story 1

```text
Juntas, las pruebas:
  T008 BordeTest
  T009 CuadrasTest
  T010 CoberturaDeZonaTest
  T011 FormatosTest (duración)

Juntas, después:
  T012 Borde.kt
  T017 Formatos.duracion
  T018 Colecciones: bordes y cuadras
```

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Setup y Foundational: la base 8 con la migración, probada en la JVM.
2. La US1 completa: dibujar, buscar, contar y ver.
3. **STOP and VALIDATE**: el jugador crea una zona chica, corre "desde cuándo cuenta" para atrás y
   compara el porcentaje con lo que ve en el mapa. Sale a caminar diez cuadras (quickstart §4). Si
   el 80% o los 10 m dejan afuera cuadras caminadas, se calibran ahí, antes de seguir.
4. La US2 y la US3 se suman sobre la US1 validada.

### Notas

- La base 8 no tiene vuelta atrás en el teléfono: una app vieja no abre una base de la 8. Antes de
  instalar la primera versión con la 008, el respaldo del quickstart.
- `[P]`: archivos distintos y sin dependencias pendientes.
- Un commit por tarea o por grupo lógico, con Conventional Commits.
