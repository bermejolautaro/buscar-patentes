package ar.lauta.buscarpatentes.domain

import kotlin.math.cos
import kotlin.math.hypot

/**
 * Coordenadas planas en metros alrededor de una latitud, para las cuentas de la zona (008).
 *
 * En una zona de pocos km, proyectar con el coseno de la latitud se equivoca en centímetros, y deja
 * medir la distancia a un segmento y repartir en una grilla con sumas y restas. Sobre grados
 * crudos el error sería direccional: ver [Geo.distanciaMetros].
 *
 * Lo usan [Cuadras] y [CoberturaDeZona].
 */
internal class Plano(latitudDeReferencia: Double) {

    private val metrosPorGradoDeLongitud = METROS_POR_GRADO * cos(latitudDeReferencia.aRadianes())

    fun punto(posicion: Pair<Double, Double>) =
        Punto(posicion.second * metrosPorGradoDeLongitud, posicion.first * METROS_POR_GRADO)

    fun linea(forma: List<Pair<Double, Double>>): List<Punto> = forma.map(::punto)

    fun posicion(punto: Punto): Pair<Double, Double> =
        (punto.y / METROS_POR_GRADO) to (punto.x / metrosPorGradoDeLongitud)

    companion object {
        /** Un grado de latitud, con el mismo radio de la Tierra que [Geo.distanciaMetros]. */
        const val METROS_POR_GRADO = 111_194.93
    }
}

internal data class Punto(val x: Double, val y: Double)

internal fun distancia(a: Punto, b: Punto): Double = hypot(b.x - a.x, b.y - a.y)

internal fun distanciaASegmento(p: Punto, a: Punto, b: Punto): Double {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val largo2 = dx * dx + dy * dy
    val t = if (largo2 == 0.0) 0.0 else (((p.x - a.x) * dx + (p.y - a.y) * dy) / largo2).coerceIn(0.0, 1.0)
    return hypot(p.x - a.x - t * dx, p.y - a.y - t * dy)
}

internal fun distanciaALinea(p: Punto, linea: List<Punto>): Double =
    if (linea.size == 1) distancia(p, linea[0]) else linea.zipWithNext { a, b -> distanciaASegmento(p, a, b) }.min()

internal fun largo(linea: List<Punto>): Double = linea.zipWithNext(::distancia).sum()

/** Un punto cada [paso] metros a lo largo de la línea, desde la primera posición hasta la última. */
internal fun muestras(linea: List<Punto>, paso: Double): List<Punto> {
    if (linea.size < 2) return linea
    val salida = mutableListOf<Punto>()
    for ((a, b) in linea.zipWithNext()) {
        val partes = maxOf(1, (distancia(a, b) / paso).toInt())
        for (i in 0 until partes) {
            val t = i.toDouble() / partes
            salida += Punto(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
        }
    }
    return salida + linea.last()
}
