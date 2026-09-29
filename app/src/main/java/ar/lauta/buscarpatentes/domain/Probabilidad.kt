package ar.lauta.buscarpatentes.domain

/**
 * Qué tan probable es que una patente siga estando donde se la vio.
 *
 * Es una escala de 0 a 10 que arranca en [INICIAL] —0— y sube cada vez que se la vuelve
 * a ver. Hasta la v7 arrancaba en [INICIAL_VIEJA], neutra, y el jugador tenía que bajarla
 * a mano; pero demostrar que un auto **no** está es difícil e incómodo, y volver a
 * anotarlo es trivial. Arrancar abajo hace que el número se construya con lo fácil: cada
 * captura repetida en el mismo lugar suma uno.
 *
 * Los votos se acumulan **uno por uno y con tope en cada paso**, no sumando todo y
 * recortando al final. La diferencia importa: diez "sigue estando" seguidos de un "ya no
 * estaba" tienen que bajar de 10 a 9, no quedarse en 10 hasta que el jugador vote nueve
 * veces en contra. Lo último que se vio pesa, que es justamente para lo que sirve esto.
 */
object Probabilidad {

    const val MINIMA = 0
    const val INICIAL = 0
    const val MAXIMA = 10

    /** Donde arrancaba la escala hasta la v7. Solo le sirve a la migración. */
    const val INICIAL_VIEJA = 5

    /**
     * [desde] es la `probabilidadInicial` del registro: [INICIAL] para todos, salvo los que
     * pasaron la v7 por encima de [INICIAL_VIEJA], que siguen contando desde ahí.
     */
    fun de(votos: List<Int>, desde: Int = INICIAL): Int =
        votos.fold(desde) { acumulada, voto -> (acumulada + voto).coerceIn(MINIMA, MAXIMA) }

    /**
     * v7: una patente conserva sus votos solo si la dejaban por encima de donde arrancaba
     * la escala vieja. Las demás vuelven a 0: se las vio, y nada más se sabe.
     */
    fun conservaSusVotos(votos: List<Int>): Boolean =
        de(votos, INICIAL_VIEJA) > INICIAL_VIEJA
}
