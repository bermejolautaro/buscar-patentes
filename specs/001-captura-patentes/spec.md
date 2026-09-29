# Feature Specification: Captura de patentes

**Feature Branch**: `001-captura-patentes`

**Created**: 2026-08-28

**Status**: Cerrada el 2026-08-28

85 de 89 tareas completas. Las cuatro restantes no son trabajo de código: son mediciones de
campo que quedaron en el Backlog de [tasks.md](./tasks.md).

**Input**: User description: "lee BRIEF.md"

## Clarifications

### Session 2026-08-28

- Q: ¿Qué hace exactamente el botón "Empezar recorrido" — graba tu trayecto con GPS de forma continua, o solo marca un inicio y un fin para agrupar las patentes que cargues en el medio? → A: Graba el trayecto completo y además avisa cuando el jugador pasa cerca de una patente guardada que ya toca por número.
- Q: Cuando abrís la app, ¿el teclado numérico ya está arriba sobre el mapa listo para tipear, o el mapa se ve limpio y el teclado aparece recién cuando tocás un botón? → A: La app abre con el mapa de fondo y el teclado numérico ya arriba, campo enfocado. La carga rápida se mantiene en 3 interacciones.
- Q: ¿El aviso de proximidad funciona solo mientras tenés un recorrido activo, o la app te avisa siempre que pasés cerca de una patente que ya toca, aunque no hayas tocado "Empezar recorrido"? → A: Siempre para el aviso, con seguimiento liviano delegado al sistema operativo. El recorrido es independiente y agrega encima el registro de por dónde se pasó.
- Q: Sin conexión a internet, ¿qué tenés que poder ver en el mapa — solo tu posición y tus patentes sobre un fondo vacío, o también las calles dibujadas? → A: El fondo de mapa de las zonas por las que el jugador ya pasó se guarda solo, y offline se ven las calles de esas zonas.
- Q: Los recorridos grabados, ¿se guardan para siempre, o la app los va borrando sola después de un tiempo? → A: Se guardan para siempre, sin limpieza automática, pero el trayecto se muestrea espaciado (30 segundos o más). El recorrido solo tiene que responder qué calles ya se exploraron, de forma acumulada entre todos los recorridos, no dibujar el camino exacto. La precisión máxima se reserva para la ubicación de las capturas.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Carga rápida en la calle (Priority: P1)

El jugador ve una patente desde la vereda o desde el auto. Abre la app, que arranca
directo en la pantalla principal: un mapa de fondo con su posición y sus patentes
guardadas, un único campo para el número con el teclado numérico ya arriba, y los
botones de acción abajo. Tipea el número y toca el botón `+`. La app resuelve sola la
ubicación y el momento exacto, guarda el registro y vuelve a estar lista. No hay más
pasos: ni elegir modo antes de tipear, ni completar campos, ni esperar a que el mapa
termine de cargar.

**Why this priority**: es el producto. Sin esto no hay app. Si la captura tarda, el
auto ya se fue y el registro se perdió. Todo lo demás (foto, listado, compartir) se
construye sobre registros que este flujo creó.

**Independent Test**: se prueba completo abriendo la app en la calle, cargando un
número y verificando que el registro quedó guardado con coordenadas, precisión y
timestamp propios, sin tocar ningún otro campo. Ya entrega valor solo: es un cuaderno
de patentes con evidencia automática.

**Acceptance Scenarios**:

1. **Given** la app cerrada, **When** el jugador la abre, **Then** aparece la pantalla
   principal con el mapa de fondo, el campo de número enfocado y el teclado numérico
   visible, sin pantalla intermedia ni selección de modo.
2. **Given** el mapa todavía cargando, **When** el jugador tipea un número y toca `+`,
   **Then** el registro se guarda igual: el estado del mapa no bloquea la captura.
3. **Given** la app abierta y el teléfono con ubicación activa, **When** el jugador
   tipea un número de 3 dígitos y toca el botón `+`, **Then** el registro queda
   guardado sin foto, con coordenadas, precisión reportada y timestamp del momento en
   que tocó el botón.
4. **Given** el teléfono sin conexión a internet, **When** el jugador completa una
   carga rápida, **Then** el registro se guarda igual y queda disponible al reabrir la
   app.
5. **Given** una lectura de ubicación degradada o vencida, **When** el jugador
   confirma la carga, **Then** el registro se guarda con la precisión real reportada y
   queda marcado visiblemente como impreciso, sin inventar ni redondear coordenadas.
6. **Given** que el almacenamiento falla al guardar, **When** el jugador confirma la
   carga, **Then** la app muestra el error de forma explícita y no reporta éxito.
7. **Given** que el jugador recorrió una zona con conexión, **When** vuelve a esa zona
   sin conexión, **Then** el mapa muestra las calles de esa zona, guardadas solas
   mientras pasaba, sin que él haya pedido ninguna descarga.
8. **Given** una zona por la que nunca pasó y sin conexión, **When** el mapa llega a esa
   zona, **Then** la app distingue visualmente que no tiene fondo guardado para ahí, en
   lugar de mostrar un vacío ambiguo.
9. **Given** el fondo de mapa guardado en su límite de espacio, **When** el jugador
   recorre una zona nueva con conexión, **Then** la app guarda la zona nueva liberando
   primero las menos visitadas, sin fallar y sin pasarse del límite.

---

### User Story 2 - Adjuntar la foto de la patente (Priority: P2)

El jugador quiere que el registro tenga la foto de la patente, que es lo que después
manda al grupo. En la misma pantalla de captura, en vez de tocar `+`, toca el botón de
cámara: se abre la cámara, saca la foto y el registro queda guardado con la foto
adentro. También puede agregarle una foto más tarde a un registro que guardó rápido.

Los dos botones no son modos que haya que elegir antes de tipear: son dos formas de
confirmar el mismo número. El jugador tipea primero y recién ahí decide si cierra con
`+` o con la cámara.

**Why this priority**: la foto es lo que se comparte al grupo, pero un registro sin
foto ya sirve como anotación con evidencia de lugar y hora. La app tiene valor sin
esto; no lo tiene sin la User Story 1.

**Independent Test**: se prueba tipeando un número, tocando el botón de cámara,
sacando la foto, y verificando que el registro quedó con la foto asociada y con la
ubicación y el timestamp de la captura, y que sigue accesible al reabrir la app.

**Acceptance Scenarios**:

1. **Given** un número tipeado en la pantalla de captura, **When** el jugador toca el
   botón de cámara y saca la foto, **Then** el registro queda guardado con la foto
   asociada, más las coordenadas, la precisión y el timestamp de la captura.
2. **Given** un registro guardado sin foto, **When** el jugador le agrega una foto más
   tarde, **Then** la foto se asocia al registro pero la ubicación y el timestamp
   originales del registro no cambian.
3. **Given** un número tipeado y la cámara abierta, **When** el jugador cancela sin
   sacar la foto, **Then** vuelve a la pantalla de captura con el número todavía
   tipeado y sin registro creado.

---

### User Story 3 - Encontrar y usar la patente que toca (Priority: P3)

La app sabe en qué número va el juego. El jugador lo lleva al día a mano: cuando el
grupo avanza, él avanza el contador. Con eso la app le responde sola la pregunta que
importa — "¿tengo el 313?" — y le muestra cuántos números seguidos tiene cubiertos
hacia adelante. Cuando le toca, encuentra el registro, lo manda al grupo y avanza el
contador.

**Why this priority**: es el momento en que la app paga. Pero requiere que ya haya
registros acumulados, así que llega después de P1 y P2.

**Independent Test**: se prueba fijando el número actual del juego, cargando varios
registros por encima y por debajo de ese número, y verificando que la app indica si el
número actual está cubierto, cuántos consecutivos hay adelantados, y que el registro
se puede compartir hacia otra aplicación del teléfono.

**Acceptance Scenarios**:

1. **Given** el número actual del juego fijado en 313 y un registro guardado para 313,
   **When** el jugador abre la app, **Then** la app le indica que ya tiene la patente
   del número actual, sin que tenga que buscarla.
2. **Given** el número actual en 313 y registros guardados para 313, 314 y 315,
   **When** el jugador mira el estado del juego, **Then** la app le indica que tiene 3
   números consecutivos cubiertos desde el actual.
3. **Given** un registro encontrado, **When** el jugador elige compartirlo, **Then**
   el sistema ofrece enviarlo a otra aplicación del teléfono y el registro queda
   marcado como compartido.
4. **Given** el número actual en 313, **When** el jugador avanza el contador,
   **Then** el número actual pasa a 314 y la app recalcula qué tiene cubierto.
5. **Given** varios registros guardados, **When** el jugador busca un número,
   **Then** ve los registros de ese número con su foto, lugar y fecha de captura.
6. **Given** que el jugador busca un número que no tiene, **When** no hay
   coincidencias, **Then** la app lo dice explícitamente en lugar de mostrar una lista
   vacía sin explicación.
7. **Given** varias zonas recorridas con su fondo de mapa ya guardado, **When** el
   jugador abre los ajustes, **Then** ve cuánto espacio ocupa ese fondo y puede
   borrarlo por completo.

---

### User Story 4 - Aviso de proximidad (Priority: P4)

El jugador guardó hace semanas la patente 450 con el botón `+`, sin foto: solo anotó
dónde vive ese auto. El juego llega al 450. Un día cualquiera, yendo al trabajo, pasa
a dos cuadras de ese lugar y el teléfono le avisa. Va, encuentra el auto, lo fotografía
en el momento y lo manda al grupo.

Este es el cierre del ciclo, y funciona sin que el jugador encienda nada: la carga
rápida sirve para anotar *dónde vive* una patente futura, y el aviso es lo que después
lo lleva de vuelta a ese lugar cuando el número llega.

**Why this priority**: sin registros acumulados no hay nada que avisar, y sin estado
del juego no se sabe qué toca. Depende de P1 y P3. Es lo que convierte el archivo de
patentes en una herramienta que trabaja sola.

**Independent Test**: se prueba guardando un registro en una ubicación conocida,
poniendo el número del juego en ese número, cerrando la app por completo y
acercándose físicamente a esa ubicación; el aviso debe llegar con la app cerrada y sin
ningún recorrido activo.

**Acceptance Scenarios**:

1. **Given** un registro pendiente guardado en una ubicación, el número del juego igual
   al de ese registro y la app cerrada, **When** el jugador pasa cerca de esa
   ubicación, **Then** la app le avisa indicando qué patente es y dónde está.
2. **Given** un registro cuyo número todavía no toca, **When** el jugador pasa cerca
   de su ubicación, **Then** la app no avisa.
3. **Given** un registro ya compartido, **When** el jugador pasa cerca de su ubicación,
   **Then** la app no avisa.
4. **Given** el permiso de ubicación en segundo plano denegado, **When** el jugador
   activa los avisos, **Then** la app explica que lo necesita y el resto de la app
   sigue funcionando normalmente.
5. **Given** los avisos apagados por el jugador, **When** pasa cerca de una patente que
   toca, **Then** la app no avisa y el resto de la app sigue funcionando.

---

### User Story 5 - Recorrido grabado (Priority: P5)

El jugador toca "Empezar recorrido" antes de salir a caminar o manejar buscando
patentes. La app va anotando por dónde pasa, con muestreo espaciado, incluso con la
pantalla apagada. Lo que le importa después no es el camino exacto sino el mapa
acumulado de **qué calles ya exploró**, para elegir por dónde ir la próxima vez y no
repetir siempre las mismas cuadras.

La precisión del recorrido es deliberadamente baja: alcanza con saber qué calles se
recorrieron. La precisión máxima se reserva para la ubicación de las capturas, que es
lo que hay que poder volver a encontrar.

**Why this priority**: es un plus. El objetivo de encontrar las patentes de vuelta ya
lo resuelve la User Story 4 sin grabar nada. Esto sirve para planificar dónde buscar.
Va última.

**Independent Test**: se prueba iniciando un recorrido, caminando un trayecto conocido
con la pantalla apagada, cargando una patente en el medio, y verificando después que
esas calles quedaron marcadas como exploradas y que la patente quedó asociada al
recorrido.

**Acceptance Scenarios**:

1. **Given** el permiso de ubicación en segundo plano otorgado, **When** el jugador
   toca "Empezar recorrido", **Then** la app empieza a registrar el trayecto y muestra
   de forma visible que hay un recorrido en curso.
2. **Given** un recorrido en curso, **When** el jugador minimiza la app o apaga la
   pantalla, **Then** el trayecto se sigue registrando.
3. **Given** un recorrido en curso, **When** el jugador hace una captura, **Then** el
   registro queda asociado a ese recorrido.
4. **Given** un recorrido terminado, **When** el jugador lo abre, **Then** ve las
   calles que recorrió en esa salida junto con las patentes que capturó en ella.
5. **Given** varios recorridos terminados en distintas salidas, **When** el jugador
   mira el mapa, **Then** ve de forma acumulada qué calles ya exploró y cuáles no.
6. **Given** un recorrido en curso interrumpido por el sistema operativo, **When** el
   jugador vuelve a abrir la app, **Then** lo registrado hasta ese momento sigue
   estando.

---

### Edge Cases

- ¿Qué pasa cuando el permiso de ubicación está denegado o revocado? La captura no
  puede cumplir el Principio II de la constitución, así que la app debe pedirlo antes
  de permitir guardar, y explicar por qué lo necesita.
- ¿Qué pasa cuando el sistema no devuelve ninguna lectura de ubicación dentro del
  presupuesto de tiempo de la carga rápida?
- ¿Qué pasa si el jugador carga un número que ya tiene guardado? ¿Se permiten
  duplicados del mismo número?
- ¿Qué pasa si el jugador tipea algo que no es un número válido de 3 dígitos?
- ¿Qué pasa si el almacenamiento del teléfono está lleno al momento de guardar la foto?
- ¿Qué pasa con el reloj del teléfono si está mal configurado, o si el usuario lo
  cambia manualmente después de una captura?
- ¿Qué pasa si el jugador avanza el contador del juego de más por error? ¿Puede
  retrocederlo?
- ¿Qué pasa si el jugador captura un número menor al número actual del juego, es decir
  uno que el grupo ya pasó? ¿Se guarda igual, se avisa, se rechaza?
- ¿Qué pasa si el jugador toca el botón de cámara sin haber tipeado ningún número?
- ¿Qué pasa si el jugador deja un recorrido abierto por días y se olvida de cerrarlo?
- ¿Qué pasa si el sistema operativo mata la app o corta el seguimiento en segundo plano
  para ahorrar batería en medio de un recorrido? ¿Se pierde el trayecto parcial?
- ¿Qué pasa si el jugador pasa cerca de la misma patente cinco veces en un recorrido?
  ¿Avisa cinco veces?
- ¿Qué pasa si el aviso de proximidad llega mientras el jugador está manejando?
- ¿Qué pasa si dos registros pendientes que ya tocan están cerca al mismo tiempo?
- ¿Qué pasa con el trayecto de un recorrido cuando la señal de ubicación se pierde por
  varias cuadras?
- ¿Qué pasa cuando el fondo de mapa guardado llega a su límite de espacio en medio de
  un recorrido?
- ¿Qué pasa si el jugador viaja a otra ciudad o se muda? ¿El fondo guardado se llena de
  zonas a las que no va a volver?
- ¿Qué pasa si el jugador pasa por una zona nueva sin conexión? Esa zona no se puede
  guardar en ese momento: ¿se guarda después, cuando vuelve a tener red?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir guardar un registro ingresando únicamente el
  número de patente, en 3 interacciones o menos desde la app cerrada.
- **FR-002**: El sistema MUST resolver la ubicación por su cuenta al momento de
  confirmar la captura, sin intervención del usuario.
- **FR-003**: El sistema MUST guardar, junto a las coordenadas, la precisión reportada
  por el proveedor de ubicación del sistema operativo, expresada en metros.
- **FR-004**: El sistema MUST guardar el timestamp del momento de la captura.
- **FR-005**: El sistema MUST NOT permitir editar la ubicación, la precisión ni el
  timestamp de un registro después de creado.
- **FR-006**: El sistema MUST NOT permitir crear un registro con ubicación o timestamp
  ingresados a mano o cargados retroactivamente.
- **FR-007**: El sistema MUST completar y persistir una captura sin conexión a
  internet.
- **FR-008**: El sistema MUST informar el fallo de forma explícita y visible cuando una
  captura no se pudo persistir, y MUST NOT reportar éxito en ese caso.
- **FR-009**: El sistema MUST guardar el registro aunque la lectura de ubicación sea
  imprecisa, marcándolo como impreciso y conservando la precisión real reportada.
- **FR-010**: El sistema MUST aceptar patentes en los dos formatos argentinos vigentes
  y extraer de ambos la parte numérica de 3 dígitos que usa el juego.
- **FR-011**: Los usuarios MUST poder buscar sus registros guardados por número de
  patente.
- **FR-012**: Los usuarios MUST poder compartir un registro hacia otra aplicación del
  teléfono.
- **FR-013**: El sistema MUST funcionar sin cuenta de usuario, sin login y sin
  servidor propio.
- **FR-014**: El sistema MUST solicitar el permiso de ubicación antes de habilitar la
  carga rápida, explicando para qué se usa.
- **FR-015**: El sistema MUST conservar los registros ya compartidos en lugar de
  borrarlos, y MUST permitir distinguir los usados de los pendientes.
- **FR-016**: La app MUST abrir directamente en la pantalla principal, con el mapa de
  fondo, el campo de número enfocado y el teclado numérico ya visible, sin pantalla
  intermedia ni selección de modo previa.
  > **Superado por el FR-017 de la especificación 002 (pulido de interfaz).** El teclado
  > ya no sube al abrir: en uso real tapaba el mapa antes de que el jugador decidiera si
  > iba a cargar algo, y el mapa es lo que se mira al llegar a una cuadra. Sigue vigente
  > todo lo demás de este requisito —sin pantalla intermedia ni selección de modo—; lo
  > único que cambia es que el foco y el teclado los pide el jugador con un toque. Ese
  > toque es el que llevó el presupuesto de la carga rápida de 3 interacciones a 4 en la
  > constitución 1.1.0.
- **FR-017**: La pantalla de captura MUST tener un único campo de entrada, y ese campo
  MUST aceptar solamente el número de patente.
- **FR-018**: La pantalla de captura MUST ofrecer dos botones de confirmación
  distinguibles, `+` para guardar sin foto y cámara para guardar con foto. Ambos MUST
  resolver ubicación y timestamp por su cuenta, y MUST diferir únicamente en la foto.
- **FR-019**: La foto MUST ser opcional. El sistema MUST NOT exigir foto para guardar
  un registro, y MUST permitir agregarle una foto más tarde a un registro guardado sin
  foto.
- **FR-020**: El sistema MUST guardar el número actual del juego y MUST permitir que el
  usuario lo modifique a mano.
- **FR-021**: El sistema MUST indicar, sin que el usuario tenga que buscar, si ya tiene
  un registro guardado para el número actual del juego.
- **FR-022**: El sistema MUST mostrar cuántos números consecutivos a partir del actual
  tiene cubiertos con registros guardados.
- **FR-023**: El sistema MUST permitir iniciar y finalizar un recorrido desde la
  pantalla principal, y MUST mostrar de forma visible cuando hay uno en curso.
- **FR-024**: Durante un recorrido en curso, el sistema MUST registrar por dónde pasa
  el jugador con un muestreo espaciado de 30 segundos o más, o su equivalente por
  distancia recorrida. El objetivo MUST ser identificar qué calles se recorrieron, no
  reconstruir el camino exacto.
  > **Superado por el FR-036 de la especificación 003 (rumbo y aspecto).** El muestreo
  > espaciado alcanzaba mientras el trayecto solo alimentaba la grilla de celdas del FR-032:
  > para pintar una celda basta con saber que se estuvo cerca. Desde que el trazo se dibuja
  > como camino, un punto cada 50 metros se saltea las esquinas y el recorrido sale cortando
  > diagonal por dentro de las manzanas. Sigue vigente la intención —el registro del
  > recorrido no puede costar la batería de la salida—; lo que cambia es que ahora sí hay que
  > reconstruir el camino, porque el camino es lo que se muestra.
- **FR-025**: El sistema MUST NOT usar la precisión de captura para el registro de
  recorridos. La precisión máxima queda reservada para la ubicación de los registros de
  captura, según el Principio II de la constitución.
  > **Superado por el FR-036 de la especificación 003 (rumbo y aspecto).** La prioridad
  > balanceada resuelve la posición por antenas y wifi, con decenas de metros de error. Con
  > una manzana midiendo alrededor de cien, ese error alcanza para poner el trazo entero
  > sobre la calle de al lado, que es lo que pasó en la salida validada. El Principio II pide
  > que la captura tenga la mejor precisión disponible, no que el recorrido tenga la peor.
- **FR-026**: El sistema MUST seguir registrando el trayecto con la app en segundo
  plano o con la pantalla apagada.
- **FR-027**: El sistema MUST asociar al recorrido en curso las capturas hechas
  mientras dura.
- **FR-028**: El sistema MUST avisar al jugador cuando pasa a menos de 150 metros de la
  ubicación de un registro pendiente cuyo número coincide con el número actual del
  juego, haya o no un recorrido en curso. El aviso MUST indicar qué patente es y dónde
  está.
- **FR-029**: El sistema MUST NOT avisar por registros cuyo número todavía no toca, ni
  por registros ya compartidos.
- **FR-030**: El sistema MUST solicitar el permiso de ubicación en segundo plano antes
  de habilitar los avisos de proximidad o los recorridos, explicando para qué se usa, y
  MUST permitir usar el resto de la app sin ese permiso.
- **FR-031**: El sistema MUST mostrar las calles recorridas en un recorrido terminado
  junto con las patentes capturadas durante ese recorrido.
- **FR-032**: El sistema MUST mostrar de forma acumulada, sobre el mapa, qué calles ya
  fueron exploradas en el conjunto de todos los recorridos, para que el jugador pueda
  elegir por dónde ir la próxima vez.
  > **Superado por el FR-021 de la especificación 003 (rumbo y aspecto).** La grilla de
  > celdas que cumplía este requisito se retiró después del primer uso real: en la calle se
  > veía como rectángulos que cubrían la zona sin parecerse a un recorrido. La pregunta que
  > el jugador se hace al volver no es "¿pasé por acá?" sino "¿por dónde fui?", y esa la
  > contesta el trazo del camino, que además dice lo otro. Sigue vigente la intención —ver
  > lo recorrido acumulado sobre el mapa—; lo que cambia es cómo se dibuja.
- **FR-033**: El sistema MUST conservar el trayecto parcial de un recorrido si el
  registro se interrumpe, en lugar de descartarlo.
- **FR-034**: La pantalla principal MUST tener un mapa de fondo que muestre la posición
  actual del jugador y la ubicación de sus registros guardados.
- **FR-035**: El mapa MUST distinguir visualmente los registros pendientes de los ya
  compartidos, y los que ya tocan por número de los que todavía no.
- **FR-036**: Los botones de acción (`+`, cámara y "Empezar recorrido") MUST estar en
  la parte inferior de la pantalla principal.
- **FR-037**: El jugador MUST poder bajar el teclado para ver el mapa completo y volver
  al campo de número sin salir de la pantalla principal.
- **FR-038**: El estado de carga del mapa MUST NOT bloquear ni demorar la captura: una
  carga rápida MUST poder completarse con el mapa todavía cargando o sin cargar.
- **FR-039**: El aviso de proximidad MUST funcionar con la app cerrada, delegando el
  seguimiento al mecanismo de bajo consumo del sistema operativo en lugar de mantener
  la app despierta vigilando la ubicación.
- **FR-040**: El jugador MUST poder apagar los avisos de proximidad sin perder el resto
  de las funciones de la app.
- **FR-041**: El sistema MUST NOT repetir el aviso por el mismo registro más de una vez
  por salida, aunque el jugador pase varias veces por la misma zona.
- **FR-042**: El sistema MUST guardar automáticamente el fondo de mapa de las zonas por
  las que el jugador ya pasó, sin que tenga que pedirlo ni elegir regiones a mano.
- **FR-043**: Sin conexión, el mapa MUST mostrar el fondo guardado de las zonas ya
  visitadas, y MUST distinguir visualmente las zonas para las que no tiene fondo.
- **FR-044**: El sistema MUST acotar cuánto espacio ocupa el fondo de mapa guardado, y
  al llegar al límite MUST liberar primero las zonas menos visitadas.
- **FR-045**: El jugador MUST poder ver cuánto espacio ocupa el fondo de mapa guardado
  y MUST poder borrarlo por completo.

### Key Entities

- **Registro de captura**: una patente vista y anotada. Atributos: número de 3 dígitos,
  texto completo de la patente, formato detectado, coordenadas, precisión en metros,
  timestamp de captura, indicador de precisión degradada, estado de uso
  (pendiente / compartida), foto asociada opcional.
- **Foto**: la imagen de la patente. Pertenece a exactamente un registro de captura y
  hereda su ubicación y timestamp; no los redefine.
- **Estado del juego**: en qué número va el grupo. Un solo valor, editable a mano por
  el jugador. Es la referencia contra la que se calcula qué registros ya tocan y
  cuántos números consecutivos hay cubiertos hacia adelante.
- **Recorrido**: una salida del jugador. Atributos: momento de inicio, momento de fin,
  estado (en curso / terminado), trayecto recorrido. Agrupa los registros de captura
  hechos mientras estuvo en curso.
- **Punto de trayecto**: una posición registrada durante un recorrido, con muestreo
  espaciado. Atributos: coordenadas, precisión, timestamp. Pertenece a exactamente un
  recorrido. Su precisión es deliberadamente menor que la de un registro de captura:
  solo tiene que alcanzar para identificar la calle.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El jugador completa una carga rápida en 10 segundos o menos desde que
  abre la app hasta la confirmación de guardado.
- **SC-002**: La carga rápida requiere 3 interacciones o menos: abrir la app, tipear el
  número, tocar `+`. La captura con foto agrega solo el paso de la cámara.
  > **Superado por el SC-008 de la especificación 002**, por la misma razón que el FR-016
  > de arriba: el techo pasa a ser 4 interacciones, con el toque sobre el campo incluido.
  > La constitución 1.1.0 enmendó el Principio I antes de que el cambio se implementara.
- **SC-003**: El 100% de las capturas hechas sin conexión a internet quedan
  persistidas y visibles al reabrir la app.
- **SC-004**: El 100% de los registros guardados tienen coordenadas, precisión en
  metros y timestamp; ninguno queda con esos campos vacíos o rellenados por defecto.
- **SC-005**: El jugador encuentra un registro guardado buscando su número en 5
  segundos o menos.
- **SC-006**: Ningún registro cambia su ubicación ni su timestamp después de creado,
  verificable comparando cada registro contra su estado inicial.
- **SC-007**: El jugador usa la app durante un mes completo sin crear cuenta, sin
  configurar nada y sin conexión permanente a internet.
- **SC-008**: El jugador sabe si tiene la patente del número actual del juego apenas
  abre la app, sin escribir nada ni navegar a otra pantalla.
- **SC-009**: El jugador recibe el aviso de proximidad estando a 150 metros o menos de
  la patente que le toca, con la app cerrada y sin ningún recorrido activo.
- **SC-010**: Un recorrido de 2 horas no consume más del 5% de la batería del
  teléfono.
- **SC-011**: El 100% de los recorridos interrumpidos por el sistema operativo
  conservan el trayecto registrado hasta el momento de la interrupción.
- **SC-012**: Tener los avisos de proximidad activos, sin recorridos, no cuesta más del
  3% de batería por día.
- **SC-013**: Sin conexión, el jugador ve las calles de las zonas por las que ya pasó,
  no un fondo vacío.
- **SC-014**: El espacio total que ocupa la app en el teléfono se mantiene por debajo de
  un límite conocido y visible para el jugador, sin crecer sin control con el uso.
- **SC-015**: Después de varias salidas, el jugador puede ver de un vistazo qué calles
  de su zona ya exploró y cuáles no, sin abrir cada recorrido por separado.
  > **Superado por el SC-003 de la especificación 003**, por la misma razón que el FR-032 de
  > arriba: lo que se mide pasa a ser que el jugador reconozca el camino que hizo y pueda
  > señalar por qué calles pasó, en lugar de contar cuadras pintadas.

## Assumptions

- Un solo usuario y un solo teléfono. No hay sincronización con el grupo ni con otros
  dispositivos; el grupo sigue siendo WhatsApp. Deriva del Principio IV de la
  constitución.
- El número del juego es de 3 dígitos (rango 000 a 999), consistente con el estado
  actual del grupo (313). No se contempla que el juego pase de 999.
- El número actual del juego lo mantiene el jugador a mano. La app no lee el grupo de
  WhatsApp ni deduce el avance sola: el grupo sigue siendo la fuente de verdad y la app
  solo lo espeja. Un contador desincronizado es un problema de uso, no de datos.
- Compartir al grupo se hace a través del mecanismo de compartir nativo del teléfono,
  no mediante una integración propia con WhatsApp.
- Los registros compartidos se archivan, no se borran. El volumen esperado es de unos
  cientos de registros a lo largo de la vida del juego, no miles.
- El radio de aviso de proximidad es de 150 metros, elegido como distancia urbana
  razonable para reaccionar a pie o en auto. Es un valor a calibrar contra el uso real,
  no una constante sagrada.
- Con muestreo cada 30 segundos, un recorrido de 2 horas son unos 240 puntos, del orden
  de 8 KB. Cien recorridos no llegan a 1 MB. Por eso no se construye limpieza automática
  de recorridos: el volumen no lo justifica, y el Principio IV lo prohíbe hasta que un
  problema real y medido aparezca.
- El aviso de proximidad solo tiene sentido para registros guardados sin foto, que son
  los que obligan a volver al lugar. Un registro que ya tiene foto se puede compartir
  sin moverse.
- El fondo de mapa se guarda solo de las zonas que el jugador realmente recorre, que en
  la práctica son su barrio y sus trayectos habituales. No se contempla descargar
  ciudades enteras ni elegir regiones a mano.
- El espacio ocupado por la app tiene tres consumidores de tamaño muy distinto: los
  registros (chicos), las fotos (medianos) y el fondo de mapa más los puntos de
  trayecto (grandes). El límite de espacio se dimensiona contra estos dos últimos.
- El teléfono tiene un proveedor de ubicación de alta precisión disponible; sin él la
  app no puede cumplir su propósito.
- La app se usa mayormente al aire libre y en movimiento, con señal de ubicación
  variable y conexión de datos poco confiable.
- La spec no toma posición sobre la regla del grupo respecto de guardar patentes
  futuras. La app registra lo que el jugador captura; el acuerdo social entre los
  amigos queda fuera del alcance del software.
