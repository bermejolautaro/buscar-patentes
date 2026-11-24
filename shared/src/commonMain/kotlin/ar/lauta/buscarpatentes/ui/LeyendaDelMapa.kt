package ar.lauta.buscarpatentes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.lauta.buscarpatentes.data.ModoMapa
import ar.lauta.buscarpatentes.domain.VistaDePatentes
import ar.lauta.buscarpatentes.mapa.colorDe

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
 * escondidas. Desde la 009, también desde qué número muestra (FR-004).
 */
@Composable
fun LeyendaDelMapa(
    modo: ModoMapa,
    vista: VistaDePatentes = VistaDePatentes.TODAS,
    /** El número que toca, o null mientras la base no contestó. */
    queToca: Int? = null,
    filtro: Int? = null,
    /** El nombre de la zona si el azul es solo lo de ella, o null si es todo lo caminado (FR-008 de la 013). */
    zonaDelRecorrido: String? = null,
    /** Con una zona elegida, tocar la leyenda cambia entre su recorrido y el de todos los tiempos. */
    onCambiarRecorrido: (() -> Unit)? = null,
    /** El zoom del mapa, para ajustar el agrupado (FR-010 de la 013). */
    zoom: Int? = null,
    modifier: Modifier = Modifier,
) {
    // Variante A del mockup de la 013: chips chicos en una sola línea, que tapan menos mapa que
    // la tarjeta de antes. El de la zona se toca para cambiar el recorrido.
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (modo) {
            ModoMapa.APAGADO -> Chip { Texto("Recorridos ocultos") }
            else -> {
                if (onCambiarRecorrido != null) {
                    Chip(onTocar = onCambiarRecorrido) {
                        Texto("⇄")
                        Text(
                            zonaDelRecorrido ?: "Todos los tiempos",
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 150.dp),
                        )
                    }
                } else if (modo == ModoMapa.COBERTURA) {
                    Chip { Texto("Todo lo caminado") }
                }
                if (modo == ModoMapa.ANTIGUEDAD) {
                    // FR-014a: los tres escalones con su color y su plazo. Los plazos van
                    // escritos y no insinuados: "hace poco" no es un dato, "3 días" sí.
                    Chip {
                        ChipDeEscalon(ColoresDeMapa.ANTIGUEDAD_RECIENTE, "≤3 d")
                        ChipDeEscalon(ColoresDeMapa.ANTIGUEDAD_MEDIA, "4-14 d")
                        ChipDeEscalon(ColoresDeMapa.ANTIGUEDAD_VIEJA, "+14 d")
                    }
                }
            }
        }

        if (zoom != null) Chip(suave = true) { Texto("z$zoom") }

        // Las patentes escondidas se dicen acá por la misma razón que los recorridos
        // apagados: un mapa sin pines y un mapa sin nada guardado se ven idénticos, y
        // esa confusión es exactamente la que hace peligroso al interruptor. Con la lupa
        // se ve un solo número, en cualquiera de las vistas que muestran (FR-006 de la 009).
        when {
            vista == VistaDePatentes.NINGUNA -> Chip(suave = true) { Texto("⦸ ocultas") }
            filtro != null -> Chip(suave = true) { Texto("Solo la ${tresCifras(filtro)}") }
            vista == VistaDePatentes.DESDE_LA_QUE_TOCA && queToca != null ->
                Chip(suave = true) { Texto("Desde la ${tresCifras(queToca)}") }
        }
    }
}

/** Una pastilla de la leyenda. Con [onTocar] lleva borde, para que se note que se toca. */
@Composable
private fun Chip(suave: Boolean = false, onTocar: (() -> Unit)? = null, contenido: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = if (suave) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(50),
        tonalElevation = 3.dp,
        border = if (onTocar != null) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        modifier = if (onTocar != null) Modifier.clickable(onClick = onTocar) else Modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { contenido() }
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
        Box(Modifier.size(9.dp).background(colorDe(color), CircleShape))
        Texto(plazo)
    }
}
