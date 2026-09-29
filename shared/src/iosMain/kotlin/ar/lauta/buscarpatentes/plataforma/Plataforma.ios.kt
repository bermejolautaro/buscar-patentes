package ar.lauta.buscarpatentes.plataforma

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import ar.lauta.buscarpatentes.ui.Ir
import ar.lauta.buscarpatentes.ui.hora
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject

/*
 * La costura del iPhone (contrato P de la 006). Lo que todavía no existe tira NotImplementedError
 * con la story que lo implementa. Lo que las pantallas llaman solas, sin que el jugador lo pida,
 * no tira: no hace nada, así la app abre igual.
 */

actual object Grabacion {
    actual val enCurso: StateFlow<Long?> = MutableStateFlow(null)
    actual val puntosGuardados: StateFlow<Int> = MutableStateFlow(0)

    actual fun empezar(): Unit = throw NotImplementedError("US6 (T068): grabar la salida")

    actual fun terminar(): Unit = throw NotImplementedError("US6 (T068): grabar la salida")
}

actual object Vigilancia {
    // Se llama después de cada captura: sin avisos todavía, no vigila nada (US5, T062).
    actual suspend fun reconciliar(elSistemaLosOlvido: Boolean) {}
}

actual fun abrirPunto(latitud: Double, longitud: Double, etiqueta: String): Boolean =
    throw NotImplementedError("US4 (T060): abrir el lugar en mapas")

actual fun abrirRecorrido(url: String, paradas: Int): Ir.ResultadoRecorrido =
    throw NotImplementedError("US4 (T060): abrir el recorrido en Maps")

actual fun abrirAjustesDelSistema() {
    val ajustes = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
    UIApplication.sharedApplication.openURL(ajustes, emptyMap<Any?, Any>(), null)
}

actual fun compartir(texto: String, foto: String?): Unit = throw NotImplementedError("US4 (T059): compartir")

// Sin `postear` no hay a quién preguntarle: el ajuste a calles queda pendiente, como sin red (US6, T070).
actual fun hayConexion(): Boolean = false

actual suspend fun postear(url: String, cuerpo: String): String? = throw NotImplementedError("US6 (T070): postear")

actual val sistema: String = "ios"

actual fun perfilDeAprovisionamiento(): ByteArray? {
    val ruta = NSBundle.mainBundle.pathForResource("embedded", "mobileprovision") ?: return null
    return SystemFileSystem.source(Path(ruta)).buffered().use { it.readByteArray() }
}

/**
 * Pide permiso de notificaciones la primera vez: el aviso tiene que poder llegar aunque los avisos
 * de patentes estén apagados. El identificador fijo hace que cada llamada reemplace a la anterior.
 */
actual fun programarAvisoDeVencimiento(vence: Long) {
    val faltanSegundos = (vence - UN_DIA_MS - ahora()) / 1000.0
    if (faltanSegundos <= 0) return
    val centro = UNUserNotificationCenter.currentNotificationCenter()
    centro.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { dado, _ ->
        if (dado) {
            val contenido = UNMutableNotificationContent().apply {
                setTitle("Buscar patentes")
                setBody("La app vence mañana a las ${hora(vence)}. Reinstalala desde la PC.")
            }
            val cuando = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(faltanSegundos, repeats = false)
            centro.addNotificationRequest(UNNotificationRequest.requestWithIdentifier("vencimiento", contenido, cuando), null)
        }
    }
}

private const val UN_DIA_MS = 24L * 60 * 60 * 1000

actual fun mandarRespaldo(ruta: String) {
    val hoja = UIActivityViewController(listOf(NSURL.fileURLWithPath(ruta)), null)
    arriba()?.presentViewController(hoja, animated = true, completion = null)
}

/** `asCopy`: iOS copia el archivo adentro de la app, y lo que llega es una ruta local. */
@Composable
actual fun rememberElegirRespaldo(alElegir: (String?) -> Unit): () -> Unit {
    val elegir by rememberUpdatedState(alElegir)
    // El selector no retiene a su delegado: lo retiene esta pantalla.
    val delegado = remember { DelegadoDelSelector { elegir(it) } }
    return {
        val selector = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeData), asCopy = true)
        selector.delegate = delegado
        arriba()?.presentViewController(selector, animated = true, completion = null)
    }
}

private class DelegadoDelSelector(private val alElegir: (String?) -> Unit) :
    NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        alElegir((didPickDocumentsAtURLs.firstOrNull() as? NSURL)?.path)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        alElegir(null)
    }
}

/** La pantalla que está arriba de todo, que es desde donde se puede presentar otra. */
private fun arriba(): UIViewController? {
    var vc = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (vc?.presentedViewController != null) vc = vc.presentedViewController
    return vc
}

actual fun versionInstalada(): Pair<String, Long?> =
    (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "?") to null

@Composable
actual fun rememberSacarFoto(alTerminar: (Boolean) -> Unit): (destino: String) -> Unit =
    { throw NotImplementedError("US4 (T058): sacar foto") }
