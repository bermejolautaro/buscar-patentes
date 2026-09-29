package ar.lauta.buscarpatentes.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.lauta.buscarpatentes.recursos.Res
import ar.lauta.buscarpatentes.recursos.ic_camara
import ar.lauta.buscarpatentes.recursos.ic_detener
import ar.lauta.buscarpatentes.recursos.ic_play
import org.jetbrains.compose.resources.painterResource

/**
 * Los tres botones de la franja inferior (FR-036 de la 001, FR-018 y FR-019 de la 003).
 *
 * Los dos de captura **no son modos que haya que elegir antes de tipear**: son dos formas de
 * confirmar el mismo número. El jugador tipea primero y recién ahí decide si cierra con `+`
 * o con la cámara. Esa distinción es lo que mantiene la carga rápida dentro del techo del
 * Principio I —4 interacciones desde la constitución 1.1.0— y no en 5: un modo que hubiera
 * que elegir antes de tipear costaría el toque que ya no queda.
 *
 * ## Por qué es una extensión de `RowScope`
 *
 * Antes esto era su propia `Row` a lo ancho de la pantalla, con el campo de número en otra
 * fila arriba. Ahora los tres botones y el campo comparten una sola fila, así que la `Row`
 * la pone quien la compone y esto emite botones adentro. Es también lo que permite que el
 * campo se lleve el espacio sobrante con `weight`.
 *
 * ## El recorrido pasa a ser un icono
 *
 * Era un botón ancho con texto —"Empezar recorrido" / "Terminar recorrido"— que se comía
 * media franja para una acción que se usa dos veces por salida. Como círculo con play o
 * stop cuesta lo mismo de tocar y devuelve el ancho al campo.
 */
@Composable
fun RowScope.BotonesAccion(
    habilitados: Boolean,
    onGuardarRapido: () -> Unit,
    onGuardarConFoto: () -> Unit,
    onRecorrido: () -> Unit,
    recorridoEnCurso: Boolean = false,
) {
    // FR-019a: el contenido de un botón cuadrado necesita `contentPadding` en cero. El
    // relleno por defecto de `Button` está pensado para un botón ancho con texto, y en uno
    // de 52 dp se come el espacio y encoge lo que hay adentro.
    Button(
        onClick = onGuardarRapido,
        enabled = habilitados,
        modifier = Modifier.size(BOTON),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text("+", fontSize = 28.sp)
    }

    // FR-022: un icono de verdad, no el emoji 📷. El emoji lo dibujaba la fuente del
    // sistema, así que cambiaba de forma según el teléfono y no tomaba el color del tema.
    Button(
        onClick = onGuardarConFoto,
        enabled = habilitados,
        modifier = Modifier.size(BOTON),
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_camara),
            contentDescription = "Guardar con foto",
            modifier = Modifier.size(ICONO),
        )
    }

    // El mismo botón empieza y termina. Cambia el símbolo, no el lugar: el jugador aprende
    // un solo control. Tonal y no lleno porque no es la acción principal de la pantalla —
    // esa es cargar una patente.
    FilledTonalButton(
        onClick = onRecorrido,
        modifier = Modifier.size(BOTON),
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(
            painter = painterResource(
                if (recorridoEnCurso) Res.drawable.ic_detener else Res.drawable.ic_play,
            ),
            contentDescription = if (recorridoEnCurso) {
                "Terminar el recorrido"
            } else {
                "Empezar un recorrido"
            },
            modifier = Modifier.size(ICONO),
            tint = if (recorridoEnCurso) {
                // FR-023: mientras graba, el control dice que graba sin necesitar texto.
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
        )
    }
}

/**
 * Lado del botón, en dp.
 *
 * 52 está por encima del objetivo táctil mínimo de 48 dp que recomienda Android, que es el
 * piso que el FR-019 protege: achicar la franja no puede volver los botones más difíciles de
 * acertar caminando, que es cuando se usan.
 */
private val BOTON = 52.dp

/** El icono escala con el botón: en 24 dp quedaba minúsculo adentro de los 52. */
private val ICONO = 26.dp
