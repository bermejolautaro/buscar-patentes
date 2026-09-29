package ar.lauta.buscarpatentes.ui

import ar.lauta.buscarpatentes.ui.Ir.AccionRecorrido
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El recorrido por varias patentes (US4 de la 005): la URL que se le entrega a Google Maps y
 * la decisión de qué hacer según cuántas paradas quedaron incluidas.
 *
 * La URL se prueba porque un error ahí no se ve hasta estar en la calle: Maps abre igual, con
 * las paradas en otro orden o con una coordenada mal leída, y el jugador camina hacia el lado
 * equivocado.
 */
class IrTest {

    private val a = -34.6037400 to -58.3816500
    private val b = -34.5979100 to -58.3809700
    private val c = -34.6012000 to -58.3778000

    @Test
    fun `la ultima es el destino y las anteriores son paradas en orden`() {
        val url = Ir.urlDeRecorrido(listOf(a, b, c))
        assertTrue(url, url.contains("destination=-34.6012000,-58.3778000"))
        assertTrue(url, url.contains("waypoints=-34.6037400,-58.3816500%7C-34.5979100,-58.3809700"))
        assertTrue(url, url.contains("travelmode=walking"))
    }

    @Test
    fun `con dos paradas hay un solo waypoint`() {
        val url = Ir.urlDeRecorrido(listOf(a, b))
        assertTrue(url, url.contains("waypoints=-34.6037400,-58.3816500&"))
        assertFalse(url, url.contains("%7C"))
    }

    @Test
    fun `con una sola parada no hay waypoints`() {
        assertFalse(Ir.urlDeRecorrido(listOf(a)).contains("waypoints"))
    }

    @Test
    fun `nunca lleva origen, arranca donde esta el telefono`() {
        assertFalse(Ir.urlDeRecorrido(listOf(a, b, c)).contains("origin"))
    }

    @Test
    fun `las coordenadas van con punto aunque el telefono este en espanol`() {
        val antes = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("es-AR"))
            val url = Ir.urlDeRecorrido(listOf(a, b))
            assertTrue(url, url.contains("-34.5979100,-58.3809700"))
            assertFalse(url, url.contains("-34,59"))
        } finally {
            Locale.setDefault(antes)
        }
    }

    @Test
    fun `que hacer segun cuantas paradas hay incluidas`() {
        assertEquals(AccionRecorrido.Nada, Ir.accionPara(0))
        assertEquals(AccionRecorrido.IrALaUnica, Ir.accionPara(1))
        assertEquals(AccionRecorrido.AbrirMaps, Ir.accionPara(2))
        assertEquals(AccionRecorrido.AbrirMaps, Ir.accionPara(Ir.MAXIMO_PARADAS_APP))
        assertEquals(AccionRecorrido.Sobran(3), Ir.accionPara(13))
    }

    @Test
    fun `en el navegador entran menos`() {
        assertEquals(AccionRecorrido.AbrirMaps, Ir.accionPara(4, Ir.MAXIMO_PARADAS_NAVEGADOR))
        assertEquals(AccionRecorrido.Sobran(2), Ir.accionPara(6, Ir.MAXIMO_PARADAS_NAVEGADOR))
    }
}
