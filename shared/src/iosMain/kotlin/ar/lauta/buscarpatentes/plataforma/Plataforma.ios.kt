package ar.lauta.buscarpatentes.plataforma

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import ar.lauta.buscarpatentes.ui.Ir
import ar.lauta.buscarpatentes.ui.hora
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
// Con comodín: el POST y la foto usan métodos de categorías de Foundation, que Kotlin ve como
// extensiones.
import platform.Foundation.*
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject

// La costura del iPhone (contrato P de la 006).

/**
 * Un link de Google Maps, que abre la app si está instalada y si no Safari (D14). El iPhone no
 * tiene un equivalente de `geo:` que deje elegir la app de mapas.
 */
actual fun abrirPunto(latitud: Double, longitud: Double, etiqueta: String): Boolean =
    abrirUrl("https://www.google.com/maps/search/?api=1&query=$latitud,$longitud")

/**
 * Como en el Android: sin la app de Google Maps el recorrido va al navegador, donde entran menos
 * paradas, y si sobran no se abre (FR-019b). Saber si la app está necesita el esquema
 * `comgooglemaps` en `LSApplicationQueriesSchemes`.
 */
actual fun abrirRecorrido(url: String, paradas: Int): Ir.ResultadoRecorrido {
    val hayApp = NSURL.URLWithString("comgooglemaps://")?.let { UIApplication.sharedApplication.canOpenURL(it) } == true
    if (!hayApp && Ir.accionPara(paradas, Ir.MAXIMO_PARADAS_NAVEGADOR) is Ir.AccionRecorrido.Sobran) {
        return Ir.ResultadoRecorrido.DEMASIADAS_PARA_EL_NAVEGADOR
    }
    return if (abrirUrl(url)) Ir.ResultadoRecorrido.ABIERTO else Ir.ResultadoRecorrido.SIN_APP
}

private fun abrirUrl(url: String): Boolean {
    val destino = NSURL.URLWithString(url) ?: return false
    UIApplication.sharedApplication.openURL(destino, emptyMap<Any?, Any>(), null)
    return true
}

actual fun abrirAjustesDelSistema() {
    val ajustes = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
    UIApplication.sharedApplication.openURL(ajustes, emptyMap<Any?, Any>(), null)
}

/** La foto como imagen y no como archivo: WhatsApp la manda como foto, con el texto de epígrafe. */
actual fun compartir(texto: String, foto: String?) {
    mostrarHoja(listOfNotNull(texto, foto?.let { UIImage.imageWithContentsOfFile(it) }))
}

private fun mostrarHoja(cosas: List<Any>) {
    arriba()?.presentViewController(UIActivityViewController(cosas, null), animated = true, completion = null)
}

/**
 * Sin preguntarle al sistema: saberlo en el iPhone es asíncrono, y sin red `postear` falla en el
 * acto, sin gastar la espera. El ajuste a calles ya trata un error como "queda para después".
 */
actual fun hayConexion(): Boolean = true

@Suppress("CAST_NEVER_SUCCEEDS")
actual suspend fun postear(url: String, cuerpo: String): String? = suspendCancellableCoroutine { sigue ->
    val pedido = NSMutableURLRequest.requestWithURL(NSURL.URLWithString(url)!!).apply {
        setHTTPMethod("POST")
        setValue("application/json", forHTTPHeaderField = "Content-Type")
        setHTTPBody((cuerpo as NSString).dataUsingEncoding(NSUTF8StringEncoding))
        setTimeoutInterval(ESPERA_S)
    }
    val tarea = NSURLSession.sharedSession.dataTaskWithRequest(pedido) { datos, respuesta, error ->
        when {
            error != null -> sigue.resumeWithException(Exception(error.localizedDescription))
            (respuesta as? NSHTTPURLResponse)?.statusCode != 200L -> sigue.resume(null)
            else -> sigue.resume(datos?.let { NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString() })
        }
    }
    sigue.invokeOnCancellation { tarea.cancel() }
    tarea.resume()
}

private const val ESPERA_S = 20.0

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

actual fun mandarRespaldo(ruta: String) = mostrarHoja(listOf(NSURL.fileURLWithPath(ruta)))

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

/** La cámara del sistema. La foto se guarda en JPEG con el nombre que da `AlmacenFotos` (T058). */
@Composable
actual fun rememberSacarFoto(alTerminar: (Boolean) -> Unit): (destino: String) -> Unit {
    val terminar by rememberUpdatedState(alTerminar)
    // La cámara no retiene a su delegado: lo retiene esta pantalla.
    val delegado = remember { DelegadoDeLaCamara { terminar(it) } }
    return { destino ->
        val camara = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        if (!UIImagePickerController.isSourceTypeAvailable(camara)) {
            terminar(false)
        } else {
            delegado.destino = destino
            val selector = UIImagePickerController().apply {
                sourceType = camara
                delegate = delegado
            }
            arriba()?.presentViewController(selector, animated = true, completion = null)
        }
    }
}

private class DelegadoDeLaCamara(private val alTerminar: (Boolean) -> Unit) :
    NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    var destino: String? = null

    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        val imagen = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        val ruta = destino
        val guardada = imagen != null && ruta != null &&
            UIImageJPEGRepresentation(imagen, CALIDAD_JPEG)?.writeToFile(ruta, atomically = true) == true
        picker.dismissViewControllerAnimated(true, completion = null)
        alTerminar(guardada)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        alTerminar(false)
    }
}

/** Una patente se lee igual, y la foto pesa la mitad que sin comprimir. */
private const val CALIDAD_JPEG = 0.85
