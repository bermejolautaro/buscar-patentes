package ar.lauta.buscarpatentes.mapa

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Qué medida tiene el pin, y de dónde salen los números.
 *
 * Desde la 013 el número va adentro de la imagen del pin ([PinConNumero]), del tamaño
 * proporcional al pin.
 * ponytail: se calibran mirando el teléfono, no son constantes sagradas.
 */
internal const val PIN_ALTO_DP = 44f
internal const val PIN_BORDE_DP = 3f
internal const val TEXTO_PIN = 12f

/** Cuánto más grande es el pin de la patente que toca ahora (FR-001 de la 005). */
internal const val PIN_TOCA = 1.4f

/**
 * El tamaño del pin con su número según el zoom: lineal entre estos dos, y fijo por fuera
 * (013). Lejos se achica para que el montón se lea; cerca vuelve al tamaño de siempre.
 * ponytail: los del mockup (`mockups/marcadores.html`). Se calibran mirando el teléfono.
 */
internal const val TAMANO_A_ZOOM_13 = 0.6f
internal const val TAMANO_A_ZOOM_17 = 1f

/** El tamaño de [tamanoPinA] en un zoom entero, para que el acomodo mida el pin que se dibuja. */
internal fun tamanoPin(zoom: Int): Float =
    TAMANO_A_ZOOM_13 + (TAMANO_A_ZOOM_17 - TAMANO_A_ZOOM_13) * ((zoom - 13) / 4f).coerceIn(0f, 1f)

/** Los tres escalones del borde: 0 a 3, 4 a 7 y 8 a 10 (FR-030). */
internal fun escalonDeProbabilidad(probabilidad: Int): Int = when {
    probabilidad <= 3 -> 0
    probabilidad <= 7 -> 1
    else -> 2
}

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

/**
 * El pin con su número adentro (013).
 *
 * Hasta la 013 el número era un texto aparte de la misma capa. El mapa dibuja primero todos los
 * íconos y después todos los textos, así que el número de un pin de atrás quedaba encima del pin
 * de adelante, y en un montón parecía que se atravesaban. Con el número en la imagen, pin y número
 * son una sola cosa: se tapan en orden y crecen juntos con el zoom.
 */
internal class PinConNumero(
    private val borde: Color,
    private val relleno: Color,
    private val numero: String,
    private val colorNumero: Color,
    private val medidor: TextMeasurer,
) : Painter() {

    private val pin = PinPainter(borde, relleno)

    override val intrinsicSize: Size get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        with(pin) { draw(size) }
        val grosor = PIN_BORDE_DP.dp.toPx()
        val radio = size.width / 2f - grosor / 2f
        val centroY = grosor / 2f + radio
        // El tamaño del texto sigue al del pin: el mismo pin registrado más grande lleva el número
        // más grande.
        val tamano = (TEXTO_PIN * size.width / PIN_ANCHO_DP.dp.toPx()).sp
        val medido = medidor.measure(numero, TextStyle(color = colorNumero, fontSize = tamano), density = this)
        drawText(
            medido,
            topLeft = androidx.compose.ui.geometry.Offset(
                size.width / 2f - medido.size.width / 2f,
                centroY - medido.size.height / 2f,
            ),
        )
    }

    override fun equals(other: Any?): Boolean =
        other is PinConNumero && other.borde == borde && other.relleno == relleno &&
            other.numero == numero && other.colorNumero == colorNumero

    override fun hashCode(): Int = listOf(borde, relleno, numero, colorNumero).hashCode()
}

/** `"#RRGGBB"` a color de Compose. Los colores del mapa viven como texto en `ColoresDeMapa`. */
internal fun colorDe(hex: String): Color = Color(("FF" + hex.removePrefix("#")).toLong(16))
