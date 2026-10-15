package ar.lauta.buscarpatentes.domain

import ar.lauta.buscarpatentes.ubicacion.BuscarCuadras
import kotlin.math.abs
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * De las vías del mapa a las cuadras de una zona (contrato Z2 de la 008).
 *
 * Las vías son inventadas y se dibujan en metros alrededor de un origen: así cada caso muestra
 * solo lo que prueba. Las posiciones están cerca del Obelisco.
 */
class CuadrasTest {

    private val lat0 = -34.6
    private val lon0 = -58.38

    /** La posición a [x] metros al este y [y] al norte del origen. */
    private fun m(x: Double, y: Double) =
        (lat0 + y / METROS_POR_GRADO) to (lon0 + x / (METROS_POR_GRADO * cos(lat0 * kotlin.math.PI / 180)))

    private fun via(nombre: String?, vararg puntos: Pair<Long, Pair<Double, Double>>, unaMano: Boolean = false) =
        Via(puntos.map { it.first }, puntos.map { m(it.second.first, it.second.second) }, nombre, unaMano)

    /** Un borde que envuelve todo lo inventado. */
    private val todo = listOf(m(-500.0, -500.0), m(1000.0, -500.0), m(1000.0, 500.0), m(-500.0, 500.0))

    private fun largo(c: CuadraArmada) = c.forma.zipWithNext { a, b -> Geo.distanciaMetros(a.first, a.second, b.first, b.second) }.sum()

    @Test
    fun `una calle que cruzan cinco calles da seis cuadras`() {
        val calle = via("Recta", *(0..6).map { (it + 1L) to (it * 100.0 to 0.0) }.toTypedArray())
        val cruces = (1..5).map { k -> via("Cruce $k", (100L + k) to (k * 100.0 to -50.0), (k + 1L) to (k * 100.0 to 0.0), (200L + k) to (k * 100.0 to 50.0)) }

        val cuadras = Cuadras.armar(listOf(calle) + cruces, todo)

        val rectas = cuadras.filter { it.nombre == "Recta" }
        assertEquals(6, rectas.size)
        rectas.forEach { assertTrue(abs(largo(it) - 100.0) < 1.0, "una cuadra de ${largo(it)} m") }
        // Cada cruce son dos cuadras de 50 m, una de cada lado.
        assertEquals(10, cuadras.count { it.nombre?.startsWith("Cruce") == true })
    }

    @Test
    fun `una calle partida en dos vias sin esquina es una cuadra`() {
        val a = via("Partida", 1L to (0.0 to 0.0), 2L to (50.0 to 0.0), 3L to (100.0 to 0.0))
        val b = via("Partida", 3L to (100.0 to 0.0), 4L to (150.0 to 0.0), 5L to (200.0 to 0.0))

        val cuadras = Cuadras.armar(listOf(a, b), todo)

        assertEquals(1, cuadras.size)
        assertTrue(abs(largo(cuadras.single()) - 200.0) < 1.0)
    }

    @Test
    fun `una calle sin salida termina en su ultimo nodo`() {
        val principal = via("Principal", 1L to (0.0 to 0.0), 2L to (100.0 to 0.0), 3L to (200.0 to 0.0))
        val cortada = via("Cortada", 2L to (100.0 to 0.0), 10L to (100.0 to 50.0), 11L to (100.0 to 100.0))

        val cuadras = Cuadras.armar(listOf(principal, cortada), todo)

        val sinSalida = cuadras.single { it.nombre == "Cortada" }
        assertTrue(abs(largo(sinSalida) - 100.0) < 1.0)
        assertTrue(m(100.0, 100.0) in listOf(sinSalida.forma.first(), sinSalida.forma.last()))
        assertEquals(2, cuadras.count { it.nombre == "Principal" })
    }

    @Test
    fun `una cuadra de 20 m se descarta`() {
        val corta = via("Corta", 1L to (0.0 to 0.0), 2L to (20.0 to 0.0))
        assertTrue(Cuadras.armar(listOf(corta), todo).isEmpty())
    }

    @Test
    fun `una cuadra es de la zona con la mitad adentro`() {
        val calle = via("Borde", 1L to (0.0 to 0.0), 2L to (100.0 to 0.0))
        fun hasta(x: Double) = listOf(m(-50.0, -50.0), m(x, -50.0), m(x, 50.0), m(-50.0, 50.0))

        assertEquals(1, Cuadras.armar(listOf(calle), hasta(60.0)).size)
        assertTrue(Cuadras.armar(listOf(calle), hasta(40.0)).isEmpty())
    }

    @Test
    fun `las dos manos de una avenida son gemelas`() {
        val ida = via("Avenida", 1L to (0.0 to 0.0), 2L to (100.0 to 0.0), unaMano = true)
        val vuelta = via("Avenida", 3L to (100.0 to 20.0), 4L to (0.0 to 20.0), unaMano = true)
        val oeste = via("Oeste", 10L to (0.0 to -50.0), 1L to (0.0 to 0.0), 4L to (0.0 to 20.0), 11L to (0.0 to 70.0))
        val este = via("Este", 20L to (100.0 to -50.0), 2L to (100.0 to 0.0), 3L to (100.0 to 20.0), 21L to (100.0 to 70.0))

        val cuadras = Cuadras.armar(listOf(ida, vuelta, oeste, este), todo)

        val avenida = cuadras.withIndex().filter { it.value.nombre == "Avenida" }
        assertEquals(2, avenida.size)
        val (principal, gemela) = avenida.sortedBy { it.value.gemelaDe ?: -1 }
        assertNull(principal.value.gemelaDe)
        assertEquals(principal.index, gemela.value.gemelaDe)
        // El pedacito de 20 m entre las dos manos no es una cuadra.
        assertTrue(cuadras.filter { it.nombre == "Oeste" || it.nombre == "Este" }.all { largo(it) >= Cuadras.LARGO_MINIMO_M })
    }

    @Test
    fun `dos cuadras seguidas de una mano no son gemelas`() {
        val calle = via("Seguida", 1L to (0.0 to 0.0), 2L to (100.0 to 0.0), 3L to (200.0 to 0.0), unaMano = true)
        val cruce = via("Cruce", 10L to (100.0 to -50.0), 2L to (100.0 to 0.0), 11L to (100.0 to 50.0))

        val cuadras = Cuadras.armar(listOf(calle, cruce), todo)

        assertEquals(2, cuadras.count { it.nombre == "Seguida" })
        assertTrue(cuadras.all { it.gemelaDe == null })
    }

    @Test
    fun `dos paralelas con distinto nombre no son gemelas`() {
        val una = via("Una", 1L to (0.0 to 0.0), 2L to (100.0 to 0.0), unaMano = true)
        val otra = via("Otra", 3L to (100.0 to 20.0), 4L to (0.0 to 20.0), unaMano = true)

        assertTrue(Cuadras.armar(listOf(una, otra), todo).all { it.gemelaDe == null })
    }

    @Test
    fun `la respuesta real arma cuadras de 30 m o mas`() {
        val vias = assertNotNull(BuscarCuadras.leerVias(VIAS))
        val franja = listOf(-34.6055 to -58.3905, -34.6055 to -58.3800, -34.6020 to -58.3800, -34.6020 to -58.3905)

        val cuadras = Cuadras.armar(vias, franja)

        assertTrue(cuadras.size > 20, "salieron ${cuadras.size} cuadras")
        cuadras.forEach { assertTrue(largo(it) >= Cuadras.LARGO_MINIMO_M, "una cuadra de ${largo(it)} m") }
    }

    @Test
    fun `lo que no es una respuesta de Overpass no se lee`() {
        assertNull(BuscarCuadras.leerVias("no soy json"))
        assertNull(BuscarCuadras.leerVias("""{"otra":"cosa"}"""))
    }

    @Test
    fun `la consulta lleva punto decimal y seis decimales`() {
        val consulta = BuscarCuadras.consulta(Borde.Rectangulo(sur = -34.6055, oeste = -58.3905, norte = -34.602, este = -58.38))
        assertTrue("(-34.605500,-58.390500,-34.602000,-58.380000)" in consulta, consulta)
        assertTrue(consulta.startsWith("[out:json]"))
        assertTrue(consulta.trimEnd().endsWith("out geom;"))
    }

    @Test
    fun `la misma cuadra se reconoce en los dos sentidos y no con una punta corrida`() {
        val cuadra = listOf(m(0.0, 0.0), m(50.0, 1.0), m(100.0, 0.0))
        assertTrue(Cuadras.misma(cuadra, cuadra.reversed()))
        assertTrue(Cuadras.misma(cuadra, listOf(m(2.0, 2.0), m(100.0, -3.0))))
        assertFalse(Cuadras.misma(cuadra, listOf(m(0.0, 0.0), m(110.0, 0.0))))
        assertFalse(Cuadras.misma(cuadra, emptyList()))
    }

    private companion object {
        const val METROS_POR_GRADO = 111_194.93
    }
}
