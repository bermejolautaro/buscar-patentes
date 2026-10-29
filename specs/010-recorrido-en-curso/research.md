# Research: Recorrido en curso

## D1. Los km salen de los tramos que ya dibuja el mapa

**Decision**: los metros de una salida son la suma de las distancias entre puntos consecutivos
**adentro de cada tramo** de `Geo.tramos`, el mismo corte de 250 m que dibuja los huecos punteados
(FR-009 de la 003). Un hueco no suma. Va en una función pura nueva, `Caminata.metros(puntos)`.

**Rationale**: los puntos ya llegan filtrados por precisión (30 m) y por distancia mínima (10 m), así
que quieto no se acumula ruido. Usar el mismo corte que el mapa hace que los km digan lo mismo que el
dibujo: lo punteado ni se ve caminado ni suma. Existe desde el primer punto, también en una salida en
curso, que todavía no tiene camino ajustado.

**Alternatives considered**:
- El largo del camino ajustado de la 007. Es más fiel a las calles, pero recién existe cuando la
  salida termina y hay conexión. La barra en vivo y la lista dirían números distintos de la misma
  salida.
- Suavizar el trazo antes de medir. Sin datos que digan que hace falta: SC-003 lo mide en la calle.

## D2. Calorías activas de caminar: 0,5 kcal por kg y por km

**Decision**: `kcal = 0,5 × peso (kg) × km`, redondeado a múltiplos de 5, y dicho con "~". Sin peso
cargado, no hay número (FR-009).

**Rationale**: es el costo neto de caminar en llano de la ecuación del ACSM: 0,1 ml de O₂ por kg y
por metro, a 5 kcal por litro de O₂. Neto quiere decir sin el gasto en reposo, que es lo que el
jugador eligió (calorías activas): quieto no suma, y depende solo de los km y el peso. Redondear a 5
dice que es un aproximado sin agregar texto. 70 kg y 2,1 km dan 73,5, que se muestra "~75 kcal".

**Alternatives considered**:
- La misma ecuación con el término de reposo (3,5 ml/kg/min), que suma por cada minuto. Son las
  calorías totales, que el jugador descartó.
- METs por velocidad. Piden la velocidad de cada tramo y dan un número parecido a éste para caminar
  en ciudad.

## D3. El peso, una columna más de `estado_del_juego`

**Decision**: `EstadoDelJuego.pesoKg: Int?`, null mientras no se carga. La base pasa a la **9**, con
`SQL_8_9 = ALTER TABLE estado_del_juego ADD COLUMN pesoKg INTEGER` en común, como el `SQL_7_8` de la
008. `Respaldo.VERSION_BASE = 9`: un respaldo de la 8 se restaura y Room lo migra.

**Rationale**: es una preferencia del jugador, y esa tabla ya guarda el modo del mapa por la misma
razón (Principio IV: no un segundo mecanismo de persistencia para un valor). Viaja en el respaldo, y
el peso pasa con el jugador del Android al iPhone. Entero, de 1 a 300 kg: un decimal de peso no
cambia un aproximado redondeado a 5.

**Alternatives considered**: preferencias del sistema (`NSUserDefaults`, `SharedPreferences`). No
viajan en el respaldo y pedirían un `expect` nuevo.

## D4. El aviso del iPhone: una notificación que se reprograma mientras graba

**Decision**: mientras la salida graba, `Grabacion` del iPhone reprograma cada **30 s** una
notificación local con el mismo identificador, para dentro de **75 s**. Mientras la app viva, la
notificación nunca llega a sonar: se corre antes. Si la app muere de cualquier forma, deja de
correrla y suena sola en 75 s o menos (FR-011). Terminar con el botón la borra (FR-013).

Cada reprogramación también borra una notificación ya entregada con ese identificador. Si el sistema
llegara a frenar la app un rato sin matarla, y sonara de más, se va sola apenas la app vuelve.

**Rationale**: no hay ningún aviso del sistema que llegue siempre cuando la app muere: el
`applicationWillTerminate` del cierre por el jugador no llega en un cierre por memoria ni en un
crash. Una notificación que la app tiene que seguir postergando cubre los tres casos con un solo
mecanismo, con las APIs de notificaciones que la app ya usa para el aviso de vencimiento. Mientras
graba, la app no se suspende (`pausesLocationUpdatesAutomatically = false`), así que el ciclo de 30 s
corre también con la pantalla apagada y el jugador quieto. Eso se verifica en la calle (quickstart
§4).

El permiso de notificaciones ya se pide al empezar una salida. Tocar la notificación abre la app, y
la app abre en la pantalla principal (FR-012): no hace falta nada más.

**Alternatives considered**:
- `applicationWillTerminate`: inmediato, pero solo para el cierre por el jugador, y pide tocar el
  `AppDelegate` en Swift.
- Las APIs de ubicación de iOS 17 que reabren la app: no cubren el cierre por el jugador, que es el
  caso pedido. Retomar se descartó en la spec.

## D5. El aviso del Android: no hace falta, y se verifica

**Decision**: nada nuevo en el Android. La salida es un servicio en primer plano, y sacar la app de
recientes no lo detiene (`stopWithTask` es falso por defecto): la salida sigue grabando, y no hay
nada que avisar (FR-023). Se verifica en el teléfono de pruebas, con HyperOS (quickstart §5).

**Rationale**: si HyperOS mata el proceso al sacar la app, lo hace como una detención forzada, que
también cancela cualquier alarma de la app: ningún aviso programado sonaría. Para ese caso queda lo
de hoy, el cartel al abrir la app (FR-028 de la 006). El Android no es el teléfono de juego.

**Alternatives considered**: el mismo mecanismo de D4 con `AlarmManager`. Se suma recién si la
verificación muestra que HyperOS mata el proceso **sin** forzar la detención.

## D6. La barra, la confirmación y Salidas usan las mismas cuentas

**Decision**: un solo resumen de salida, `ResumenDeCaminata(duracionMs, metros, kcal?)`, que arma
`Caminata.resumen(recorrido, puntos, pesoKg, ahora)`. Lo usan la barra (US3), la confirmación (US1),
la lista y el detalle de Salidas (US4) y las estadísticas (US5). Los textos salen de dos funciones de
`Formatos`: `horasYMinutos` ("0:42") y `caminata` ("2,1 km · ~75 kcal").

**Rationale**: SC-006 pide que los km de la semana sean la suma exacta de los de la lista. Con una
sola cuenta, no pueden diferir.

La barra relee los puntos de la salida en curso con cada punto nuevo, como ya hace el trazo, y el
tiempo sube con un ciclo de 20 s mientras hay salida (FR-005).

## D7. Las estadísticas: una función pura y barras de Compose

**Decision**: `Estadisticas` en `domain`: dado un tipo de período (día, semana, mes) y una fecha,
suma las salidas que empezaron adentro y las patentes que se anotaron adentro (FR-016, FR-019). La
semana va de lunes a domingo, en la zona horaria del teléfono, con kotlinx-datetime. Las barras son
`Box` con alto proporcional en una `Row`, sin librería de gráficos (Principio IV).

La pantalla se abre desde Salidas, como un estado más adentro de esa pantalla, igual que el detalle
de una salida: un botón "Estadísticas" arriba de la lista.

**Rationale**: con un año de salidas son unos 100.000 puntos. Salidas ya los lee todos al abrir, y las
cuentas son sumas: SC-007 (menos de 1 s) se mide en el teléfono.

**Alternatives considered**: una librería de gráficos. Es una dependencia para cuatro rectángulos.
