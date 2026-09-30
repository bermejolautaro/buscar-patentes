package ar.lauta.buscarpatentes.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/** El camino ajustado de la 007: el formato guardado y el armado sin rectas inventadas. */
class CaminoAjustadoTest {

    private val tramoA = listOf(-34.603722 to -58.381592, -34.604515 to -58.382999, -34.6049 to -58.3831)
    private val tramoB = listOf(-34.61 to -58.391, -34.61002 to -58.3903)

    // --- El formato (contrato A1) ---

    @Test
    fun `sin camino es pendiente`() {
        assertEquals(CaminoGuardado.Pendiente, CaminoGuardado.leer(null))
    }

    @Test
    fun `un camino con el criterio anterior de la 007 se lee pendiente para que se reajuste`() {
        // Los `2:` llevaban los puntos medidos entre pedazos de calle: se vuelven a ajustar.
        assertEquals(CaminoGuardado.Pendiente, CaminoGuardado.leer("2:" + Polilinea.codificar(tramoA)))
    }

    @Test
    fun `un camino de antes de la 007 se lee pendiente para que se reajuste`() {
        val unaPierna = Polilinea.codificar(tramoA)
        val dosPiernas = Polilinea.codificar(tramoA) + Polilinea.SEPARADOR_PIERNAS + Polilinea.codificar(tramoB)
        assertEquals(CaminoGuardado.Pendiente, CaminoGuardado.leer(unaPierna))
        assertEquals(CaminoGuardado.Pendiente, CaminoGuardado.leer(dosPiernas))
    }

    @Test
    fun `el prefijo solo es que no se pudo`() {
        assertEquals(CaminoGuardado.NoSePudo, CaminoGuardado.leer("3:"))
        assertEquals("3:", CaminoGuardado.escribir(emptyList()))
    }

    @Test
    fun `los tramos van y vuelven en orden`() {
        val guardado = CaminoGuardado.escribir(listOf(tramoA, tramoB))
        assertEquals(CaminoGuardado.Ajustado(listOf(tramoA, tramoB)), CaminoGuardado.leer(guardado))
    }

    @Test
    fun `un tramo de menos de dos posiciones no se dibuja`() {
        val guardado = CaminoGuardado.escribir(listOf(tramoA, listOf(-34.6 to -58.4)))
        assertEquals(CaminoGuardado.Ajustado(listOf(tramoA)), CaminoGuardado.leer(guardado))
    }

    // --- Los pedazos de calle (contrato A4) ---

    private val f = List(8) { -35.0 - it * 0.001 to -59.0 }

    @Test
    fun `donde una arista no sigue a la anterior hay dos pedazos y ninguna recta entre ellos`() {
        // Aristas 0..2 y 3..5: la segunda no empieza donde terminó la primera. La forma trae los
        // dos pedazos pegados, y la recta de f[2] a f[3] era la diagonal.
        val pedazos = CaminoAjustado.pedazos(f.take(6), listOf(0..2, 3..5))
        assertEquals(listOf(f.subList(0, 3), f.subList(3, 6)), pedazos)
    }

    @Test
    fun `aristas contiguas son un solo pedazo`() {
        assertEquals(listOf(f.take(5)), CaminoAjustado.pedazos(f.take(5), listOf(0..2, 2..3, 3..4)))
    }

    @Test
    fun `sin aristas no hay ningun pedazo`() {
        assertEquals(emptyList(), CaminoAjustado.pedazos(emptyList(), emptyList()))
    }

    @Test
    fun `un pedazo de una sola posicion no es una calle`() {
        assertEquals(listOf(f.subList(0, 3)), CaminoAjustado.pedazos(f.take(5), listOf(0..2, 4..4)))
    }
}
