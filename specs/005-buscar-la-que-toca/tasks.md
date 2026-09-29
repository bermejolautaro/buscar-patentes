---

description: "Task list for 005-buscar-la-que-toca"
---

# Tasks: Ir a buscar la que toca

**Input**: Design documents from `/specs/005-buscar-la-que-toca/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/pantalla-principal.md](./contracts/pantalla-principal.md), [quickstart.md](./quickstart.md)

**Tests**: la spec no pide TDD. Las pruebas de esta lista son las que la constitución obliga
—toda rama nueva deja prueba ejecutable en JVM— y las que quedan rotas por el cambio:
`CoberturaTest`, `PrioridadTest`, `IrTest`, `ColeccionTest` y, si la US5 entra, `AcomodoTest`.

**Organization**: agrupadas por user story, en el orden del plan: US1 → US2 → US3 (con el retiro
de la pantalla de búsqueda al final) → US4 → US5. Cada story se instala y se juzga sola.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede hacer en paralelo (archivo distinto, sin depender de nada incompleto)
- **[Story]**: a qué user story pertenece (US1 a US5)
- Cada tarea nombra el archivo exacto y cita la decisión (D*) o el contrato (C*) que la gobierna

## Path Conventions

Un solo módulo Android. Código en `app/src/main/java/ar/lauta/buscarpatentes/`, pruebas en
`app/src/test/java/ar/lauta/buscarpatentes/`, recursos en `app/src/main/res/`. Abajo se abrevia
el prefijo como `main/…` y `test/…` cuando la ruta ya se nombró completa en la misma fase.

---

## Phase 1: Setup

**Purpose**: línea de base en verde. Esta feature no toca la base, así que no hace falta
respaldo del teléfono.

- [X] T001 Correr `.\gradlew.bat testDebugUnitTest` en la rama `005-buscar-la-que-toca` y anotar que pasa. Todo lo que se rompa después lo rompió esta feature
- [X] T002 **Hecha el 2026-09-28**: captura de la zona con más patentes a zoom de calle, con 4 grupos (2, 2, 2 y 3) y 2 pines sueltos; la 338, que era la que tocaba, no se veía: estaba adentro de un grupo. Captura del "antes" para el SC-007, **con la app que el teléfono tiene hoy, previa a la 005**: pedirle al usuario que deje el mapa en la zona con más patentes a zoom de calle (el zoom con que abre la app) y guardar la captura con `adb exec-out screencap -p` (salió del repositorio al publicarlo, en la 006) (adb no puede tocar la pantalla; ver memoria del proyecto). Anotar cuántos grupos se ven. T038 compara contra esto

---

## Phase 2: Foundational

**Sin tareas.** Ninguna pieza bloquea a todas las stories: cada una trae lo suyo, y las que
comparten archivo (la barra, `EstadoDelMapa`) se construyen en el orden de las stories.

---

## Phase 3: User Story 1 - Ver de un vistazo cuántas hay y dónde (Priority: P1) 🎯 MVP

**Goal**: la barra dice cuántas patentes hay del número actual; sus pines tienen fondo verde y
nunca quedan adentro de un grupo.

**Independent Test**: con el número actual cargado y tres patentes de ese número, una rodeada de
otras: la barra dice "3 descubiertas", las tres se ven en verde, y al alejar el mapa siguen
sueltas encima del grupo (quickstart, bloque US1).

- [X] T003 [P] [US1] En `app/src/main/java/ar/lauta/buscarpatentes/domain/Cobertura.kt`: borrar `tieneElActual` y `consecutivosCubiertos` (y `MAXIMO` si queda sin uso) y agregar `fun descubiertas(cuenta: Int): String` que devuelve "Ninguna descubierta" para 0, "1 descubierta" para 1 y "N descubiertas" para más. Actualizar el KDoc del objeto: ahora dice cuántas hay del número que toca (D3, FR-003, FR-003a)
- [X] T004 [P] [US1] Reescribir `app/src/test/java/ar/lauta/buscarpatentes/CoberturaTest.kt`: borrar las ocho pruebas de `tieneElActual` y `consecutivosCubiertos` y cubrir las tres ramas de `descubiertas` (0, 1, y un número mayor como 3 y 12). Actualizar el KDoc: la cuenta de seguidos se retiró por el FR-003a de la 005 (depende de T003)
- [X] T005 [P] [US1] En `app/src/main/java/ar/lauta/buscarpatentes/ui/Tema.kt`: devolver `const val TOCA = "#2F9E44"` a `ColoresDeMapa`, con un KDoc que diga que es el relleno del pin de la patente del número actual (FR-001 de la 005, vuelve lo que retiró el FR-030d de la 004). El comentario de `CONFIANZA_ALTA` que menciona `[TOCA]` vuelve a tener a qué apuntar
- [X] T006 [US1] En `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt`: `bitmapDePin(context, borde, relleno)` recibe el color de relleno (hoy fijo en blanco); `registrarPines` registra además `PIN_TOCA_BAJA`, `PIN_TOCA_MEDIA` y `PIN_TOCA_ALTA` con relleno `ColoresDeMapa.TOCA`; `imagenPorProbabilidad(baja, media, alta)` recibe los tres nombres para servir a las dos capas (D2, depende de T005)
- [X] T007 [US1] En `main/…/mapa/Mapa.kt`, la fuente propia de la que toca (D1, C5): constantes `FUENTE_TOCA = "patentes-toca"` y `CAPA_TOCA = "patentes-toca-pines"`; una función `internal fun coleccionesPatentes(marcadores: List<Marcador>): Pair<FeatureCollection, FeatureCollection>` que devuelve (las que no tocan, las que tocan) usando `coleccion`; `actualizarCapas` le da la primera a `FUENTE` (agrupada, como hoy) y la segunda a `FUENTE_TOCA`, creada **sin** `GeoJsonOptions` de agrupación; `pintarCapa` agrega `CAPA_TOCA` **después** de `CAPA_GRUPO_CUENTA` con las mismas propiedades de `CAPA` salvo: `iconImage(imagenPorProbabilidad(PIN_TOCA_*))`, `iconSize(PIN_TOCA)`, `textSize(TEXTO_PIN * PIN_TOCA)` y `textColor("#FFFFFF")` (depende de T006)
- [X] T008 [US1] En `main/…/mapa/Mapa.kt`: con la que toca en su capa, borrar `porToca(...)`, `PROP_TOCA` y `addBooleanProperty(PROP_TOCA, …)` en `coleccion`; `CAPA` queda con `iconSize(1f)`, `textSize(TEXTO_PIN)` y sin `symbolSortKey`. El listener de `addOnMapClickListener` consulta `CAPA` **y** `CAPA_TOCA` en `queryRenderedFeatures`. `Marcador.toca` se queda: es lo que separa las fuentes. Actualizar su KDoc (depende de T007)
- [X] T009 [P] [US1] En `app/src/test/java/ar/lauta/buscarpatentes/mapa/ColeccionTest.kt`: la prueba de "cada feature conserva coordenadas, si toca y numero" deja de afirmar la propiedad `toca` (ya no viaja); agregar una prueba de `coleccionesPatentes` que verifique que el marcador con `toca = true` va solo a la segunda colección y el otro solo a la primera (depende de T008)
- [X] T010 [US1] Crear `app/src/main/java/ar/lauta/buscarpatentes/ui/BarraDelJuego.kt` con `@Composable fun BarraDelJuego(numero: Int?, cuenta: Int, modifier: Modifier = Modifier)`: `OutlinedCard` como el `IndicadorDelJuego` de hoy, con el número en grande (con ceros, `%03d`) y abajo `Cobertura.descubiertas(cuenta)`. Con `numero == null` muestra "…" como hoy (C1). Es la base que la US2 y la US3 amplían
- [X] T011 [US1] En `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt`: borrar `IndicadorDelJuego`, `tengoElActual` y `consecutivos`; guardar en estado la lista de registros que ya se lee con `todosUnaVez()` (`var registros by remember { mutableStateOf<List<RegistroDeCaptura>>(emptyList()) }`) y calcular la cuenta como `registros.count { it.numero == numeroActual }`, **sin filtrar por estado** (FR-003b); poner `BarraDelJuego(numero = numeroActual, cuenta = …)` donde estaba el indicador (depende de T003, T010)

**Checkpoint**: instalar y correr el bloque US1 del quickstart. La barra dice cuántas hay, los
pines del número actual son verdes, nunca se agrupan, y tocar un pin verde abre su ficha.

---

## Phase 4: User Story 2 - Ver en el mapa solo las de un número (Priority: P1)

**Goal**: la lupa de la barra filtra el mapa por un número, lo encuadra junto con la posición del
jugador, y la `X` lo saca.

**Independent Test**: tocar la lupa deja solo las del número actual, encuadradas con el punto
azul y con el teclado arriba; bajar el teclado mantiene el filtro; escribir otro número cambia el
filtro al tercer dígito; la `X` devuelve todas sin mover el mapa (quickstart, bloque US2).

- [X] T012 [P] [US2] Crear `app/src/main/res/drawable/ic_cerrar.xml`: una cruz, vector de 24 dp con el mismo formato y color de relleno que los `ic_*.xml` existentes (por ejemplo `ic_volver.xml`). Es la `X` que cierra el filtro (C1)
- [X] T013 [P] [US2] En `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt`, `EstadoDelMapa.encuadrar(posiciones: List<Pair<Double, Double>>)` (D6, C5): si el `locationComponent` está activo, `cameraMode = CameraMode.NONE` y `siguiendo = false` (lo mismo que `elJugadorArrastro`) y suma `lastKnownLocation` a las posiciones; con cero posiciones no hace nada; con una, centra a `ZOOM_CALLE`; con más, `animateCamera(newLatLngBounds(límites, RELLENO_ENCUADRE))`. Extraer el cálculo compartido con el `encuadrar` privado a una función `camaraPara(posiciones: List<LatLng>): CameraUpdate?` para no duplicarlo
- [X] T014 [P] [US2] En `app/src/main/java/ar/lauta/buscarpatentes/ui/LeyendaDelMapa.kt`: parámetro `filtro: Int? = null`; si no es null, agregar `Texto("· Solo la %03d")` después de la línea del modo y antes de "· Patentes ocultas" (FR-007). Actualizar el KDoc: la leyenda dice también qué número filtra el mapa
- [X] T015 [US2] En `app/src/main/java/ar/lauta/buscarpatentes/ui/BarraDelJuego.kt`, los dos estados de C1 y D5: parámetros nuevos `filtrando: Boolean`, `onAbrirFiltro: () -> Unit`, `onFiltrar: (Int) -> Unit`, `onCerrarFiltro: () -> Unit`. Cerrada: el número, la cuenta y un `IconButton` con `ic_buscar` que llama `onAbrirFiltro`. Filtrando: en lugar del número, un `OutlinedTextField` sobre un `TextFieldValue` que arranca con `numero` en `%03d` y `selection = TextRange(0, 3)`; acepta solo dígitos y como mucho 3; al completar el tercer dígito llama `onFiltrar`; un `FocusRequester` pide el foco al entrar en este estado (sube el teclado, FR-005); la cuenta abajo; un `IconButton` con `ic_cerrar` que llama `onCerrarFiltro`. Bajar el teclado no cambia de estado (FR-005c). Con `numero == null` el campo arranca vacío (depende de T010, T012)
- [X] T016 [US2] En `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt` (D4): `var filtro by remember { mutableStateOf<Int?>(null) }` — `remember`, no `rememberSaveable` (FR-009) — y `var filtrando by remember { mutableStateOf(false) }`. Los marcadores que van a `MapaDeFondo` se filtran por `filtro` **antes** del `if (patentesVisibles)` de hoy. La barra recibe `numero = filtro ?: numeroActual` y la cuenta de ese número. `onAbrirFiltro`: `filtrando = true` y, si hay número actual, `filtro = numeroActual`. `onFiltrar(n)`: `filtro = n`. `onCerrarFiltro`: `filtro = null`, `filtrando = false`, sin tocar la cámara (FR-006c). `LeyendaDelMapa` recibe `filtro` (depende de T014, T015)
- [X] T017 [US2] En `main/…/ui/PantallaPrincipal.kt`: un `LaunchedEffect(filtro)` que, cuando `filtro` no es null, llama `estadoDelMapa.encuadrar(...)` con las posiciones de los registros de ese número (FR-006a). Sin registros de ese número no se mueve (C5). Verificar que el botón de recentrar aparece solo, porque `siguiendo` quedó en false (FR-006b) (depende de T013, T016)

**Checkpoint**: instalar y correr el bloque US2 del quickstart. La US1 sigue funcionando igual.

---

## Phase 5: User Story 3 - Saber cuál revisar primero (Priority: P2)

**Goal**: tocar la barra despliega la lista de las patentes del número, ordenada por distancia y
confianza, y cada fila lleva a su ficha. Con la lista en la pantalla principal, se retira la
pantalla de búsqueda.

**Independent Test**: con varias patentes del mismo número a distintas distancias y confianzas,
el orden coincide con los escenarios 3, 4 y 5 de la US3; tocar una fila lleva a la patente y abre
su ficha; la ficha permite agregar foto; ya no existe el botón de búsqueda (quickstart, bloques
US3 y "Lo que se fue").

### Orden de revisión

- [X] T018 [P] [US3] Crear `app/src/main/java/ar/lauta/buscarpatentes/domain/Prioridad.kt` (D8, data-model): `data class Candidato(val id: Long, val latitud: Double, val longitud: Double, val confianza: Int, val capturadoEn: Long)`, `const val PESO_CONFIANZA = 5` con un comentario `ponytail:` que diga que se calibra en la calle (FR-012a), y `fun paraRevisar(candidatos: List<Candidato>, desde: Pair<Double, Double>?): List<Candidato>`. Con `desde`: ascendente por `Geo.distanciaMetros(desde…, c…) / (1 + c.confianza / PESO_CONFIANZA.toDouble())`, empate por `capturadoEn` descendente. Sin `desde`: `confianza` descendente, después `capturadoEn` descendente. Sin Android: se prueba en JVM
- [X] T019 [P] [US3] Crear `app/src/test/java/ar/lauta/buscarpatentes/domain/PrioridadTest.kt` con los casos de data-model: los escenarios 3 (100 m y 600 m, misma confianza), 4 (50 m conf. 0 antes que 240 m conf. 10) y 5 (500 m conf. 10 antes que 300 m conf. 0) de la US3, un caso del SC-005, el empate por fecha, y el orden sin posición. Las posiciones se arman desplazando la latitud de un punto fijo (1° de latitud ≈ 111 km), como hace `GeoTest` (depende de T018)

### La lista de la barra

- [X] T020 [P] [US3] En `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt`: `EstadoDelMapa.centrarEn(latitud: Double, longitud: Double)` apaga el seguimiento igual que `encuadrar` y anima la cámara hasta ahí, subiendo a `ZOOM_CALLE` si el zoom actual es menor (C5, FR-011a)
- [X] T021 [US3] En `app/src/main/java/ar/lauta/buscarpatentes/ui/BarraDelJuego.kt` (D7, C2): parámetros nuevos `filas: List<FilaDeLaBarra>`, `sinPosicion: Boolean`, `listaAbierta: Boolean`, `onTocarBarra: () -> Unit`, `onTocarFila: (Long) -> Unit`. `FilaDeLaBarra(id, texto, distanciaYRumbo, confianza)` es un `data class` del mismo archivo. Tocar la barra fuera de la lupa y de la `X` llama `onTocarBarra`. Con `listaAbierta`, debajo de la tarjeta, una `LazyColumn` con `heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.4f)` sobre una superficie con fondo propio: una fila por patente con el texto (22 sp, negrita), la distancia y el rumbo, y "Confianza N de 10"; con `sinPosicion`, una línea arriba: "Sin tu ubicación: ordenadas por confianza" (FR-012b) (depende de T015)
- [X] T022 [US3] En `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt`: `var listaAbierta by remember { mutableStateOf(false) }`. `onTocarBarra`: si hay cero patentes del número de la barra, no hace nada; si no, alterna `listaAbierta` y, al abrir, lee `posicionActual` una vez con `ubicacion.leerAhora()` si hay permiso (como hace hoy `onTocarMarcador`). Las filas salen de los registros del número de la barra convertidos a `Candidato` —la confianza es la `probabilidad` que ya se calcula para el `Marcador` de ese id— ordenados con `Prioridad.paraRevisar(…, posicionActual)`; el texto es `patenteTexto ?: "%03d"`; la distancia, `Geo.distanciaYRumbo`. `onTocarFila(id)`: `listaAbierta = false`, `estadoDelMapa.centrarEn(...)` y `registroTocado = registros.first { it.id == id }`, que abre la ficha por el camino de siempre (FR-011, FR-011a) (depende de T018, T020, T021)

### Retiro de la pantalla de búsqueda (D12, C7)

- [X] T023 [US3] En `app/src/main/java/ar/lauta/buscarpatentes/ui/FichaDeRegistro.kt` (FR-013a): `FichaDeRegistro` gana `onAgregarFoto: () -> Unit = {}` y un `TextButton("Agregar foto")` en la fila de Compartir / Corregir / Borrar, visible solo si `registro.fotoRuta == null`. En `FichaConectada`, mudar desde `ui/PantallaBusqueda.kt` los dos lanzadores —`TakePicture` y `RequestPermission` de cámara—, el archivo pendiente con `context.contenedor.fotos.archivoNuevo(...)` y `FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)`; al volver bien, `Captura.adjuntarFotoA(context, registro.id, archivo.absolutePath)`, `onAviso` con el resultado y `onCambio()`; al volver mal, borrar el archivo. **Implementado distinto**: en lugar de un `fotoAgregada`, `FichaConectada` guarda `var actual by remember(registro.id) { mutableStateOf(registro) }` y lo relee de la base después de adjuntar. Así desaparece el botón y además "Compartir" y "Borrar" usan el registro con la foto nueva, que con el flag no pasaba. Actualizar el comentario del diálogo de borrado que cita "la pantalla de búsqueda"
- [X] T024 [US3] Borrar `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaBusqueda.kt`. En `app/src/main/java/ar/lauta/buscarpatentes/ui/MainActivity.kt`: sacar `BUSQUEDA` de `Destino`, la rama del `when` y `onBuscar`; actualizar el KDoc que cuenta los destinos (quedan tres). En `main/…/ui/PantallaPrincipal.kt`: sacar el parámetro `onBuscar` y su `FilledTonalIconButton` de la franja, y actualizar los comentarios de la franja que nombran "Buscar" (FR-013) (depende de T022, T023)
- [X] T025 [P] [US3] En `app/src/main/java/ar/lauta/buscarpatentes/ui/PantallaAjustes.kt`, el texto de la línea ~220 que dice "borrá registros ya compartidos desde Buscar" pasa a "desde la ficha de cada patente" (FR-013c)
- [X] T026 [P] [US3] En `app/src/main/java/ar/lauta/buscarpatentes/ui/BarraSuperior.kt`: el KDoc que justifica la barra con "tres usos reales —Buscar, Salidas y Ajustes—" pasa a dos, Salidas y Ajustes, que siguen alcanzando para el Principio IV
- [X] T027 [US3] `Grep` de `Buscar`, `PantallaBusqueda`, `búsqueda` en `app/src/main/`: ningún texto visible ni comentario vigente manda a la pantalla que se fue. Los comentarios históricos ("hasta la 004…") pueden quedar si dicen que es historia (FR-013c) (depende de T024, T025, T026)

**Checkpoint**: instalar y correr los bloques US3 y "Lo que se fue" del quickstart. La US1 y la
US2 siguen funcionando.

---

## Phase 6: User Story 4 - Armar un recorrido por varias (Priority: P2)

**Goal**: desde la lista de la barra, un diálogo para elegir y ordenar paradas, que se abre en
Google Maps como un solo recorrido a pie.

**Independent Test**: con tres patentes de un número, armar el recorrido, sacar una, invertir las
otras dos y abrir: Google Maps muestra esas dos paradas en ese orden, saliendo de la posición del
teléfono (quickstart, bloque US4).

- [X] T028 [US4] En `app/src/main/java/ar/lauta/buscarpatentes/domain/Prioridad.kt`: `fun ordenDeParadas(candidatos: List<Candidato>, desde: Pair<Double, Double>?): List<Candidato>` (D9). Se llama así y no `recorrido` para no pisarse con `data.Recorrido`, que es la salida grabada. Con `desde`: vecino más cercano —primero la más cercana a `desde`, después cada vez la más cercana a la anterior—, desempatando por el orden de `paraRevisar`. Sin `desde`: `paraRevisar(candidatos, null)` (depende de T018)
- [X] T029 [US4] En `app/src/test/java/ar/lauta/buscarpatentes/domain/PrioridadTest.kt`, casos de `ordenDeParadas`: tres paradas en línea donde el vecino más cercano no coincide con el orden de revisión (una con mucha confianza más lejos); lista vacía y de una sola; sin posición devuelve el orden de revisión (depende de T028)
- [X] T030 [P] [US4] En `app/src/main/java/ar/lauta/buscarpatentes/ui/Ir.kt` (D11, C4): `internal fun urlDeRecorrido(paradas: List<Pair<Double, Double>>): String` —`destination` es la última, `waypoints` las anteriores separadas por `%7C`, `travelmode=walking`, sin `origin`, coordenadas con `String.format(Locale.ROOT, "%.7f,%.7f", lat, lon)`, sin `waypoints` si hay una sola parada—; las constantes `MAXIMO_PARADAS_APP = 10` y `MAXIMO_PARADAS_NAVEGADOR = 4`, con un comentario que cite la documentación de Maps URLs; **la decisión pura** `internal fun accionPara(incluidas: Int, maximo: Int = MAXIMO_PARADAS_APP): AccionRecorrido`, con `sealed interface AccionRecorrido { Nada; IrALaUnica; Sobran(val cuantas: Int); AbrirMaps }` —0 → `Nada`, 1 → `IrALaUnica`, más de `maximo` → `Sobran(incluidas - maximo)`, si no `AbrirMaps`—, que es la única rama de la US4 y por eso la que se prueba (constitución, Flujo de desarrollo); y `fun enGoogleMaps(context: Context, paradas: List<Pair<Double, Double>>): ResultadoRecorrido`, con `enum class ResultadoRecorrido { ABIERTO, DEMASIADAS_PARA_EL_NAVEGADOR, SIN_APP }`: primero `Intent(ACTION_VIEW, url).setPackage("com.google.android.apps.maps")`; si tira `ActivityNotFoundException`, usa `accionPara(paradas.size, MAXIMO_PARADAS_NAVEGADOR)` y si da `Sobran` devuelve `DEMASIADAS_PARA_EL_NAVEGADOR` sin abrir; si no, el mismo `Intent` sin paquete; si también falla, `SIN_APP`. Ampliar el KDoc de `Ir` con por qué el recorrido no usa `geo:`
- [X] T031 [P] [US4] Crear `app/src/test/java/ar/lauta/buscarpatentes/ui/IrTest.kt`: con tres paradas, `destination` es la tercera y `waypoints` son la primera y la segunda en ese orden, separadas por `%7C`; con dos, un solo waypoint; con una, sin `waypoints`; nunca aparece `origin`; con `Locale.setDefault(Locale("es", "AR"))` las coordenadas siguen con punto decimal (restaurar el locale al terminar). Y `accionPara`: 0 → `Nada`, 1 → `IrALaUnica`, 2 y 10 → `AbrirMaps`, 13 → `Sobran(3)`, y con `maximo = 4`, 6 → `Sobran(2)` (depende de T030)
- [X] T032 [US4] Crear `app/src/main/java/ar/lauta/buscarpatentes/ui/ArmarRecorrido.kt` (D10, C3): `data class Parada(val registro: RegistroDeCaptura, val distanciaYRumbo: String, val confianza: Int, val incluida: Boolean = true)` y `@Composable fun ArmarRecorrido(numero: Int, inicial: List<Parada>, onCerrar: () -> Unit, onAviso: (String) -> Unit)`. Un `Dialog` con título "Recorrido por las %03d", una fila por parada con `Checkbox`, el texto de la patente, distancia · "conf. N", y dos `IconButton` ▲ ▼ que intercambian con la vecina (deshabilitados en los extremos). El estado es una lista local, copiada de `inicial` con `remember`. Botón "Abrir en Google Maps", que decide con `Ir.accionPara(incluidas)` y no con condiciones propias: `Nada` → deshabilitado; `IrALaUnica` → `Ir.aLaPatente`; `Sobran(n)` → `onAviso("Google Maps acepta hasta 10 paradas: sacá $n")` sin abrir; `AbrirMaps` → `Ir.enGoogleMaps`, con `onAviso("En el navegador entran 4 paradas: sacá N")` para `DEMASIADAS_PARA_EL_NAVEGADOR` y `onAviso("No hay app para abrir el recorrido")` para `SIN_APP`. Al abrir o cancelar, `onCerrar`. No inicia ninguna salida (FR-021) ni guarda nada (FR-022) (depende de T030). **Implementado distinto**: los avisos van adentro del diálogo y no por `onAviso` al snackbar, porque el fondo oscurecido del diálogo lo tapaba; con más de 10 paradas el botón se deshabilita y el diálogo dice cuántas sacar antes de tocarlo
- [X] T033 [US4] En `app/src/main/java/ar/lauta/buscarpatentes/ui/BarraDelJuego.kt`: parámetro `onArmarRecorrido: (() -> Unit)?`; si no es null y hay dos o más filas, un `TextButton("Armar recorrido")` al pie de la lista (C2). En `main/…/ui/PantallaPrincipal.kt`: `var paradasPlaneadas by remember { mutableStateOf<List<Parada>?>(null) }`; `onArmarRecorrido` arma las paradas con `Prioridad.ordenDeParadas(candidatos, posicionActual)` y cierra la lista; si no es null, se muestra `ArmarRecorrido`, con los avisos por el `snackbar` de la pantalla (depende de T022, T028, T032)

**Checkpoint**: instalar y correr el bloque US4 del quickstart. Confirmar en el teléfono cuántas
paradas acepta Google Maps y anotarlo en research.md D11 si difiere de 10.

---

## Phase 7: User Story 5 - Pines que se hacen lugar antes de agruparse (Priority: P3)

**Goal**: en lugar de agruparse apenas se tocan, los pines se corren hasta media cuadra, y solo
se agrupan si así no entran.

**Independent Test**: en una cuadra con cuatro patentes, a zoom de calle, se ven cuatro pines
separados cerca de su lugar; alejando, se agrupan; desplazando sin cambiar el zoom, nada se mueve
(quickstart, bloque US5).

**⚠️ Línea de corte (D13)**: si T038 no pasa, se revierten T036 y T037 y la US5 queda como pedido
abierto. Nada de las US1 a US4 depende de esta fase.

- [X] T034 [P] [US5] Crear `app/src/main/java/ar/lauta/buscarpatentes/domain/Acomodo.kt` (D13, data-model): `data class Pin(val id: Long, val latitud: Double, val longitud: Double)`; `sealed interface Dibujo` con `data class Suelto(val id: Long, val latitud: Double, val longitud: Double)` y `data class Grupo(val latitud: Double, val longitud: Double, val cuenta: Int)`; `const val LIMITE_ACOMODO_M = 50.0` con un comentario `ponytail:` (FR-023a); y `fun para(pines: List<Pin>, zoom: Int, anchoPinDp: Double): List<Dibujo>`. Pasos: proyectar a Web Mercator en dp (mundo de `512 * 2^zoom`); metros por dp = `40075016.686 * cos(lat) / (512 * 2^zoom)`; armar componentes conexos de pines a menos de `anchoPinDp` entre sí; para cada componente de más de uno, repulsión de a pares con tope de 50 iteraciones (dos pines en el mismo punto se separan en un ángulo fijo que depende de su posición en la lista, para que el resultado sea determinista); si termina sin choques y sin ningún pin corrido más de `LIMITE_ACOMODO_M`, todos `Suelto` en su posición corrida; si no, un `Grupo` en el centroide de las posiciones reales. Un pin sin choques sale `Suelto` en su lugar. Sin Android
- [X] T035 [P] [US5] Crear `app/src/test/java/ar/lauta/buscarpatentes/domain/AcomodoTest.kt` con los casos de data-model: un pin solo no se mueve; dos pines a 10 m en zoom 17 salen sueltos, separados al menos un ancho de pin y cada uno a menos de 50 m de su lugar; los mismos en zoom 13 salen como un grupo de 2; veinte en el mismo punto, un grupo de 20 en zoom 15 y en zoom 18; dos corridas con la misma entrada dan listas iguales (depende de T034). **Nota**: los veinte en el mismo punto se prueban solo en zoom 15; en zoom 18 un grupo de 20 no es lo pedido, porque ahí caben sueltos en unos 20 m, dentro del límite. Se sumaron dos casos: una cadena no termina en un solo grupo, y nadie se pierde entre los zoom 12 y 19
- [X] T036 [US5] En `app/src/main/java/ar/lauta/buscarpatentes/mapa/Mapa.kt` (C6): `FUENTE` se crea sin `opcionesDeAgrupacion()` (y se borra la función); `internal fun coleccionAcomodada(marcadores: List<Marcador>, dibujos: List<Dibujo>): FeatureCollection` arma los `Suelto` como hoy arma `coleccion` pero en la posición de dibujo, y los `Grupo` como un punto con `addNumberProperty("cuenta", …)`; `esGrupo()` pasa a `Expression.has("cuenta")` y la capa de la cuenta lee `"cuenta"` en lugar de `point_count`; `actualizarCapas` calcula `Acomodo.para(…, floor(zoom), PIN_ANCHO_DP.toDouble())` con los marcadores que no tocan; `MapaDeFondo` agrega un `addOnCameraIdleListener` que recalcula y hace `setGeoJson` solo si `floor(zoom)` cambió desde el último acomodo. `FUENTE_TOCA` no pasa por el acomodo (depende de T034)
- [X] T037 [US5] En `app/src/test/java/ar/lauta/buscarpatentes/mapa/ColeccionTest.kt`: una prueba de `coleccionAcomodada` que verifique que un `Suelto` lleva las coordenadas de dibujo y su número y probabilidad, y que un `Grupo` lleva `cuenta` y no lleva `numero` (depende de T036)
- [X] T038 [US5] Instalar y recorrer el bloque US5 del quickstart en el teléfono, incluida la línea de corte. Con el mapa en la misma zona y zoom que la captura de T002, guardar `despues-grupos.png` y comparar la cantidad de grupos (SC-007). Si no pasa, revertir T036 y T037 con git, dejar la nota en research.md D13 y marcar la US5 como no entregada en este archivo. **Hecho el 2026-09-28**: el jugador lo recorrió en el teléfono y respondió "funciona perfecto". No se sacó `despues-grupos.png`: cuando llegó el OK el teléfono ya estaba desenchufado

**Checkpoint**: el mapa completo se lee sin grupos innecesarios, o la US5 quedó cortada y
anotada.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T039 [P] En `README.md`: la fila de `CoberturaTest` pasa a "El texto de descubiertas del número actual (FR-003 de la 005)"; agregar `PrioridadTest`, `IrTest` y, si entró, `AcomodoTest` a la tabla de pruebas; sumar los enlaces a la spec y al quickstart de la 005 junto a los de la 004; sacar cualquier mención a la pantalla de búsqueda
- [X] T040 Correr `.\gradlew.bat testDebugUnitTest assembleDebug`: todo en verde, incluido `InmutabilidadTest` sin cambios
- [X] T041 Copiar el APK al teléfono como `Download/buscar-patentes.apk`, pisando el anterior, con el `adb push` de quickstart.md §2, y pedir al usuario que lo instale a mano. Después, comparar el hash del APK instalado contra el compilado (memoria del proyecto) **Hecho**: se copió el APK. El hash del instalado no se comparó, porque el teléfono ya estaba desenchufado; el comportamiento nuevo de la US5 confirma que se instaló el build nuevo
- [X] T042 Recorrer [quickstart.md](./quickstart.md) entero en el teléfono, incluido "Lo que se fue" y la carga rápida en 4 interacciones (SC-008) **Hecho el 2026-09-28**, con el mismo OK que T038
- [X] T043 En `TODO.md`, tachar los ítems que esta feature cerró: el filtro por número, la confianza en la lista, el orden por cercanía, "N números seguidos", el recorrido por varias, el fondo verde, la buscada siempre visible y, si entró la US5, los grupos menos sensibles

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias
- **Foundational (Phase 2)**: vacía
- **US1 (Phase 3)**: después de Setup
- **US2 (Phase 4)**: después de la US1, porque amplía `BarraDelJuego` (T010) y el estado de `PantallaPrincipal` (T011)
- **US3 (Phase 5)**: después de la US2, porque la lista cuelga de la barra con lupa (T015) y usa el número del filtro
- **US4 (Phase 6)**: después de la US3, porque el recorrido se arma desde la lista (T021, T022) y reutiliza `Prioridad` (T018)
- **US5 (Phase 7)**: después de la US1, porque toca las mismas funciones de `Mapa.kt` que T007 y T008. Es independiente de la US2 a la US4 y puede hacerse antes si conviene
- **Polish (Phase 8)**: después de las stories que se entreguen

### Dentro de cada story

- Las funciones puras y su prueba primero (T003/T004, T018/T019, T028/T029, T030/T031, T034/T035)
- Después el mapa, después la barra, y al final el cableado en `PantallaPrincipal.kt`
- El retiro de la búsqueda (T023 a T027) va **después** de que la lista de la barra funciona (T022), para no dejar un día sin forma de listar las patentes de un número

### Parallel Opportunities

- US1: T003, T005 y (después de T003) T004 en paralelo; T006 → T007 → T008 en serie, porque son el mismo archivo
- US2: T012, T013 y T014 en paralelo; T015 → T016 → T017 en serie
- US3: T018 y T020 en paralelo, T019 detrás de T018; T025 y T026 en paralelo con cualquier cosa de la fase
- US4: T030 y T031 en paralelo con T028 y T029
- US5: T034 y T035 se pueden escribir en paralelo con cualquier fase, porque son archivos nuevos y puros

---

## Parallel Example: User Story 3

```text
# Las piezas puras y el mapa, a la vez:
Task: "T018 Crear domain/Prioridad.kt con Candidato y paraRevisar"
Task: "T020 EstadoDelMapa.centrarEn en mapa/Mapa.kt"
Task: "T025 Texto de PantallaAjustes.kt"
Task: "T026 KDoc de BarraSuperior.kt"

# Después, en serie: T019 → T021 → T022 → T023 → T024 → T027
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. T001
2. Phase 3 (US1)
3. **STOP and VALIDATE**: bloque US1 del quickstart en el teléfono. Con esto solo ya se ve la que
   toca en cualquier zoom y se sabe cuántas hay

### Incremental Delivery

1. US1 → instalar → validar
2. US2 → instalar → validar (el filtro)
3. US3 → instalar → validar (la lista, y la búsqueda ya no está)
4. US4 → instalar → validar (Google Maps)
5. US5 → instalar → validar o cortar
6. Polish

Cada instalación le cuesta un toque al usuario (el teléfono no instala por adb), así que conviene
juntar dos stories por APK cuando la primera no necesite juzgarse sola en la calle.

---

## Notes

- [P] = archivo distinto, sin depender de nada incompleto
- Commit después de cada story, con Conventional Commits en español
- `InmutabilidadTest` tiene que seguir pasando sin cambios en todas las fases: ninguna tarea
  escribe sobre un campo de evidencia
- Los números a calibrar (`PESO_CONFIANZA`, `LIMITE_ACOMODO_M`, los límites de paradas) llevan
  comentario `ponytail:` con qué mirar en la calle para moverlos
