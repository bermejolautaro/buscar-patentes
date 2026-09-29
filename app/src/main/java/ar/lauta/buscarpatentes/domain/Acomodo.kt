package ar.lauta.buscarpatentes.domain

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.tan

/**
 * Dónde dibujar cada patente en un zoom: suelta y un poco corrida, o adentro de un grupo
 * (D13 de la 005, US5).
 *
 * El jugador lo pidió así: *"que las patentes puedan empujarse entre sí un poco para mostrarse
 * sin agruparse, y después de ese threshold ya agruparse. Mientras la patente quede sobre la
 * misma cuadra ya es suficiente para entender dónde está"*. MapLibre no trae nada que haga eso:
 * o agrupa, o corre la etiqueta y **esconde** la que no entra, y una patente que desaparece del
 * mapa es justo el problema que el jugador reportó.
 *
 * ## Cómo
 *
 * 1. Se proyectan los pines a dp del mundo en ese zoom (Web Mercator, teselas de 512).
 * 2. Se arman los conjuntos de pines que se encimarían: a menos de un ancho de pin, en cadena.
 * 3. Cada conjunto se intenta separar por repulsión, de a pares.
 * 4. Si se separa sin que ninguno se corra más de [LIMITE_ACOMODO_M] en el terreno, quedan
 *    sueltos en su posición corrida. Si no, se agrupan como agrupaba MapLibre: cada pin sin
 *    grupo junta a los que tiene a menos de un ancho de pin.
 *
 * Se calcula **por zoom entero**, y eso es lo que cumple el FR-023c: desplazar el mapa no
 * cambia el zoom, así que no mueve nada. Es determinista —sin azar, con el orden fijado por
 * id— por la misma razón.
 *
 * **La posición corrida es solo de dibujo** (FR-023b). Nadie más que el mapa la ve: la ficha,
 * "Ir", la distancia y el recorrido buscan el registro por id.
 */
object Acomodo {

    /**
     * Cuánto se puede correr un pin de su lugar real, en metros. Media cuadra.
     *
     * ponytail: se calibra en la calle (FR-023a). Es el mismo largo que la app usa como umbral
     * de precisión degradada. Si los pines corridos se leen sobre la cuadra equivocada, bajarlo;
     * si se agrupan patentes que entrarían sueltas, subirlo.
     */
    const val LIMITE_ACOMODO_M = 50.0

    /** Circunferencia ecuatorial de la Tierra, la de Web Mercator. */
    private const val CIRCUNFERENCIA_M = 40_075_016.686

    /** El mundo mide `512 * 2^zoom` dp: MapLibre Native usa teselas de 512. */
    private const val TESELA_DP = 512.0

    /**
     * ponytail: tope fijo de vueltas de repulsión. Alcanza para los conjuntos chicos que esto
     * viene a separar; uno que no se resuelve en 50 vueltas es de los que tienen que agruparse.
     */
    private const val VUELTAS = 50

    /** Media dp de tolerancia: dos pines a 33,6 dp no se ven encimados. */
    private const val TOLERANCIA_DP = 0.5

    fun para(pines: List<Pin>, zoom: Int, anchoPinDp: Double): List<Dibujo> {
        val mundo = TESELA_DP * 2.0.pow(zoom)
        val ordenados = pines.sortedBy { it.id }
        val xy = ordenados.map { proyectar(it.latitud, it.longitud, mundo) }

        return conjuntos(xy, anchoPinDp).flatMap { indices ->
            if (indices.size == 1) {
                val p = ordenados[indices.first()]
                listOf(Dibujo.Suelto(p.id, p.latitud, p.longitud))
            } else {
                separar(indices, ordenados, xy, mundo, anchoPinDp)
                    ?: agrupar(indices, ordenados, xy, anchoPinDp)
            }
        }
    }

    /**
     * Intenta separar un conjunto por repulsión. Devuelve null si no entra sin pasarse del
     * límite, y entonces el conjunto se agrupa.
     *
     * ponytail: solo mira los choques dentro del conjunto. Un pin corrido puede quedar encima
     * de uno de otro conjunto vecino; con [LIMITE_ACOMODO_M] de techo es raro y se nota poco.
     * Si en la calle molesta, repetir la detección de conjuntos después de correr.
     */
    private fun separar(
        indices: List<Int>,
        pines: List<Pin>,
        xy: List<Pair<Double, Double>>,
        mundo: Double,
        ancho: Double,
    ): List<Dibujo>? {
        val x = DoubleArray(indices.size) { xy[indices[it]].first }
        val y = DoubleArray(indices.size) { xy[indices[it]].second }

        repeat(VUELTAS) {
            var huboChoque = false
            for (i in x.indices) for (j in i + 1 until x.size) {
                var dx = x[i] - x[j]
                var dy = y[i] - y[j]
                var d = hypot(dx, dy)
                if (d >= ancho - TOLERANCIA_DP) continue
                huboChoque = true
                if (d < 1e-9) {
                    // Dos en el mismo punto no tienen para dónde empujarse. Se abren en un
                    // ángulo que depende solo de su lugar en la lista: determinista.
                    val angulo = i * 2.399963 + j
                    dx = cos(angulo)
                    dy = sin(angulo)
                    d = 1.0
                }
                val empuje = (ancho - d) / 2 / d
                x[i] += dx * empuje; y[i] += dy * empuje
                x[j] -= dx * empuje; y[j] -= dy * empuje
            }
            if (!huboChoque) {
                val dibujos = indices.mapIndexed { k, idx ->
                    val p = pines[idx]
                    val corrido = hypot(x[k] - xy[idx].first, y[k] - xy[idx].second)
                    if (corrido * metrosPorDp(p.latitud, mundo) > LIMITE_ACOMODO_M) return null
                    val (lat, lon) = desproyectar(x[k], y[k], mundo)
                    Dibujo.Suelto(p.id, lat, lon)
                }
                return dibujos
            }
        }
        return null
    }

    /**
     * Agrupa como agrupaba MapLibre: cada pin sin grupo, en orden de id, junta a los que tiene a
     * menos de un ancho de pin. Un conjunto encadenado —A toca a B, B toca a C— no termina en un
     * solo grupo gigante con el centro en ninguna parte.
     */
    private fun agrupar(
        indices: List<Int>,
        pines: List<Pin>,
        xy: List<Pair<Double, Double>>,
        ancho: Double,
    ): List<Dibujo> {
        val libres = indices.toMutableList()
        val dibujos = mutableListOf<Dibujo>()
        while (libres.isNotEmpty()) {
            val semilla = libres.first()
            val juntos = libres.filter {
                hypot(xy[it].first - xy[semilla].first, xy[it].second - xy[semilla].second) < ancho
            }
            libres -= juntos.toSet()
            dibujos += if (juntos.size == 1) {
                val p = pines[semilla]
                Dibujo.Suelto(p.id, p.latitud, p.longitud)
            } else {
                // En el centro de las posiciones reales, no de las corridas.
                Dibujo.Grupo(
                    latitud = juntos.map { pines[it].latitud }.average(),
                    longitud = juntos.map { pines[it].longitud }.average(),
                    cuenta = juntos.size,
                )
            }
        }
        return dibujos
    }

    /** Los conjuntos de pines que se enciman, en cadena. Unión de a pares, O(n²). */
    private fun conjuntos(xy: List<Pair<Double, Double>>, ancho: Double): List<List<Int>> {
        val padre = IntArray(xy.size) { it }
        fun raiz(i: Int): Int {
            var r = i
            while (padre[r] != r) r = padre[r]
            return r
        }
        for (i in xy.indices) for (j in i + 1 until xy.size) {
            if (hypot(xy[i].first - xy[j].first, xy[i].second - xy[j].second) < ancho) {
                padre[raiz(j)] = raiz(i)
            }
        }
        return xy.indices.groupBy { raiz(it) }.values.toList()
    }

    private fun proyectar(latitud: Double, longitud: Double, mundo: Double): Pair<Double, Double> {
        val lat = Math.toRadians(latitud)
        val x = (longitud + 180.0) / 360.0 * mundo
        val y = (1 - ln(tan(lat) + 1 / cos(lat)) / PI) / 2 * mundo
        return x to y
    }

    private fun desproyectar(x: Double, y: Double, mundo: Double): Pair<Double, Double> {
        val longitud = x / mundo * 360.0 - 180.0
        val latitud = Math.toDegrees(atan(sinh(PI * (1 - 2 * y / mundo))))
        return latitud to longitud
    }

    private fun metrosPorDp(latitud: Double, mundo: Double) =
        CIRCUNFERENCIA_M * cos(Math.toRadians(latitud)) / mundo
}

/** Una patente para acomodar: solo lo que hace falta para saber dónde está. */
data class Pin(val id: Long, val latitud: Double, val longitud: Double)

/** Cómo se dibuja una patente, o varias, en un zoom. */
sealed interface Dibujo {
    /** Una patente, en su posición **de dibujo**: la real, o corrida hasta media cuadra. */
    data class Suelto(val id: Long, val latitud: Double, val longitud: Double) : Dibujo

    /** Varias que no entran sueltas. En el centro de sus posiciones reales. */
    data class Grupo(val latitud: Double, val longitud: Double, val cuenta: Int) : Dibujo
}
