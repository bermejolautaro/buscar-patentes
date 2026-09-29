package ar.lauta.buscarpatentes.domain

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

/**
 * Pines que se hacen lugar antes de agruparse (D13 de la 005, US5).
 *
 * Los números de referencia, a la latitud de Buenos Aires: a zoom 17 un dp son unos 0,49 m, a
 * zoom 13 unos 7,9 m. El ancho del pin es el del mapa, 34 dp.
 */
class AcomodoTest {

    private val ancho = 34.0
    private val base = -34.60374 to -58.38165

    /** Un pin a [metrosNorte] al norte del punto base. Un grado de latitud son ~111 km. */
    private fun pin(id: Long, metrosNorte: Double = 0.0) =
        Pin(id, base.first + metrosNorte / 111_195.0, base.second)

    private fun metros(a: Dibujo.Suelto, latitud: Double, longitud: Double) =
        Geo.distanciaMetros(a.latitud, a.longitud, latitud, longitud)

    @Test
    fun `un pin solo no se mueve`() {
        val p = pin(1)
        assertEquals(listOf(Dibujo.Suelto(1, p.latitud, p.longitud)), Acomodo.para(listOf(p), 17, ancho))
    }

    @Test
    fun `dos pines cercanos se separan a zoom de calle, sin irse de la cuadra`() {
        val a = pin(1)
        val b = pin(2, 10.0)
        val dibujos = Acomodo.para(listOf(a, b), 17, ancho)

        assertEquals(2, dibujos.size)
        val sueltos = dibujos.map { it as Dibujo.Suelto }.associateBy { it.id }
        val da = sueltos.getValue(1)
        val db = sueltos.getValue(2)

        // Cada uno a menos de media cuadra de su lugar real.
        assertTrue(metros(da, a.latitud, a.longitud) <= Acomodo.LIMITE_ACOMODO_M)
        assertTrue(metros(db, b.latitud, b.longitud) <= Acomodo.LIMITE_ACOMODO_M)

        // Y separados al menos un ancho de pin: 34 dp a 0,49 m/dp son unos 16,7 m.
        val separacion = Geo.distanciaMetros(da.latitud, da.longitud, db.latitud, db.longitud)
        assertTrue(separacion >= 16.0, "quedaron a $separacion m")
    }

    @Test
    fun `los mismos dos se agrupan de lejos, donde no entran sin irse de la cuadra`() {
        val dibujos = Acomodo.para(listOf(pin(1), pin(2, 10.0)), 13, ancho)
        assertEquals(1, dibujos.size)
        assertEquals(2, (dibujos.single() as Dibujo.Grupo).cuenta)
    }

    @Test
    fun `veinte en el mismo lugar son un grupo a zoom 15`() {
        val veinte = (1L..20L).map { pin(it) }
        val dibujos = Acomodo.para(veinte, 15, ancho)
        assertEquals(listOf(20), dibujos.map { (it as Dibujo.Grupo).cuenta })
    }

    @Test
    fun `lejos entre si no se tocan ni se agrupan`() {
        val dibujos = Acomodo.para(listOf(pin(1), pin(2, 500.0)), 15, ancho)
        assertTrue(dibujos.all { it is Dibujo.Suelto })
        val a = dibujos.first { (it as Dibujo.Suelto).id == 1L } as Dibujo.Suelto
        assertEquals(base.first, a.latitud, 1e-12)
    }

    @Test
    fun `una cadena no termina en un solo grupo con el centro en ninguna parte`() {
        // Cinco en fila, cada uno a 30 m del anterior. A zoom 15 (~2 m/dp) cada uno toca al
        // siguiente, pero separarlos correría las puntas ~74 m: no entran sueltos. Y el primero y
        // el último están a 120 m, más de un ancho de pin: tiene que haber más de un grupo.
        val fila = (0..4).map { pin(it.toLong() + 1, it * 30.0) }
        val dibujos = Acomodo.para(fila, 15, ancho)
        assertTrue(dibujos.size > 1, "salieron ${dibujos.size}")
        assertEquals(5, dibujos.sumOf { if (it is Dibujo.Grupo) it.cuenta else 1 })
    }

    @Test
    fun `la misma entrada da siempre lo mismo`() {
        // FR-023c: desplazar el mapa no cambia el zoom, y con el mismo zoom nada se mueve.
        val pines = listOf(pin(3), pin(1, 5.0), pin(2, 12.0), pin(4, 12.0))
        assertEquals(Acomodo.para(pines, 17, ancho), Acomodo.para(pines.reversed(), 17, ancho))
    }

    @Test
    fun `nadie se pierde`() {
        val pines = (1L..12L).map { pin(it, (it % 4) * 8.0) }
        for (zoom in 12..19) {
            val dibujos = Acomodo.para(pines, zoom, ancho)
            assertEquals(
                12,
                dibujos.sumOf { if (it is Dibujo.Grupo) it.cuenta else 1 },
                "zoom $zoom",
            )
        }
    }
}
