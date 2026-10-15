# Quickstart: Patentes desde la que toca

Cómo se valida la 009 en el teléfono.

## Requisitos

- La app de esta rama instalada. La base no cambia: no hace falta respaldo antes.
- Patentes guardadas con números menores, iguales y mayores que el que toca.

## 1. Las pruebas automáticas

`./gradlew :shared:testAndroidHostTest`. `VistaDePatentesTest` cubre el ciclo y qué número se ve
en cada vista, con y sin lupa.

## 2. El arranque (FR-002, FR-003, SC-001)

1. Abrir la app. El mapa muestra la que toca, en verde, y las de números más altos. Ninguna menor.
2. La leyenda dice "Desde la" y el número que toca.

## 3. El ciclo (FR-001, FR-005, SC-002)

1. Tocar el botón de patentes: aparecen todas. La leyenda ya no dice "Desde".
2. Otro toque: no queda ninguna, y la leyenda dice "Patentes ocultas".
3. Otro toque: vuelve a la vista del arranque. Cada vista con su ícono.
4. Dejarlo en todas, cerrar la app y abrirla: arranca otra vez desde la que toca.

## 4. Cambiar el número (FR-002)

1. En Ajustes, subir el número que toca. Al volver, las de números menores ya no están.

## 5. La lupa (FR-006)

1. En mayor o igual, buscar con la lupa un número menor que el que toca: el mapa lo muestra, y la
   leyenda dice "Solo la" con ese número.
2. Pasar a ninguna con la lupa abierta: no queda ninguna.
