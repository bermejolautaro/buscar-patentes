# Phase 1 Contracts: la superficie de interfaz

Esta app no expone API ni CLI: su contrato es lo que el jugador ve y toca. Cinco contratos, en
el orden en que se construyen.

Dos de ellos **reemplazan** contratos de specs anteriores. Está dicho en cada uno: un contrato
que se quedó describiendo lo que se descartó es peor que no tenerlo.

---

## C1 — El interruptor del mapa

**Requisito**: FR-001 a FR-006.

| Regla | Contrato |
|---|---|
| Estados | Tres, en este ciclo: **cobertura → antigüedad → apagado → cobertura** |
| Gesto | Un toque avanza un estado. No hay menú, no hay pulsación larga, no hay Ajustes |
| Ubicación | La franja de acciones que ya existe, junto a Buscar, Salidas y Ajustes (D7) |
| Qué dice | El icono cambia por estado, **y** la línea de C2 nombra el estado con palabras |
| Persistencia | El estado se escribe en `estado_del_juego.modoMapa` al cambiarlo, y se lee al abrir |
| Estado inicial | **Cobertura**, en instalación nueva y al actualizar |
| Qué NO apaga | Las patentes, el punto del jugador, el fondo del mapa. **Solo** el camino |
| Qué NO afecta | El registro del trayecto. El servicio graba igual con el trazo apagado |

**Lo que este contrato prohíbe explícitamente**: que apagar los recorridos toque la grabación, y
que el estado del interruptor viva solo en memoria. Las dos cosas parecen atajos razonables y las
dos rompen un requisito.

**Verificación**: tres toques vuelven al punto de partida. Cerrar y abrir la app conserva el
estado. Con los recorridos apagados, un recorrido en curso sigue sumando puntos, y al prenderlo
aparecen todos.

---

## C2 — La línea de estado y leyenda

**Requisito**: FR-004, FR-014a.

Una sola línea, apoyada arriba de la franja de acciones, **siempre presente**.

| Modo | Contenido |
|---|---|
| Cobertura | Nombra el modo: todo lo caminado, sin distinción |
| Antigüedad | Los **tres** escalones, cada uno con su color y su plazo escrito: hasta 3 días · 4 a 14 días · más de 14 días |
| Apagado | Dice que los recorridos están ocultos |

**Por qué es obligatoria y no un adorno**: con el mapa sin salidas guardadas, "apagado" y "no
caminé nada" se ven idénticos. Sin esta línea el jugador no puede distinguirlos, y el icono del
botón solo no alcanza.

**Lo que este contrato prohíbe**: una leyenda que aparezca solo al cambiar de modo y se
desvanezca. La leyenda hace falta mientras se mira el mapa, que es después del cambio, no
durante.

**Verificación**: los tres modos producen tres textos distintos, y en antigüedad los tres colores
de la línea son los mismos tres que están dibujados en el mapa.

---

## C3 — El trazo del recorrido

**Requisito**: FR-007 a FR-016.

**Reemplaza al contrato C2 de la 003** en todo lo que hablaba de destacar. Lo demás de aquel
contrato sigue vigente y se repite acá para que este documento se lea solo.

| Regla | Contrato |
|---|---|
| Unidad de dibujo | Un `LineString` por **tramo continuo**, agrupados por salida. **Nunca** uno solo con todos los puntos: uniría el final de una salida con el principio de la siguiente |
| Orden de los vértices | El momento en que se registró cada punto |
| Orden de las salidas | **De la más vieja a la más nueva.** Es lo que hace que la reciente quede encima donde se superponen (FR-013) |
| Continuidad | Se corta donde hubo desconexión, y los extremos se unen con línea punteada. **En los tres modos** (FR-010) |
| Camino ajustado | Cuando la salida lo tiene se dibuja ese y no el crudo, y no se corta |
| Destacado | **No hay.** En cobertura todas las salidas se ven igual, incluida la que está en curso |
| Color en cobertura | Uno solo, azul de ruta. Mismo ancho y misma opacidad para todas |
| Color en antigüedad | Tres escalones netos, colores **distintos** entre sí, ninguno el azul de cobertura. El más viejo es el que más resalta |
| Escala | Tres escalones. **Nunca** un gradiente continuo |
| Orden de dibujo | Debajo de los marcadores. Las patentes son el dato, el recorrido es el contexto |
| Filtrado | Ninguno al dibujar: no se diezman puntos, no se suaviza, no se simplifica |

**Lo que este contrato prohíbe explícitamente**: que el hueco punteado de una desconexión se
pinte como calle caminada. Aplanar los colores en modo cobertura no puede convertir "no sé qué
pasó acá" en "pasé por acá". Es la lección que la 003 aprendió en la calle —un corte de señal
"teletransportó" el recorrido hasta la casa del jugador— y no se revisa.

**Verificación**: con tres salidas de fechas distintas, en cobertura las tres se ven idénticas;
en antigüedad caen en escalones distintos. Donde dos salidas pisan la misma calle, gana la
reciente. El hueco punteado sigue punteado en los tres modos.

---

## C4 — El marcador de patente

**Requisito**: FR-028 a FR-032.

**Reemplaza al contrato de marcadores de la 001 y la 002** en lo que hace al color: el relleno
dejaba de decir estado.

| Regla | Contrato |
|---|---|
| Forma | Pin o globo con punta. La punta cae **sobre** la coordenada |
| Relleno | Blanco, en tema claro y oscuro |
| Número | Negro, tres dígitos con ceros a la izquierda, dentro de la cabeza del pin |
| Borde | **La probabilidad**, y nada más. Tres escalones netos: baja, media, alta |
| Estado del registro | **No se dibuja.** Pendiente, ya toca y compartida se ven igual |
| La que toca | Se destaca **sin color**: más grande, y dibujada encima de las que se le superpongan |
| Grupos | Siguen siendo círculos con su cuenta. Los separa la forma, no el color |
| Toque | Sin cambios: abre la ficha del registro |
| Agrupación | Sin cambios de comportamiento. El radio se recalibra al ancho real del pin |
| Legibilidad | El pin tiene que leerse **encima del trazo**, no solo sobre el fondo del mapa: casi todas las patentes están sobre una calle caminada |

**Techo del Principio I, y es parte del contrato**: el interruptor de C1 entra en la franja de
acciones **sin achicar el campo de número**. Si los cuatro botones más el de recentrar no entran
sin comerse ancho del campo, lo que sale de la franja es el interruptor, no el ancho del campo.
El campo es camino de captura; el interruptor no.

**Lo que este contrato prohíbe explícitamente**: recuperar el color de estado "en chiquito" —un
puntito, un borde secundario, un símbolo— cuando alguien lo extrañe. Se retiró porque el jugador
no sabía que existía; devolverlo en miniatura sería lo mismo, más chico.

**Verificación**: una patente compartida y una pendiente se ven idénticas. Tres patentes con
probabilidad baja, media y alta se distinguen por el borde. Con el número actual cargado, su pin
se encuentra sin leer números uno por uno. El número se lee sobre el trazo, en claro y oscuro.

---

## C5 — La pantalla de Salidas

**Requisito**: FR-017 a FR-023.

**Reemplaza al FR-010a de la 003**, que puso un mapa adentro de cada fila de la lista.

### La lista

| Regla | Contrato |
|---|---|
| Fila | Compacta y de **altura pareja**. Sin mapa adentro, sin la lista de patentes adentro |
| Contenido de la fila | Cuándo fue, cuánto duró, cuántas patentes |
| La fecha | Legible como tiempo transcurrido: "hace 3 días" |
| Encabezado | Hace cuánto fue la última salida. **Sin ningún total acumulado** |
| Lista vacía | Sigue diciendo cómo empezar una salida |

### El detalle

| Regla | Contrato |
|---|---|
| Cómo se llega | Tocando una fila. Reemplaza a la lista, no la expande |
| Cómo se vuelve | Botón de volver **y** gesto de retroceso del sistema |
| El mapa | Ocupa el espacio de una pantalla, no 220 dp. Encuadra el camino completo y no sigue al jugador |
| Sin puntos | Lo dice, y no abre un mapa vacío |
| Las patentes | Lista recorrible, cada una lleva a su ficha. **No** una línea de texto separada por comas |
| Borrar | Pide confirmación, **no toca ninguna patente**, y al confirmar vuelve a la lista |

**Lo que este contrato prohíbe explícitamente**: que el detalle sea un destino de navegación par
de las otras pantallas. Es el adentro de Salidas, y sale y entra con el estado de Salidas.

**Verificación**: con diez salidas, la lista entra en menos de dos pantallas de alto. Abrir una
salida muestra su mapa completo. El gesto de retroceso vuelve a la lista, no a la principal ni
fuera de la app.
