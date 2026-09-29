package ar.lauta.buscarpatentes

import ar.lauta.buscarpatentes.domain.Avisos
import ar.lauta.buscarpatentes.domain.RegistroParaAviso
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

/** D4, FR-028, FR-029, FR-041: qué merece geofence y qué merece notificación. */
class AvisosTest {

    private fun reg(id: Long, numero: Int, compartida: Boolean = false) =
        RegistroParaAviso(id, numero, compartida, -34.6 + id / 1000.0, -58.4)

    // --- Selección de geofences (D4) ---

    @Test
    fun `solo entran los pendientes del numero actual`() {
        val todos = listOf(reg(1, 313), reg(2, 314), reg(3, 312), reg(4, 313, compartida = true))
        val elegidos = Avisos.aRegistrar(todos, numeroActual = 313)
        assertEquals(listOf(1L), elegidos.map { it.id })
    }

    @Test
    fun `los futuros no gastan ranura`() {
        // Es la razon por la que el limite de 100 nunca se alcanza con un archivo grande.
        val archivoGrande = (1..500).map { reg(it.toLong(), 400 + it) }
        assertTrue(Avisos.aRegistrar(archivoGrande, numeroActual = 313).isEmpty())
    }

    /**
     * Regresión de la falla más silenciosa de todo el sistema (T059, C2).
     *
     * Android borra los geofences al reiniciar, pero las preferencias donde la app anota
     * cuáles registró sobreviven. Reconciliar contra esa lista después de un arranque no
     * encuentra ninguna diferencia, así que no da nada de alta: el aviso queda muerto y
     * nada muestra un error. Este test existe porque el bug estuvo escrito y en el teléfono.
     */
    @Test
    fun `tras un reinicio se da todo de alta aunque la app crea que ya estaban`() {
        val loQueLaAppCreeRegistrado = setOf(1L)
        val deseados = listOf(reg(1, 313))

        val enOperacionNormal = Avisos.reconciliar(
            Avisos.previosSegunElSistema(loQueLaAppCreeRegistrado, elSistemaLosOlvido = false),
            deseados,
        )
        assertTrue(enOperacionNormal.sinCambios, "Sin reinicio no hay que despertar al sistema de gusto")

        val despuesDeReiniciar = Avisos.reconciliar(
            Avisos.previosSegunElSistema(loQueLaAppCreeRegistrado, elSistemaLosOlvido = true),
            deseados,
        )
        assertFalse(
            despuesDeReiniciar.sinCambios,
            "Después de reiniciar hay que registrar de nuevo, no confiar en lo anotado",
        )
        assertEquals(listOf(1L), despuesDeReiniciar.aAgregar.map { it.id })
        assertTrue(despuesDeReiniciar.aQuitar.isEmpty(), "No hay nada que dar de baja: el sistema ya los perdió")
    }

    @Test
    fun `si hubiera mas de 100 se trunca de forma determinista, no falla`() {
        val muchos = (1..150).map { reg(it.toLong(), 313) }
        val elegidos = Avisos.aRegistrar(muchos, numeroActual = 313)
        assertEquals(Avisos.LIMITE_GEOFENCES, elegidos.size)
        assertEquals(1L, elegidos.first().id)
        assertEquals(100L, elegidos.last().id)
    }

    @Test
    fun `archivo vacio no rompe`() {
        assertTrue(Avisos.aRegistrar(emptyList(), 313).isEmpty())
    }

    // --- Decisión de aviso (FR-028, FR-029, FR-041) ---

    @Test
    fun `avisa por un pendiente que ya toca`() {
        assertTrue(
            Avisos.corresponde(reg(1, 313), 313, avisosActivos = true, yaAvisadoEnEstaSalida = false),
        )
    }

    @Test
    fun `no avisa por un numero que todavia no toca`() {
        assertFalse(
            Avisos.corresponde(reg(1, 314), 313, avisosActivos = true, yaAvisadoEnEstaSalida = false),
        )
    }

    @Test
    fun `no avisa por una ya compartida`() {
        assertFalse(
            Avisos.corresponde(
                reg(1, 313, compartida = true), 313,
                avisosActivos = true, yaAvisadoEnEstaSalida = false,
            ),
        )
    }

    @Test
    fun `no avisa con los avisos apagados`() {
        assertFalse(
            Avisos.corresponde(reg(1, 313), 313, avisosActivos = false, yaAvisadoEnEstaSalida = false),
        )
    }

    @Test
    fun `no repite el aviso en la misma salida`() {
        assertFalse(
            Avisos.corresponde(reg(1, 313), 313, avisosActivos = true, yaAvisadoEnEstaSalida = true),
        )
    }

    // --- Reconciliación (C2) ---

    @Test
    fun `al avanzar el contador se baja el viejo y se sube el nuevo`() {
        val r = Avisos.reconciliar(activos = setOf(1L), deseados = listOf(reg(2, 314)))
        assertEquals(setOf(1L), r.aQuitar)
        assertEquals(listOf(2L), r.aAgregar.map { it.id })
    }

    @Test
    fun `lo que no cambio no se toca`() {
        // Desregistrar y volver a registrar lo mismo despierta al sistema de gusto.
        val r = Avisos.reconciliar(activos = setOf(1L, 2L), deseados = listOf(reg(1, 313), reg(2, 313)))
        assertTrue(r.sinCambios)
    }

    @Test
    fun `compartir una patente la saca del conjunto`() {
        val r = Avisos.reconciliar(activos = setOf(1L), deseados = emptyList())
        assertEquals(setOf(1L), r.aQuitar)
        assertTrue(r.aAgregar.isEmpty())
    }
}
