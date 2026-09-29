# buscar-patentes — brief

Insumo crudo para `/speckit-specify`. No es la spec: es el material del que sale.

## El juego

Minijuego entre amigos por WhatsApp. Se buscan patentes **en secuencia, solo por el
número**. Estado actual del grupo: **313**.

Ciclo de una jugada:
1. Encontrar en la calle la patente con el número que toca.
2. Fotografiarla en el momento.
3. Mandarla al grupo de WhatsApp.

Regla del grupo: **no vale guardarse fotos de patentes futuras**. Hay que verla en el
momento, fotografiarla y pasarla.

## Qué quiero construir

Una app personal para trackear patentes. Utilidad para **guardar patentes futuras**
con su evidencia asociada.

Lo importante es la metadata de captura:
- **Ubicación** donde se sacó la foto, lo más exacta posible.
- **Timestamp** de la captura.

### Carga rápida

Funcionalidad clave: cargar **solo el número de patente** y que la app resuelva sola
el resto a partir del GPS del teléfono — ubicación (lo más precisa posible) y
timestamp. Mínima fricción, se usa parado en la vereda.

En Android la referencia es **FusedLocationProvider** (Google Play Services Location).

## Puntos abiertos para `/speckit-clarify`

- **Tensión con la regla del grupo.** La regla dice que no vale stockear patentes
  futuras; la app existe para stockearlas. Definir qué es el producto: ¿un archivo
  privado del que se van soltando patentes cuando toca el número, o una herramienta
  de registro donde GPS+timestamp son la *prueba* de que se vio en vivo? Las dos
  lecturas dan features distintas.
- ¿La foto es obligatoria o la carga rápida puede ser sin foto?
- ¿Qué pasa si no hay señal GPS o está degradada? ¿Se guarda igual, con precisión
  declarada?
- ¿Formato de patente? Argentina tiene viejo (`AAA 123`) y Mercosur (`AB 123 CD`).
  El juego usa "solo el número" — hay que definir cómo se extrae de cada formato.
- ¿Multiusuario / sincronización con el grupo, o estrictamente local de un teléfono?
- ¿Cómo sale la patente hacia WhatsApp? ¿Share nativo, export, copy?
- Retención: ¿las patentes ya usadas se archivan o se borran?
