# Phase 0 Research: Pulido de interfaz

**Fecha**: 2026-08-29
**Spec**: [spec.md](./spec.md) · **Constitución**: 1.1.0

Ocho decisiones. Las dos primeras son diagnósticos: antes de decidir cómo arreglar el modo
oscuro hubo que averiguar por qué está roto, y antes de planificar el mapa hubo que mirar
qué quedó construido de la especificación 001.

---

## D1 — Por qué el modo oscuro se ve roto

**Diagnóstico**: son tres causas encadenadas, todas verificadas leyendo el código.

1. `app/src/main/res/values/themes.xml` declara el tema de la ventana como
   `android:Theme.Material.NoActionBar`. En la plataforma Android, `Theme.Material` es la
   variante **oscura**; la clara es `Theme.Material.Light`. O sea que la ventana de la app
   tiene fondo oscuro **siempre**, sin importar qué modo tenga configurado el teléfono.
2. `MainActivity` llama a `MaterialTheme { ... }` sin pasarle `colorScheme`. Compose
   entonces usa su esquema **claro** por defecto, también siempre. El texto sale casi
   negro.
3. `PantallaBusqueda`, `PantallaAjustes` y `PantallaRecorridos` son un `Column` pelado, sin
   `Surface` ni fondo propio. No pintan nada, así que se ve la ventana oscura de la causa 1
   por detrás del texto casi negro de la causa 2.

Eso da texto negro sobre fondo negro. No es un problema de "modo oscuro": la app está en
modo oscuro y claro **al mismo tiempo**, en capas distintas.

`PantallaPrincipal` se salva por accidente: usa `Scaffold`, que sí pinta un fondo de
superficie, y además tiene el mapa detrás.

**Decision**: un tema de Compose propio con los dos esquemas, que siga el del sistema, más
un `Surface` que pinte el fondo en el punto donde hoy no lo pinta nadie, más el tema XML
corregido a una variante DayNight para que la ventana acompañe.

**Rationale**: arreglar solo una de las tres causas deja el problema. Con el tema XML
corregido pero Compose fijo en claro, el modo oscuro seguiría dando texto negro sobre
fondo oscuro. Las tres van juntas o no va ninguna.

**Alternatives considered**:

- **Forzar modo claro siempre**. Más simple, y elimina la mitad del trabajo. Rechazado
  porque el usuario tiene el teléfono en oscuro y la app le quedaría gritando en blanco de
  noche, que es cuando se juega a esto.
- **Colores fijos en cada pantalla**, sin tema. Rechazado: es lo que produce este tipo de
  bug, repartido en más lugares.
- **Dynamic color de Material You**. Toma la paleta del fondo de pantalla del usuario.
  Tentador y gratis en Android 12+, pero acopla la legibilidad de la app a una imagen que
  el jugador puede cambiar. Rechazado para las señales que tienen significado —pendiente,
  ya toca, compartida—, que necesitan colores estables.

---

## D2 — Contra qué versión se formó cada reporte

**Primera versión de este diagnóstico, y era falsa.** Se asumió que el usuario reportaba
sobre una versión vieja sin instalar. Verificado en el dispositivo el 2026-08-29, es al
revés: el APK instalado pesa exactamente lo mismo que el último build, y su
`firstInstallTime` es igual a su `lastUpdateTime`, así que fue una instalación limpia. Está
al día. Como efecto colateral de haber desinstalado, la base se recreó vacía.

Lo que sí importa, y es más fino: **cada reporte se formó sobre una versión distinta**,
porque los arreglos fueron saliendo durante la misma sesión.

| Reporte | Versión sobre la que se formó | Qué significa |
|---|---|---|
| "El botón Volver está tapado por la barra de notificaciones" | Una que **ya tenía** `safeDrawingPadding()` en las tres pantallas | Los insets **no son la causa**. Ver el re-diagnóstico abajo |
| "Que acerque el mapa a mi ubicación" | Una **anterior a T084** | T084 activó el punto de posición y el seguimiento con zoom de calle. Puede estar resuelto; hay que mirarlo |
| "Se ve todo roto en modo oscuro" | Cualquiera | Sin arreglar. Es D1 |
| "El icono de la cámara es horrible" | Cualquiera | Sin arreglar. Es un emoji `📷` en `BotonesAccion.kt` |

### Re-diagnóstico del botón "Volver"

Si el inset ya estaba aplicado y el usuario igual lo describe como "tan arriba que ni se ve
y casi tapado", entonces el problema no es que quede **debajo** de la barra de estado: es
que queda **pegado** a ella. Las tres pantallas son un `Column` que arranca con una fila de
título y un botón de texto, sin nada que le dé aire ni jerarquía. Sobre un teléfono alto
eso se lee apretado y queda lejos del pulgar.

**Decision**: las pantallas secundarias necesitan una barra superior de verdad, con la
altura, el aire y el control de navegación que Material 3 ya define. No es un arreglo de
insets: es la estructura que nunca tuvieron.

**Rationale**: encaja con lo que el usuario pidió en la misma frase —"más cómoda y
moderna"— y explica por qué el arreglo anterior no le cambió la percepción. Un control
correctamente posicionado pero sin jerarquía sigue siendo incómodo.

**Consecuencia práctica**: antes de dimensionar la User Story 2 hay que mirar el mapa en el
dispositivo, porque T084 pudo haber resuelto la mayor parte. Y la base está vacía: para
validar cualquier cosa de la User Story 3 hay que volver a cargar registros de prueba.

**Lección que vale registrar**: dar por sentado qué versión tiene el usuario, en vez de
preguntarle o mirarlo, produjo un diagnóstico invertido que llegó hasta el plan.

---

## D3 — Iconos propios en lugar de material-icons-extended

**Decision**: los iconos que falten se dibujan como vector drawables en
`app/src/main/res/drawable/`. No se agrega `material-icons-extended`.

**Rationale**: el Principio IV obliga a justificar toda dependencia nueva contra lo ya
instalado. `material-icons-core` viene con Material 3 y ya cubre lo genérico. Lo único que
falta de verdad es una cámara y un par de controles del mapa. Traer una biblioteca de miles
de iconos —del orden de 10 MB antes de reducir— para usar tres es exactamente lo que el
principio prohíbe.

Un vector drawable es un archivo XML de veinte líneas. Tres archivos contra una dependencia.

**Alternatives considered**:

- **`material-icons-extended`**. Cómodo, un import y listo. Rechazado por tamaño y por el
  Principio IV. Si algún día hicieran falta veinte iconos, la cuenta cambia y se
  reconsidera.
- **Seguir con emojis**. Rechazado por FR-022: un emoji se dibuja distinto en cada
  teléfono, no se puede colorear con el tema y se ve como un parche.

---

## D4 — Marcadores con el número adentro

**Decision**: una `SymbolLayer` de solo texto encima de la `CircleLayer` que ya existe,
las dos alimentadas por la misma fuente GeoJSON, con el número como propiedad de cada
feature.

**Rationale**: la capa de círculos ya está construida y ya resuelve los tres colores de
FR-035. Ponerle el número encima es agregar una capa y una propiedad, no rehacer nada. El
texto lo dibuja MapLibre desde la fuente, así que no hay que generar bitmaps ni mantener un
mapa de imágenes por número.

Tres dígitos entran cómodos en un círculo de radio suficiente, que es justamente lo que el
usuario señaló: nunca van a ser más de 3.

**Alternatives considered**:

- **Un icono de globo con el número dibujado encima**, generando un bitmap por número.
  Rechazado: hasta 1000 bitmaps a mantener y registrar en el estilo, para el mismo
  resultado visual.
- **Marcadores de Android encima del mapa**, como vistas. Rechazado: no escalan, no
  siguen el mapa al hacer zoom, y MapLibre desaconseja mezclarlos con capas.

---

## D5 — Marcadores superpuestos: agrupación nativa

**Decision**: activar la agrupación por cercanía que trae `GeoJsonSource` de MapLibre.
Cuando varios registros caen muy juntos se ven como un grupo con la cuenta, y al acercarse
se abren en marcadores individuales.

**Rationale**: FR-016 pide distinguir marcadores amontonados. MapLibre lo resuelve en la
fuente, con opciones de configuración, sin código propio. Escribir a mano una separación de
marcadores sería reimplementar algo que la dependencia ya instalada hace mejor — el rung
que el Principio IV manda revisar antes de escribir nada.

**Alternatives considered**:

- **Correr los marcadores unos píxeles para que no se pisen**. Rechazado: mueve la
  posición de la evidencia en la pantalla, que es justo lo que no hay que hacer.
- **Esconder los que colisionan**, que es el comportamiento por defecto de las etiquetas.
  Rechazado: un registro que desaparece del mapa es peor que uno amontonado.

---

## D6 — Tocar un marcador

**Decision**: un listener de toque sobre el mapa que consulta qué features hay dibujadas
en ese punto, filtrando por la capa de patentes, y abre la ficha del registro cuyo id
venga en la feature.

**Rationale**: es el mecanismo que MapLibre expone para esto. Consultar lo dibujado
—en lugar de calcular a mano qué marcador está más cerca del dedo— respeta el zoom, la
rotación y la agrupación de D5 sin lógica propia.

El id del registro viaja como propiedad de la feature, igual que el número de D4 y la clase
de color que ya existe.

---

## D7 — La ficha del registro

**Decision**: una hoja que sube desde abajo (`ModalBottomSheet` de Material 3) con la
información del registro y sus acciones.

**Rationale**: Material 3 ya está instalado y la trae. Es el patrón que usan las apps de
mapas para exactamente esto: mostrar el detalle de algo tocado en el mapa sin abandonar el
mapa. Una pantalla completa obligaría a volver, y perdería el contexto de dónde estaba el
marcador.

**Alternatives considered**:

- **Una pantalla nueva** en la navegación a mano que ya existe. Rechazado: saca al jugador
  del mapa y agrega un destino con argumento, que es lo que la navegación manual empieza a
  no aguantar.
- **Un diálogo**. Rechazado: tapa el mapa entero y no deja ver dónde estaba el marcador.

---

## D8 — Corregir el número sin tocar la evidencia

**Decision**: una operación de escritura nueva y acotada en el DAO, que actualiza
únicamente el número, el texto de la patente y el formato detectado. Ubicación, precisión y
timestamp quedan fuera de la sentencia.

**Rationale**: es el Principio II hecho esquema, igual que las dos operaciones que ya
existen. `adjuntarFoto` toca solo la foto, `marcarCompartida` toca solo el estado, y esta
toca solo el número. La prueba de inmutabilidad que ya corre lee el fuente del DAO y falla
si alguna sentencia escribe sobre un campo de evidencia, así que esta operación queda
cubierta por una prueba que ya existe sin escribir una nueva.

El número es lo que el jugador **leyó** en la calle. La ubicación y la hora son lo que el
teléfono **midió**. La constitución protege lo segundo; lo primero es dato de entrada y
siempre pudo estar mal tipeado.

**Consecuencia**: corregir el número cambia qué registros ya tocan, así que dispara la
misma reconciliación de avisos que disparan guardar y compartir (FR-015).

**Alternatives considered**:

- **Borrar y volver a crear con la misma ubicación**. Rechazado, y es importante por qué:
  crear un registro con una ubicación que no se acaba de medir es precisamente la carga
  retroactiva de metadata que el Principio II prohíbe. Sería violar el principio creyendo
  respetarlo.

---

## D9 — Seguir al jugador y volver a él

**Decision**: el mapa arranca en modo seguimiento; el primer gesto de arrastre del jugador
lo apaga; un control flotante lo vuelve a encender y recentra.

**Rationale**: es el comportamiento de cualquier app de mapas, y es lo que FR-006 y FR-007
describen. Sin apagar el seguimiento al arrastrar, el mapa pelea contra el dedo: el jugador
arrastra, llega la siguiente lectura de ubicación y el mapa vuelve solo. Esa pelea es el
modo en que este comportamiento se hace mal.

**Alternatives considered**:

- **Centrar una sola vez al abrir y nunca más**. Más simple, pero incumple FR-006: el
  jugador camina y el mapa se queda donde estaba.
- **Seguir siempre, sin apagar**. Rechazado por la pelea contra el dedo descrita arriba.

---

## Nota sobre pruebas

Nada de esta especificación agrega lógica pura nueva. Es interfaz, tema, capas de mapa y
una sentencia de escritura acotada.

El flujo de desarrollo de la constitución pide una prueba ejecutable por cada lógica no
trivial. Acá el único candidato es la corrección del número, y **ya está cubierta**: la
prueba de inmutabilidad lee el fuente del DAO y falla si aparece una escritura sobre un
campo de evidencia. Agregar pruebas de interfaz sería exactamente lo que el Principio IV
prohibió al decidir no tener suite de UI en la especificación 001.
