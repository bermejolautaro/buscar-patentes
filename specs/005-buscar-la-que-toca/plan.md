# Implementation Plan: Ir a buscar la que toca

**Branch**: `005-buscar-la-que-toca` | **Date**: 2026-09-28 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-buscar-la-que-toca/spec.md`

## Summary

La app sabe dónde están las patentes del número que toca, pero no lleva al jugador hasta ellas:
se esconden en grupos, la barra de arriba muestra una cifra que nadie entiende y no hace nada al
tocarla, la lista está en otra pantalla sin la confianza, y "Ir" va de a una. Esta feature hace
de la barra de arriba el centro de todo: dice **cuántas hay**, su **lupa filtra** el mapa y lo
encuadra, **tocarla despliega** la lista ordenada por distancia y confianza, y desde la lista se
**arma un recorrido** que se abre en Google Maps. La que toca vuelve a tener **fondo verde** y
deja de poder quedar adentro de un grupo. La pantalla de búsqueda se retira.

Enfoque técnico: **ninguna dependencia nueva y ningún cambio en la base**. La que toca pasa a una
fuente de MapLibre propia, sin agrupar (D1). El filtro es un `Int?` de pantalla (D4). Lo que tiene
lógica de verdad son tres funciones puras con su prueba: el texto de descubiertas (D3), el orden
de revisión y el del recorrido (D8, D9), y la URL de Google Maps (D11). La US5, pines que se
corren antes de agruparse, reemplaza la agrupación de MapLibre por un acomodo propio (D13): es la
pieza de más riesgo, va última y tiene línea de corte.

El detalle y las alternativas descartadas están en [research.md](./research.md).

## Technical Context

**Language/Version**: Kotlin 2.3.21, JDK 17. Sin cambios.

**Primary Dependencies**: las mismas — Compose con Material 3, Room 2.8.4, Play Services
Location 21.4.0, MapLibre Native Android 13.6.0. **Ninguna nueva.** Google Maps se abre por
`Intent`, no se integra (D11).

**Storage**: Room, **sin cambios**. Ni tablas, ni columnas, ni migración. La base queda en la v7
del 2026-09-28.

**Testing**: JUnit sobre dominio en JVM. Nuevos: `PrioridadTest`, `IrTest` y, si la US5 entra,
`AcomodoTest`. Cambian: `CoberturaTest` (sale la cuenta de seguidos, entra descubiertas) y
`ColeccionTest` (la que toca en su colección). `InmutabilidadTest` sin cambios.

**Target Platform**: Android 8.0 (API 26) o superior. Validación en el teléfono de pruebas, sin
SIM: el APK va a `Download/buscar-patentes.apk` y se instala a mano.

**Project Type**: mobile-app, un solo módulo Android.

**Performance Goals**: el mapa se desplaza sin tirones con el filtro puesto o sacado (SC-008 de la
004 sigue vigente). Filtrar y quitar el filtro no recargan el estilo: cambian el GeoJSON de dos
fuentes. El acomodo de la US5 corre solo al cambiar de zoom entero, nunca por cuadro.

**Constraints**: la carga rápida no se toca, y gana ancho (sale un botón de la franja). Todo sin
red salvo abrir Google Maps. La única escritura es adjuntar foto, que ya existía.

**Scale/Scope**: cientos de registros, una decena por número como mucho, recorridos de hasta diez
paradas. Un usuario, un teléfono.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitución **1.1.0**, sin enmiendas nuevas.

### Principio I — Captura sin fricción (NO NEGOCIABLE)

**Gate**: de app cerrada a registro guardado en 4 interacciones o menos.

| Momento | Resultado |
|---|---|
| Pre-Phase 0 | **PASS** — el campo de filtro vive en la barra de arriba y solo existe si se abre. El camino de carga no pasa por él |
| Post-Phase 1 | **PASS**, con una vigilancia |

**Riesgo vigilado**: con el filtro abierto hay **dos** campos numéricos en pantalla. El contrato
C1 los separa por lugar (arriba y abajo) y por vida (el de arriba solo existe si el jugador lo
abrió). Tocar el campo de carga con el filtro abierto le pasa el foco, y guardar funciona igual:
lo cubre un caso borde de la spec y un paso del quickstart.

**A favor**: la franja de acciones pierde el botón de búsqueda, así que el campo de carga gana
ancho.

### Principio II — La evidencia es inmutable (NO NEGOCIABLE)

**Gate**: ubicación, precisión y timestamp no se editan después de creados.

**PASS.** Ninguna sentencia nueva. La única escritura es `adjuntarFoto`, que ya existía y toca solo
`fotoRuta`: se muda de la pantalla de búsqueda a la ficha. El acomodo de la US5 corre los pines
**en el dibujo**. La ficha, "Ir", la distancia y el recorrido reciben el registro por `id`, nunca
la posición corrida (data-model, FR-023b).

### Principio III — Local-first, sin red

**Gate**: la captura no depende de la red; la red solo para funciones accesorias.

**PASS.** Filtro, lista, orden, acomodo y encuadre salen de lo que está en el teléfono. La red
aparece en un solo lugar, y es de otra app: Google Maps, cuando se abre el recorrido.

### Principio IV — Alcance personal (YAGNI)

**Gate**: sin cuentas ni backend; toda abstracción con dos usos reales; toda dependencia
justificada.

**PASS**, con una complejidad declarada en Complexity Tracking.

| Se agrega | Se borra |
|---|---|
| `domain/Prioridad.kt` (dos funciones puras) | `PantallaBusqueda.kt` entera (406 líneas) y su destino |
| `Ir.urlDeRecorrido` y `Ir.enGoogleMaps` | `Cobertura.tieneElActual` y `consecutivosCubiertos` |
| Una fuente y una capa de MapLibre para la que toca | `porToca(...)` y la propiedad `toca` del feature |
| La barra con lupa y la lista desplegable | `IndicadorDelJuego` |
| El diálogo del recorrido | Un botón de la franja |
| `domain/Acomodo.kt`, si la US5 entra | `opcionesDeAgrupacion`, si la US5 entra |

**Abstracción con un solo uso, declarada**: `Candidato` (data-model) tiene dos consumidores reales,
la lista y el diálogo del recorrido, así que pasa el gate. Existe para que `domain/` no dependa de
Room, igual que `Geo.masCercano` recibe un selector en vez de un `RegistroDeCaptura`.

### Restricciones técnicas

**PASS.** Ubicación y almacenamiento sin cambios. Los formatos de patente no se tocan: la lista
muestra el texto completo si lo hay, igual que la pantalla que se va.

### Flujo de desarrollo

**PASS.** Cada rama nueva tiene su prueba: el texto de descubiertas (3 ramas), el orden de
revisión (con y sin posición, empates), el orden de paradas (vecino más cercano), la URL, y
**qué hacer según cuántas paradas hay incluidas** (`Ir.accionPara`: nada, "Ir", sobran o abrir
Maps). Esa última vivía en el composable del diálogo, y el análisis de consistencia la sacó a una
función pura para que tuviera prueba. El acomodo, si entra, deja `AcomodoTest`. Lo que queda sin
JUnit es pegamento de plataforma —la cadena de `Intent` con su respaldo, el campo que acepta hasta
tres dígitos— y composición y dibujo, que se validan con [quickstart.md](./quickstart.md).

### Re-check post-Phase 1

| Gate | Resultado | Qué cambió en Phase 1 |
|---|---|---|
| I — Captura sin fricción | **PASS con vigilancia** | C1 fijó que el campo de filtro vive arriba y solo mientras está abierto; C7 confirmó que la franja pierde un botón |
| II — Evidencia inmutable | **PASS** | data-model dejó escrito que ninguna función recibe la posición corrida del acomodo |
| III — Local-first | **PASS** | D11 es la única salida a red, y es un `Intent` a otra app |
| IV — Alcance personal | **PASS con complejidad declarada** | D13 reemplaza la agrupación de MapLibre por código propio. Justificado abajo y con línea de corte |
| Flujo de desarrollo | **PASS** | Las cuatro pruebas nuevas quedaron nombradas y con sus casos en data-model y quickstart |

**Deuda declarada**: `PESO_CONFIANZA` (D8) y `LIMITE_ACOMODO_M` (D13) se calibran en la calle, y
los límites de paradas de Google Maps (D11) salen de su documentación y hay que confirmarlos en el
teléfono. Son calibración, no preguntas de diseño abiertas.

## Project Structure

### Documentation (this feature)

```text
specs/005-buscar-la-que-toca/
├── plan.md              # Este archivo
├── research.md          # Phase 0: D1 a D14
├── data-model.md        # Phase 1: estado de pantalla y funciones puras
├── quickstart.md        # Phase 1: cómo se valida
├── contracts/
│   └── pantalla-principal.md   # C1 a C7
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks — no lo crea este comando)
```

### Source Code (repository root)

```text
app/src/main/java/ar/lauta/buscarpatentes/
├── domain/
│   ├── Cobertura.kt          # - tieneElActual, - consecutivosCubiertos, + descubiertas
│   ├── Prioridad.kt          # NUEVO: Candidato, paraRevisar, ordenDeParadas
│   └── Acomodo.kt            # NUEVO, solo US5: el acomodo por zoom
├── mapa/
│   └── Mapa.kt               # fuente y capa de la que toca, pines verdes, encuadrar/centrarEn,
│                             # y en la US5, el acomodo en lugar de withCluster
└── ui/
    ├── PantallaPrincipal.kt  # la barra con lupa y lista, el filtro, sin botón de búsqueda
    ├── BarraDelJuego.kt      # NUEVO: la barra, su campo y la lista (reemplaza IndicadorDelJuego)
    ├── ArmarRecorrido.kt     # NUEVO: el diálogo de las paradas
    ├── Ir.kt                 # + urlDeRecorrido, + enGoogleMaps
    ├── LeyendaDelMapa.kt     # + "Solo la N"
    ├── FichaDeRegistro.kt    # + "Agregar foto", con el lanzador de cámara que se muda
    ├── PantallaAjustes.kt    # el texto que mandaba a Buscar
    ├── BarraSuperior.kt      # comentario: dos usos
    ├── Tema.kt               # vuelve ColoresDeMapa.TOCA
    ├── MainActivity.kt       # - Destino.BUSQUEDA
    └── PantallaBusqueda.kt   # SE BORRA

app/src/test/java/ar/lauta/buscarpatentes/
├── CoberturaTest.kt          # casos nuevos (vive en la raíz del paquete, no en domain/)
├── domain/PrioridadTest.kt   # NUEVO
├── domain/AcomodoTest.kt     # NUEVO, solo US5
├── ui/IrTest.kt              # NUEVO
└── mapa/ColeccionTest.kt     # la que toca en su colección
```

**Structure Decision**: los mismos cuatro paquetes. La barra sale a su propio archivo porque deja
de ser un indicador de diez líneas y pasa a tener campo, lista y dos estados; dejarla adentro de
`PantallaPrincipal`, que ya pasa las 750 líneas, la volvería inmanejable. El diálogo del recorrido
va aparte por lo mismo. Las funciones puras van a `domain/`, que es donde la constitución las
puede probar en JVM.

## Orden de trabajo sugerido

Cada paso deja la app usable, y la US5 puede quedarse afuera sin romper nada.

1. **US1**: la fuente de la que toca y el verde (D1, D2), y descubiertas (D3).
2. **US2**: la barra nueva con lupa y filtro, el encuadre (D4, D5, D6).
3. **Retiro de búsqueda** (D12): se hace apenas existe la lista de la US3, no antes, para no dejar
   al jugador un día sin forma de listar las patentes de un número.
4. **US3**: la lista y el orden (D7, D8).
5. **US4**: el diálogo y Google Maps (D9, D10, D11).
6. **US5**: el acomodo (D13), con su prueba en el teléfono y su línea de corte.

## Complexity Tracking

| Violación | Por qué hace falta | Alternativa más simple, descartada porque |
|---|---|---|
| Un acomodo propio (D13) en lugar de la agrupación que MapLibre trae hecha | FR-023 pide que los pines se **corran** antes de agruparse. MapLibre agrupa o esconde, pero no corre y después agrupa | Achicar el radio de agrupación: agrupa menos pero deja los pines encimados, que no es lo pedido. `text-variable-anchor`: corre, pero esconde el que no entra, y una patente que desaparece es el problema original |
| Dos fuentes de GeoJSON para la misma clase de dato (D1) | FR-002: la que toca no puede quedar en un grupo a ningún zoom, y la agrupación de MapLibre es por fuente | Filtros de capa o `clusterMaxZoom`: ninguno impide que la feature entre al grupo |
