package ar.lauta.buscarpatentes.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class ProbabilidadTest {

    @Test
    fun `sin votos arranca en cero`() {
        assertEquals(0, Probabilidad.de(emptyList()))
    }

    @Test
    fun `cada voto mueve un punto`() {
        assertEquals(2, Probabilidad.de(listOf(1, 1)))
        assertEquals(1, Probabilidad.de(listOf(1, 1, -1)))
        assertEquals(7, Probabilidad.de(listOf(1, 1), desde = 5))
    }

    @Test
    fun `no pasa de los extremos`() {
        assertEquals(10, Probabilidad.de(List(20) { 1 }))
        assertEquals(0, Probabilidad.de(List(20) { -1 }))
    }

    /** La razón de acumular con tope en cada paso y no sumar y recortar al final. */
    @Test
    fun `un voto en contra baja aunque venga de muchos a favor`() {
        assertEquals(9, Probabilidad.de(List(20) { 1 } + listOf(-1)))
    }

    /** v7: solo lo que estaba por encima de 5 sobrevive; el resto vuelve a 0. */
    @Test
    fun `conserva sus votos solo si la dejaban por encima de 5`() {
        assertTrue(Probabilidad.conservaSusVotos(listOf(1)))
        assertFalse(Probabilidad.conservaSusVotos(emptyList()))
        assertFalse(Probabilidad.conservaSusVotos(listOf(1, -1)))
        assertFalse(Probabilidad.conservaSusVotos(listOf(-1, -1)))
        // El tope importa también acá: diez en contra y seis a favor dejan 6, no 1.
        assertTrue(Probabilidad.conservaSusVotos(List(10) { -1 } + List(6) { 1 }))
    }
}
