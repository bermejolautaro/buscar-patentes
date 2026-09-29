# Quickstart: validar rumbo y aspecto

**Fecha**: 2026-08-29
**Spec**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Contracts**: [contracts/ui-y-mapa.md](./contracts/ui-y-mapa.md)

Esta es la primera feature que salió de usar la app caminando, así que su validación también
se camina. Dos cosas se prueban en JVM —la geometría nueva y que la grilla se fue de verdad—;
todo lo demás se mira, y la mitad se mira en la calle.

---

## Prerrequisitos

- Los mismos de la [001](../001-captura-patentes/quickstart.md): JDK 17, SDK de Android, un
  teléfono físico.
- **Al menos una salida grabada con recorrido real**, de diez minutos o más, por calles
  conocidas. Sin eso la User Story 2 no se puede juzgar: un trazo de tres puntos se ve igual
  bien o mal dibujado.
- **Al menos tres patentes guardadas**, una de ellas a varias cuadras de donde vas a estar
  parado. La User Story 1 necesita una distancia que no sea cero.

```bash
./gradlew test                  # incluye GeoTest, la única prueba nueva
./gradlew assembleDebug
```

En el dispositivo de referencia —Xiaomi con HyperOS sin SIM— `installDebug` está bloqueado por
el sistema. Ahí el camino es `adb push` del APK a `/sdcard/Download/` e instalarlo a mano desde
el gestor de archivos.

---

## Paso 0 — Respaldar, sin excepción

**Antes de instalar nada.** Ya hay datos de calle en el teléfono, y reinstalar en vez de
actualizar borra la base y las fotos.

```bash
./scripts/respaldo.ps1 respaldar
```

Verificar que el respaldo tiene contenido antes de seguir: la base sola pesa poco, pero el
archivo `-wal` tiene que traer cientos de KB. Un respaldo con `-wal` vacío no sirve.

Después, confirmar contra qué versión se está probando:

```bash
adb shell dumpsys package ar.lauta.buscarpatentes | grep -E "firstInstallTime|lastUpdateTime"
```

Si `firstInstallTime` es igual a `lastUpdateTime`, fue instalación limpia y hay que restaurar
el respaldo antes de validar nada.

---

## US1 — Ir hasta una patente guardada (P1)

Esta se valida **parado en la calle**, a varias cuadras de una patente guardada.

1. Abrir el mapa y tocar el marcador de una patente que esté lejos.
2. En la ficha, **antes de tocar nada**: tiene que decir a qué distancia y en qué dirección
   está. Comparar contra lo que sabés: si la patente está al norte y dice "al sur", el rumbo
   está invertido y `GeoTest` no lo agarró.
3. Contar las interacciones desde ver la patente hasta tener la indicación: **2 o menos**
   (SC-001).
4. Tocar la acción de ir. Tiene que abrir la app de mapas con el punto marcado y el número de
   la patente como etiqueta.
5. Volver a la app. Tiene que estar como la dejaste, y tenés que poder cargar una patente sin
   volver a empezar nada (US1/AC5).
6. **Modo avión, y repetir desde el paso 1.** La distancia y el rumbo tienen que seguir ahí,
   exactamente iguales, sin ninguna pantalla de error de por medio (SC-002). Lo único que
   cambia es que la app de mapas no va a poder rutear.
7. Caminar hasta la patente. Al llegar, la distancia tiene que reflejar que llegaste y no
   mandarte a seguir caminando (US1/AC3).
8. Abrir una patente capturada con precisión degradada: tiene que advertirlo antes de mandarte
   (FR-004).
9. En Ajustes, revocar el permiso de ubicación y volver a abrir una ficha: **"distancia
   desconocida"**, y el destino se tiene que poder abrir lo mismo (FR-003a).

---

## US2 — El recorrido se ve como el camino que caminé (P2)

1. Empezar un recorrido y caminar **diez minutos por calles conocidas**, doblando al menos dos
   esquinas. Doblar importa: un trazo recto no distingue un camino bien dibujado de uno mal
   ordenado.
2. Mientras caminás, mirar el mapa de la pantalla principal: el camino hecho hasta ese momento
   tiene que estar dibujándose, y **seguir creciendo sin tocar nada** (US2/AC3, FR-008). Es lo
   único que el punto nuevo refresca: los marcadores y el resto del histórico se quedan
   quietos, y el mapa no puede trabarse cada cinco segundos (FR-010).
3. Terminar el recorrido y mirar el mapa. **Señalar en voz alta por qué calles pasaste**
   (SC-003). Si el trazo no permite hacerlo, falló.
4. Con tres salidas guardadas y una en curso, mirar el mapa: se ven todas, y la que estás
   caminando se distingue del resto (SC-004).
5. **Ningún rectángulo violeta en ninguna parte del mapa** (US2/AC6, FR-021).
6. Ir a Salidas, abrir una salida: se tiene que ver **solo** ese camino, entero y encuadrado,
   sin desplazar ni hacer zoom a mano. Contar: **1 interacción** (SC-005a).
7. Abrir una salida que quedó sin puntos —empezada y cortada sin moverte—: tiene que decir que
   no hay camino que mostrar, no abrir un mapa vacío (FR-010b).
8. Si en el recorrido hubo un tramo sin señal, mirar cómo quedó: el trazo **se corta** y los
   dos extremos se unen con una **línea punteada** (FR-009, FR-009a). Una recta continua
   cruzando manzanas por las que no pasaste es el defecto que la segunda salida encontró —el
   recorrido "teletransportado"— y este paso existe para agarrarlo. Una vez que la salida se
   ajusta a las calles con wifi (FR-031), el punteado desaparece: el servicio contestó qué
   pasó en el medio.
9. Con la salida más larga que tengas dibujada, desplazar y hacer zoom: sin tirones (SC-005).

---

## US3 — Buscar sirve para encontrar (P3)

1. Abrir Buscar **sin escribir nada**. Sin tocar un solo dígito, tiene que contestar si tenés
   la patente del número actual del juego (SC-006).
2. Si no la tenés, tiene que **decirlo explícitamente**. No desaparecer sin explicación
   (FR-011).
3. Debajo, las capturas más recientes (FR-012).
4. Escribir **un** dígito: la lista ya se acota. No espera al tercero (FR-013).
5. Hacer la búsqueda completa **sosteniendo el teléfono con una sola mano, sin recolocarlo**
   (SC-007). Si tuviste que estirar el pulgar hasta arriba, el FR-014 no se cumplió.
6. Desde un resultado, la acción de ir hasta la patente tiene que estar (FR-002), y **cada
   resultado tiene que decir a qué distancia y para qué lado queda**, igual que la ficha del
   mapa (FR-003). Comparar una fila contra la ficha de la misma patente: los dos textos tienen
   que coincidir.
7. Con el permiso de ubicación revocado, esa línea tiene que decir que la distancia es
   desconocida, y **nunca** un número (FR-003a).
8. Con la base vacía o casi, revisar que el vacío se dice y no queda una lista muda.

---

## US4 — Se ve como una herramienta (P4)

1. **Modo claro**: mirar la barra de navegación del sistema, abajo de todo. Sus controles se
   tienen que distinguir del fondo (SC-008). Este es el defecto que motivó el FR-015.
2. **Modo oscuro**: lo mismo. Y cambiar de modo con la app abierta: se tiene que ajustar solo.
3. Recorrer las cinco pantallas: **ningún violeta que nadie haya escrito** (C1). Botones
   tonales, tarjetas y fondos, todos neutros.
4. Con la app sobre distintas zonas del mapa —parque, avenida, zona sin fondo guardado— mirar
   el campo de número: lo escrito se tiene que leer siempre (FR-017, SC-009).
5. La franja de acciones de abajo: más baja que antes, con fondo propio, y los botones
   separados del mapa (FR-018).
6. **Tocar los tres botones de la franja diez veces cada uno, caminando.** Si fallás alguno,
   el FR-019 no se cumplió y la franja quedó demasiado chica.
7. Los tres estados de una patente en el mapa siguen distinguiéndose (FR-020).

---

## Regresión: lo que no se puede haber roto

Esta feature no escribe en la base, así que la evidencia no está en riesgo. Lo que sí puede
haberse roto es el camino de captura, por todo lo que se movió alrededor.

1. **Modo avión, carga rápida completa.** Tiene que guardar igual: 4 interacciones, 10 segundos
   (SC-010). Es la misma medición que la 002 dejó pendiente en su T034.
2. **Guardar con el mapa en cualquier estado** —cargando, sin cargar, con un recorrido
   dibujándose encima—. El estado del mapa nunca puede demorar el guardado (FR-005).
3. El indicador del juego sigue contestando si tenés el número actual y cuántos consecutivos.
4. Compartir una patente sigue funcionando, y el marcador cambia de color al compartirla.
5. Un recorrido sigue grabando con la pantalla apagada.
6. `./gradlew test` en verde, con `GeoTest` incluido y sin las cinco pruebas de la grilla.
