package ar.lauta.buscarpatentes.mapa

import ar.lauta.buscarpatentes.domain.Dibujo
import ar.lauta.buscarpatentes.domain.Escalon
import ar.lauta.buscarpatentes.domain.Probabilidad
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point

private fun Feature<Geometry, JsonObject>.texto(clave: String) = properties[clave]!!.jsonPrimitive.content
private fun Feature<Geometry, JsonObject>.entero(clave: String) = properties[clave]!!.jsonPrimitive.int
private fun Feature<Geometry, JsonObject>.largo(clave: String) = properties[clave]!!.jsonPrimitive.long

/**
 * El GeoJSON que se le entrega a MapLibre para dibujar los marcadores.
 *
 * Existe por el bug de los marcadores invisibles: en el dispositivo, la fuente reportaba
 * cero features con cinco marcadores en la mano, y no había forma de saber si el problema
 * estaba en los datos o en el dibujado. Esto responde esa mitad sin depender del teléfono.
 */
class ColeccionTest {

    private val muestra = listOf(
        Marcador(id = 1, numero = 313, latitud = -34.6039991, longitud = -58.3811085,
            toca = true, probabilidad = 9),
        Marcador(id = 2, numero = 7, latitud = -34.603991, longitud = -58.3811204,
            toca = false),
    )

    @Test
    fun `arma un feature por marcador`() {
        assertEquals(2, coleccion(muestra).features.size)
    }

    @Test
    fun `cada feature conserva coordenadas y numero`() {
        val features = coleccion(muestra).features

        val primero = features[0]
        assertEquals("313", primero.texto("numero"))
        assertEquals(1L, primero.largo("id"))

        // Orden GeoJSON: longitud primero. Invertirlo pondría las patentes en el mar.
        val punto = (primero.geometry as Point).coordinates
        assertEquals(-58.3811085, punto.longitude)
        assertEquals(-34.6039991, punto.latitude)

        // Si toca ya no viaja en el feature: desde la 005 decide la fuente, no la pintura.
        assertFalse("toca" in primero.properties)
        // Con ceros a la izquierda, como en la lista y en el aviso de proximidad.
        assertEquals("007", features[1].texto("numero"))
    }

    /**
     * D1 de la 005: la frontera que impide que la que toca quede adentro de un grupo (FR-002).
     * Si se colara en la primera colección, la fuente agrupada podría volver a esconderla.
     */
    @Test
    fun `la que toca va sola a su propia coleccion`() {
        val (resto, tocan) = coleccionesPatentes(muestra, zoom = 17)

        assertEquals(listOf(2L), resto.features.map { it.largo("id") })
        assertEquals(listOf(1L), tocan.features.map { it.largo("id") })
    }

    /**
     * US5 de la 005: el suelto se dibuja corrido pero sigue siendo su registro; el grupo es solo
     * una cuenta, sin número que mostrar ni ficha que abrir.
     */
    @Test
    fun `lo acomodado lleva la posicion de dibujo y el grupo solo su cuenta`() {
        val dibujos = listOf(
            Dibujo.Suelto(id = 2, latitud = -34.6041, longitud = -58.3812),
            Dibujo.Grupo(latitud = -34.5932, longitud = -58.3728, cuenta = 3),
        )
        val (suelto, grupo) = coleccionAcomodada(muestra, dibujos).features

        val punto = (suelto.geometry as Point).coordinates
        assertEquals(-58.3812, punto.longitude, 1e-9)
        assertEquals(-34.6041, punto.latitude, 1e-9)
        assertEquals("007", suelto.texto("numero"))
        assertEquals(Probabilidad.INICIAL, suelto.entero("probabilidad"))
        assertFalse("cuenta" in suelto.properties)

        assertEquals(3, grupo.entero("cuenta"))
        assertFalse("numero" in grupo.properties)
        assertFalse("id" in grupo.properties)
    }

    @Test
    fun `la probabilidad viaja en el feature y sin votos es la inicial`() {
        val features = coleccion(muestra).features

        assertEquals(9, features[0].entero("probabilidad"))
        // El segundo marcador no la declara: viaja la inicial, no un null que deje al
        // anillo sin imagen.
        assertEquals(Probabilidad.INICIAL, features[1].entero("probabilidad"))
    }
}

/**
 * El GeoJSON de los recorridos: los tramos que se dibujan y los huecos que se puntean.
 *
 * `Geo.tramos` ya tiene su prueba en `domain/GeoTest`, y lo que se cubre acá es lo otro: el
 * armado del GeoJSON. Es la mitad que en los marcadores falló —la fuente con cero features,
 * las coordenadas al revés— y la que no se ve mirando el teléfono, porque un trazo mal
 * armado se lee como "el GPS anduvo mal".
 */
class ColeccionTrazosTest {

    // Alrededor de 55 m entre puntos consecutivos: una caminata, muy por debajo del umbral
    // de corte de 250 m.
    private val caminata = listOf(
        -34.6032 to -58.3728,
        -34.6037 to -58.3728,
        -34.6042 to -58.3728,
    )

    /** Más de un kilómetro de salto: la desconexión que la salida del 29/08 destapó. */
    private val conCorte = listOf(
        -34.6032 to -58.3728,
        -34.6037 to -58.3728,
        -34.6132 to -58.3728,
        -34.6137 to -58.3728,
    )

    private fun coordenadas(coleccion: Coleccion, i: Int) =
        (coleccion.features[i].geometry as LineString).coordinates

    @Test
    fun `un LineString por recorrido no uno solo con todos los puntos`() {
        val trazos = listOf(
            Trazo(recorridoId = 1, puntos = caminata, escalon = Escalon.RECIENTE),
            Trazo(recorridoId = 2, puntos = caminata, escalon = Escalon.VIEJO),
        )

        // Uno solo cosería el final de una salida con el principio de la siguiente.
        assertEquals(2, coleccionTrazos(trazos).features.size)
    }

    @Test
    fun `orden GeoJSON longitud primero`() {
        val puntos = coordenadas(coleccionTrazos(listOf(Trazo(1, caminata, escalon = Escalon.VIEJO))), 0)

        // Invertirlo pondría el recorrido en el mar, que es exactamente el bug que este
        // archivo existe para atajar.
        assertEquals(-58.3728, puntos[0].longitude, 0.00001)
        assertEquals(-34.6032, puntos[0].latitude, 0.00001)
    }

    @Test
    fun `el escalon de antiguedad viaja en el feature`() {
        val trazos = listOf(
            Trazo(1, caminata, escalon = Escalon.RECIENTE),
            Trazo(2, caminata, escalon = Escalon.VIEJO),
        )
        val features = coleccionTrazos(trazos).features

        assertEquals("RECIENTE", features[0].texto("escalon"))
        assertEquals("VIEJO", features[1].texto("escalon"))
    }

    @Test
    fun `la coleccion conserva el orden de entrada`() {
        // D5: el orden **es** el mecanismo. Los trazos llegan de la salida más vieja a la más
        // nueva, y MapLibre dibuja en ese orden, así que la reciente queda encima donde se
        // superponen (FR-013).
        val trazos = listOf(
            Trazo(1, caminata, escalon = Escalon.VIEJO),
            Trazo(2, caminata, escalon = Escalon.MEDIO),
            Trazo(3, caminata, escalon = Escalon.RECIENTE),
        )
        val features = coleccionTrazos(trazos).features

        assertEquals(listOf("VIEJO", "MEDIO", "RECIENTE"), features.map { it.texto("escalon") })
    }

    @Test
    fun `un corte parte el recorrido en dos features y deja un hueco`() {
        val trazos = listOf(Trazo(1, conCorte, escalon = Escalon.RECIENTE))

        assertEquals(2, coleccionTrazos(trazos).features.size)
        assertEquals(2, coordenadas(coleccionTrazos(trazos), 0).size)
        assertEquals(1, coleccionHuecos(trazos).features.size)
    }

    @Test
    fun `el hueco va del final de un tramo al principio del siguiente`() {
        val hueco = coordenadas(coleccionHuecos(listOf(Trazo(1, conCorte, escalon = Escalon.VIEJO))), 0)

        assertEquals(2, hueco.size)
        assertEquals(-34.6037, hueco[0].latitude, 0.00001)
        assertEquals(-34.6132, hueco[1].latitude, 0.00001)
    }

    @Test
    fun `un recorrido continuo no deja ningun hueco`() {
        assertEquals(0, coleccionHuecos(listOf(Trazo(1, caminata, escalon = Escalon.VIEJO))).features.size)
    }

    @Test
    fun `un tramo de un solo punto se saltea porque no hay linea que dibujar`() {
        // Empezó y terminó sin moverse: un punto, ninguna línea (edge case de la spec).
        val solo = listOf(Trazo(1, listOf(-34.6032 to -58.3728), escalon = Escalon.VIEJO))
        assertEquals(0, coleccionTrazos(solo).features.size)

        // Y el punto suelto que queda después de un corte tampoco se dibuja, pero el hueco
        // hasta él sí: es la desconexión, y existió.
        val cortadoAlFinal = listOf(Trazo(1, conCorte.dropLast(1), escalon = Escalon.VIEJO))
        assertEquals(1, coleccionTrazos(cortadoAlFinal).features.size)
        assertEquals(1, coleccionHuecos(cortadoAlFinal).features.size)
    }

    @Test
    fun `un camino ajustado a las calles no se corta ni deja huecos`() {
        // El servicio ya resolvió por dónde se fue entre dos posiciones lejanas, siguiendo
        // calles. Cortarlo ahí puntearía un tramo que sí se conoce.
        val ajustado = listOf(Trazo(1, conCorte, escalon = Escalon.RECIENTE, ajustado = true))

        assertEquals(1, coleccionTrazos(ajustado).features.size)
        assertEquals(4, coordenadas(coleccionTrazos(ajustado), 0).size)
        assertEquals(0, coleccionHuecos(ajustado).features.size)
    }
}
