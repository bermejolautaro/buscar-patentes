package ar.lauta.buscarpatentes.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.TimeZone

/**
 * Los textos que reemplazan a `SimpleDateFormat` y `String.format` (D16 de la 006).
 *
 * Los esperados son los que daban `SimpleDateFormat("d MMM yyyy, HH:mm", Locale("es", "AR"))` y
 * `"%.5f".format(...)` con el teléfono en castellano, anotados antes de borrarlos.
 */
class FormatosTest {

    private val buenosAires = TimeZone.of("America/Argentina/Buenos_Aires")

    // 2026-09-05 09:07 y 2026-01-28 23:59, en Buenos Aires.
    private val septiembre = 1_788_610_020_000L
    private val enero = 1_769_655_540_000L

    @Test
    fun `la fecha con anio es la de SimpleDateFormat`() {
        assertEquals("5 sept 2026, 09:07", fechaConAnio(septiembre, buenosAires))
        assertEquals("28 ene 2026, 23:59", fechaConAnio(enero, buenosAires))
    }

    @Test
    fun `la fecha sin anio es la de SimpleDateFormat`() {
        assertEquals("5 sept 09:07", fechaSinAnio(septiembre, buenosAires))
        assertEquals("28 ene 23:59", fechaSinAnio(enero, buenosAires))
    }

    @Test
    fun `los decimales redondean como format y llevan coma`() {
        assertEquals("-34,60374", decimales(-34.603741234, 5))
        assertEquals("-58,38165", decimales(-58.381651, 5))
        assertEquals("12,3", decimales(12.345, 1))
        assertEquals("0,5", decimales(0.46, 1))
        assertEquals("3", decimales(2.6, 0))
    }

    @Test
    fun `las coordenadas para otra app van con punto`() {
        assertEquals("-34.6037400", decimales(-34.60374, 7, '.'))
    }

    @Test
    fun `el numero va con tres cifras`() {
        assertEquals("007", tresCifras(7))
        assertEquals("318", tresCifras(318))
    }
}
