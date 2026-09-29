package ar.lauta.buscarpatentes.ubicacion

import ar.lauta.buscarpatentes.data.PuntoDeTrayecto
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
