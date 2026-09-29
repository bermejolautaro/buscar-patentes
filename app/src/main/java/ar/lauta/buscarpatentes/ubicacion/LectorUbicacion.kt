package ar.lauta.buscarpatentes.ubicacion

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.SystemClock
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Resultado de pedir la ubicación para una captura. */
sealed interface LecturaUbicacion {
    data class Ok(val location: Location) : LecturaUbicacion

    /** El sistema no devolvió ninguna lectura dentro del presupuesto de tiempo. */
    data object SinLectura : LecturaUbicacion

    data object SinPermiso : LecturaUbicacion
}

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
        if (!Permisos.tieneUbicacionPrecisa(context)) return LecturaUbicacion.SinPermiso

        val pedido = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(PRESUPUESTO_MS)
            .setMaxUpdateAgeMillis(0) // nada cacheado: la lectura es de ahora
            .build()

        return suspendCancellableCoroutine { cont ->
            cliente.getCurrentLocation(pedido, null)
                .addOnSuccessListener { location ->
                    cont.resume(
                        if (location != null) LecturaUbicacion.Ok(location)
                        else LecturaUbicacion.SinLectura,
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

        /**
         * Cuánto puede envejecer una lectura precalentada y seguir siendo "de ahora".
         *
         * El precalentado pide la posición cuando el jugador tipea el primer dígito, no
         * cuando toca `+`. Eso aprovecha los segundos que igual iba a pasar tipeando, sin
         * relajar nada: la posición sigue siendo del momento de la captura.
         *
         * Este umbral es lo que impide que se convierta en un `getLastLocation()` encubierto.
         * Si el jugador tipea, se distrae y confirma cinco minutos después, la lectura vieja
         * se descarta y se pide una nueva. Guardar esa posición como si fuera del momento
         * violaría el Principio II en silencio, que es exactamente lo que D3 prohíbe.
         * ponytail: 15 s cubre tipear tres dígitos con holgura. Calibrable.
         */
        const val FRESCURA_MAXIMA_MS = 15_000L

        /** Edad de una lectura en milisegundos, inmune a cambios del reloj del teléfono. */
        fun edadDe(location: Location): Long =
            (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000

        /** ¿Una lectura precalentada de esta edad todavía sirve como "de ahora"? */
        fun sirvePrecalentada(edadMs: Long): Boolean = edadMs in 0..FRESCURA_MAXIMA_MS
    }
}
