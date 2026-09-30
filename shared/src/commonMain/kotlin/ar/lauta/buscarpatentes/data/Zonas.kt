package ar.lauta.buscarpatentes.data

import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.domain.Borde
import ar.lauta.buscarpatentes.domain.CaminoGuardado
import ar.lauta.buscarpatentes.domain.CoberturaDeZona
import ar.lauta.buscarpatentes.domain.CuadraParaContar
import ar.lauta.buscarpatentes.domain.CuentaDeZona
import ar.lauta.buscarpatentes.domain.Polilinea
import ar.lauta.buscarpatentes.domain.SalidaParaContar
import ar.lauta.buscarpatentes.ui.fechaCorta
import ar.lauta.buscarpatentes.ui.inicioDelDia

/**
 * Lo que las pantallas le piden a la base sobre las zonas (008).
 *
 * Las salidas se leen y nunca se escriben (FR-019): la cuenta sale de [CoberturaDeZona], que es una
 * función pura, contra los caminos ajustados que ya guardó la 007.
 */
object Zonas {

    /** Guarda una zona nueva, todavía sin cuadras. Las busca [ar.lauta.buscarpatentes.ubicacion.BuscarCuadras]. */
    suspend fun crear(nombre: String, esquinas: List<Pair<Double, Double>>, cuentaDesde: Long, ahora: Long): Long =
        contenedor.zonas.insertar(
            Zona(
                nombre = nombre.trim().ifEmpty { "Zona del ${fechaCorta(ahora, conAnio = false)}" },
                borde = Borde.escribir(esquinas),
                creadaEn = ahora,
                cuentaDesde = inicioDelDia(minOf(cuentaDesde, ahora)),
            ),
        )

    /** Corre "desde cuándo cuenta" (FR-003). Nunca después de hoy, y solo con la zona activa. */
    suspend fun cambiarCuentaDesde(id: Long, desde: Long, ahora: Long) {
        contenedor.zonas.cambiarCuentaDesde(id, inicioDelDia(minOf(desde, ahora)))
        revisar()
    }

    suspend fun salidasParaContar(): List<SalidaParaContar> = salidasParaContar(contenedor.recorridos.todosUnaVez())

    /** Las salidas que pueden contar para una zona: terminadas y ajustadas a las calles. */
    fun salidasParaContar(recorridos: List<Recorrido>): List<SalidaParaContar> =
        recorridos
            .filter { it.estado == EstadoRecorrido.TERMINADO }
            .mapNotNull { r ->
                (CaminoGuardado.leer(r.caminoAjustado) as? CaminoGuardado.Ajustado)
                    ?.let { SalidaParaContar(r.iniciadoEn, it.tramos) }
            }

    fun paraContar(c: Cuadra) = CuadraParaContar(c.id, Polilinea.decodificar(c.forma), c.gemelaDe, c.quitada)

    /** Cómo va [zona] con estas [cuadras] y estas [salidas]. */
    fun cuenta(zona: Zona, cuadras: List<CuadraParaContar>, salidas: List<SalidaParaContar>): CuentaDeZona =
        CoberturaDeZona.calcular(cuadras, salidas, zona.cuentaDesde)

    /**
     * Qué cuadras se quitan o se suman al tocar una (FR-012): esa sola, o todas las de su calle
     * adentro de la zona. Siempre con su gemela: las dos manos de una avenida van juntas.
     * Una cuadra sin nombre no tiene calle con la que juntarse.
     */
    fun seleccion(tocada: Long, cuadras: List<Cuadra>, todaLaCalle: Boolean): Set<Long> {
        val cuadra = cuadras.firstOrNull { it.id == tocada } ?: return emptySet()
        val elegidas = if (todaLaCalle && cuadra.nombre != null) cuadras.filter { it.nombre == cuadra.nombre } else listOf(cuadra)
        val principales = elegidas.map { it.gemelaDe ?: it.id }.toSet()
        return cuadras.filter { (it.gemelaDe ?: it.id) in principales }.map { it.id }.toSet()
    }

    /** Quita o vuelve a sumar (FR-013). Solo con la zona activa: una terminada no cambia. */
    suspend fun quitar(zonaId: Long, tocada: Long, todaLaCalle: Boolean, quitada: Boolean) {
        if (contenedor.zonas.porId(zonaId)?.estado != EstadoZona.ACTIVA) return
        val ids = seleccion(tocada, contenedor.zonas.cuadrasDe(zonaId), todaLaCalle)
        if (ids.isEmpty()) return
        contenedor.zonas.marcarQuitadas(ids.toList(), quitada)
        // Quitar la última pendiente completa la zona.
        revisar()
    }

    /**
     * Completa las zonas activas que llegaron al 100% (FR-014). Devuelve cuántas.
     *
     * Corre cada vez que puede haber un camino nuevo o una cuenta distinta: después del ajuste a
     * calles y de la búsqueda de cuadras, al quitar y al cambiar "desde cuándo cuenta" (D7).
     */
    suspend fun revisar(): Int {
        val activas = contenedor.zonas.activas()
        if (activas.isEmpty()) return 0
        val salidas = salidasParaContar()
        var completadas = 0
        for (zona in activas) {
            val cuenta = cuenta(zona, contenedor.zonas.cuadrasDe(zona.id).map(::paraContar), salidas)
            val en = cuenta.completadaEn ?: continue
            contenedor.zonas.terminar(zona.id, EstadoZona.COMPLETADA, en, 100, cuenta.recorridas.toList())
            completadas++
        }
        return completadas
    }

    /** Cierra el objetivo a mano, con el porcentaje de ese momento (FR-015). */
    suspend fun cerrar(id: Long, ahora: Long) {
        val zona = contenedor.zonas.porId(id)?.takeIf { it.estado == EstadoZona.ACTIVA } ?: return
        val cuenta = cuenta(zona, contenedor.zonas.cuadrasDe(id).map(::paraContar), salidasParaContar())
        contenedor.zonas.terminar(id, EstadoZona.CERRADA, ahora, cuenta.porcentaje, cuenta.recorridas.toList())
    }

    /** Borra la zona con sus cuadras (FR-018). Las salidas no se tocan. */
    suspend fun borrar(id: Long) = contenedor.zonas.borrarConSusCuadras(id)
}
