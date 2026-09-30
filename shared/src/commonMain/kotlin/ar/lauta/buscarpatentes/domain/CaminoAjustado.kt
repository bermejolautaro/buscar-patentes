package ar.lauta.buscarpatentes.domain

/**
 * El camino ajustado de una salida, tal como se guarda (contrato A de la 007).
 *
 * Hasta la 006 era la polilínea que devolvía el servicio, entera. Esa polilínea pega en una sola
 * línea los pedazos de un camino cortado, y las rectas entre pedazos eran las diagonales que
 * cruzaban manzanas. Desde la 007 son solo los **pedazos de calle** que el servicio emparejó, cada
 * uno por su lado, y el prefijo [PREFIJO] los distingue de lo viejo sin tocar el esquema de la base.
 */
sealed interface CaminoGuardado {

    /** Nunca se ajustó, o se ajustó con un criterio anterior: se dibujan los puntos medidos. */
    data object Pendiente : CaminoGuardado

    /** Se ajustó y ningún tramo emparejó con una calle. No pinta nada, y no se vuelve a pedir. */
    data object NoSePudo : CaminoGuardado

    /** Cada tramo es un pedazo de calle, y se dibuja solo: entre uno y otro no se une nada. */
    data class Ajustado(val tramos: List<List<Pair<Double, Double>>>) : CaminoGuardado

    companion object {
        /**
         * `3` y `:` son los caracteres 51 y 58. Una polilínea usa solo del 63 en adelante, así que
         * ningún camino viejo puede empezar así. Fue `2:` mientras los tramos llevaban los puntos
         * medidos entre pedazos; esos se leen [Pendiente] y se reajustan.
         */
        const val PREFIJO = "3:"

        fun leer(texto: String?): CaminoGuardado {
            if (texto == null || !texto.startsWith(PREFIJO)) return Pendiente
            val tramos = texto.removePrefix(PREFIJO)
                .split(Polilinea.SEPARADOR_PIERNAS)
                .map { Polilinea.decodificar(it) }
                .filter { it.size >= 2 }
            return if (tramos.isEmpty()) NoSePudo else Ajustado(tramos)
        }

        /** Sin tramos da [PREFIJO] solo, que se lee [NoSePudo]. */
        fun escribir(tramos: List<List<Pair<Double, Double>>>): String =
            PREFIJO + tramos.joinToString(Polilinea.SEPARADOR_PIERNAS) { Polilinea.codificar(it) }
    }
}

/** Cómo se lee la respuesta del servicio de ajuste (contrato A4 de la 007). */
object CaminoAjustado {

    /**
     * Los pedazos de calle de una respuesta: [forma] cortada donde una arista no empieza en el
     * índice donde terminó la anterior. Ahí el servicio no pudo unir el camino, y la forma trae
     * los dos pedazos pegados uno atrás del otro: esa recta era la diagonal.
     *
     * Lo que no emparejó no aparece: el mapa por defecto pinta calles, y los puntos medidos se
     * ven con el interruptor (aclaración de la spec, sesión del 2026-09-29).
     */
    fun pedazos(forma: List<Pair<Double, Double>>, aristas: List<IntRange>): List<List<Pair<Double, Double>>> {
        val rangos = mutableListOf<IntRange>()
        for (arista in aristas) {
            val ultimo = rangos.lastOrNull()
            if (ultimo != null && arista.first == ultimo.last) {
                rangos[rangos.lastIndex] = ultimo.first..arista.last
            } else {
                rangos += arista
            }
        }
        return rangos.map { forma.slice(it) }.filter { it.size >= 2 }
    }
}
