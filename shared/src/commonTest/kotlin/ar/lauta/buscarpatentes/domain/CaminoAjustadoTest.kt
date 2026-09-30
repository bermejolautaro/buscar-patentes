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
    fun `un camino de antes de la 007 se lee pendiente para que se reajuste`() {
        val unaPierna = Polilinea.codificar(tramoA)
        val dosPiernas = Polilinea.codificar(tramoA) + Polilinea.SEPARADOR_PIERNAS + Polilinea.codificar(tramoB)
        assertEquals(CaminoGuardado.Pendiente, CaminoGuardado.leer(unaPierna))
        assertEquals(CaminoGuardado.Pendiente, CaminoGuardado.leer(dosPiernas))
    }

    @Test
    fun `el prefijo solo es que no se pudo`() {
        assertEquals(CaminoGuardado.NoSePudo, CaminoGuardado.leer("2:"))
        assertEquals("2:", CaminoGuardado.escribir(emptyList()))
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
}
