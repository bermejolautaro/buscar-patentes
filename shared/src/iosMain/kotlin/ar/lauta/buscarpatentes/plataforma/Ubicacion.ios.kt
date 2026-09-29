@file:OptIn(ExperimentalForeignApi::class)

package ar.lauta.buscarpatentes.plataforma

import ar.lauta.buscarpatentes.ubicacion.Lectura
import ar.lauta.buscarpatentes.ubicacion.LecturaUbicacion
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLAccuracyAuthorization
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSinceNow
import platform.darwin.NSObject

/**
 * La ubicación del iPhone para la captura (P2, D10 de la 006).
 *
 * Pide posiciones seguidas hasta que llega una de ahora, y corta: `requestLocation` puede
 * entregar una guardada, y guardarla con la hora de ahora sería el `getLastLocation()` que el
 * Principio II prohíbe. Su propio `CLLocationManager`: apagarlo al terminar no toca ni la salida
 * ni los avisos, que usan el suyo.
 */
actual object Ubicacion {

    private val esperandoPosicion = mutableListOf<CancellableContinuation<CLLocation?>>()
    private val esperandoPermiso = mutableListOf<CancellableContinuation<Unit>>()

    private val delegado = Delegado()

    /** Se crea en el hilo principal, que es donde Core Location entrega lo que mide. */
    internal val manager: CLLocationManager by lazy {
        CLLocationManager().apply {
            delegate = delegado
            desiredAccuracy = kCLLocationAccuracyBest
        }
    }

    actual suspend fun leerAhora(): LecturaUbicacion = withContext(Dispatchers.Main) {
        if (!tienePermiso(Permiso.UBICACION)) return@withContext LecturaUbicacion.SinPermiso

        val posicion = withTimeoutOrNull(Lectura.PRESUPUESTO_MS) {
            suspendCancellableCoroutine<CLLocation?> { espera ->
                esperandoPosicion += espera
                espera.invokeOnCancellation { soltar(espera) }
                manager.startUpdatingLocation()
            }
        } ?: return@withContext LecturaUbicacion.SinLectura

        posicion.coordinate.useContents {
            LecturaUbicacion.Ok(
                Lectura(
                    latitud = latitude,
                    longitud = longitude,
                    precisionMetros = posicion.horizontalAccuracy.toFloat(),
                    tomada = TimeSource.Monotonic.markNow() - edadSegundos(posicion).seconds,
                ),
            )
        }
    }

    /**
     * Pide ubicación "mientras se usa" si todavía no se preguntó, y espera la respuesta. Si ya se
     * preguntó, iOS no vuelve a mostrar nada: se contesta con lo que hay.
     */
    internal suspend fun pedirMientrasSeUsa(): Boolean = withContext(Dispatchers.Main) {
        if (manager.authorizationStatus == kCLAuthorizationStatusNotDetermined) {
            suspendCancellableCoroutine<Unit> { espera ->
                esperandoPermiso += espera
                espera.invokeOnCancellation { esperandoPermiso.remove(espera) }
                manager.requestWhenInUseAuthorization()
            }
        }
        tienePermiso(Permiso.UBICACION)
    }

    /**
     * Pide "siempre". No se espera la respuesta: iOS puede postergar el cartel hasta que la app
     * use la ubicación en segundo plano, y hasta entonces no contesta nada.
     */
    internal fun pedirSiempre(): Boolean {
        manager.requestAlwaysAuthorization()
        return tienePermiso(Permiso.UBICACION_SIEMPRE)
    }

    private fun edadSegundos(posicion: CLLocation): Double = -posicion.timestamp.timeIntervalSinceNow

    private fun soltar(espera: CancellableContinuation<CLLocation?>) {
        esperandoPosicion.remove(espera)
        if (esperandoPosicion.isEmpty()) manager.stopUpdatingLocation()
    }

    private fun entregar(posicion: CLLocation?) {
        val esperas = esperandoPosicion.toList()
        esperandoPosicion.clear()
        manager.stopUpdatingLocation()
        esperas.forEach { if (it.isActive) it.resume(posicion) }
    }

    private class Delegado : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val deAhora = didUpdateLocations.filterIsInstance<CLLocation>().lastOrNull {
                it.horizontalAccuracy >= 0 && Ubicacion.edadSegundos(it) * 1000 <= Lectura.FRESCURA_MAXIMA_MS
            } ?: return
            Ubicacion.entregar(deAhora)
        }

        // "Todavía no sé dónde estás" es pasajero: se sigue esperando hasta el tiempo límite.
        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            if (!tienePermiso(Permiso.UBICACION)) Ubicacion.entregar(null)
        }

        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            if (manager.authorizationStatus == kCLAuthorizationStatusNotDetermined) return
            val esperas = Ubicacion.esperandoPermiso.toList()
            Ubicacion.esperandoPermiso.clear()
            esperas.forEach { if (it.isActive) it.resume(Unit) }
        }
    }
}

actual fun ubicacionExacta(): Boolean =
    Ubicacion.manager.accuracyAuthorization == CLAccuracyAuthorization.CLAccuracyAuthorizationFullAccuracy
