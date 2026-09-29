# Phase 1 Contracts: la superficie de interfaz

Esta app no expone API ni CLI: su contrato es lo que el jugador ve y toca. Cinco contratos,
en el orden en que se construyen.

---

## C1 — La paleta

**Requisito**: FR-016, FR-020.

El esquema de color se define **completo**, claro y oscuro. Ningún rol queda sin definir, y
esa es la regla entera del contrato: lo que no se define lo pone Material 3, y su base es
violeta (D6).

| Zona | Contrato |
|---|---|
| Fondos y superficies | Neutros. Blancos y grises en claro, negros y grises en oscuro |
| Contenedores de controles | Neutros, distinguibles del fondo por valor y no por tinte |
| Acento | **Uno solo**, reservado a la acción principal de cargar una patente |
| Colores del mapa | Aparte de la paleta de interfaz, como ya están: pendiente, ya toca, compartida |
| Trazo del recorrido | Dos tratamientos: el destacado y el resto del histórico |

**Lo que este contrato prohíbe explícitamente**: definir un rol nuevo "a ojo" cuando aparezca
un componente que se ve mal. Si un componente sale con un color raro, el rol que usa está sin
definir, y el arreglo es definirlo en el esquema — no pisar el color en el componente.

**Verificación**: no queda ningún violeta en pantalla que nadie haya escrito.

---

## C2 — El trazo del recorrido

**Requisito**: FR-006, FR-006a, FR-007, FR-007a, FR-009, FR-010.

| Regla | Contrato |
|---|---|
| Unidad de dibujo | Un `LineString` por **tramo continuo**, agrupados por salida. **Nunca** uno solo con todos los puntos: uniría el final de una salida con el principio de la siguiente |
| Orden de los vértices | El momento en que se registró cada punto |
| Continuidad | **Se corta donde hubo una desconexión** y los dos extremos se unen con línea punteada (FR-009, FR-009a) |
| Camino ajustado | Cuando la salida tiene camino ajustado a las calles se dibuja ese y **no** el crudo, y no se corta: el servicio ya contestó qué pasó en el medio (FR-031, FR-033) |
| Destacado | El recorrido `EN_CURSO`; si no hay ninguno, el más reciente. Se distingue del resto por color y grosor |
| Orden de dibujo | Debajo de los marcadores de patentes. Las patentes son el dato, el recorrido es el contexto |
| Filtrado | Al dibujar, ninguno: no se diezman puntos, no se suaviza, no se simplifica (D9). El descarte por precisión ocurre antes, al grabar (FR-036) |

**Reescrito después de la segunda y la tercera salida.** La versión original de este contrato
decía "sin cortes: un tramo sin señal se dibuja como recta y se acepta", y prohibía pegar el
trazo a las calles. Las dos cosas se revirtieron en la spec —FR-009 y FR-009a la primera,
FR-031 la segunda— y el código las siguió; el contrato se había quedado describiendo lo que se
descartó.

**Excepción deliberada**: no hay leyenda. Cuál es el recorrido de hoy se entiende porque es el
que se está dibujando mientras el jugador camina, no porque un cartel lo explique.

---

## C3 — El mapa de una salida

**Requisito**: FR-010a, FR-010b.

El mismo componente de mapa, con otros datos. No es una pantalla nueva: vive en la fila que la
pantalla de Salidas **ya expande**.

| Aspecto | Contrato |
|---|---|
| Qué dibuja | **Solo** el trazo de esa salida, y las patentes capturadas durante ella |
| Encuadre | Al abrirse, el trazo entero visible sin que el jugador desplace ni haga zoom |
| Alto | Fijo. No compite con la lista de salidas por el alto de la pantalla |
| Sin puntos | Se dice que no hay camino que mostrar. **No se abre un mapa vacío** (FR-010b) |
| Seguimiento del jugador | Apagado. Acá se mira dónde se estuvo, no dónde se está |

---

## C4 — La ficha: distancia, rumbo e ir hasta la patente

**Requisito**: FR-001, FR-001a, FR-002, FR-003, FR-003a, FR-004.

| Elemento | Contrato |
|---|---|
| Distancia y rumbo | **Siempre visibles**, junto a los datos del registro. No son un respaldo que aparece al fallar algo |
| Formato | Distancia y punto cardinal, legibles de un vistazo: "a 320 m al noreste" |
| Sin posición actual | "Distancia desconocida", explicando que falta saber dónde está el jugador. **Nunca un número inventado** (FR-003a) |
| Precisión degradada | Se advierte **antes** de mandar al jugador, no después de que caminó (FR-004) |
| Acción de ir | Entrega el destino a la app de mapas del teléfono. Esta app no calcula rutas |
| Sin app de mapas | Se avisa. No hace falta respaldo: la distancia y el rumbo ya estaban en pantalla (FR-001a) |
| Dónde aparece | En la ficha del mapa **y** en los resultados de búsqueda (FR-002) |

**Lo que este contrato prohíbe**: guardar la distancia. Depende de dónde está parado el
jugador ahora, y persistirla metería en la evidencia un dato de otro día.

---

## C5 — Buscar

**Requisito**: FR-011, FR-012, FR-013, FR-014.

| Estado de la pantalla | Contrato |
|---|---|
| Recién abierta, sin escribir | Muestra la patente del número actual del juego si la hay, **y dice que falta si no la hay**. Debajo, las capturas más recientes |
| Un dígito escrito | Ya acota lo que muestra. No espera al tercero |
| Sin ninguna captura todavía | Se dice. No hay lista muda |
| Campo de entrada | Al alcance del pulgar con una sola mano, igual que la franja de la pantalla principal |

**Lo que cambia respecto de hoy**: la pantalla deja de ser un filtro que exige tres dígitos
para contestar algo, y pasa a contestar la pregunta que el jugador trae —"¿tengo la que
toca?"— antes de que escriba.

---

## C6 — Insets y barras del sistema

**Requisito**: FR-015, y continuidad con el C2 de la 002.

| Zona | Contrato |
|---|---|
| Barra de navegación del sistema | Sus controles **MUST** contrastar contra el fondo de la app, en los dos modos, y ajustarse solo al cambiar de modo |
| Barra de estado y muesca | Ningún control ni texto debajo, como ya fijó la 002 |
| Teclado | Los controles se corren, no se tapan |
| Mapa de fondo | Excepción deliberada, sigue yendo a sangre |
| Mapa de una salida | Va adentro de una fila con `safeDrawingPadding` ya aplicado, así que hereda el contrato |

**Por qué vuelve un contrato que la 002 ya tenía**: porque su C2 cubría los insets —dónde cae
el contenido— y no el **tinte de los iconos del sistema**, que es un asunto distinto y el que
efectivamente falló en la calle.
