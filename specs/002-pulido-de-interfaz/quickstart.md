# Quickstart: validar el pulido de interfaz

**Fecha**: 2026-08-29
**Spec**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Contracts**: [contracts/ui-y-mapa.md](./contracts/ui-y-mapa.md)

Casi todo acá se valida mirando. Es una feature de interfaz: no hay lógica pura nueva que
probar en JVM, y la constitución descartó la suite de interfaz en la 001.

---

## Prerrequisitos

- Los mismos de la [001](../001-captura-patentes/quickstart.md): JDK 17, SDK de Android, un
  teléfono físico.
- Al menos **tres registros guardados con números distintos**, y al menos uno compartido.
  Sin eso no se puede ver nada de la User Story 3.

```bash
./gradlew installDebug
./gradlew test                  # sigue en verde: esta feature no agrega pruebas
```

En el dispositivo de referencia —Xiaomi con HyperOS sin SIM— `installDebug` está bloqueado
por el sistema. Ahí el camino es `adb push` del APK a `/sdcard/Download/` e instalarlo a
mano desde el gestor de archivos.

---

## Paso 0 — Medir contra qué se está trabajando

No es opcional, y la razón está en D2: la primera versión de ese diagnóstico fue falsa por
suponer qué versión tenía instalada el usuario en vez de mirarlo.

1. Confirmar que el APK instalado es el último:

```bash
adb shell dumpsys package ar.lauta.buscarpatentes | grep -E "firstInstallTime|lastUpdateTime"
adb shell ls -la /sdcard/Download/*.apk
```

Comparar el tamaño del instalado con el del archivo. Si `firstInstallTime` es igual a
`lastUpdateTime`, fue instalación limpia y **la base está vacía**.

2. Abrir la app en la calle y mirar el mapa.

**Esperado**: arranca centrado en tu posición con el punto azul, no en vista mundial. T084
ya construyó esa parte, así que si funciona, la User Story 2 se reduce al control de
recentrado, al comportamiento al arrastrar y al caso sin permiso.

3. Cargar tres registros de prueba con números distintos y compartir uno.

Sin esto no se puede validar nada de la User Story 3, y tras una reinstalación limpia no
queda ninguno.

Anotar qué se vio antes de seguir.

---

## US1 — Las pantallas se leen (P1)

1. Poner el teléfono en **modo oscuro**.
2. Entrar a Buscar, a Salidas y a Ajustes.

**Esperado**: todo el texto se lee. Ningún texto casi negro sobre fondo oscuro, que es el
síntoma exacto del bug de D1.

3. Poner el teléfono en **modo claro** y repetir.

**Esperado**: se ven bien y de forma coherente con el modo oscuro.

4. Con la app abierta, cambiar el modo del sistema y volver a la app.

**Esperado**: acompaña el cambio. Si una parte queda clara y otra oscura, alguna de las tres
capas de C1 no está siguiendo al sistema.

5. Mirar el mapa en los dos modos.

**Esperado**: los tres colores de marcador —pendiente, ya toca, compartida— se distinguen
en ambos.

---

## US2 — El mapa abre donde estoy (P2)

1. Abrir la app en la calle.

**Esperado**: centrado en tu posición, a altura donde se leen las cuadras, sin tocar nada.

2. Caminar media cuadra.

**Esperado**: el mapa acompaña.

3. **Arrastrar el mapa** a otra zona y esperar unos segundos quieto.

**Esperado**: el mapa **se queda donde lo dejaste**. Si vuelve solo a tu posición, el
seguimiento no se apagó al arrastrar y el mapa está peleando contra el dedo (C4).

4. Tocar el control de recentrado.

**Esperado**: vuelve a tu posición y retoma el seguimiento.

5. **Caso sin permiso**: quitar el permiso de ubicación en Ajustes del sistema y abrir la
   app.

**Esperado**: el mapa encuadra tus registros guardados o explica por qué no puede
centrarse. Nunca vista mundial muda.

---

## US3 — Tocar una patente (P3)

1. Mirar el mapa con tus tres registros a la vista.

**Esperado**: cada marcador muestra su número de 3 dígitos, con ceros a la izquierda. Se
sigue distinguiendo cuál está compartido y cuál ya toca.

2. Alejar el zoom hasta que los marcadores se junten.

**Esperado**: se agrupan mostrando la cuenta, no un amontonamiento ilegible. Al acercar, se
abren.

3. Tocar un marcador.

**Esperado**: sube la ficha con número, momento, precisión, si tiene foto y su estado. El
mapa sigue visible detrás. **Una sola interacción** desde el mapa (SC-006).

4. Mirar la ficha con atención.

**Esperado**: la ubicación y la hora se muestran **como dato, no como campos**. Si parecen
editables aunque estén bloqueadas, está mal: insinúan algo que el Principio II prohíbe.

5. Corregir el número de un registro.

**Esperado**: el número cambia en la ficha y en el marcador del mapa. Y —esto es lo que hay
que mirar— si el número corregido es el actual del juego, la pantalla principal pasa a decir
que lo tenés, y se reconcilian los avisos.

6. **La verificación que importa** (SC-007): antes de corregir, anotar la ubicación y la
   hora del registro. Después de corregir, volver a mirarlas.

**Esperado**: idénticas. Si cambió una coma, el Principio II está roto.

7. Borrar un registro desde la ficha.

**Esperado**: pide confirmación, y al confirmar el marcador desaparece del mapa.

---

## US4 — El teclado aparece cuando lo pido (P4)

1. Cerrar la app por completo y abrirla.

**Esperado**: el mapa se ve completo. Sin teclado encima.

2. Tocar el campo de número, tipear tres dígitos, tocar `+`.

**Esperado**: se guarda igual que siempre.

3. Contar las interacciones del paso completo, desde la app cerrada.

**Esperado**: **4 o menos** — abrir, tocar el campo, tipear, confirmar. Ese es el techo que
fijó la constitución 1.1.0, y la enmienda dice explícitamente que no es un punto de partida:
si sale 5, algo se agregó al camino de captura que no corresponde.

4. Cronometrar el paso 2 desde la app cerrada.

**Esperado**: 10 segundos o menos (SC-009). El toque extra no puede comerse el presupuesto
de tiempo.

---

## US5 — Se ve como una app terminada (P5)

1. Mirar la fila de botones de la pantalla principal.

**Esperado**: iconos reales. Ningún emoji.

2. Arrastrar el mapa hasta que debajo de los controles haya, sucesivamente, calles densas,
   una zona clara y una zona sin fondo guardado.

**Esperado**: los controles se siguen distinguiendo en los tres casos.

3. Mostrarle la pantalla principal a alguien que nunca usó la app y preguntarle cuál es el
   botón para anotar una patente.

**Esperado**: acierta sin ayuda (SC-010).

---

## Regresión: lo que no se puede haber roto

Esta feature toca las cinco pantallas, así que conviene repasar lo que la 001 dejó
funcionando:

| Verificar | Requisito |
|---|---|
| Guardar en modo avión sigue funcionando | FR-007 de la 001, SC-003 |
| El mapa en cualquier estado no demora el guardado | FR-009, hereda FR-038 |
| El indicador sigue diciendo si tenés el número actual y cuántos consecutivos | FR-025 |
| Compartir sigue marcando el registro como compartida | FR-015 de la 001 |
| El aviso de proximidad sigue llegando con la app cerrada | FR-028 de la 001 |

Las dos últimas se prueban caminando, igual que en la 001.
