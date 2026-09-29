package ar.lauta.buscarpatentes.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La única prueba nueva de la 003, y la única lógica pura que la feature agrega (D3).
 *
 * El foco está en [Geo.cardinal] y no en la distancia. La distancia, si estuviera mal, se
 * vería: "3 km" para algo que está a la vuelta salta a la vista. Un cardinal equivocado, no:
 * "noreste" en lugar de "norte" se lee igual de plausible cuando uno no sabe la respuesta, y
 * la app mandaría al jugador a caminar para el lado equivocado sin que nada parezca roto.
 *
 * Las posiciones son de una zona cualquiera de la ciudad, alrededor del Obelisco.
 */
class GeoTest {

    // Dos posiciones separadas por unas cuadras.
    private val latA = -34.60374
    private val lonA = -58.38165
    private val latB = -34.59791
    private val lonB = -58.38097

    @Test
    fun `la distancia entre dos posiciones conocidas da lo que mide la cuadra`() {
        val d = Geo.distanciaMetros(latA, lonA, latB, lonB)
        // ~0.0058° de latitud son unos 650 m. Margen amplio: lo que se prueba es que la
        // fórmula está bien, no el sexto decimal.
        assertTrue("dio $d m", d in 600.0..700.0)
    }

    @Test
    fun `la distancia de un punto a si mismo es cero`() {
        assertEquals(0.0, Geo.distanciaMetros(latA, lonA, latA, lonA), 0.001)
    }

    @Test
    fun `la distancia es simetrica`() {
        assertEquals(
            Geo.distanciaMetros(latA, lonA, latB, lonB),
            Geo.distanciaMetros(latB, lonB, latA, lonA),
            0.001,
        )
    }

    @Test
    fun `ir hacia el norte da rumbo cero`() {
        assertEquals(0.0, Geo.rumboGrados(0.0, 0.0, 1.0, 0.0), 0.001)
    }

    @Test
    fun `ir hacia el este da rumbo noventa`() {
        assertEquals(90.0, Geo.rumboGrados(0.0, 0.0, 0.0, 1.0), 0.001)
    }

    @Test
    fun `ir hacia el oeste da doscientos setenta y no menos noventa`() {
        // Es el caso que rompe si falta normalizar la salida de atan2: daría -90.
        assertEquals(270.0, Geo.rumboGrados(0.0, 0.0, 0.0, -1.0), 0.001)
    }

    @Test
    fun `el rumbo entre dos registros reales apunta al norte`() {
        // B está al norte de A y apenas al este: tiene que caer en el sector norte.
        assertEquals("norte", Geo.cardinal(Geo.rumboGrados(latA, lonA, latB, lonB)))
    }

    @Test
    fun `los ocho sectores en su centro`() {
        assertEquals("norte", Geo.cardinal(0.0))
        assertEquals("noreste", Geo.cardinal(45.0))
        assertEquals("este", Geo.cardinal(90.0))
        assertEquals("sudeste", Geo.cardinal(135.0))
        assertEquals("sur", Geo.cardinal(180.0))
        assertEquals("sudoeste", Geo.cardinal(225.0))
        assertEquals("oeste", Geo.cardinal(270.0))
        assertEquals("noroeste", Geo.cardinal(315.0))
    }

    @Test
    fun `el norte es el sector partido y no se rompe cruzando el cero`() {
        // Este es el caso que motiva la prueba entera. El sector norte va de 337.5 a 22.5
        // pasando por 0, así que es el único que no es un intervalo contiguo.
        assertEquals("norte", Geo.cardinal(359.9))
        assertEquals("norte", Geo.cardinal(360.0))
        assertEquals("norte", Geo.cardinal(340.0))
        assertEquals("norte", Geo.cardinal(20.0))
    }

    @Test
    fun `los limites de sector caen del lado que corresponde`() {
        // 22.5 y 337.5 son los bordes del sector norte. Con redondeo, el borde exacto cae
        // hacia arriba, y lo que importa es que un grado a cada lado no se confunda.
        assertEquals("norte", Geo.cardinal(22.4))
        assertEquals("noreste", Geo.cardinal(22.6))
        assertEquals("noroeste", Geo.cardinal(337.4))
        assertEquals("norte", Geo.cardinal(337.6))
    }

    @Test
    fun `un rumbo fuera de rango se normaliza en vez de romper`() {
        // Un llamador que sume o reste grados no puede hacer explotar el índice.
        assertEquals("norte", Geo.cardinal(720.0))
        assertEquals("oeste", Geo.cardinal(-90.0))
        assertEquals("sur", Geo.cardinal(-180.0))
    }

    // --- Cortar el recorrido donde hubo una desconexión (FR-009) ---
    //
    // Esta mitad del archivo existe por un defecto encontrado caminando: un corte de señal
    // "teletransportó" el recorrido hasta la casa del jugador. El trazo unía en línea recta
    // la última posición antes del corte con la primera de la vuelta, afirmando un camino
    // que nunca existió.

    /** Un punto a unos 60 m al norte del anterior: la separación normal caminando. */
    private fun caminando(pasos: Int) = (0..pasos).map { -34.6032 + it * 0.00055 to -58.3808 }

    @Test
    fun `un recorrido continuo es un solo tramo`() {
        assertEquals(1, Geo.tramos(caminando(10)).size)
    }

    @Test
    fun `un recorrido vacio no da ningun tramo`() {
        assertTrue(Geo.tramos(emptyList()).isEmpty())
    }

    @Test
    fun `un solo punto es un tramo de un punto`() {
        val t = Geo.tramos(listOf(latA to lonA))
        assertEquals(1, t.size)
        assertEquals(1, t[0].size)
    }

    @Test
    fun `un salto grande parte el recorrido en dos`() {
        // Tres cuadras caminando, y de golpe 3 km: el caso real del teletransporte.
        val conCorte = caminando(3) + listOf(-34.6332 to -58.3808)
        val t = Geo.tramos(conCorte)
        assertEquals(2, t.size)
        assertEquals(4, t[0].size)
        assertEquals(1, t[1].size)
    }

    @Test
    fun `dos cortes dan tres tramos`() {
        val puntos = listOf(
            -34.6032 to -58.3808,
            -34.6038 to -58.3808,
            -34.6332 to -58.3808,
            -34.6338 to -58.3808,
            -34.6632 to -58.3808,
        )
        assertEquals(3, Geo.tramos(puntos).size)
    }

    @Test
    fun `una pausa larga sin moverse no corta nada`() {
        // El caso que un umbral por tiempo se comería: pararse a tomar algo y seguir. Los
        // puntos quedan pegados, así que no hay salto y no hay corte.
        val puntos = listOf(
            -34.6032 to -58.3808,
            -34.6032 to -58.3808,
            -34.6038 to -58.3808,
        )
        assertEquals(1, Geo.tramos(puntos).size)
    }

    @Test
    fun `el umbral corta arriba y no abajo`() {
        // 250 m es el techo. A 200 no corta; a 300 sí.
        val cerca = listOf(-34.6032 to -58.3808, -34.6050 to -58.3808)
        val lejos = listOf(-34.6032 to -58.3808, -34.6062 to -58.3808)
        assertEquals(1, Geo.tramos(cerca).size)
        assertEquals(2, Geo.tramos(lejos).size)
    }

    @Test
    fun `cada corte deja un hueco entre el final de un tramo y el principio del siguiente`() {
        val conCorte = caminando(3) + listOf(-34.6332 to -58.3808)
        val tramos = Geo.tramos(conCorte)
        val huecos = Geo.huecos(tramos)

        assertEquals(1, huecos.size)
        assertEquals(tramos[0].last(), huecos[0].first)
        assertEquals(tramos[1].first(), huecos[0].second)
    }

    @Test
    fun `un recorrido sin cortes no deja huecos`() {
        assertTrue(Geo.huecos(Geo.tramos(caminando(10))).isEmpty())
    }

    @Test
    fun `la distancia legible redondea a decenas bajo el kilometro`() {
        assertEquals("120 m", Geo.distanciaLegible(123.0))
        assertEquals("0 m", Geo.distanciaLegible(2.0))
        assertEquals("990 m", Geo.distanciaLegible(994.0))
    }

    @Test
    fun `la distancia legible pasa a kilometros con coma decimal`() {
        assertEquals("1,4 km", Geo.distanciaLegible(1_430.0))
        assertEquals("1,0 km", Geo.distanciaLegible(1_000.0))
    }

    // El texto del FR-003, que la ficha y cada resultado de búsqueda muestran igual.

    @Test
    fun `sin posicion actual dice que no se sabe, y no da ningun numero`() {
        // FR-003a: la rama que no puede salir mal. Un número inventado acá sería la misma
        // clase de mentira que el Principio II prohíbe en la evidencia.
        val texto = Geo.distanciaYRumbo(null, -34.6221, -58.3591)

        assertTrue(texto, texto.contains("desconocida"))
        assertTrue("no puede haber ningún dígito: $texto", texto.none { it.isDigit() })
    }

    @Test
    fun `con posicion actual dice distancia y punto cardinal`() {
        // Un punto al norte del jugador, a algo más de una cuadra.
        val texto = Geo.distanciaYRumbo(-34.6232 to -58.3591, -34.6221, -58.3591)

        assertEquals("A 120 m al norte", texto)
    }

    // Reconocer una patente ya anotada: mismo número, mismo lugar (Captura.confirmar).

    @Test
    fun `una captura a pocos metros de otra la reconoce como la misma`() {
        // Unos 8 m al norte de A: el jugador volvió a pasar por el mismo auto.
        val candidatos = listOf("vieja" to (latA + 0.00007 to lonA))

        val encontrado = Geo.masCercano(latA, lonA, candidatos, 10.0) { it.second }

        assertEquals("vieja", encontrado?.first)
    }

    @Test
    fun `a unas cuadras es otra patente, aunque tenga el mismo numero`() {
        // B está a ~650 m: dos autos distintos con el mismo número, no uno solo.
        val candidatos = listOf("lejana" to (latB to lonB))

        assertNull(Geo.masCercano(latA, lonA, candidatos, 10.0) { it.second })
    }

    @Test
    fun `entre dos dentro del radio gana la mas cercana`() {
        // ~9 m y ~3 m. El borde importa: elegir la primera de la lista confirmaría el auto
        // equivocado cuando hay dos anotados en la misma cuadra.
        val candidatos = listOf(
            "a nueve metros" to (latA + 0.00008 to lonA),
            "a tres metros" to (latA + 0.00003 to lonA),
        )

        val encontrado = Geo.masCercano(latA, lonA, candidatos, 10.0) { it.second }

        assertEquals("a tres metros", encontrado?.first)
    }

    @Test
    fun `sin candidatos no hay nada que confirmar`() {
        assertNull(
            Geo.masCercano(latA, lonA, emptyList<Pair<String, Pair<Double, Double>>>(), 10.0) {
                it.second
            },
        )
    }
}
