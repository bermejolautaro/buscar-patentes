package ar.lauta.buscarpatentes

import ar.lauta.buscarpatentes.domain.FormatoPatente
import ar.lauta.buscarpatentes.domain.Patente
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

/** FR-010: extracción del número de 3 dígitos de los dos formatos argentinos. */
class PatenteTest {

    @Test
    fun `solo el numero es lo que se tipea en carga rapida`() {
        val p = Patente.parsear("313")!!
        assertEquals(313, p.numero)
        assertNull(p.texto)
        assertEquals(FormatoPatente.DESCONOCIDO, p.formato)
    }

    @Test
    fun `formato viejo AAA 123`() {
        val p = Patente.parsear("ABC 123")!!
        assertEquals(123, p.numero)
        assertEquals("ABC123", p.texto)
        assertEquals(FormatoPatente.VIEJO, p.formato)
    }

    @Test
    fun `formato mercosur AB 123 CD`() {
        val p = Patente.parsear("AB 123 CD")!!
        assertEquals(123, p.numero)
        assertEquals("AB123CD", p.texto)
        assertEquals(FormatoPatente.MERCOSUR, p.formato)
    }

    @Test
    fun `minusculas y separadores raros no importan`() {
        assertEquals(456, Patente.parsear("ab-456-cd")!!.numero)
        assertEquals(789, Patente.parsear("  xyz 789  ")!!.numero)
    }

    @Test
    fun `ceros a la izquierda se conservan como numero`() {
        assertEquals(7, Patente.parsear("007")!!.numero)
        assertEquals(0, Patente.parsear("000")!!.numero)
    }

    @Test
    fun `el numero sale del medio en mercosur, no del principio`() {
        // AB 313 CD: si alguien tomara los primeros digitos que encuentra daria otra cosa.
        val p = Patente.parsear("AA313ZZ")!!
        assertEquals(313, p.numero)
    }

    @Test
    fun `entradas invalidas devuelven null en vez de adivinar`() {
        assertNull(Patente.parsear(""))
        assertNull(Patente.parsear("31"))
        assertNull(Patente.parsear("3131"))
        assertNull(Patente.parsear("ABCD123"))
        assertNull(Patente.parsear("AB12CD"))
        assertNull(Patente.parsear("AB123C"))
        assertNull(Patente.parsear("hola"))
    }
}
