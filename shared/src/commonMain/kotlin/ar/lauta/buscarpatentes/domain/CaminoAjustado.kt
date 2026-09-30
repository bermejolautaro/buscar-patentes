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

/** Cómo se arma un tramo con la respuesta del servicio de ajuste (contrato A4 de la 007). */
object CaminoAjustado {

    /**
     * Un tramo continuo sin rectas inventadas, a partir de los puntos [medidos] de un tramo de
     * señal y lo que contestó el servicio.
     *
     * - [forma]: las posiciones pegadas a las calles, en orden.
     * - [aristas]: el rango de [forma] de cada arista. Donde una arista no empieza en el índice
     *   donde terminó la anterior, el servicio cortó el camino: son dos **pedazos**, y la forma
     *   los trae pegados uno atrás del otro. Esa recta era la diagonal.
     * - [emparejados]: para cada punto medido, la arista donde emparejó, o null.
     *
     * Los pedazos se unen con los puntos medidos: del último que emparejó con un pedazo al
     * primero que emparejó con el siguiente, los dos incluidos. Lo que no emparejó al principio
     * o al final va con su posición medida, en lugar de desaparecer.
     */
    fun armar(
        medidos: List<Pair<Double, Double>>,
        forma: List<Pair<Double, Double>>,
        aristas: List<IntRange>,
        emparejados: List<Int?>,
    ): List<Pair<Double, Double>> {
        // Qué pedazo es cada arista, y qué rango de la forma ocupa cada pedazo.
        val pedazoDeArista = IntArray(aristas.size)
        val pedazos = mutableListOf<IntRange>()
        aristas.forEachIndexed { i, arista ->
            if (i == 0) {
                pedazos += arista
            } else if (arista.first != aristas[i - 1].last) {
                pedazos += arista
            } else {
                pedazos[pedazos.lastIndex] = pedazos.last().first..arista.last
            }
            pedazoDeArista[i] = pedazos.lastIndex
        }

        val pedazoDe = emparejados.map { arista -> arista?.let { pedazoDeArista.getOrNull(it) } }
        val primero = pedazoDe.indexOfFirst { it != null }
        if (primero < 0) return medidos

        val tramo = mutableListOf<Pair<Double, Double>>()
        fun sumar(posiciones: List<Pair<Double, Double>>) =
            posiciones.forEach { if (tramo.lastOrNull() != it) tramo += it }

        if (primero > 0) sumar(medidos.subList(0, primero + 1))
        var actual = pedazoDe[primero]!!
        sumar(forma.slice(pedazos[actual]))
        var ultimo = primero
        for (i in primero + 1 until medidos.size) {
            val pedazo = pedazoDe[i] ?: continue
            if (pedazo != actual) {
                sumar(medidos.subList(ultimo, i + 1))
                actual = pedazo
                sumar(forma.slice(pedazos[actual]))
            }
            ultimo = i
        }
        if (ultimo < medidos.lastIndex) sumar(medidos.subList(ultimo, medidos.size))
        return tramo
    }
}
