package ar.lauta.buscarpatentes.ubicacion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.data.EstadoDelJuego
import ar.lauta.buscarpatentes.data.EstadoRegistro
import ar.lauta.buscarpatentes.data.RegistroDeCaptura
import ar.lauta.buscarpatentes.domain.Avisos
import ar.lauta.buscarpatentes.domain.RegistroParaAviso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.gms.location.GeofencingEvent

/**
 * Recibe el evento de proximidad y decide si notificar (T055 a T057).
 *
 * **Declarado en el manifiesto, no registrado en código.** Un receiver registrado en
 * código muere con el proceso, y FR-039 exige que el aviso llegue con la app cerrada.
 */
class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val evento = GeofencingEvent.fromIntent(intent) ?: return
        if (evento.hasError()) return

        val ids = evento.triggeringGeofences
            ?.mapNotNull { it.requestId.toLongOrNull() }
            ?: return

        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                procesar(context, ids)
            } finally {
                pendiente.finish()
            }
        }
    }

    private suspend fun procesar(context: Context, ids: List<Long>) {
        val estado = context.contenedor.estadoDelJuego.leer() ?: EstadoDelJuego()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ahora = System.currentTimeMillis()

        for (id in ids) {
            val registro = context.contenedor.registros.porId(id) ?: continue

            // T057: un aviso por salida (FR-041). La regla vive en Avisos, y el iPhone usa la misma.
            val yaAvisado = Avisos.yaAvisado(ahora, ultimoAviso = prefs.getLong(clave(id), 0L))

            val corresponde = Avisos.corresponde(
                registro = RegistroParaAviso(
                    id = registro.id,
                    numero = registro.numero,
                    compartida = registro.estado == EstadoRegistro.COMPARTIDA,
                    latitud = registro.latitud,
                    longitud = registro.longitud,
                ),
                numeroActual = estado.numeroActual,
                avisosActivos = estado.avisosActivos,
                yaAvisadoEnEstaSalida = yaAvisado,
            )

            if (!corresponde) continue

            notificar(context, registro)
            prefs.edit().putLong(clave(id), ahora).apply()
        }
    }

    /** T056: el aviso dice qué patente es y dónde está. */
    private fun notificar(context: Context, registro: RegistroDeCaptura) {
        val gestor = context.getSystemService(NotificationManager::class.java) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            gestor.createNotificationChannel(
                NotificationChannel(CANAL, "Patentes cerca", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Avisa cuando pasás cerca de una patente que ya toca." },
            )
        }

        val patente = registro.patenteTexto ?: registro.numero.toString().padStart(3, '0')

        // Abrir el lugar en cualquier app de mapas del teléfono.
        val verEnMapa = PendingIntent.getActivity(
            context,
            registro.id.toInt(),
            Intent(
                Intent.ACTION_VIEW,
                android.net.Uri.parse(
                    "geo:${registro.latitud},${registro.longitud}" +
                        "?q=${registro.latitud},${registro.longitud}($patente)",
                ),
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val aviso = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("Estás cerca de la $patente")
            .setContentText("La guardaste acá y ya toca. Andá, sacale la foto y mandala.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(verEnMapa)
            .build()

        // Si el permiso de notificaciones no está, esto no explota: simplemente no se ve.
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            gestor.notify(registro.id.toInt(), aviso)
        }
    }

    private fun clave(id: Long) = "ultimo_aviso_$id"

    private companion object {
        const val PREFS = "avisos"
        const val CANAL = "patentes_cerca"
    }
}
