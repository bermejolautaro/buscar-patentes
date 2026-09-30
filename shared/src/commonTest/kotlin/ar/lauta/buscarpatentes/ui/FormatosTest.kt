package ar.lauta.buscarpatentes.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

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

    /** Medianoche en Buenos Aires del día [dia] de [mes] de 2026. */
    @OptIn(ExperimentalTime::class)
    private fun dia(mes: Int, dia: Int): Long =
        kotlinx.datetime.LocalDate(2026, mes, dia).atStartOfDayIn(buenosAires).toEpochMilliseconds()

    @Test
    fun `la duracion va en dias hasta un mes y despues en meses y dias`() {
        val desde = dia(9, 1)
        assertEquals("Desde hoy", duracion(desde, desde + 5 * HORA, buenosAires))
        assertEquals("1 día", duracion(desde, dia(9, 2), buenosAires))
        assertEquals("23 días", duracion(desde, dia(9, 24), buenosAires))
        assertEquals("30 días", duracion(desde, dia(10, 1), buenosAires))
        assertEquals("1 mes y 1 día", duracion(desde, dia(10, 2), buenosAires))
        assertEquals("2 meses", duracion(desde, dia(11, 1), buenosAires))
        assertEquals("2 meses y 4 días", duracion(desde, dia(11, 5), buenosAires))
        // A las 23 del mismo día sigue siendo hoy: se cuentan días del calendario, no horas.
        assertEquals("Desde hoy", duracion(desde, desde + 23 * HORA, buenosAires))
    }

    @Test
    fun `la fecha corta y el comienzo del dia`() {
        assertEquals("05/09/2026", fechaCorta(septiembre, buenosAires))
        assertEquals("05/09", fechaCorta(septiembre, buenosAires, conAnio = false))
        assertEquals(dia(9, 5), inicioDelDia(septiembre, buenosAires))
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

    private companion object {
        const val HORA = 3_600_000L
    }
}
