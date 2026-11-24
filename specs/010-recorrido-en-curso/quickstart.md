# Quickstart: Recorrido en curso

Cómo se valida la 010 en los teléfonos.

## Requisitos

- La app de esta rama instalada. **La base pasa a la 9: sacar un respaldo antes de instalar.**
- Permiso de ubicación y de notificaciones.

## 1. Las pruebas automáticas

`./gradlew :shared:testAndroidHostTest`. Cubren las cuentas y los textos del
[contrato](./contracts/cuentas.md): `CaminataTest`, `EstadisticasTest`, `FormatosTest` y la migración
de la 8 a la 9 en `MigracionTest`.

## 2. Confirmar antes de terminar (US1)

1. Empezar una salida: no pregunta nada.
2. Tocar el botón otra vez: aparece la confirmación con tiempo, km y calorías. **Seguir**: la salida
   sigue grabando.
3. Tocar afuera: sigue grabando. Tocar el botón y **Terminar**: termina.

## 3. La barra (US3)

1. Sin peso cargado, empezar una salida y caminar 10 minutos: la barra dice el tiempo y los km, y
   sube sola.
2. Cargar el peso en Ajustes y volver: la barra suma las calorías.

## 4. El aviso del iPhone (US2, SC-004, SC-005)

1. Con una salida en curso, cerrar la app desde el selector de apps y bloquear el teléfono. En menos
   de 2 minutos llega la notificación. Tocarla abre la pantalla principal, que dice que la salida se
   cortó.
2. **Sin falsas alarmas**: una salida de una hora con la pantalla bloqueada, con ratos quieto de más
   de 5 minutos, terminada con el botón. No llega ninguna notificación, ni durante ni después.

## 5. El Android al sacarlo de recientes (D5)

1. Con una salida en curso, sacar la app de recientes. La notificación de la salida sigue.
2. Caminar dos cuadras y abrir la app: la salida sigue en curso y tiene los puntos de esas cuadras.
3. Si en cambio la salida se cortó, anotar si HyperOS la detuvo a la fuerza
   (`adb shell dumpsys package ar.lauta.buscarpatentes | grep stopped`). Decide si hace falta el
   aviso también en el Android (D5).

## 6. Km y calorías (US4, SC-003)

1. Caminar un recorrido de 2 km medido en un mapa y terminarlo. En Salidas dice entre 1,8 y 2,3 km.
2. Una salida vieja, de antes de esta rama, también muestra sus km y, con peso, sus calorías.
3. Cambiar el peso: cambian las calorías de todas. Borrarlo: no queda ninguna, y el detalle dice que
   se ven cargando el peso.

## 7. Las estadísticas (US5, SC-006, SC-007)

1. Desde Salidas, abrir **Estadísticas**. Abre en menos de un segundo.
2. En "semana", los km son la suma exacta de las salidas de esa semana en la lista.
3. Las flechas van a los períodos anteriores y no pasan del actual. El gráfico resalta el período
   elegido.

## 8. Entre teléfonos (D3)

1. Sacar un respaldo con el peso cargado y restaurarlo en el otro teléfono: el peso viaja.
