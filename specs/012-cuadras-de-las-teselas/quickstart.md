# Quickstart: Cuadras de las teselas

## 1. Las pruebas automáticas

`./gradlew :shared:testAndroidHostTest`. `TeselasTest` cubre el decodificador, el recorte, las
esquinas, el borde entre teselas, los nombres y qué calles entran. `CuadrasTest` y
`CoberturaDeZonaTest` arman las cuadras de la franja del centro con las teselas reales.

## 2. Contra Overpass, con la zona del jugador (SC-002)

Fuera del repo: con el borde de la zona completada del jugador, armar las cuadras con las teselas y
compararlas por sus puntas con las que ya tiene guardadas, que salieron de Overpass. Al menos el 95%
coinciden.

## 3. En el teléfono (US1)

1. Instalar la app de esta rama. La base no cambia.
2. Abrir la app con conexión: la zona que buscaba sus calles las encuentra, en segundos.
3. Crear una zona nueva del tamaño de un barrio: en menos de 10 s tiene sus cuadras.
4. Editar el borde de una zona con una cuadra quitada: vuelve a buscar, y la quitada sigue quitada.
