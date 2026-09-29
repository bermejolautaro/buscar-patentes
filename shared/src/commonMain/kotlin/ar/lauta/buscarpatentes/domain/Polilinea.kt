package ar.lauta.buscarpatentes.domain

/**
 * Decodifica la polilínea codificada que devuelve el servicio de map matching (FR-031).
 *
 * Es el formato clásico de Google —diferencias en zigzag, base 64, en trozos de 5 bits— con
 * una diferencia que importa: **Valhalla codifica a 1e6 y no a 1e5**. Con el factor
 * equivocado el camino sale en el lugar correcto pero diez veces más chico, un garabato
 * apretado sobre una manzana. Por eso el factor es un parámetro y no una constante escondida.
 *
 * Vive en `domain/` porque es aritmética pura sobre un `String`: sin Android, sin red, y
 * testeable en JVM, que es lo que la constitución exige para algo que se rompe en silencio.
 */
object Polilinea {

    /** Valhalla codifica con seis decimales. OSRM y Google, con cinco. */
    const val PRECISION_VALHALLA = 1e6

    /**
     * Separa las piernas de un mismo camino guardado.
     *
     * **Dos polilíneas codificadas no se pueden pegar una atrás de la otra.** Cada una es un
     * flujo de diferencias acumuladas **desde cero**, así que la segunda vuelve a arrancar en
     * el origen: decodificar la concatenación pone sus puntos en la suma de las dos
     * posiciones, que cae fuera del planeta. Hay que decodificar cada una por su lado y
     * concatenar los **puntos**, que es lo que hace [decodificar].
     *
     * El punto y coma no puede aparecer adentro de una polilínea: el formato solo usa
     * caracteres de 63 (`?`) para arriba, y este es el 59. Por lo mismo un camino guardado de
     * una sola pierna —todos los que ya están en la base— se decodifica igual que antes.
     */
    const val SEPARADOR_PIERNAS = ";"

    /**
     * Devuelve los puntos como pares de latitud y longitud, en orden.
     *
     * Una cadena vacía devuelve una lista vacía. Una cadena corrupta devuelve lo que alcanzó
     * a decodificar en lugar de tirar una excepción: un camino a medias sobre el mapa es
     * mejor que una pantalla que se cae, y el trazo crudo sigue estando abajo como respaldo.
     *
     * Si la cadena trae varias piernas separadas por [SEPARADOR_PIERNAS], se decodifica cada
     * una por su lado y se pegan los puntos resultantes.
     */
    fun decodificar(codificada: String, precision: Double = PRECISION_VALHALLA):
        List<Pair<Double, Double>> =
        codificada.split(SEPARADOR_PIERNAS).flatMap { decodificarUna(it, precision) }

    /** Una sola pierna: el formato clásico, diferencias acumuladas desde cero. */
    private fun decodificarUna(codificada: String, precision: Double):
        List<Pair<Double, Double>> {
        val puntos = mutableListOf<Pair<Double, Double>>()
        var indice = 0
        var lat = 0
        var lon = 0

        while (indice < codificada.length) {
            val dLat = siguienteValor(codificada, indice) ?: break
            indice = dLat.second
            lat += dLat.first

            val dLon = siguienteValor(codificada, indice) ?: break
            indice = dLon.second
            lon += dLon.first

            puntos.add(lat / precision to lon / precision)
        }
        return puntos
    }

    /**
     * Lee un entero del flujo y devuelve el valor junto con el índice siguiente.
     *
     * Cada carácter aporta 5 bits; el sexto bit dice si hay continuación. El valor viene en
     * zigzag —el bit menos significativo es el signo— porque así los números negativos chicos
     * ocupan poco, que es todo el punto del formato.
     *
     * Devuelve null si la cadena se corta en el medio de un número.
     */
    private fun siguienteValor(s: String, desde: Int): Pair<Int, Int>? {
        var indice = desde
        var desplazamiento = 0
        var acumulado = 0

        while (indice < s.length) {
            val b = s[indice].code - 63
            indice++
            acumulado = acumulado or ((b and 0x1f) shl desplazamiento)
            if (b < 0x20) {
                val valor = if (acumulado and 1 != 0) (acumulado shr 1).inv() else acumulado shr 1
                return valor to indice
            }
            desplazamiento += 5
        }
        return null
    }
}
