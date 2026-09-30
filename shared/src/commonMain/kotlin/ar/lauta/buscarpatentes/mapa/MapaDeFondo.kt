package ar.lauta.buscarpatentes.mapa

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ar.lauta.buscarpatentes.data.ModoMapa
import ar.lauta.buscarpatentes.domain.Escalon
import ar.lauta.buscarpatentes.plataforma.Carpetas
import ar.lauta.buscarpatentes.plataforma.Permiso
import ar.lauta.buscarpatentes.plataforma.tienePermiso
import ar.lauta.buscarpatentes.ui.ColoresDeMapa
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.expressions.dsl.asBoolean
import org.maplibre.compose.expressions.dsl.asNumber
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.case
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToString
import org.maplibre.compose.expressions.dsl.exponential
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.interpolate
import org.maplibre.compose.expressions.dsl.not
import org.maplibre.compose.expressions.dsl.span
import org.maplibre.compose.expressions.dsl.step
import org.maplibre.compose.expressions.dsl.switch
import org.maplibre.compose.expressions.dsl.textOffset
import org.maplibre.compose.expressions.dsl.zoom
import org.maplibre.compose.expressions.value.BooleanValue
import org.maplibre.compose.expressions.value.ColorValue
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.expressions.ast.Expression
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.layers.Anchor
import org.maplibre.compose.layers.BackgroundLayer
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.FeaturesClickHandler
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.LocationIndicatorLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.location.LocationTrackingEffect
import org.maplibre.compose.location.rememberLocationState
import org.maplibre.compose.map.CameraConstraints
import org.maplibre.compose.map.DefaultMapRuntime
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonSource
import org.maplibre.compose.sources.VectorSource
import org.maplibre.compose.sources.getBaseSource
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.DpPadding
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position

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
     * **`positron` se probó y se descartó**: dibuja las calles menores en un gris tan claro que
     * en la calle solo se distinguían las avenidas. `liberty` las dibuja bien, y `fiord` es su
     * equivalente oscuro. **Los dos publican Noto Sans**, que es la tipografía de los números.
     */
    fun estiloUrl(oscuro: Boolean): String =
        if (oscuro) "https://tiles.openfreemap.org/styles/fiord"
        else "https://tiles.openfreemap.org/styles/liberty"

    /**
     * Techo del caché ambiente de teselas (FR-044). MapLibre guarda solo lo que se muestra y
     * desaloja por uso al llegar al límite.
     * ponytail: 200 MB cubre un barrio entero con holgura. Calibrable.
     */
    const val CACHE_MAXIMO_BYTES = 200L * 1024 * 1024

    /**
     * El archivo del caché. Vive en `Carpetas.base` y no en la caché del sistema, que el sistema
     * vacía cuando quiere: el mapa sin conexión de una zona ya vista es el SC-013 de la 001.
     * Cada teléfono lo configura al arrancar, antes del primer mapa.
     */
    const val ARCHIVO_CACHE = "maplibre-cache.db"

    val rutaCache: String get() = Path(Carpetas.base, ARCHIVO_CACHE).toString()
}

/** Estado del caché de mapa, para la pantalla de ajustes (FR-045). */
object CacheDeMapa {
    fun espacioOcupado(): Long = SystemFileSystem.metadataOrNull(Path(ConfigMapa.rutaCache))?.size ?: 0L

    suspend fun borrar() {
        runCatching { DefaultMapRuntime.instance.offlineManager.clearAmbientCache() }
    }
}

/** Zoom de calle: a esta altura se leen las cuadras, que es la unidad del juego. */
private const val ZOOM_CALLE = 16.0

/**
 * Hasta dónde se deja acercar el mapa (FR-039). Por arriba de 19 el estilo no publica más detalle.
 * ponytail: si dos patentes de la misma vereda siguen sin poder separarse, subirlo.
 */
private const val ZOOM_MAXIMO = 19.0

/**
 * Cuánto de la pantalla principal tapa lo que va encima del mapa: la barra arriba, y la
 * leyenda, la franja de botones y el campo abajo. Un encuadre que no lo descuente deja las
 * patentes del borde debajo de los controles.
 * ponytail: se calibra mirando el teléfono.
 */
private val MARGENES_PANTALLA_PRINCIPAL = DpPadding(left = 40.dp, top = 140.dp, right = 40.dp, bottom = 220.dp)

/** Margen para que los marcadores del borde no queden pegados al canto. */
private val RELLENO_ENCUADRE = DpPadding(48.dp, 48.dp, 48.dp, 48.dp)

/**
 * El control que la pantalla tiene sobre el mapa (FR-006, FR-007).
 *
 * `MapaDeFondo` se dibuja solo, pero dos cosas tienen que cruzar la frontera: la pantalla
 * necesita saber si el mapa está siguiendo al jugador —para mostrar u ocultar el botón de
 * recentrado— y necesita poder pedirle que vuelva. Dos consumidores reales, así que el
 * Principio IV admite la clase.
 */
class EstadoDelMapa {

    internal var mapa: MapState? = null
    internal var alcance: CoroutineScope? = null

    /** La última posición del jugador, o null sin permiso o antes de la primera lectura. */
    internal var jugador: Position? = null

    /** False desde que el jugador arrastra el mapa, hasta que toca recentrar. */
    var siguiendo by mutableStateOf(true)
        internal set

    /** FR-007: volver a la posición actual y retomar el seguimiento. */
    fun recentrar() {
        siguiendo = true
        val m = mapa ?: return
        val p = jugador ?: return
        alcance?.launch { m.animateCamera(CameraUpdate(target = p, zoom = ZOOM_CALLE)) }
    }

    /**
     * FR-006: el arrastre del jugador apaga el seguimiento. Sin esto el mapa pelea contra el
     * dedo: llega la siguiente lectura de ubicación y la cámara vuelve sola.
     */
    internal fun elJugadorArrastro() {
        if (jugador != null) siguiendo = false
    }

    /**
     * Encuadra todas las patentes de un número junto con el jugador (FR-006a de la 005).
     *
     * **Sin patentes no se mueve**, aunque se sepa dónde está el jugador: centrarlo en su propia
     * posición diría "acá están", y no hay ninguna (C5). Deja de seguir al jugador igual que
     * cuando él arrastra el mapa (FR-006b).
     */
    fun encuadrar(posiciones: List<Pair<Double, Double>>) {
        val m = mapa ?: return
        if (posiciones.isEmpty()) return
        val yo = jugador
        if (yo != null) siguiendo = false
        val puntos = posiciones.map { (lat, lon) -> Position(lon, lat) } + listOfNotNull(yo)
        alcance?.launch { m.irA(puntos, MARGENES_PANTALLA_PRINCIPAL, animado = true) }
    }

    /**
     * Lleva el mapa hasta una patente de la lista de la barra (FR-011a de la 005). No aleja
     * nunca: si el mapa ya está a zoom de calle o más cerca, lo deja en ese zoom.
     */
    fun centrarEn(latitud: Double, longitud: Double) {
        val m = mapa ?: return
        if (jugador != null) siguiendo = false
        val zoom = maxOf(m.cameraPosition.zoom, ZOOM_CALLE)
        alcance?.launch { m.animateCamera(CameraUpdate(target = Position(longitud, latitud), zoom = zoom)) }
    }
}

/**
 * Mueve la cámara para que se vean todas las [posiciones]. Con una sola, la centra a zoom de
 * calle: un área de un punto no tiene zoom que calcular.
 */
private suspend fun MapState.irA(posiciones: List<Position>, margen: DpPadding, animado: Boolean) {
    when {
        posiciones.isEmpty() -> Unit
        posiciones.size == 1 -> {
            val destino = CameraUpdate(target = posiciones.first(), zoom = ZOOM_CALLE)
            if (animado) animateCamera(destino) else setCameraPosition(
                cameraPosition.copy(target = posiciones.first(), zoom = ZOOM_CALLE),
            )
        }
        else -> {
            val limites = BoundingBox(
                west = posiciones.minOf { it.longitude },
                south = posiciones.minOf { it.latitude },
                east = posiciones.maxOf { it.longitude },
                north = posiciones.maxOf { it.latitude },
            )
            if (animado) animateCameraToBounds(limites, fitPadding = margen)
            else fitCameraToBounds(limites, fitPadding = margen)
        }
    }
}

/**
 * Mapa de fondo de la pantalla principal y del detalle de una salida (FR-034).
 *
 * **Nada de lo que pasa acá puede bloquear la captura** (FR-038): el campo de número y el
 * teclado se componen en el primer frame, y este mapa aparece detrás cuando esté listo. Si nunca
 * carga —sin red, en un sótano— la carga rápida sigue funcionando igual.
 *
 * Desde la 006 es el mismo mapa en el Android y en el iPhone, sobre maplibre-compose (D5).
 */
@Composable
fun MapaDeFondo(
    marcadores: List<Marcador> = emptyList(),
    trazos: List<Trazo> = emptyList(),
    /** Cuál de las tres preguntas contesta el mapa (FR-002). Baja de la pantalla. */
    modo: ModoMapa = ModoMapa.COBERTURA,
    oscuro: Boolean = isSystemInDarkTheme(),
    seguirAlJugador: Boolean = true,
    estado: EstadoDelMapa? = null,
    onTocarMarcador: (Long) -> Unit = {},
    /** El borde y las cuadras de cada zona, ya decididos por quien llama (contrato Z4 de la 008). */
    zonas: List<DibujoDeZona> = emptyList(),
    /** Un toque que no cayó sobre nada: marca una esquina de una zona nueva (D9 de la 008). */
    onTocarMapa: ((latitud: Double, longitud: Double) -> Unit)? = null,
    /** Un toque sobre una cuadra, con su id (FR-012 de la 008). */
    onTocarCuadra: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val alcance = rememberCoroutineScope()
    val conPermiso = remember { tienePermiso(Permiso.UBICACION) }

    // FR-023c de la 005: el acomodo es por zoom entero, y se recalcula recién cuando la cámara
    // quedó quieta. Desplazar no cambia el zoom, así que no mueve ningún pin.
    var zoomAcomodado by remember { mutableIntStateOf(ZOOM_CALLE.toInt()) }
    val (resto, tocan) = remember(marcadores, zoomAcomodado) { coleccionesPatentes(marcadores, zoomAcomodado) }
    val coleccionTrazos = remember(trazos) { coleccionTrazos(trazos) }
    val coleccionHuecos = remember(trazos) { coleccionHuecos(trazos) }
    val coleccionBordes = remember(zonas) { coleccionBordes(zonas) }
    val coleccionCuadras = remember(zonas) { coleccionCuadras(zonas) }
    val coleccionEsquinas = remember(zonas) { coleccionEsquinas(zonas) }

    val pines = remember {
        Pines(
            baja = PinPainter(colorDe(ColoresDeMapa.CONFIANZA_BAJA), Color.White),
            media = PinPainter(colorDe(ColoresDeMapa.CONFIANZA_MEDIA), Color.White),
            alta = PinPainter(colorDe(ColoresDeMapa.CONFIANZA_ALTA), Color.White),
            tocaBaja = PinPainter(colorDe(ColoresDeMapa.CONFIANZA_BAJA), colorDe(ColoresDeMapa.TOCA)),
            tocaMedia = PinPainter(colorDe(ColoresDeMapa.CONFIANZA_MEDIA), colorDe(ColoresDeMapa.TOCA)),
            tocaAlta = PinPainter(colorDe(ColoresDeMapa.CONFIANZA_ALTA), colorDe(ColoresDeMapa.TOCA)),
        )
    }

    val ubicacion = rememberLocationState(enabled = conPermiso)

    val mapa = rememberMapState(
        baseStyle = BaseStyle.Uri(ConfigMapa.estiloUrl(oscuro)),
        initialCameraPosition = CameraPosition(zoom = ZOOM_CALLE),
    ) {
        // FR-043: una zona sin fondo guardado tiene que verse como tal. Sin conexión, las teselas
        // que no están en el caché no se dibujan y queda a la vista el fondo del estilo. Pintarlo
        // de un tono deliberado dice "acá no tengo mapa guardado".
        Anchor.Above("background") {
            BackgroundLayer(
                id = "sin-fondo",
                color = const(colorDe(if (oscuro) ColoresDeMapa.SIN_FONDO_OSCURO else ColoresDeMapa.SIN_FONDO)),
            )
        }

        // El fondo oscuro dibuja las avenidas casi igual que las calles, y el jugador se orienta
        // por ellas. Encima de las calles del estilo y debajo de las vías y los nombres.
        if (oscuro) Anchor.Above("highway_motorway_subtle") { CapasDeAvenidas() }

        Anchor.Top {
            // Debajo de los trazos: lo pendiente es el hueco que dejan, y lo caminado se ve encima.
            CapasDeZonas(
                cuadras = rememberGeoJsonSource(GeoJsonData.Features(coleccionCuadras)),
                bordes = rememberGeoJsonSource(GeoJsonData.Features(coleccionBordes)),
                esquinas = rememberGeoJsonSource(GeoJsonData.Features(coleccionEsquinas)),
                onTocarCuadra = onTocarCuadra,
            )
            CapasDeTrazos(
                trazos = rememberGeoJsonSource(GeoJsonData.Features(coleccionTrazos)),
                huecos = rememberGeoJsonSource(GeoJsonData.Features(coleccionHuecos)),
                modo = modo,
            )
            CapasDePatentes(
                resto = rememberGeoJsonSource(GeoJsonData.Features(resto)),
                tocan = rememberGeoJsonSource(GeoJsonData.Features(tocan)),
                pines = pines,
                onTocar = onTocarMarcador,
            )
            if (conPermiso) LocationIndicatorLayer(id = "jugador", locationState = ubicacion)
        }
    }

    LaunchedEffect(mapa, estado) {
        estado?.mapa = mapa
        estado?.alcance = alcance
    }

    // FR-023c de la 005: el zoom entero de la cámara quieta.
    LaunchedEffect(mapa) {
        snapshotFlow { if (mapa.isCameraMoving) null else mapa.cameraPosition.zoom.toInt() }
            .distinctUntilChanged()
            .collect { zoom -> if (zoom != null) zoomAcomodado = zoom }
    }

    // FR-006: el arrastre del jugador apaga el seguimiento. Los movimientos que dispara el propio
    // seguimiento llegan como PROGRAMMATIC, así que esto no se apaga a sí mismo.
    LaunchedEffect(mapa) {
        snapshotFlow { mapa.isCameraMoving && mapa.cameraMoveReason == CameraMoveReason.GESTURE }
            .distinctUntilChanged()
            .filter { it }
            .collect { estado?.elJugadorArrastro() }
    }

    // FR-034: la cámara puesta donde está parado el jugador, y siguiéndolo. La primera lectura
    // salta sin animar y fija el zoom de calle; las siguientes solo mueven el objetivo.
    LocationTrackingEffect(locationState = ubicacion, enabled = conPermiso && seguirAlJugador, trackBearing = false) {
        val posicion = currentLocation.position
        estado?.jugador = posicion
        if (estado?.siguiendo == false) return@LocationTrackingEffect
        if (previousLocation == null) {
            mapa.setCameraPosition(mapa.cameraPosition.copy(target = posicion, zoom = ZOOM_CALLE))
        } else {
            mapa.animateCamera(CameraUpdate(target = posicion))
        }
    }

    // FR-008 y FR-010a: sin a quién seguir, encuadrar lo que hay para mostrar en vez del planeta.
    // En el mapa de una salida es lo que hace que el camino se vea entero. Una sola vez: la
    // primera en que hay algo que encuadrar.
    var encuadrado by remember { mutableStateOf(false) }
    LaunchedEffect(marcadores, trazos, zonas) {
        if (encuadrado || (seguirAlJugador && conPermiso)) return@LaunchedEffect
        val posiciones = marcadores.map { Position(it.longitud, it.latitud) } +
            trazos.flatMap { t -> t.tramos.flatten().map { (lat, lon) -> Position(lon, lat) } } +
            zonas.flatMap { z -> z.borde.map { (lat, lon) -> Position(lon, lat) } }
        if (posiciones.isEmpty()) return@LaunchedEffect
        encuadrado = true
        mapa.irA(posiciones, RELLENO_ENCUADRE, animado = false)
    }

    MaplibreMap(
        modifier = modifier,
        state = mapa,
        cameraConstraints = CameraConstraints(maxZoom = ZOOM_MAXIMO),
        // FR-040: sin rotación. El norte arriba es información —el rumbo a la patente se dice
        // en puntos cardinales (FR-003)— y el gesto de dos dedos se disparaba solo al hacer zoom.
        interactions = MapInteractions(MapInteractions.Standard) {
            camera { rotate { enabled = false } }
            // D9 de la 008: el toque que ninguna capa consumió —ni un pin ni una cuadra— es una
            // esquina de la zona que se está dibujando.
            if (onTocarMapa != null) {
                callbacks {
                    click {
                        onUnhandled { evento ->
                            val posicion = evento.position ?: return@onUnhandled ClickResult.Pass
                            onTocarMapa(posicion.latitude, posicion.longitude)
                            ClickResult.Consume
                        }
                    }
                }
            }
        },
        // Sin brújula (no hay rotación) ni escala: la franja de controles de la pantalla es
        // la única que habla.
        overlay = {},
    )
}

/**
 * Las avenidas y las autopistas resaltadas sobre `fiord`, como las dibuja `liberty` de día pero
 * en tonos apagados: de noche un amarillo pleno encandila.
 *
 * Usa las mismas teselas que el estilo, así que no descarga nada más y anda sin conexión donde el
 * fondo ande. Las clases y los anchos son los del estilo: el color tapa el relleno de la calle y
 * deja su borde. Sin túneles, que van por abajo.
 * ponytail: los dos colores se calibran mirando el teléfono de noche.
 */
@Composable
private fun CapasDeAvenidas() {
    // Null mientras el estilo no cargó, o si algún día deja de llamarse así: sin resaltado, nada más.
    val teselas = getBaseSource<VectorSource>("openmaptiles") ?: return

    fun deClase(vararg clases: String): Expression<BooleanValue> = switch(
        input = feature["brunnel"].convertToString(),
        case("tunnel", const(false)),
        fallback = switch(
            input = feature["class"].convertToString(),
            *clases.map { case(it, const(true)) }.toTypedArray(),
            fallback = const(false),
        ),
    )

    LineLayer(
        id = "avenidas",
        source = teselas,
        sourceLayer = "transportation",
        filter = deClase("primary", "trunk", "secondary", "tertiary"),
        color = const(colorDe(AVENIDA_OSCURO)),
        width = interpolate(exponential(1.3f), zoom(), 10 to const(2.dp), 20 to const(20.dp)),
        cap = const(LineCap.Round),
        join = const(LineJoin.Round),
    )
    LineLayer(
        id = "autopistas",
        source = teselas,
        sourceLayer = "transportation",
        filter = deClase("motorway"),
        color = const(colorDe(AUTOPISTA_OSCURO)),
        width = interpolate(exponential(1.4f), zoom(), 6 to const(1.3.dp), 20 to const(30.dp)),
        cap = const(LineCap.Round),
        join = const(LineJoin.Round),
    )
}

// Apagados a pedido del jugador: con el dorado pleno la vista se iba a las avenidas y no a los
// pines. Son el primer intento mezclado a la mitad con el gris de las calles del estilo.
private const val AVENIDA_OSCURO = "#6F654C"
private const val AUTOPISTA_OSCURO = "#836353"

private class Pines(
    val baja: PinPainter,
    val media: PinPainter,
    val alta: PinPainter,
    val tocaBaja: PinPainter,
    val tocaMedia: PinPainter,
    val tocaAlta: PinPainter,
)

/**
 * Las zonas (contrato Z4 de la 008): cada cuadra con el color de su clase, y el borde, continuo en
 * el mapa de una zona y punteado en la pantalla principal.
 *
 * Las cuadras se tocan solo si hay [onTocarCuadra]: en la pantalla principal un toque sobre una
 * cuadra pendiente tiene que seguir llegando al mapa.
 */
@Composable
private fun CapasDeZonas(
    cuadras: GeoJsonSource,
    bordes: GeoJsonSource,
    esquinas: GeoJsonSource,
    onTocarCuadra: ((Long) -> Unit)?,
) {
    val alTocar: FeaturesClickHandler? = onTocarCuadra?.let { tocar ->
        { tocadas ->
            val id = tocadas.firstNotNullOfOrNull { it.properties?.get(PROP_ID)?.jsonPrimitive?.longOrNull }
            if (id != null) {
                tocar(id)
                ClickResult.Consume
            } else {
                ClickResult.Pass
            }
        }
    }
    LineLayer(
        id = "zonas-cuadras",
        source = cuadras,
        color = switch(
            input = feature[PROP_CLASE].asString(),
            case(ClaseDeCuadra.RECORRIDA.name, const(colorDe(ColoresDeMapa.RECORRIDO))),
            case(ClaseDeCuadra.QUITADA.name, const(colorDe(ColoresDeMapa.QUITADA))),
            fallback = const(colorDe(ColoresDeMapa.PENDIENTE)),
        ),
        width = const(6.dp),
        opacity = switch(
            input = feature[PROP_CLASE].asString(),
            case(ClaseDeCuadra.QUITADA.name, const(0.5f)),
            fallback = const(0.85f),
        ),
        cap = const(LineCap.Round),
        join = const(LineJoin.Round),
        onClick = alTocar,
    )
    LineLayer(
        id = "zonas-borde-continuo",
        source = bordes,
        filter = feature[PROP_CONTINUO].asBoolean(),
        color = const(colorDe(ColoresDeMapa.BORDE)),
        width = const(2.dp),
    )
    LineLayer(
        id = "zonas-borde-punteado",
        source = bordes,
        filter = !feature[PROP_CONTINUO].asBoolean(),
        color = const(colorDe(ColoresDeMapa.BORDE)),
        width = const(2.dp),
        dasharray = const(listOf(3, 2)),
    )
    CircleLayer(
        id = "zonas-esquinas",
        source = esquinas,
        color = const(colorDe(ColoresDeMapa.BORDE)),
        radius = const(5.dp),
        strokeColor = const(Color.White),
        strokeWidth = const(1.5.dp),
    )
}

/**
 * La capa del camino recorrido (FR-006 de la 003, FR-007 de la 004).
 *
 * **Una sola capa para los tres modos**: el modo cambia la pintura y la visibilidad, no la
 * fuente (D4). Va **antes** que las patentes, y en MapLibre el orden de declaración es el orden
 * de dibujo: un trazo encima de un marcador taparía justo lo que se fue a buscar (C2).
 */
@Composable
private fun CapasDeTrazos(trazos: GeoJsonSource, huecos: GeoJsonSource, modo: ModoMapa) {
    val visible = modo != ModoMapa.APAGADO
    val color: Expression<ColorValue> = when (modo) {
        // FR-007: todas las salidas con el mismo color. Lo que tiene que leerse es el hueco.
        ModoMapa.COBERTURA, ModoMapa.APAGADO -> const(colorDe(ColoresDeMapa.RECORRIDO))
        ModoMapa.ANTIGUEDAD -> colorPorEscalon()
    }

    LineLayer(
        id = "recorridos-linea",
        source = trazos,
        visible = visible,
        color = color,
        width = const(5.dp),
        opacity = const(0.9f),
        cap = const(LineCap.Round),
        join = const(LineJoin.Round),
    )

    // FR-009a: el hueco de una desconexión, punteado. El punteado **es** el mensaje: una línea
    // continua ahí afirmaría un camino que la app no midió. No cambia de color con el modo.
    LineLayer(
        id = "recorridos-huecos-linea",
        source = huecos,
        visible = visible,
        color = const(colorDe(ColoresDeMapa.RECORRIDO)),
        width = const(2.dp),
        opacity = const(0.6f),
        dasharray = const(listOf(2, 3)),
    )
}

/**
 * El color de cada escalón de antigüedad (FR-012, FR-015).
 *
 * Por el nombre del escalón y no por su posición: el ordinal de un enum es un acoplamiento que
 * nadie ve hasta que alguien reordena las constantes. Tres colores netos y **nunca un
 * degradado**; el más viejo es el que más resalta, porque es lo que hay que ir a revisar.
 */
private fun colorPorEscalon(): Expression<ColorValue> = switch(
    input = feature[PROP_ESCALON],
    case(Escalon.RECIENTE.name, const(colorDe(ColoresDeMapa.ANTIGUEDAD_RECIENTE))),
    case(Escalon.MEDIO.name, const(colorDe(ColoresDeMapa.ANTIGUEDAD_MEDIA))),
    case(Escalon.VIEJO.name, const(colorDe(ColoresDeMapa.ANTIGUEDAD_VIEJA))),
    fallback = const(colorDe(ColoresDeMapa.ANTIGUEDAD_VIEJA)),
)

/**
 * Las capas de los marcadores (FR-028 a FR-032).
 *
 * El suelto es una sola capa de símbolo con el pin y el número. El grupo es un círculo con su
 * cuenta: la **forma** separa un grupo de una patente. **La de la que toca va última**, después
 * de los grupos (D1 de la 005): es lo que la dibuja encima de todo.
 */
@Composable
private fun CapasDePatentes(
    resto: GeoJsonSource,
    tocan: GeoJsonSource,
    pines: Pines,
    onTocar: (Long) -> Unit,
) {
    // T024 / FR-012: se consulta qué hay **dibujado** donde tocó el dedo, no cuál queda más cerca.
    val alTocar: FeaturesClickHandler =
        { tocadas ->
            val id = tocadas.firstNotNullOfOrNull { it.properties?.get(PROP_ID)?.jsonPrimitive?.longOrNull }
            if (id != null) {
                onTocar(id)
                ClickResult.Consume
            } else {
                ClickResult.Pass
            }
        }

    CapaDePines(
        id = "patentes-pines",
        fuente = resto,
        baja = pines.baja,
        media = pines.media,
        alta = pines.alta,
        escala = 1f,
        // FR-029: el número en negro sobre el relleno blanco.
        colorNumero = Color.Black,
        soloSueltos = true,
        alTocar = alTocar,
    )

    // FR-016: el grupo se dibuja más grande que un marcador suelto, para que se lea como
    // "acá hay varios" y no como una patente con un número raro.
    CircleLayer(
        id = "patentes-grupos",
        source = resto,
        filter = feature.has(PROP_CUENTA),
        radius = const(18.dp),
        strokeWidth = const(2.dp),
        strokeColor = const(Color.White),
        color = const(colorDe(ColoresDeMapa.GRUPO)),
    )

    SymbolLayer(
        id = "patentes-grupos-cuenta",
        source = resto,
        filter = feature.has(PROP_CUENTA),
        textField = format(span(feature[PROP_CUENTA].convertToString())),
        // **`textFont` es obligatorio**: sin declararlo MapLibre pide una tipografía que
        // OpenFreeMap no publica, y no se dibuja nada de la fuente. Fue el bug de los marcadores
        // invisibles de la 002.
        textFont = const(listOf("Noto Sans Regular")),
        textSize = const(13.sp),
        textColor = const(Color.White),
        textAllowOverlap = const(true),
        textIgnorePlacement = const(true),
    )

    // FR-001 y FR-002 de la 005: la que toca, verde, más grande y encima de todo.
    CapaDePines(
        id = "patentes-toca-pines",
        fuente = tocan,
        baja = pines.tocaBaja,
        media = pines.tocaMedia,
        alta = pines.tocaAlta,
        escala = PIN_TOCA,
        // Negro sobre ese verde queda con poco contraste.
        colorNumero = Color.White,
        soloSueltos = false,
        alTocar = alTocar,
    )
}

/**
 * Una capa de pines con su número (FR-028 a FR-030). Dos la usan: los pines blancos y los verdes
 * de la que toca.
 *
 * El borde del pin **es** la probabilidad, en tres escalones y no un degradado: 0 a 3 rojo, 4 a 7
 * amarillo, 8 a 10 verde (FR-030).
 */
@Composable
private fun CapaDePines(
    id: String,
    fuente: GeoJsonSource,
    baja: PinPainter,
    media: PinPainter,
    alta: PinPainter,
    escala: Float,
    colorNumero: Color,
    soloSueltos: Boolean,
    alTocar: FeaturesClickHandler,
) {
    SymbolLayer(
        id = id,
        source = fuente,
        filter = if (soloSueltos) !feature.has(PROP_CUENTA) else null,
        iconImage = step(
            input = feature[PROP_PROBABILIDAD].asNumber(),
            fallback = image(baja, TAMANO_PIN),
            4 to image(media, TAMANO_PIN),
            8 to image(alta, TAMANO_PIN),
        ),
        // FR-028: la punta cae sobre la coordenada. Un pin marca un punto, un círculo un área.
        iconAnchor = const(SymbolAnchor.Bottom),
        iconSize = const(escala),
        iconAllowOverlap = const(true),
        iconIgnorePlacement = const(true),
        textField = format(span(feature[PROP_NUMERO].asString())),
        textFont = const(listOf("Noto Sans Regular")),
        textSize = const((TEXTO_PIN * escala).sp),
        textColor = const(colorNumero),
        // El número va en la cabeza del pin. **El desplazamiento está en ems**, así el mismo
        // número sirve para el pin normal y para el agrandado (D2 de la 004).
        textAnchor = const(SymbolAnchor.Center),
        textOffset = textOffset(0f.em, (-DESPLAZAMIENTO_NUMERO).em),
        // Sin esto MapLibre esconde las etiquetas que colisionan, y un registro que desaparece
        // del mapa es peor que uno amontonado.
        textAllowOverlap = const(true),
        textIgnorePlacement = const(true),
        onClick = alTocar,
    )
}
