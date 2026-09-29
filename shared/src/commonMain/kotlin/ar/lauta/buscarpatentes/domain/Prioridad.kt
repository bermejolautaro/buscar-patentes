package ar.lauta.buscarpatentes.domain

/**
 * En qué orden conviene ir a revisar las patentes de un número (D8 de la 005, FR-012).
 *
 * El jugador lo pidió así: *"más o menos weighted porque capaz una tiene confianza maso pero
 * está tan cerca que vale la pena chequearla"*. Ni solo distancia —mandaría a revisar primero
 * la de al lado aunque nunca se la haya vuelto a ver— ni solo confianza —mandaría a diez
 * cuadras por una que está confirmada cuando hay otra a media cuadra—.
 *
 * ## La cuenta
 *
 * ```
 * distancia efectiva = distancia / (1 + confianza / PESO_CONFIANZA)
 * ```
 *
 * **División y no resta.** Restar "tantos metros por punto de confianza" trata igual 50 m que
 * 5 km. Lo que el jugador describe es una proporción: cien metros de diferencia pesan mucho a
 * media cuadra y nada a diez. Con confianza 10 la distancia efectiva se divide por 3; con 5,
 * por 2. Eso además garantiza el SC-005 por construcción: una que está a más del doble de
 * distancia con igual o menos confianza nunca puede pasar adelante.
 *
 * Trabaja sobre [Candidato] y no sobre el registro de la base, por la misma razón que
 * [Geo.masCercano] recibe un selector: el dominio no conoce Room.
 */
object Prioridad {

    /**
     * Cuántos puntos de confianza hacen falta para dividir la distancia por dos.
     *
     * ponytail: se calibra en la calle (FR-012a). Hoy casi todas las patentes tienen confianza
     * 0 (FR-024), así que el orden va a ser casi solo por distancia hasta que se acumulen
     * confirmaciones. Si en la lista una muy confirmada queda detrás de una recién anotada que
     * no vale la pena, bajar este número; si pasa lo contrario, subirlo.
     */
    const val PESO_CONFIANZA = 5.0

    /**
     * Las patentes en el orden en que conviene revisarlas.
     *
     * [desde] es dónde está parado el jugador, o null si no se sabe. Sin posición no hay
     * distancia que pesar, así que ordena por confianza, y la pantalla lo dice (FR-012b).
     * A igualdad, primero la capturada más recientemente (FR-012c).
     */
    fun paraRevisar(candidatos: List<Candidato>, desde: Pair<Double, Double>?): List<Candidato> {
        val masRecienteAntes = compareByDescending<Candidato> { it.capturadoEn }
        if (desde == null) {
            return candidatos.sortedWith(
                compareByDescending<Candidato> { it.confianza }.then(masRecienteAntes),
            )
        }
        return candidatos.sortedWith(
            compareBy<Candidato> { distanciaEfectiva(desde, it) }.then(masRecienteAntes),
        )
    }

    /**
     * El orden propuesto para pasar por varias patentes (D9 de la 005, FR-015).
     *
     * Vecino más cercano: primero la más cercana a [desde], después cada vez la más cercana a
     * la anterior. No es el recorrido más corto posible, y no hace falta: un número tiene pocas
     * patentes, la diferencia son pocas cuadras, y el jugador lo corrige con las flechas si no
     * le sirve. A igualdad de distancia desempata el orden de [paraRevisar].
     *
     * No se llama `recorrido` para no confundirse con `data.Recorrido`, que es la salida grabada:
     * esto es a dónde ir, aquello es por dónde se fue.
     *
     * Sin posición no hay de dónde arrancar, así que devuelve el orden de revisión.
     */
    fun ordenDeParadas(candidatos: List<Candidato>, desde: Pair<Double, Double>?): List<Candidato> {
        val pendientes = paraRevisar(candidatos, desde).toMutableList()
        if (desde == null) return pendientes

        val orden = mutableListOf<Candidato>()
        var aca: Pair<Double, Double> = desde
        while (pendientes.isNotEmpty()) {
            // `minBy` se queda con la primera a igual distancia, y `pendientes` está en el
            // orden de revisión: ese es el desempate.
            val siguiente = pendientes.minBy {
                Geo.distanciaMetros(aca.first, aca.second, it.latitud, it.longitud)
            }
            orden += siguiente
            pendientes -= siguiente
            aca = siguiente.latitud to siguiente.longitud
        }
        return orden
    }

    private fun distanciaEfectiva(desde: Pair<Double, Double>, c: Candidato): Double =
        Geo.distanciaMetros(desde.first, desde.second, c.latitud, c.longitud) /
            (1 + c.confianza / PESO_CONFIANZA)
}

/** Lo mínimo de una patente para decidir en qué orden revisarla. */
data class Candidato(
    val id: Long,
    val latitud: Double,
    val longitud: Double,
    /** De 0 a 10, como [Probabilidad]. */
    val confianza: Int,
    /** Epoch millis de la captura. Desempata: primero la más reciente. */
    val capturadoEn: Long,
)
