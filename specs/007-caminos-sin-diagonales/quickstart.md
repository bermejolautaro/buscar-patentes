# Quickstart: Caminos sin diagonales

## 1. Lo automático

```powershell
./gradlew.bat :shared:testAndroidHostTest assembleDebug
```

- `CaminoAjustadoTest`: el armado de un tramo con pedazos cortados, puntos sueltos al principio, en
  el medio y al final, y un tramo sin nada emparejado. En ningún caso hay dos posiciones seguidas
  que no vengan de la misma calle o de dos puntos medidos consecutivos. También la lectura y
  escritura del formato `2:` y que lo viejo se lea como "a reajustar".
- `PolilineaTest`: codificar y decodificar dan lo mismo, a 1e6.
- `AjustarACallesTest`: una respuesta real recortada, de una caminata inventada cerca del Obelisco,
  se lee bien.
- `ColeccionTest`: un trazo con varios tramos da una línea por tramo y un hueco punteado entre
  cada uno, venga del ajuste o de los puntos medidos.
- En la nube, las mismas pruebas en el simulador del iPhone antes del `.ipa`.

## 2. El reajuste de lo que ya está (US1, SC-002, SC-004)

1. Antes de actualizar: anotar cuántas salidas hay y, en el detalle de dos o tres, la cantidad de
   patentes y la duración.
2. Actualizar la app en los dos teléfonos, encima, sin desinstalar.
3. **Sin conexión**, abrir la app: todas las salidas se ven con los puntos medidos. En el detalle,
   el interruptor dice "Todavía sin ajustar".
4. Con conexión, entrar a la pantalla principal y esperar un minuto.
5. **Esperado**: las salidas se ven ajustadas, y en el detalle el interruptor ya deja cambiar. Las
   cuentas del paso 1 son las mismas.

## 3. Sin diagonales (US1, SC-001)

En el mapa, en cobertura y en antigüedad, recorrer con la vista **cada** salida de la zona:

- ninguna recta cruza una manzana;
- donde hubo un corte de señal, se ve la línea punteada, también en las salidas ajustadas;
- el final de cada salida llega hasta donde terminó la caminata.

Para comparar contra la versión anterior: una salida con una diagonal conocida, antes y después.

## 4. El interruptor (US2, SC-003)

1. Abrir el detalle de una salida ajustada: arranca en "Ajustado a las calles".
2. Tocar el interruptor: dice "Lo que midió el teléfono", el camino cambia en menos de un segundo, y
   **la cámara no se mueve**.
3. Volver a la lista y abrir la misma salida: arranca otra vez en ajustado.
4. Abrir una salida en curso: dice "Todavía sin ajustar" y no deja cambiar.

## 5. Los dos teléfonos

Pasos 2 a 4 en el Android y en el iPhone. Un respaldo del Android restaurado en el iPhone trae sus
salidas: las que venían con el camino viejo se reajustan solas la primera vez con conexión.
