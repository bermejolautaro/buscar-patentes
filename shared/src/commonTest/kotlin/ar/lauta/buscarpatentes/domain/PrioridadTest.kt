package ar.lauta.buscarpatentes.domain

import kotlin.test.assertEquals
import kotlin.test.Test

/**
 * El orden en que conviene revisar las patentes de un número (D8 de la 005).
 *
 * Los números de los escenarios son los de la US3 de la spec, que son la referencia de cómo
 * tiene que comportarse la cuenta. Las posiciones se arman corriendo la latitud de un punto
 * fijo hacia el norte: un grado de latitud son unos 111 km en cualquier parte del mundo, así
 * que los metros salen con una cuenta y no hay que confiar en el mapa.
 */
class PrioridadTest {

    private val aca = -34.60374 to -58.38165

    private fun aMetros(id: Long, metros: Double, confianza: Int = 0, capturadoEn: Long = 0) =
        Candidato(
            id = id,
            latitud = aca.first + metros / 111_195.0,
            longitud = aca.second,
            confianza = confianza,
            capturadoEn = capturadoEn,
        )

    private fun ids(lista: List<Candidato>) = lista.map { it.id }

    @Test
    fun `con la misma confianza primero la mas cercana`() {
        val lejos = aMetros(1, 600.0)
        val cerca = aMetros(2, 100.0)
        assertEquals(listOf(2L, 1L), ids(Prioridad.paraRevisar(listOf(lejos, cerca), aca)))
    }

    @Test
    fun `una muy cercana va primero aunque tenga poca confianza`() {
        // Escenario 4 de la US3: 50 contra 240 / 3 = 80.
        val confirmadaLejos = aMetros(1, 240.0, confianza = 10)
        val recienAnotadaCerca = aMetros(2, 50.0, confianza = 0)
        assertEquals(
            listOf(2L, 1L),
            ids(Prioridad.paraRevisar(listOf(confirmadaLejos, recienAnotadaCerca), aca)),
        )
    }

    @Test
    fun `la confianza compensa cuando la diferencia de distancia es chica`() {
        // Escenario 5 de la US3: 300 contra 500 / 3 = 167.
        val sinConfirmar = aMetros(1, 300.0, confianza = 0)
        val confirmada = aMetros(2, 500.0, confianza = 10)
        assertEquals(listOf(2L, 1L), ids(Prioridad.paraRevisar(listOf(sinConfirmar, confirmada), aca)))
    }

    @Test
    fun `nunca pasa adelante una al doble de distancia con igual o menos confianza`() {
        // SC-005.
        val cerca = aMetros(1, 200.0, confianza = 3)
        val alDobleYPico = aMetros(2, 450.0, confianza = 3)
        val alDobleConMenos = aMetros(3, 450.0, confianza = 1)
        assertEquals(
            listOf(1L, 2L, 3L),
            ids(Prioridad.paraRevisar(listOf(alDobleConMenos, alDobleYPico, cerca), aca)),
        )
    }

    @Test
    fun `a igualdad primero la capturada mas recientemente`() {
        val vieja = aMetros(1, 100.0, capturadoEn = 1_000)
        val nueva = aMetros(2, 100.0, capturadoEn = 2_000)
        assertEquals(listOf(2L, 1L), ids(Prioridad.paraRevisar(listOf(vieja, nueva), aca)))
    }

    @Test
    fun `el orden de paradas va cada vez a la mas cercana de la anterior`() {
        // Tres en línea hacia el norte. La del medio tiene tanta confianza que para revisar va
        // primero (400 / 3 = 133 contra 200), pero para caminar conviene ir en orden: 200, 400,
        // 700.
        val primera = aMetros(1, 200.0, confianza = 0)
        val segunda = aMetros(2, 400.0, confianza = 10)
        val tercera = aMetros(3, 700.0, confianza = 0)
        val todas = listOf(tercera, primera, segunda)

        assertEquals(listOf(2L, 1L, 3L), ids(Prioridad.paraRevisar(todas, aca)))
        assertEquals(listOf(1L, 2L, 3L), ids(Prioridad.ordenDeParadas(todas, aca)))
    }

    @Test
    fun `el orden de paradas sigue desde la ultima no desde el jugador`() {
        // Desde el jugador, la más cercana es la de 300 m al norte (la del sur está a 350). Desde
        // ahí, la de 500 m al norte queda a 200 y la del sur a 650: se sigue para el norte y se
        // vuelve al sur al final, en vez de ir y volver dos veces.
        val norte = aMetros(1, 300.0)
        val sur = aMetros(2, -350.0)
        val masAlNorte = aMetros(3, 500.0)
        assertEquals(listOf(1L, 3L, 2L), ids(Prioridad.ordenDeParadas(listOf(sur, masAlNorte, norte), aca)))
    }

    @Test
    fun `el orden de paradas con una o ninguna`() {
        assertEquals(emptyList<Long>(), ids(Prioridad.ordenDeParadas(emptyList(), aca)))
        assertEquals(listOf(9L), ids(Prioridad.ordenDeParadas(listOf(aMetros(9, 50.0)), aca)))
    }

    @Test
    fun `el orden de paradas sin posicion es el de revision`() {
        val a = aMetros(1, 10.0, confianza = 0)
        val b = aMetros(2, 900.0, confianza = 7)
        assertEquals(
            ids(Prioridad.paraRevisar(listOf(a, b), null)),
            ids(Prioridad.ordenDeParadas(listOf(a, b), null)),
        )
    }

    @Test
    fun `sin posicion ordena por confianza y despues por fecha`() {
        val a = aMetros(1, 10.0, confianza = 0, capturadoEn = 3_000)
        val b = aMetros(2, 900.0, confianza = 7, capturadoEn = 1_000)
        val c = aMetros(3, 500.0, confianza = 0, capturadoEn = 5_000)
        assertEquals(listOf(2L, 3L, 1L), ids(Prioridad.paraRevisar(listOf(a, b, c), null)))
    }
}
