package ar.lauta.buscarpatentes.domain

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sinh
import kotlin.math.tan
import kotlin.math.cos

/** Una tesela vectorial bajada: su lugar en la grilla de [z] y sus bytes (D2 de la 012). */
class Tesela(val z: Int, val x: Int, val y: Int, val datos: ByteArray)

/**
 * De las teselas del mapa de fondo a las vías de una zona (D2 a D5 de la 012).
 *
 * Entrega las mismas [Via] que daba Overpass, así [Cuadras.armar] no cambia. Lo que Overpass traía
 * hecho y las teselas no, se arma acá:
 *
 * - **Las esquinas.** Las teselas no traen nodos. Todo se pasa a una grilla común de zoom 14, donde
 *   un punto compartido por dos calles tiene las mismas coordenadas, aunque venga de dos teselas.
 * - **El borde de la tesela.** Cada calle se recorta al cuadrado de su tesela, y los cortes se juntan
 *   con los de la vecina a menos de [TOLERANCIA].
 * - **Los cruces sin punto.** La tesela simplifica las calles rectas, y se lleva el punto donde la
 *   cruza o termina otra. Dos calles del mismo nivel que se tocan se cortan ahí.
 * - **Los nombres**, que vienen en otra capa.
 */
object Teselas {

    /** El más detallado que publica OpenFreeMap. Una tesela tiene unos 2 km de lado. */
    const val ZOOM = 14

    /** Unidades de una tesela en la grilla común, como el `extent` de OpenFreeMap. */
    private const val EXTENSION = 4096.0

    /**
     * En unidades de la grilla: medio metro cada una a zoom 14 en Buenos Aires.
     * ponytail: 1 m alcanzó en todas las teselas medidas. Si una esquina real no se junta, se sube.
     */
    private const val TOLERANCIA = 2.0

    /** Cuántas unidades mide de lado cada celda de las búsquedas por cercanía. */
    private const val CELDA = 64.0

    private val CLASES = setOf("trunk", "primary", "secondary", "tertiary", "minor")

    /** Las teselas (x, y) de [ZOOM] que tocan el rectángulo. */
    fun cubren(r: Borde.Rectangulo): List<Pair<Int, Int>> {
        val (x0, y0) = grilla(r.norte, r.oeste)
        val (x1, y1) = grilla(r.sur, r.este)
        return (floor(x0 / EXTENSION).toInt()..floor(x1 / EXTENSION).toInt()).flatMap { x ->
            (floor(y0 / EXTENSION).toInt()..floor(y1 / EXTENSION).toInt()).map { y -> x to y }
        }
    }

    /** Latitud y longitud de un punto de la grilla común de [ZOOM]. */
    internal fun posicion(gx: Double, gy: Double): Pair<Double, Double> {
        val n = (1 shl ZOOM) * EXTENSION
        val lon = gx / n * 360 - 180
        val lat = atan(sinh(PI * (1 - 2 * gy / n))) * 180 / PI
        return lat to lon
    }

    /** El punto de la grilla común de [ZOOM] de una latitud y longitud. */
    private fun grilla(lat: Double, lon: Double): Pair<Double, Double> {
        val n = (1 shl ZOOM) * EXTENSION
        val r = lat * PI / 180
        return (lon + 180) / 360 * n to (1 - ln(tan(r) + 1 / cos(r)) / PI) / 2 * n
    }

    // --- Las vías ---

    private data class P(val x: Double, val y: Double)

    private class Vertice(val p: P, var nodo: Long = -1, val corte: Boolean = false)

    private class Pedazo(val vertices: MutableList<Vertice>, val unaMano: Boolean, val nivel: Long) {
        var nombre: String? = null
    }

    fun vias(teselas: List<Tesela>): List<Via> {
        val pedazos = mutableListOf<Pedazo>()
        val nombres = mutableListOf<Pair<String, List<P>>>()
        for (t in teselas) {
            val capas = Mvt.capas(t.datos)
            capas["transportation"]?.let { capa ->
                for (rasgo in capa.rasgos) {
                    if (rasgo.tipo != Mvt.LINEA || !caminable(rasgo.props)) continue
                    val oneway = rasgo.props["oneway"] as? Long
                    val nivel = rasgo.props["layer"] as? Long ?: 0L
                    for (linea in rasgo.lineas) {
                        for (pedazo in recortar(linea.map { global(t, capa.extension, it) }, t)) {
                            pedazos += Pedazo(pedazo, unaMano = oneway == 1L || oneway == -1L, nivel = nivel)
                        }
                    }
                }
            }
            capas["transportation_name"]?.let { capa ->
                for (rasgo in capa.rasgos) {
                    val nombre = rasgo.props["name"] as? String ?: continue
                    rasgo.lineas.forEach { l -> nombres += nombre to l.map { global(t, capa.extension, it) } }
                }
            }
        }
        nodos(pedazos)
        cruces(pedazos)
        ponerNombres(pedazos, nombres)
        return pedazos.map { pedazo ->
            val vs = pedazo.vertices.filterIndexed { i, v -> i == 0 || v.nodo != pedazo.vertices[i - 1].nodo }
            Via(vs.map { it.nodo }, vs.map { posicion(it.p.x, it.p.y) }, pedazo.nombre, pedazo.unaMano)
        }
    }

    /** D3 de la 012: las mismas calles que se le pedían a Overpass. */
    private fun caminable(props: Map<String, Any?>): Boolean {
        if (props["access"] == "no" || props["foot"] == "no") return false
        val clase = props["class"]
        return clase in CLASES || (clase == "path" && props["subclass"] == "pedestrian")
    }

    private fun global(t: Tesela, extension: Int, p: Pair<Int, Int>) = P(
        (t.x + p.first.toDouble() / extension) * EXTENSION,
        (t.y + p.second.toDouble() / extension) * EXTENSION,
    )

    /** La línea recortada al cuadrado propio de su tesela: lo que cae en el margen lo trae la vecina. */
    private fun recortar(linea: List<P>, t: Tesela): List<MutableList<Vertice>> {
        val x0 = t.x * EXTENSION
        val y0 = t.y * EXTENSION
        val x1 = x0 + EXTENSION
        val y1 = y0 + EXTENSION
        val salida = mutableListOf<MutableList<Vertice>>()
        var actual: MutableList<Vertice>? = null
        for ((a, b) in linea.zipWithNext()) {
            val (t0, t1) = liangBarsky(a, b, x0, y0, x1, y1) ?: run {
                actual = null
                null
            } ?: continue
            val entra = P(a.x + t0 * (b.x - a.x), a.y + t0 * (b.y - a.y))
            val sale = P(a.x + t1 * (b.x - a.x), a.y + t1 * (b.y - a.y))
            var pedazo = actual
            if (pedazo == null || t0 > 0) {
                pedazo = mutableListOf(Vertice(entra, corte = t0 > 0))
                salida += pedazo
            }
            pedazo += Vertice(sale, corte = t1 < 1)
            actual = if (t1 < 1) null else pedazo
        }
        return salida.filter { p -> p.size >= 2 && p.zipWithNext().any { (a, b) -> a.p != b.p } }
    }

    /** Qué parte del tramo de [a] a [b] cae adentro del rectángulo, como fracciones del tramo. */
    private fun liangBarsky(a: P, b: P, x0: Double, y0: Double, x1: Double, y1: Double): Pair<Double, Double>? {
        val dx = b.x - a.x
        val dy = b.y - a.y
        var t0 = 0.0
        var t1 = 1.0
        val ps = doubleArrayOf(-dx, dx, -dy, dy)
        val qs = doubleArrayOf(a.x - x0, x1 - a.x, a.y - y0, y1 - a.y)
        for (i in 0..3) {
            val p = ps[i]
            val q = qs[i]
            if (p == 0.0) {
                if (q < 0) return null
            } else {
                val r = q / p
                if (p < 0) t0 = maxOf(t0, r) else t1 = minOf(t1, r)
            }
        }
        return if (t0 > t1) null else t0 to t1
    }

    /**
     * Pasos 1 y 3 del D4: un punto exacto es un nodo, y un corte del borde se junta con lo que haya a
     * menos de [TOLERANCIA], que es el mismo punto visto desde la tesela vecina.
     */
    private fun nodos(pedazos: List<Pedazo>) {
        var siguiente = 0L
        val exactos = HashMap<P, Long>()
        val cerca = HashMap<Long, MutableList<Vertice>>()
        fun anotar(v: Vertice) {
            cerca.getOrPut(celda(v.p, TOLERANCIA)) { mutableListOf() } += v
        }
        for (v in pedazos.flatMap { it.vertices }.filter { !it.corte }) {
            v.nodo = exactos.getOrPut(v.p) { siguiente++ }
            anotar(v)
        }
        for (v in pedazos.flatMap { it.vertices }.filter { it.corte }) {
            val vecino = vecinas(v.p, TOLERANCIA).flatMap { cerca[it].orEmpty() }
                .filter { it.nodo >= 0 && distancia(it.p, v.p) <= TOLERANCIA }
                .minByOrNull { distancia(it.p, v.p) }
            v.nodo = vecino?.nodo ?: siguiente++
            anotar(v)
        }
    }

    /**
     * Paso 4 del D4: dos calles del mismo nivel que se cruzan, o una que termina sobre otra, se
     * cortan ahí aunque no compartan el punto. La tesela simplifica las calles rectas y se lleva el
     * punto del cruce de una de las dos, o de las dos: Uruguay pasaba por Sarmiento sin cortarla.
     *
     * El cruce es un nodo nuevo, o el de un punto que ya estaba a menos de [TOLERANCIA]. Si de los
     * dos lados había uno, se unen.
     */
    private fun cruces(pedazos: List<Pedazo>) {
        class Tramo(val pedazo: Pedazo, val a: Vertice, val b: Vertice)

        val tramos = pedazos.flatMap { p -> p.vertices.zipWithNext { a, b -> Tramo(p, a, b) } }
        val porCelda = HashMap<Long, MutableList<Int>>()
        tramos.forEachIndexed { i, t -> celdasDelTramo(t.a.p, t.b.p).forEach { porCelda.getOrPut(it) { mutableListOf() } += i } }

        val padre = HashMap<Long, Long>()
        fun raiz(n: Long): Long {
            var r = n
            while (true) r = padre[r]?.takeIf { it != r } ?: return r
        }
        fun unir(a: Long, b: Long) {
            val ra = raiz(a)
            val rb = raiz(b)
            if (ra != rb) padre[maxOf(ra, rb)] = minOf(ra, rb)
        }
        var siguiente = (pedazos.flatMap { it.vertices }.maxOfOrNull { it.nodo } ?: -1) + 1

        val insertar = HashMap<Tramo, MutableList<Vertice>>()
        /** El nodo del cruce [x] sobre [t]: el de una punta cercana, o uno nuevo que se inserta. */
        fun nodoEn(t: Tramo, x: P): Long = when {
            distancia(x, t.a.p) <= TOLERANCIA -> t.a.nodo
            distancia(x, t.b.p) <= TOLERANCIA -> t.b.nodo
            else -> {
                val lista = insertar.getOrPut(t) { mutableListOf() }
                lista.firstOrNull { distancia(it.p, x) <= TOLERANCIA }?.nodo
                    ?: siguiente++.also { lista += Vertice(x, it) }
            }
        }

        val vistos = HashSet<Long>()
        for (indices in porCelda.values) {
            for (i in indices.indices) {
                for (j in i + 1 until indices.size) {
                    val a = minOf(indices[i], indices[j])
                    val b = maxOf(indices[i], indices[j])
                    if (!vistos.add(a.toLong() * tramos.size + b)) continue
                    val t1 = tramos[a]
                    val t2 = tramos[b]
                    if (t1.pedazo === t2.pedazo || t1.pedazo.nivel != t2.pedazo.nivel) continue
                    val compartidos = setOf(t1.a.nodo, t1.b.nodo).intersect(setOf(t2.a.nodo, t2.b.nodo))
                    if (compartidos.isNotEmpty()) continue
                    val x = cruce(t1.a.p, t1.b.p, t2.a.p, t2.b.p) ?: continue
                    unir(nodoEn(t1, x), nodoEn(t2, x))
                }
            }
        }

        for ((t, nuevos) in insertar) {
            val vs = t.pedazo.vertices
            vs.addAll(vs.indexOf(t.a) + 1, nuevos.sortedBy { distancia(t.a.p, it.p) })
        }
        pedazos.forEach { p -> p.vertices.forEach { it.nodo = raiz(it.nodo) } }
    }

    /**
     * Dónde se tocan los tramos [a]–[b] y [c]–[d]: el cruce de los dos, o la punta de uno que cae a
     * menos de [TOLERANCIA] del otro. Null si no se tocan.
     */
    private fun cruce(a: P, b: P, c: P, d: P): P? {
        for ((punta, tramo) in listOf(a to (c to d), b to (c to d), c to (a to b), d to (a to b))) {
            if (distanciaASegmento(punta, tramo.first, tramo.second) <= TOLERANCIA) return punta
        }
        val rx = b.x - a.x
        val ry = b.y - a.y
        val sx = d.x - c.x
        val sy = d.y - c.y
        val den = rx * sy - ry * sx
        if (den == 0.0) return null
        val t = ((c.x - a.x) * sy - (c.y - a.y) * sx) / den
        val u = ((c.x - a.x) * ry - (c.y - a.y) * rx) / den
        return if (t in 0.0..1.0 && u in 0.0..1.0) P(a.x + t * rx, a.y + t * ry) else null
    }

    /** D5: cada pedazo toma el nombre de la línea que pasa cerca de la mayoría de sus puntos. */
    private fun ponerNombres(pedazos: List<Pedazo>, nombres: List<Pair<String, List<P>>>) {
        class Tramo(val nombre: String, val a: P, val b: P)
        val tramos = HashMap<Long, MutableList<Tramo>>()
        for ((nombre, linea) in nombres) {
            for ((a, b) in linea.zipWithNext()) {
                val t = Tramo(nombre, a, b)
                for (c in celdasDelTramo(a, b)) tramos.getOrPut(c) { mutableListOf() } += t
            }
        }
        for (p in pedazos) {
            // Del medio de cada tramo y no de sus puntas: en una esquina la calle que cruza queda a la
            // misma distancia, y en una cuadrícula casi todos los puntos de una calle son esquinas.
            val muestras = p.vertices.zipWithNext { a, b ->
                listOf(0.25, 0.5, 0.75).map { f -> P(a.p.x + f * (b.p.x - a.p.x), a.p.y + f * (b.p.y - a.p.y)) }
            }.flatten()
            val votos = muestras.mapNotNull { m ->
                vecinas(m, CELDA).flatMap { tramos[it].orEmpty() }
                    .filter { distanciaASegmento(m, it.a, it.b) <= TOLERANCIA }
                    .minByOrNull { distanciaASegmento(m, it.a, it.b) }?.nombre
            }.groupingBy { it }.eachCount()
            val (nombre, cuantos) = votos.maxByOrNull { it.value } ?: continue
            if (cuantos * 2 > muestras.size) p.nombre = nombre
        }
    }

    // --- Búsquedas por cercanía ---

    private fun clave(cx: Int, cy: Int): Long = (cx.toLong() shl 32) or (cy.toLong() and 0xffffffffL)

    private fun celda(p: P, lado: Double) = clave(floor(p.x / lado).toInt(), floor(p.y / lado).toInt())

    /** La celda de [p] y sus ocho vecinas, del tamaño [lado]. */
    private fun vecinas(p: P, lado: Double): List<Long> {
        val cx = floor(p.x / lado).toInt()
        val cy = floor(p.y / lado).toInt()
        return (-1..1).flatMap { dx -> (-1..1).map { dy -> clave(cx + dx, cy + dy) } }
    }

    /** Las celdas de [CELDA] que toca el rectángulo de un tramo. */
    private fun celdasDelTramo(a: P, b: P): List<Long> =
        (floor(minOf(a.x, b.x) / CELDA).toInt()..floor(maxOf(a.x, b.x) / CELDA).toInt()).flatMap { cx ->
            (floor(minOf(a.y, b.y) / CELDA).toInt()..floor(maxOf(a.y, b.y) / CELDA).toInt()).map { cy -> clave(cx, cy) }
        }

    private fun distancia(a: P, b: P) = hypot(b.x - a.x, b.y - a.y)

    private fun distanciaASegmento(p: P, a: P, b: P): Double {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val l2 = dx * dx + dy * dy
        val t = if (l2 == 0.0) 0.0 else (((p.x - a.x) * dx + (p.y - a.y) * dy) / l2).coerceIn(0.0, 1.0)
        return hypot(p.x - a.x - t * dx, p.y - a.y - t * dy)
    }
}

/**
 * Un decodificador mínimo de Mapbox Vector Tiles: capas, rasgos, propiedades y líneas (D2 de la
 * 012). Lee lo que usan las teselas de OpenFreeMap y nada más; una librería de protobuf sería una
 * dependencia para cuatro campos (Principio IV).
 */
internal object Mvt {

    const val LINEA = 2

    class Capa(val extension: Int, val rasgos: List<Rasgo>)

    class Rasgo(val tipo: Int, val props: Map<String, Any?>, val lineas: List<List<Pair<Int, Int>>>)

    /** Las capas por nombre. Los valores de las propiedades son `String`, `Long`, `Double` o `Boolean`. */
    fun capas(datos: ByteArray): Map<String, Capa> {
        val salida = HashMap<String, Capa>()
        val lector = Lector(datos, 0, datos.size)
        while (lector.quedan()) {
            val (campo, tipo) = lector.campo()
            if (campo != 3) {
                lector.saltar(tipo)
                continue
            }
            val capa = lector.mensaje()
            var nombre = ""
            var extension = 4096
            val rasgos = mutableListOf<Lector>()
            val claves = mutableListOf<String>()
            val valores = mutableListOf<Any?>()
            while (capa.quedan()) {
                val (c, t) = capa.campo()
                when (c) {
                    1 -> nombre = capa.texto()
                    2 -> rasgos += capa.mensaje()
                    3 -> claves += capa.texto()
                    4 -> valores += valor(capa.mensaje())
                    5 -> extension = capa.varint().toInt()
                    else -> capa.saltar(t)
                }
            }
            salida[nombre] = Capa(extension, rasgos.map { rasgo(it, claves, valores) })
        }
        return salida
    }

    private fun rasgo(r: Lector, claves: List<String>, valores: List<Any?>): Rasgo {
        var tipo = 0
        var tags = emptyList<Long>()
        var geometria = emptyList<Long>()
        while (r.quedan()) {
            val (c, t) = r.campo()
            when (c) {
                2 -> tags = r.empaquetados()
                3 -> tipo = r.varint().toInt()
                4 -> geometria = r.empaquetados()
                else -> r.saltar(t)
            }
        }
        val props = tags.chunked(2).associate { (k, v) -> claves[k.toInt()] to valores[v.toInt()] }
        return Rasgo(tipo, props, lineas(geometria))
    }

    private fun valor(v: Lector): Any? {
        var salida: Any? = null
        while (v.quedan()) {
            val (c, t) = v.campo()
            salida = when (c) {
                1 -> v.texto()
                2 -> Float.fromBits(v.fijo(4).toInt()).toDouble()
                3 -> Double.fromBits(v.fijo(8))
                4, 5 -> v.varint()
                6 -> v.varint().let { (it ushr 1) xor -(it and 1) }
                7 -> v.varint() != 0L
                else -> {
                    v.saltar(t)
                    salida
                }
            }
        }
        return salida
    }

    /** Los comandos de geometría: mover a, línea a, cerrar. Las coordenadas van en zigzag y relativas. */
    private fun lineas(comandos: List<Long>): List<List<Pair<Int, Int>>> {
        val salida = mutableListOf<List<Pair<Int, Int>>>()
        var actual = mutableListOf<Pair<Int, Int>>()
        var x = 0
        var y = 0
        var i = 0
        fun zz(n: Long) = ((n ushr 1) xor -(n and 1)).toInt()
        while (i < comandos.size) {
            val comando = comandos[i++]
            val id = (comando and 7).toInt()
            val cuenta = (comando ushr 3).toInt()
            if (id == 7) continue
            repeat(cuenta) {
                x += zz(comandos[i])
                y += zz(comandos[i + 1])
                i += 2
                if (id == 1) {
                    if (actual.size > 1) salida += actual
                    actual = mutableListOf(x to y)
                } else {
                    actual += x to y
                }
            }
        }
        if (actual.size > 1) salida += actual
        return salida
    }

    private class Lector(val b: ByteArray, var i: Int, val fin: Int) {
        fun quedan() = i < fin

        fun varint(): Long {
            var r = 0L
            var s = 0
            while (true) {
                val x = b[i++].toInt() and 0xff
                r = r or ((x and 0x7f).toLong() shl s)
                if (x < 0x80) return r
                s += 7
            }
        }

        fun campo(): Pair<Int, Int> = varint().let { (it ushr 3).toInt() to (it and 7).toInt() }

        fun fijo(bytes: Int): Long {
            var r = 0L
            for (k in 0 until bytes) r = r or ((b[i + k].toLong() and 0xff) shl (8 * k))
            i += bytes
            return r
        }

        fun mensaje(): Lector {
            val n = varint().toInt()
            return Lector(b, i, i + n).also { i += n }
        }

        fun texto(): String = mensaje().let { it.b.decodeToString(it.i, it.fin) }

        fun empaquetados(): List<Long> = mensaje().let { m -> buildList { while (m.quedan()) add(m.varint()) } }

        fun saltar(tipo: Int) {
            when (tipo) {
                0 -> varint()
                1 -> i += 8
                2 -> i += varint().toInt()
                5 -> i += 4
                else -> error("Tipo de campo desconocido: $tipo")
            }
        }
    }
}
