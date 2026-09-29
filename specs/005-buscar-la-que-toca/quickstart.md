# Quickstart: validar "Ir a buscar la que toca"

Cómo se prueba que la feature funciona. Primero lo que corre en la computadora, después lo que
solo se juzga en la calle, con el teléfono en la mano.

## 1. Pruebas en JVM

```powershell
.\gradlew.bat testDebugUnitTest
```

Tienen que pasar, además de las que ya existen:

| Prueba | Qué cubre |
|---|---|
| `CoberturaTest` | "Ninguna descubierta", "1 descubierta", "N descubiertas" (D3) |
| `PrioridadTest` | Los escenarios 3, 4 y 5 de la US3 con sus números, el SC-005, el empate por fecha, el orden sin posición, y el recorrido por vecino más cercano (D8, D9) |
| `IrTest` | La URL de Google Maps: destino = última, waypoints en orden, `%7C`, punto decimal con el teléfono en español (C4) |
| `AcomodoTest` | Solo si la US5 entra: un pin quieto, dos que se separan en zoom 17, los mismos agrupados en zoom 13, veinte en un grupo, determinismo (data-model) |
| `ColeccionTest` | La que toca va a su propia colección y la otra no la lleva (D1) |
| `InmutabilidadTest` | Sin cambios: sigue en verde |

## 2. Instalar en el teléfono

```powershell
.\gradlew.bat assembleDebug
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb push app\build\outputs\apk\debug\app-debug.apk /sdcard/Download/buscar-patentes.apk
```

Se instala a mano tocando `buscar-patentes.apk` desde Archivos: el teléfono no tiene SIM y adb no
puede instalar. Si `adb devices` dice `unauthorized`, esperar un rato y volver a mirar.

**Antes de probar**: tener cargado un número actual con **al menos tres** patentes guardadas, una
de ellas en una zona con varias patentes cerca, y otro número con una sola.

## 3. En el teléfono

### US1 — cuántas hay y dónde

1. Abrir la app sin tocar nada. La barra dice el número actual y "3 descubiertas" (o las que
   haya). No aparece "números seguidos".
2. Las patentes del número actual tienen **fondo verde** con el número en blanco, y el borde de
   su confianza se distingue sobre el verde.
3. Alejar el mapa hasta que la zona cargada se agrupe. Las verdes **siguen sueltas**, encima del
   grupo, y la cuenta del grupo no las incluye.
4. Cargar el número actual en otro lugar: la barra pasa a "4 descubiertas".

### US2 — filtrar

1. Tocar la lupa. El mapa queda con las del número actual, **se mueve para mostrarlas todas junto
   con el punto azul**, y sube el teclado con el número seleccionado.
2. Bajar el teclado: el filtro sigue, la leyenda dice qué número se muestra.
3. Tocar el campo, escribir otro número de tres dígitos: al tercer dígito cambia el mapa, la barra
   y el encuadre. Con uno o dos dígitos no cambia nada.
4. Escribir un número sin patentes: "Ninguna descubierta".
5. Tocar la `X`: vuelven todas, el mapa **no se mueve**, y aparece el botón de recentrar.
6. Con el filtro puesto, cerrar la app y abrirla: sin filtro. Lo mismo al ir a Salidas y volver.
7. Con el filtro puesto, cargar una patente de otro número por el campo de abajo: se guarda y lo
   avisa.

### US3 — la lista

1. Tocar la barra: se despliega la lista, sin tapar más de media pantalla.
2. Cada fila dice distancia, rumbo y confianza. Las más cercanas arriba; mirar que una con más
   confianza pase adelante cuando la diferencia de distancia es chica respecto del total.
3. Tocar una fila: se pliega, el mapa va a esa patente y abre la ficha.
4. Sacarle el permiso de ubicación en Ajustes del sistema: la lista dice que ordena por
   confianza.

### US4 — el recorrido

1. Con tres patentes del número, "Armar recorrido": están las tres, la primera es la más
   cercana.
2. Destildar una, invertir las otras dos con las flechas, "Abrir en Google Maps": Maps abre a pie,
   desde donde se está, con esas dos paradas en ese orden.
3. Destildar todas: el botón se deshabilita. Dejar una: abre como "Ir".
4. Volver a la app: no hay ninguna salida iniciada por esto.

### US5 — pines que se hacen lugar (si entró)

1. En la zona con cuatro o cinco patentes en una cuadra, a zoom de calle: se ven separadas, cada
   una cerca de su lugar.
2. Alejar de a un nivel: en algún zoom se agrupan, y no antes de que dejen de entrar.
3. Desplazar el mapa sin cambiar el zoom: ningún pin se mueve.
4. Tocar un pin corrido: la ficha muestra su ubicación real.
5. **Línea de corte**: si el salto al soltar el pellizco molesta o el mapa se traba al
   desplazarlo, se vuelve a la agrupación de siempre (D13).

### La confianza arranca en 0 (FR-024)

Ya está implementada desde el 2026-09-28; esto verifica que la 005 la usa bien.

1. Anotar una patente nueva: la ficha dice "0 de 10" y el borde del pin es rojo.
2. Volver a anotarla en el mismo lugar: sube a "1 de 10".
3. Las siete que la migración conservó (335 y 344 en 10; 344, 369 y 380 en 7; 316 y 327 en 6)
   siguen con ese valor en la ficha y en la lista de la barra.
4. En la lista de la barra, dos patentes del mismo número con confianza 0 quedan ordenadas solo por
   distancia.

### Lo que se fue

1. La franja de abajo ya no tiene el botón de búsqueda, y el campo de número es más ancho.
2. Abrir la ficha de una patente sin foto: "Agregar foto" funciona, y la ubicación y la fecha no
   cambian.
3. Ajustes no manda a nadie a "Buscar".
4. Carga rápida: abrir, tocar el campo, tipear, confirmar. Siguen siendo 4 (SC-008).
