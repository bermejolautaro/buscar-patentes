package ar.lauta.buscarpatentes.domain

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Distancia y rumbo en línea recta entre dos posiciones (FR-003, D3).
 *
 * ## Por qué existe, si la plataforma ya lo trae
 *
 * Android tiene `Location.distanceBetween`, y usarlo era el escalón más barato de escribir.
 * No es el más barato de tener: es un método estático de la plataforma, así que en una
 * prueba unitaria de JVM tira `RuntimeException: not mocked`. La única forma de cubrirlo
 * sería sumar Robolectric — una dependencia entera para no escribir estas quince líneas.
 *
 * Y la constitución exige prueba ejecutable para toda lógica no trivial. [cardinal] es
 * exactamente el caso: se rompe en los bordes —el cruce por 0°, el límite entre sectores—
 * sin que se note mirando el teléfono, porque "noreste" en vez de "norte" se lee igual de
 * plausible cuando uno no sabe la respuesta.
 *
 * ## Qué NO hace
 *
 * Línea recta, no calles. Esto no rutea: el ruteo lo hace la app de mapas del teléfono
 * (D2). Lo que esto contesta es "¿está lejos y para qué lado?", que es lo que se necesita
 * para decidir si vale la pena ir, y lo único honesto que se puede contestar sin red.
 */
object Geo {

    /** Radio medio de la Tierra, en metros. */
    private const val RADIO_TIERRA_M = 6_371_000.0

    /**
     * Distancia en metros entre dos posiciones, sobre la esfera.
     *
     * Haversine y no distancia euclídea sobre grados: a la latitud de Buenos Aires un grado
     * de longitud mide alrededor del 82% de uno de latitud, así que el error euclídeo no es
     * solo un error, es un error **direccional** —achica las distancias este-oeste— y por lo
     * tanto sesga hacia qué lado parece estar más cerca una patente.
     */
    fun distanciaMetros(
        latitudA: Double,
        longitudA: Double,
        latitudB: Double,
        longitudB: Double,
    ): Double {
        val dLat = (latitudB - latitudA).aRadianes()
        val dLon = (longitudB - longitudA).aRadianes()
        val latA = (latitudA).aRadianes()
        val latB = (latitudB).aRadianes()

        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(latA) * cos(latB) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * RADIO_TIERRA_M * atan2(sqrt(a), sqrt(1 - a))
    }

    /**
     * El candidato más cercano a una posición, si hay alguno dentro de [radioMetros].
     *
     * Es la mitad geométrica de "¿esta patente ya la tengo?": filtrar por número lo hace la
     * base, y acá se decide si lo que quedó está lo bastante cerca como para ser el mismo
     * auto y no otro con el mismo número a cinco cuadras.
     *
     * Genérico y con [posicionDe] porque el dominio no conoce las entidades de la base, y
     * porque así se prueba con pares sueltos, sin Room de por medio.
     *
     * Devuelve el más cercano y no el primero que entra en el radio: si hay dos capturas
     * viejas cerca, la que corresponde confirmar es la de al lado, no la de la esquina.
     */
    fun <T> masCercano(
        latitud: Double,
        longitud: Double,
        candidatos: List<T>,
        radioMetros: Double,
        posicionDe: (T) -> Pair<Double, Double>,
    ): T? = candidatos
        .minByOrNull { distanciaA(latitud, longitud, posicionDe(it)) }
        ?.takeIf { distanciaA(latitud, longitud, posicionDe(it)) <= radioMetros }

    private fun distanciaA(latitud: Double, longitud: Double, posicion: Pair<Double, Double>) =
        distanciaMetros(latitud, longitud, posicion.first, posicion.second)

    /**
     * Rumbo inicial de A hacia B, en grados desde el norte, siempre en `[0, 360)`.
     *
     * "Inicial" porque sobre una esfera el rumbo de un arco cambia a lo largo del camino.
     * A escala de barrio la diferencia es despreciable, pero el nombre no miente.
     */
    fun rumboGrados(
        latitudA: Double,
        longitudA: Double,
        latitudB: Double,
        longitudB: Double,
    ): Double {
        val latA = (latitudA).aRadianes()
        val latB = (latitudB).aRadianes()
        val dLon = (longitudB - longitudA).aRadianes()

        val y = sin(dLon) * cos(latB)
        val x = cos(latA) * sin(latB) - sin(latA) * cos(latB) * cos(dLon)

        // `atan2` devuelve `(-180, 180]`. El `+ 360` y el resto lo llevan a `[0, 360)`, que
        // es lo que `cardinal` espera: sin esto, todo rumbo hacia el oeste cae en el sector
        // equivocado.
        return (atan2(y, x).aGrados() + 360.0) % 360.0
    }

    private val PUNTOS = listOf("norte", "noreste", "este", "sudeste", "sur", "sudoeste", "oeste", "noroeste")

    /**
     * El punto cardinal de un rumbo, en ocho sectores de 45°.
     *
     * El norte es el sector **partido**: va de 337.5° a 22.5° pasando por 0°. Dividir por 45
     * y redondear resuelve eso solo —357° da índice 8, que el resto vuelve a 0— y es la razón
     * de que el `% 8` no sea decorativo.
     *
     * Acepta cualquier grado, incluso fuera de `[0, 360)`, y lo normaliza: así un llamador
     * que sume o reste grados no puede romperlo.
     */
    fun cardinal(grados: Double): String {
        val normalizado = ((grados % 360.0) + 360.0) % 360.0
        return PUNTOS[(normalizado / 45.0).roundToInt() % 8]
    }

    /**
     * Salto por encima del cual dos posiciones consecutivas **no** se unen (FR-009).
     *
     * El servicio de recorrido guarda una posición cada 5 segundos y cada 10 metros de
     * desplazamiento (FR-036), así que caminando los puntos consecutivos caen a la decena de
     * metros. Un salto de 250 m significa que se perdieron del orden de veinticinco
     * posiciones: no es una caminata, es una desconexión.
     *
     * **El umbral no bajó cuando bajó el muestreo, y es deliberado.** Lo que mide no es cada
     * cuánto llega un punto sino cuánto camino quedó sin conocer, y 250 m siguen siendo dos
     * cuadras y media de ciudad: por debajo de eso la recta entre dos puntos todavía se
     * parece a por dónde se fue. Lo que cambió es el margen — antes 250 m eran cuatro
     * posiciones perdidas y ahora son veinticinco, así que el corte es más tardío respecto
     * de lo que el teléfono podría haber medido.
     *
     * Se mide distancia y no tiempo a propósito. Con umbral de tiempo, pararse diez minutos
     * a tomar algo generaría un corte falso donde el jugador sí caminó los metros del medio.
     * Lo que falta en un corte no es tiempo, es **conocimiento del camino**, y eso lo mide la
     * distancia.
     *
     * ponytail: 250 m es calibrable. Si en uso real corta de más o de menos, se cambia acá.
     */
    const val SALTO_MAXIMO_M = 250.0

    /**
     * Parte un recorrido en tramos continuos, cortando donde hubo una desconexión (FR-009).
     *
     * La primera versión de esta feature unía todo sin cortar, para no tener que elegir un
     * umbral. En la calle eso "teletransportó" un recorrido hasta la casa del jugador: al
     * volver la señal, la recta entre la última posición conocida y la primera de la vuelta
     * cruzó manzanas por las que nunca pasó. El trazo afirmaba un camino falso, que es peor
     * que no afirmar ninguno.
     *
     * Devuelve los tramos en orden. Un recorrido sin cortes devuelve un solo tramo; una lista
     * vacía devuelve una lista vacía.
     */
    fun tramos(
        puntos: List<Pair<Double, Double>>,
        saltoMaximoM: Double = SALTO_MAXIMO_M,
    ): List<List<Pair<Double, Double>>> {
        if (puntos.isEmpty()) return emptyList()

        val resultado = mutableListOf<List<Pair<Double, Double>>>()
        var actual = mutableListOf(puntos.first())

        for (i in 1 until puntos.size) {
            val (latA, lonA) = puntos[i - 1]
            val (latB, lonB) = puntos[i]
            if (distanciaMetros(latA, lonA, latB, lonB) > saltoMaximoM) {
                resultado.add(actual)
                actual = mutableListOf()
            }
            actual.add(puntos[i])
        }
        resultado.add(actual)
        return resultado
    }

    /**
     * Los huecos entre tramos, como pares de extremos (FR-009a).
     *
     * Cada hueco se dibuja punteado: dice "acá hubo una desconexión y hay algún camino entre
     * estos dos puntos", sin afirmar cuál fue. Es la diferencia entre admitir que no se sabe
     * y inventar un recorrido.
     */
    fun huecos(
        tramos: List<List<Pair<Double, Double>>>,
    ): List<Pair<Pair<Double, Double>, Pair<Double, Double>>> =
        tramos.zipWithNext { anterior, siguiente ->
            anterior.last() to siguiente.first()
        }.filter { (a, b) -> a != b }

    /**
     * La distancia como la diría una persona: "120 m", "1,4 km".
     *
     * Bajo el kilómetro se redondea a decenas de metros, porque la precisión del GPS de un
     * teléfono no justifica decir "127 m" — y decirlo insinuaría una exactitud que el dato
     * no tiene, que es la misma clase de mentira que el Principio II prohíbe en la evidencia.
     */
    fun distanciaLegible(metros: Double): String = when {
        metros < 1_000 -> "${(metros / 10).roundToInt() * 10} m"
        else -> (metros / 100).roundToInt().let { d -> "${d / 10},${d % 10} km" }
    }

    /**
     * Lo que el FR-003 pide en pantalla: "A 320 m al noreste", o que no se sabe.
     *
     * Vive acá y no en cada pantalla porque tiene **dos llamadores reales** —la ficha de un
     * registro y cada fila de la lista de la barra —que en la 005 reemplazó a los resultados
     * de búsqueda—, que son los dos lugares donde el jugador se
     * encuentra con una patente (FR-002)— y porque su rama es justamente la que no puede
     * salir mal: sin posición actual se dice que no se sabe, **nunca un número inventado**
     * (FR-003a). Duplicada en dos pantallas, esa rama se arregla en una sola el día que
     * cambie.
     *
     * [desde] es dónde está parado el jugador ahora, o null si no hay permiso ni lectura.
     */
    fun distanciaYRumbo(desde: Pair<Double, Double>?, latitud: Double, longitud: Double): String {
        if (desde == null) return "Distancia desconocida: falta saber dónde estás."
        val (lat, lon) = desde
        val metros = distanciaMetros(lat, lon, latitud, longitud)
        val rumbo = rumboGrados(lat, lon, latitud, longitud)
        return "A ${distanciaLegible(metros)} al ${cardinal(rumbo)}"
    }
}

/**
 * `Math.toRadians` y `Math.toDegrees` de Java, que en código común no existen (D1 de la 006).
 * Multiplican por la misma constante que Java, así que dan el mismo resultado bit a bit.
 */
internal fun Double.aRadianes(): Double = this * (PI / 180.0)

internal fun Double.aGrados(): Double = this * (180.0 / PI)
