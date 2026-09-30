# Research: Zona a recorrer

Las mediciones de este documento se hicieron con dos zonas públicas del centro, alrededor del
Obelisco, y con la respuesta real del servicio de ajuste que ya usan las pruebas de la 007
(`RespuestaDeAjuste.kt`). Nada de la zona de juego del jugador.

## D1 — De dónde salen las cuadras

**Decisión**: de OpenStreetMap, con un pedido a **Overpass** (`overpass-api.de`, libre y sin
clave), una sola vez por zona. Se piden las vías del rectángulo que envuelve el borde de la zona
más un margen de 200 m, con su geometría completa (`out geom`). Qué vías y con qué filtro está en
el contrato Z1 ([contracts/cuadras.md](./contracts/cuadras.md)).

**Por qué**:
- Es el mismo mapa de calles sobre el que trabaja el servicio de ajuste: una cuadra recorrida y el
  camino que la recorre comparten los mismos nodos, y la comparación del D5 da 100% sin
  calibraciones raras.
- Trae los nodos de cada vía. Con eso las esquinas se encuentran exactas: dos vías que comparten
  un nodo se cruzan ahí (D2).
- Anda con el `postear` que ya existe: acepta la consulta como cuerpo del POST, con el
  `Content-Type: application/json` que manda la app. Probado: responde 200.

**Medido**:

| Zona de prueba | Superficie | Cuadras | Respuesta | Tiempo |
|---|---|---|---|---|
| Unas 60 manzanas | 0,8 km² | 141 | 122 KB | 2,7 s |
| Unas 150 manzanas | 1,7 km² | 280 | 235 KB | < 3 s |
| El máximo del FR-004 | 13,7 km² | 2.004 | 1,3 MB | 5,9 s |

El máximo entra holgado en los 20 s de espera de `postear`.

**El margen de 200 m** existe para las cuadras del borde. Una cuadra que cruza el borde de la zona
termina en una esquina que puede quedar afuera; si la calle que la corta no vino en la respuesta,
la cuadra se estira hasta la esquina siguiente.

**Alternativas descartadas**:
- **Las teselas vectoriales del mapa de fondo** (OpenFreeMap). Ya están en el teléfono, pero vienen
  simplificadas, cortadas en el borde de cada tesela y sin nodos: las esquinas habría que
  adivinarlas cruzando geometrías.
- **El servicio de ajuste (Valhalla)**. No tiene ningún pedido que devuelva las calles de un
  área.
- **Un extracto de OpenStreetMap dentro de la app**. Son cientos de MB para usar unos pocos KB.

**Un cuidado**: Overpass rechaza con un 406 los pedidos que llegan con la identificación genérica
de una biblioteca: pasó con la de Python. `postear` pasa a mandar un `User-Agent` propio,
`buscar-patentes`, en los dos teléfonos. Es además lo que piden las reglas de uso de Overpass y
del servidor de Valhalla.

## D2 — Qué es una cuadra

**Decisión**: una cuadra es un tramo de calle **entre dos nodos que no tienen grado 2**. El grado de
un nodo es cuántos segmentos de las vías pedidas lo tocan. Un nodo de grado 2 es un punto del
medio de una calle, o el lugar donde el mapa partió una calle en dos vías por un cambio de etiqueta:
ahí la cuadra sigue. Grado 3 o más es una esquina; grado 1, el final de una calle sin salida.

Se descartan las cuadras de **menos de 30 m**. Son los pedacitos que quedan en las rotondas y en el
cantero central de las avenidas de doble mano: nadie los caminaría como una cuadra, y quedarían
pendientes para siempre.

**Por qué**:
- Coincide con lo que el jugador llama cuadra, también donde el mapa parte una calle en varias vías.
- Solo cuentan las esquinas **entre las vías pedidas**. Un sendero de plaza o una entrada de garaje
  que toca la calle no la corta.

**Medido** en la zona de 280 cuadras:
- la mediana da 128 m;
- 10 cuadras miden menos de 30 m;
- ninguna cuadra quedó sin nombre.

Dibujadas sobre el plano, las cuadras se parten en cada esquina y en ningún otro lado.

`ponytail:` el corte de 30 m es fijo. Si una cuadra real y corta queda afuera, se baja el número.

## D3 — Las avenidas de doble mano

**Decisión**: dos cuadras son **gemelas**, y cuentan como una sola, si cumplen todo esto:
- las dos son de una sola mano y tienen el mismo nombre;
- no comparten ninguna punta;
- cada una tiene al menos el 60% de sus muestras a entre 5 y 40 m de la otra, y ese punto más
  cercano no es una punta de la otra.

Se guarda en la cuadra que no es la principal (`gemelaDe`). Quitar una quita las dos. La pareja
está recorrida si lo está cualquiera de las dos.

**Por qué**: el mapa dibuja una avenida con bulevar como dos vías paralelas, una por mano. El
servicio de ajuste pega la caminata a la más cercana, que puede ser cualquiera de las dos según la
vereda. Sin esta regla, caminar una avenida entera deja la otra mano pendiente.

**Medido** en la zona de 2.004 cuadras:
- aparecen 10 pares, todos en avenidas con bulevar o en las calles laterales de una avenida ancha;
- ninguno junta calles distintas: el nombre igual lo impide;
- ninguno junta dos cuadras seguidas de la misma calle: la condición de la punta lo impide.

## D4 — Qué cuadras son de la zona

**Decisión**: se muestrea cada cuadra cada 5 m. La cuadra es de la zona si la mitad de sus
muestras, o más, caen adentro del borde. Para saber si una muestra cae adentro se usa el método
del rayo: se cuentan los cruces con el borde.

Se hace una vez, al crear la zona. Lo que se guarda son solo las cuadras de la zona.

## D5 — Cuándo una cuadra está recorrida

**Decisión**: se muestrea la cuadra cada 5 m. Una muestra está **cubierta** si queda a 10 m o menos
de algún tramo ajustado de alguna salida que cuenta. La cuadra está **recorrida** si el 80% de sus
muestras, o más, están cubiertas. Las salidas se suman: si un día se caminó media cuadra desde un
lado y otro día la otra media, la cuadra queda recorrida.

**Por qué**:
- El camino ajustado de la 007 va sobre los mismos nodos que la cuadra (D1). Caminar una cuadra de
  esquina a esquina la cubre entera.
- Cruzarla en la esquina cubre solo los primeros 10 m.

**Medido** con la respuesta real de la prueba de la 007 contra las 280 cuadras de su zona:

| Cómo se pasó por la cuadra | Cuántas | Cubierto |
|---|---|---|
| Caminada de esquina a esquina | 11 | 100% todas |
| Cruzada en una esquina | 28 | de 4% a 14% |
| Una parte: el arranque, la rotonda, un desvío por adentro de una manzana | 5 | de 20% a 68% |

El 80% separa limpio las caminadas de las cruzadas, con margen para los dos lados.

**Qué salidas cuentan**:
- las terminadas;
- con camino ajustado (`CaminoGuardado.Ajustado`);
- empezadas en "desde cuándo cuenta" o después.

Un camino pendiente o que no se pudo ajustar no suma nada.

**Velocidad**: el máximo es de 2.000 cuadras, unas 50.000 muestras. Contra cada tramo de cada salida
son millones de cuentas, así que no se compara todo contra todo. Los segmentos de los caminos se
reparten en una grilla de celdas de 50 m, y cada muestra mira solo su celda y las vecinas. Las
salidas cuyo rectángulo no toca el de la zona ni se leen.

`ponytail:` los 10 m y el 80% son constantes. Si una salida real deja afuera una cuadra caminada,
se calibran ahí.

## D6 — Cuándo se completa, y con qué fecha

**Decisión**: cada muestra guarda la fecha de inicio de la **primera** salida que la cubrió. Así
cada cuadra tiene una fecha de recorrida: la del momento en que llegó al 80% cubierto, que es el
percentil 80 de las fechas de sus muestras. La zona está completa cuando están recorridas todas
las cuadras que cuentan, y la fecha de completada es la más tardía de esas fechas.

**Por qué**:
- En el caso normal es exactamente "la salida que la completó" (FR-014).
- Si la zona se completa al quitar la última cuadra pendiente, la fecha es la de la última cuadra
  recorrida y no la del día en que se tocó un botón.
- Sale en la misma pasada que el porcentaje, sin ir sumando salida por salida.

## D7 — Qué se guarda y qué se calcula

**Decisión**:
- **Se guarda** lo que no se puede recalcular: el borde, las cuadras con su forma, lo quitado, las
  fechas, el estado de la zona y, cuando la zona termina, su resultado. Las tablas son `zona` y
  `cuadra`, y el esquema completo está en [data-model.md](./data-model.md).
- **Se calcula**, cada vez que se muestra, qué cuadras están recorridas en una zona activa. Es una
  función pura (D5): mismas cuadras y mismas salidas dan lo mismo. Borrar una salida o cambiar
  "desde cuándo cuenta" se refleja solo, sin nada que mantener sincronizado.
- **Se congela** al terminar la zona, por completarla o por cerrarla. Cada cuadra guarda si estaba
  recorrida y la zona guarda su porcentaje y su fecha. Desde ahí la zona ya no mira las salidas
  (FR-016).

**Quién congela**:
- `Zonas.revisar()` recalcula las zonas activas y completa las que llegaron al 100%.
- Corre cada vez que el ajuste a calles termina de trabajar: al abrir la pantalla principal, en
  el mismo `LaunchedEffect` que ya llama a `AjustarACalles.pendientes()`. Es el único momento en
  que llegan caminos nuevos.
- También corre al quitar una cuadra.

**Base**:
- La base pasa a la versión 8, con la migración 7→8 escrita en común, como dejó dicho la 006 en
  `ConstruirBase.ios.kt`.
- El Android la suma a sus migraciones. El iPhone la usa al abrir un respaldo de la 7.
- El respaldo no cambia de formato: `VACUUM INTO` copia todas las tablas, así que las zonas viajan
  solas. Solo sube `Respaldo.VERSION_BASE` a 8.
- Un respaldo de la 7 se restaura igual. Room lo migra al abrirlo, y queda sin zonas.

**Alternativa descartada**: guardar en cada cuadra si está recorrida e irlo actualizando. Obliga a
recalcular en cada cambio de salida, de fecha o de cuadra quitada, y a no olvidarse ninguno. La
cuenta en vivo cuesta milisegundos.

## D8 — La búsqueda de cuadras, con y sin conexión

**Decisión**: la misma forma de trabajo que el ajuste a calles. Una zona sin cuadras está en
estado `BUSCANDO`. `BuscarCuadras.pendientes()` recorre esas zonas y, con conexión, las busca.
Corre al crear la zona y cada vez que abre la pantalla principal.

**Qué pasa con cada resultado**:

| Resultado | Qué pasa |
|---|---|
| Sin conexión, o el servicio no responde | La zona queda en `BUSCANDO` y se reintenta en la próxima apertura |
| Ninguna cuadra, o más de 2.000 | La zona guarda el problema, "Adentro no hay calles" o "Tiene más de 2.000 cuadras", y solo se puede borrar |
| Entre 1 y 2.000 cuadras | Se guardan las cuadras y la zona pasa a `ACTIVA` |

Con conexión, el jugador está mirando la pantalla mientras se busca: el problema lo ve en el
momento, que es lo que pide el borde de la spec ("no la crea"). Sin conexión, lo ve cuando vuelve
a abrir la zona.

## D9 — Dibujar el borde y tocar una cuadra

**Decisión**: el mismo `MapaDeFondo` de la pantalla principal y del detalle de una salida, con tres
parámetros más:
- `zonas`: lo que se dibuja de cada zona, es decir el borde y las cuadras, cada una con su clase;
- `onTocarMapa`: un toque que no cayó sobre nada, para marcar una esquina;
- `onTocarCuadra`: un toque sobre una cuadra, con su id.

**Cómo se hace en maplibre-compose 0.18**:
- **El toque suelto** va por `MapInteractions`, con el `click { onUnhandled { … } }` que entrega el
  `Position` del toque. Solo llega si ninguna capa lo consumió, así que no choca con los pines.
- **El toque sobre una cuadra** es el `onClick` de la `LineLayer` de las cuadras, igual que los
  pines de las patentes (`FeaturesClickHandler`), leyendo el id de las propiedades del feature.

**Por qué**: la cámara, la ubicación, el fondo sin conexión y el tema oscuro ya viven ahí. Una
tercera pantalla con otro mapa duplicaría todo eso.

**En la pantalla principal**:
- cada zona activa muestra su borde punteado y sus cuadras pendientes, en los modos cobertura y
  antigüedad;
- con los recorridos apagados, tampoco se ven las zonas (FR-010);
- las cuadras pendientes van debajo de los trazos y con un color propio, `ColoresDeMapa.PENDIENTE`,
  que no es el azul de los recorridos.

`ponytail:` el color se calibra mirando el teléfono al sol y de noche, como las avenidas de la 006.

## D10 — Fechas y duraciones

**Decisión**:
- **Guardado**: "desde cuándo cuenta" se guarda como el instante del comienzo de ese día en la hora
  del teléfono. Se compara directo con el `iniciadoEn` de cada salida.
- **Elección**: con el `DatePicker` de Material 3, que no deja elegir días posteriores a hoy.
- **La duración** se escribe en días hasta los 30 días, por ejemplo "23 días". Desde ahí, en meses y
  días, por ejemplo "2 meses y 4 días". Vive en `Formatos`, con su prueba, igual que las otras
  fechas de la 006.
