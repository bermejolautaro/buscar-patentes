package ar.lauta.buscarpatentes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.lauta.buscarpatentes.data.ModoMapa

/**
 * La línea que dice qué está mostrando el mapa (FR-004, FR-014a).
 *
 * **No es un adorno, y por eso está siempre.** Con el mapa sin ninguna salida guardada,
 * "los recorridos están apagados" y "no caminé nada todavía" se ven exactamente igual: el
 * mapa vacío no distingue una cosa de la otra, y el icono del botón tampoco alcanza para
 * distinguirlas de un vistazo. Esta línea es lo que hace viable que el interruptor cicle en
 * lugar de ofrecer tres opciones a la vez.
 *
 * En [ModoMapa.ANTIGUEDAD] es además la leyenda de la escala, con los plazos escritos: un
 * degradado sin leyenda no es un dato, es una decoración.
 *
 * Va sobre una superficie propia y no suelta sobre el mapa: es la misma razón por la que la
 * 003 le puso fondo al campo de número. Lo que se lee detrás cambia según por dónde ande el
 * jugador, y un texto chico sobre una avenida se lee distinto que sobre un parque.
 *
 * Desde la 005 dice también qué número filtra el mapa (FR-007): un mapa filtrado y un mapa con
 * pocas patentes guardadas se ven iguales, que es la misma confusión que la de las patentes
 * escondidas.
 */
@Composable
fun LeyendaDelMapa(
    modo: ModoMapa,
    patentesVisibles: Boolean = true,
    filtro: Int? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 3.dp,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (modo) {
                ModoMapa.COBERTURA -> Texto("Recorridos: todo lo caminado")
                ModoMapa.APAGADO -> Texto("Recorridos ocultos")
                ModoMapa.ANTIGUEDAD -> {
                    // FR-014a: los tres escalones con su color y su plazo. Los plazos van
                    // escritos y no insinuados: "hace poco" no es un dato, "3 días" sí.
                    Texto("Hace")
                    ChipDeEscalon(ColoresDeMapa.ANTIGUEDAD_RECIENTE, "≤3 d")
                    ChipDeEscalon(ColoresDeMapa.ANTIGUEDAD_MEDIA, "4-14 d")
                    ChipDeEscalon(ColoresDeMapa.ANTIGUEDAD_VIEJA, "+14 d")
                }
            }

            // Las patentes escondidas se dicen acá por la misma razón que los recorridos
            // apagados: un mapa sin pines y un mapa sin nada guardado se ven idénticos, y
            // esa confusión es exactamente la que hace peligroso al interruptor.
            if (filtro != null) Texto("· Solo la %03d".format(filtro))
            if (!patentesVisibles) Texto("· Patentes ocultas")
        }
    }
}

@Composable
private fun Texto(texto: String) = Text(texto, fontSize = 12.sp)

/** Un punto del color del escalón y su plazo al lado. */
@Composable
private fun ChipDeEscalon(color: String, plazo: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Los colores del mapa son hex, porque MapLibre los quiere así. Acá hay que
        // traducirlos una vez para que Compose los pinte.
        Box(Modifier.size(9.dp).background(Color(android.graphics.Color.parseColor(color)), CircleShape))
        Texto(plazo)
    }
}
