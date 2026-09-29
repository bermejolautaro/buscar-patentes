# Phase 0 Research: Rumbo y aspecto

Nueve decisiones. Ninguna agrega una dependencia: las cuatro capacidades nuevas —trazo,
trayecto delegado, orientación en línea recta y paleta— salen de lo que ya está instalado o
de la plataforma.

Una advertencia que vale para toda esta feature: **es la primera que nace de uso real**, así
que varias decisiones son de retirar cosas, no de agregarlas. D4 borra código y D8 no escribe
ninguna consulta nueva. Si el balance de líneas da negativo, está bien.

---

## D1 — El trazo del recorrido

**Decision**: una sola fuente GeoJSON con un `LineString` por recorrido, y una única capa de
línea. Cada feature lleva una propiedad booleana que dice si es el recorrido destacado —el
en curso, o el más reciente si no hay ninguno en curso—, y la capa cambia color y ancho según
esa propiedad con una expresión.

**Rationale**: es exactamente el mecanismo que la 002 ya usa para los marcadores —fuente,
capa, expresión sobre una propiedad— así que no entra ningún concepto nuevo. Distinguir el
destacado cuesta una expresión, no una segunda capa con su propia configuración que después
haya que mantener sincronizada.

El `LineString` por recorrido, y no uno solo con todos los puntos, es lo que impide que el
final de una salida se una con el principio de la siguiente: dos salidas de días distintos
quedarían cosidas por una recta que cruza la ciudad.

**Alternatives considered**:

- **Una capa por recorrido.** Rechazado: N capas y N fuentes que crear y destruir al cambiar
  los datos, para lograr lo que una expresión resuelve.
- **La API de anotaciones de MapLibre (`Polyline`).** Rechazado: es la API vieja, tiene costo
  por objeto y no comparte nada con la forma en que ya se dibujan los marcadores.

---

## D2 — Delegar el trayecto en la app de mapas

**Decision**: `Intent(ACTION_VIEW)` con una URI `geo:0,0?q=<lat>,<lng>(<etiqueta>)`, donde la
etiqueta es el número de la patente. Si no hay ninguna app que la reciba, se captura
`ActivityNotFoundException` y se avisa.

**Rationale**: `geo:` es el esquema de la plataforma, no de un producto: cualquier app de
mapas instalada lo declara. La forma `geo:0,0?q=lat,lng(etiqueta)` es la documentada para
"mostrame este punto con este nombre", que es exactamente lo que hace falta — la app entrega
el destino y el ruteo lo hace quien sabe hacerlo (FR-001).

Capturar la excepción en lugar de consultar `resolveActivity` no es pereza: desde Android 11
`resolveActivity` devuelve null salvo que el manifiesto declare un bloque `<queries>` para ese
esquema. Son dos mecanismos para el mismo resultado, y uno de ellos necesita manifiesto.

**Alternatives considered**:

- **`google.navigation:q=lat,lng`.** Rechazado: arranca la navegación paso a paso directamente,
  que suena mejor, pero ata la app a Google Maps. Con `geo:` el jugador usa lo que tenga.
- **Consultar `resolveActivity` con `<queries>` en el manifiesto.** Rechazado por lo de arriba:
  más configuración para saber de antemano lo que el `try` responde igual.
- **Calcular la ruta adentro de la app.** Rechazado en la spec, con la pregunta hecha al
  usuario: exige un servicio de ruteo, o sea red obligatoria y una dependencia nueva, para
  hacer peor lo que la app de mapas ya hace.

---

## D3 — Distancia y rumbo en línea recta

**Decision**: funciones puras en `domain/` — distancia haversine entre dos posiciones, rumbo
inicial, y rumbo a punto cardinal (8 sectores: N, NE, E, SE, S, SO, O, NO). Sin Android
adentro, testeables en JVM.

**Rationale**: la plataforma tiene `android.location.Location.distanceBetween`, y usarla
parecía el escalón más barato. No lo es: es un método estático de Android que en una prueba
unitaria de JVM tira `RuntimeException: not mocked`, así que la única forma de probarlo sería
agregar Robolectric — una dependencia nueva para no escribir quince líneas.

Y la constitución pide prueba ejecutable para toda lógica no trivial. Esto la tiene: la
conversión de rumbo a cardinal es exactamente el tipo de función que se rompe en los bordes
—337.5°, 0°, 360°— sin que nadie lo note mirando el teléfono. **Es la única lógica pura que
esta feature agrega, y por lo tanto la única prueba nueva que le corresponde.**

**Alternatives considered**:

- **`Location.distanceBetween`.** Rechazado por lo de arriba: gratis de escribir, imposible de
  probar sin sumar Robolectric.
- **`SphericalUtil` de maps-android-utils.** Rechazado: una dependencia entera para dos
  funciones que entran en una pantalla.
- **Distancia euclídea sobre grados.** Rechazado: a la latitud de Buenos Aires un grado de
  longitud mide un 82% de uno de latitud, así que el error es sistemático y direccional. Con
  haversine el costo es una línea más.

---

## D4 — Retirar la grilla de cobertura

**Decision**: borrar la grilla y todo lo que existe solo para ella.

Se va: `Celda`, `Limites`, `Cobertura.celdaDe`, `Cobertura.celdasExploradas`,
`Cobertura.limitesDe`, `Cobertura.PASO_GRADOS`, la fuente y la capa de cobertura en el mapa,
el color `COBERTURA` de la paleta, el campo `celdas` de `ResumenDeSalida`, y las pruebas de
`CoberturaTest` que cubren la grilla.

Se queda: `Cobertura.tieneElActual` y `Cobertura.consecutivosCubiertos`, con sus pruebas. Las
usa el indicador del juego y no tienen nada que ver con la grilla.

**Rationale**: el FR-021 dice que la grilla deja de dibujarse. Dejar el cálculo vivo "por si
acaso" es exactamente lo que el Principio IV descarta, y encima el código muerto se lee como
código vivo seis meses después. Lo que se necesite volver está en el historial de git.

**Alternatives considered**:

- **Dejar de dibujarla pero conservar el cálculo.** Rechazado: mantener una función que nadie
  llama cuesta más que recuperarla del historial si algún día vuelve a hacer falta.
- **Dejarla apagable desde Ajustes.** Rechazado en la spec: un ajuste que nadie pidió, y la
  002 ya rechazó el mismo patrón para el selector de tema.

---

## D5 — La segunda superficie de mapa

**Decision**: generalizar el mapa que ya existe para que reciba qué dibujar, y usarlo también
dentro de la fila expandida de la pantalla de Salidas, con alto fijo y encuadrado sobre los
puntos de esa salida.

**Rationale**: el Principio IV exige dos usos reales antes de que exista una abstracción
compartida. Acá los hay, y son los dos concretos y presentes, así que este es el caso que el
principio admite. Reusar la fila expandida que la pantalla **ya tiene** evita además un
destino de navegación nuevo: no hay pantalla nueva, hay una fila que muestra un mapa.

El manejo de ciclo de vida del `MapView` ya está resuelto en el componente y se aplica solo a
la segunda instancia, así que expandir y colapsar no filtra nada.

**Alternatives considered**:

- **Una pantalla nueva para una salida.** Rechazado: un quinto destino de navegación, con su
  ida y su vuelta, para mostrar lo que entra en la fila que ya se expande.
- **Un mapa único que filtre por salida.** Rechazado: obligaría a llevar al jugador a la
  pantalla principal y volver, perdiendo la lista de salidas de vista.

---

## D6 — De dónde sale el violeta

**Decision**: definir el esquema de color **completo**, claro y oscuro, con superficies
neutras. Un solo acento, reservado para la acción principal, y los colores con significado del
mapa aparte, como ya están.

**Rationale**: este es el diagnóstico, y explica por qué el violeta aparecía en lugares que
nadie pintó. `Tema.kt` define hoy solo `primary` y `secondary`. **Todos los demás roles
—`secondaryContainer`, `surfaceVariant`, `primaryContainer`, `surfaceContainer`— se quedaron
en el esquema base de Material 3, que es violeta.** Los botones tonales usan
`secondaryContainer` y las tarjetas usan superficies: por eso salen lilas sin que ningún
color violeta esté escrito en el código de la app.

Es la misma clase de error que el modo oscuro de la 002: no era que alguien hubiera elegido
mal un color, era que nadie lo había elegido y el default no era neutro.

**Alternatives considered**:

- **Pisar solo los roles que se ven violetas.** Rechazado: es jugar al topo. El próximo
  componente que se agregue va a tomar otro rol sin definir y el violeta vuelve.
- **Color dinámico del sistema (Material You).** Rechazado: la paleta pasaría a depender del
  fondo de pantalla del jugador, que es lo contrario de "estándar y neutro". La 002 ya lo
  rechazó para los colores del mapa por la misma razón.

---

## D7 — La barra de navegación blanca sobre blanco

**Decision**: llamar a `enableEdgeToEdge()` en la creación de la actividad.

**Rationale**: es el bug más chico de la feature y el de causa más precisa. Desde targetSdk 35
Android dibuja la barra de navegación transparente sí o sí, y el tinte de sus iconos lo
decide `isAppearanceLightNavigationBars`. Sin `enableEdgeToEdge()` nadie lo fija, queda en el
valor que trae el tema de plataforma, y en modo claro resultan iconos claros sobre el fondo
claro de la app: invisibles. `enableEdgeToEdge()` instala el comportamiento que el resto de
las apps tiene —el que el usuario notó que faltaba— y lo ajusta solo al cambiar de modo.

`androidx.activity` 1.12.4 ya está en el proyecto y la trae. Cero dependencias.

**Alternatives considered**:

- **Declarar `windowLightNavigationBar` en los dos temas XML.** Funciona, pero son dos
  archivos y dos valores que hay que mantener en espejo, contra una línea que se ajusta sola.
- **Pintar una franja opaca detrás de la barra.** Rechazado: devuelve el borde a la app, que
  es justo lo que Android 15 sacó, y encima come pantalla.

---

## D8 — Buscar, sin consultas nuevas

**Decision**: los recientes y el filtrado incremental se resuelven en memoria sobre la
consulta que ya existe. Ninguna consulta nueva en la base.

**Rationale**: `todosUnaVez()` ya devuelve todos los registros ordenados por fecha
descendente. Los recientes son los primeros N de esa lista; el filtrado por prefijo es
comparar el número con ceros a la izquierda contra lo tipeado. La escala lo permite con
holgura: es un jugador, un teléfono, y en un año de juego intenso son cientos de registros,
no millones. Escribir `@Query` con `LIMIT` y `LIKE` sería más código para el mismo resultado.

La sugerencia del número actual sale de `EstadoDelJuego`, que la pantalla principal ya lee.

**Alternatives considered**:

- **Consultas `recientes(limite)` y `porPrefijo(texto)` en el DAO.** Rechazado por ahora: dos
  consultas más para una lista que entra entera en memoria. Si algún día la base crece a un
  volumen que lo justifique, se agregan entonces — y el cambio queda contenido en el DAO.

---

## D9 — Que el trazo no ponga lento el mapa

**Decision**: dibujar todos los recorridos sin diezmar ni simplificar los puntos, y medirlo
en el dispositivo antes de optimizar nada.

**Rationale**: la cuenta da holgada. El servicio de recorrido guarda una posición cada 30
segundos y cada 50 metros de desplazamiento, así que una hora de caminata deja del orden de
cien puntos. Cincuenta salidas guardadas son unos pocos miles de puntos en total: para un
motor de mapas que dibuja ciudades enteras, no es nada.

Optimizar antes de medir sería agregar complejidad contra un problema que la aritmética dice
que no existe. El FR-010 y el SC-005 lo vigilan; si aparece, se ataca entonces.

**Alternatives considered**:

- **Simplificar la línea (Douglas-Peucker) antes de dibujar.** Rechazado por ahora: algoritmo
  propio, con su tolerancia que calibrar y su prueba que escribir, contra un problema no
  observado.

---

## Nota sobre pruebas

Esta feature agrega **una** prueba, y es deliberado: la de D3.

La distancia, el rumbo y la conversión a punto cardinal son lógica pura con bordes que se
rompen en silencio —el cruce por 0°, el redondeo entre sectores— y la constitución las
obliga. Todo lo demás que la feature toca es tema, capas de mapa, un intent y disposición de
pantalla: nada de eso deja una función que se pueda romper sin que se vea.

`CoberturaTest` **pierde** las cinco pruebas de la grilla junto con la grilla (D4) —las que
cubren `celdasExploradas`, `celdaDe` y `limitesDe`— y conserva las ocho del indicador. El
saldo de la feature es un archivo de prueba nuevo contra cinco pruebas borradas.
