package ar.lauta.buscarpatentes.data

import kotlin.test.Test
import kotlin.test.assertEquals

/** Qué se quita al tocar una cuadra (FR-012, T024 de la 008). */
class ZonasTest {

    private fun cuadra(id: Long, nombre: String?, gemelaDe: Long? = null) =
        Cuadra(id = id, zonaId = 1, nombre = nombre, forma = "", gemelaDe = gemelaDe)

    // Una avenida de dos manos (1 y su gemela 2, 3 y su gemela 4), una calle (5 y 6) y una sin nombre (7).
    private val cuadras = listOf(
        cuadra(1, "Avenida"),
        cuadra(2, "Avenida", gemelaDe = 1),
        cuadra(3, "Avenida"),
        cuadra(4, "Avenida", gemelaDe = 3),
        cuadra(5, "Calle"),
        cuadra(6, "Calle"),
        cuadra(7, null),
        cuadra(8, null),
    )

    @Test
    fun `una cuadra sola va con su gemela`() {
        assertEquals(setOf(1L, 2L), Zonas.seleccion(1, cuadras, todaLaCalle = false))
        assertEquals(setOf(1L, 2L), Zonas.seleccion(2, cuadras, todaLaCalle = false))
        assertEquals(setOf(5L), Zonas.seleccion(5, cuadras, todaLaCalle = false))
    }

    @Test
    fun `toda la calle son todas las de ese nombre con sus gemelas`() {
        assertEquals(setOf(1L, 2L, 3L, 4L), Zonas.seleccion(4, cuadras, todaLaCalle = true))
        assertEquals(setOf(5L, 6L), Zonas.seleccion(6, cuadras, todaLaCalle = true))
    }

    @Test
    fun `una cuadra sin nombre no tiene calle`() {
        assertEquals(setOf(7L), Zonas.seleccion(7, cuadras, todaLaCalle = true))
    }
}
