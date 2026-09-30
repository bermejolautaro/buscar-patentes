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

    // --- El armado (contrato A4) ---
    //
    // Posiciones de juguete: lo que importa es el orden en que salen, no dónde caen. `M` son los
    // puntos medidos y `F`, la forma que devolvió el servicio.

    private val m = List(6) { -34.0 - it * 0.001 to -58.0 }
    private val f = List(8) { -35.0 - it * 0.001 to -59.0 }

    @Test
    fun `dos pedazos se unen por los puntos medidos del medio y no por una recta`() {
        // Aristas 0..2 y 3..5: la segunda no empieza donde terminó la primera, así que son dos
        // pedazos. M2 no emparejó.
        val armado = CaminoAjustado.armar(
            medidos = m.take(5),
            forma = f.take(6),
            aristas = listOf(0..2, 3..5),
            emparejados = listOf(0, 0, null, 1, 1),
        )
        assertEquals(listOf(f[0], f[1], f[2], m[1], m[2], m[3], f[3], f[4], f[5]), armado)
    }

    @Test
    fun `sin puntos sueltos en el medio el empalme son los dos medidos de cada lado`() {
        val armado = CaminoAjustado.armar(
            medidos = m.take(4),
            forma = f.take(6),
            aristas = listOf(0..2, 3..5),
            emparejados = listOf(0, 0, 1, 1),
        )
        assertEquals(listOf(f[0], f[1], f[2], m[1], m[2], f[3], f[4], f[5]), armado)
    }

    @Test
    fun `lo que no emparejo al principio y al final no desaparece`() {
        val armado = CaminoAjustado.armar(
            medidos = m.take(5),
            forma = f.take(3),
            aristas = listOf(0..1, 1..2),
            emparejados = listOf(null, 0, 1, null, null),
        )
        assertEquals(listOf(m[0], m[1], f[0], f[1], f[2], m[2], m[3], m[4]), armado)
    }

    @Test
    fun `sin nada emparejado son los puntos medidos`() {
        val medidos = m.take(3)
        assertEquals(medidos, CaminoAjustado.armar(medidos, forma = emptyList(), aristas = emptyList(), emparejados = listOf(null, null, null)))
    }

    @Test
    fun `aristas contiguas son un solo pedazo aunque haya varios puntos por arista`() {
        val armado = CaminoAjustado.armar(
            medidos = m.take(3),
            forma = f.take(5),
            aristas = listOf(0..2, 2..4),
            emparejados = listOf(0, 0, 1),
        )
        assertEquals(f.take(5), armado)
    }
}
