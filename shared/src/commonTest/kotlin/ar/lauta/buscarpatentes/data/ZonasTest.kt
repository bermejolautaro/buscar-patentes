package ar.lauta.buscarpatentes.data

import ar.lauta.buscarpatentes.domain.CaminoGuardado
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Qué se quita al tocar una cuadra (FR-012, T024 de la 008), y qué tramos le pasa cada salida a la
 * cuenta de una zona (FR-001 a FR-003 de la 011).
 */
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

    // --- Lo que cuenta de cada salida (011) ---

    private val ajustado = listOf(listOf(-34.6 to -58.38, -34.6 to -58.379))

    private fun recorrido(id: Long, camino: String?, estado: EstadoRecorrido = EstadoRecorrido.TERMINADO) =
        Recorrido(id = id, iniciadoEn = id * 1_000, finalizadoEn = id * 1_000 + 500, estado = estado, caminoAjustado = camino)

    private fun punto(recorridoId: Long, lat: Double, lon: Double) =
        PuntoDeTrayecto(recorridoId = recorridoId, latitud = lat, longitud = lon, precisionMetros = 5f, registradoEn = 0)

    @Test
    fun `una salida cuenta con lo ajustado y con lo medido`() {
        val medidos = listOf(punto(1, -34.6, -58.38), punto(1, -34.6001, -58.3795))
        val salidas = Zonas.salidasParaContar(
            listOf(recorrido(1, CaminoGuardado.escribir(ajustado))),
            mapOf(1L to medidos),
        )
        assertEquals(1, salidas.size)
        assertEquals(ajustado + listOf(medidos.map { it.latitud to it.longitud }), salidas.single().tramos)
    }

    @Test
    fun `una salida sin ajustar cuenta con lo medido y una en curso no cuenta`() {
        val medidos = listOf(punto(2, -34.6, -58.38), punto(2, -34.6001, -58.3795))
        val salidas = Zonas.salidasParaContar(
            listOf(recorrido(2, null), recorrido(3, null, EstadoRecorrido.EN_CURSO)),
            mapOf(2L to medidos, 3L to medidos),
        )
        assertEquals(listOf(2_000L), salidas.map { it.iniciadoEn })
        assertEquals(listOf(medidos.map { it.latitud to it.longitud }), salidas.single().tramos)
    }

    @Test
    fun `un hueco de mas de 250 m parte lo medido`() {
        // De -58.38 a -58.37 hay unos 900 m: un hueco.
        val medidos = listOf(punto(4, -34.6, -58.38), punto(4, -34.6, -58.3799), punto(4, -34.6, -58.37), punto(4, -34.6, -58.3699))
        val tramos = Zonas.salidasParaContar(listOf(recorrido(4, null)), mapOf(4L to medidos)).single().tramos
        assertEquals(2, tramos.size)
    }
}
