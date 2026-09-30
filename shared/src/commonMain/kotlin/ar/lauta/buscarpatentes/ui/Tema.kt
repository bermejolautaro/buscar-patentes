package ar.lauta.buscarpatentes.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * El tema de la app (FR-016, contrato C1).
 *
 * ## Por qué el esquema está definido entero
 *
 * La 002 definía acá **solo `primary` y `secondary`**, y la app se veía violeta: botones
 * tonales lilas, tarjetas lilas, fondos con tinte. No había un solo color violeta escrito
 * en el código.
 *
 * La causa es que un `ColorScheme` de Material 3 tiene decenas de roles, y los que no se
 * declaran **se quedan en el esquema base de Material 3, que es violeta**. Un
 * `FilledTonalIconButton` usa `secondaryContainer`; una `Card` usa `surfaceContainer`; la
 * elevación tiñe con `surfaceTint`. Ninguno de esos tres estaba declarado.
 *
 * Es la misma forma de error que el modo oscuro roto de la 002: no era que alguien hubiera
 * elegido mal un color, era que nadie lo había elegido y el default no era neutro.
 *
 * **La regla que sale de eso, y que este archivo tiene que sostener**: si un componente se
 * ve con un color raro, el rol que usa está sin declarar. El arreglo es declararlo acá,
 * nunca pisar el color en el componente.
 *
 * ## Por qué es monocromo
 *
 * Superficies neutras y un solo acento —casi negro en claro, casi blanco en oscuro—, que va
 * a la acción de cargar una patente. Así **los únicos colores saturados de la app son los
 * tres del mapa**, que son los que significan algo: pendiente, ya toca, compartida. Un
 * acento azul habría competido justamente con el azul de "pendiente".
 *
 * No hay selector de tema adentro de la app: se sigue el del sistema (Principio IV).
 */

/**
 * ## Por qué es la escala zinc de shadcn/ui
 *
 * El primer intento de "paleta neutra" quedó gris y apagado: neutros tibios de contraste
 * bajo, sin bordes visibles, todo del mismo valor. Sacó el violeta y trajo otro problema.
 *
 * La escala de shadcn resuelve las dos cosas a la vez, y por eso se copia tal cual en lugar
 * de inventar una:
 *
 * - **Neutros fríos**, no tibios. `#71717A` en lugar de `#77777B`: la diferencia se ve.
 * - **Primer plano casi negro** (`#09090B`), no gris oscuro. El texto tiene que ser texto.
 * - **Bordes con valor propio** (`#E4E4E7`), que es lo que separa una tarjeta del fondo sin
 *   necesidad de sombra.
 * - **Poca elevación y esquinas de 8 dp.** La jerarquía la hace el borde, no la sombra.
 */

// Escala zinc, modo claro.
private val ZINC_950 = Color(0xFF09090B)
private val ZINC_900 = Color(0xFF18181B)
private val ZINC_800 = Color(0xFF27272A)
private val ZINC_500 = Color(0xFF71717A)
private val ZINC_400 = Color(0xFFA1A1AA)
private val ZINC_200 = Color(0xFFE4E4E7)
private val ZINC_100 = Color(0xFFF4F4F5)
private val ZINC_050 = Color(0xFFFAFAFA)
private val BLANCO = Color(0xFFFFFFFF)

// El rojo destructivo de shadcn. Se mantiene saturado a propósito: es la única señal de la
// interfaz que tiene que gritar, y apagarla para que combine sería empeorar la app para que
// se vea mejor.
private val ROJO = Color(0xFFDC2626)
private val ROJO_CLARO = Color(0xFFF87171)

private val EsquemaClaro = lightColorScheme(
    primary = ZINC_900,
    onPrimary = ZINC_050,
    primaryContainer = ZINC_100,
    onPrimaryContainer = ZINC_900,
    inversePrimary = ZINC_200,

    secondary = ZINC_500,
    onSecondary = BLANCO,
    // El rol de los botones tonales: acá nacía la mitad del violeta de la 002.
    secondaryContainer = ZINC_100,
    onSecondaryContainer = ZINC_900,

    tertiary = ZINC_500,
    onTertiary = BLANCO,
    tertiaryContainer = ZINC_100,
    onTertiaryContainer = ZINC_900,

    background = BLANCO,
    onBackground = ZINC_950,
    surface = BLANCO,
    onSurface = ZINC_950,
    surfaceVariant = ZINC_100,
    onSurfaceVariant = ZINC_500,
    surfaceContainerLowest = BLANCO,
    surfaceContainerLow = ZINC_050,
    surfaceContainer = ZINC_100,
    surfaceContainerHigh = ZINC_100,
    surfaceContainerHighest = ZINC_200,
    // Sin esto la elevación tiñe de violeta cada superficie que se levanta.
    surfaceTint = BLANCO,
    inverseSurface = ZINC_900,
    inverseOnSurface = ZINC_050,

    // El borde es el que hace la jerarquía en este lenguaje visual.
    outline = ZINC_200,
    outlineVariant = ZINC_100,
    scrim = Color(0xFF000000),

    error = ROJO,
    onError = BLANCO,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

private val EsquemaOscuro = darkColorScheme(
    primary = ZINC_050,
    onPrimary = ZINC_900,
    primaryContainer = ZINC_800,
    onPrimaryContainer = ZINC_050,
    inversePrimary = ZINC_800,

    secondary = ZINC_400,
    onSecondary = ZINC_900,
    secondaryContainer = ZINC_800,
    onSecondaryContainer = ZINC_050,

    tertiary = ZINC_400,
    onTertiary = ZINC_900,
    tertiaryContainer = ZINC_800,
    onTertiaryContainer = ZINC_050,

    background = ZINC_950,
    onBackground = ZINC_050,
    surface = ZINC_950,
    onSurface = ZINC_050,
    surfaceVariant = ZINC_800,
    onSurfaceVariant = ZINC_400,
    surfaceContainerLowest = Color(0xFF050506),
    surfaceContainerLow = ZINC_950,
    surfaceContainer = ZINC_900,
    surfaceContainerHigh = ZINC_800,
    surfaceContainerHighest = ZINC_800,
    surfaceTint = ZINC_950,
    inverseSurface = ZINC_050,
    inverseOnSurface = ZINC_900,

    outline = ZINC_800,
    outlineVariant = ZINC_900,
    scrim = Color(0xFF000000),

    error = ROJO_CLARO,
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

/**
 * Esquinas de 8 dp en todo, como el `--radius: 0.5rem` de shadcn.
 *
 * Material 3 trae radios que van de 4 a 28 dp según el componente, y esa variedad es
 * justamente lo que hace que la app se lea como "una app de Material" en vez de como una
 * herramienta. Un solo radio en todo es la mitad del look.
 */
private val Esquinas = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun TemaBuscarPatentes(
    oscuro: Boolean = isSystemInDarkTheme(),
    contenido: @Composable () -> Unit,
) {
    val esquema = if (oscuro) EsquemaOscuro else EsquemaClaro
    MaterialTheme(colorScheme = esquema, shapes = Esquinas) {
        // Un `Text` sin color toma `LocalContentColor`, que sin un `Surface` arriba es negro.
        // Ajustes y Salidas pintan su fondo pero no ponen `Surface`: en oscuro quedaba texto
        // negro sobre fondo casi negro.
        CompositionLocalProvider(LocalContentColor provides esquema.onBackground, content = contenido)
    }
}

/**
 * Los colores con significado del mapa (FR-020, hereda FR-011 de la 002 y FR-035 de la 001).
 *
 * **No cambian con el modo del sistema, y es deliberado.** El fondo **sí** cambia desde la
 * tercera vuelta: `liberty` en claro y `fiord` en oscuro (FR-027), así que la razón original
 * de esta decisión —"el estilo del mapa no se oscurece"— dejó de ser cierta y hay que dar la
 * de verdad.
 *
 * Es que estos seis son los colores que **significan** algo, y su trabajo es distinguirse
 * entre sí antes que combinar con el fondo. Duplicarlos por modo duplicaría el lugar donde
 * se rompe esa distinción, y por un margen chico: son saturados y van sobre un círculo o una
 * línea gruesa, o sea que no dependen del fondo para leerse como dependería un texto. Lo que
 * el FR-029 exige es que se distingan sobre los dos fondos, y eso se verifica mirando el
 * teléfono en los dos modos, no eligiendo dos juegos de colores.
 *
 * Donde el fondo sí obligó a diferenciar, está diferenciado y se dice por qué: [SIN_FONDO] y
 * [SIN_FONDO_OSCURO], que no son señales sino ausencia de mapa, y el gris de [GRUPO], que
 * sobre `fiord` había que aclarar.
 *
 * Viven acá igual, y no sueltos en `mapa/Mapa.kt`, porque son señales del producto —qué
 * está pendiente, qué ya toca, qué se usó— y no decoración. Tenerlos en un solo lugar es
 * lo que impide que se desincronicen de la leyenda cuando alguien retoque uno.
 *
 * **Son los únicos colores saturados de la app**, y el esquema de arriba es monocromo
 * justamente para que no compitan con nada.
 *
 * Por la misma razón no se toman del color dinámico del sistema: dependerían del fondo de
 * pantalla que el jugador tenga puesto.
 */
object ColoresDeMapa {
    // Acá vivían PENDIENTE y COMPARTIDA: el relleno del marcador decía en qué estado estaba
    // el registro. Se fueron en la 004 (FR-030a). El motivo no es de diseño sino de producto:
    // el jugador no sabía que ese color decía algo, y "compartida o no compartida" no es una
    // distinción que use. El relleno del pin es blanco y el borde dice la probabilidad.

    /**
     * Relleno del pin de la patente del número actual (FR-001 de la 005).
     *
     * Se había ido con los otros dos en la 004, que pedía destacarla solo por tamaño. En la
     * calle el tamaño no alcanzó, y el verde es lo que el jugador reconoce como "la que toca".
     * Vuelve solo: el estado de los demás pines sigue sin color. El número va en blanco.
     */
    const val TOCA = "#2F9E44"

    /**
     * Varios registros amontonados, dibujados como un grupo con su cuenta.
     *
     * Gris medio y no casi negro: con el fondo `dark` del mapa, un círculo casi negro
     * desaparecía salvo por su borde blanco (FR-029).
     */
    const val GRUPO = "#495057"

    /**
     * El camino recorrido, en modo cobertura (FR-008a de la 004).
     *
     * Azul de ruta y no el naranja de la 003: es el color que ya significa "camino" para
     * cualquiera que usó un GPS, y deja el naranja libre para que no compita con nada.
     *
     * Acá había además un `RECORRIDO_VIEJO`, un naranja apagado para las salidas anteriores.
     * Se fue con el FR-007a: **todas las salidas se dibujan igual**. Apagar el histórico
     * escondía justo lo que hay que mirar para planear.
     * ponytail: el hex es un punto de partida, se calibra mirando el teléfono al sol.
     */
    const val RECORRIDO = "#1A73E8"

    /**
     * Una cuadra de una zona que falta recorrer (FR-008 y FR-010 de la 008).
     *
     * Naranja, el que la 004 dejó libre al pasar el recorrido a azul: no se confunde con lo
     * caminado ni con la escala de antigüedad. En el mapa de una zona, lo recorrido va con el
     * mismo azul del [RECORRIDO]: azul es "caminado" en todo el mapa.
     * ponytail: se calibra al sol y de noche, contra las avenidas de los dos estilos.
     */
    const val PENDIENTE = "#F76707"

    /** Una cuadra que el jugador quitó de su zona: gris, y con poca opacidad en el mapa. */
    const val QUITADA = "#868E96"

    /** El borde de una zona. Más oscuro que [PENDIENTE] para que no se lea como una cuadra. */
    const val BORDE = "#D9480F"

    /**
     * La escala de antigüedad, en tres escalones (FR-012a de la 004).
     *
     * **Tres colores distintos, no tres intensidades de uno.** Dos intensidades vecinas
     * obligan a comparar un marcador contra otro que esté a la vista, y comparar es
     * exactamente lo que esta feature vino a sacar del medio. Es la misma lección que el
     * FR-037b de la 003 aprendió con el degradado de probabilidad.
     *
     * Familia violeta y magenta, y no rojo/ámbar/verde: esos tres ya dicen otra cosa en el
     * borde del pin, y tener líneas rojas y anillos rojos significando cosas distintas en la
     * misma pantalla es peor que cualquier color feo. Tampoco el azul del [RECORRIDO], que lo
     * prohíbe el FR-012b: así el modo se reconoce mirando el mapa, sin leer la etiqueta.
     *
     * Que el violeta vuelva al mapa no contradice a la 002: lo que esa spec sacó fue el
     * violeta que Material 3 ponía **por default sin que nadie lo escribiera** en fondos y
     * superficies. Los colores del mapa son una paleta aparte y declarada.
     * ponytail: los tres hex se calibran al sol, en claro y en oscuro.
     */
    const val ANTIGUEDAD_RECIENTE = "#8D99AE"

    /** De 4 a 14 días. */
    const val ANTIGUEDAD_MEDIA = "#7048E8"

    /** Más de 14 días: lo que más resalta, porque es lo que hay que ir a revisar (FR-012). */
    const val ANTIGUEDAD_VIEJA = "#D6336C"

    /**
     * Anillo de 0 a 3: vista una vez o pocas, o se pasó y no estaba (FR-037).
     *
     * Rojo y no gris: gris es lo que ya se compartió, y una patente que se fue no es lo
     * mismo que una que ya se usó.
     */
    const val CONFIANZA_BAJA = "#F03E3E"

    /** Anillo de 4 a 7: se la volvió a ver algunas veces. */
    const val CONFIANZA_MEDIA = "#FCC419"

    /** Anillo de 8 a 10: se la volvió a ver. Verde claro, para leerse sobre el relleno de [TOCA]. */
    const val CONFIANZA_ALTA = "#40C057"

    /** Zona sin fondo de mapa guardado (FR-043 de la 001), sobre el fondo claro. */
    const val SIN_FONDO = "#D4DAE0"

    /** Lo mismo sobre el fondo oscuro: un bloque claro ahí sería un cartel, no una señal. */
    const val SIN_FONDO_OSCURO = "#23272B"
}
