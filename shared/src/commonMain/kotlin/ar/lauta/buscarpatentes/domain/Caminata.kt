package ar.lauta.buscarpatentes.domain

import kotlin.math.roundToInt

/** Cuánto llevó una salida: su tiempo, sus metros y, con el peso, sus calorías (D6 de la 010). */
data class ResumenDeCaminata(val duracionMs: Long, val metros: Double, val kcal: Int?)

/**
 * Las cuentas de una salida (contrato C1 y C2 de la 010). Se hacen siempre sobre lo guardado y no
 * se guardan: así las salidas viejas también las tienen, y cambiar el peso las cambia a todas.
 *
 * Una sola cuenta para la barra, la confirmación de terminar, Salidas y las estadísticas: los km
 * de una semana no pueden dar distinto que la suma de sus salidas (SC-006).
 */
object Caminata {

    /**
     * El costo neto de caminar en llano, en kcal por kg y por km: la ecuación del ACSM, 0,1 ml de
     * O₂ por kg y por metro, a 5 kcal por litro (D2). Solo las calorías activas: quieto no suma.
     */
    private const val KCAL_POR_KG_Y_KM = 0.5

    /** Las calorías se redondean a esto: dice que es un aproximado sin agregar texto. */
    private const val REDONDEO_KCAL = 5

    /**
     * Los metros caminados (D1): la suma adentro de cada tramo de [Geo.tramos], el mismo corte que
     * dibuja los huecos punteados. Un hueco no se sabe por dónde se caminó, así que no suma.
     */
    fun metros(puntos: List<Pair<Double, Double>>): Double =
        Geo.tramos(puntos).sumOf { tramo ->
            tramo.zipWithNext { (latA, lonA), (latB, lonB) -> Geo.distanciaMetros(latA, lonA, latB, lonB) }.sum()
        }

    /** Las calorías activas de [metros] con [pesoKg], o null sin peso: ningún número inventado (FR-009). */
    fun kcal(metros: Double, pesoKg: Int?): Int? {
        val peso = pesoKg ?: return null
        val kcal = KCAL_POR_KG_Y_KM * peso * metros / 1000
        return (kcal / REDONDEO_KCAL).roundToInt() * REDONDEO_KCAL
    }

    /** El resumen de una salida. Una en curso, sin [finalizadoEn], cuenta hasta [ahora]. */
    fun resumen(
        iniciadoEn: Long,
        finalizadoEn: Long?,
        puntos: List<Pair<Double, Double>>,
        pesoKg: Int?,
        ahora: Long,
    ): ResumenDeCaminata {
        val metros = metros(puntos)
        return ResumenDeCaminata((finalizadoEn ?: ahora) - iniciadoEn, metros, kcal(metros, pesoKg))
    }
}
