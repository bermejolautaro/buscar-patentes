# Data Model: Caminos sin diagonales

Sin cambios de esquema: la base sigue en la versión 7, y el respaldo de la 006 no cambia.

## Punto de trayecto (`punto_de_trayecto`)

Sin cambios. Es lo que midió el teléfono: latitud, longitud, precisión y hora. **No se escribe
nunca después de grabarse** (Principio II). El ajuste solo lo lee.

## Recorrido (`recorrido`)

Un campo cambia de significado, no de tipo:

| Campo | Tipo | Qué guarda |
|---|---|---|
| `caminoAjustado` | `TEXT`, nullable | El camino ajustado, en el formato de [contracts/camino-ajustado.md](./contracts/camino-ajustado.md) |

## Estado del ajuste

No es una columna: se deduce de `estado` y de `caminoAjustado`, con `CaminoGuardado.leer`.

| Estado | Cuándo | Se dibuja | El interruptor dice |
|---|---|---|---|
| **Pendiente** | `caminoAjustado` es `null` | Los puntos medidos | "Todavía sin ajustar" |
| **A reajustar** | Tiene camino y no empieza con `2:` | Los puntos medidos | "Todavía sin ajustar" |
| **No se pudo** | Es exactamente `2:` | Los puntos medidos | "No se pudo ajustar" |
| **Ajustado** | Empieza con `2:` y trae tramos | Los tramos | El interruptor, en "Ajustado a las calles" |

Una salida `EN_CURSO` está siempre pendiente: se ajusta al terminar.

### Transiciones

```text
Pendiente ──ajuste con respuesta──▶ Ajustado | No se pudo
A reajustar ──ajuste con respuesta──▶ Ajustado | No se pudo
Pendiente | A reajustar ──sin red o error──▶ igual que antes (se reintenta)
Ajustado | No se pudo ──▶ (fin: no se vuelve a pedir)
```

La cola de ajuste (`sinAjustar()`) son las salidas `TERMINADO` en **Pendiente** o **A reajustar**.

## Tramo

Una lista de posiciones (latitud, longitud) que se dibuja como una línea continua. Entre un tramo y
el siguiente de la misma salida va un hueco punteado (FR-009a de la 003). Sale de dos lados:
- de los puntos medidos, cortados donde dos seguidos están a más de 250 m (`Geo.tramos`);
- del camino ajustado guardado: un tramo por tramo de señal, con los pedazos emparejados y los
  puntos medidos que los unen (D2 de [research.md](./research.md)).

## Trazo (en memoria, para el mapa)

| Campo | Qué es |
|---|---|
| `recorridoId` | La salida |
| `tramos` | Lo que se dibuja: los tramos del camino ajustado o los de los puntos medidos |
| `escalon` | Hace cuánto se caminó, para el modo antigüedad |

Reemplaza a `puntos` + `ajustado`: quien arma el trazo ya decidió qué se dibuja.
