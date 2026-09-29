package ar.lauta.buscarpatentes.ubicacion

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import ar.lauta.buscarpatentes.plataforma.Permiso
import ar.lauta.buscarpatentes.plataforma.tienePermiso
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.TimeSource

/**
 * Lectura de ubicación para el momento de la captura (D3).
 *
 * Usa `getCurrentLocation()` con [Priority.PRIORITY_HIGH_ACCURACY], **nunca**
 * `getLastLocation()`: la última posición conocida puede tener minutos de antigüedad, y
 * guardarla como si fuera del momento de la captura violaría el Principio II en
 * silencio — el registro diría una cosa y la realidad otra.
 *
 * Esta es la única parte del sistema que pide máxima precisión. El registro de recorridos
 * usa un presupuesto mucho más barato, por FR-025.
 */
class LectorUbicacion(private val context: Context) {

    private val cliente by lazy { LocationServices.getFusedLocationProviderClient(context) }

    @SuppressLint("MissingPermission")
    suspend fun leerAhora(): LecturaUbicacion {
        if (!tienePermiso(Permiso.UBICACION)) return LecturaUbicacion.SinPermiso

        val pedido = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(PRESUPUESTO_MS)
            .setMaxUpdateAgeMillis(0) // nada cacheado: la lectura es de ahora
            .build()

        return suspendCancellableCoroutine { cont ->
            cliente.getCurrentLocation(pedido, null)
                .addOnSuccessListener { location ->
                    cont.resume(
                        if (location != null) {
                            // La edad se mide con el reloj de arranque, inmune a cambios de la
                            // hora del teléfono, y se lleva al reloj monótono de Kotlin.
                            val edad = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos).nanoseconds
                            LecturaUbicacion.Ok(
                                Lectura(
                                    latitud = location.latitude,
                                    longitud = location.longitude,
                                    precisionMetros = location.accuracy,
                                    tomada = TimeSource.Monotonic.markNow() - edad,
                                ),
                            )
                        } else {
                            LecturaUbicacion.SinLectura
                        },
                    )
                }
                .addOnFailureListener { cont.resume(LecturaUbicacion.SinLectura) }
        }
    }

    companion object {
        /**
         * Cuánto se espera una lectura antes de rendirse.
         *
         * SC-001 da 10 segundos para todo el flujo de carga rápida, incluido lo que
         * tarda el jugador en tipear. 5 segundos para el GPS deja margen.
         * ponytail: calibrable contra uso real en la calle.
         */
        const val PRESUPUESTO_MS = 5_000L
    }
}
