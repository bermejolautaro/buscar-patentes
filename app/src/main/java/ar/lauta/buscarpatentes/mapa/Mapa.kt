package ar.lauta.buscarpatentes.mapa

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import java.io.File
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdate
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.OnLocationCameraTransitionListener
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.android.offline.OfflineManager
import ar.lauta.buscarpatentes.data.ModoMapa
import ar.lauta.buscarpatentes.domain.Acomodo
import ar.lauta.buscarpatentes.domain.Dibujo
import ar.lauta.buscarpatentes.domain.Escalon
import ar.lauta.buscarpatentes.domain.Geo
import ar.lauta.buscarpatentes.domain.Pin
import ar.lauta.buscarpatentes.domain.Probabilidad
import ar.lauta.buscarpatentes.ubicacion.Permisos
import ar.lauta.buscarpatentes.ui.ColoresDeMapa

/**
 * Configuración del mapa (C5).
 *
 * La URL del estilo vive **acá y en ningún otro lado**. OpenFreeMap es un servicio
 * gratuito operado por un tercero (riesgo declarado en D2): si desaparece, se cambia esta
 * constante y nada más. La captura no depende de esto — FR-038 garantiza que el estado
 * del mapa nunca la bloquea.
 */
object ConfigMapa {
    /**
     * El fondo del mapa, elegido por el modo del sistema (FR-027, FR-028).
     *
     * **`positron` se probó y se descartó**: es el estilo más sobrio de OpenFreeMap, pero
     * dibuja las calles menores en un gris tan claro que en la calle solo se distinguían las
     * avenidas. Sobrio no sirve si se come la información: el jugador camina por calles, no
     * por avenidas, y necesita verlas para ubicar dónde está parado.
     *
     * `liberty` es el que estaba y el que las dibuja bien. `fiord` es su equivalente oscuro:
     * su capa `highway_minor` arranca en zoom 8 y las pinta en gris azulado sobre fondo azul
     * oscuro, con contraste suficiente.
     *
     * **Los tres publican Noto Sans**, así que el `textFont` obligatorio de la capa de
     * números —el que costó el bug de los marcadores invisibles— sigue resolviéndose.
     */
    fun estiloUrl(oscuro: Boolean): String =
        if (oscuro) "https://tiles.openfreemap.org/styles/fiord"
        else "https://tiles.openfreemap.org/styles/liberty"

    /**
     * Techo del caché ambiente de tiles (FR-044).
     *
     * MapLibre guarda solo lo que se muestra y desaloja por uso cuando llega al límite,
     * que es exactamente FR-042 y FR-044 sin código propio.
     * ponytail: 200 MB entra cómodo en un teléfono y cubre un barrio entero con holgura.
     * Calibrable si en uso real queda corto o molesta.
     */
    const val CACHE_MAXIMO_BYTES = 200L * 1024 * 1024
}

/** Estado del caché de mapa, para la pantalla de ajustes (FR-045). */
class CacheDeMapa(private val context: Context) {

    private companion object {
        const val ARCHIVO_CACHE = "mbgl-offline.db"
    }

    fun configurarTecho() {
        OfflineManager.getInstance(context)
            .setMaximumAmbientCacheSize(ConfigMapa.CACHE_MAXIMO_BYTES, null)
    }

    /**
     * Bytes que ocupa el caché de tiles en disco (FR-045).
     *
     * MapLibre no expone el tamaño por API, así que se mide el archivo que usa. Si cambia
     * de nombre en una versión futura esto devuelve 0, no rompe.
     */
    fun espacioOcupado(): Long {
        val archivo = File(context.filesDir, ARCHIVO_CACHE)
        return if (archivo.exists()) archivo.length() else 0L
    }

    fun borrar(alTerminar: () -> Unit = {}) {
        OfflineManager.getInstance(context).clearAmbientCache(
            object : OfflineManager.FileSourceCallback {
                override fun onSuccess() = alTerminar()
                override fun onError(message: String) = alTerminar()
            },
        )
    }
}

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
     * Se dibuja en el anillo y no en el relleno: el relleno dice en qué estado está la
     * patente —FR-020 pide que los tres se sigan distinguiendo— y el número blanco de
     * adentro necesita un fondo oscuro para leerse. El anillo estaba blanco y fijo, o sea
     * que no decía nada, y es donde entra este dato sin sacar ninguno.
     */
    val probabilidad: Int = Probabilidad.INICIAL,
)

/**
 * El camino de una salida (FR-006 de la 003, FR-007 de la 004).
 *
 * **Ya no hay un recorrido destacado.** Hasta la 003 este tipo llevaba un `destacado` que era
 * el recorrido en curso, o el más reciente: se dibujaba fuerte y el resto apagado. Eso
 * contesta "¿cuál fue la última salida?", que es una pregunta que el jugador se hace una sola
 * vez, mientras camina, cuando ya sabe la respuesta. La 004 lo revierte (FR-007): al planear,
 * la salida de ayer no vale más que la de hace un mes.
 *
 * [escalon] lo calcula quien arma el trazo, no esta capa: el mapa dibuja, no interpreta
 * fechas. Y los trazos llegan **ordenados de la salida más vieja a la más nueva**, que es lo
 * que hace que la reciente quede dibujada encima donde se superponen (FR-013, D5).
 */
data class Trazo(
    val recorridoId: Long,
    /** En orden. Cada par es latitud y longitud. */
    val puntos: List<Pair<Double, Double>>,

    /** Hace cuánto se caminó. Solo se dibuja en [ModoMapa.ANTIGUEDAD]. */
    val escalon: Escalon,

    /**
     * True cuando estos puntos son el camino ajustado a las calles (FR-031).
     *
     * Cambia cómo se dibuja: un camino ajustado ya viene continuo y pegado al grafo de
     * calles, así que **no se corta ni se puntea**. Cortar tenía sentido sobre el trazo
     * crudo, donde un salto significaba "no sé qué pasó en el medio"; acá el servicio ya
     * contestó qué pasó en el medio.
     */
    val ajustado: Boolean = false,
)

private const val FUENTE_TRAZOS = "recorridos"
private const val CAPA_TRAZOS = "recorridos-linea"
private const val FUENTE_HUECOS = "recorridos-huecos"
private const val CAPA_HUECOS = "recorridos-huecos-linea"
private const val PROP_ESCALON = "escalon"

/**
 * Un solo ancho y una sola opacidad para todo el histórico (FR-007).
 *
 * Eran dos de cada uno hasta la 003, para separar el destacado del resto. Ahora todas las
 * salidas pesan lo mismo: lo que tiene que leerse es el hueco, no cuál fue la última.
 */
private const val ANCHO_TRAZO = 5f
private const val OPACIDAD_TRAZO = 0.9f

/**
 * Un `LineString` por recorrido (D1, C2).
 *
 * **Uno por recorrido, nunca uno solo con todos los puntos.** Un `LineString` único cosería
 * el final de una salida con el principio de la siguiente: una recta que cruza la ciudad por
 * calles que el jugador no caminó nunca.
 *
 * Dentro de cada recorrido, en cambio, se corta donde hubo una desconexión: [tramosDe] parte
 * los puntos crudos con `Geo.tramos`, y cada tramo va como su propio `LineString`. El hueco
 * entre dos tramos lo dibuja [coleccionHuecos], punteado (FR-009, FR-009a).
 *
 * **Esto era al revés hasta la segunda salida**, y el comentario que estaba acá lo defendía:
 * se unía todo sin cortar para no tener que elegir un umbral, aceptando la recta sobre un
 * tramo sin señal como un error cosmético. En la calle no fue cosmético — un corte de señal
 * "teletransportó" el recorrido hasta la casa del jugador, afirmando un camino falso.
 *
 * Un tramo de un solo punto se saltea: dos vértices es el mínimo para que haya línea.
 */
internal fun coleccionTrazos(trazos: List<Trazo>) = FeatureCollection.fromFeatures(
    trazos.flatMap { trazo ->
        tramosDe(trazo)
            .filter { it.size >= 2 }
            .map { tramo ->
                Feature.fromGeometry(
                    LineString.fromLngLats(tramo.map { (lat, lon) -> Point.fromLngLat(lon, lat) }),
                ).apply { addStringProperty(PROP_ESCALON, trazo.escalon.name) }
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
internal fun coleccionHuecos(trazos: List<Trazo>) = FeatureCollection.fromFeatures(
    trazos.flatMap { trazo ->
        Geo.huecos(tramosDe(trazo)).map { (desde, hasta) ->
            Feature.fromGeometry(
                LineString.fromLngLats(
                    listOf(
                        Point.fromLngLat(desde.second, desde.first),
                        Point.fromLngLat(hasta.second, hasta.first),
                    ),
                ),
            ).apply { addStringProperty(PROP_ESCALON, trazo.escalon.name) }
        }
    },
)

/**
 * La capa del camino recorrido (FR-006 de la 003, FR-007 de la 004).
 *
 * **Una sola capa para los tres modos.** El modo no viaja en cada feature: es un valor global,
 * y meterlo adentro de la colección obligaría a reconstruirla entera en cada toque del
 * interruptor. Acá se crean las capas una vez, y [aplicarModo] les cambia la pintura (D4).
 *
 * Se agrega **antes** que los círculos de las patentes, y eso no es un detalle: MapLibre
 * dibuja en orden de inserción, y las patentes son el dato mientras el recorrido es el
 * contexto. Un trazo encima de un marcador taparía justo lo que se fue a buscar (C2).
 */
private fun pintarTrazos(style: Style) {
    if (style.getLayer(CAPA_TRAZOS) != null) return
    style.addLayer(
        LineLayer(CAPA_TRAZOS, FUENTE_TRAZOS).withProperties(
            PropertyFactory.lineColor(ColoresDeMapa.RECORRIDO),
            PropertyFactory.lineWidth(ANCHO_TRAZO),
            PropertyFactory.lineOpacity(OPACIDAD_TRAZO),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )

    // FR-009a: el hueco de una desconexión, punteado.
    //
    // El punteado **es** el mensaje. Una línea continua ahí afirmaría un camino que la app
    // no midió, y eso ya pasó: en la salida del 29/08 un corte de señal unió dos puntos en
    // recta y el recorrido "se teletransportó" hasta la casa del jugador. Punteado dice lo
    // único que se sabe: acá falta información, y hay algún camino en el medio.
    style.addLayer(
        LineLayer(CAPA_HUECOS, FUENTE_HUECOS).withProperties(
            PropertyFactory.lineColor(ColoresDeMapa.RECORRIDO),
            PropertyFactory.lineWidth(2f),
            PropertyFactory.lineOpacity(0.6f),
            PropertyFactory.lineDasharray(arrayOf(2f, 3f)),
        ),
    )
}

/**
 * Le cambia la pintura a las capas del camino según el modo elegido (FR-002, D4).
 *
 * Tres `setProperties` sobre capas que ya existen: la fuente ni se entera, así que cambiar de
 * modo es instantáneo y volver a prender no recalcula nada.
 *
 * Apagar es `visibility = NONE` y no una colección vacía: alimentar la fuente con nada tiraría
 * el trabajo de armar la colección para tener que rehacerlo al prender.
 */
/**
 * El color de cada escalón de antigüedad (FR-012, FR-015).
 *
 * `match` sobre el nombre del escalón y no `step` sobre su posición: el ordinal de un enum es
 * un acoplamiento que nadie ve hasta que alguien reordena las constantes y el mapa empieza a
 * mentir en silencio.
 *
 * Tres colores netos y **nunca un degradado**: la pregunta es "¿hace mucho o hace poco?", y
 * una rampa de tonos no la contesta mejor. El más viejo es el que más resalta, al revés de la
 * 003, y a propósito: lo viejo es lo que hay que ir a revisar.
 */
private fun colorPorEscalon(): Expression = Expression.match(
    Expression.get(PROP_ESCALON),
    Expression.literal(ColoresDeMapa.ANTIGUEDAD_VIEJA),
    Expression.stop(Escalon.RECIENTE.name, ColoresDeMapa.ANTIGUEDAD_RECIENTE),
    Expression.stop(Escalon.MEDIO.name, ColoresDeMapa.ANTIGUEDAD_MEDIA),
    Expression.stop(Escalon.VIEJO.name, ColoresDeMapa.ANTIGUEDAD_VIEJA),
)

private fun aplicarModo(style: Style, modo: ModoMapa) {
    val visible = if (modo == ModoMapa.APAGADO) Property.NONE else Property.VISIBLE
    val color = when (modo) {
        // FR-007: todas las salidas con el mismo color. Lo que tiene que leerse es el hueco.
        ModoMapa.COBERTURA, ModoMapa.APAGADO -> Expression.literal(ColoresDeMapa.RECORRIDO)
        ModoMapa.ANTIGUEDAD -> colorPorEscalon()
    }

    style.getLayer(CAPA_TRAZOS)?.setProperties(
        PropertyFactory.visibility(visible),
        PropertyFactory.lineColor(color),
    )

    // El hueco no cambia de color con el modo: no es una calle caminada, y no tiene una
    // antigüedad que decir. Solo se apaga y se prende con el resto.
    style.getLayer(CAPA_HUECOS)?.setProperties(PropertyFactory.visibility(visible))
}

/**
 * Los tramos de un trazo: uno solo si viene ajustado, cortados por salto si viene crudo.
 *
 * Un camino ajustado no se corta porque no tiene nada que ocultar: el servicio ya resolvió
 * por dónde se fue entre dos posiciones lejanas, siguiendo calles. Cortarlo ahí dibujaría un
 * hueco punteado sobre un tramo que sí se conoce.
 */
private fun tramosDe(trazo: Trazo): List<List<Pair<Double, Double>>> =
    if (trazo.ajustado) listOf(trazo.puntos) else Geo.tramos(trazo.puntos)

private const val FUENTE = "patentes"
private const val CAPA = "patentes-pines"
private const val CAPA_GRUPO = "patentes-grupos"
private const val CAPA_GRUPO_CUENTA = "patentes-grupos-cuenta"

/**
 * La que toca, en su propia fuente, fuera del acomodo (D1 de la 005, FR-002).
 *
 * Lo que no pasa por [Acomodo] no puede quedar en un grupo, y la cuenta del grupo tampoco la
 * incluye (FR-002a). La capa se agrega después de las de grupo, y en MapLibre el orden de
 * inserción es el orden de dibujo: queda encima de todo.
 */
private const val FUENTE_TOCA = "patentes-toca"
private const val CAPA_TOCA = "patentes-toca-pines"

private const val PROP_ID = "id"
private const val PROP_NUMERO = "numero"

/**
 * Las tres imágenes de pin, una por escalón de probabilidad (FR-028, FR-030, D1).
 *
 * Tres y no una teñida: un SDF admite **un** color por feature, y el pin necesita dos —relleno
 * blanco y borde de color—, así que teñir costaría dos capas apiladas y un borde blando. Tres
 * bitmaps de unos pocos KB, dibujados una vez por carga de estilo, dejan el borde bajo control
 * total: grosor, color y forma se deciden en el `Canvas`.
 */
private const val PIN_BAJA = "pin-confianza-baja"
private const val PIN_MEDIA = "pin-confianza-media"
private const val PIN_ALTA = "pin-confianza-alta"

/** Las mismas tres, con el relleno verde de la que toca (FR-001, FR-001a de la 005). */
private const val PIN_TOCA_BAJA = "pin-toca-confianza-baja"
private const val PIN_TOCA_MEDIA = "pin-toca-confianza-media"
private const val PIN_TOCA_ALTA = "pin-toca-confianza-alta"

/** Cuántas patentes tiene un grupo. Solo los grupos la llevan. */
private const val PROP_CUENTA = "cuenta"
private const val PROP_PROBABILIDAD = "probabilidad"

/*
 * Acá vivía `opcionesDeAgrupacion`, la agrupación de MapLibre por fuente.
 *
 * **Se fue en la 005 (US5, D13)**: MapLibre agrupa en cuanto dos pines se tocan, y el jugador
 * pidió que antes se hagan lugar, mientras sigan sobre la misma cuadra. Eso no lo trae; ahora
 * los grupos los arma [Acomodo] y la fuente recibe los pines ya acomodados.
 */

/** Un grupo trae `cuenta`; un registro suelto, no. Es el filtro de las capas de pines y grupos. */
private fun esGrupo() = Expression.has(PROP_CUENTA)

/**
 * Acá vivía `claseDe`, que traducía el estado del registro —pendiente, ya toca, compartida— a
 * un color de relleno.
 *
 * **Se fue en la 004 (FR-030a), y no por diseño sino por producto**: el jugador no sabía que
 * ese color decía algo. Un color que nadie lee no es información, es ruido compitiendo con el
 * que sí importa, que es la probabilidad de que la patente siga estando.
 *
 * El estado sigue existiendo y sigue haciendo su trabajo donde nadie se quejó: que una patente
 * ya compartida no dispare aviso al pasar cerca (FR-030b). Lo que se retiró es el color, no el
 * concepto.
 *
 * La propiedad `toca` que sobrevivió a aquello, eligiendo tamaño, también se fue en la 005: la
 * que toca va en su propia fuente ([coleccionesPatentes]) y su capa ya sabe qué dibuja.
 */

internal fun coleccion(marcadores: List<Marcador>) = FeatureCollection.fromFeatures(
    marcadores.map { featureDe(it, it.latitud, it.longitud) },
)

/** Un marcador dibujado en [latitud], [longitud]: su lugar real, o el corrido por el acomodo. */
private fun featureDe(m: Marcador, latitud: Double, longitud: Double) =
    Feature.fromGeometry(Point.fromLngLat(longitud, latitud)).apply {
        addNumberProperty(PROP_ID, m.id)
        // Con ceros a la izquierda: el 7 se ve `007`. Mantiene el ancho constante y es
        // lo mismo que ya hacen la barra de arriba y la notificación de proximidad.
        addStringProperty(PROP_NUMERO, m.numero.toString().padStart(3, '0'))
        addNumberProperty(PROP_PROBABILIDAD, m.probabilidad)
    }

/**
 * Las patentes ya acomodadas para un zoom (D13 de la 005, US5).
 *
 * Un `Suelto` va como cualquier marcador, pero en su posición de dibujo: el id viaja igual, así
 * que tocarlo abre la ficha del registro real (FR-023b). Un `Grupo` es un punto con su cuenta
 * y nada más: no tiene número ni probabilidad propia, ni id que abrir.
 */
internal fun coleccionAcomodada(marcadores: List<Marcador>, dibujos: List<Dibujo>): FeatureCollection {
    val porId = marcadores.associateBy { it.id }
    return FeatureCollection.fromFeatures(
        dibujos.map { d ->
            when (d) {
                is Dibujo.Suelto -> featureDe(porId.getValue(d.id), d.latitud, d.longitud)
                is Dibujo.Grupo -> Feature.fromGeometry(Point.fromLngLat(d.longitud, d.latitud))
                    .apply { addNumberProperty(PROP_CUENTA, d.cuenta) }
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
internal fun coleccionesPatentes(
    marcadores: List<Marcador>,
    zoom: Int,
): Pair<FeatureCollection, FeatureCollection> {
    val (tocan, resto) = marcadores.partition { it.toca }
    val pines = resto.map { Pin(it.id, it.latitud, it.longitud) }
    val dibujos = Acomodo.para(pines, zoom, PIN_ANCHO_DP.toDouble())
    return coleccionAcomodada(resto, dibujos) to coleccion(tocan)
}

/**
 * Qué medida tiene el pin, en px lógicos, y de dónde salen los números.
 *
 * [DESPLAZAMIENTO_NUMERO] es la única de las cuatro que no se elige: es la distancia del
 * centro de la cabeza a la punta, dividida por el tamaño del texto, y está **en ems** a
 * propósito. Al estar en ems escala sola con el texto, así que el mismo número centra el
 * número en el pin normal y en el agrandado de la que toca (D2).
 * ponytail: las cuatro se calibran mirando el teléfono, no son constantes sagradas.
 */
private const val PIN_ANCHO_DP = 34f
private const val PIN_ALTO_DP = 44f
private const val PIN_BORDE_DP = 3f
private const val TEXTO_PIN = 12f

/** Cuánto más grande es el pin de la patente que toca ahora (FR-030d de la 004, FR-001 de la 005). */
private const val PIN_TOCA = 1.4f

/** Centro de la cabeza sobre la punta, en ems de [TEXTO_PIN]. */
private const val DESPLAZAMIENTO_NUMERO = 2.25f

/**
 * Cuál de las tres imágenes de pin le toca a cada patente (FR-030).
 *
 * **Tres escalones y no un degradado.** La primera versión de la 003 interpolaba de rojo a
 * blanco a verde, y en la calle no se leía: entre un 4 y un 6 la diferencia era un tono de
 * blanco sucio, y el color de un marcador solo se puede juzgar contra los otros que estén a
 * la vista. Tres colores separados se reconocen de a uno y sin comparar.
 *
 * El corte va en 4 y en 8: 0 a 3 es rojo, 4 a 7 amarillo, 8 a 10 verde. Desde la v7 una
 * patente arranca en 0, así que la recién anotada sale roja y se pone amarilla a la cuarta
 * vez que se la vuelve a ver.
 *
 * Recibe los tres nombres porque sirve a dos capas: la de los pines blancos y la de los verdes
 * de la que toca (D2 de la 005). Los cortes son los mismos en las dos.
 */
private fun imagenPorProbabilidad(baja: String, media: String, alta: String) = Expression.step(
    Expression.get(PROP_PROBABILIDAD),
    Expression.literal(baja),
    Expression.stop(4, media),
    Expression.stop(8, alta),
)

/**
 * Dibuja el pin: relleno del color que se le pase, borde del otro, punta abajo (FR-028, FR-029).
 *
 * El relleno es blanco en todos los pines salvo en los de la que toca, que es verde (FR-001 de
 * la 005).
 *
 * Un solo `Path` y no un círculo más un triángulo: dos figuras superpuestas dejarían la línea
 * interna del borde cruzando la cabeza. El recorrido es punta, línea hasta el costado de la
 * cabeza, la vuelta entera de la cabeza, y `close` de vuelta a la punta.
 *
 * El bitmap se construye a la densidad de la pantalla y se le fija esa densidad encima, que es
 * de donde MapLibre saca la escala: así el pin mide lo mismo en cualquier teléfono, y el
 * `iconSize` queda libre para lo único que tiene que decir, cuál es la patente que toca.
 */
private fun bitmapDePin(context: Context, borde: String, relleno: Int = AndroidColor.WHITE): Bitmap {
    val metricas = context.resources.displayMetrics
    val escala = metricas.density
    val ancho = (PIN_ANCHO_DP * escala)
    val alto = (PIN_ALTO_DP * escala)
    val grosor = PIN_BORDE_DP * escala

    val bitmap = Bitmap.createBitmap(ancho.toInt(), alto.toInt(), Bitmap.Config.ARGB_8888)
    bitmap.density = metricas.densityDpi
    val lienzo = Canvas(bitmap)

    val radio = ancho / 2f - grosor / 2f
    val centroX = ancho / 2f
    val centroY = grosor / 2f + radio
    val cabeza = RectF(
        centroX - radio, centroY - radio,
        centroX + radio, centroY + radio,
    )

    // 0° es a las 3 en punto y los grados crecen hacia abajo. La cola se engancha a los 55° y
    // a los 125°, y el arco da la vuelta larga por arriba: 290° en sentido antihorario.
    val figura = Path().apply {
        moveTo(centroX, alto - grosor / 2f)
        arcTo(cabeza, 55f, -290f)
        close()
    }

    lienzo.drawPath(
        figura,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = relleno
        },
    )
    lienzo.drawPath(
        figura,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = grosor
            color = AndroidColor.parseColor(borde)
        },
    )
    return bitmap
}

/**
 * Registra las seis imágenes en el estilo: tres blancas y tres verdes. Corre una vez, al
 * cargarlo.
 *
 * Va antes de que se agregue ninguna capa: una `SymbolLayer` cuyo `iconImage` no existe no
 * falla, simplemente no dibuja nada, que es la clase de silencio que ya costó un diagnóstico
 * entero en la 002.
 */
private fun registrarPines(context: Context, style: Style) {
    style.addImage(PIN_BAJA, bitmapDePin(context, ColoresDeMapa.CONFIANZA_BAJA))
    style.addImage(PIN_MEDIA, bitmapDePin(context, ColoresDeMapa.CONFIANZA_MEDIA))
    style.addImage(PIN_ALTA, bitmapDePin(context, ColoresDeMapa.CONFIANZA_ALTA))

    val verde = AndroidColor.parseColor(ColoresDeMapa.TOCA)
    style.addImage(PIN_TOCA_BAJA, bitmapDePin(context, ColoresDeMapa.CONFIANZA_BAJA, verde))
    style.addImage(PIN_TOCA_MEDIA, bitmapDePin(context, ColoresDeMapa.CONFIANZA_MEDIA, verde))
    style.addImage(PIN_TOCA_ALTA, bitmapDePin(context, ColoresDeMapa.CONFIANZA_ALTA, verde))
}

/**
 * Las capas de los marcadores (FR-028 a FR-032).
 *
 * **Tres capas donde había cuatro.** El marcador suelto pasó de dos —círculo y número
 * encima— a una sola capa de símbolo que lleva las dos cosas: el pin como `iconImage` y el
 * número como `textField`. El grupo sigue con las suyas, círculo y cuenta, y eso ahora es
 * parte del mensaje: la **forma** separa un grupo de una patente mejor de lo que los
 * separaba el color, y un grupo no tiene un número ni una probabilidad propia que mostrar.
 *
 * La capa se agrega después de los trazos, y no es un detalle: MapLibre dibuja en orden de
 * inserción, y un camino encima de un marcador taparía justo lo que se fue a buscar (C2).
 *
 * **La de la que toca va última, después de los grupos** (D1 de la 005): es lo que la dibuja
 * encima de todo, grupos incluidos, y ocupa el lugar del `symbolSortKey` que antes la subía
 * dentro de una capa compartida.
 */
private fun pintarCapa(style: Style) {
    if (style.getLayer(CAPA) != null) return

    style.addLayer(
        capaDePines(
            id = CAPA,
            fuente = FUENTE,
            imagen = imagenPorProbabilidad(PIN_BAJA, PIN_MEDIA, PIN_ALTA),
            escala = 1f,
            // FR-029: el número en negro sobre el relleno blanco. El blanco no compite con
            // ningún trazo del mapa en ninguno de los tres modos.
            colorNumero = "#000000",
        ).withFilter(Expression.not(esGrupo())),
    )

    // FR-016: el grupo se dibuja más grande que un marcador suelto, para que se lea como
    // "acá hay varios" y no como una patente con un número raro.
    style.addLayer(
        CircleLayer(CAPA_GRUPO, FUENTE).withProperties(
            PropertyFactory.circleRadius(18f),
            PropertyFactory.circleStrokeWidth(2f),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleColor(ColoresDeMapa.GRUPO),
        ).withFilter(esGrupo()),
    )

    style.addLayer(
        SymbolLayer(CAPA_GRUPO_CUENTA, FUENTE).withProperties(
            // `cuenta` llega como número y `textField` espera texto.
            PropertyFactory.textField(Expression.toString(Expression.get(PROP_CUENTA))),
            // Misma obligación que en [capaDePines]: sin `textFont` el pedido de glifos no se
            // resuelve y se cae el teselado de la fuente entera.
            PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
            PropertyFactory.textSize(13f),
            PropertyFactory.textColor("#FFFFFF"),
            PropertyFactory.textAllowOverlap(true),
            PropertyFactory.textIgnorePlacement(true),
        ).withFilter(esGrupo()),
    )

    // FR-001 y FR-002 de la 005: la que toca, verde, más grande y encima de todo. Su fuente no
    // agrupa, así que no necesita filtro de grupo.
    style.addLayer(
        capaDePines(
            id = CAPA_TOCA,
            fuente = FUENTE_TOCA,
            imagen = imagenPorProbabilidad(PIN_TOCA_BAJA, PIN_TOCA_MEDIA, PIN_TOCA_ALTA),
            escala = PIN_TOCA,
            // Negro sobre ese verde queda con poco contraste; blanco es lo que llevaba antes
            // de la 004 (D2).
            colorNumero = "#FFFFFF",
        ),
    )
}

/**
 * Una capa de pines con su número (FR-028 a FR-030).
 *
 * Dos capas la usan —los pines blancos y los verdes de la que toca—, y difieren solo en la
 * fuente, las imágenes, el tamaño y el color del número. Todo lo demás es lo mismo, y es lo
 * que costó aprender.
 */
private fun capaDePines(
    id: String,
    fuente: String,
    imagen: Expression,
    escala: Float,
    colorNumero: String,
) = SymbolLayer(id, fuente).withProperties(
    // FR-030: el borde del pin **es** la probabilidad, y es el único color que habla de la
    // patente. Va elegido por imagen y no por tinte, ver D1 de la 004.
    PropertyFactory.iconImage(imagen),

    // FR-028: la punta cae sobre la coordenada. Es la mitad de por qué un pin y no un
    // círculo: un círculo marca un área, un pin marca un punto.
    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),

    PropertyFactory.iconSize(escala),
    PropertyFactory.iconAllowOverlap(true),
    PropertyFactory.iconIgnorePlacement(true),

    PropertyFactory.textField(Expression.get(PROP_NUMERO)),

    // **`textFont` es obligatorio acá, no decorativo.**
    //
    // Sin declararlo, MapLibre pide su tipografía por defecto —"Open Sans Regular"— al
    // servidor de glifos del estilo. OpenFreeMap Liberty solo publica Noto Sans, así que ese
    // pedido no se resuelve nunca. Y como la capa comparte fuente con los pines, arrastra
    // consigo el teselado de la fuente entera: no se dibuja **nada**, sin un solo error en
    // logcat. Ese fue el bug de los marcadores invisibles de la 002, y la línea sigue haciendo
    // falta.
    PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
    PropertyFactory.textSize(TEXTO_PIN * escala),
    PropertyFactory.textColor(colorNumero),

    // El número va en la cabeza del pin, no sobre la punta. **El desplazamiento está en ems**,
    // o sea relativo al tamaño del texto, y eso es lo que hace que el mismo número sirva para
    // el pin normal y para el agrandado: los dos escalan igual (D2 de la 004).
    PropertyFactory.textAnchor(Property.TEXT_ANCHOR_CENTER),
    PropertyFactory.textOffset(arrayOf(0f, -DESPLAZAMIENTO_NUMERO)),

    // Sin esto MapLibre esconde las etiquetas que colisionan, y un registro que desaparece
    // del mapa es peor que uno amontonado.
    PropertyFactory.textAllowOverlap(true),
    PropertyFactory.textIgnorePlacement(true),
)

/**
 * FR-034: el punto azul del jugador, y la cámara puesta donde está parado.
 *
 * Sin esto el mapa abre en vista mundial y las patentes quedan como puntos sobre el
 * planeta, que es lo que pasaba antes de T084. `CameraMode.TRACKING` resuelve las dos
 * mitades del requisito de una: dibuja la posición actual y sigue al jugador.
 *
 * No toca el camino de captura (FR-038): corre después de que el estilo cargó, y si el
 * permiso no está, se saltea sin romper nada.
 */
@SuppressLint("MissingPermission")
private fun mostrarAlJugador(context: Context, map: MapLibreMap, style: Style) {
    if (!Permisos.tieneUbicacionPrecisa(context)) return

    val componente = map.locationComponent
    componente.activateLocationComponent(
        LocationComponentActivationOptions.builder(context, style).build(),
    )
    componente.isLocationComponentEnabled = true
    componente.renderMode = RenderMode.NORMAL

    // El zoom se fija en la cámara **antes** de activar el seguimiento, y no solo en el
    // callback de abajo. En el primer arranque del día todavía no hay ninguna posición: la
    // transición de cámara termina sin nada que mover, el zoom se pide sobre un mapa sin
    // objetivo, y cuando el fix finalmente llega la cámara centra al jugador pero al zoom
    // del planeta. Se ve como "no se centró".
    //
    // La segunda vez sí funciona porque ya hay última posición conocida, y ese era
    // exactamente el síntoma reportado. Fijando el zoom acá, al seguimiento solo le queda
    // mover el objetivo, que es lo único que depende de tener posición.
    map.moveCamera(CameraUpdateFactory.zoomTo(ZOOM_CALLE))

    // El zoom se pide **cuando la transición de cámara terminó**, no en la línea siguiente
    // a pedir el modo. Activar el seguimiento arranca una animación, y durante esa
    // animación MapLibre descarta el pedido de zoom en silencio, con este log:
    //
    //   E Mbgl-LocationComponent: LocationComponent#zoomWhileTracking method call is
    //   ignored because the camera mode is transitioning
    //
    // El síntoma es un mapa que sigue tu posición pero se queda en vista mundial: parece
    // que la ubicación no funciona, y en realidad lo único que se perdió fue el zoom.
    componente.setCameraMode(
        CameraMode.TRACKING,
        object : OnLocationCameraTransitionListener {
            override fun onLocationCameraTransitionFinished(cameraMode: Int) {
                componente.zoomWhileTracking(ZOOM_CALLE)
            }

            override fun onLocationCameraTransitionCanceled(cameraMode: Int) = Unit
        },
    )
}

/**
 * FR-043 y US1/AC8: una zona sin fondo guardado tiene que verse como tal.
 *
 * Sin conexión, los tiles que no están en el caché ambiente simplemente no se dibujan y
 * queda a la vista la capa `background` del estilo, que en Liberty es un gris claro
 * indistinguible de un área vacía real. Pintarla de un tono deliberado convierte ese
 * "vacío ambiguo" que la spec prohíbe en una señal legible: gris azulado = acá no tengo
 * mapa guardado.
 *
 * ponytail: MapLibre no expone el estado de caché por tile, así que esto distingue por
 * ausencia de dibujo, no por consulta. Si algún día hace falta precisión real, el camino
 * es `OfflineManager` con regiones explícitas, que es mucho más caro y que FR-042
 * justamente evita.
 */
private fun marcarZonaSinFondo(style: Style, oscuro: Boolean) {
    (style.getLayer("background") as? BackgroundLayer)?.setProperties(
        PropertyFactory.backgroundColor(
            if (oscuro) ColoresDeMapa.SIN_FONDO_OSCURO else ColoresDeMapa.SIN_FONDO,
        ),
    )
}

/** Zoom de calle: a esta altura se leen las cuadras, que es la unidad del juego. */
private const val ZOOM_CALLE = 16.0

/**
 * Hasta dónde se deja acercar el mapa (FR-039).
 *
 * 19 es un par de niveles por encima del zoom de calle: alcanza para separar patentes de la
 * misma cuadra. Por arriba de eso el estilo no publica más detalle y el mapa se sentía
 * pesado al desplazarlo.
 *
 * ponytail: si dos patentes de la misma vereda siguen sin poder separarse, subirlo — y
 * volver a mirar cómo se mueve el mapa, que es lo que este número paga.
 */
private const val ZOOM_MAXIMO = 19.0


/**
 * El control que la pantalla tiene sobre el mapa (FR-006, FR-007).
 *
 * `MapaDeFondo` se dibuja solo, pero dos cosas tienen que cruzar la frontera: la pantalla
 * necesita saber si el mapa está siguiendo al jugador —para mostrar u ocultar el botón de
 * recentrado— y necesita poder pedirle que vuelva. Dos consumidores reales, así que el
 * Principio IV admite la clase.
 */
class EstadoDelMapa {

    internal var mapa: MapLibreMap? = null

    /** False desde que el jugador arrastra el mapa, hasta que toca recentrar. */
    var siguiendo by mutableStateOf(true)
        internal set

    /** FR-007: volver a la posición actual y retomar el seguimiento. */
    @SuppressLint("MissingPermission")
    fun recentrar() {
        val componente = mapa?.locationComponent ?: return
        if (!componente.isLocationComponentActivated) return

        componente.setCameraMode(
            CameraMode.TRACKING,
            object : OnLocationCameraTransitionListener {
                override fun onLocationCameraTransitionFinished(cameraMode: Int) {
                    componente.zoomWhileTracking(ZOOM_CALLE)
                }

                override fun onLocationCameraTransitionCanceled(cameraMode: Int) = Unit
            },
        )
        siguiendo = true
    }

    /**
     * FR-006: el arrastre del jugador apaga el seguimiento.
     *
     * Sin esto el mapa pelea contra el dedo: el jugador arrastra para mirar otra zona,
     * llega la siguiente lectura de ubicación y la cámara vuelve sola. Es la forma
     * concreta en que este comportamiento se hace mal.
     */
    internal fun elJugadorArrastro() = dejarDeSeguir()

    /**
     * Encuadra todas las patentes de un número junto con el jugador (FR-006a de la 005).
     *
     * **Sin patentes no se mueve**, aunque se sepa dónde está el jugador: centrarlo en su propia
     * posición diría "acá están", y no hay ninguna (C5). La guarda vive acá y no en quien llama
     * para que ningún llamador pueda olvidarla.
     *
     * Deja de seguir al jugador igual que cuando él arrastra el mapa (FR-006b): si la cámara
     * volviera a centrarlo en la próxima lectura de ubicación, deshacería el encuadre. Volver a
     * seguirlo es el botón de recentrar de siempre, que aparece solo porque [siguiendo] queda en
     * false.
     */
    @SuppressLint("MissingPermission")
    fun encuadrar(posiciones: List<Pair<Double, Double>>) {
        val map = mapa ?: return
        if (posiciones.isEmpty()) return

        val componente = map.locationComponent
        val jugador = if (componente.isLocationComponentActivated) {
            dejarDeSeguir()
            componente.lastKnownLocation?.let { LatLng(it.latitude, it.longitude) }
        } else {
            null
        }

        val puntos = posiciones.map { (lat, lon) -> LatLng(lat, lon) } + listOfNotNull(jugador)
        camaraPara(puntos, margenesDePantallaPrincipal())?.let { map.animateCamera(it) }
    }

    /**
     * Lleva el mapa hasta una patente de la lista de la barra (FR-011a de la 005).
     *
     * Deja de seguir al jugador por lo mismo que [encuadrar]. No aleja nunca: si el mapa ya está
     * a zoom de calle o más cerca, lo deja en ese zoom.
     */
    fun centrarEn(latitud: Double, longitud: Double) {
        val map = mapa ?: return
        dejarDeSeguir()
        val zoom = maxOf(map.cameraPosition.zoom, ZOOM_CALLE)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitud, longitud), zoom))
    }

    private fun dejarDeSeguir() {
        val componente = mapa?.locationComponent ?: return
        if (!componente.isLocationComponentActivated) return
        componente.cameraMode = CameraMode.NONE
        siguiendo = false
    }
}

/**
 * Cuánto de la pantalla principal tapa lo que va encima del mapa, en px: la barra arriba, y la
 * leyenda, la franja de botones y el campo abajo. Un encuadre que no lo descuente deja las
 * patentes del borde debajo de los controles.
 *
 * Se lee la densidad del sistema y no la de un `Context` porque [EstadoDelMapa] no tiene uno, y
 * para convertir dp alcanza con la de la pantalla.
 * ponytail: los dp se calibran mirando el teléfono. Con el teclado arriba la mitad de abajo
 * queda tapada igual; el encuadre queda bien apenas se lo baja (D6).
 */
private fun margenesDePantallaPrincipal(): IntArray {
    val densidad = android.content.res.Resources.getSystem().displayMetrics.density
    fun px(dp: Int) = (dp * densidad).toInt()
    return intArrayOf(px(40), px(140), px(40), px(220))
}

/**
 * FR-008 y FR-010a: encuadrar lo que hay para mostrar, en vez del planeta.
 *
 * Sirve a dos casos con el mismo cálculo. En la pantalla principal, cuando no hay permiso ni
 * lectura: una vista mundial muda es lo que la spec prohíbe. En el mapa de una salida, donde
 * es el mecanismo que hace que el camino se vea entero sin que el jugador desplace ni haga
 * zoom a mano.
 *
 * Encuadra las patentes **y** el camino: encuadrar solo las patentes dejaría afuera el tramo
 * de la salida donde no se capturó ninguna, que suele ser la mitad. Si no hay nada de nada
 * —app recién instalada— no hay qué encuadrar y el mapa se queda como está.
 */
private fun encuadrar(map: MapLibreMap, marcadores: List<Marcador>, trazos: List<Trazo>) {
    val posiciones = marcadores.map { LatLng(it.latitud, it.longitud) } +
        trazos.flatMap { t -> t.puntos.map { (lat, lon) -> LatLng(lat, lon) } }
    camaraPara(posiciones)?.let { map.moveCamera(it) }
}

/**
 * La cámara que muestra todas las posiciones, o null si no hay ninguna.
 *
 * `LatLngBounds` no admite un solo punto: con uno, se centra a zoom de calle. [margenes] va en
 * px, en el orden de MapLibre —izquierda, arriba, derecha, abajo—; sin él, el mismo relleno en
 * los cuatro lados.
 */
private fun camaraPara(posiciones: List<LatLng>, margenes: IntArray? = null): CameraUpdate? =
    when {
        posiciones.isEmpty() -> null
        posiciones.size == 1 -> CameraUpdateFactory.newLatLngZoom(posiciones.first(), ZOOM_CALLE)
        else -> {
            val limites = LatLngBounds.Builder().includes(posiciones).build()
            if (margenes == null) {
                CameraUpdateFactory.newLatLngBounds(limites, RELLENO_ENCUADRE)
            } else {
                CameraUpdateFactory.newLatLngBounds(
                    limites, margenes[0], margenes[1], margenes[2], margenes[3],
                )
            }
        }
    }

/** Margen en píxeles para que los marcadores del borde no queden pegados al canto. */
private const val RELLENO_ENCUADRE = 120

/**
 * Mapa de fondo de la pantalla principal (FR-034).
 *
 * Se infla por [AndroidView] y carga su estilo por su cuenta. **Nada de lo que pasa acá
 * puede bloquear la captura** (FR-038): el campo de número y el teclado se componen en el
 * primer frame, y este mapa aparece detrás cuando esté listo. Si nunca carga —sin red, en
 * un sótano— la carga rápida sigue funcionando igual.
 */
@Composable
fun MapaDeFondo(
    marcadores: List<Marcador> = emptyList(),
    trazos: List<Trazo> = emptyList(),
    /** Cuál de las tres preguntas contesta el mapa (FR-002). Baja de la pantalla, que la leyó
     *  de la base: el mapa no lo decide ni lo persiste. */
    modo: ModoMapa = ModoMapa.COBERTURA,
    oscuro: Boolean = isSystemInDarkTheme(),
    seguirAlJugador: Boolean = true,
    estado: EstadoDelMapa? = null,
    onTocarMarcador: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapLibre.getInstance(context)
        CacheDeMapa(context).configurarTecho()
        MapView(context)
    }

    DisposableEffect(lifecycleOwner) {
        mapView.onCreate(null)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    // El estilo se arma una sola vez, y su callback no vuelve a correr: sin esto encuadraría
    // la lista vacía de la primera composición, antes de que Room emita.
    val marcadoresActuales by rememberUpdatedState(marcadores)
    val trazosActuales by rememberUpdatedState(trazos)

    var estiloListo by remember { mutableStateOf(false) }

    // **La carga del estilo corre una sola vez.**
    //
    // Antes vivía en el `update` del AndroidView, con un guard `if (map.style?.isFullyLoaded
    // != true)`. Pero `setStyle` es asíncrono: mientras la primera carga está en vuelo el
    // guard sigue dando true, así que cada recomposición —y tipear un dígito recompone la
    // pantalla entera— disparaba otra carga. Cada `setStyle` destruye las fuentes y capas
    // de la anterior, y la última en llegar se quedaba sin las que ya se habían pintado.
    LaunchedEffect(Unit) {
        mapView.getMapAsync { map ->
            map.setStyle(ConfigMapa.estiloUrl(oscuro)) { style ->
                marcarZonaSinFondo(style, oscuro)

                // Antes que ninguna capa: la de los pines los busca por nombre.
                registrarPines(context, style)

                estado?.mapa = map

                // FR-039: techo de zoom. Más allá de esto el estilo ya no tiene detalle que
                // agregar —viene estirando teselas— y el mapa se arrastraba al desplazarlo.
                map.setMaxZoomPreference(ZOOM_MAXIMO)

                // FR-040: sin rotación. Acá el norte arriba es información —el rumbo a la
                // patente se dice en puntos cardinales (FR-003)— y el gesto de dos dedos se
                // disparaba solo al hacer zoom, dejando el mapa torcido caminando. La brújula
                // se apaga con él: existía únicamente para deshacer la rotación.
                map.uiSettings.isRotateGesturesEnabled = false
                map.uiSettings.isCompassEnabled = false

                // T024 / FR-012: se consulta qué hay **dibujado** en el punto del dedo, en
                // lugar de calcular a mano cuál marcador queda más cerca. Así el zoom sale
                // gratis, y también saldría la rotación si la hubiera.
                map.addOnMapClickListener { punto ->
                    val enPantalla = map.projection.toScreenLocation(punto)
                    // Las dos capas de pines: la que toca vive en la suya desde la 005.
                    val tocadas = map.queryRenderedFeatures(enPantalla, CAPA_TOCA, CAPA)
                    val id = tocadas.firstOrNull()?.getNumberProperty(PROP_ID)?.toLong()
                    if (id != null) onTocarMarcador(id)
                    id != null
                }
                // T014 / FR-006: REASON_API_GESTURE es el dedo del jugador. Los movimientos
                // que dispara el propio seguimiento llegan con otra razón, así que esto no
                // se apaga a sí mismo.
                map.addOnCameraMoveStartedListener { razon ->
                    if (razon == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                        estado?.elJugadorArrastro()
                    }
                }

                // FR-023c de la 005: el acomodo es por zoom entero. Desplazar no cambia el
                // zoom, así que no mueve ningún pin; pasar a otro nivel sí lo recalcula.
                var zoomAcomodado = -1
                map.addOnCameraIdleListener {
                    val zoom = map.cameraPosition.zoom.toInt()
                    if (zoom == zoomAcomodado) return@addOnCameraIdleListener
                    zoomAcomodado = zoom
                    val (resto, _) = coleccionesPatentes(marcadoresActuales, zoom)
                    style.getSourceAs<GeoJsonSource>(FUENTE)?.setGeoJson(resto)
                }

                if (seguirAlJugador && Permisos.tieneUbicacionPrecisa(context)) {
                    mostrarAlJugador(context, map, style)
                } else {
                    // FR-008: sin permiso no hay a quién seguir, pero tampoco se deja el
                    // planeta entero a la vista. Con seguimiento apagado —el mapa de una
                    // salida, C3— esto es además lo que encuadra el camino entero.
                    encuadrar(map, marcadoresActuales, trazosActuales)
                }

                estiloListo = true
            }
        }
    }

    // Pinta los datos: cuando el estilo termina de cargar, y cada vez que cambian de verdad
    // —al guardar, borrar o corregir una patente, o al volver de un recorrido.
    //
    // `estiloListo` es una clave y no un detalle. Room emite los marcadores mucho antes de
    // que vuelva el tile server, así que para cuando hay estilo los datos ya llegaron a su
    // valor final y no van a cambiar más. Sin esta clave, el efecto no vuelve a correr
    // nunca y las capas no se pintan: datos correctos, mapa vacío. Ese era el bug de los
    // marcadores invisibles, y no la agrupación ni la capa de números.
    LaunchedEffect(estiloListo, marcadores, trazos, modo) {
        if (!estiloListo) return@LaunchedEffect
        mapView.getMapAsync { map ->
            map.style?.takeIf { it.isFullyLoaded }?.let {
                actualizarCapas(it, marcadores, trazos, modo, map.cameraPosition.zoom.toInt())
            }
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

/**
 * Refresca los datos dibujados sin volver a cargar el estilo entero.
 *
 * Los trazos se agregan **antes** que los marcadores, y el orden es el contrato C2: en
 * MapLibre el orden de inserción es el orden de dibujo, y las patentes tienen que quedar
 * arriba del camino.
 */
private fun actualizarCapas(
    style: Style,
    marcadores: List<Marcador>,
    trazos: List<Trazo>,
    modo: ModoMapa,
    /** El zoom entero de la cámara, para el acomodo (D13 de la 005). */
    zoom: Int,
) {
    val recorridos = style.getSourceAs<GeoJsonSource>(FUENTE_TRAZOS)
    if (recorridos == null) {
        style.addSource(GeoJsonSource(FUENTE_TRAZOS, coleccionTrazos(trazos)))
        style.addSource(GeoJsonSource(FUENTE_HUECOS, coleccionHuecos(trazos)))
        pintarTrazos(style)
    } else {
        recorridos.setGeoJson(coleccionTrazos(trazos))
        style.getSourceAs<GeoJsonSource>(FUENTE_HUECOS)?.setGeoJson(coleccionHuecos(trazos))
    }

    aplicarModo(style, modo)

    // D1 de la 005: las que tocan a una fuente propia, el resto acomodado a la de siempre.
    val (resto, tocan) = coleccionesPatentes(marcadores, zoom)

    val fuente = style.getSourceAs<GeoJsonSource>(FUENTE)
    if (fuente == null) {
        style.addSource(GeoJsonSource(FUENTE, resto))
        style.addSource(GeoJsonSource(FUENTE_TOCA, tocan))
        pintarCapa(style)
    } else {
        fuente.setGeoJson(resto)
        style.getSourceAs<GeoJsonSource>(FUENTE_TOCA)?.setGeoJson(tocan)
    }
}
