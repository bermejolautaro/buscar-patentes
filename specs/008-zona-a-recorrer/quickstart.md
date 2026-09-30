# Quickstart: Zona a recorrer

Cómo se prueba que la zona funciona.

## Requisitos

- La app de esta rama instalada en el teléfono de juego, con las salidas de siempre.
- Conexión para crear la zona. El resto se prueba también sin conexión.
- Antes de instalar, un respaldo (Ajustes → Respaldo): la base pasa a la versión 8.

## 1. Las pruebas automáticas

```bash
./gradlew :shared:testAndroidHostTest
```

Tienen que pasar, además de las de siempre:
- `CuadrasTest`: el armado de cuadras del contrato Z2;
- `CoberturaDeZonaTest`: la cuenta del Z3;
- `BordeTest`: tres esquinas o más, sin cruces, y qué queda adentro;
- `FormatosTest`: la duración en días, y en meses y días;
- `RespaldoTest`: un respaldo de la 7 que se restaura en la 8.

El `.ipa` corre las mismas pruebas en el simulador del iPhone antes de armarse.

## 2. La migración (FR-020)

1. Instalar encima de la versión anterior, sin borrar datos.
2. Abrir la app: están todas las patentes y todas las salidas, y el mapa se ve como antes.
3. Restaurar el respaldo del paso de requisitos: se restaura sin error y la pantalla de zonas está
   vacía.

## 3. Crear una zona y ver cuánto falta (US1, SC-001)

1. Pantalla principal → **Zonas** → **Nueva zona**.
2. Tocar las esquinas de unas diez manzanas conocidas, moviendo el mapa entre toque y toque. Probar
   **Deshacer** una vez.
3. Confirmar sin escribir un nombre, y dejar "desde cuándo cuenta" en hoy. Cronometrar desde el
   primer toque: menos de un minuto.
4. Aparece "Buscando las calles", y en menos de 30 segundos el porcentaje. Tiene que ser **0%**,
   aunque haya salidas viejas adentro.
5. Correr "desde cuándo cuenta" a una fecha anterior a esas salidas. Las cuadras caminadas pasan a
   recorridas. Contar a mano, en el mapa de la zona, las recorridas y el total: el porcentaje
   coincide.
6. Volver a la pantalla principal: el borde punteado y las cuadras que faltan resaltadas, sin
   ningún número. Apagar los recorridos: la zona también se apaga.

**Un dibujo que no es una zona**: con dos esquinas no deja confirmar, y lo mismo si el borde se
cruza.

## 4. Caminar (US1, SC-002)

1. Con la zona activa, salir y caminar **diez cuadras pendientes de esquina a esquina**. En el
   camino, cruzar en las esquinas por lo menos tres calles de la zona sin caminarlas.
2. Terminar la salida y abrir la app con conexión.
3. Cuando la salida queda ajustada, la zona suma exactamente esas diez: ninguna de menos, y ninguna
   de las calles cruzadas. En la pantalla principal, las diez dejan de estar resaltadas.
4. Si hay una avenida con bulevar en la zona, caminar una cuadra por una sola vereda: cuenta como
   una cuadra, y la otra mano no queda pendiente.

## 5. Sin conexión (FR-005, SC-003)

1. Modo avión. Abrir la zona: el porcentaje y el mapa aparecen en menos de un segundo.
2. Crear otra zona en modo avión: queda en "Buscando las calles".
3. Sacar el modo avión y volver a la pantalla principal. Al abrir la zona, ya tiene sus cuadras.

## 6. Quitar cuadras (US2, SC-004)

1. En el mapa de la zona, tocar una cuadra pendiente → **Quitar esta cuadra**. Son dos toques. El
   total baja en uno y el porcentaje cambia en el momento.
2. Tocar otra → **Quitar toda la calle en la zona**: se van todas las cuadras de esa calle
   adentro de la zona.
3. Tocar una quitada → **Volver a sumar**: vuelve a contar como antes.
4. En la pantalla principal, las quitadas no se resaltan.

## 7. Terminar (US3)

1. En una zona chica, quitar todas las pendientes menos una y caminarla. Cuando la salida se
   ajusta, la zona queda **completada**, con la fecha de esa salida y los días desde "desde cuándo
   cuenta".
2. En otra zona, **Cerrar objetivo** → confirmar. Queda cerrada con su porcentaje, su fecha y sus
   días.
3. Borrar una salida que contaba para esa zona cerrada: su resultado no cambia.
4. La lista de zonas muestra primero las activas y después las terminadas.
5. **Borrar** una zona → confirmar. Las salidas quedan como estaban: se verifica en la pantalla de
   Salidas y en el mapa.

## 8. Entre teléfonos (FR-020, FR-021, SC-005)

1. Sacar un respaldo en un teléfono con zonas activas, cuadras quitadas y una zona terminada.
2. Restaurarlo en el otro teléfono, con la app de esta rama.
3. Cada zona tiene lo mismo: cuadras, lo quitado, las fechas y el porcentaje.
