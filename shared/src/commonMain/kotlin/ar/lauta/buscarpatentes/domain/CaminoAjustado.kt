package ar.lauta.buscarpatentes.domain

/**
 * El camino ajustado de una salida, tal como se guarda (contrato A de la 007).
 *
 * Hasta la 006 era la polilínea que devolvía el servicio, entera. Esa polilínea pega en una sola
 * línea los pedazos de un camino cortado, y las rectas entre pedazos eran las diagonales que
 * cruzaban manzanas. Desde la 007 son **tramos** armados en la app, y el prefijo [PREFIJO] los
 * distingue de lo viejo sin tocar el esquema de la base.
 */
sealed interface CaminoGuardado {

    /**
     * Los tramos que se dibujan: los guardados si está ajustado, y si no los puntos [medidos]
     * cortados donde se cortó la señal, que es como se dibujó siempre el trazo crudo.
     */
    fun dibujo(medidos: List<Pair<Double, Double>>): List<List<Pair<Double, Double>>> = Geo.tramos(medidos)

    /** Nunca se ajustó, o se ajustó con el criterio de antes de la 007: se dibujan los puntos medidos. */
    data object Pendiente : CaminoGuardado

    /** Se ajustó y ningún punto emparejó con una calle. No se vuelve a pedir. */
    data object NoSePudo : CaminoGuardado

    /** Cada tramo es una línea continua. Entre uno y el siguiente hubo un corte de señal. */
    data class Ajustado(val tramos: List<List<Pair<Double, Double>>>) : CaminoGuardado {
        override fun dibujo(medidos: List<Pair<Double, Double>>) = tramos
    }

    companion object {
        /**
         * `2` y `:` son los caracteres 50 y 58. Una polilínea usa solo del 63 en adelante, así que
         * ningún camino viejo puede empezar así.
         */
        const val PREFIJO = "2:"

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
