package ar.lauta.buscarpatentes

import ar.lauta.buscarpatentes.domain.Cobertura
import kotlin.test.assertEquals
import kotlin.test.Test

/**
 * FR-003 de la 005: cuántas patentes hay del número que toca, dicho en castellano.
 *
 * Este archivo probaba la cuenta de números seguidos cubiertos de la 001 (FR-022). La 005 la
 * retiró de la pantalla (FR-003a) porque el jugador no sabía qué significaba, y con ella se
 * fueron sus pruebas: probar código borrado no protege nada.
 */
class CoberturaTest {

    @Test
    fun `ninguna`() {
        assertEquals("Ninguna descubierta", Cobertura.descubiertas(0))
    }

    @Test
    fun `una va en singular`() {
        assertEquals("1 descubierta", Cobertura.descubiertas(1))
    }

    @Test
    fun `mas de una va en plural`() {
        assertEquals("3 descubiertas", Cobertura.descubiertas(3))
        assertEquals("12 descubiertas", Cobertura.descubiertas(12))
    }
}
