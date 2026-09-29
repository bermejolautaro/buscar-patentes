package ar.lauta.buscarpatentes.ubicacion

import kotlin.time.TimeSource

/**
 * Una posición del GPS, igual en los dos teléfonos (P2 de la 006).
 *
 * [tomada] es la marca de un reloj que no se mueve con la hora del teléfono, fijada al momento
 * en que el sistema midió la posición y no al momento en que llegó. De ahí sale su edad.
 */
data class Lectura(
    val latitud: Double,
    val longitud: Double,
    val precisionMetros: Float,
    val tomada: TimeSource.Monotonic.ValueTimeMark,
) {
    val edadMs: Long
        get() = tomada.elapsedNow().inWholeMilliseconds

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

        /** ¿Una lectura precalentada de esta edad todavía sirve como "de ahora"? */
        fun sirvePrecalentada(edadMs: Long): Boolean = edadMs in 0..FRESCURA_MAXIMA_MS
    }
}

/** Resultado de pedir la ubicación para una captura. */
sealed interface LecturaUbicacion {
    data class Ok(val lectura: Lectura) : LecturaUbicacion

    /** El sistema no devolvió ninguna lectura dentro del presupuesto de tiempo. */
    data object SinLectura : LecturaUbicacion

    data object SinPermiso : LecturaUbicacion
}
