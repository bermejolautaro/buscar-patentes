package ar.lauta.buscarpatentes.domain

/**
 * Cuántas patentes tiene el jugador del número que toca (FR-003 de la 005).
 *
 * Hasta la 004 este objeto contestaba otra cosa: si tenía la del número actual y **cuántos
 * números seguidos** tenía cubiertos desde ahí (FR-021 y FR-022 de la 001). El jugador no
 * sabía qué significaba "7 números seguidos cubiertos" y lo que quería saber era cuántas
 * tenía de la que busca, así que la 005 retiró las dos cuentas (FR-003a).
 *
 * La cuenta en sí es un `count` sobre los registros y la hace quien los tiene. Acá queda la
 * frase, que es la que tiene ramas.
 */
object Cobertura {

    /** "Ninguna descubierta", "1 descubierta" o "N descubiertas" (FR-003). */
    fun descubiertas(cuenta: Int): String = when (cuenta) {
        0 -> "Ninguna descubierta"
        1 -> "1 descubierta"
        else -> "$cuenta descubiertas"
    }
}
