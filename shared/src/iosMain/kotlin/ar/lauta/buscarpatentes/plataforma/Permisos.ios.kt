package ar.lauta.buscarpatentes.plataforma

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.launch
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter

actual fun tienePermiso(permiso: Permiso): Boolean = when (permiso) {
    Permiso.UBICACION -> Ubicacion.manager.authorizationStatus.let {
        it == kCLAuthorizationStatusAuthorizedWhenInUse || it == kCLAuthorizationStatusAuthorizedAlways
    }
    Permiso.UBICACION_SIEMPRE -> Ubicacion.manager.authorizationStatus == kCLAuthorizationStatusAuthorizedAlways
    Permiso.CAMARA -> AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized
    // iOS lo contesta solo de forma asincrónica. Pedirlo de nuevo no molesta: si ya se preguntó,
    // no muestra nada.
    Permiso.NOTIFICACIONES -> false
}

/**
 * Cada permiso se pide por su lado, en orden, y la respuesta se entrega junta cuando contestó el
 * último (D13 de la 006).
 */
@Composable
actual fun rememberPedirPermisos(alResponder: (Map<Permiso, Boolean>) -> Unit): (List<Permiso>) -> Unit {
    val responder by rememberUpdatedState(alResponder)
    val alcance = rememberCoroutineScope()
    return { lista -> alcance.launch { responder(lista.associateWith { pedir(it) }) } }
}

private suspend fun pedir(permiso: Permiso): Boolean = when (permiso) {
    Permiso.UBICACION -> Ubicacion.pedirMientrasSeUsa()
    Permiso.UBICACION_SIEMPRE -> Ubicacion.pedirSiempre()
    // Si ya se preguntó, iOS contesta enseguida con lo que el jugador eligió aquella vez.
    Permiso.NOTIFICACIONES -> suspendCoroutine { respuesta ->
        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { concedido, _ ->
                respuesta.resume(concedido)
            }
    }
    Permiso.CAMARA -> suspendCoroutine { respuesta ->
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { concedido -> respuesta.resume(concedido) }
    }
}
