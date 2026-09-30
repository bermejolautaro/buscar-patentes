package ar.lauta.buscarpatentes.ubicacion

import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.data.PuntoDeTrayecto
import ar.lauta.buscarpatentes.domain.CaminoAjustado
import ar.lauta.buscarpatentes.domain.CaminoGuardado
import ar.lauta.buscarpatentes.domain.Geo
import ar.lauta.buscarpatentes.domain.Polilinea
import ar.lauta.buscarpatentes.plataforma.hayConexion
import ar.lauta.buscarpatentes.plataforma.postear
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Ajusta el camino de una salida a las calles por las que se pudo haber ido (FR-031, FR-032).
 *
 * ## Qué problema resuelve
 *
 * El trazo crudo une las posiciones que el GPS entregó, y esas posiciones caen sobre techos,
 * patios y a mitad de manzana. Peor: cuando la señal se corta, la línea entre la última
 * posición y la primera de la vuelta cruza en recta lo que haya en el medio — en la salida
 * del 29/08 eso hizo que el recorrido "se teletransportara" hasta la casa del jugador.
 *
 * El map matching pega esas posiciones al grafo de calles y rellena los huecos con el camino
 * peatonal más razonable entre los dos extremos. El resultado es un recorrido que se puede
 * seguir con el dedo sobre las calles reales.
 *
 * ## Por qué es aceptable que use red, si el Principio III existe
 *
 * Porque **no está en el camino de captura**. El Principio III prohíbe que la captura
 * dependa de la red, y permite explícitamente que las funciones accesorias la usen. Guardar
 * una patente sigue funcionando en un sótano; lo único que espera a tener conexión es el
 * dibujo bonito de un recorrido ya terminado.
 *
 * ## El standby
 *
 * No hay cola, ni reintentos programados, ni `WorkManager`. La cola **es** la consulta
 * `sinAjustar()`: un recorrido terminado con `caminoAjustado` en null es un recorrido
 * pendiente. Cada vez que la app abre con conexión se intentan todos los pendientes. Si el
 * jugador termina la salida en la calle sin datos, queda pendiente hasta que llegue a su
 * casa y abra la app con wifi, que es exactamente el caso que hay que cubrir.
 *
 * Desde la 007, un camino guardado antes de la 007 también está en la cola: `sinAjustar()` trae
 * todo lo que no tiene el formato nuevo (`CaminoGuardado.PREFIJO`), así que se reajusta solo.
 *
 * Un fallo permanente —un recorrido que el servicio nunca puede resolver— se reintenta en
 * cada apertura. Es barato y se cura solo si el servicio vuelve.
 *
 * ## Por tramos (D2 de la 007)
 *
 * Un pedido por **tramo de señal**, cortado igual que el trazo crudo (`Geo.tramos`). Con un
 * solo pedido, el servicio rellenaba un corte de señal con calles que nadie caminó, y pegaba en
 * una recta los pedazos de un camino que no pudo unir: esas eran las diagonales. Cada respuesta
 * se arma con `CaminoAjustado.armar`, que une los pedazos por los puntos medidos.
 *
 * ## Ninguna dependencia de más
 *
 * El POST es el `postear` de cada sistema, y el JSON sale de `kotlinx-serialization-json`, solo
 * la biblioteca, sin plugin (D16 de la 006): `org.json` no existe en el iPhone. Sumar Retrofit o
 * Ktor para una sola llamada a un solo endpoint es lo que el Principio IV manda no hacer.
 */
object AjustarACalles {

    /**
     * Valhalla de FOSSGIS: libre, sin clave, con costeo peatonal.
     *
     * **`trace_attributes` y no `trace_route`**, y la diferencia no es de estilo. `trace_route`
     * arma una ruta con maniobras entre los extremos del trazo, y una caminata de búsqueda se
     * pisa a sí misma —se pasa dos veces por la misma cuadra— así que la ruta que arma se queda
     * en el primer tramo: la salida del 31/08, de 6,7 km, volvía como 224 m. `trace_attributes`
     * no arma ruta, devuelve la geometría que matcheó, que es exactamente lo que se dibuja: los
     * mismos puntos dieron 6489 m con confianza 1.0.
     *
     * ponytail: si el servicio desaparece se cambia esta constante. Nada más depende de él —
     * cuando no responde, el trazo crudo sigue dibujándose igual.
     */
    private const val SERVICIO = "https://valhalla1.openstreetmap.de/trace_attributes"

    /** Sin dos posiciones no hay camino que ajustar. */
    private const val MINIMO_PUNTOS = 2

    /**
     * El radio de búsqueda que se le pasa a cada punto, tomado de la precisión que el
     * teléfono reportó. Mínimo 5 m porque un radio de cero no encuentra ninguna calle;
     * máximo 30 m porque más que eso alcanza a la manzana de al lado y el servicio empieza a
     * elegir entre calles que no son.
     */
    private const val RADIO_MINIMO_M = 5f
    private const val RADIO_MAXIMO_M = 30f

    /**
     * Intenta ajustar todas las salidas pendientes. Devuelve cuántas se ajustaron.
     *
     * No tira nunca: si algo falla, el recorrido se queda pendiente y se reintenta la próxima
     * vez. Un mapa con el trazo crudo es mucho mejor que una pantalla que se cae.
     */
    suspend fun pendientes(): Int {
        // Una wifi de hotel que todavía pide login está conectada y no llega a internet:
        // preguntar antes evita gastar un tiempo de espera de 20 segundos para descubrirlo.
        if (!hayConexion()) return 0

        var ajustadas = 0
        for (recorrido in contenedor.recorridos.sinAjustar()) {
            val puntos = contenedor.puntos.deRecorrido(recorrido.id)
            if (puntos.size < MINIMO_PUNTOS) continue

            val camino = ajustar(puntos) ?: continue
            contenedor.recorridos.guardarCaminoAjustado(recorrido.id, camino)
            ajustadas++
        }
        return ajustadas
    }

    /**
     * El camino a guardar (contrato A1 de la 007), o null si hay que volver a intentar.
     *
     * - **Sin red** en cualquier pedido: null, y la salida queda como estaba.
     * - **El servicio rechaza un tramo** (no contesta 200, o contesta algo que no se entiende):
     *   ese tramo va con sus puntos medidos. Un tramo de dos puntos en una plaza no puede dejar
     *   sin ajustar la salida entera.
     * - **Rechaza todos**: null. Es más probable que el servicio esté caído que no haya calles.
     * - **Contesta y nada emparejó**: `2:` solo, que no se vuelve a pedir.
     */
    private suspend fun ajustar(puntos: List<PuntoDeTrayecto>): String? {
        val tramos = mutableListOf<List<Pair<Double, Double>>>()
        var respondio = false
        var emparejo = false
        for (tramo in porTramoDeSenal(puntos)) {
            if (tramo.size < MINIMO_PUNTOS) continue
            val medidos = tramo.map { it.latitud to it.longitud }
            // POST y no GET con el JSON en la query. Una salida de 647 puntos son 34 KB de
            // cuerpo, y metidos en la URL codificada dan 52 KB: el servidor cierra la conexión
            // sin contestar y el recorrido queda pendiente para siempre.
            val texto = try {
                postear(SERVICIO, cuerpo(tramo).toString())
            } catch (e: Exception) {
                return null
            }
            val respuesta = texto?.let(::leerRespuesta)
            if (respuesta == null) {
                tramos += medidos
                continue
            }
            respondio = true
            emparejo = emparejo || respuesta.emparejados.any { it != null }
            tramos += CaminoAjustado.armar(medidos, respuesta.forma, respuesta.aristas, respuesta.emparejados)
        }
        if (!respondio) return null
        return CaminoGuardado.escribir(if (emparejo) tramos else emptyList())
    }

    /** Los puntos cortados donde se cortó la señal, con el mismo umbral que el trazo crudo. */
    private fun porTramoDeSenal(puntos: List<PuntoDeTrayecto>): List<List<PuntoDeTrayecto>> {
        var desde = 0
        return Geo.tramos(puntos.map { it.latitud to it.longitud }).map { tramo ->
            puntos.subList(desde, desde + tramo.size).also { desde += tramo.size }
        }
    }

    /** Lo que se usa de una respuesta de `trace_attributes` (contrato A3 de la 007). */
    internal class Respuesta(
        val forma: List<Pair<Double, Double>>,
        val aristas: List<IntRange>,
        /** Por cada punto pedido, la arista donde emparejó, o null. `interpolated` cuenta. */
        val emparejados: List<Int?>,
    )

    /** Null si falta algo o no cierra: un índice fuera de la forma, o una arista que no existe. */
    internal fun leerRespuesta(texto: String): Respuesta? = try {
        val json = Json.parseToJsonElement(texto).jsonObject
        val forma = Polilinea.decodificar(json.getValue("shape").jsonPrimitive.content)
        val aristas = json.getValue("edges").jsonArray.map {
            val arista = it.jsonObject
            arista.getValue("begin_shape_index").jsonPrimitive.int..arista.getValue("end_shape_index").jsonPrimitive.int
        }
        val emparejados = json.getValue("matched_points").jsonArray.map {
            val punto = it.jsonObject
            if (punto["type"]?.jsonPrimitive?.contentOrNull == "unmatched") null
            else punto.getValue("edge_index").jsonPrimitive.int
        }
        require(aristas.all { it.first in forma.indices && it.last in forma.indices })
        require(emparejados.all { it == null || it in aristas.indices })
        Respuesta(forma, aristas, emparejados)
    } catch (e: Exception) {
        null
    }

    internal fun cuerpo(puntos: List<PuntoDeTrayecto>) = buildJsonObject {
        putJsonArray("shape") {
            puntos.forEach {
                addJsonObject {
                    put("lat", it.latitud)
                    put("lon", it.longitud)
                    // Cuánto se le cree a este punto. Sin esto el servicio busca calle con
                    // su radio por defecto y pega con la misma confianza un punto de 5 m que
                    // uno de 25, que es como termina eligiendo la paralela.
                    put("radius", it.precisionMetros.coerceIn(RADIO_MINIMO_M, RADIO_MAXIMO_M))
                }
            }
        }
        // Peatonal, que es como se juega. Con costeo de auto el camino se iría por avenidas
        // y respetaría manos únicas que caminando no existen.
        put("costing", "pedestrian")
        // `map_snap` pega al grafo y rellena los huecos; `edge_walk` exigiría que las
        // posiciones ya cayeran sobre calles, que es justo lo que no pasa.
        put("shape_match", "map_snap")
    }
}
