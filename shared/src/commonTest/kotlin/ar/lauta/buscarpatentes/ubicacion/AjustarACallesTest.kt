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
    fun `el cuerpo lleva cada punto sin radio y el costeo peatonal`() {
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
        // Con el radio de la precisión, un tercio de los puntos de una salida real no emparejaba
        // (D7 de la 007): el servicio busca la calle con su radio por defecto.
        assertTrue(forma.none { "radius" in it })
        assertEquals("pedestrian", cuerpo["costing"]!!.jsonPrimitive.content)
        assertEquals("map_snap", cuerpo["shape_match"]!!.jsonPrimitive.content)
    }

    // --- La respuesta (contrato A3) y los pedazos de una respuesta real (A4) ---

    @Test
    fun `la respuesta real se lee entera`() {
        val r = AjustarACalles.leerRespuesta(RESPUESTA)!!

        // La arista 18 no empieza donde terminó la 17: ahí está el corte entre pedazos.
        assertTrue(r.aristas[18].first != r.aristas[17].last)
        assertEquals(r.aristas.last().last + 1, r.forma.size)
    }

    @Test
    fun `de la respuesta real salen dos pedazos y la recta entre ellos no se dibuja`() {
        val r = AjustarACalles.leerRespuesta(RESPUESTA)!!
        val pedazos = CaminoAjustado.pedazos(r.forma, r.aristas)

        assertEquals(2, pedazos.size)
        val finDelPrimero = r.forma[r.aristas[17].last]
        val principioDelSegundo = r.forma[r.aristas[18].first]
        assertEquals(finDelPrimero, pedazos[0].last())
        assertEquals(principioDelSegundo, pedazos[1].first())
        // Era una recta de más de 100 m cruzando una manzana.
        assertTrue(Geo.distanciaMetros(finDelPrimero.first, finDelPrimero.second, principioDelSegundo.first, principioDelSegundo.second) > 100)
    }

    @Test
    fun `una respuesta sin campos o con indices fuera de la forma no sirve`() {
        // Como texto JSON: la polilínea puede traer una barra invertida, que hay que escapar.
        val forma = Polilinea.codificar(listOf(-34.6037 to -58.3816, -34.6040 to -58.3816)).replace("\\", "\\\\")
        val bien = AjustarACalles.leerRespuesta(
            """{"shape":"$forma","edges":[{"begin_shape_index":0,"end_shape_index":1}]}""",
        )
        assertEquals(listOf(0..1), bien?.aristas)
        assertNull(AjustarACalles.leerRespuesta("""{"shape":"$forma"}"""))
        assertNull(
            AjustarACalles.leerRespuesta(
                """{"shape":"$forma","edges":[{"begin_shape_index":0,"end_shape_index":7}]}""",
            ),
        )
        assertNull(AjustarACalles.leerRespuesta("no es json"))
    }

    @Test
    fun `una caminata con un corte de senal se pide en dos tramos`() {
        assertEquals(listOf(13, 11), Geo.tramos(CAMINATA).map { it.size })
    }
}
