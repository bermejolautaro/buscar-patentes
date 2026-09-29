# Implementation Plan: Rumbo y aspecto

**Branch**: `003-rumbo-y-aspecto` | **Date**: 2026-08-29 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-rumbo-y-aspecto/spec.md`

## Summary

Primera feature que nace de haber usado la app caminando. Cierra el círculo que la 001 abrió
al guardar la ubicación —ahora se puede volver hasta una patente—, reemplaza la grilla de
rectángulos por el trazo del camino efectivamente recorrido, hace que Buscar conteste antes
de que el jugador escriba nada, y saca el violeta que Material 3 estaba poniendo por default.

Enfoque técnico: **ninguna dependencia nueva, y una parte del trabajo es borrar**. El trazo
usa el mismo mecanismo de fuente, capa y expresión que la 002 ya usa para los marcadores. El
trayecto lo resuelve la app de mapas del teléfono con un intent de la plataforma. La distancia
y el rumbo son quince líneas de matemática pura en `domain/`, que es además la única prueba
nueva de la feature. La barra de navegación se arregla con una línea. Y la grilla de cobertura
se va con todo lo que existía solo para ella.

El detalle y las alternativas descartadas están en [research.md](./research.md).

## Technical Context

**Language/Version**: Kotlin 2.3.21, JDK 17. Sin cambios respecto de la 002.

**Primary Dependencies**: las mismas — Compose con Material 3, Room, Play Services Location,
MapLibre Native Android, `androidx.activity` 1.12.4. **Ninguna nueva.** D3 rechaza Robolectric
y maps-android-utils explícitamente; D2 rechaza cualquier servicio de ruteo.

**Storage**: Room, sin entidades, campos ni migración nuevas. **Sin consultas nuevas**: D8
resuelve los recientes y el filtrado incremental en memoria sobre `todosUnaVez()`, que ya
existe. La única consulta que se toca es la de puntos de trayecto, para que devuelva ordenado
por recorrido.

**Testing**: JUnit sobre dominio en JVM. Un archivo nuevo, por la geometría de D3. Cinco
pruebas borradas junto con la grilla.

**Target Platform**: Android 8.0 (API 26) o superior. El dispositivo de validación corre
Android 15 con navegación por gestos, que es donde apareció el defecto de la barra del sistema.

**Project Type**: mobile-app, un solo módulo Android. Sin cambios de estructura.

**Performance Goals**: el mapa con todos los recorridos dibujados se desplaza sin tirones
(SC-005). La aritmética de D9 da holgada: una hora de caminata deja del orden de cien puntos,
y cincuenta salidas unos pocos miles.

**Constraints**: la captura sigue sin depender de nada de esto (FR-005, hereda FR-009 de la
002 y FR-038 de la 001). La evidencia sigue intacta: esta feature **no escribe en la base**.
Nada del camino de captura necesita red.

**Scale/Scope**: cinco pantallas, dos superficies de mapa, decenas de salidas y cientos de
registros. Un usuario, un teléfono.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitución **1.1.0**, sin enmiendas nuevas: esta feature no necesita ninguna.

### Principio I — Captura sin fricción (NO NEGOCIABLE)

**Gate**: la carga rápida llega de app cerrada a registro guardado en 4 interacciones o menos.

| Momento | Resultado |
|---|---|
| Pre-Phase 0 | **PASS** — nada de esta feature toca el camino de captura. Todo lo nuevo cuelga de la ficha de un registro, de Buscar o de Salidas, que están fuera de ese camino |
| Post-Phase 1 | **PASS** — y la US4 lo mejora de rebote: achicar la franja de acciones (FR-018) devuelve pantalla sin agregar ningún paso |

**Riesgo vigilado**: el FR-019 prohíbe que achicar la franja haga los botones más difíciles de
acertar. Un objetivo táctil más chico no cuesta una interacción más en el papel, pero sí en la
vereda cuando se falla el primer toque. El SC-010 lo mide junto con el techo de 4.

### Principio II — La evidencia es inmutable (NO NEGOCIABLE)

**Gate**: ubicación, precisión y timestamp no se pueden editar después de creados.

**PASS, y del modo más fuerte posible: esta feature no escribe absolutamente nada en la
base.** Lee registros, lee puntos de trayecto, lee el estado del juego. No hay ni una sentencia
de escritura nueva, así que no hay superficie por la que el principio pueda romperse.

La prueba de inmutabilidad existente sigue en verde sin que haya nada nuevo que cubra.

Un detalle que sí toca el principio de refilón y queda del lado correcto: la distancia hasta
una patente **se calcula al mirarla y no se guarda**. Guardarla sería meter en el registro un
dato derivado de dónde estaba el jugador otro día, que es exactamente la clase de metadata
retroactiva que el principio prohíbe.

### Principio III — Local-first, sin red

**Gate**: toda captura se completa sin conexión; fallar es ruidoso.

**PASS** — y la respuesta a la clarificación 3 lo refuerza por diseño. La distancia y el rumbo
se muestran **siempre**, no como respaldo, así que quedarse sin señal no activa ningún camino
de excepción: simplemente no se puede abrir la app de mapas, y lo que ya estaba en pantalla
sigue sirviendo.

El trayecto delegado es una función accesoria, que es justo lo que el principio permite que
use red. La captura no lo toca.

### Principio IV — Alcance personal (YAGNI)

**Gate**: sin cuentas ni backend; toda abstracción con dos usos reales; toda dependencia
justificada contra lo ya instalado.

**PASS**, y este principio es el que más forma le dio al plan. Tres veces mandó **no**
construir:

- **Ninguna dependencia nueva.** D2 usa un intent de la plataforma en vez de un servicio de
  ruteo. D3 escribe quince líneas de haversine en vez de sumar Robolectric o maps-utils. D7
  usa `enableEdgeToEdge()`, que ya está en el classpath.
- **Ninguna consulta nueva** (D8): los recientes y el filtrado por prefijo salen de la lista
  que ya se lee entera, porque la escala lo permite con holgura.
- **Ninguna optimización preventiva** (D9): la aritmética dice que el trazo no puede poner
  lento el mapa, así que no se simplifica la línea hasta que alguien vea un tirón.

Y una vez mandó **borrar**: D4 retira la grilla y todo lo que existía solo para ella, en lugar
de dejar el cálculo vivo por si acaso.

La única abstracción nueva —el mapa que sirve a dos pantallas (D5)— tiene los dos usos reales
que el principio exige, presentes y concretos. Es el caso que el principio admite, no el que
descarta.

### Flujo de desarrollo

**Gate**: toda lógica no trivial deja una prueba ejecutable.

**PASS** — la geometría de D3 es la única lógica pura que la feature agrega, y deja su prueba.
La conversión de rumbo a punto cardinal se rompe en los bordes —el cruce por 0°, el redondeo
entre sectores— sin que se note mirando el teléfono, que es exactamente el caso que el
principio quiere cubierto.

### Resultado del gate

**Los cuatro principios pasan.** Complexity Tracking queda vacío.

## Project Structure

### Documentation (this feature)

```text
specs/003-rumbo-y-aspecto/
├── plan.md              # Este archivo
├── spec.md              # Qué se construye, con 4 clarificaciones resueltas
├── research.md          # Phase 0: D1 a D9
├── data-model.md        # Phase 1: lo que se lee, lo que se deriva y lo que se borra
├── quickstart.md        # Phase 1: cómo validar cada user story
├── contracts/
│   └── ui-y-mapa.md     # Phase 1: C1 a C5, la superficie de interfaz
├── checklists/
│   └── requirements.md  # Calidad de la spec — 16/16
└── tasks.md             # Phase 2 (/speckit-tasks — todavía no existe)
```

### Source Code (repository root)

Archivos que esta feature toca. La estructura de la 002 no cambia: un solo módulo, con
`domain/` como única división.

```text
app/src/main/java/ar/lauta/buscarpatentes/
├── domain/
│   ├── Geo.kt                     # D3: distancia, rumbo y punto cardinal — archivo nuevo
│   └── Cobertura.kt               # D4: se le va la mitad de la grilla
├── ui/
│   ├── Tema.kt                    # D6: el esquema completo, y se va COBERTURA
│   ├── MainActivity.kt            # D7: enableEdgeToEdge()
│   ├── PantallaPrincipal.kt       # Trazos en el mapa, campo con fondo, franja más baja
│   ├── PantallaBusqueda.kt        # Sugerencia, recientes, filtrado incremental, campo abajo
│   ├── PantallaRecorridos.kt      # D5: mapa de una salida en la fila expandida
│   ├── FichaDeRegistro.kt         # Distancia, rumbo y la acción de ir hasta la patente
│   ├── BotonesAccion.kt           # FR-018: la franja más baja
│   └── Ir.kt                      # D2: el intent al mapa del teléfono — archivo nuevo
├── data/Daos.kt                   # Puntos de trayecto ordenados por recorrido
└── mapa/Mapa.kt                   # D1 trazo, D4 se va la cobertura, D5 parámetros

app/src/test/java/ar/lauta/buscarpatentes/
├── domain/GeoTest.kt              # D3: la única prueba nueva — archivo nuevo
└── CoberturaTest.kt               # D4: se le van las cinco pruebas de la grilla
```

**Structure Decision**: sin cambios. Un solo módulo `app/`, con `domain/` como la única
división —lógica pura sin Android, testeable en JVM—. Esta feature es la primera desde la 001
que agrega algo a `domain/`, y por la razón correcta: `Geo` es matemática sin plataforma.

Dos archivos de producción nuevos, cada uno contra un requisito concreto: `Geo.kt` por FR-003
y `Ir.kt` por FR-001. Ninguno es andamiaje.

## Orden de ataque sugerido

No es dependencia técnica estricta salvo donde se dice, pero importa:

1. **D7, la barra del sistema.** Una línea, arregla un defecto visible, y no depende de nada.
   Sacarla del camino primero evita que se pierda detrás del trabajo grande.
2. **D6, la paleta.** Bloquea todo lo demás de la US4 y tiñe cada pantalla que se toque
   después. Hacerlo tarde obliga a revisar dos veces lo que ya se miró.
3. **D4, retirar la grilla.** Va **antes** del trazo, no después: dejarla mientras se dibuja el
   trazo significa mirar las dos cosas encimadas y no poder juzgar ninguna.
4. **D1 y D5, el trazo y el mapa de una salida.** El grueso de la US2.
5. **D3 y D2, distancia, rumbo e ir hasta la patente.** La US1, que es la P1 de la spec. Va
   acá y no primero porque comparte la ficha con nada más, así que no bloquea a nadie, y
   conviene verla sobre la paleta ya arreglada.
6. **La US3, Buscar.** Independiente del resto, y la que menos riesgo tiene.
7. **Lo que queda de la US4**: campo con fondo, franja más baja.

La US1 es P1 en la spec y va quinta acá a propósito: el orden de la spec es por valor para el
jugador, el de acá es por dependencia entre archivos. No es una contradicción, y si hay que
cortar la feature por la mitad, se corta por valor y no por este orden.

## Complexity Tracking

> Se completa solo si el Constitution Check tiene violaciones que justificar.

**Vacío.** Ninguna violación. La única tensión candidata —la segunda superficie de mapa que
salió de la clarificación 4— resulta ser el caso que el Principio IV admite y no el que
descarta: dos usos reales, presentes y concretos, para una abstracción que ya existe y solo
hay que parametrizar.
