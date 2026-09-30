package ar.lauta.buscarpatentes.domain

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** El borde de una zona: cuándo es un dibujo válido y qué queda adentro (FR-001, D4 de la 008). */
class BordeTest {

    // Un cuadrado de unos 200 m cerca del Obelisco, en el orden en que se tocarían las esquinas.
    private val cuadrado = listOf(
        -34.6030 to -58.3830,
        -34.6030 to -58.3808,
        -34.6048 to -58.3808,
        -34.6048 to -58.3830,
    )

    // Una "U": el hueco del medio queda afuera.
    private val u = listOf(
        -34.6030 to -58.3830,
        -34.6030 to -58.3824,
        -34.6042 to -58.3824,
        -34.6042 to -58.3814,
        -34.6030 to -58.3814,
        -34.6030 to -58.3808,
        -34.6048 to -58.3808,
        -34.6048 to -58.3830,
    )

    @Test
    fun `con dos esquinas faltan esquinas`() {
        assertEquals(Borde.FALTAN_ESQUINAS, Borde.problema(cuadrado.take(2)))
        assertEquals(Borde.FALTAN_ESQUINAS, Borde.problema(emptyList()))
    }

    @Test
    fun `un moño se cruza`() {
        val mono = listOf(cuadrado[0], cuadrado[2], cuadrado[1], cuadrado[3])
        assertEquals(Borde.SE_CRUZA, Borde.problema(mono))
    }

    @Test
    fun `un cuadrado y una U son validos`() {
        assertNull(Borde.problema(cuadrado))
        assertNull(Borde.problema(u))
        assertNull(Borde.problema(cuadrado.take(3)))
    }

    @Test
    fun `adentro y afuera`() {
        assertTrue(Borde.adentro(-34.6039 to -58.3819, cuadrado))
        assertFalse(Borde.adentro(-34.6060 to -58.3819, cuadrado))
        assertFalse(Borde.adentro(-34.6039 to -58.3840, cuadrado))
        // En la U: un brazo adentro, el hueco del medio afuera.
        assertTrue(Borde.adentro(-34.6036 to -58.3827, u))
        assertFalse(Borde.adentro(-34.6034 to -58.3819, u))
        assertTrue(Borde.adentro(-34.6045 to -58.3819, u))
    }

    @Test
    fun `el rectangulo se agranda 200 m para cada lado`() {
        val r = Borde.rectangulo(cuadrado)
        fun cerca(esperado: Double, real: Double) = assertTrue(abs(esperado - real) < 2.0, "esperaba $esperado m, fue $real m")
        cerca(200.0, Geo.distanciaMetros(-34.6048, -58.3819, r.sur, -58.3819))
        cerca(200.0, Geo.distanciaMetros(-34.6030, -58.3819, r.norte, -58.3819))
        cerca(200.0, Geo.distanciaMetros(-34.6039, -58.3830, -34.6039, r.oeste))
        cerca(200.0, Geo.distanciaMetros(-34.6039, -58.3808, -34.6039, r.este))
    }

    @Test
    fun `el borde va y vuelve como texto`() {
        assertEquals(cuadrado, Borde.leer(Borde.escribir(cuadrado)))
    }
}
