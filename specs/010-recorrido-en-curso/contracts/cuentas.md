# Contrato: las cuentas y los textos de una salida

Lo que prueban `CaminataTest`, `EstadisticasTest` y `FormatosTest`.

## C1. Metros

- Los puntos de una salida, en el orden en que se grabaron, se parten con `Geo.tramos` (salto de más
  de 250 m).
- Los metros son la suma de `Geo.distanciaMetros` entre puntos consecutivos de un mismo tramo.
- Sin puntos o con uno solo: 0.

## C2. Calorías

- `kcal = 0,5 × pesoKg × metros / 1000`, redondeado al múltiplo de 5 más cercano.
- Sin peso: no hay valor, y no se muestra nada (FR-009).
- Ejemplos: 70 kg y 2.100 m dan 75; 70 kg y 0 m dan 0; 80 kg y 5.000 m dan 200.

## C3. Textos

| Qué | Formato | Ejemplos |
|---|---|---|
| Tiempo de la barra y de la confirmación | `h:mm` | `0:07`, `0:42`, `1:05`, `26:13` |
| Km | Un decimal con coma | `0,0 km`, `2,1 km`, `12,4 km` |
| Kcal | `~` y el número | `~75 kcal`, `~0 kcal` |
| Barra con peso | Tiempo, km y kcal separados por ` · ` | `0:42 · 2,1 km · ~75 kcal` |
| Barra sin peso | Tiempo y km | `0:42 · 2,1 km` |
| Fila de Salidas | Duración, km, kcal y patentes | `1 h 5 min · 3,2 km · ~110 kcal · 4 patentes` |

## C4. Períodos

- Día: de las 00:00 a las 00:00 del día siguiente, en la hora del teléfono.
- Semana: de lunes 00:00 a lunes 00:00.
- Mes: del día 1 a las 00:00 al día 1 del mes siguiente.
- Una salida cuenta entera en el período de su `iniciadoEn`. Una patente cuenta en el período de su
  `capturadoEn`.

## C5. El aviso del iPhone (D4)

- Identificador `salida-cortada`.
- Mientras hay una salida grabando, cada 30 s: se borra la entregada con ese identificador, si hay
  una, y se programa otra para dentro de 75 s, que reemplaza a la pendiente.
- Al terminar la salida con el botón: se borran la pendiente y la entregada.
- Texto: "Se cortó la salida" / "La app se cerró y dejó de grabar. Abrila para empezar otra salida."
