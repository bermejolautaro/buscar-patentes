package ar.lauta.buscarpatentes.ubicacion

import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.data.Cuadra
import ar.lauta.buscarpatentes.data.Zona
import ar.lauta.buscarpatentes.domain.Borde
import ar.lauta.buscarpatentes.domain.CuadraArmada
import ar.lauta.buscarpatentes.domain.Cuadras
import ar.lauta.buscarpatentes.domain.Polilinea
import ar.lauta.buscarpatentes.domain.Via
import ar.lauta.buscarpatentes.plataforma.hayConexion
import ar.lauta.buscarpatentes.plataforma.postear
import ar.lauta.buscarpatentes.ui.decimales
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * Las cuadras de una zona, pedidas una sola vez a OpenStreetMap (D1 y D8 de la 008).
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

    /**
     * Overpass, la consulta pública de OpenStreetMap: libre y sin clave.
     *
     * ponytail: si el servicio desaparece se cambia esta constante. Hay otras instancias con la
     * misma consulta.
     */
    private const val SERVICIO = "https://overpass-api.de/api/interpreter"

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

    /** Las cuadras armadas de la zona, o null si no hubo una respuesta que se entienda. */
    private suspend fun buscar(zona: Zona): List<CuadraArmada>? {
        val esquinas = Borde.leer(zona.borde)
        val texto = postear(SERVICIO, consulta(Borde.rectangulo(esquinas))) ?: return null
        val vias = leerVias(texto) ?: return null
        return Cuadras.armar(vias, esquinas)
    }

    /** Las principales primero, y cada gemela con el índice de su principal entre ellas. */
    private suspend fun guardar(zonaId: Long, cuadras: List<CuadraArmada>) {
        fun cuadra(c: CuadraArmada) = Cuadra(zonaId = zonaId, nombre = c.nombre, forma = Polilinea.codificar(c.forma))
        val indiceEntrePrincipales = HashMap<Int, Int>()
        val principales = mutableListOf<Cuadra>()
        cuadras.forEachIndexed { i, c ->
            if (c.gemelaDe == null) {
                indiceEntrePrincipales[i] = principales.size
                principales += cuadra(c)
            }
        }
        val gemelas = cuadras.filter { it.gemelaDe != null }
            .map { indiceEntrePrincipales.getValue(it.gemelaDe!!) to cuadra(it) }
        contenedor.zonas.activar(zonaId, principales, gemelas)
    }

    /**
     * La consulta del contrato Z1. Las coordenadas van con punto y seis decimales, sin depender
     * del idioma del teléfono.
     */
    internal fun consulta(r: Borde.Rectangulo): String {
        fun g(v: Double) = decimales(v, 6, '.')
        return "[out:json][timeout:25];" +
            "way[\"highway\"~\"^(trunk|primary|secondary|tertiary|unclassified|residential|living_street|pedestrian|road)$\"]" +
            "[\"area\"!=\"yes\"][\"access\"!~\"^(private|no)$\"][\"foot\"!=\"no\"]" +
            "(${g(r.sur)},${g(r.oeste)},${g(r.norte)},${g(r.este)});" +
            "out geom;"
    }

    /** Las vías de una respuesta de Overpass, o null si no se entiende (contrato Z1). */
    internal fun leerVias(texto: String): List<Via>? = try {
        Json.parseToJsonElement(texto).jsonObject.getValue("elements").jsonArray
            .map { it.jsonObject }
            .filter { it["type"]?.jsonPrimitive?.contentOrNull == "way" }
            .map { via ->
                val nodos = via.getValue("nodes").jsonArray.map { it.jsonPrimitive.long }
                val posiciones = via.getValue("geometry").jsonArray.map {
                    val p = it.jsonObject
                    p.getValue("lat").jsonPrimitive.double to p.getValue("lon").jsonPrimitive.double
                }
                require(nodos.size == posiciones.size)
                val etiquetas = via["tags"]?.jsonObject
                Via(
                    nodos = nodos,
                    posiciones = posiciones,
                    nombre = etiquetas?.get("name")?.jsonPrimitive?.contentOrNull,
                    unaMano = etiquetas?.get("oneway")?.jsonPrimitive?.contentOrNull == "yes",
                )
            }
    } catch (e: Exception) {
        null
    }
}
