package ar.lauta.buscarpatentes.ui

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Los textos de fechas y números, iguales en los dos teléfonos (D16 de la 006).
 *
 * Hasta la 006 salían de `SimpleDateFormat` y `String.format`, que no existen en el iPhone. Los
 * meses van en una lista propia para que no dependan de cómo cada sistema abrevia en castellano:
 * son los que daba `SimpleDateFormat` con `Locale("es", "AR")`.
 */
private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic")

/** "5 sept 2026, 09:07": el momento de una captura o de una salida. */
fun fechaConAnio(millis: Long, zona: TimeZone = TimeZone.currentSystemDefault()): String =
    fecha(millis, zona, conAnio = true)

/** "5 sept 09:07": cuándo se instaló la app. */
fun fechaSinAnio(millis: Long, zona: TimeZone = TimeZone.currentSystemDefault()): String =
    fecha(millis, zona, conAnio = false)

@OptIn(ExperimentalTime::class)
private fun fecha(millis: Long, zona: TimeZone, conAnio: Boolean): String {
    val f = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zona)
    val hora = "${dosCifras(f.hour)}:${dosCifras(f.minute)}"
    return if (conAnio) "${f.day} ${MESES[f.month.ordinal]} ${f.year}, $hora"
    else "${f.day} ${MESES[f.month.ordinal]} $hora"
}

/** "09:07". */
@OptIn(ExperimentalTime::class)
fun hora(millis: Long, zona: TimeZone = TimeZone.currentSystemDefault()): String =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(zona).let { "${dosCifras(it.hour)}:${dosCifras(it.minute)}" }

private fun dosCifras(n: Int) = n.toString().padStart(2, '0')

/**
 * [valor] con [cifras] decimales, redondeado como `%.nf`, con [separador] como coma decimal.
 *
 * La coma es la del teléfono en castellano, para lo que se lee. Lo que se le pasa a otra app,
 * como las coordenadas de un recorrido de Maps, va con punto.
 */
fun decimales(valor: Double, cifras: Int, separador: Char = ','): String {
    val escala = 10.0.pow(cifras).toLong()
    val total = (abs(valor) * escala).roundToLong()
    val signo = if (valor < 0 && total != 0L) "-" else ""
    val parteEntera = total / escala
    if (cifras == 0) return "$signo$parteEntera"
    return "$signo$parteEntera$separador${(total % escala).toString().padStart(cifras, '0')}"
}

/** El número de una patente como se lee: tres cifras, con ceros a la izquierda. */
fun tresCifras(numero: Int): String = numero.toString().padStart(3, '0')
