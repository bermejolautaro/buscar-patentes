package ar.lauta.buscarpatentes.domain

import kotlin.math.ceil
import kotlin.math.floor

/** Una cuadra de la zona, como la necesita la cuenta. */
data class CuadraParaContar(
    val id: Long,
    val forma: List<Pair<Double, Double>>,
    /** El id de la principal, si es la otra mano de una avenida (D3). */
    val gemelaDe: Long?,
    val quitada: Boolean,
)

/** Una salida ajustada, con su fecha y sus pedazos de calle (CaminoGuardado.Ajustado de la 007). */
data class SalidaParaContar(
    val iniciadoEn: Long,
    val tramos: List<List<Pair<Double, Double>>>,
)

/** Cómo va una zona. Se calcula cada vez y no se guarda (D7 de la 008). */
data class CuentaDeZona(
    /** Las cuadras recorridas, principales y gemelas, quitadas o no: es lo que se pinta. */
    val recorridas: Set<Long>,
    /** Las cuadras que cuentan: una por pareja de gemelas, sin las quitadas. */
    val total: Int,
    /** Redondeado hacia abajo: no dice 100 mientras falte una (FR-007). */
    val porcentaje: Int,
    val faltan: Int,
    /** Sin ninguna que falte, la fecha de la última cuadra recorrida (D6). Si no, null. */
    val completadaEn: Long?,
)

/**
 * Qué cuadras de una zona están recorridas (contrato Z3 de la 008).
 *
 * Cada cuadra se muestrea cada 5 m. Una muestra queda cubierta por la **primera** salida que pasa a
 * 10 m o menos, y la cuadra está recorrida con el 80% de sus muestras cubiertas. Las salidas se
 * suman: media cuadra un día y la otra media otro día la recorren (D5).
 *
 * Con la respuesta real del servicio de ajuste, una cuadra caminada de esquina a esquina da 100%
 * cubierto, y una cruzada en la esquina 14% o menos.
 *
 * ponytail: los 10 m y el 80% son constantes. Si una salida real deja afuera una cuadra caminada,
 * se calibran contra esa salida.
 */
object CoberturaDeZona {

    private const val PASO_M = 5.0
    private const val TOLERANCIA_M = 10.0
    private const val UMBRAL = 0.8

    /** Más grande que la tolerancia: una muestra solo mira su celda y las ocho vecinas. */
    private const val CELDA_M = 50.0

    fun calcular(cuadras: List<CuadraParaContar>, salidas: List<SalidaParaContar>, desde: Long): CuentaDeZona {
        if (cuadras.isEmpty()) return CuentaDeZona(emptySet(), 0, 0, 0, null)
        val plano = Plano(cuadras.first().forma.first().first)
        val lineas = cuadras.associate { it.id to plano.linea(it.forma) }
        val grilla = grilla(salidas.filter { it.iniciadoEn >= desde }, plano, caja(lineas.values.flatten()))

        // La fecha en que cada cuadra llegó al 80% cubierto, o null.
        val fecha = cuadras.associate { c -> c.id to fechaDeRecorrida(lineas.getValue(c.id), grilla) }

        // Una pareja de gemelas es una sola cuadra: la principal, con la fecha más temprana de las dos.
        val porPrincipal = cuadras.groupBy { it.gemelaDe ?: it.id }
        val fechaDe = porPrincipal.mapValues { (_, pareja) -> pareja.mapNotNull { fecha[it.id] }.minOrNull() }
        val principales = cuadras.filter { it.gemelaDe == null }
        val cuentan = principales.filter { !it.quitada }
        val recorridasQueCuentan = cuentan.mapNotNull { fechaDe[it.id] }
        val total = cuentan.size
        val faltan = total - recorridasQueCuentan.size
        return CuentaDeZona(
            recorridas = cuadras.filter { fechaDe[it.gemelaDe ?: it.id] != null }.map { it.id }.toSet(),
            total = total,
            porcentaje = if (total == 0) 0 else recorridasQueCuentan.size * 100 / total,
            faltan = faltan,
            completadaEn = if (total > 0 && faltan == 0) recorridasQueCuentan.max() else null,
        )
    }

    private class Segmento(val a: Punto, val b: Punto, val iniciadoEn: Long)

    private class Caja(val oeste: Double, val sur: Double, val este: Double, val norte: Double) {
        fun toca(otra: Caja) = oeste <= otra.este && otra.oeste <= este && sur <= otra.norte && otra.sur <= norte
    }

    private fun caja(puntos: List<Punto>) = Caja(
        puntos.minOf { it.x } - TOLERANCIA_M,
        puntos.minOf { it.y } - TOLERANCIA_M,
        puntos.maxOf { it.x } + TOLERANCIA_M,
        puntos.maxOf { it.y } + TOLERANCIA_M,
    )

    /**
     * Los segmentos de las salidas, repartidos en celdas de [CELDA_M]. Un segmento va a todas las
     * celdas que toca su rectángulo. Las salidas que no tocan la zona ni se miran.
     */
    private fun grilla(salidas: List<SalidaParaContar>, plano: Plano, zona: Caja): Map<Long, List<Segmento>> {
        val grilla = HashMap<Long, MutableList<Segmento>>()
        for (salida in salidas) {
            val tramos = salida.tramos.filter { it.size >= 2 }.map(plano::linea)
            if (tramos.isEmpty() || !caja(tramos.flatten()).toca(zona)) continue
            for (tramo in tramos) {
                for ((a, b) in tramo.zipWithNext()) {
                    val segmento = Segmento(a, b, salida.iniciadoEn)
                    for (cx in celda(minOf(a.x, b.x))..celda(maxOf(a.x, b.x))) {
                        for (cy in celda(minOf(a.y, b.y))..celda(maxOf(a.y, b.y))) {
                            grilla.getOrPut(clave(cx, cy)) { mutableListOf() } += segmento
                        }
                    }
                }
            }
        }
        return grilla
    }

    private fun fechaDeRecorrida(linea: List<Punto>, grilla: Map<Long, List<Segmento>>): Long? {
        val puntos = muestras(linea, PASO_M)
        val fechas = puntos.mapNotNull { primeraQueCubre(it, grilla) }.sorted()
        val hacenFalta = ceil(puntos.size * UMBRAL - 1e-9).toInt()
        return if (fechas.size >= hacenFalta) fechas[hacenFalta - 1] else null
    }

    private fun primeraQueCubre(p: Punto, grilla: Map<Long, List<Segmento>>): Long? {
        var primera: Long? = null
        val cx = celda(p.x)
        val cy = celda(p.y)
        for (dx in -1..1) {
            for (dy in -1..1) {
                val segmentos = grilla[clave(cx + dx, cy + dy)] ?: continue
                for (s in segmentos) {
                    if ((primera == null || s.iniciadoEn < primera) && distanciaASegmento(p, s.a, s.b) <= TOLERANCIA_M) {
                        primera = s.iniciadoEn
                    }
                }
            }
        }
        return primera
    }

    private fun celda(metros: Double): Int = floor(metros / CELDA_M).toInt()

    private fun clave(cx: Int, cy: Int): Long = (cx.toLong() shl 32) or (cy.toLong() and 0xffffffffL)
}
