package ar.lauta.buscarpatentes.domain

import kotlin.math.cos

/**
 * El borde de una zona: las esquinas que tocó el jugador, en orden, cerradas contra la primera
 * (FR-001, D4 de la 008).
 *
 * Las posiciones son pares de latitud y longitud, como en el resto del dominio.
 */
object Borde {

    const val FALTAN_ESQUINAS = "Faltan esquinas"
    const val SE_CRUZA = "El borde se cruza"

    /** Lo que se pide alrededor del borde, para que las cuadras del borde lleguen hasta su esquina (D1). */
    const val MARGEN_M = 200.0

    /** Por qué el dibujo todavía no es una zona, o null si ya lo es. */
    fun problema(esquinas: List<Pair<Double, Double>>): String? = when {
        esquinas.size < 3 -> FALTAN_ESQUINAS
        seCruza(esquinas) -> SE_CRUZA
        else -> null
    }

    /**
     * Si la posición cae adentro, por el método del rayo: se cuentan los lados que cruza una
     * semirrecta hacia el este. En pocos km, hacerlo sobre grados no cambia el resultado.
     */
    fun adentro(posicion: Pair<Double, Double>, esquinas: List<Pair<Double, Double>>): Boolean {
        val (lat, lon) = posicion
        var adentro = false
        var j = esquinas.lastIndex
        for (i in esquinas.indices) {
            val (latI, lonI) = esquinas[i]
            val (latJ, lonJ) = esquinas[j]
            if ((latI > lat) != (latJ > lat) && lon < (lonJ - lonI) * (lat - latI) / (latJ - latI) + lonI) {
                adentro = !adentro
            }
            j = i
        }
        return adentro
    }

    data class Rectangulo(val sur: Double, val oeste: Double, val norte: Double, val este: Double)

    /** El rectángulo que envuelve el borde, agrandado [margenM] metros para cada lado. */
    fun rectangulo(esquinas: List<Pair<Double, Double>>, margenM: Double = MARGEN_M): Rectangulo {
        val sur = esquinas.minOf { it.first }
        val norte = esquinas.maxOf { it.first }
        val dLat = margenM / Plano.METROS_POR_GRADO
        val dLon = margenM / (Plano.METROS_POR_GRADO * cos(((sur + norte) / 2).aRadianes()))
        return Rectangulo(
            sur = sur - dLat,
            oeste = esquinas.minOf { it.second } - dLon,
            norte = norte + dLat,
            este = esquinas.maxOf { it.second } + dLon,
        )
    }

    fun escribir(esquinas: List<Pair<Double, Double>>): String = Polilinea.codificar(esquinas)

    fun leer(texto: String): List<Pair<Double, Double>> = Polilinea.decodificar(texto)

    /**
     * El tramo del borde que pasa a [tolerancia] o menos de [toque], el más cercano, o null. Todo en
     * las mismas unidades, las de la pantalla. El tramo i va de la esquina i a la siguiente; con tres
     * esquinas o más, el último cierra contra la primera. Partirlo es poner la esquina nueva en el
     * lugar i + 1 (FR-001).
     */
    internal fun tramoTocado(toque: Punto, esquinas: List<Punto>, tolerancia: Double): Int? {
        if (esquinas.size < 2) return null
        val tramos = if (esquinas.size >= 3) esquinas.size else 1
        return (0 until tramos)
            .map { i -> i to distanciaASegmento(toque, esquinas[i], esquinas[(i + 1) % esquinas.size]) }
            .filter { (_, d) -> d <= tolerancia }
            .minByOrNull { (_, d) -> d }
            ?.first
    }

    /** Si algún par de lados que no son vecinos se toca. */
    private fun seCruza(esquinas: List<Pair<Double, Double>>): Boolean {
        val n = esquinas.size
        val lados = (0 until n).map { esquinas[it] to esquinas[(it + 1) % n] }
        for (i in 0 until n) {
            for (j in i + 2 until n) {
                if (i == 0 && j == n - 1) continue // el primero y el último son vecinos
                if (seTocan(lados[i].first, lados[i].second, lados[j].first, lados[j].second)) return true
            }
        }
        return false
    }

    private fun seTocan(a: Pair<Double, Double>, b: Pair<Double, Double>, c: Pair<Double, Double>, d: Pair<Double, Double>): Boolean {
        val o1 = orientacion(a, b, c)
        val o2 = orientacion(a, b, d)
        val o3 = orientacion(c, d, a)
        val o4 = orientacion(c, d, b)
        if (o1 != o2 && o3 != o4) return true
        return (o1 == 0 && sobre(a, c, b)) || (o2 == 0 && sobre(a, d, b)) ||
            (o3 == 0 && sobre(c, a, d)) || (o4 == 0 && sobre(c, b, d))
    }

    private fun orientacion(p: Pair<Double, Double>, q: Pair<Double, Double>, r: Pair<Double, Double>): Int {
        val v = (q.second - p.second) * (r.first - q.first) - (q.first - p.first) * (r.second - q.second)
        return if (v > 0) 1 else if (v < 0) -1 else 0
    }

    /** Si [q], colineal con [p] y [r], cae entre los dos. */
    private fun sobre(p: Pair<Double, Double>, q: Pair<Double, Double>, r: Pair<Double, Double>) =
        q.first in minOf(p.first, r.first)..maxOf(p.first, r.first) &&
            q.second in minOf(p.second, r.second)..maxOf(p.second, r.second)
}
