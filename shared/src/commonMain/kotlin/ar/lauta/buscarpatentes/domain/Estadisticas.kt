package ar.lauta.buscarpatentes.domain

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** Las pestañas de las estadísticas (US5 de la 010). */
enum class TipoDePeriodo { DIA, SEMANA, MES }

/**
 * Un día, una semana de lunes a domingo o un mes del calendario, en la hora del teléfono
 * (contrato C4 de la 010).
 */
data class Periodo(val tipo: TipoDePeriodo, val inicio: LocalDate) {

    /** El primer día del período siguiente. */
    private val fin: LocalDate = mover(inicio, 1)

    fun anterior() = copy(inicio = mover(inicio, -1))

    fun siguiente() = copy(inicio = fin)

    /** False en el período actual: no se va a uno que todavía no pasó (FR-017). */
    fun haySiguiente(ahora: Long, zona: TimeZone) = inicio < de(tipo, ahora, zona).inicio

    fun contiene(millis: Long, zona: TimeZone): Boolean = fecha(millis, zona).let { it >= inicio && it < fin }

    private fun mover(desde: LocalDate, cuantos: Int): LocalDate = when (tipo) {
        TipoDePeriodo.DIA -> desde.plus(cuantos, DateTimeUnit.DAY)
        TipoDePeriodo.SEMANA -> desde.plus(cuantos * 7, DateTimeUnit.DAY)
        TipoDePeriodo.MES -> desde.plus(cuantos, DateTimeUnit.MONTH)
    }

    companion object {
        /** El período de [tipo] que contiene a [millis]. */
        fun de(tipo: TipoDePeriodo, millis: Long, zona: TimeZone): Periodo {
            val dia = fecha(millis, zona)
            return Periodo(
                tipo,
                when (tipo) {
                    TipoDePeriodo.DIA -> dia
                    TipoDePeriodo.SEMANA -> dia.minus(dia.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
                    TipoDePeriodo.MES -> LocalDate(dia.year, dia.month, 1)
                },
            )
        }

        @OptIn(ExperimentalTime::class)
        private fun fecha(millis: Long, zona: TimeZone): LocalDate =
            Instant.fromEpochMilliseconds(millis).toLocalDateTime(zona).date
    }
}

/** Lo que las estadísticas necesitan de una salida: cuándo empezó y su resumen. */
data class SalidaParaSumar(val iniciadoEn: Long, val caminata: ResumenDeCaminata)

data class TotalesDelPeriodo(
    val salidas: Int,
    val duracionMs: Long,
    val metros: Double,
    /** Null sin peso cargado (FR-009). */
    val kcal: Int?,
    val patentes: Int,
)

/**
 * Las sumas de un período (FR-016, FR-019 de la 010). Una salida cuenta entera en el período en
 * que empezó, y una patente en el que se anotó, esté o no en una salida. Nada se guarda: se suma
 * lo que ya calculó [Caminata], así los km de la semana son la suma de los de la lista (SC-006).
 */
object Estadisticas {

    fun totales(
        periodo: Periodo,
        salidas: List<SalidaParaSumar>,
        /** Cuándo se anotó cada patente. */
        patentes: List<Long>,
        conPeso: Boolean,
        zona: TimeZone,
    ): TotalesDelPeriodo {
        val adentro = salidas.filter { periodo.contiene(it.iniciadoEn, zona) }
        return TotalesDelPeriodo(
            salidas = adentro.size,
            duracionMs = adentro.sumOf { it.caminata.duracionMs },
            metros = adentro.sumOf { it.caminata.metros },
            kcal = if (conPeso) adentro.sumOf { it.caminata.kcal ?: 0 } else null,
            patentes = patentes.count { periodo.contiene(it, zona) },
        )
    }

    /** Los metros de los últimos 7 días, 8 semanas o 12 meses, terminando en [periodo] (FR-018). */
    fun barras(periodo: Periodo, salidas: List<SalidaParaSumar>, zona: TimeZone): List<Pair<Periodo, Double>> {
        val cuantas = when (periodo.tipo) {
            TipoDePeriodo.DIA -> 7
            TipoDePeriodo.SEMANA -> 8
            TipoDePeriodo.MES -> 12
        }
        return generateSequence(periodo) { it.anterior() }.take(cuantas).toList().reversed().map { p ->
            p to salidas.filter { p.contiene(it.iniciadoEn, zona) }.sumOf { it.caminata.metros }
        }
    }
}
