package ar.lauta.buscarpatentes.domain

/** Formatos de patente argentinos vigentes (FR-010). */
enum class FormatoPatente { VIEJO, MERCOSUR, DESCONOCIDO }

/**
 * Una patente entendida por la app.
 *
 * [numero] es lo único que le importa al juego: la parte numérica de 3 dígitos.
 * [texto] conserva la patente completa cuando el jugador la cargó entera; es null si
 * solo tipeó el número, que es el caso normal de la carga rápida (FR-017).
 */
data class PatenteParseada(
    val numero: Int,
    val texto: String?,
    val formato: FormatoPatente,
)

/**
 * Extracción del número de 3 dígitos desde cualquiera de las dos formas argentinas.
 *
 * - Viejo:     `AAA 123`  → 3 letras + 3 dígitos
 * - Mercosur:  `AB 123 CD` → 2 letras + 3 dígitos + 2 letras
 * - Solo el número: `313` → lo que se tipea en la carga rápida
 *
 * Devuelve null si la entrada no es ninguna de las tres. Nunca adivina.
 */
object Patente {

    private val SOLO_NUMERO = Regex("^([0-9]{3})$")
    private val VIEJO = Regex("^([A-Z]{3})([0-9]{3})$")
    private val MERCOSUR = Regex("^([A-Z]{2})([0-9]{3})([A-Z]{2})$")

    fun parsear(entrada: String): PatenteParseada? {
        val limpio = entrada.uppercase().filter { it.isLetterOrDigit() }

        SOLO_NUMERO.matchEntire(limpio)?.let {
            return PatenteParseada(
                numero = it.groupValues[1].toInt(),
                texto = null,
                formato = FormatoPatente.DESCONOCIDO,
            )
        }

        VIEJO.matchEntire(limpio)?.let {
            return PatenteParseada(
                numero = it.groupValues[2].toInt(),
                texto = limpio,
                formato = FormatoPatente.VIEJO,
            )
        }

        MERCOSUR.matchEntire(limpio)?.let {
            return PatenteParseada(
                numero = it.groupValues[2].toInt(),
                texto = limpio,
                formato = FormatoPatente.MERCOSUR,
            )
        }

        return null
    }
}
