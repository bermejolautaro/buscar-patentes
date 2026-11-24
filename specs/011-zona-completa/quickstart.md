# Quickstart: Zona completa

## 1. Las pruebas automáticas

`./gradlew :shared:testAndroidHostTest`. `ZonasTest` y `CoberturaDeZonaTest` cubren lo medido, los
huecos y el pasaje pegado a la diagonal.

## 2. Con el respaldo del jugador (SC-001, SC-002)

Fuera del repo, con el script de diagnóstico: la cuadra del pasaje sale recorrida, la zona al 100%,
y ninguna cuadra recorrida deja de estarlo.

## 3. En el teléfono (US1, US2)

1. Instalar la app de esta rama. La base no cambia.
2. Abrir la app con permiso de notificaciones: la zona se completa y aparece "¡Zona completada!",
   con la app abierta.
3. En Zonas, la zona figura en Terminadas, con la fecha de la salida en que se caminó el pasaje.
4. En otra zona, caminar la última cuadra y terminar la salida: la notificación llega en menos de
   10 s. Cerrar otra a mano: no felicita.
