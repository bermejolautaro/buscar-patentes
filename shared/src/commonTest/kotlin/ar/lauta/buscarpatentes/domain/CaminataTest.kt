package ar.lauta.buscarpatentes.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Los metros, las calorías y la duración de una salida (contrato C1 y C2 de la 010). */
class CaminataTest {

    private val lat0 = -34.6
    private val lon0 = -58.38

    /** La posición a [x] metros al este y [y] al norte del origen. */
    private fun m(x: Double, y: Double) =
        (lat0 + y / METROS_POR_GRADO) to (lon0 + x / (METROS_POR_GRADO * cos(lat0 * PI / 180)))

    @Test
    fun `sin puntos o con uno no hay metros`() {
        assertEquals(0.0, Caminata.metros(emptyList()))
        assertEquals(0.0, Caminata.metros(listOf(m(0.0, 0.0))))
    }

    @Test
    fun `tres puntos en linea suman sus dos distancias`() {
        assertEquals(150.0, Caminata.metros(listOf(m(0.0, 0.0), m(100.0, 0.0), m(150.0, 0.0))), 0.5)
    }

    @Test
    fun `un salto de mas de 250 m es un hueco y no suma`() {
        val puntos = listOf(m(0.0, 0.0), m(100.0, 0.0), m(500.0, 0.0), m(550.0, 0.0))
        assertEquals(150.0, Caminata.metros(puntos), 0.5)
    }

    @Test
    fun `las calorias son medio kcal por kg y por km redondeadas a 5`() {
        assertEquals(75, Caminata.kcal(2_100.0, 70))
        assertEquals(0, Caminata.kcal(0.0, 70))
        assertEquals(200, Caminata.kcal(5_000.0, 80))
        assertNull(Caminata.kcal(2_100.0, null))
    }

    @Test
    fun `la duracion de una terminada y la de una en curso`() {
        val puntos = listOf(m(0.0, 0.0), m(100.0, 0.0))

        val terminada = Caminata.resumen(iniciadoEn = 1_000, finalizadoEn = 61_000, puntos = puntos, pesoKg = 70, ahora = 999_999)
        assertEquals(60_000, terminada.duracionMs)
        assertEquals(100.0, terminada.metros, 0.5)
        assertEquals(5, terminada.kcal)

        val enCurso = Caminata.resumen(iniciadoEn = 1_000, finalizadoEn = null, puntos = puntos, pesoKg = null, ahora = 121_000)
        assertEquals(120_000, enCurso.duracionMs)
        assertNull(enCurso.kcal)
    }

    private companion object {
        const val METROS_POR_GRADO = 111_194.93
    }
}
