# Quickstart: validar el planeamiento de recorridos

**Fecha**: 2026-08-30
**Spec**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Contracts**: [contracts/mapa-y-salidas.md](./contracts/mapa-y-salidas.md)

Casi todo lo de esta feature se juzga mirando. Una sola cosa se prueba en JVM —los escalones de
antigüedad— y el resto es un mapa que hay que ver, de día, en el teléfono, con salidas de verdad
guardadas.

---

## Prerrequisitos

- Los mismos de la [001](../001-captura-patentes/quickstart.md): JDK 17, SDK de Android, un
  teléfono físico.
- **Al menos tres salidas guardadas en fechas separadas**, y que al menos dos pisen alguna calle
  en común. Sin eso los modos no se pueden juzgar: con una sola salida, cobertura y antigüedad se
  ven igual.
  - Si las salidas guardadas son todas de esta semana, para probar el escalón viejo se puede
    correr el reloj del teléfono hacia adelante unos días. No hace falta esperar dos semanas.
- **Al menos tres patentes guardadas** con probabilidades distintas: una con votos positivos, una
  sin ningún voto y una con votos negativos. Es lo que hace visible el borde del pin.
- **Una patente cuyo número sea el actual del juego**, para juzgar el destacado por tamaño.

```bash
./gradlew test           # incluye AntiguedadTest, la única prueba nueva
./gradlew assembleDebug
```

En el teléfono de pruebas —HyperOS sin SIM— `installDebug` está bloqueado por el sistema. El
camino es `adb push` del APK a `/sdcard/Download/` e instalarlo a mano desde el gestor de
archivos.

**Antes de instalar sobre la app que ya está**: correr `scripts/respaldo.ps1`. Esta feature trae
una migración de base (v5 → v6) y, aunque es aditiva, es la primera vez que corre contra datos
reales.

---

## 1. La migración no perdió nada

Es lo primero porque si falla, lo demás no importa.

| Paso | Resultado esperado |
|---|---|
| Instalar sobre la versión anterior, sin desinstalar | La app abre sin cerrarse |
| Mirar el mapa | Están **todas** las patentes que había antes |
| Entrar a Salidas | Están **todas** las salidas que había antes |
| Mirar el modo del mapa al abrir | **Cobertura**. La app actualizada arranca ahí |

Si la app se cierra al abrir, la migración falló y hay que mirarla antes de seguir: nada de lo
que viene tiene sentido con la base rota.

---

## 2. El interruptor (C1, C2)

| Paso | Resultado esperado |
|---|---|
| Tocar el interruptor una vez | Pasa a antigüedad. La línea de leyenda cambia y muestra los tres plazos |
| Tocarlo otra vez | Los recorridos desaparecen del mapa. La línea dice que están ocultos |
| Mirar el mapa con los recorridos apagados | Las patentes siguen ahí. El punto del jugador sigue ahí |
| Tocarlo una tercera vez | Vuelve a cobertura. Tres toques cerraron el ciclo |
| Cerrar la app y volver a abrirla | Está en el modo en que se la dejó |
| Con el trazo apagado, empezar un recorrido y caminar una cuadra | Al prender el trazo, esa cuadra está dibujada: apagar el dibujo no apagó la grabación |

**El caso que hay que probar y es fácil saltearse**: con los recorridos apagados, abrir la app en
una zona donde no hay ninguna salida guardada. La línea de leyenda tiene que decir que están
ocultos. Si dijera cualquier otra cosa, no habría forma de distinguir "apagado" de "no caminé
nada".

---

## 3. Modo cobertura (C3)

| Paso | Resultado esperado |
|---|---|
| Mirar el mapa con tres salidas guardadas | Las tres se ven **idénticas**: mismo azul, mismo ancho, misma opacidad |
| Buscar la salida de hoy | No se distingue de las demás. Eso es correcto, no un defecto |
| Alejar el mapa hasta ver el barrio entero | Lo que salta a la vista es **lo que no está pintado** |
| Empezar un recorrido y caminar | El trazo crece en vivo, con el mismo aspecto que el resto |
| Buscar un tramo donde hubo corte de señal | Sigue punteado, y se distingue de una calle caminada |

**La pregunta que decide si la feature sirvió**: mirando el mapa, ¿podés señalar en menos de
cinco segundos una calle a la que no fuiste? Si hay que acercar o comparar tonos, el contraste
del azul no alcanza (SC-001).

---

## 4. Modo antigüedad (C3)

| Paso | Resultado esperado |
|---|---|
| Pasar a antigüedad | Las salidas se separan en tres colores |
| Comparar la de hoy con una de hace un mes | La vieja es la que **resalta**; la reciente se corre al fondo |
| Comparar dos salidas de hace 5 y de hace 12 días | Se ven **iguales**: caen en el mismo escalón |
| Mirar una calle que pisaron dos salidas de fechas distintas | Se ve con el color de la **más reciente** |
| Leer la línea de leyenda | Dice los tres plazos con los mismos tres colores que están en el mapa |
| Comparar un trazo de antigüedad con uno de cobertura | Ningún color de la escala es el azul de cobertura |

**El caso de D5 que hay que mirar de cerca**: la calle compartida por dos salidas. Si se ve con
el color de la salida vieja en vez de la reciente, el orden de dibujo no está funcionando y
corresponde el plan B del research —una capa por escalón—.

---

## 5. Los marcadores nuevos (C4)

| Paso | Resultado esperado |
|---|---|
| Mirar cualquier patente | Es un pin blanco con el número en negro, y la punta cae sobre el lugar |
| Mirar un pin que está **encima** de un trazo | El número se lee igual de bien |
| Cambiar el teléfono a tema oscuro | El número se sigue leyendo |
| Comparar las tres probabilidades | Se distinguen por el borde |
| Comparar una patente compartida con una pendiente | Se ven **iguales**. Eso es el requisito, no un defecto |
| Buscar la patente del número actual | Es más grande, y si se superpone con otra queda encima |
| Acercar hasta que se separe un grupo | El grupo sigue siendo un círculo con su cuenta, y se abre como antes |

**Al sol, no adentro.** Los tres colores del borde se eligieron para leerse caminando; el juicio
que vale es el de la vereda.

---

## 6. La pantalla de Salidas (C5)

| Paso | Resultado esperado |
|---|---|
| Entrar a Salidas con diez guardadas | Entra en menos de dos pantallas de alto |
| Mirar sin tocar nada | Se lee hace cuánto fue la última salida |
| Buscar un total de kilómetros o de calles | **No hay ninguno.** Eso es el requisito |
| Mirar una fila | Cuándo fue —como "hace 3 días"—, cuánto duró, cuántas patentes. Sin mapa adentro |
| Tocar una fila | Se abre el detalle **en lugar** de la lista, con el mapa a pantalla |
| Mirar el mapa del detalle | Encuadra el camino completo, sin seguir al jugador |
| Tocar una patente del detalle | Abre su ficha |
| Hacer el gesto de retroceso | Vuelve a la lista, no a la pantalla principal ni fuera de la app |
| Abrir una salida sin puntos registrados | Dice que no hay camino que mostrar, sin mapa vacío |
| Borrar una salida desde el detalle | Pide confirmación, vuelve a la lista, y **las patentes de esa salida siguen en el mapa** |

---

## 6b. Esconder las patentes (C1)

| Paso | Resultado esperado |
|---|---|
| Tocar el botón del pin, al lado del interruptor de recorridos | Los marcadores desaparecen y el trazo queda igual |
| Mirar la leyenda | Dice "Patentes ocultas" |
| Volver a tocarlo | Los pines vuelven al instante, sin recargar el mapa |
| Esconder las patentes y apagar además los recorridos | El mapa queda vacío y la leyenda dice **las dos** cosas |
| Con las patentes escondidas, cargar una patente con `+` | Se guarda —el aviso lo confirma— y **no** aparece ningún pin |
| Esconderlas, cerrar la app por completo y volver a abrirla | Las patentes están a la vista otra vez. Eso es el requisito (FR-035a), no un olvido |
| Con las patentes escondidas, pasar cerca de una pendiente | El aviso llega igual |

---

## 6c. La misma patente dos veces (US6)

Este bloque necesita estar parado en la calle, al lado de un auto ya anotado.

| Paso | Resultado esperado |
|---|---|
| Cargar un número que ya está anotado ahí mismo | Dice "ya estaba acá" con la confianza, **no** "Guardada" |
| Mirar el mapa | Sigue habiendo **un** pin, no dos encimados |
| Abrir su ficha | La confianza subió uno respecto de antes |
| Mirar la fecha y la ubicación de esa ficha | Son las de la **primera** vez, no las de recién |
| Cargar ese mismo número a un par de cuadras | Se crea un registro nuevo: son dos autos |
| Cargar con foto sobre una patente confirmada que no tenía foto | La foto queda adjunta al registro que ya existía |
| Cargar con foto sobre una que **sí** tenía | La foto vieja se conserva; la nueva se descarta |

**Calibración.** Si aparecen pines duplicados a pocos metros, el radio de diez metros quedó corto
para el GPS del barrio: es un solo valor, `Captura.RADIO_MISMA_PATENTE_M`. Si dos autos distintos
de la misma cuadra se fusionan, quedó largo. Se decide caminando, no en el escritorio.

---

## 7. Lo que no puede haberse roto

La feature toca el mapa, que es la superficie donde vive casi todo lo demás.

| Paso | Resultado esperado |
|---|---|
| Cargar una patente con `+` | Cuatro interacciones desde la app cerrada, como antes |
| Mirar el ancho del campo de número | No se achicó por el botón nuevo |
| Pasar cerca de una patente pendiente con avisos activos | El aviso llega igual |
| Pasar cerca de una patente **ya compartida** | **No** llega aviso: la columna `estado` sigue haciendo su trabajo |
| Mirar el indicador de "tengo la que toca" | Sigue sin contar las compartidas |
| Poner el teléfono en modo avión y abrir el mapa | Los tres modos dibujan igual: nada de esto usa red |

---

## Criterios de aceptación

La feature está lista cuando:

- [ ] `./gradlew test` pasa, con `AntiguedadTest` cubriendo los dos bordes (3 y 14 días), la
      salida en curso y la fecha futura.
- [ ] La migración v5 → v6 corrió sobre la base real sin perder ni una patente ni una salida.
- [ ] Los tres modos se recorren con tres toques y el modo sobrevive a cerrar la app.
- [ ] En cobertura, una calle no caminada se señala en menos de cinco segundos.
- [ ] En antigüedad, una calle pisada por dos salidas muestra el color de la reciente.
- [ ] El número de un pin se lee encima del trazo, en claro y en oscuro, al sol.
- [ ] La lista de Salidas con diez entradas entra en menos de dos pantallas.
- [ ] La carga rápida sigue entrando en cuatro interacciones y el campo no se achicó.
- [ ] Un toque esconde las patentes y otro las trae, y al reabrir la app están a la vista.
- [ ] Cargar dos veces el mismo número sin moverse deja un solo pin y sube la confianza.
- [ ] Una patente confirmada conserva la ubicación y la fecha de la primera vez.
