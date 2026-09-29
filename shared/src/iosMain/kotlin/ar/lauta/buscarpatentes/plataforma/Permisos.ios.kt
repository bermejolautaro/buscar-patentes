package ar.lauta.buscarpatentes.plataforma

import androidx.compose.runtime.Composable
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse

actual fun tienePermiso(permiso: Permiso): Boolean = when (permiso) {
    Permiso.UBICACION -> CLLocationManager().authorizationStatus.let {
        it == kCLAuthorizationStatusAuthorizedWhenInUse || it == kCLAuthorizationStatusAuthorizedAlways
    }
    Permiso.UBICACION_SIEMPRE -> CLLocationManager().authorizationStatus == kCLAuthorizationStatusAuthorizedAlways
    Permiso.CAMARA -> AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized
    // iOS lo contesta solo de forma asincrónica. Lo implementa la US1 (T039).
    Permiso.NOTIFICACIONES -> false
}

@Composable
actual fun rememberPedirPermisos(alResponder: (Map<Permiso, Boolean>) -> Unit): (List<Permiso>) -> Unit =
    { throw NotImplementedError("US1 (T039): pedir permisos en el iPhone") }
