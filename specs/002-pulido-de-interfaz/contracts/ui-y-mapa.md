# Phase 1 Contracts: la superficie de interfaz

**Fecha**: 2026-08-29
**Spec**: [spec.md](./spec.md) · **Research**: [research.md](./research.md)

Esta feature no expone API ni habla con servicios. Su superficie externa es lo que el
jugador ve y toca, y lo que la app le pide al sistema operativo. Estos cuatro contratos son
los puntos donde una decisión mal tomada se nota en el teléfono y no en el compilador.

---

## C1 — Tema: dos esquemas, una fuente de verdad

**Requisito**: FR-001, FR-002. **Origen del bug**: D1.

| Elemento | Contrato |
|---|---|
| Fuente del modo | El ajuste del sistema. La app **no** tiene selector propio |
| Tema de ventana (XML) | Variante DayNight. **Nunca** `android:Theme.Material` a secas, que es la oscura fija y es la causa 1 del bug |
| Tema de Compose | Recibe un esquema claro o uno oscuro según el sistema. **Nunca** `MaterialTheme` sin argumentos, que es claro fijo y es la causa 2 |
| Fondo de cada pantalla | Lo pinta un `Surface` con el color del tema. Ninguna pantalla puede dejar que se vea la ventana desnuda — esa es la causa 3 |

**Las tres causas van juntas.** Arreglar una o dos deja el bug: con la ventana corregida
pero Compose fijo en claro, el modo oscuro sigue dando texto negro sobre fondo oscuro.

**Colores con significado**: los tres del mapa —pendiente, ya toca, compartida (FR-011)—
tienen que seguir distinguiéndose en los dos modos. No se toman del color dinámico del
sistema: son señales, no decoración, y no pueden depender del fondo de pantalla que el
jugador tenga puesto.

**Verificación**: cambiar el modo del teléfono con la app abierta y volver a ella. Si algo
quedó a medio camino, alguna de las tres capas no está siguiendo al sistema.

---

## C2 — Insets: nada debajo del sistema

**Requisito**: FR-003, FR-004.

| Zona | Contrato |
|---|---|
| Barra de estado y muesca | Ningún control ni texto debajo |
| Barra de navegación o gestos | Ningún control debajo |
| Teclado | Los controles se corren, no se tapan |
| Mapa de fondo | **Excepción deliberada**: va a sangre, de borde a borde. Es fondo, y taparle una franja con el color del tema lo empeora |

**Estado**: las tres pantallas secundarias ya cumplen esto desde la convergencia de la 001
(D2). Esta feature lo **verifica en el dispositivo**, no lo construye de nuevo.

**Por qué existe este contrato igual**: Android 15 fuerza edge-to-edge desde targetSdk 35,
y `adjustResize` dejó de achicar la ventana. Cualquier pantalla nueva —la ficha de C3—
nace con el problema si no lo consume explícitamente. El contrato es para lo que viene.

---

## C3 — La ficha de un registro

**Requisito**: FR-012, FR-013, FR-014. **Decisión**: D7.

| Aspecto | Contrato |
|---|---|
| Forma | Hoja que sube desde abajo. El mapa sigue visible detrás: el jugador no pierde de vista dónde estaba el marcador |
| Entrada | Tocar un marcador. **Una sola interacción** desde el mapa (SC-006) |
| Salida | Gesto de bajar la hoja, toque fuera, o gesto de retroceso del sistema |
| Muestra | Número, momento de la captura, precisión —marcada si es degradada—, si tiene foto, y estado pendiente o compartida |
| Acciones | Corregir el número · Compartir · Borrar |
| Borrar | **Pide confirmación.** Es destructivo y no tiene vuelta atrás. Se lleva la foto con él |
| Corregir | Solo el número y el texto. La ficha **no muestra ubicación ni hora como editables**: no es que estén deshabilitadas, es que no son campos |

**Regla de diseño que sostiene el Principio II**: si la ficha mostrara la ubicación en algo
que se parezca a un campo de entrada, aunque esté bloqueado, estaría insinuando que se
puede editar. Se muestran como dato, igual que la fecha.

**Fuera del camino de captura**: la ficha se abre desde el mapa, nunca al guardar. El
Principio I acota el camino de captura a 4 interacciones y esto no está en él.

---

## C4 — El mapa: marcadores, toque y seguimiento

**Requisito**: FR-005 a FR-011, FR-016. **Decisiones**: D4, D5, D6, D9.

### Marcadores

| Parámetro | Valor |
|---|---|
| Contenido | El número de 3 dígitos, con ceros a la izquierda |
| Color | Los tres de FR-011, sin cambios respecto de la 001 |
| Superposición | Agrupación nativa por cercanía. Un grupo muestra la cuenta; al acercar el zoom se abre |
| Toque | Consulta de lo dibujado en ese punto, filtrando la capa de patentes. Devuelve el id del registro |

### Cámara

| Situación | Comportamiento |
|---|---|
| Abrir con permiso y posición | Centrado en el jugador, a altura donde se leen las cuadras |
| El jugador se mueve | El mapa acompaña |
| El jugador arrastra el mapa | **El seguimiento se apaga.** Sin esto el mapa pelea contra el dedo: llega la siguiente lectura y vuelve solo |
| Toque en el control de recentrado | Vuelve a la posición y reactiva el seguimiento |
| Sin permiso o sin lectura | Encuadra los registros guardados, o dice por qué no puede centrarse. **Nunca vista mundial muda** |

### La regla que manda sobre todas las anteriores

**El mapa no puede demorar la captura** (FR-009, hereda FR-038 de la 001). Si la posición
no llega, si el estilo no carga, si no hay red: el campo de número y los botones ya
respondían y siguen respondiendo. Cada comportamiento de esta sección corre después de que
el estilo cargó y falla en silencio hacia el lado seguro.

**Verificación**: modo avión, app cerrada, abrir y guardar una patente. Tiene que guardarse
igual, con el mapa en cualquier estado.
