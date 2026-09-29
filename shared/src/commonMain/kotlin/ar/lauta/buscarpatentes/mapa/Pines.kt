package ar.lauta.buscarpatentes.mapa

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Qué medida tiene el pin, y de dónde salen los números.
 *
 * [DESPLAZAMIENTO_NUMERO] es la única que no se elige: es la distancia del centro de la cabeza a la
 * punta, dividida por el tamaño del texto, y está **en ems** a propósito. Al estar en ems escala
 * sola con el texto, así que el mismo número centra el número en el pin normal y en el agrandado
 * de la que toca (D2 de la 004).
 * ponytail: se calibran mirando el teléfono, no son constantes sagradas.
 */
internal const val PIN_ALTO_DP = 44f
internal const val PIN_BORDE_DP = 3f
internal const val TEXTO_PIN = 12f

/** Cuánto más grande es el pin de la patente que toca ahora (FR-001 de la 005). */
internal const val PIN_TOCA = 1.4f

/** Centro de la cabeza sobre la punta, en ems de [TEXTO_PIN]. */
internal const val DESPLAZAMIENTO_NUMERO = 2.25f

internal val TAMANO_PIN = DpSize(PIN_ANCHO_DP.dp, PIN_ALTO_DP.dp)

/**
 * El pin: relleno del color que se le pase, borde del otro, punta abajo (FR-028, FR-029).
 *
 * Hasta la 006 era un `Bitmap` del Android dibujado con `Canvas`. Ahora lo dibuja Compose, y
 * maplibre-compose lo registra en el estilo: el mismo pin en los dos teléfonos.
 *
 * Un solo `Path` y no un círculo más un triángulo: dos figuras superpuestas dejarían la línea
 * interna del borde cruzando la cabeza. El recorrido es punta, línea hasta el costado de la
 * cabeza, la vuelta entera de la cabeza, y `close` de vuelta a la punta.
 */
internal class PinPainter(private val borde: Color, private val relleno: Color) : Painter() {

    override val intrinsicSize: Size get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        val ancho = size.width
        val alto = size.height
        val grosor = PIN_BORDE_DP.dp.toPx()

        val radio = ancho / 2f - grosor / 2f
        val centroX = ancho / 2f
        val centroY = grosor / 2f + radio
        val cabeza = Rect(centroX - radio, centroY - radio, centroX + radio, centroY + radio)

        // 0° es a las 3 en punto y los grados crecen hacia abajo. La cola se engancha a los 55° y
        // a los 125°, y el arco da la vuelta larga por arriba: 290° en sentido antihorario.
        val figura = Path().apply {
            moveTo(centroX, alto - grosor / 2f)
            arcTo(cabeza, 55f, -290f, forceMoveTo = false)
            close()
        }

        drawPath(figura, relleno)
        drawPath(figura, borde, style = Stroke(width = grosor))
    }

    override fun equals(other: Any?): Boolean =
        other is PinPainter && other.borde == borde && other.relleno == relleno

    override fun hashCode(): Int = 31 * borde.hashCode() + relleno.hashCode()
}

/** `"#RRGGBB"` a color de Compose. Los colores del mapa viven como texto en `ColoresDeMapa`. */
internal fun colorDe(hex: String): Color = Color(("FF" + hex.removePrefix("#")).toLong(16))
