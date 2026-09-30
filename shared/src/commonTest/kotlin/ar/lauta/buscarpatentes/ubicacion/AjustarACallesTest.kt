package ar.lauta.buscarpatentes.ubicacion

import ar.lauta.buscarpatentes.data.PuntoDeTrayecto
import ar.lauta.buscarpatentes.domain.CaminoAjustado
import ar.lauta.buscarpatentes.domain.Geo
import ar.lauta.buscarpatentes.domain.Polilinea
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * El pedido al servicio de ajuste a calles, que en la 006 pasó de `org.json` a
 * `kotlinx-serialization-json` (D16). Tiene que pedir lo mismo que antes: si cambia, el servicio
 * contesta otra cosa y el trazo sale sobre la calle de al lado.
 */
class AjustarACallesTest {

    private fun punto(latitud: Double, longitud: Double, precision: Float) = PuntoDeTrayecto(
        recorridoId = 1,
        latitud = latitud,
        longitud = longitud,
        precisionMetros = precision,
        registradoEn = 0,
    )

    @Test
    fun `el cuerpo lleva cada punto con su radio acotado y el costeo peatonal`() {
        val cuerpo = AjustarACalles.cuerpo(
            listOf(
                punto(-34.6037, -58.3816, 2f),
                punto(-34.6040, -58.3816, 12.5f),
                punto(-34.6043, -58.3816, 80f),
            ),
        )

        val forma = cuerpo["shape"]!!.jsonArray.map { it.jsonObject }
        assertEquals(-34.6037, forma[0]["lat"]!!.jsonPrimitive.content.toDouble())
        assertEquals(-58.3816, forma[0]["lon"]!!.jsonPrimitive.content.toDouble())
        // Ni un radio de cero, que no encuentra calle, ni uno que alcance la manzana de al lado.
        assertEquals(listOf(5f, 12.5f, 30f), forma.map { it["radius"]!!.jsonPrimitive.float })
        assertEquals("pedestrian", cuerpo["costing"]!!.jsonPrimitive.content)
        assertEquals("map_snap", cuerpo["shape_match"]!!.jsonPrimitive.content)
    }

    // --- La respuesta (contrato A3) y el armado sobre una respuesta real (A4) ---

    @Test
    fun `la respuesta real se lee entera`() {
        val r = AjustarACalles.leerRespuesta(RESPUESTA)!!

        assertEquals(CAMINATA.size, r.emparejados.size)
        // El punto 11 y los cinco del final no emparejaron.
        assertNull(r.emparejados[11])
        assertTrue(r.emparejados.takeLast(5).all { it == null })
        // La arista 18 no empieza donde terminó la 17: ahí está el corte entre pedazos.
        assertTrue(r.aristas[18].first != r.aristas[17].last)
        assertEquals(r.aristas.last().last + 1, r.forma.size)
    }

    @Test
    fun `sobre la respuesta real no queda ninguna recta entre pedazos`() {
        val r = AjustarACalles.leerRespuesta(RESPUESTA)!!
        val armado = CaminoAjustado.armar(CAMINATA, r.forma, r.aristas, r.emparejados)

        // Qué índices de la forma van juntos: los de un mismo pedazo.
        val pedazoDe = IntArray(r.forma.size) { -1 }
        var pedazo = 0
        r.aristas.forEachIndexed { i, arista ->
            if (i > 0 && arista.first != r.aristas[i - 1].last) pedazo++
            for (k in arista) pedazoDe[k] = pedazo
        }
        val deLaForma = r.forma.withIndex().associate { (k, p) -> p to k }
        val medidos = CAMINATA.withIndex().associate { (j, p) -> p to j }

        armado.zipWithNext().forEach { (a, b) ->
            val formaSeguida = deLaForma[a]?.let { ka -> deLaForma[b]?.let { kb -> kb == ka + 1 && pedazoDe[ka] == pedazoDe[kb] } } == true
            val medidosSeguidos = medidos[a]?.let { ja -> medidos[b]?.let { jb -> jb == ja + 1 || jb == ja } } == true
            // Lo único que queda es el empalme entre un punto medido y el pedazo al que emparejó:
            // la distancia de pegarlo a la calle, nunca una cuadra.
            val empalme = Geo.distanciaMetros(a.first, a.second, b.first, b.second) <= EMPALME_MAXIMO_M
            assertTrue(formaSeguida || medidosSeguidos || empalme, "recta de $a a $b")
        }
    }

    @Test
    fun `lo que no emparejo al final de la caminata sigue dibujado`() {
        val r = AjustarACalles.leerRespuesta(RESPUESTA)!!
        val armado = CaminoAjustado.armar(CAMINATA, r.forma, r.aristas, r.emparejados)
        assertEquals(CAMINATA.last(), armado.last())
    }

    @Test
    fun `un punto interpolado cuenta como emparejado y una respuesta sin campos no sirve`() {
        // Como texto JSON: la polilínea puede traer una barra invertida, que hay que escapar.
        val forma = Polilinea.codificar(listOf(-34.6037 to -58.3816, -34.6040 to -58.3816)).replace("\\", "\\\\")
        val r = AjustarACalles.leerRespuesta(
            """{"shape":"$forma","edges":[{"begin_shape_index":0,"end_shape_index":1}],""" +
                """"matched_points":[{"type":"matched","edge_index":0},{"type":"interpolated","edge_index":0},{"type":"unmatched"}]}""",
        )!!
        assertEquals(listOf(0, 0, null), r.emparejados)
        assertNull(AjustarACalles.leerRespuesta("""{"shape":"$forma"}"""))
        assertNull(AjustarACalles.leerRespuesta("no es json"))
    }

    @Test
    fun `una caminata con un corte de senal se pide en dos tramos`() {
        assertEquals(listOf(13, 11), Geo.tramos(CAMINATA).map { it.size })
    }

    private companion object {
        /** Cuánto puede separarse un punto medido de su lugar en la calle: el radio máximo del pedido. */
        const val EMPALME_MAXIMO_M = 30.0
    }
}
