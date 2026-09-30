package ar.lauta.buscarpatentes.mapa

import ar.lauta.buscarpatentes.data.ModoMapa
import ar.lauta.buscarpatentes.domain.Acomodo
import ar.lauta.buscarpatentes.domain.Dibujo
import ar.lauta.buscarpatentes.domain.CaminoGuardado
import ar.lauta.buscarpatentes.domain.Escalon
import ar.lauta.buscarpatentes.domain.Geo
import ar.lauta.buscarpatentes.domain.Pin
import ar.lauta.buscarpatentes.domain.Probabilidad
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position

/**
 * Una patente sobre el mapa (FR-034, FR-035).
 *
 * [toca] es true cuando su número coincide con el número actual del juego: esa es la que
 * el jugador puede mandar al grupo ahora mismo, y por eso se dibuja verde, más grande y
 * fuera de los grupos (FR-001 y FR-002 de la 005).
 */
data class Marcador(
    /** Para saber qué registro abrir cuando el jugador toca el marcador (FR-012). */
    val id: Long,

    /** Se dibuja adentro del marcador, con ceros a la izquierda (FR-010). */
    val numero: Int,

    val latitud: Double,
    val longitud: Double,

    /**
     * True cuando su número es el actual del juego.
     *
     * Desde la 005 decide **en qué fuente** va el marcador (D1): las que tocan van a una
     * fuente propia que no agrupa, con pin verde y más grande, dibujada encima de todo. Así
     * nunca quedan adentro de un grupo (FR-002). Ya no viaja en el feature: cada capa sabe
     * qué dibuja.
     */
    val toca: Boolean,

    /**
     * Qué tan probable es que la patente siga estando donde se la vio, de 0 a 10 (FR-037).
     *
     * Se dibuja en el borde del pin y no en el relleno: el relleno es blanco para que el número
     * se lea, y verde en la que toca.
     */
    val probabilidad: Int = Probabilidad.INICIAL,
)

/**
 * El camino de una salida (FR-006 de la 003, FR-007 de la 004).
 *
 * **No hay un recorrido destacado**: al planear, la salida de ayer no vale más que la de hace un
 * mes (FR-007 de la 004).
 *
 * [escalon] lo calcula quien arma el trazo, no esta capa: el mapa dibuja, no interpreta
 * fechas. Y los trazos llegan **ordenados de la salida más vieja a la más nueva**, que es lo
 * que hace que la reciente quede dibujada encima donde se superponen (FR-013, D5).
 */
data class Trazo(
    val recorridoId: Long,

    /** Cada tramo es una línea continua. Los decide [trazoDe]. */
    val tramos: List<List<Pair<Double, Double>>>,

    /** Hace cuánto se caminó. Solo se dibuja en [ModoMapa.ANTIGUEDAD]. */
    val escalon: Escalon,

    /**
     * Si entre un tramo y el siguiente va un hueco punteado (FR-009a de la 003). Sí para los
     * puntos medidos, donde entre dos tramos hubo un corte de señal. No para los pedazos de calle
     * del camino ajustado: el mapa por defecto pinta calles y nada más (D4 de la 007).
     */
    val huecos: Boolean = true,
)

/**
 * El trazo de una salida (D4 de la 007).
 *
 * - **Vista por defecto**: los pedazos de calle del camino ajustado, sin nada entre ellos. Si no
 *   se pudo ajustar, no pinta nada.
 * - **Vista de los puntos reales** ([puntosReales]): lo que midió el teléfono, cortado donde se
 *   cortó la señal, con los cortes punteados.
 * - **Todavía sin ajustar**: los puntos reales en las dos vistas. Una salida recién terminada sin
 *   conexión no puede verse como una salida que no existe (FR-033 de la 003).
 */
fun trazoDe(
    recorridoId: Long,
    medidos: List<Pair<Double, Double>>,
    guardado: CaminoGuardado,
    escalon: Escalon,
    puntosReales: Boolean,
): Trazo = when {
    !puntosReales && guardado is CaminoGuardado.Ajustado -> Trazo(recorridoId, guardado.tramos, escalon, huecos = false)
    !puntosReales && guardado is CaminoGuardado.NoSePudo -> Trazo(recorridoId, emptyList(), escalon, huecos = false)
    else -> Trazo(recorridoId, Geo.tramos(medidos), escalon)
}

internal typealias Coleccion = FeatureCollection<Geometry, JsonObject>

internal const val PROP_ID = "id"
internal const val PROP_NUMERO = "numero"
internal const val PROP_PROBABILIDAD = "probabilidad"
internal const val PROP_ESCALON = "escalon"

/** Cuántas patentes tiene un grupo. Solo los grupos la llevan. */
internal const val PROP_CUENTA = "cuenta"

/**
 * El ancho del pin en dp. Lo usa el dibujo del pin y también el acomodo, que separa los pines
 * de a un ancho (D13 de la 005).
 */
internal const val PIN_ANCHO_DP = 34f

private fun punto(latitud: Double, longitud: Double) = Point(Position(longitud, latitud))

/** GeoJSON va longitud primero. Invertirlo pondría las patentes en el mar. */
private fun linea(puntos: List<Pair<Double, Double>>) =
    LineString(puntos.map { (lat, lon) -> Position(lon, lat) })

/**
 * Un `LineString` por tramo de cada recorrido (D1, C2).
 *
 * **Uno por recorrido, nunca uno solo con todos los puntos.** Un `LineString` único cosería
 * el final de una salida con el principio de la siguiente: una recta que cruza la ciudad por
 * calles que el jugador no caminó nunca.
 *
 * Dentro de cada recorrido se corta donde hubo una desconexión: [tramosDe] parte los puntos
 * crudos con `Geo.tramos`, y cada tramo va como su propio `LineString`. El hueco entre dos
 * tramos lo dibuja [coleccionHuecos], punteado (FR-009, FR-009a). Un corte de señal ya
 * "teletransportó" una vez el recorrido hasta la casa del jugador.
 *
 * Un tramo de un solo punto se saltea: dos vértices es el mínimo para que haya línea.
 */
internal fun coleccionTrazos(trazos: List<Trazo>): Coleccion = FeatureCollection(
    trazos.flatMap { trazo ->
        trazo.tramos
            .filter { it.size >= 2 }
            .map { tramo ->
                Feature<Geometry, JsonObject>(
                    linea(tramo),
                    buildJsonObject { put(PROP_ESCALON, trazo.escalon.name) },
                )
            }
    },
)

/**
 * Los huecos entre tramos, para dibujarlos punteados (FR-009a).
 *
 * Un segmento de dos puntos por cada corte: del final de un tramo al principio del
 * siguiente. Punteado y no continuo porque eso es exactamente lo que se sabe — que hubo una
 * desconexión y que existe **algún** camino entre los dos extremos, no cuál.
 */
internal fun coleccionHuecos(trazos: List<Trazo>): Coleccion = FeatureCollection(
    trazos.flatMap { trazo ->
        (if (trazo.huecos) Geo.huecos(trazo.tramos) else emptyList()).map { (desde, hasta) ->
            Feature<Geometry, JsonObject>(
                linea(listOf(desde, hasta)),
                buildJsonObject { put(PROP_ESCALON, trazo.escalon.name) },
            )
        }
    },
)

internal fun coleccion(marcadores: List<Marcador>): Coleccion = FeatureCollection(
    marcadores.map { featureDe(it, it.latitud, it.longitud) },
)

/** Un marcador dibujado en [latitud], [longitud]: su lugar real, o el corrido por el acomodo. */
private fun featureDe(m: Marcador, latitud: Double, longitud: Double) =
    Feature<Geometry, JsonObject>(
        punto(latitud, longitud),
        buildJsonObject {
            put(PROP_ID, m.id)
            // Con ceros a la izquierda: el 7 se ve `007`. Mantiene el ancho constante y es
            // lo mismo que ya hacen la barra de arriba y la notificación de proximidad.
            put(PROP_NUMERO, m.numero.toString().padStart(3, '0'))
            put(PROP_PROBABILIDAD, m.probabilidad)
        },
    )

/**
 * Las patentes ya acomodadas para un zoom (D13 de la 005, US5).
 *
 * Un `Suelto` va como cualquier marcador, pero en su posición de dibujo: el id viaja igual, así
 * que tocarlo abre la ficha del registro real (FR-023b). Un `Grupo` es un punto con su cuenta
 * y nada más: no tiene número ni probabilidad propia, ni id que abrir.
 */
internal fun coleccionAcomodada(marcadores: List<Marcador>, dibujos: List<Dibujo>): Coleccion {
    val porId = marcadores.associateBy { it.id }
    return FeatureCollection(
        dibujos.map { d ->
            when (d) {
                is Dibujo.Suelto -> featureDe(porId.getValue(d.id), d.latitud, d.longitud)
                is Dibujo.Grupo -> Feature<Geometry, JsonObject>(
                    punto(d.latitud, d.longitud),
                    buildJsonObject { put(PROP_CUENTA, d.cuenta) },
                )
            }
        },
    )
}

/**
 * Las dos colecciones de patentes: (las que no tocan, las que tocan) (D1 de la 005).
 *
 * La primera pasa por el acomodo de [zoom]; la segunda no, y va a una fuente propia. Es la
 * frontera que garantiza el FR-002, y por eso se prueba en `ColeccionTest`: si una que toca se
 * colara en la primera, podría volver a quedar escondida adentro de un grupo.
 */
internal fun coleccionesPatentes(marcadores: List<Marcador>, zoom: Int): Pair<Coleccion, Coleccion> {
    val (tocan, resto) = marcadores.partition { it.toca }
    val pines = resto.map { Pin(it.id, it.latitud, it.longitud) }
    val dibujos = Acomodo.para(pines, zoom, PIN_ANCHO_DP.toDouble())
    return coleccionAcomodada(resto, dibujos) to coleccion(tocan)
}

// --- Zonas (008) ---

internal const val PROP_CLASE = "clase"
internal const val PROP_CONTINUO = "continuo"

/** Qué dice cada cuadra de una zona en el mapa (contrato Z4). */
enum class ClaseDeCuadra { RECORRIDA, PENDIENTE, QUITADA }

data class CuadraDibujada(val id: Long, val forma: List<Pair<Double, Double>>, val clase: ClaseDeCuadra)

/**
 * Una zona sobre el mapa (contrato Z4). Quien la arma ya decidió qué cuadras van: en la pantalla
 * principal, solo las pendientes; en el mapa de la zona, las tres clases.
 */
data class DibujoDeZona(
    val borde: List<Pair<Double, Double>>,
    val cuadras: List<CuadraDibujada>,
    /** Continuo en el mapa de la zona, punteado en la pantalla principal. */
    val bordeContinuo: Boolean,
    /** Un punto en cada esquina, mientras se dibuja: con una sola esquina todavía no hay línea. */
    val conEsquinas: Boolean = false,
)

/** Un `LineString` por zona, cerrado contra la primera esquina. */
internal fun coleccionBordes(zonas: List<DibujoDeZona>): Coleccion = FeatureCollection(
    zonas.filter { it.borde.size >= 2 }.map { zona ->
        val cerrado = if (zona.borde.size >= 3) zona.borde + zona.borde.first() else zona.borde
        Feature<Geometry, JsonObject>(linea(cerrado), buildJsonObject { put(PROP_CONTINUO, zona.bordeContinuo) })
    },
)

/** Un `LineString` por cuadra, con su id para tocarla y su clase para pintarla. */
internal fun coleccionCuadras(zonas: List<DibujoDeZona>): Coleccion = FeatureCollection(
    zonas.flatMap { it.cuadras }.filter { it.forma.size >= 2 }.map { cuadra ->
        Feature<Geometry, JsonObject>(
            linea(cuadra.forma),
            buildJsonObject {
                put(PROP_ID, cuadra.id)
                put(PROP_CLASE, cuadra.clase.name)
            },
        )
    },
)

/** Las esquinas de las zonas que se están dibujando. */
internal fun coleccionEsquinas(zonas: List<DibujoDeZona>): Coleccion = FeatureCollection(
    zonas.filter { it.conEsquinas }.flatMap { zona ->
        zona.borde.map { (lat, lon) -> Feature<Geometry, JsonObject>(punto(lat, lon), JsonObject(emptyMap())) }
    },
)
