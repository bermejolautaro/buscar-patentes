package ar.lauta.buscarpatentes.domain

/** Una vía del mapa de calles, como la devuelve Overpass: sus nodos y la posición de cada uno. */
data class Via(
    val nodos: List<Long>,
    val posiciones: List<Pair<Double, Double>>,
    val nombre: String?,
    val unaMano: Boolean,
)

/**
 * Una cuadra recién armada, antes de guardarse. [gemelaDe] es el **índice** de su principal en la
 * lista que devuelve [Cuadras.armar]: el id recién existe cuando se guarda.
 */
data class CuadraArmada(
    val nombre: String?,
    val forma: List<Pair<Double, Double>>,
    val gemelaDe: Int? = null,
)

/**
 * De las vías del mapa a las cuadras de una zona (contrato Z2 de la 008).
 *
 * Una cuadra corre entre dos nodos de grado distinto de 2: una esquina, o el final de una calle
 * sin salida (D2). Las dos manos de una avenida con bulevar son gemelas y cuentan una vez (D3).
 */
object Cuadras {

    /** ponytail: fijo. Si una cuadra real y corta queda afuera, se baja (D2). */
    const val LARGO_MINIMO_M = 30.0

    private const val PASO_M = 5.0
    private const val PARTE_ADENTRO = 0.5

    // Gemelas (D3): la otra mano corre paralela a entre 5 y 40 m, en el 60% de su largo o más.
    private const val GEMELA_MINIMO_M = 5.0
    private const val GEMELA_MAXIMO_M = 40.0
    private const val GEMELA_PARTE = 0.6

    fun armar(vias: List<Via>, borde: List<Pair<Double, Double>>): List<CuadraArmada> {
        val cadenas = cadenas(vias)
        if (cadenas.isEmpty()) return emptyList()
        val plano = Plano(cadenas.first().posiciones.first().first)
        val deLaZona = cadenas
            .map { it to plano.linea(it.posiciones) }
            .filter { (_, linea) -> largo(linea) >= LARGO_MINIMO_M }
            .filter { (cadena, linea) -> esDeLaZona(cadena, linea, plano, borde) }
        return conGemelas(deLaZona)
    }

    /** Una cuadra armada con lo que hace falta para buscarle gemela. */
    private class Cadena(val nodos: List<Long>, val posiciones: List<Pair<Double, Double>>, val nombre: String?, val unaMano: Boolean)

    /** Pasos 1 y 2 del Z2: el grafo de las vías, y las cadenas de segmentos entre nodos de grado distinto de 2. */
    private fun cadenas(vias: List<Via>): List<Cadena> {
        class Segmento(val a: Long, val b: Long, val via: Via)

        val posicion = HashMap<Long, Pair<Double, Double>>()
        val segmentos = mutableListOf<Segmento>()
        for (via in vias) {
            via.nodos.zip(via.posiciones).forEach { (nodo, p) -> posicion[nodo] = p }
            via.nodos.zipWithNext { a, b -> if (a != b) segmentos += Segmento(a, b, via) }
        }
        val grado = HashMap<Long, Int>()
        val tocan = HashMap<Long, MutableList<Int>>()
        segmentos.forEachIndexed { i, s ->
            for (nodo in listOf(s.a, s.b)) {
                grado[nodo] = (grado[nodo] ?: 0) + 1
                tocan.getOrPut(nodo) { mutableListOf() } += i
            }
        }

        val usado = BooleanArray(segmentos.size)
        /** Sigue desde [nodo], que se alcanzó por [desde], mientras el nodo sea un punto del medio. */
        fun seguir(nodo: Long, desde: Int): List<Long> {
            val camino = mutableListOf<Long>()
            var actual = nodo
            var previo = desde
            while (grado[actual] == 2) {
                val siguiente = tocan.getValue(actual).firstOrNull { it != previo && !usado[it] } ?: break
                usado[siguiente] = true
                val s = segmentos[siguiente]
                actual = if (s.a == actual) s.b else s.a
                camino += actual
                previo = siguiente
            }
            return camino
        }

        val salida = mutableListOf<Cadena>()
        for ((i, s) in segmentos.withIndex()) {
            if (usado[i]) continue
            usado[i] = true
            val nodos = seguir(s.a, i).asReversed() + listOf(s.a, s.b) + seguir(s.b, i)
            salida += Cadena(nodos, nodos.map { posicion.getValue(it) }, s.via.nombre, s.via.unaMano)
        }
        return salida
    }

    /** Paso 4 del Z2: la mitad de las muestras, o más, adentro del borde (D4). */
    private fun esDeLaZona(cadena: Cadena, linea: List<Punto>, plano: Plano, borde: List<Pair<Double, Double>>): Boolean {
        val puntos = muestras(linea, PASO_M)
        val adentro = puntos.count { p -> Borde.adentro(plano.posicion(p), borde) }
        return adentro >= puntos.size * PARTE_ADENTRO
    }

    /** Paso 5 del Z2: aparea las dos manos de una avenida. La de índice menor es la principal. */
    private fun conGemelas(cuadras: List<Pair<Cadena, List<Punto>>>): List<CuadraArmada> {
        val gemelaDe = arrayOfNulls<Int>(cuadras.size)
        val tieneGemela = BooleanArray(cuadras.size)
        for (i in cuadras.indices) {
            if (gemelaDe[i] != null || tieneGemela[i]) continue
            for (j in i + 1 until cuadras.size) {
                if (gemelaDe[j] != null || tieneGemela[j]) continue
                if (sonGemelas(cuadras[i], cuadras[j])) {
                    gemelaDe[j] = i
                    tieneGemela[i] = true
                    break
                }
            }
        }
        return cuadras.mapIndexed { i, (cadena, _) -> CuadraArmada(cadena.nombre, cadena.posiciones, gemelaDe[i]) }
    }

    private fun sonGemelas(a: Pair<Cadena, List<Punto>>, b: Pair<Cadena, List<Punto>>): Boolean {
        val (ca, la) = a
        val (cb, lb) = b
        if (!ca.unaMano || !cb.unaMano || ca.nombre == null || ca.nombre != cb.nombre) return false
        val puntasA = setOf(ca.nodos.first(), ca.nodos.last())
        if (cb.nodos.first() in puntasA || cb.nodos.last() in puntasA) return false
        if (distancia(la[la.size / 2], lb[lb.size / 2]) > largo(la) + largo(lb)) return false
        return paralela(la, lb) && paralela(lb, la)
    }

    /**
     * Si al menos el 60% de [a] corre a entre 5 y 40 m de [b], contra el medio de [b] y no contra
     * una de sus puntas: así dos cuadras seguidas de la misma calle no se aparean.
     */
    private fun paralela(a: List<Punto>, b: List<Punto>): Boolean {
        val puntos = muestras(a, PASO_M)
        val cerca = puntos.count { p ->
            val d = distanciaALinea(p, b)
            val aLaPunta = minOf(distancia(p, b.first()), distancia(p, b.last()))
            d in GEMELA_MINIMO_M..GEMELA_MAXIMO_M && aLaPunta > d + 1.0
        }
        return cerca >= puntos.size * GEMELA_PARTE
    }

    /** ponytail: fijo. Dos esquinas de una misma cuadra en dos búsquedas caen en el mismo nodo. */
    private const val MISMA_PUNTA_M = 5.0

    /**
     * Si [a] y [b] son la misma cuadra, en cualquier sentido: las dos puntas a 5 m o menos. Con el
     * borde nuevo se vuelven a buscar las cuadras, y así lo quitado sigue quitado (FR-022).
     */
    fun misma(a: List<Pair<Double, Double>>, b: List<Pair<Double, Double>>): Boolean {
        if (a.isEmpty() || b.isEmpty()) return false
        val plano = Plano(a.first().first)
        fun cerca(p: Pair<Double, Double>, q: Pair<Double, Double>) = distancia(plano.punto(p), plano.punto(q)) <= MISMA_PUNTA_M
        return (cerca(a.first(), b.first()) && cerca(a.last(), b.last())) ||
            (cerca(a.first(), b.last()) && cerca(a.last(), b.first()))
    }
}
