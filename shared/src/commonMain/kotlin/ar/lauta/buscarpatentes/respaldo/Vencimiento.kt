package ar.lauta.buscarpatentes.respaldo

import ar.lauta.buscarpatentes.plataforma.perfilDeAprovisionamiento
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Cuándo deja de abrir la app instalada con un Apple ID gratis (D15, P7 de la 006).
 *
 * La firma de Sideloadly dura 7 días. La fecha está en el perfil que viaja dentro de la app,
 * `embedded.mobileprovision`: un plist XML envuelto en una firma binaria. En el Android no hay
 * perfil, y todo esto da null.
 */
object Vencimiento {

    /** La línea de la pantalla principal aparece con 48 horas o menos (FR-011). */
    const val AVISO_MS = 48L * 60 * 60 * 1000

    private val FECHA = Regex("<key>ExpirationDate</key>\\s*<date>([^<]+)</date>")

    /** La de esta instalación, o null si no hay perfil. */
    val deEstaInstalacion: Long? by lazy { leer(perfilDeAprovisionamiento()) }

    /**
     * `ExpirationDate` del perfil, en epoch millis.
     *
     * Los bytes se leen como ISO-8859-1, uno a uno: la firma que envuelve al plist no es texto, y
     * decodificarla como UTF-8 podría comerse los caracteres de alrededor.
     */
    @OptIn(ExperimentalTime::class)
    fun leer(perfil: ByteArray?): Long? {
        if (perfil == null) return null
        val texto = CharArray(perfil.size) { (perfil[it].toInt() and 0xFF).toChar() }.concatToString()
        val fecha = FECHA.find(texto)?.groupValues?.get(1)?.trim() ?: return null
        return runCatching { Instant.parse(fecha).toEpochMilliseconds() }.getOrNull()
    }

    fun mostrarAviso(ahora: Long, vence: Long?): Boolean = vence != null && vence - ahora <= AVISO_MS
}
