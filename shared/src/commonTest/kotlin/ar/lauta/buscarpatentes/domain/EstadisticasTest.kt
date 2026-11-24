package ar.lauta.buscarpatentes.domain

import ar.lauta.buscarpatentes.domain.TipoDePeriodo.DIA
import ar.lauta.buscarpatentes.domain.TipoDePeriodo.MES
import ar.lauta.buscarpatentes.domain.TipoDePeriodo.SEMANA
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/** Los períodos, las sumas y las barras de las estadísticas (contrato C4 y US5 de la 010). */
@OptIn(ExperimentalTime::class)
class EstadisticasTest {

    private val zona = TimeZone.of("America/Argentina/Buenos_Aires")

    private fun a(anio: Int, mes: Int, dia: Int, hora: Int = 12, minuto: Int = 0) =
        LocalDateTime(anio, mes, dia, hora, minuto).toInstant(zona).toEpochMilliseconds()

    private fun salida(iniciadoEn: Long, minutos: Long, metros: Double, kcal: Int?) =
        SalidaParaSumar(iniciadoEn, ResumenDeCaminata(minutos * 60_000, metros, kcal))

    @Test
    fun `la semana empieza el lunes tambien un domingo a la noche`() {
        // El 18 de octubre de 2026 es domingo; el 12, lunes.
        assertEquals(LocalDate(2026, 10, 12), Periodo.de(SEMANA, a(2026, 10, 18, 23, 30), zona).inicio)
        assertEquals(LocalDate(2026, 10, 12), Periodo.de(SEMANA, a(2026, 10, 12, 0, 0), zona).inicio)
        assertEquals(LocalDate(2026, 10, 5), Periodo.de(SEMANA, a(2026, 10, 12), zona).anterior().inicio)
    }

    @Test
    fun `el mes empieza el 1 y el anterior de enero es diciembre`() {
        assertEquals(LocalDate(2026, 10, 1), Periodo.de(MES, a(2026, 10, 17), zona).inicio)
        assertEquals(LocalDate(2025, 12, 1), Periodo.de(MES, a(2026, 1, 20), zona).anterior().inicio)
        assertEquals(LocalDate(2026, 2, 1), Periodo.de(MES, a(2026, 1, 31), zona).siguiente().inicio)
    }

    @Test
    fun `una salida que cruza la medianoche cuenta en el dia en que empezo`() {
        val salidas = listOf(salida(a(2026, 10, 16, 23, 30), minutos = 90, metros = 4_000.0, kcal = 140))
        val viernes = Estadisticas.totales(Periodo.de(DIA, a(2026, 10, 16), zona), salidas, emptyList(), conPeso = true, zona)
        val sabado = Estadisticas.totales(Periodo.de(DIA, a(2026, 10, 17), zona), salidas, emptyList(), conPeso = true, zona)
        assertEquals(1, viernes.salidas)
        assertEquals(0, sabado.salidas)
    }

    @Test
    fun `los totales suman salidas tiempo metros calorias y patentes`() {
        val salidas = listOf(
            salida(a(2026, 10, 17, 9), minutos = 30, metros = 1_500.0, kcal = 50),
            salida(a(2026, 10, 17, 18), minutos = 40, metros = 2_000.0, kcal = 70),
            salida(a(2026, 10, 16, 18), minutos = 60, metros = 3_000.0, kcal = 105),
        )
        // Dos patentes el sábado, una de ellas fuera de cualquier salida, y una el viernes.
        val patentes = listOf(a(2026, 10, 17, 9, 10), a(2026, 10, 17, 21), a(2026, 10, 16, 18, 30))

        val sabado = Estadisticas.totales(Periodo.de(DIA, a(2026, 10, 17), zona), salidas, patentes, conPeso = true, zona)
        assertEquals(TotalesDelPeriodo(salidas = 2, duracionMs = 70 * 60_000, metros = 3_500.0, kcal = 120, patentes = 2), sabado)

        val semana = Estadisticas.totales(Periodo.de(SEMANA, a(2026, 10, 17), zona), salidas, patentes, conPeso = true, zona)
        assertEquals(3, semana.salidas)
        assertEquals(6_500.0, semana.metros)
        assertEquals(225, semana.kcal)
        assertEquals(3, semana.patentes)
    }

    @Test
    fun `sin peso no hay calorias y un periodo vacio da cero`() {
        val salidas = listOf(salida(a(2026, 10, 17), minutos = 30, metros = 1_500.0, kcal = null))
        assertNull(Estadisticas.totales(Periodo.de(DIA, a(2026, 10, 17), zona), salidas, emptyList(), conPeso = false, zona).kcal)

        val vacio = Estadisticas.totales(Periodo.de(DIA, a(2026, 10, 10), zona), salidas, emptyList(), conPeso = true, zona)
        assertEquals(TotalesDelPeriodo(salidas = 0, duracionMs = 0, metros = 0.0, kcal = 0, patentes = 0), vacio)
    }

    @Test
    fun `las barras son 7 dias 8 semanas o 12 meses y terminan en el periodo elegido`() {
        val salidas = listOf(
            salida(a(2026, 10, 17), minutos = 30, metros = 1_500.0, kcal = null),
            salida(a(2026, 10, 11), minutos = 30, metros = 2_000.0, kcal = null),
        )
        val dias = Estadisticas.barras(Periodo.de(DIA, a(2026, 10, 17), zona), salidas, zona)
        assertEquals(7, dias.size)
        assertEquals(LocalDate(2026, 10, 17), dias.last().first.inicio)
        assertEquals(LocalDate(2026, 10, 11), dias.first().first.inicio)
        assertEquals(listOf(2_000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1_500.0), dias.map { it.second })

        assertEquals(8, Estadisticas.barras(Periodo.de(SEMANA, a(2026, 10, 17), zona), salidas, zona).size)
        val meses = Estadisticas.barras(Periodo.de(MES, a(2026, 10, 17), zona), salidas, zona)
        assertEquals(12, meses.size)
        assertEquals(3_500.0, meses.last().second)
    }

    @Test
    fun `el actual no tiene siguiente`() {
        val ahora = a(2026, 10, 17)
        assertFalse(Periodo.de(SEMANA, ahora, zona).haySiguiente(ahora, zona))
        assertTrue(Periodo.de(SEMANA, ahora, zona).anterior().haySiguiente(ahora, zona))
    }
}
