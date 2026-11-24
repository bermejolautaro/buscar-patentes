package ar.lauta.buscarpatentes.ubicacion

import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.data.Cuadra
import ar.lauta.buscarpatentes.data.Zona
import ar.lauta.buscarpatentes.domain.Borde
import ar.lauta.buscarpatentes.domain.CuadraArmada
import ar.lauta.buscarpatentes.domain.Cuadras
import ar.lauta.buscarpatentes.domain.Polilinea
import ar.lauta.buscarpatentes.domain.Tesela
import ar.lauta.buscarpatentes.domain.Teselas
import ar.lauta.buscarpatentes.mapa.ConfigMapa
import ar.lauta.buscarpatentes.plataforma.bajar
import ar.lauta.buscarpatentes.plataforma.hayConexion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Las cuadras de una zona, armadas una sola vez con las teselas del mapa de fondo (D8 de la 008,
 * D1 de la 012).
 *
 * Hasta la 012 salían de Overpass, la consulta pública de OpenStreetMap. En noviembre de 2026
 * tardaba más de 20 s o daba error, y una zona quedó meses buscando sus calles. Las teselas son las
 * mismas que dibuja el mapa, de OpenFreeMap, y contestan en una fracción de segundo.
 *
 * ## Por qué es aceptable que use red
 *
 * Por lo mismo que [AjustarACalles]: no está en el camino de captura. Y acá la red se usa una vez
 * por zona. Con las cuadras guardadas, el porcentaje se calcula sin conexión.
 *
 * ## La cola
 *
 * Igual que el ajuste, no hay cola aparte: la cola **es** la consulta `buscando()`. Una zona sin
 * cuadras y sin problema está pendiente, y se intenta cada vez que abre la pantalla principal o la
 * de zonas con conexión.
 */
object BuscarCuadras {

    /** Más que esto no se arma: acota la cuenta en el teléfono (FR-004). */
    const val MAXIMO = 2_000

    const val SIN_CALLES = "Adentro no hay calles"
    const val DEMASIADAS = "Tiene más de 2.000 cuadras"

    /**
     * Busca las cuadras de todas las zonas pendientes. Devuelve cuántas cambiaron: las que quedaron
     * activas y las que quedaron con un problema.
     *
     * No tira nunca: si algo falla, la zona sigue pendiente y se reintenta la próxima vez.
     */
    suspend fun pendientes(): Int {
        if (!hayConexion()) return 0
        var cambiaron = 0
        for (zona in contenedor.zonas.buscando()) {
            val cuadras = try {
                buscar(zona)
            } catch (e: Exception) {
                null
            } ?: continue
            // Mientras se buscaba, la zona se pudo borrar o su borde se pudo editar (FR-022): lo
            // encontrado ya no es de ella.
            if (contenedor.zonas.porId(zona.id)?.borde != zona.borde) continue
            val principales = cuadras.count { it.gemelaDe == null }
            when {
                principales == 0 -> contenedor.zonas.marcarProblema(zona.id, SIN_CALLES)
                principales > MAXIMO -> contenedor.zonas.marcarProblema(zona.id, DEMASIADAS)
                else -> guardar(zona.id, cuadras)
            }
            cambiaron++
        }
        return cambiaron
    }

    /**
     * Las cuadras armadas de la zona, o null si falta alguna tesela. Las teselas son las que toca el
     * rectángulo del borde con su margen de 200 m, como el que se le pedía a Overpass.
     */
    private suspend fun buscar(zona: Zona): List<CuadraArmada>? {
        val esquinas = Borde.leer(zona.borde)
        val plantilla = bajar(ConfigMapa.TESELAS)?.let { plantilla(it.decodeToString()) } ?: return null
        val teselas = Teselas.cubren(Borde.rectangulo(esquinas)).map { (x, y) ->
            val url = plantilla.replace("{z}", "${Teselas.ZOOM}").replace("{x}", "$x").replace("{y}", "$y")
            Tesela(Teselas.ZOOM, x, y, bajar(url) ?: return null)
        }
        return Cuadras.armar(Teselas.vias(teselas), esquinas)
    }

    /**
     * Las principales primero, y cada gemela con el índice de su principal entre ellas.
     *
     * Si la zona ya tenía cuadras es que se editó su borde: las que estaban quitadas siguen
     * quitadas en las nuevas (FR-022). La gemela va con su principal, como al quitar.
     */
    private suspend fun guardar(zonaId: Long, cuadras: List<CuadraArmada>) {
        val quitadas = contenedor.zonas.cuadrasDe(zonaId).filter { it.quitada }.map { Polilinea.decodificar(it.forma) }
        fun cuadra(c: CuadraArmada, quitada: Boolean) =
            Cuadra(zonaId = zonaId, nombre = c.nombre, forma = Polilinea.codificar(c.forma), quitada = quitada)
        val indiceEntrePrincipales = HashMap<Int, Int>()
        val principales = mutableListOf<Cuadra>()
        cuadras.forEachIndexed { i, c ->
            if (c.gemelaDe == null) {
                indiceEntrePrincipales[i] = principales.size
                principales += cuadra(c, quitadas.any { Cuadras.misma(it, c.forma) })
            }
        }
        val gemelas = cuadras.filter { it.gemelaDe != null }.map {
            val indice = indiceEntrePrincipales.getValue(it.gemelaDe!!)
            indice to cuadra(it, principales[indice].quitada)
        }
        contenedor.zonas.activar(zonaId, principales, gemelas)
    }

    /** La plantilla de las teselas de la semana, del TileJSON de OpenFreeMap, o null (D2 de la 012). */
    internal fun plantilla(tileJson: String): String? = try {
        Json.parseToJsonElement(tileJson).jsonObject.getValue("tiles").jsonArray.first().jsonPrimitive.content
    } catch (e: Exception) {
        null
    }
}
