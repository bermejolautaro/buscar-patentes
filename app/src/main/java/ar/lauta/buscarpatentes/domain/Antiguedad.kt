package ar.lauta.buscarpatentes.domain

import java.util.concurrent.TimeUnit

/**
 * Hace cuánto se caminó una calle, en tres escalones (FR-011 a FR-016 de la 004).
 *
 * Tres y no un degradado: la pregunta que el jugador se hace es "¿hace mucho o hace poco?",
 * y una rampa de tonos no la contesta mejor. Es la misma lección que la 003 aprendió con el
 * color de probabilidad de los marcadores —el degradado no se leía porque un color solo se
 * puede juzgar contra otro que esté a la vista— aplicada al trazo.
 *
 * El orden de las constantes **es** el orden de la escala, y de eso depende el
 * `Expression.step` que pinta la capa: de lo más reciente a lo más viejo.
 */
enum class Escalon {
    /** Hasta 3 días. Recién pisado: no es lo que hay que ir a revisar. */
    RECIENTE,

    /** De 4 a 14 días. */
    MEDIO,

    /** Más de 14 días. Lo que más resalta en el mapa, porque es lo que hay que revisar. */
    VIEJO,
}

/**
 * Funciones puras sobre el tiempo transcurrido. No saben de Room ni de Android.
 *
 * Vive en `domain/` por eso mismo: es la única lógica no trivial de la 004, y la constitución
 * pide que toda lógica no trivial deje una prueba que corra en JVM.
 */
object Antiguedad {

    /**
     * Hasta acá una salida cuenta como recién caminada.
     *
     * Tres días y no una semana: las patentes rotan en días, y el corte separa lo caminado
     * el fin de semana de lo de la semana anterior.
     * ponytail: calibrable contra el uso real, no una constante sagrada.
     */
    const val CORTE_RECIENTE_DIAS = 3L

    /** Pasados estos días la calle merece revisarse. */
    const val CORTE_MEDIO_DIAS = 14L

    /**
     * En qué escalón cae una salida.
     *
     * La fecha de referencia es la de fin, **y la de inicio cuando todavía no terminó**: una
     * salida en curso tiene antigüedad cero por definición, así que cae en [Escalon.RECIENTE]
     * sin ser un caso aparte.
     *
     * Una fecha futura —el reloj del teléfono corrido hacia atrás, o una zona horaria rara—
     * da días negativos y cae también en [Escalon.RECIENTE]. Es lo correcto y no hace falta
     * tratarla: "algo del futuro" es, para esta pregunta, lo más reciente que hay.
     */
    fun escalon(finalizadoEn: Long?, iniciadoEn: Long, ahora: Long): Escalon {
        val dias = diasDesde(finalizadoEn ?: iniciadoEn, ahora)
        return when {
            dias <= CORTE_RECIENTE_DIAS -> Escalon.RECIENTE
            dias <= CORTE_MEDIO_DIAS -> Escalon.MEDIO
            else -> Escalon.VIEJO
        }
    }

    /**
     * El mismo lapso, dicho como lo diría una persona (FR-019).
     *
     * "hace 3 días" y no "27 ago 2026, 18:40": la pregunta que el jugador trae a la pantalla
     * de Salidas es *hace cuánto que no salgo*, y una fecha absoluta lo obliga a hacer la
     * resta él. La fecha exacta sigue estando en el detalle de la salida, que es donde
     * importa cuándo fue y no cuánto hace.
     *
     * Pasado el mes se cambia de unidad: "hace 47 días" es un número que nadie convierte a
     * intuición, y a esa distancia la precisión de un día ya no significa nada.
     */
    fun hace(momento: Long, ahora: Long): String {
        val dias = diasDesde(momento, ahora)
        return when {
            dias <= 0 -> "hoy"
            dias == 1L -> "ayer"
            dias < 30 -> "hace $dias días"
            dias < 60 -> "hace un mes"
            dias < 365 -> "hace ${dias / 30} meses"
            dias < 730 -> "hace un año"
            else -> "hace ${dias / 365} años"
        }
    }

    /**
     * Días cumplidos entre dos momentos. Trunca hacia abajo: a las 25 horas todavía es 1.
     *
     * Truncar y no redondear es lo que hace que "hasta 3 días" signifique lo que dice: una
     * salida de hace 3 días y 20 horas sigue siendo reciente hasta que cumple los 4.
     */
    fun diasDesde(momento: Long, ahora: Long): Long =
        TimeUnit.MILLISECONDS.toDays(ahora - momento)
}
