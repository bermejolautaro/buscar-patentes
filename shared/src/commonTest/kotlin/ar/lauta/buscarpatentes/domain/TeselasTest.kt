package ar.lauta.buscarpatentes.domain

import ar.lauta.buscarpatentes.ubicacion.BuscarCuadras
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * De las teselas vectoriales a las vías de una zona (D2 a D5 de la 012).
 *
 * Las teselas se arman acá con un codificador mínimo: una capa es una lista de rasgos, y un rasgo sus
 * propiedades y sus líneas, en unidades de la tesela (0 a 4096, medio metro cada una a zoom 14).
 */
class TeselasTest {

    private class Rasgo(val props: Map<String, Any>, val lineas: List<List<Pair<Int, Int>>>, val tipo: Int = 2)

    private fun calle(vararg puntos: Pair<Int, Int>, clase: String = "minor", extra: Map<String, Any> = emptyMap()) =
        Rasgo(mapOf("class" to clase) + extra, listOf(puntos.toList()))

    private fun nombre(nombre: String, vararg puntos: Pair<Int, Int>) =
        Rasgo(mapOf("name" to nombre, "class" to "minor"), listOf(puntos.toList()))

    private fun tesela(x: Int, calles: List<Rasgo>, nombres: List<Rasgo> = emptyList(), y: Int = Y) =
        Tesela(14, x, y, capa("transportation", calles) + capa("transportation_name", nombres))

    /** Un borde que envuelve las dos teselas de prueba. */
    private val todo = run {
        val (n, o) = Teselas.posicion(X.toDouble() * 4096, Y.toDouble() * 4096)
        val (s, e) = Teselas.posicion((X + 2.0) * 4096, (Y + 1.0) * 4096)
        listOf(n to o, n to e, s to e, s to o)
    }

    @Test
    fun `decodifica las capas con sus rasgos propiedades y lineas`() {
        val capas = Mvt.capas(capa("transportation", listOf(calle(10 to 20, 30 to 40, extra = mapOf("oneway" to 1L, "layer" to -1L)))))
        val rasgo = capas.getValue("transportation").rasgos.single()
        assertEquals("minor", rasgo.props["class"])
        assertEquals(1L, rasgo.props["oneway"])
        assertEquals(-1L, rasgo.props["layer"])
        assertEquals(listOf(listOf(10 to 20, 30 to 40)), rasgo.lineas)
    }

    @Test
    fun `entran las calles caminables y no las demas`() {
        val entran = listOf(
            calle(0 to 100, 4000 to 100),
            calle(0 to 300, 4000 to 300, clase = "primary"),
            calle(0 to 500, 4000 to 500, clase = "path", extra = mapOf("subclass" to "pedestrian")),
        )
        val noEntran = listOf(
            calle(0 to 700, 4000 to 700, clase = "service"),
            calle(0 to 900, 4000 to 900, extra = mapOf("foot" to "no")),
            calle(0 to 1100, 4000 to 1100, extra = mapOf("access" to "no")),
            calle(0 to 1300, 4000 to 1300, clase = "path", extra = mapOf("subclass" to "footway")),
            calle(0 to 1500, 4000 to 1500, clase = "motorway"),
            Rasgo(mapOf("class" to "minor"), listOf(listOf(0 to 1700, 100 to 1700, 100 to 1800, 0 to 1700)), tipo = 3),
        )
        assertEquals(3, Teselas.vias(listOf(tesela(X, entran + noEntran))).size)
    }

    @Test
    fun `una calle partida en el borde entre dos teselas es una sola cuadra`() {
        // La misma calle en las dos teselas, cada una con su margen. Del lado de la segunda, el punto
        // del margen quedó una unidad corrido, como pasa al recortar en el servidor.
        val izquierda = tesela(X, listOf(calle(3000 to 2000, 4160 to 2000)))
        val derecha = tesela(X + 1, listOf(calle(-96 to 2001, 1000 to 2000)))
        val cuadras = Cuadras.armar(Teselas.vias(listOf(izquierda, derecha)), todo)
        assertEquals(1, cuadras.size)
    }

    @Test
    fun `una calle que termina sobre otra la parte ahi`() {
        val calles = listOf(calle(0 to 2000, 4000 to 2000), calle(2000 to 500, 2000 to 2001))
        assertEquals(3, Cuadras.armar(Teselas.vias(listOf(tesela(X, calles))), todo).size)
    }

    @Test
    fun `dos calles que se cruzan sin compartir un punto se cortan en el cruce`() {
        // Lo que hace la tesela con dos calles rectas: les saca el punto de la esquina a las dos.
        val calles = listOf(calle(0 to 2000, 4000 to 2000), calle(2000 to 500, 2000 to 3500))
        assertEquals(4, Cuadras.armar(Teselas.vias(listOf(tesela(X, calles))), todo).size)
    }

    @Test
    fun `una calle que no llega a otra queda suelta`() {
        val calles = listOf(calle(0 to 2000, 4000 to 2000), calle(2000 to 500, 2000 to 1990))
        assertEquals(2, Cuadras.armar(Teselas.vias(listOf(tesela(X, calles))), todo).size)
    }

    @Test
    fun `una calle en otro nivel no es una esquina`() {
        val calles = listOf(calle(0 to 2000, 4000 to 2000), calle(2000 to 500, 2000 to 2001, extra = mapOf("layer" to 1L)))
        assertEquals(2, Cuadras.armar(Teselas.vias(listOf(tesela(X, calles))), todo).size)
    }

    @Test
    fun `el nombre sale de su capa y el sentido unico de oneway`() {
        val calles = listOf(calle(0 to 2000, 4000 to 2000, extra = mapOf("oneway" to 1L)), calle(0 to 3000, 4000 to 3000))
        val nombres = listOf(nombre("Calle Larga", 0 to 2000, 4000 to 2000))
        val vias = Teselas.vias(listOf(tesela(X, calles, nombres))).sortedBy { it.posiciones.first().first }.reversed()
        assertEquals("Calle Larga", vias[0].nombre)
        assertTrue(vias[0].unaMano)
        assertNull(vias[1].nombre)
        assertTrue(!vias[1].unaMano)
    }

    @Test
    fun `las teselas que cubren un rectangulo`() {
        val (n, o) = Teselas.posicion(X * 4096.0 + 100, Y * 4096.0 + 100)
        val (s, e) = Teselas.posicion((X + 1) * 4096.0 + 100, (Y + 1) * 4096.0 + 100)
        assertEquals(
            listOf(X to Y, X to Y + 1, X + 1 to Y, X + 1 to Y + 1),
            Teselas.cubren(Borde.Rectangulo(sur = s, oeste = o, norte = n, este = e)),
        )
    }

    @Test
    fun `la plantilla de las teselas sale del TileJSON`() {
        val tileJson = """{"tilejson":"3.0.0","tiles":["https://tiles.openfreemap.org/planet/20261101_080013_pt/{z}/{x}/{y}.pbf"]}"""
        assertEquals(
            "https://tiles.openfreemap.org/planet/20261101_080013_pt/{z}/{x}/{y}.pbf",
            BuscarCuadras.plantilla(tileJson),
        )
        assertNull(BuscarCuadras.plantilla("no soy json"))
    }

    // --- Un codificador mínimo de Mapbox Vector Tiles ---

    private fun capa(nombre: String, rasgos: List<Rasgo>): ByteArray {
        val claves = mutableListOf<String>()
        val valores = mutableListOf<Any>()
        val cuerpo = Escritor()
        cuerpo.texto(1, nombre)
        for (r in rasgos) {
            val tags = mutableListOf<Long>()
            for ((k, v) in r.props) {
                if (k !in claves) claves += k
                if (v !in valores) valores += v
                tags += claves.indexOf(k).toLong()
                tags += valores.indexOf(v).toLong()
            }
            val rasgo = Escritor()
            rasgo.empaquetados(2, tags)
            rasgo.varint(3, r.tipo.toLong())
            rasgo.empaquetados(4, geometria(r.lineas))
            cuerpo.mensaje(2, rasgo)
        }
        claves.forEach { cuerpo.texto(3, it) }
        valores.forEach { v ->
            val valor = Escritor()
            when (v) {
                is String -> valor.texto(1, v)
                is Long -> valor.varint(6, (v shl 1) xor (v shr 63))
                else -> error(v)
            }
            cuerpo.mensaje(4, valor)
        }
        cuerpo.varint(5, 4096)
        return Escritor().also { it.mensaje(3, cuerpo) }.bytes()
    }

    private fun geometria(lineas: List<List<Pair<Int, Int>>>): List<Long> {
        val salida = mutableListOf<Long>()
        var cx = 0
        var cy = 0
        fun zz(n: Int) = ((n shl 1) xor (n shr 31)).toLong() and 0xffffffffL
        for (l in lineas) {
            salida += 1L or (1L shl 3)
            salida += zz(l[0].first - cx)
            salida += zz(l[0].second - cy)
            cx = l[0].first
            cy = l[0].second
            salida += 2L or ((l.size - 1).toLong() shl 3)
            for ((x, y) in l.drop(1)) {
                salida += zz(x - cx)
                salida += zz(y - cy)
                cx = x
                cy = y
            }
        }
        return salida
    }

    private class Escritor {
        private val datos = mutableListOf<Byte>()

        fun bytes() = datos.toByteArray()

        private fun crudo(v: Long) {
            var x = v
            while (true) {
                if (x and 0x7fL.inv() == 0L) {
                    datos += x.toByte()
                    return
                }
                datos += ((x and 0x7f) or 0x80).toByte()
                x = x ushr 7
            }
        }

        fun varint(campo: Int, v: Long) {
            crudo((campo shl 3).toLong())
            crudo(v)
        }

        private fun delimitado(campo: Int, contenido: ByteArray) {
            crudo(((campo shl 3) or 2).toLong())
            crudo(contenido.size.toLong())
            datos += contenido.toList()
        }

        fun texto(campo: Int, s: String) = delimitado(campo, s.encodeToByteArray())

        fun mensaje(campo: Int, m: Escritor) = delimitado(campo, m.bytes())

        fun empaquetados(campo: Int, vs: List<Long>) = delimitado(campo, Escritor().also { e -> vs.forEach { e.crudo(it) } }.bytes())
    }

    private companion object {
        // Una tesela del centro: da lo mismo cuál, mientras las cuentas sean en latitudes reales.
        const val X = 5534
        const val Y = 9872
    }
}
