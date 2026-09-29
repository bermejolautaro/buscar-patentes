<!--
Sync Impact Report
==================
Version change: TEMPLATE (sin ratificar) -> 1.0.0
Bump rationale: Ratificación inicial. Se reemplaza el scaffold de plantilla por
principios concretos derivados de BRIEF.md.

Principios (plantilla -> concreto):
  [PRINCIPLE_1_NAME] -> I. Captura sin fricción (NO NEGOCIABLE)
  [PRINCIPLE_2_NAME] -> II. La evidencia es inmutable (NO NEGOCIABLE)
  [PRINCIPLE_3_NAME] -> III. Local-first, sin red
  [PRINCIPLE_4_NAME] -> IV. Alcance personal (YAGNI)
  [PRINCIPLE_5_NAME] -> eliminado

Secciones concretadas:
  [SECTION_2_NAME] -> Restricciones técnicas
  [SECTION_3_NAME] -> Flujo de desarrollo

Removed: bloque del quinto principio (cuatro principios cubren el proyecto).
Deferred TODOs: ninguno.

Version change: 1.0.0 -> 1.1.0
Bump rationale: MINOR. Se expande materialmente el Principio I: el presupuesto de la
carga rápida pasa de 3 interacciones a 4. Ningún principio se elimina ni se redefine de
forma incompatible, así que no corresponde MAJOR.

Motivo: la especificación 002 (pulido de interfaz) pide que la app abra con el teclado
guardado, porque el jugador la abre tanto para mirar el mapa como para cargar. Con el
teclado guardado hace falta un toque más para escribir. La alternativa era dejar el
principio diciendo 3 mientras el código hacía 4, y un principio desmentido por el código
no gobierna nada.

Lo que NO cambia y sigue siendo el corazón del principio: ubicación y timestamp se
resuelven solos, no hay pantalla intermedia ni selección de modo, y cada paso que alguien
quiera agregar al camino de captura tiene que justificarse contra este principio.

Version change: 1.1.0 -> 1.2.0
Bump rationale: MINOR. Se expande materialmente una guía: "Restricciones técnicas" suma la
referencia de ubicación del iPhone. El Principio IV se aclara sin cambiar su sentido. Ningún
principio se elimina ni se redefine.

Motivo: la especificación 006 lleva la app al iPhone, que pasa a ser el teléfono de juego. La
constitución nombraba solo la plataforma Android.

Qué se aclara en el Principio IV: "un teléfono" quiere decir uno a la vez. Pasar al iPhone es una
mudanza. El respaldo reemplaza los datos, no sincroniza dos teléfonos. Usar dos teléfonos a la par
seguiría pidiendo una enmienda.

Templates: sin cambios. Deferred TODOs: ninguno.
-->

# Constitución de buscar-patentes

## Core Principles

### I. Captura sin fricción (NO NEGOCIABLE)

La app se usa parado en la vereda, con una mano, mirando un auto que se puede ir.
El camino de carga rápida MUST llegar desde la app cerrada hasta patente guardada
en 4 interacciones o menos: abrir, tocar el campo, tipear el número, confirmar.
Ubicación y timestamp MUST resolverse solos, sin que el usuario los toque.

El presupuesto era 3 hasta la versión 1.1.0, con el teclado ya arriba al abrir. Se
subió a 4 porque la app se abre tanto para mirar el mapa como para cargar, y un
teclado que ocupa media pantalla sin que nadie lo pida es fricción en el otro
sentido. El presupuesto MUST NOT volver a subir sin otra enmienda: 4 es el techo,
no un punto de partida.

Cualquier pantalla, campo o confirmación que se agregue al camino de carga rápida
MUST justificarse contra este principio o no entra.

Racional: si la captura tarda, el auto se fue y la app no sirvió para nada.

### II. La evidencia es inmutable (NO NEGOCIABLE)

Ubicación y timestamp se registran en el momento de la captura y MUST NOT ser
editables después. No existe carga retroactiva de metadata: si no se capturó en el
momento, no existe.

Cada registro MUST guardar la precisión reportada por el proveedor de ubicación
junto con las coordenadas. Una lectura degradada se guarda como degradada; nunca
se redondea, se rellena ni se infiere.

Racional: la metadata es lo único que distingue una patente vista de una inventada.
Si se puede editar, no prueba nada.

### III. Local-first, sin red

Toda captura MUST completarse y persistir sin conexión. La red MAY usarse para
funciones accesorias (mapas, exportar, compartir) pero NEVER como requisito del
camino de captura.

Una captura que no se pudo persistir MUST fallar de forma visible y ruidosa.
Perder un registro en silencio es el peor resultado posible del sistema.

Racional: la calle tiene mala señal, túneles y estacionamientos subterráneos. La app
existe justamente para el momento en que la red no está.

### IV. Alcance personal (YAGNI)

Un usuario, un teléfono. NO hay cuentas, login, backend, sincronización ni
multiusuario hasta que un requisito concreto y presente lo obligue.

"Un teléfono" quiere decir uno a la vez, no una sola plataforma. Cambiar de teléfono es una
mudanza: un respaldo que se lleva de uno al otro y reemplaza los datos. Fusionar los datos de
dos teléfonos es sincronizar, y no entra.

Toda abstracción MUST tener al menos dos usos reales en el código antes de existir.
Toda dependencia nueva MUST justificarse contra lo que ya está instalado y contra
la biblioteca estándar de la plataforma.

Racional: es una herramienta para un juego de WhatsApp entre amigos. Mantener
infraestructura que nadie pidió cuesta más que cualquier beneficio que traiga.

## Restricciones técnicas

El stack NO se decide en esta constitución; se decide en `/speckit-plan` a partir de
la spec. El Principio III igual acota el espacio: la plataforma elegida MUST tener
acceso real a ubicación de alta precisión y a almacenamiento local persistente. En
Android la referencia es FusedLocationProvider (Google Play Services Location). En iPhone es
Core Location, con la precisión que el sistema reporta; si el usuario apagó la ubicación
exacta, la lectura se guarda como degradada, igual que cualquier otra.

La app corre en Android y en iPhone desde la especificación 006. Las reglas del juego se escriben
una sola vez y MUST dar el mismo resultado en los dos.

Formato de patente: el juego usa solo la parte numérica. La app MUST soportar los dos
formatos argentinos vigentes, el viejo (`AAA 123`) y el Mercosur (`AB 123 CD`), y la
spec MUST definir cómo se extrae el número de cada uno.

## Flujo de desarrollo

El trabajo sigue el ciclo de Spec Kit: `/speckit-specify`, `/speckit-clarify`,
`/speckit-plan`, `/speckit-tasks`, `/speckit-implement`. No se escribe código de
feature antes de que exista su spec.

Toda lógica no trivial (una rama, un parser, el manejo de precisión del GPS, la
extracción del número de patente) MUST dejar al menos una prueba ejecutable que falle
si la lógica se rompe. Un one-liner no necesita prueba.

Los commits siguen Conventional Commits.

## Governance

Esta constitución tiene prioridad sobre cualquier otra práctica del proyecto. Ante un
conflicto entre la constitución y una spec, un plan o una tarea, gana la constitución;
si la constitución es la que está mal, se enmienda primero y después se avanza.

Enmiendas: se editan en este archivo, con el Sync Impact Report de la cabecera
actualizado y la versión incrementada en el mismo commit.

Versionado semántico de este documento:

- MAJOR: se elimina o redefine un principio de forma incompatible.
- MINOR: se agrega un principio o se expande materialmente una guía.
- PATCH: aclaraciones, redacción, correcciones sin cambio semántico.

Cumplimiento: cada `/speckit-plan` MUST verificarse contra estos principios antes de
generar tareas. Toda complejidad que viole el Principio IV MUST justificarse por
escrito en el plan o eliminarse.

**Version**: 1.2.0 | **Ratified**: 2026-08-28 | **Last Amended**: 2026-09-28
