package ar.lauta.buscarpatentes.plataforma

import androidx.compose.runtime.Composable
import ar.lauta.buscarpatentes.ubicacion.LecturaUbicacion
import ar.lauta.buscarpatentes.ui.Ir
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.StateFlow

/**
 * La costura con cada sistema (contrato P de la 006, D9).
 *
 * Una pantalla común no importa nada de `android.*` ni de `platform.*`: si necesita algo del
 * sistema, pasa por acá. Cada declaración tiene exactamente dos implementaciones, y la del
 * Android es el código de siempre, mudado.
 */

/** La hora, en milisegundos desde 1970. Es la de `System.currentTimeMillis()`. */
@OptIn(ExperimentalTime::class)
fun ahora(): Long = Clock.System.now().toEpochMilliseconds()

/** P2: una lectura de alta precisión para la captura, con tiempo de espera. */
expect object Ubicacion {
    suspend fun leerAhora(): LecturaUbicacion
}

/**
 * P2: false solo si el jugador apagó la ubicación exacta, que el iPhone deja apagar por app
 * (FR-024). El Android no tiene ese interruptor.
 */
expect fun ubicacionExacta(): Boolean

/** P3: la salida que se graba con la pantalla apagada. */
expect object Grabacion {
    /**
     * El recorrido en curso, o null.
     *
     * Vive en memoria y no en la base porque si el proceso muere la grabación muere con él: no
     * hay estado que recuperar, hay un recorrido que cerrar.
     */
    val enCurso: StateFlow<Long?>

    /** Sube con cada punto que se escribe, para que el mapa lo dibuje mientras se camina (FR-008). */
    val puntosGuardados: StateFlow<Int>

    fun empezar()

    fun terminar()
}

/** P4: los avisos al pasar cerca de la que toca. */
expect object Vigilancia {
    /**
     * Deja vigiladas exactamente las patentes que el estado del juego dicta. Sin el permiso
     * "siempre" no vigila nada, y no rompe.
     *
     * [elSistemaLosOlvido] va en true cuando el proceso viene de arrancar: el sistema puede no
     * conservar lo que se había registrado.
     */
    suspend fun reconciliar(elSistemaLosOlvido: Boolean = false)
}

/** P6: abre un punto en la app de mapas. False si no hay ninguna que lo reciba. */
expect fun abrirPunto(latitud: Double, longitud: Double, etiqueta: String): Boolean

/** P6: abre la Maps URL de un recorrido de [paradas] paradas (C4 de la 005). */
expect fun abrirRecorrido(url: String, paradas: Int): Ir.ResultadoRecorrido

/** P6: los ajustes de la app en el sistema, donde se dan los permisos que no se piden desde acá. */
expect fun abrirAjustesDelSistema()

/** P5: la hoja de compartir del sistema, con el texto y, si hay, la foto. */
expect fun compartir(texto: String, foto: String?)

/** P6: si hay una red que llegue a internet. Evita gastar un tiempo de espera para saberlo. */
expect fun hayConexion(): Boolean

/**
 * P6: un POST de JSON, con 20 s de espera. El cuerpo de la respuesta, o null si no fue un 200.
 * Tira si no hubo respuesta.
 */
expect suspend fun postear(url: String, cuerpo: String): String?

/** De qué sistema sale un respaldo (R2): `android` o `ios`. */
expect val sistema: String

/** P6: los bytes de `embedded.mobileprovision`, que dicen cuándo vence la app. En el Android, null. */
expect fun perfilDeAprovisionamiento(): ByteArray?

/**
 * P6: la notificación de "vence mañana", 24 horas antes de [vence] (epoch ms). Reemplaza la
 * anterior, y si ese momento ya pasó no programa nada (FR-012). En el Android, nada.
 */
expect fun programarAvisoDeVencimiento(vence: Long)

/** P5: la hoja de compartir del sistema con el archivo de un respaldo, para sacarlo del teléfono. */
expect fun mandarRespaldo(ruta: String)

/**
 * P5: devuelve la función que deja elegir un archivo. Cuando se elige, llama a [alElegir] con la
 * ruta de una copia local; si se cancela, con null.
 */
@Composable
expect fun rememberElegirRespaldo(alElegir: (String?) -> Unit): () -> Unit

/** La versión instalada y cuándo se instaló, en milisegundos, para el pie de Ajustes. */
expect fun versionInstalada(): Pair<String, Long?>

/**
 * P5: devuelve la función que abre la cámara y escribe el JPEG en la ruta que recibe. Cuando se
 * cierra la cámara, llama a [alTerminar] con true si la foto quedó escrita.
 */
@Composable
expect fun rememberSacarFoto(alTerminar: (Boolean) -> Unit): (destino: String) -> Unit
