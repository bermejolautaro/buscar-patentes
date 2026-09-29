# Quickstart: validar Captura de patentes

**Fecha**: 2026-08-28
**Spec**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Contracts**: [contracts/os-and-share.md](./contracts/os-and-share.md)

Cómo correr la app y probar que cada user story hace lo que la spec dice. Los escenarios
de aceptación completos están en la spec; acá está cómo ejecutarlos.

---

## Prerrequisitos

- JDK 17
- Android Studio, o el SDK de línea de comandos con `ANDROID_HOME` configurado
- Un **teléfono Android físico** con Google Play Services, Android 8.0 o superior

El emulador sirve para la lógica y la interfaz, pero **no** alcanza para validar las
User Stories 4 y 5. La ubicación simulada del emulador no dispara geofences de forma
confiable y no reproduce el comportamiento de batería ni las restricciones de segundo
plano. Todo lo que dependa de moverse en el mundo se prueba caminando.

---

## Correr

```bash
./gradlew installDebug
./gradlew test                  # lógica de dominio, JVM
./gradlew connectedAndroidTest  # consultas de Room, requiere dispositivo
```

---

## Validación por user story

### US1 — Carga rápida (P1)

La más importante y la más fácil de probar. Se prueba con un cronómetro.

1. Cerrar la app por completo (deslizarla de recientes).
2. Arrancar el cronómetro y abrirla.
3. Tipear tres dígitos y tocar `+`.

**Esperado**: el campo ya estaba enfocado con el teclado arriba, sin pantalla
intermedia. El registro queda guardado. Menos de 10 segundos y 3 interacciones
(SC-001, SC-002).

**Offline** (SC-003): poner el teléfono en modo avión y repetir. El registro se guarda
igual. El mapa se ve sin fondo o con lo cacheado; no bloquea nada.

**Mapa cargando** (FR-038): abrir con red lenta y tipear antes de que el mapa termine.
El guardado no espera al mapa.

**Precisión** (SC-004, FR-009): guardar adentro de un edificio. El registro se guarda
con la precisión real y marcado como degradado. Verificar que no hay coordenadas
inventadas ni redondeadas.

**Inmutabilidad** (SC-006, Principio II): no debe existir ninguna pantalla que permita
editar ubicación o timestamp. Si aparece una, es un bug de diseño, no una función.

---

### US2 — Foto (P2)

1. Tipear un número y tocar el botón de cámara.
2. Sacar la foto.

**Esperado**: registro guardado con foto, y con la ubicación y el timestamp del momento
de la captura.

**Cancelar** (escenario 3): abrir la cámara y volver atrás sin disparar. Vuelve al campo
con el número todavía tipeado y **sin** registro creado.

**Adjuntar después**: guardar con `+`, después agregar foto. Verificar que la ubicación
y el timestamp originales **no** cambiaron.

---

### US3 — Encontrar y usar (P3)

1. Fijar el número del juego en 313.
2. Cargar registros para 313, 314 y 315.

**Esperado**: al abrir la app se ve que el 313 está cubierto y que hay 3 consecutivos
adelantados (SC-008), sin buscar nada.

3. Compartir el registro del 313.

**Esperado**: aparece el selector del sistema. El registro pasa a `COMPARTIDA` (C1).

4. Avanzar el contador a 314.

**Esperado**: recalcula. Y —esto es lo que hay que mirar— **reconcilia los geofences**
(D4): se desregistra el del 313, se registra el del 314.

---

### US4 — Aviso de proximidad (P4)

La validación crítica del proyecto. Requiere salir a la calle.

1. Guardar un registro con `+`, sin foto, en una ubicación conocida — la puerta de un
   comercio del barrio sirve.
2. Poner el número del juego en ese número.
3. **Cerrar la app por completo.**
4. Alejarse más de 300 metros y volver caminando.

**Esperado**: la notificación llega a menos de 150 metros, con la app cerrada y sin
ningún recorrido activo (SC-009).

**Casos negativos**, igual de importantes:

- Registro cuyo número **no** toca: no debe avisar (FR-029).
- Registro ya compartido: no debe avisar (FR-029).
- Avisos apagados por el jugador: no debe avisar, y el resto de la app sigue andando
  (FR-040).
- Pasar dos veces en la misma salida: un solo aviso (FR-041).

**Después de reiniciar el teléfono** (C2): repetir el paso 4. Si el aviso no llega, la
app no volvió a registrar los geofences en el arranque. Es la falla más silenciosa de
todo el sistema: nada muestra error, simplemente el aviso nunca llega.

**Batería** (SC-012): dejar el teléfono un día con avisos activos y sin recorridos.
Menos del 3% atribuible a la app en el reporte del sistema.

---

### US5 — Recorrido (P5)

1. Tocar "Empezar recorrido".
2. Caminar unas cuadras **con la pantalla apagada**.
3. Cargar una patente en el camino.
4. Terminar el recorrido.

**Esperado**: la notificación persistente estuvo visible todo el tiempo (C3). Las calles
recorridas quedaron marcadas. La patente quedó asociada al recorrido.

**Cobertura acumulada** (SC-015, FR-032): repetir en otra salida por calles distintas.
El mapa muestra el acumulado de las dos, no una por vez.

**Interrupción** (FR-033): durante un recorrido, forzar el cierre de la app desde
ajustes. Reabrir. El recorrido aparece como terminado, **con los puntos que alcanzó a
juntar**. No debe perderse el tramo.

**Batería** (SC-010): un recorrido de 2 horas, menos del 5%.

---

## Validación de permisos (C4)

Se prueba negando, que es el caso que rompe apps.

| Negar | Esperado |
|---|---|
| Ubicación precisa | Guardar se bloquea con explicación. No se guarda un registro sin ubicación |
| Ubicación en segundo plano | Avisos y recorridos apagados. **Todo lo demás funciona** |
| Notificaciones | Se explica que sin esto no hay aviso, y se ofrece abrir ajustes |
| Cámara | El botón `+` sigue guardando normal |

La regla de C4: negar un permiso degrada una función, nunca rompe la app.

---

## Validación de espacio (SC-014, FR-044 y FR-045)

1. Recorrer varias zonas nuevas con red para llenar el caché de mapa.
2. Ver el espacio ocupado en la pantalla de ajustes de la app.
3. Borrar el fondo guardado.

**Esperado**: el tamaño reportado baja. El caché no crece sin techo: al llegar al límite
desaloja lo menos usado, no falla.

---

## Lo que las pruebas automáticas cubren

`./gradlew test` cubre la lógica pura identificada en D8 y en Derivados del data model:

- extracción del número de 3 dígitos de formato viejo y Mercosur (FR-010)
- consecutivos cubiertos desde el número actual (FR-022)
- selección del conjunto de geofences ante cambio de contador (D4)
- decisión de si un aviso corresponde: número, estado, repetición (FR-028, FR-029, FR-041)
- agregación de puntos de trayecto a cobertura de calles (FR-032)

Todo lo demás de esta guía es manual y presencial, porque depende de moverse por la
ciudad. No hay forma honesta de automatizarlo.
