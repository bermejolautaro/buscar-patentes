# Feature Specification: Jugar en el iPhone

**Feature Branch**: `006-jugar-en-iphone`

**Created**: 2026-09-28

**Status**: Draft

**Input**: User description:

> Quiero que la app funcione en iPhone, que pasaría a ser mi teléfono de juego. No tengo Mac:
> compilar en GitHub Actions e instalar desde Windows con Sideloadly y un Apple ID gratis,
> reinstalando cada 7 días sin perder datos. Tengo que poder llevar mis patentes, recorridos y
> fotos del Android al iPhone. Android tiene que seguir funcionando igual.

Y en la conversación que la originó:

> No tengo acceso a una mac. Si logro hacerlo funcionar en iphone entonces sería mi teléfono de
> juego. El hecho de reinstalar la app cada 7 días no me molesta asumiendo de que no pierdo nada a
> nivel data.

## Contexto

La app existe solo para Android. El jugador quiere jugar con un iPhone, y el iPhone pasa a ser el
teléfono de juego. El Android deja de usarse todos los días, pero su app no se puede romper: hoy
tiene todos los datos, y es el respaldo si el camino del iPhone falla.

Tres condiciones marcan la feature:

1. **No hay Mac.** Apple solo deja compilar apps de iPhone en una Mac. La compilación pasa a una
   Mac en la nube, y la instalación se hace desde la PC con Windows.
2. **El Apple ID es gratis.** Una app instalada así **vence a los 7 días**: queda en la pantalla
   del iPhone pero no abre hasta que se reinstala. El jugador acepta reinstalar cada semana **solo
   si no pierde nada**. Y en la calle, una app vencida no abre: no anota, no avisa y no graba la
   salida.
3. **Los datos están en el Android.** Son patentes con su ubicación, hora y precisión, fotos,
   salidas con su camino, votos de confianza y el número actual. Si no se pueden llevar, el
   iPhone arranca de cero y la evidencia juntada queda en un teléfono que ya no se usa.

### Qué es distinto en el iPhone

| Qué | En el Android hoy | En el iPhone |
|---|---|---|
| Instalar | Un APK que el jugador abre desde Descargas | Desde la PC, firmado con su Apple ID, y otra vez cada 7 días |
| Respaldar | Un script en la PC que copia los datos por cable | El iPhone no deja sacar los datos de una app por cable: el respaldo sale desde la app |
| Avisos de proximidad | Hasta 100 lugares vigilados | Hasta 20. La app solo vigila las patentes del número actual que no se compartieron, que suelen ser entre cero y unas pocas, así que el límite no debería notarse |
| Salida con la pantalla apagada | Sigue aunque se cierre la app | Si el jugador cierra la app deslizándola en el multitarea, el iPhone corta la grabación |
| Volver atrás | Botón o gesto del sistema | No hay botón: cada pantalla necesita su propia forma de volver |
| Ubicación | Siempre precisa si hay permiso | El jugador puede apagar "ubicación exacta" y dejar solo una aproximada |

## Clarifications

### Session 2026-09-28

- Q: ¿El repositorio en GitHub puede ser público? → A: Sí, pero limpio antes. Sin simulador de
  iPhone en Windows, cada prueba del lado iPhone es una compilación en la nube, y un repositorio
  privado tiene unas 15 gratis por mes. Antes de subirlo se sacan las coordenadas de la zona del
  jugador y la captura del mapa, y se sube un historial nuevo, sin el viejo y sin su email
  personal (FR-005, FR-005a).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Anotar y encontrar patentes en el iPhone (Priority: P1)

Con el iPhone en la mano, el jugador abre la app, toca el campo, tipea 318 y confirma. La patente
queda guardada con ubicación, hora y precisión, aunque no haya señal. En el mapa ve sus patentes
como en el Android: la barra con el número actual y cuántas hay descubiertas, la que toca en verde,
el filtro por número, la lista ordenada y la ficha con los votos.

**Why this priority**: es el juego. Sin esto no hay app. Además es lo mínimo que prueba que toda la
cadena funciona: compilar sin Mac, instalar desde Windows y abrir en el iPhone.

**Independent Test**: compilar en la nube, instalar desde la PC, anotar tres patentes (una sin
señal), verlas en el mapa y en la lista, abrir una ficha y votar.

**Acceptance Scenarios**:

1. **Given** la app instalada y la ubicación disponible, **When** el jugador abre la app, toca el
   campo, tipea el número y confirma, **Then** la patente queda guardada con ubicación, hora y
   precisión, en 4 interacciones o menos.
2. **Given** el iPhone sin conexión, **When** el jugador anota una patente, **Then** se guarda
   igual y aparece al volver a abrir la app.
3. **Given** una lectura de ubicación peor que el umbral de precisión, **When** el jugador anota,
   **Then** el registro queda marcado como degradado, igual que en el Android.
4. **Given** los mismos datos cargados en los dos teléfonos, **When** el jugador mira el mapa, la
   barra y la lista en cada uno, **Then** ve lo mismo en los dos: los mismos pines y colores de
   confianza, la misma que toca en verde, los mismos grupos al mismo zoom, el mismo orden de la
   lista y el mismo texto en la barra.
5. **Given** una patente guardada, **When** el jugador abre su ficha, **Then** no hay forma de
   editar su ubicación ni su hora.
6. **Given** cualquier pantalla que no sea la principal, **When** el jugador quiere volver,
   **Then** tiene una forma visible de hacerlo sin botón del sistema.

---

### User Story 2 - Reinstalar cada semana sin perder nada (Priority: P1)

La instalación vence cada 7 días. El jugador enchufa el iPhone a la PC y vuelve a instalar, sea la
misma versión o una nueva. La app abre con todo lo que tenía: patentes, fotos, salidas, votos,
número actual y ajustes. La app le avisa con tiempo que se viene el vencimiento, para que no lo
sorprenda en la calle. Y en cualquier momento puede sacar un respaldo completo a un lugar fuera de
la app.

**Why this priority**: es la condición que el jugador puso para usar el iPhone. Si reinstalar
borra algo, el iPhone no sirve como teléfono de juego.

**Independent Test**: con datos cargados, reinstalar la misma versión y después una nueva, y
comparar antes y después la cantidad de patentes, fotos y salidas y un registro puntual campo por
campo. Mirar la fecha de vencimiento en ajustes y verificar el aviso cuando faltan dos días.

**Acceptance Scenarios**:

1. **Given** la app con datos, **When** se instala una versión nueva encima, **Then** todos los
   datos siguen, idénticos.
2. **Given** la instalación vencida, **When** el jugador la reinstala, **Then** la app abre con
   todos los datos.
3. **Given** faltan dos días o menos para el vencimiento, **When** el jugador abre la app,
   **Then** ve en la pantalla principal cuándo vence, y la carga rápida sigue entrando en 4
   interacciones.
4. **Given** falta un día para el vencimiento, **When** llega ese momento, **Then** el iPhone
   muestra una notificación aunque la app esté cerrada.
5. **Given** la app con datos, **When** el jugador pide un respaldo, **Then** obtiene un solo
   archivo con todo, y lo puede guardar donde elija fuera de la app.
6. **Given** un respaldo del iPhone, **When** se restaura en una instalación vacía, **Then** todo
   vuelve idéntico.

---

### User Story 3 - Traer todo del Android (Priority: P1)

El jugador saca un respaldo del Android y lo abre en el iPhone. En el iPhone quedan todas las
patentes con la misma ubicación, hora y precisión, al detalle, y también sus fotos, las marcas de
compartida, los votos, las salidas con sus caminos y el número actual del juego. Si algún día el
iPhone falla, el camino inverso también funciona.

**Why this priority**: sin esto el iPhone arranca vacío, y la evidencia de meses de salidas queda
en un teléfono que ya no se usa.

**Independent Test**: con un respaldo real del Android, restaurarlo en un iPhone vacío. Comparar
la cantidad de patentes, fotos y salidas, y cinco registros al azar campo por campo. Abrir sus
fotos y ver que las salidas se dibujan igual.

**Acceptance Scenarios**:

1. **Given** el iPhone sin datos y un respaldo del Android, **When** el jugador lo restaura,
   **Then** coinciden la cantidad de patentes, fotos y salidas, y el número actual.
2. **Given** cualquier registro traído, **When** se compara con el original, **Then** latitud,
   longitud, precisión, hora, marca de degradada, formato y texto de la patente son idénticos, sin
   redondear nada.
3. **Given** una salida que en el Android ya tenía su camino ajustado a las calles, **When** se
   la mira en el iPhone, **Then** se dibuja igual sin volver a pedir el ajuste.
4. **Given** el iPhone ya tiene patentes, **When** el jugador restaura un respaldo, **Then** la
   app muestra qué hay de cada lado (cuántas patentes y la fecha de la última captura), pide
   confirmación, y antes de reemplazar guarda un respaldo de lo que había.
5. **Given** un archivo que no es un respaldo, está roto o viene de una versión más nueva de la
   app, **When** el jugador lo abre, **Then** la app lo rechaza con un mensaje que dice por qué, y
   no toca los datos.
6. **Given** un respaldo sacado del iPhone, **When** se restaura en el Android, **Then** queda
   todo idéntico, igual que en el camino de ida.

---

### User Story 4 - La foto, el WhatsApp y el camino hasta la patente (Priority: P2)

Cuando llega la que toca, el jugador va hasta ella, le saca una foto desde la app y la manda al
grupo de WhatsApp con el texto de siempre. La patente queda marcada como compartida. Para llegar,
"Ir" abre Google Maps a pie hasta la patente, y el recorrido por varias abre Google Maps con todas
las paradas elegidas.

**Why this priority**: es cómo se gana el turno en el juego. Va después de la P1 porque sin
anotar y sin los datos no hay nada que mandar.

**Independent Test**: sacar una foto desde la ficha, mandarla a un chat de WhatsApp y ver que la
patente pasa a compartida; tocar "Ir" y armar un recorrido de tres paradas, y ver que Google Maps
abre con esas paradas.

**Acceptance Scenarios**:

1. **Given** una patente guardada, **When** el jugador le saca una foto desde la app, **Then** la
   foto queda asociada a esa patente y se ve en su ficha.
2. **Given** una patente con foto, **When** el jugador la comparte, **Then** puede elegir WhatsApp
   y el mensaje lleva la foto y el texto, igual que en el Android.
3. **Given** el jugador compartió una patente, **When** vuelve a la app, **Then** la patente queda
   marcada como compartida.
4. **Given** una o varias paradas elegidas, **When** el jugador toca "Abrir en Google Maps",
   **Then** se abre Google Maps a pie con esas paradas; si Google Maps no está instalado, se abre
   en el navegador.

---

### User Story 5 - Avisos con la app cerrada (Priority: P2)

El juego está en 318 y el jugador tiene una 318 anotada a tres cuadras. Camina con el iPhone en el
bolsillo y la app cerrada. Al pasar a 150 metros o menos, el iPhone le avisa. Tocar el aviso abre
el mapa en esa patente. El aviso funciona igual después de reiniciar el iPhone.

**Why this priority**: es lo que convierte una patente vista hace meses en una patente que se
encuentra hoy. Va después de la P1 porque sin patentes anotadas no hay nada que vigilar.

**Independent Test**: con una patente del número actual anotada y sin compartir, cerrar la app,
reiniciar el iPhone y caminar hasta ella.

**Acceptance Scenarios**:

1. **Given** una patente del número actual sin compartir y la app cerrada, **When** el jugador
   pasa a 150 metros o menos, **Then** recibe un aviso.
2. **Given** el iPhone recién reiniciado y la app sin abrir, **When** el jugador pasa cerca,
   **Then** el aviso llega igual.
3. **Given** un aviso, **When** el jugador lo toca, **Then** la app abre en el mapa sobre esa
   patente.
4. **Given** el jugador ya recibió el aviso de una patente en la salida actual, **When** vuelve a
   pasar por la misma cuadra, **Then** no recibe otro aviso de esa misma patente en esa salida.
5. **Given** el jugador dio permiso de ubicación solo "mientras se usa la app", **When** abre la
   app, **Then** la app le dice que los avisos no van a llegar con la app cerrada, y cómo
   arreglarlo.

---

### User Story 6 - Salidas con la pantalla apagada (Priority: P3)

El jugador empieza una salida, bloquea el iPhone y lo guarda. Dos horas después, la salida tiene
su camino entero, que se ajusta a las calles cuando hay conexión. En el mapa, las calles caminadas
se pintan por antigüedad, igual que en el Android, y la pantalla de salidas las lista.

**Why this priority**: da el mapa de dónde ya se buscó, que sirve para planear. Pero se puede
anotar, avisar y compartir sin esto, así que va última.

**Independent Test**: empezar una salida, bloquear la pantalla, caminar treinta minutos, terminar
y ver el camino dibujado y ajustado.

**Acceptance Scenarios**:

1. **Given** una salida en curso, **When** el jugador bloquea el iPhone y camina, **Then** el
   camino completo queda grabado.
2. **Given** una salida en curso, **When** el jugador cierra la app deslizándola en el multitarea,
   **Then** lo grabado hasta ese momento se conserva, y al volver a abrir la app le dice que la
   salida se cortó.
3. **Given** una salida terminada y conexión, **When** se ajusta a las calles, **Then** se dibuja
   ajustada, igual que en el Android.
4. **Given** varias salidas en distintas fechas, **When** el jugador mira el modo del mapa por
   antigüedad, **Then** ve los mismos escalones que en el Android.

---

### Edge Cases

- **La app vence en la calle**: no abre. No se puede anotar, no llegan avisos, y una salida en
  curso se corta; lo grabado hasta ahí se conserva. Para eso están el aviso de dos días antes y la
  notificación del día anterior (FR-011, FR-012).
- **Se firma con otro Apple ID**: el iPhone la trata como otra app y no deja instalarla encima sin
  borrar la anterior, y borrarla borra los datos. Hay que firmar siempre con el mismo Apple ID, y
  respaldar antes de cualquier cambio de cuenta.
- **El jugador borra la app por error**: los datos se pierden salvo el último respaldo. Ajustes
  dice de cuándo es el último (FR-020).
- **Se restaura un respaldo más viejo que lo que hay**: la app lo muestra en la confirmación
  (fecha de la última captura de cada lado), y lo que había queda respaldado antes de reemplazar
  (FR-016).
- **Al respaldo le falta la foto de un registro**: el registro se trae igual, sin foto, y la app
  dice cuántas fotos faltaron. Un registro nunca se descarta por su foto (FR-019).
- **No hay lugar en el iPhone para restaurar**: la restauración falla de forma visible, y los datos
  quedan como estaban. No queda nada a medias (FR-018).
- **"Ubicación exacta" apagada**: la lectura llega con una precisión de kilómetros. Se guarda así,
  degradada, sin corregirla (Principio II), y la app avisa que la ubicación exacta está apagada
  (FR-024).
- **Más patentes del número actual que lugares que el iPhone deja vigilar**: se aplica el mismo
  recorte que hoy en el Android, con el límite del iPhone (FR-025). Con 20 lugares y el juego
  vigilando un solo número, no se espera que pase.
- **Falla la compilación en la nube**: la versión instalada sigue como estaba. Ningún dato vive en
  la nube, así que no hay nada que perder.
- **WhatsApp o Google Maps no están instalados**: compartir ofrece las otras apps del iPhone, e ir
  abre en el navegador, igual que en el Android.

## Requirements *(mandatory)*

### Compilar e instalar sin Mac

- **FR-001**: La versión de iPhone MUST poder compilarse sin una Mac propia, en un servicio en la
  nube, lanzada desde la PC del jugador.
- **FR-002**: El resultado MUST ser un archivo que el jugador instala desde Windows con Sideloadly
  y un Apple ID gratis.
- **FR-003**: Las credenciales de Apple del jugador MUST NOT salir de su PC: el servicio que
  compila no las recibe ni las guarda. El archivo se firma en la PC al instalar.
- **FR-004**: El identificador de la app MUST ser el mismo en todas las compilaciones, para que
  cada instalación reemplace a la anterior en vez de sumarse como otra app.
- **FR-005**: El código MUST vivir en un repositorio público de GitHub, para que las compilaciones
  en la nube no tengan tope por mes.
- **FR-005a**: Antes de publicarlo, el repositorio MUST quedar sin nada que ubique al jugador: las
  pruebas pasan a usar coordenadas de otra zona, sale la captura del mapa de la 005, y se sube un
  historial nuevo, sin el viejo y sin su email personal. El historial completo sigue en la PC.
- **FR-006**: Lo que se sube a la nube MUST NOT incluir datos del jugador: ni respaldos, ni bases,
  ni fotos, ni capturas del mapa con patentes reales, ni coordenadas de su zona. Vale para la
  primera subida y para todas las que sigan.

### Que no se pierda nada

- **FR-007**: Instalar una versión nueva encima de la anterior MUST conservar todo: las patentes con
  su evidencia, las fotos, las salidas con sus puntos y su camino ajustado, los votos, el número
  actual, los ajustes y qué avisos ya se dieron en la salida en curso.
- **FR-008**: Una instalación vencida MUST conservar los datos, y reinstalar MUST devolverlos.
- **FR-009**: Si una versión nueva cambia la forma de guardar los datos, MUST convertir los que ya
  había sin perder ninguno, en los dos teléfonos.
- **FR-010**: Ajustes MUST mostrar la fecha y la hora en que vence la instalación del iPhone.
- **FR-011**: Desde dos días antes del vencimiento, la pantalla principal MUST mostrar cuándo vence.
  El aviso MUST NOT agregar interacciones a la carga rápida (Principio I).
- **FR-012**: Un día antes del vencimiento, el iPhone MUST mostrar una notificación aunque la app
  esté cerrada.

### Respaldo

- **FR-013**: En los dos teléfonos, el jugador MUST poder sacar desde la app un respaldo completo en
  un solo archivo, y guardarlo donde elija fuera de la app.
- **FR-014**: Restaurar un respaldo MUST dejar cada registro idéntico al original: la misma latitud,
  longitud, precisión, hora y marca de degradada, sin redondear ni recalcular nada (Principio II).
- **FR-015**: El formato del respaldo MUST ser el mismo en los dos teléfonos. El mismo archivo sirve
  para mudarse al iPhone y para volver al Android.
- **FR-016**: Si el teléfono ya tiene patentes, restaurar MUST mostrar qué hay de cada lado (cuántas
  patentes y la fecha de la última captura), pedir confirmación, y guardar primero un respaldo de
  lo que había.
- **FR-017**: Restaurar MUST reemplazar los datos, no fusionarlos. Se juega con un teléfono a la
  vez (Principio IV).
- **FR-018**: Un archivo que no es un respaldo, está roto o viene de una versión más nueva de la app
  MUST rechazarse con un mensaje que diga por qué. Una restauración que falla a mitad de camino MUST
  dejar los datos como estaban.
- **FR-019**: Una foto que falta en el respaldo MUST NOT impedir traer su registro. El registro
  llega sin foto, y la app dice cuántas fotos faltaron.
- **FR-020**: Ajustes MUST mostrar la fecha del último respaldo.

### Lo mismo que en el Android

- **FR-021**: En el iPhone MUST funcionar todo lo que hoy hace la app de Android, con el mismo
  comportamiento: la carga rápida, los dos formatos de patente, el mapa (con sus modos, el borde de
  confianza, la que toca en verde, los pines que se hacen lugar y los grupos), la barra con el
  filtro y la lista, la ficha, los votos, la foto, compartir, ir y el recorrido por varias en
  Google Maps, los avisos, las salidas con la pantalla apagada, el ajuste a las calles, la pantalla
  de salidas, los ajustes y el modo oscuro. Los requisitos de las specs 001 a 005 valen en el iPhone,
  salvo donde esta spec dice otra cosa.
- **FR-022**: Con los mismos datos, los dos teléfonos MUST dar el mismo resultado: la misma cuenta
  en la barra, el mismo orden en la lista, la misma confianza, los mismos grupos al mismo zoom y el
  mismo orden de paradas propuesto.
- **FR-023**: La app MUST verse igual en los dos teléfonos: mismas pantallas, colores y textos. En
  el iPhone, toda pantalla de la que en el Android se sale con el botón atrás MUST tener una forma
  visible de volver.
- **FR-024**: Si la ubicación exacta está apagada, la app MUST guardar la precisión que reporta el
  teléfono, marcar el registro como degradado si corresponde, y avisar que la ubicación exacta está
  apagada.
- **FR-025**: Los avisos MUST vigilar las mismas patentes que en el Android: las del número actual
  que no se compartieron. Si son más de las que el iPhone deja vigilar, MUST aplicarse el mismo
  recorte que en el Android, con el límite del iPhone.
- **FR-026**: Los avisos MUST llegar con la app cerrada y después de reiniciar el iPhone. La única
  excepción es la instalación vencida.
- **FR-027**: Si el jugador no dio el permiso de ubicación que los avisos y las salidas necesitan
  con la app cerrada, la app MUST decirle qué no va a funcionar y cómo arreglarlo.
- **FR-028**: Si una salida se corta, porque el jugador cerró la app, porque la cortó el sistema o
  porque venció la instalación, lo grabado MUST conservarse, y al volver a abrir la app MUST decir
  que la salida se cortó. El mensaje es del iPhone: en el Android la grabación no se corta al cerrar
  la app, y el FR-031 no deja sumar cambios visibles.

### El Android sigue igual

- **FR-029**: La app de Android MUST seguir haciendo todo lo que hace hoy, igual. Todas sus pruebas
  automáticas MUST seguir pasando.
- **FR-030**: Pasar la app de Android a la versión de esta feature MUST ser una actualización que
  conserva todos los datos, no una reinstalación.
- **FR-031**: El único cambio visible en el Android MUST ser el respaldo desde la app (FR-013).

### Key Entities

- **Respaldo**: un archivo con todo lo del jugador: patentes con su evidencia, fotos, salidas con
  sus puntos y su camino ajustado, votos, número actual y ajustes. Dice de qué versión del formato
  es, cuándo se sacó y de qué teléfono salió, y cuántas patentes, fotos y salidas trae, para poder
  mostrarlo antes de restaurar.
- **Instalación del iPhone**: tiene una fecha de vencimiento, que la app conoce y muestra.
- Las entidades que ya existen (registro de captura, salida, punto de trayecto, estado del juego)
  no cambian. En el iPhone son las mismas.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Desde un cambio en el código hasta la app instalada en el iPhone pasan menos de 30
  minutos, sin Mac y sin salir de la PC.
- **SC-002**: La reinstalación semanal toma menos de 5 minutos con el iPhone enchufado, y después
  están el 100% de las patentes, fotos y salidas.
- **SC-003**: Mudarse del Android al iPhone toma menos de 15 minutos. Llegan el 100% de las
  patentes, fotos y salidas, con cero diferencias en ubicación, hora y precisión al compararlas
  campo por campo.
- **SC-004**: En el iPhone, la carga rápida entra en 4 interacciones y en 10 segundos o menos,
  igual que en el Android.
- **SC-005**: Puestos lado a lado con los mismos datos, los dos teléfonos muestran la misma barra,
  la misma lista en el mismo orden y los mismos grupos al mismo zoom.
- **SC-006**: Con la app cerrada, también después de reiniciar el iPhone, el aviso llega a 150
  metros o menos de la patente que toca.
- **SC-007**: Una salida de 2 horas con la pantalla bloqueada queda grabada entera y no consume más
  del 5% de la batería, igual que se le pide al Android (SC-010 de la 001).
- **SC-008**: El jugador nunca descubre el vencimiento en la calle: se entera por lo menos dos días
  antes.
- **SC-009**: En el Android, todas las pruebas automáticas pasan, y actualizar la app conserva el
  100% de los datos.
- **SC-010**: No hay ninguna credencial de Apple guardada en el servicio que compila.
- **SC-011**: Buscar en el repositorio público, en todos sus commits, no encuentra coordenadas de la
  zona del jugador, capturas del mapa con patentes reales ni su email personal.

## Assumptions

- **El iPhone reemplaza al Android, no lo acompaña.** El Principio IV dice un usuario, un teléfono.
  La mudanza es una vez; el camino de vuelta existe para cuando el iPhone falle, no para usar los
  dos a la par. Usar los dos a la vez pediría fusionar datos, y eso es sincronizar: primero habría
  que enmendar la constitución.
- **Siempre el mismo Apple ID.** Firmar con otro hace que el iPhone la trate como otra app (ver
  Edge Cases).
- **El Apple ID gratis alcanza.** La app no necesita nada que Apple reserve a las cuentas pagas: ni
  notificaciones enviadas desde un servidor, ni servicios de Apple. El plan MUST verificar que los
  avisos y la ubicación con la pantalla apagada funcionan con una cuenta gratis antes de dar la
  feature por viable.
- **El iPhone tiene iOS 15.5 o más**, que es lo que pide la biblioteca del mapa. Con iOS 16 o más,
  además, hay que activar el modo de desarrollador para instalar así. La prueba piloto anota la
  versión del teléfono del jugador.
- **La PC tiene Windows, Sideloadly y los componentes de Apple que Sideloadly pide**, y el jugador
  tiene o crea una cuenta de GitHub.
- **La app se ve igual que en el Android, no como una app de iPhone.** El jugador conoce esta app;
  rediseñarla no es parte del pedido.
- **OpenFreeMap, el servicio de ajuste a calles, Google Maps y WhatsApp funcionan igual en el
  iPhone.** No dependen del teléfono.
- **La compilación necesita internet; la app no.** El Principio III sigue igual: anotar funciona
  sin conexión en los dos teléfonos.
- **Una cuenta paga de Apple (USD 99 por año) sacaría el vencimiento semanal.** Queda fuera por
  ahora; si reinstalar cada semana resulta molesto, es la salida.

## Out of Scope

- **App Store, TestFlight y la cuenta paga de Apple.**
- **Usar los dos teléfonos a la vez**, sincronizarlos o fusionar sus datos.
- **Rediseñar la app al estilo iPhone.**
- **iPad, Apple Watch, widgets, Siri y CarPlay.**
- **Instalar o renovar desde el iPhone sin la PC.**
- **Lo que queda en el `TODO.md`**: las diagonales de los caminos ajustados, el interruptor entre
  camino crudo y ajustado, la confianza por turno y las zonas objetivo. Cada uno sigue esperando
  su propia spec.
