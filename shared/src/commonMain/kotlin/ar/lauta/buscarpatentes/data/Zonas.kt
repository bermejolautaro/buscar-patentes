package ar.lauta.buscarpatentes.data

import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.domain.Borde
import ar.lauta.buscarpatentes.domain.CaminoGuardado
import ar.lauta.buscarpatentes.domain.CoberturaDeZona
import ar.lauta.buscarpatentes.domain.CuadraParaContar
import ar.lauta.buscarpatentes.domain.CuentaDeZona
import ar.lauta.buscarpatentes.domain.Geo
import ar.lauta.buscarpatentes.domain.Polilinea
import ar.lauta.buscarpatentes.domain.SalidaParaContar
import ar.lauta.buscarpatentes.plataforma.notificar
import ar.lauta.buscarpatentes.ui.DESDE_HOY
import ar.lauta.buscarpatentes.ui.duracion
import ar.lauta.buscarpatentes.ui.fechaCorta
import ar.lauta.buscarpatentes.ui.inicioDelDia

/**
 * Lo que las pantallas le piden a la base sobre las zonas (008).
 *
 * Las salidas se leen y nunca se escriben (FR-019): la cuenta sale de [CoberturaDeZona], que es una
 * función pura, contra los caminos ajustados que ya guardó la 007 y, desde la 011, contra lo que
 * midió el teléfono.
 */
object Zonas {

    /** Guarda una zona nueva, todavía sin cuadras. Las busca [ar.lauta.buscarpatentes.ubicacion.BuscarCuadras]. */
    suspend fun crear(nombre: String, esquinas: List<Pair<Double, Double>>, cuentaDesde: Long, ahora: Long): Long =
        contenedor.zonas.insertar(
            Zona(
                nombre = nombre.trim().ifEmpty { nombrePorDefecto(ahora) },
                borde = Borde.escribir(esquinas),
                creadaEn = ahora,
                cuentaDesde = inicioDelDia(minOf(cuentaDesde, ahora)),
            ),
        )

    private fun nombrePorDefecto(creadaEn: Long) = "Zona del ${fechaCorta(creadaEn, conAnio = false)}"

    /**
     * Cambia el nombre, el borde y desde cuándo cuenta de una zona que no terminó (FR-022). Con otro
     * borde la zona vuelve a buscar sus cuadras. Las viejas quedan hasta que llegan las nuevas, que
     * heredan lo quitado.
     */
    suspend fun editar(id: Long, nombre: String, esquinas: List<Pair<Double, Double>>, cuentaDesde: Long, ahora: Long) {
        val zona = contenedor.zonas.porId(id) ?: return
        if (zona.estado != EstadoZona.ACTIVA && zona.estado != EstadoZona.BUSCANDO) return
        val borde = Borde.escribir(esquinas)
        val otroBorde = borde != zona.borde
        contenedor.zonas.editar(
            id = id,
            nombre = nombre.trim().ifEmpty { nombrePorDefecto(zona.creadaEn) },
            borde = borde,
            desde = inicioDelDia(minOf(cuentaDesde, ahora)),
            estado = if (otroBorde) EstadoZona.BUSCANDO else zona.estado,
            problema = if (otroBorde) null else zona.problema,
        )
        revisar()
    }

    /** Corre "desde cuándo cuenta" (FR-003). Nunca después de hoy, y solo con la zona activa. */
    suspend fun cambiarCuentaDesde(id: Long, desde: Long, ahora: Long) {
        contenedor.zonas.cambiarCuentaDesde(id, inicioDelDia(minOf(desde, ahora)))
        revisar()
    }

    suspend fun salidasParaContar(): List<SalidaParaContar> =
        salidasParaContar(contenedor.recorridos.todosUnaVez(), contenedor.puntos.todos().groupBy { it.recorridoId })

    /**
     * Las salidas que cuentan para una zona: las terminadas, con su camino ajustado si lo tienen
     * **y** lo que midió el teléfono (FR-001 a FR-003 de la 011).
     *
     * Lo medido se corta en los mismos huecos que el mapa: la recta de un hueco no cubre nada. Hace
     * falta porque el ajuste a veces pega la caminata a la calle de al lado: un pasaje de 35 m junto
     * a una diagonal quedaba al 57% por más que se caminara entero. Una salida sin ajustar todavía
     * cuenta con lo medido; cuando se ajusta, suma lo ajustado.
     */
    fun salidasParaContar(recorridos: List<Recorrido>, puntosPorRecorrido: Map<Long, List<PuntoDeTrayecto>>): List<SalidaParaContar> =
        recorridos
            .filter { it.estado == EstadoRecorrido.TERMINADO }
            .map { r ->
                val ajustados = (CaminoGuardado.leer(r.caminoAjustado) as? CaminoGuardado.Ajustado)?.tramos.orEmpty()
                val medidos = Geo.tramos(puntosPorRecorrido[r.id].orEmpty().map { it.latitud to it.longitud })
                SalidaParaContar(r.iniciadoEn, ajustados + medidos)
            }
            .filter { it.tramos.isNotEmpty() }

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
     * Completa las zonas activas que llegaron al 100% (FR-014), y felicita cada una (FR-005 de la
     * 011). Devuelve cuántas.
     *
     * Corre cada vez que puede haber un camino nuevo o una cuenta distinta: al terminar una salida,
     * después del ajuste a calles y de la búsqueda de cuadras, al quitar y al cambiar "desde cuándo
     * cuenta" (D7, D3 de la 011).
     */
    suspend fun revisar(): Int {
        val activas = contenedor.zonas.activas()
        if (activas.isEmpty()) return 0
        val salidas = salidasParaContar()
        var completadas = 0
        for (zona in activas) {
            val cuenta = cuenta(zona, contenedor.zonas.cuadrasDe(zona.id).map(::paraContar), salidas)
            val en = cuenta.completadaEn ?: continue
            if (!contenedor.zonas.terminar(zona.id, EstadoZona.COMPLETADA, en, 100, cuenta.recorridas.toList())) continue
            felicitar(zona, en)
            completadas++
        }
        return completadas
    }

    /**
     * La notificación de una zona que llegó al 100% (FR-005 de la 011). En cuánto se completó cuenta
     * desde "desde cuándo cuenta", como el resto de la zona. Cerrar a mano no pasa por acá.
     */
    private fun felicitar(zona: Zona, en: Long) {
        val tiempo = duracion(zona.cuentaDesde, en).let { if (it == DESDE_HOY) "en el día" else "en $it" }
        notificar(zona.id.toInt(), "¡Zona completada!", "Recorriste todas las cuadras de ${zona.nombre} $tiempo.")
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
