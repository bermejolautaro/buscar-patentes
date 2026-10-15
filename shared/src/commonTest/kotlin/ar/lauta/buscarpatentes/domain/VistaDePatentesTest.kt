package ar.lauta.buscarpatentes.domain

import ar.lauta.buscarpatentes.domain.VistaDePatentes.DESDE_LA_QUE_TOCA
import ar.lauta.buscarpatentes.domain.VistaDePatentes.NINGUNA
import ar.lauta.buscarpatentes.domain.VistaDePatentes.TODAS
import kotlin.test.Test
import kotlin.test.assertEquals

/** Qué patentes se ven en cada vista del botón (FR-001, FR-002 y FR-006 de la 009). */
class VistaDePatentesTest {

    private val guardadas = listOf(120, 313, 500)

    private fun vistas(vista: VistaDePatentes, queToca: Int?, filtro: Int? = null) =
        guardadas.filter { vista.muestra(it, queToca, filtro) }

    @Test
    // Sin comas en los nombres: Kotlin/Native no las acepta.
    fun `el ciclo es mayor o igual y despues todas y ninguna y vuelve`() {
        assertEquals(TODAS, DESDE_LA_QUE_TOCA.siguiente())
        assertEquals(NINGUNA, TODAS.siguiente())
        assertEquals(DESDE_LA_QUE_TOCA, NINGUNA.siguiente())
    }

    @Test
    fun `cada vista sin lupa`() {
        assertEquals(listOf(313, 500), vistas(DESDE_LA_QUE_TOCA, queToca = 313))
        assertEquals(guardadas, vistas(TODAS, queToca = 313))
        assertEquals(emptyList(), vistas(NINGUNA, queToca = 313))
    }

    @Test
    fun `sin saber todavia cual toca se ven todas`() {
        assertEquals(guardadas, vistas(DESDE_LA_QUE_TOCA, queToca = null))
    }

    @Test
    fun `la lupa gana aunque el numero sea menor que el que toca`() {
        assertEquals(listOf(120), vistas(DESDE_LA_QUE_TOCA, queToca = 313, filtro = 120))
        assertEquals(listOf(120), vistas(TODAS, queToca = 313, filtro = 120))
        assertEquals(emptyList(), vistas(NINGUNA, queToca = 313, filtro = 120))
    }
}
