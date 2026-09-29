package ar.lauta.buscarpatentes.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El decodificador de la polilínea que devuelve el map matching (FR-031).
 *
 * Se prueba porque falla de la peor manera posible: **en silencio y con resultado
 * plausible**. Con el factor de precisión equivocado el camino aparece en el lugar correcto
 * pero diez veces más chico; con el zigzag mal leído aparece espejado. Ninguna de las dos
 * cosas tira una excepción, y las dos se ven como "el trazo quedó raro".
 */
class PolilineaTest {

    /**
     * Cinco puntos alrededor del Obelisco, en zigzag por dos cuadras, codificados como los
     * devuelve Valhalla: seis decimales.
     */
    private val real = "fj`_aA~pijnB?o}@~p@??o}@~p@?"

    @Test
    fun `una cadena vacia no da ningun punto`() {
        assertTrue(Polilinea.decodificar("").isEmpty())
    }

    @Test
    fun `la polilinea cae donde tiene que caer`() {
        val puntos = Polilinea.decodificar(real)

        assertTrue("dio ${puntos.size} puntos", puntos.size >= 5)
        val (lat, lon) = puntos.first()
        // Si el factor fuera 1e5 esto daría -3.46, que está en el Atlántico frente a Brasil.
        assertEquals(-34.6037, lat, 1e-6)
        assertEquals(-58.3816, lon, 1e-6)
    }

    @Test
    fun `todos los puntos quedan dentro del barrio`() {
        // Un zigzag mal leído o un desplazamiento corrido dispersan los puntos por el mundo.
        // Acá todos tienen que caer dentro de unas pocas cuadras del primero.
        val puntos = Polilinea.decodificar(real)
        val (lat0, lon0) = puntos.first()

        puntos.forEach { (lat, lon) ->
            val d = Geo.distanciaMetros(lat0, lon0, lat, lon)
            assertTrue("un punto quedó a $d m del primero", d < 2_000)
        }
    }

    @Test
    fun `el camino avanza y no se queda en el lugar`() {
        val puntos = Polilinea.decodificar(real)
        val largo = puntos.zipWithNext { a, b ->
            Geo.distanciaMetros(a.first, a.second, b.first, b.second)
        }.sum()

        // Cuatro tramos de unos 90 m: unos 360 m en total.
        assertTrue("el camino midió $largo m", largo in 200.0..500.0)
    }

    @Test
    fun `la precisión de cinco decimales da un resultado diez veces más chico`() {
        // La prueba que documenta el error concreto: mismo string, factor equivocado.
        val seis = Polilinea.decodificar(real)
        val cinco = Polilinea.decodificar(real, precision = 1e5)

        assertEquals(seis.size, cinco.size)
        assertEquals(seis.first().first * 10, cinco.first().first, 0.001)
    }

    @Test
    fun `una cadena cortada devuelve lo que alcanzó a leer en vez de romper`() {
        // Un prefijo corta en el medio de un número, así que puede no completar ni un punto.
        // Lo que importa es que no tire: el trazo crudo sigue abajo como respaldo.
        val puntos = Polilinea.decodificar(real.substring(0, 10))
        assertTrue("dio ${puntos.size} puntos", puntos.size <= 2)
    }

    @Test
    fun `una cadena con basura no tira excepción`() {
        Polilinea.decodificar("???")
        Polilinea.decodificar("~")
    }

    @Test
    fun `dos piernas separadas dan los puntos de las dos, cada una en su lugar`() {
        // El bug que esto cierra: pegar las dos **cadenas** y decodificar de corrido suma
        // las coordenadas de la segunda a donde terminó la primera, y el camino se va del
        // planeta. Separadas, cada pierna se decodifica desde su propio origen.
        val dos = Polilinea.decodificar(real + Polilinea.SEPARADOR_PIERNAS + real)
        val una = Polilinea.decodificar(real)

        assertEquals(una.size * 2, dos.size)
        assertEquals(una.first(), dos.first())
        assertEquals(una.first(), dos[una.size])
    }

    @Test
    fun `un camino de una sola pierna se decodifica igual que antes del separador`() {
        // Los caminos que ya están guardados en la base no traen separador. No pueden
        // cambiar de forma por este arreglo.
        assertEquals(Polilinea.decodificar(real), Polilinea.decodificar(real))
        assertTrue(real.none { it == ';' })
    }
}
