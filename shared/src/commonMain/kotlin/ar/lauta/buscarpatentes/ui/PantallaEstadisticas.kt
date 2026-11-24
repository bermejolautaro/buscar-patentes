package ar.lauta.buscarpatentes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.lauta.buscarpatentes.domain.Estadisticas
import ar.lauta.buscarpatentes.domain.Periodo
import ar.lauta.buscarpatentes.domain.SalidaParaSumar
import ar.lauta.buscarpatentes.domain.TipoDePeriodo
import ar.lauta.buscarpatentes.plataforma.ahora
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/**
 * Cuánto se caminó por día, semana y mes (US5 de la 010).
 *
 * Arranca en la semana actual: es la pregunta de todos los días ("¿cuánto caminé esta semana?").
 * Las barras comparan los km con los períodos anteriores, y tocar una lleva a ese período. Son
 * `Box` de alto proporcional: una librería de gráficos para cuatro rectángulos sería una
 * dependencia de más (D7).
 */
@Composable
fun PantallaEstadisticas(
    salidas: List<SalidaParaSumar>,
    /** Cuándo se anotó cada patente, esté o no en una salida (FR-019). */
    patentes: List<Long>,
    conPeso: Boolean,
) {
    val zona = remember { TimeZone.currentSystemDefault() }
    val ahora = remember { ahora() }
    var tipo by remember { mutableStateOf(TipoDePeriodo.SEMANA) }
    var periodo by remember(tipo) { mutableStateOf(Periodo.de(tipo, ahora, zona)) }
    val totales = remember(periodo, salidas, patentes) { Estadisticas.totales(periodo, salidas, patentes, conPeso, zona) }
    val barras = remember(periodo, salidas) { Estadisticas.barras(periodo, salidas, zona) }
    val actual = remember(tipo) { Periodo.de(tipo, ahora, zona) }

    Column(Modifier.padding(horizontal = 16.dp)) {
        PrimaryTabRow(selectedTabIndex = tipo.ordinal) {
            TipoDePeriodo.entries.forEach { t ->
                Tab(
                    selected = t == tipo,
                    onClick = { tipo = t },
                    text = {
                        Text(
                            when (t) {
                                TipoDePeriodo.DIA -> "Día"
                                TipoDePeriodo.SEMANA -> "Semana"
                                TipoDePeriodo.MES -> "Mes"
                            },
                        )
                    },
                )
            }
        }

        // FR-017: para atrás siempre; para adelante, hasta el actual.
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { periodo = periodo.anterior() }) { Text("‹", fontSize = 24.sp) }
            Text(titulo(periodo, actual), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { periodo = periodo.siguiente() }, enabled = periodo.haySiguiente(ahora, zona)) {
                Text("›", fontSize = 24.sp)
            }
        }

        // FR-016: los totales del período.
        Dato("Salidas", "${totales.salidas}")
        Dato("Tiempo caminando", tiempoDeSalida(totales.duracionMs))
        Dato("Distancia", kilometros(totales.metros))
        Dato("Calorías", totales.kcal?.let(::calorias) ?: "Cargá tu peso en Ajustes")
        Dato("Patentes anotadas", "${totales.patentes}")

        // FR-018: los km de los períodos anteriores, con el elegido resaltado.
        Text("Km", fontSize = 13.sp, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
        val maximo = barras.maxOf { it.second }.coerceAtLeast(1.0)
        Row(Modifier.fillMaxWidth().height(ALTO_BARRAS), verticalAlignment = Alignment.Bottom) {
            barras.forEach { (p, metros) ->
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable { periodo = p },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.6f)
                            .fillMaxHeight((metros / maximo).toFloat() * 0.85f)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = if (p == periodo) 1f else 0.35f),
                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                            ),
                    )
                    Text(etiqueta(p), fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun Dato(nombre: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(nombre)
        Text(valor, fontWeight = FontWeight.Bold)
    }
}

private val ALTO_BARRAS = 140.dp

private val MESES_LARGOS = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
)

private fun diaYMes(d: LocalDate) = "${d.day.toString().padStart(2, '0')}/${(d.month.ordinal + 1).toString().padStart(2, '0')}"

private fun titulo(p: Periodo, actual: Periodo): String = when (p.tipo) {
    TipoDePeriodo.DIA -> when (p) {
        actual -> "Hoy"
        actual.anterior() -> "Ayer"
        else -> diaYMes(p.inicio)
    }
    TipoDePeriodo.SEMANA -> if (p == actual) "Esta semana" else "Semana del ${diaYMes(p.inicio)}"
    TipoDePeriodo.MES -> "${MESES_LARGOS[p.inicio.month.ordinal]} ${p.inicio.year}"
}

private fun etiqueta(p: Periodo): String = when (p.tipo) {
    TipoDePeriodo.DIA -> "${p.inicio.day}"
    TipoDePeriodo.SEMANA -> diaYMes(p.inicio)
    TipoDePeriodo.MES -> MESES_LARGOS[p.inicio.month.ordinal].take(3).lowercase()
}
