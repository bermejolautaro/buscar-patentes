package ar.lauta.buscarpatentes.ubicacion

import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.data.PuntoDeTrayecto
import ar.lauta.buscarpatentes.plataforma.hayConexion
import ar.lauta.buscarpatentes.plataforma.postear
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
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
 * Un fallo permanente —un recorrido que el servicio nunca puede resolver— se reintenta en
 * cada apertura. Es barato y se cura solo si el servicio vuelve.
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

            val polilinea = pedirCamino(puntos) ?: continue
            contenedor.recorridos.guardarCaminoAjustado(recorrido.id, polilinea)
            ajustadas++
        }
        return ajustadas
    }

    /** La polilínea del camino ajustado, o null si el servicio no contestó algo utilizable. */
    private suspend fun pedirCamino(puntos: List<PuntoDeTrayecto>): String? = try {
        // POST y no GET con el JSON en la query. Una salida de 647 puntos son 34 KB de
        // cuerpo, y metidos en la URL codificada dan 52 KB: el servidor cierra la conexión
        // sin contestar y el recorrido queda pendiente para siempre, porque no adelgaza
        // solo. Con nueve puntos entraba, que es por qué las primeras salidas sí se
        // ajustaron y la primera caminata larga no.
        //
        // Una sola cadena: `trace_attributes` devuelve la geometría matcheada entera y no
        // hay piernas que pegar. `Polilinea` sigue sabiendo separarlas por los dos caminos
        // que ya están guardados.
        postear(SERVICIO, cuerpo(puntos).toString())
            ?.let { Json.parseToJsonElement(it).jsonObject["shape"]?.jsonPrimitive?.contentOrNull }
            ?.takeIf { it.isNotEmpty() }
    } catch (e: Exception) {
        // Sin red, con el servicio caído o con una respuesta inesperada, el recorrido se
        // queda pendiente. Es el mismo estado que tenía antes de intentar.
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
