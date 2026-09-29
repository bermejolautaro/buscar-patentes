# Implementation Plan: Planear el próximo recorrido

**Branch**: `004-planear-recorridos` | **Date**: 2026-08-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-planear-recorridos/spec.md`

## Summary

La 003 dibujó el camino recorrido y lo pintó destacando la última salida. En uso resultó que
eso contesta una pregunta que nadie se hace. Esta feature invierte la jerarquía —el histórico
completo pasa a ser el dato— y le pone al mapa un interruptor de tres estados: **cobertura**
(todo igual, para ver el hueco), **antigüedad** (tres escalones de tiempo, para saber qué
revisitar) y **apagado**. De paso rehace la pantalla de Salidas, que hoy mete un mapa de 220 dp
adentro de cada fila de una lista.

La clarificación agregó una cuarta pieza que no estaba en la spec original: al pasar el trazo
a azul de ruta, los marcadores se rediseñan a **pin blanco con el número en negro**, con el
borde diciendo la probabilidad. Y el color que distinguía pendiente / ya toca / compartida se
retira del mapa, porque el jugador no sabía que existía.

Enfoque técnico: **ninguna dependencia nueva, y buena parte del trabajo es borrar**. Todo sale
de datos ya guardados. Lo único que se persiste es una preferencia —el modo del mapa— y va en
la tabla que ya guarda las preferencias del jugador. La lógica no trivial es una sola función
pura, la que convierte una fecha en un escalón de antigüedad, y esa es la prueba nueva de la
feature.

Usarla caminando agregó dos piezas más, y las dos siguen la misma regla —ninguna dependencia
nueva, ningún dato nuevo—: un botón que **esconde las patentes** del mapa (US5), que es no
mandarle los marcadores a `MapaDeFondo`, y **la confirmación de duplicados** (US6): cargar un
número que ya está anotado a menos de diez metros sube la confianza del registro que existe en
vez de apilar un pin encima. La segunda no toca la base: `porNumero` ya tiene su índice,
`Geo.distanciaMetros` ya es puro y la tabla `voto` de la 003 ya era el lugar donde vive "la volví
a ver". La lógica no trivial que suma es una sola función pura —el candidato más cercano dentro
de un radio— y deja su prueba en `GeoTest`.

El detalle y las alternativas descartadas están en [research.md](./research.md).

## Technical Context

**Language/Version**: Kotlin 2.3.21, JDK 17. Sin cambios respecto de la 003.

**Primary Dependencies**: las mismas — Compose con Material 3, Room, Play Services Location,
MapLibre Native Android, `androidx.activity`. **Ninguna nueva.** D10 rechaza Navigation-Compose
para el detalle de salida; D1 rechaza cualquier biblioteca de iconos de mapa.

**Storage**: Room. **Una columna nueva** —`estado_del_juego.modoMapa`— y su migración v5 → v6.
Ninguna entidad nueva, ninguna consulta nueva: los trazos, las fechas y las probabilidades ya
se leen todos hoy.

**Testing**: JUnit sobre dominio en JVM. Un archivo nuevo (`AntiguedadTest`) por la función de
escalones de D9. `ColeccionTest` se amplía para el orden de dibujo de D5. Se borran las
aserciones de `claseDe` que quedan sin objeto.

**Target Platform**: Android 8.0 (API 26) o superior. Validación en el teléfono de pruebas, que
corre HyperOS sin SIM: el APK se instala a mano.

**Project Type**: mobile-app, un solo módulo Android. Sin cambios de estructura.

**Performance Goals**: el mapa se desplaza sin tirones en los tres modos (SC-008). Cambiar de
modo no reconstruye la fuente: toca propiedades de capa (D4), así que es instantáneo.

**Constraints**: la captura sigue sin depender de nada de esto. Ningún modo pide red (FR-026).
La evidencia sigue intacta: la única escritura nueva es una preferencia de interfaz.

**Scale/Scope**: cinco pantallas más una sub-pantalla, dos superficies de mapa, decenas de
salidas y cientos de registros. Un usuario, un teléfono.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitución **1.1.0**, sin enmiendas nuevas: esta feature no necesita ninguna.

### Principio I — Captura sin fricción (NO NEGOCIABLE)

**Gate**: la carga rápida llega de app cerrada a registro guardado en 4 interacciones o menos.

| Momento | Resultado |
|---|---|
| Pre-Phase 0 | **PASS** — nada de esta feature toca el camino de captura. El interruptor, la leyenda y Salidas están todos fuera de él |
| Post-Phase 1 | **PASS** — con una vigilancia, abajo |

**Riesgo vigilado**: el interruptor se suma a la franja de acciones que la 003 dejó con tres
botones (D7). Un cuarto botón le come ancho al campo de número, que es la pieza del camino de
captura. El contrato C4 le pone techo: el campo no baja del ancho que tiene hoy, y si no
entran los cuatro, sale el interruptor de la franja, no el ancho del campo.

### Principio II — La evidencia es inmutable (NO NEGOCIABLE)

**Gate**: ubicación, precisión y timestamp no se pueden editar después de creados.

**PASS.** La única escritura nueva de la feature es `estado_del_juego.modoMapa`, que es una
preferencia de cómo se mira el mapa. No hay ni una sentencia que nombre un campo de
`registro_de_captura`, de `punto_de_trayecto` ni de `recorrido`. `InmutabilidadTest` sigue en
verde sin cambios.

La US6 confirma patentes que ya existen, así que vale mirarla de cerca: **no reescribe nada**. El
dato nuevo entra como una fila en `voto`, con su propia hora, y lo único que puede tocar del
registro es `fotoRuta`, y solo cuando estaba vacía —el mismo update acotado que el FR-019 ya
permitía—. La ubicación, la precisión y el momento del registro confirmado siguen siendo los de
la primera captura. Confirmar es agregar una observación, no corregir el pasado (FR-039a).

Vale decirlo explícito porque esta feature **retira** algo del mapa: el color de estado. Retirar
una lectura no es tocar el dato. La columna `estado` sigue existiendo, sigue escribiéndose al
compartir y sigue haciendo su trabajo en avisos y cobertura (FR-030b).

### Principio III — Local-first, sin red

**Gate**: toda captura se completa sin conexión; la red solo para funciones accesorias.

**PASS, y esta feature no usa red para nada.** Los tres modos, la escala de antigüedad, el
"hace cuánto" de Salidas y los pines nuevos salen todos de lo que ya está en el teléfono. El
único consumo de red del área —el ajuste a calles de la 003— no se toca.

### Principio IV — Alcance personal (YAGNI)

**Gate**: sin cuentas ni backend; toda abstracción con dos usos reales; toda dependencia
justificada contra lo instalado.

**PASS.** El saldo de la feature es negativo en código:

| Se agrega | Se borra |
|---|---|
| `domain/Antiguedad.kt` (una función pura y su prueba) | `Trazo.destacado` y su expresión de dos colores |
| `ModoMapa` (enum de 3 valores) y una columna | `claseDe`, `colorPorClase`, `ColoresDeMapa.PENDIENTE/TOCA/COMPARTIDA` |
| Un botón y una línea de leyenda | `Marcador.compartida` y sus tres call sites |
| Los pines (una capa de símbolo reemplaza una de círculo) | `RECORRIDO_VIEJO`, y el mapa embebido de la lista de Salidas |

**Justificación de la única dependencia nueva: no hay ninguna.** Los pines se dibujan con
`android.graphics.Canvas`, que viene con la plataforma (D1). El detalle de salida es un estado
`Long?` dentro de la pantalla que ya existe, no un destino de navegación (D10).

**Abstracción con un solo uso, declarada**: `ModoMapa` tiene un solo consumidor —el mapa de la
pantalla principal—. Se justifica porque es un valor persistido con tres estados, no una
indirección: sin el enum sería un `String` sin validar en la base y tres comparaciones de texto
repartidas por la interfaz.

### Restricciones técnicas

**PASS.** Ubicación y almacenamiento local sin cambios. Los formatos de patente no se tocan.

### Flujo de desarrollo

**PASS.** La lógica no trivial de la feature es la función de escalones de antigüedad, y deja
prueba ejecutable en JVM (`AntiguedadTest`). El resto es dibujo y layout, que no se prueba en
JUnit y se valida en el teléfono con [quickstart.md](./quickstart.md).

### Re-check post-Phase 1

Los cinco gates se vuelven a mirar contra el diseño ya hecho, no contra la intención.

| Gate | Resultado | Qué cambió en Phase 1 |
|---|---|---|
| I — Captura sin fricción | **PASS con vigilancia** | El diseño confirmó que el interruptor entra en la franja que comparte con el campo de número. El contrato C4 le puso techo por escrito: si no entran, sale el interruptor, no el ancho del campo |
| II — Evidencia inmutable | **PASS** | D6 concretó la única escritura de la feature, y es una preferencia de interfaz. Ninguna sentencia nombra un campo de evidencia |
| III — Local-first | **PASS** | Ninguna de las doce decisiones introdujo una llamada de red |
| IV — Alcance personal | **PASS** | D1, D5, D6 y D10 se resolvieron todas hacia la opción más chica. El saldo de código sigue siendo negativo |
| Flujo de desarrollo | **PASS** | La prueba nueva quedó nombrada y con sus casos en data-model.md |

**Deuda declarada, para que no se pierda**: dos decisiones se toman mirando el teléfono y no en
este documento —los valores hex de D11 y el radio de agrupación de D12—, y una tiene plan B
nombrado por si el teléfono la desmiente: el orden de dibujo de D5. Ninguna de las tres es una
pregunta abierta de diseño; las tres son calibración.

## Project Structure

### Documentation (this feature)

```text
specs/004-planear-recorridos/
├── plan.md              # Este archivo
├── research.md          # Phase 0: las decisiones y lo descartado
├── data-model.md        # Phase 1: la columna nueva y las lecturas derivadas
├── quickstart.md        # Phase 1: cómo se valida en el teléfono
├── contracts/
│   └── mapa-y-salidas.md
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks — no lo crea este comando)
```

### Source Code (repository root)

```text
app/src/main/java/ar/lauta/buscarpatentes/
├── data/
│   ├── BaseDeDatos.kt        # + MIGRACION_5_6
│   ├── Entidades.kt          # + EstadoDelJuego.modoMapa, + enum ModoMapa, + convertidores
│   └── Daos.kt               # + fijarModoMapa
├── domain/
│   └── Antiguedad.kt         # NUEVO: fecha -> escalón. La prueba de la feature
├── mapa/
│   └── Mapa.kt               # Trazo sin destacado, pines, color por modo, orden de dibujo
└── ui/
    ├── PantallaPrincipal.kt  # arma trazos con escalón, cicla el modo, lo persiste
    ├── BotonesAccion.kt      # sin cambios
    ├── LeyendaDelMapa.kt     # NUEVO: la línea que nombra el modo y explica la escala
    ├── PantallaRecorridos.kt # lista compacta + sub-pantalla de detalle
    └── Tema.kt               # ColoresDeMapa: entran los del trazo, salen los de estado

app/src/test/java/ar/lauta/buscarpatentes/
├── domain/AntiguedadTest.kt  # NUEVO
└── mapa/ColeccionTest.kt     # + orden de dibujo, - aserciones de clase
```

**Structure Decision**: sin cambios de estructura. La feature vive en los cuatro paquetes que
ya existen. El único archivo de dominio nuevo es la función pura que la constitución obliga a
poder probar en JVM; el único de interfaz nuevo es la leyenda, que se compone en la pantalla
principal.

## Complexity Tracking

Sin violaciones que justificar. Las tres decisiones que podrían haber costado complejidad se
resolvieron hacia abajo, y están argumentadas en research.md:

| Tentación | Qué se hizo en cambio |
|---|---|
| Navigation-Compose para el detalle de salida | Un `Long?` y un `BackHandler` dentro de la pantalla que ya existe (D10) |
| Calcular intersecciones de geometría para la antigüedad por tramo | Dibujar las salidas de la más vieja a la más nueva y dejar que se tapen (D5) |
| Una capa de mapa por modo | Una capa, y el modo cambia propiedades de pintura (D4) |
| DataStore para la preferencia del modo | Una columna en la tabla que ya guarda preferencias (D6) |
