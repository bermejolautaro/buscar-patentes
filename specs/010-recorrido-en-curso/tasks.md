# Tasks: Recorrido en curso

**Input**: Design documents from `/specs/010-recorrido-en-curso/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/cuentas.md](./contracts/cuentas.md), [quickstart.md](./quickstart.md)

**Tests**: la constitución pide una prueba para la lógica no trivial. Cada prueba va antes que su
código y falla primero.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1)

---

## Phase 1: Setup

Nada: sin dependencias nuevas.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: las cuentas, los textos y el peso. Los usan la confirmación, la barra, Salidas y las
estadísticas (D6).

- [X] T001 [P] Escribir `shared/src/commonTest/kotlin/ar/lauta/buscarpatentes/domain/CaminataTest.kt` con el contrato C1 y C2:
  - metros: sin puntos y con uno, 0; tres puntos en línea suman sus dos distancias; un salto de más de 250 m en el medio no suma;
  - kcal: 70 kg y 2.100 m dan 75; 70 kg y 0 m dan 0; 80 kg y 5.000 m dan 200; sin peso, null;
  - resumen: la duración de una terminada es `finalizadoEn - iniciadoEn`, y la de una en curso llega hasta `ahora`.
- [X] T002 Crear `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/domain/Caminata.kt` (D1, D2, D6):
  - `object Caminata` con `metros(puntos: List<Pair<Double, Double>>)`, que suma `Geo.distanciaMetros` adentro de cada tramo de `Geo.tramos`;
  - `kcal(metros, pesoKg: Int?): Int?`, que da `0,5 × peso × km` redondeado a 5, o null sin peso;
  - `data class ResumenDeCaminata(duracionMs: Long, metros: Double, kcal: Int?)` y `resumen(iniciadoEn, finalizadoEn: Long?, puntos, pesoKg, ahora)`.

  T001 pasa.
- [X] T003 [P] Sumar a `shared/src/commonTest/kotlin/ar/lauta/buscarpatentes/ui/FormatosTest.kt` los textos del contrato C3: `0:07`, `0:42`, `1:05`, `26:13`; `0,0 km`, `2,1 km`; `~75 kcal`; la línea con y sin calorías.
- [X] T004 En `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/ui/Formatos.kt`:
  - `horasYMinutos(ms)`;
  - `km(metros)`, con `decimales(…, 1)`;
  - `kcal(n)`;
  - `caminata(resumen)`, que da "2,1 km · ~75 kcal", o solo los km sin peso.

  T003 pasa.
- [X] T005 [P] Sumar a `shared/src/commonTest/kotlin/ar/lauta/buscarpatentes/data/MigracionTest.kt` la prueba de la 8 a la 9:
  - sobre el esquema 7 más `SQL_7_8`, con una fila en `estado_del_juego`, `SQL_8_9` suma `pesoKg` en null;
  - no cambia ninguna otra columna de esa fila, ni las salidas.
- [X] T006 La columna del peso (D3, data-model):
  - `pesoKg: Int? = null` en `EstadoDelJuego` de `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/data/Entidades.kt`;
  - `fijarPeso(pesoKg: Int?)` en `EstadoDelJuegoDao` de `data/Daos.kt`;
  - `version = 9` en `data/BaseDeDatos.kt`;
  - `SQL_8_9` en `data/Migraciones.kt`, con la definición de columna copiada **textual** del `createAllTables` que genera KSP;
  - `MIGRACION_8_9` en `shared/src/androidMain/kotlin/ar/lauta/buscarpatentes/data/ConstruirBase.kt` y en `shared/src/iosMain/kotlin/ar/lauta/buscarpatentes/data/ConstruirBase.ios.kt`;
  - `VERSION_BASE = 9` en `respaldo/Respaldo.kt`, y el comentario de `RespaldoTest` que dice cuál es la primera versión desconocida.

  T005 pasa.
- [X] T007 En `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/ui/PantallaAjustes.kt`, el peso (FR-009):
  - un campo numérico "Peso (kg)" al lado del número que toca, que guarda con `fijarPeso`;
  - vacío lo borra; fuera de 1 a 300 no se guarda y lo dice;
  - una línea: "Se usa solo para estimar las calorías de las salidas."

**Checkpoint**: las cuentas probadas, el peso guardado y migrado.

---

## Phase 3: User Story 1 - No terminar la salida sin querer (Priority: P1) 🎯 MVP

**Goal**: terminar una salida desde la pantalla principal pide confirmación.

**Independent Test**: [quickstart](./quickstart.md) §2.

- [X] T008 [US1] En `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt`:
  - el resumen de la salida en curso: su `iniciadoEn`, leído al empezar; los puntos del efecto que ya relee el trazo con cada punto, ahora también guardados para la cuenta; y el peso, leído con el estado del juego;
  - `onRecorrido` con salida en curso abre un `AlertDialog` "¿Terminar la salida?" con "Lleva 0:42 · 2,1 km · ~75 kcal";
  - **Terminar** llama a `Grabacion.terminar()`; **Seguir** o tocar afuera lo cierran sin tocar nada (FR-001, FR-002);
  - empezar sigue sin preguntar (FR-003).

**Checkpoint**: ninguna salida termina con un toque (SC-001).

---

## Phase 4: User Story 2 - Enterarse de que la salida se cortó (Priority: P1)

**Goal**: en el iPhone, si la app muere con una salida grabando, suena una notificación en menos de
2 minutos.

**Independent Test**: [quickstart](./quickstart.md) §4 y §5.

- [X] T009 [US2] En `shared/src/iosMain/kotlin/ar/lauta/buscarpatentes/plataforma/Grabacion.ios.kt`, el aviso del contrato C5 (D4):
  - al empezar, un ciclo en el `alcance` que cada 30 s borra la entregada `salida-cortada` y programa otra para dentro de 75 s, con el mismo identificador;
  - al terminar, cancela el ciclo y borra la pendiente y la entregada (FR-013);
  - el texto de C5.
- [X] T010 [US2] En `shared/src/iosMain/kotlin/ar/lauta/buscarpatentes/MainViewController.kt`, al arrancar, borrar la notificación **pendiente** `salida-cortada`: si la app vuelve antes de los 75 s, no tiene que sonar después. La entregada queda, y el cartel de hoy sigue (FR-022).
- [ ] T011 [US2] Verificar el Android (D5, quickstart §5) con el jugador: sacar la app de recientes con una salida en curso y leer por adb si el servicio sigue y si la app quedó detenida a la fuerza. Si HyperOS la mata **sin** forzar la detención, sumar acá una tarea nueva con el mismo aviso hecho con `AlarmManager`.

**Checkpoint**: una salida cortada avisa en el iPhone (SC-004) y no avisa de más (SC-005).

---

## Phase 5: User Story 3 - Ver cuánto lleva la salida (Priority: P2)

**Goal**: la barra de arriba dice tiempo, km y calorías de la salida en curso.

**Independent Test**: [quickstart](./quickstart.md) §3.

- [X] T012 [P] [US3] En `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/ui/BarraDelJuego.kt`, un parámetro `salidaEnCurso: String? = null`: con valor, una línea más en la tarjeta, debajo de la cuenta, con el color primario. Sin valor, la barra queda como hoy (FR-004, FR-006).
- [X] T013 [US3] En `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/ui/PantallaPrincipal.kt`:
  - un ciclo de 20 s mientras hay salida, que refresca `ahora`;
  - pasarle a la barra `horasYMinutos` y `caminata` del resumen de T008, por ejemplo "0:42 · 2,1 km · ~75 kcal" (FR-005).

**Checkpoint**: la salida se lee en la pantalla principal sin abrir nada (SC-002).

---

## Phase 6: User Story 4 - Saber cuánto se caminó y cuánto se gastó (Priority: P3)

**Goal**: km y calorías de cada salida en la lista y el detalle de Salidas.

**Independent Test**: [quickstart](./quickstart.md) §6.

- [X] T014 [US4] En `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/ui/PantallaRecorridos.kt`:
  - el peso, leído con las salidas;
  - el resumen de cada una, con `Caminata.resumen` sobre `salida.puntos`;
  - la fila: "1 h 5 min · 3,2 km · ~110 kcal · 4 patentes" (contrato C3);
  - el detalle: la misma línea, y sin peso "Cargá tu peso en Ajustes para ver las calorías" (FR-007 a FR-010);
  - la lista sigue sin totales arriba (FR-020a de la 004).

**Checkpoint**: las salidas viejas también muestran km y calorías.

---

## Phase 7: User Story 5 - Ver cuánto se caminó por día, semana y mes (Priority: P3)

**Goal**: la pantalla de estadísticas, desde Salidas.

**Independent Test**: [quickstart](./quickstart.md) §7.

- [X] T015 [P] [US5] Escribir `shared/src/commonTest/kotlin/ar/lauta/buscarpatentes/domain/EstadisticasTest.kt` con el contrato C4:
  - **períodos**: la semana empieza el lunes, también un domingo a la noche; el mes, el día 1; el anterior de enero es diciembre del año anterior;
  - **qué cuenta**: una salida que cruza la medianoche cuenta en el día en que empezó; una patente fuera de una salida cuenta;
  - **sumas**: los totales suman salidas, duración, metros, kcal y patentes, y sin peso las kcal dan null;
  - **barras**: son 7, 8 o 12 y terminan en el período elegido; el siguiente del actual no existe.
- [X] T016 [US5] Crear `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/domain/Estadisticas.kt` (D7, data-model):
  - `enum TipoDePeriodo { DIA, SEMANA, MES }`;
  - `data class Periodo(tipo, inicio: LocalDate)` con `actual(tipo, ahora, zona)`, `anterior()`, `siguiente()` y si contiene un instante;
  - `TotalesDelPeriodo` y `totales(periodo, salidas, patentes, zona)`;
  - `barras(periodo, …)`.

  T015 pasa.
- [X] T017 [US5] Crear `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/ui/PantallaEstadisticas.kt`:
  - tres pestañas, Día, Semana y Mes, y el título del período, por ejemplo "Hoy", "Semana del 12/10" u "Octubre 2026";
  - flechas, con la de adelante apagada en el actual;
  - los totales (FR-016);
  - las barras como `Box` en una `Row`, alto proporcional a los km y la del período elegido resaltada (FR-018), con la etiqueta corta de cada una.

  Arranca en la pestaña Semana, y tocar una barra lleva a ese período.
- [X] T018 [US5] En `shared/src/commonMain/kotlin/ar/lauta/buscarpatentes/ui/PantallaRecorridos.kt`, un botón "Estadísticas" arriba de la lista, que abre la pantalla de T017 como un estado más adentro de Salidas, con su `BackHandler` (FR-015).

**Checkpoint**: las cinco historias completas.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T019 Correr `./gradlew :shared:testAndroidHostTest :app:assembleDebug`. Todo verde, sin warnings nuevos.
- [X] T020 [P] `README.md`:
  - la 010 en la lista de specs y en los quickstarts;
  - `CaminataTest` y `EstadisticasTest` en la tabla de pruebas;
  - una línea en la sección de Salidas sobre km, calorías y estadísticas.
- [ ] T021 Armar lo que el jugador instala:
  - **Android**: el APK a `/sdcard/Download/buscar-patentes.apk`, pisando el anterior y borrando cualquier otro `buscar-patentes*.apk`, sin tocar `NavegadorCamiones.apk`, y verificando el hash;
  - **iPhone**: `gh workflow run ios.yml --ref 010-recorrido-en-curso`, con el `.ipa` a `Downloads\buscar-patentes-ios\recorrido-en-curso`.

  Recordarle al jugador que saque un respaldo antes de instalar: la base pasa a la 9.
- [ ] T022 El jugador corre el [quickstart](./quickstart.md) §2 a §8 en los teléfonos. Lo que falle vuelve como tarea nueva en esta fase.
- [ ] T023 Antes de cada push, revisar el diff: ni coordenadas de la zona de juego, ni nombres de calles o barrios de la lista de "Nunca subir", ni capturas del mapa, ni respaldos.

---

## Dependencies & Execution Order

- **Fase 2 antes que todo**: T001 → T002, T003 → T004, T005 → T006 → T007. T001, T003 y T005 van en paralelo.
- **US1** (T008) necesita T002, T004 y T006.
- **US2** (T009 a T011) es independiente de las demás: se puede hacer después de la fase 2, o antes.
- **US3** (T012, T013) necesita el resumen de T008: va después de US1.
- **US4** (T014) necesita la fase 2.
- **US5** (T015 a T018) necesita la fase 2. T018 toca el mismo archivo que T014: va después.
- **Polish** al final.

## Parallel Example: Phase 2

```text
T001 CaminataTest.kt   |   T003 FormatosTest.kt   |   T005 MigracionTest.kt
```

## Parallel Example: después de la fase 2

```text
T009 Grabacion.ios.kt (US2)   |   T012 BarraDelJuego.kt (US3)   |   T015 EstadisticasTest.kt (US5)
```

## Implementation Strategy

1. **MVP**: la fase 2, US1 y US2. Son las dos que evitan perder o cortar una salida sin enterarse.
2. **Después**: US3 (la barra), US4 (Salidas) y US5 (estadísticas), en ese orden. Cada una se puede
   probar sola con su sección del quickstart.
3. Un solo build para los teléfonos al final (T021): cada instalación le cuesta un rato al jugador.
