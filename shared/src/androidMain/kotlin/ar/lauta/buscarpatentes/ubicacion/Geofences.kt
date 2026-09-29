package ar.lauta.buscarpatentes.ubicacion

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.plataforma.Permiso
import ar.lauta.buscarpatentes.plataforma.tienePermiso
import ar.lauta.buscarpatentes.data.EstadoDelJuego
import ar.lauta.buscarpatentes.data.EstadoRegistro
import ar.lauta.buscarpatentes.domain.Avisos
import ar.lauta.buscarpatentes.domain.RegistroParaAviso
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/**
 * Registro y reconciliación de geofences (C2, D4, T052 a T054).
 *
 * El conjunto activo son los registros pendientes cuyo número **ya toca**. Cambia en tres
 * momentos y en ninguno más: cuando avanza el contador del juego, cuando se guarda una
 * patente de ese número, y cuando se comparte una.
 *
 * Los ids activos se persisten en preferencias porque el proceso puede morir entre un
 * cambio y el siguiente, y sin esa memoria la reconciliación no sabría qué dar de baja.
 */
class Geofences(private val context: Context) {

    private val cliente by lazy { LocationServices.getGeofencingClient(context) }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, GeofenceReceiver::class.java),
            // MUTABLE es obligatorio: el sistema le mete el evento adentro al dispararlo.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    private var activos: Set<Long>
        get() = prefs.getStringSet(CLAVE_ACTIVOS, emptySet())!!.mapNotNull { it.toLongOrNull() }.toSet()
        set(valor) = prefs.edit().putStringSet(CLAVE_ACTIVOS, valor.map { it.toString() }.toSet()).apply()

    /**
     * Deja el conjunto de geofences igual a lo que dicta el estado actual (T053).
     *
     * Sin permiso de segundo plano no hace nada y no rompe: FR-030 dice que negarlo apaga
     * los avisos, no la app.
     */
    /**
     * @param elSistemaLosOlvido true cuando el proceso viene de arrancar —reinicio del
     * teléfono, actualización de la app, arranque en frío—. En esos casos el sistema no
     * conserva ningún geofence y hay que darlos todos de alta de nuevo, aunque las
     * preferencias digan que ya estaban. Ver [Avisos.previosSegunElSistema].
     */
    @SuppressLint("MissingPermission")
    suspend fun reconciliar(elSistemaLosOlvido: Boolean = false) {
        val estado = contenedor.estadoDelJuego.leer() ?: EstadoDelJuego()

        if (!estado.avisosActivos || !tienePermiso(Permiso.UBICACION_SIEMPRE)) {
            quitarTodos()
            return
        }

        val candidatos = contenedor.registros.todosUnaVez().map {
            RegistroParaAviso(
                id = it.id,
                numero = it.numero,
                compartida = it.estado == EstadoRegistro.COMPARTIDA,
                latitud = it.latitud,
                longitud = it.longitud,
            )
        }

        // T054: el truncado por límite de 100 lo decide el dominio, ya probado.
        val deseados = Avisos.aRegistrar(candidatos, estado.numeroActual, Avisos.LIMITE_GEOFENCES)
        val previos = Avisos.previosSegunElSistema(activos, elSistemaLosOlvido)
        val plan = Avisos.reconciliar(previos, deseados)

        if (plan.sinCambios) return

        if (plan.aQuitar.isNotEmpty()) {
            cliente.removeGeofences(plan.aQuitar.map { it.toString() })
        }

        if (plan.aAgregar.isNotEmpty()) {
            val pedido = GeofencingRequest.Builder()
                // Avisar si el jugador ya está adentro del radio al registrarlo.
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(plan.aAgregar.map(::construir))
                .build()
            cliente.addGeofences(pedido, pendingIntent)
        }

        activos = deseados.map { it.id }.toSet()
    }

    fun quitarTodos() {
        val previos = activos
        if (previos.isNotEmpty()) {
            cliente.removeGeofences(previos.map { it.toString() })
        }
        activos = emptySet()
    }

    private fun construir(r: RegistroParaAviso): Geofence = Geofence.Builder()
        .setRequestId(r.id.toString())
        .setCircularRegion(r.latitud, r.longitud, Avisos.RADIO_METROS)
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
        .build()

    private companion object {
        const val PREFS = "geofences"
        const val CLAVE_ACTIVOS = "activos"
    }
}
