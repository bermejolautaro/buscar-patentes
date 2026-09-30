package ar.lauta.buscarpatentes.domain

import ar.lauta.buscarpatentes.ubicacion.AjustarACalles
import ar.lauta.buscarpatentes.ubicacion.BuscarCuadras
import ar.lauta.buscarpatentes.ubicacion.RESPUESTA
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Qué cuadras de una zona están recorridas (contrato Z3 de la 008). */
class CoberturaDeZonaTest {

    // --- Con los datos reales ---

    /**
     * Las cuadras de la franja de `VIAS` contra el camino que devolvió el servicio de ajuste para
     * la caminata de `RespuestaDeAjuste.kt`. El primer tramo camina cuatro cuadras enteras de la
     * avenida de la franja, entre -58.3880 y -58.3820, y cruza en la esquina las calles que la
     * cortan.
     */
    @Test
    fun `lo caminado de esquina a esquina cuenta y lo cruzado no`() {
        val franja = listOf(-34.6055 to -58.3905, -34.6055 to -58.3800, -34.6020 to -58.3800, -34.6020 to -58.3905)
        val armadas = Cuadras.armar(BuscarCuadras.leerVias(VIAS)!!, franja)
        val cuadras = armadas.mapIndexed { i, c -> CuadraParaContar(i.toLong(), c.forma, c.gemelaDe?.toLong(), quitada = false) }
        val respuesta = AjustarACalles.leerRespuesta(RESPUESTA)!!
        val salida = SalidaParaContar(1_000L, CaminoAjustado.pedazos(respuesta.forma, respuesta.aristas))

        val cobertura = CoberturaDeZona.calcular(cuadras, listOf(salida), desde = 0L)

        val detalle = armadas.mapIndexed { i, c ->
            "$i ${c.nombre} ${c.forma.first()}→${c.forma.last()}: ${if (i.toLong() in cobertura.recorridas) "recorrida" else "-"}"
        }.joinToString("\n")
        val caminadas = armadas.indices.filter { i ->
            armadas[i].nombre == "Avenida Corrientes" && armadas[i].forma.all { it.second in -58.3880..-58.3820 }
        }
        assertEquals(4, caminadas.size, detalle)
        caminadas.forEach { assertTrue(it.toLong() in cobertura.recorridas, "no cuenta la cuadra $it\n$detalle") }
        val cruzadas = setOf("Uruguay", "Libertad", "Talcahuano", "Cerrito", "Paraná")
        armadas.indices.filter { armadas[it].nombre in cruzadas }.forEach {
            assertFalse(it.toLong() in cobertura.recorridas, "cuenta una cruzada: ${armadas[it].nombre}\n$detalle")
        }
    }

    // --- Con cuadras inventadas, en metros alrededor de un origen cerca del Obelisco ---

    private val lat0 = -34.6
    private val lon0 = -58.38

    private fun m(x: Double, y: Double) =
        (lat0 + y / METROS_POR_GRADO) to (lon0 + x / (METROS_POR_GRADO * cos(lat0 * kotlin.math.PI / 180)))

    /** Una cuadra horizontal de 100 m a [y] metros del origen. */
    private fun cuadra(id: Long, y: Double = 0.0, gemelaDe: Long? = null, quitada: Boolean = false) =
        CuadraParaContar(id, listOf(m(0.0, y), m(100.0, y)), gemelaDe, quitada)

    private fun salida(en: Long, vararg tramo: Pair<Double, Double>) =
        SalidaParaContar(en, listOf(tramo.map { m(it.first, it.second) }))

    @Test
    fun `cruzar en la esquina no recorre la cuadra`() {
        val c = CoberturaDeZona.calcular(listOf(cuadra(1)), listOf(salida(1, 0.0 to -100.0, 0.0 to 100.0)), desde = 0)
        assertTrue(c.recorridas.isEmpty())
        assertEquals(1, c.faltan)
    }

    @Test
    fun `media cuadra un dia y la otra media otro dia la recorren`() {
        val salidas = listOf(salida(1, 0.0 to 0.0, 55.0 to 0.0), salida(2, 45.0 to 0.0, 100.0 to 0.0))

        val c = CoberturaDeZona.calcular(listOf(cuadra(1)), salidas, desde = 0)

        assertEquals(setOf(1L), c.recorridas)
        assertEquals(100, c.porcentaje)
        assertEquals(2L, c.completadaEn)
    }

    @Test
    fun `una sola media cuadra no alcanza`() {
        val c = CoberturaDeZona.calcular(listOf(cuadra(1)), listOf(salida(1, 0.0 to 0.0, 55.0 to 0.0)), desde = 0)
        assertTrue(c.recorridas.isEmpty())
    }

    @Test
    fun `una salida anterior a desde no suma`() {
        val c = CoberturaDeZona.calcular(listOf(cuadra(1)), listOf(salida(1, 0.0 to 0.0, 100.0 to 0.0)), desde = 2)
        assertTrue(c.recorridas.isEmpty())
        assertEquals(0, c.porcentaje)
    }

    @Test
    fun `una cuadra quitada no cuenta ni arriba ni abajo`() {
        val cuadras = listOf(cuadra(1, y = 0.0, quitada = true), cuadra(2, y = 200.0))
        val c = CoberturaDeZona.calcular(cuadras, listOf(salida(1, 0.0 to 0.0, 100.0 to 0.0)), desde = 0)

        assertEquals(1, c.total)
        assertEquals(1, c.faltan)
        assertEquals(0, c.porcentaje)
        assertNull(c.completadaEn)
    }

    @Test
    fun `una pareja de gemelas cuenta una vez y la recorre cualquiera`() {
        val cuadras = listOf(cuadra(1, y = 0.0), cuadra(2, y = 20.0, gemelaDe = 1))
        val c = CoberturaDeZona.calcular(cuadras, listOf(salida(5, 0.0 to 20.0, 100.0 to 20.0)), desde = 0)

        assertEquals(1, c.total)
        assertEquals(setOf(1L, 2L), c.recorridas)
        assertEquals(100, c.porcentaje)
    }

    @Test
    fun `99 de 100 no es 100 por ciento`() {
        val cuadras = (0 until 100).map { cuadra(it.toLong(), y = it * 100.0) }
        val salidas = (0 until 99).map { salida(1, 0.0 to it * 100.0, 100.0 to it * 100.0) }

        val c = CoberturaDeZona.calcular(cuadras, salidas, desde = 0)

        assertEquals(99, c.porcentaje)
        assertEquals(1, c.faltan)
        assertNull(c.completadaEn)
    }

    @Test
    fun `se completa con la fecha de la ultima cuadra recorrida`() {
        val cuadras = listOf(cuadra(1, y = 0.0), cuadra(2, y = 200.0))
        val salidas = listOf(
            salida(10, 0.0 to 0.0, 100.0 to 0.0),
            salida(20, 0.0 to 200.0, 100.0 to 200.0),
            // Volver a pasar por la primera no cambia cuándo se recorrió.
            salida(30, 0.0 to 0.0, 100.0 to 0.0),
        )

        val c = CoberturaDeZona.calcular(cuadras, salidas, desde = 0)

        assertEquals(0, c.faltan)
        assertEquals(20L, c.completadaEn)
    }

    @Test
    fun `sin cuadras no hay nada que contar`() {
        val c = CoberturaDeZona.calcular(emptyList(), listOf(salida(1, 0.0 to 0.0, 100.0 to 0.0)), desde = 0)
        assertEquals(0, c.total)
        assertEquals(0, c.porcentaje)
        assertNull(c.completadaEn)
    }

    private companion object {
        const val METROS_POR_GRADO = 111_194.93
    }
}
