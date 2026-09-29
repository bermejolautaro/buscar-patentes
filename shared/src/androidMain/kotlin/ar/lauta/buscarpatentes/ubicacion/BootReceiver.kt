package ar.lauta.buscarpatentes.ubicacion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Vuelve a registrar los geofences después de reiniciar el teléfono (T059, C2).
 *
 * **Esta clase existe para tapar la falla más silenciosa de todo el sistema.** Android
 * borra todos los geofences al reiniciar y al desactivar la ubicación. Sin este receiver
 * el aviso simplemente deja de llegar: no hay error, no hay log, nada se ve roto. El
 * jugador se entera meses después, cuando se da cuenta de que la app nunca le avisó.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // El sistema arranca con la tabla de geofences vacía. Sin esta bandera, la
                // reconciliación compara contra lo que la app dejó anotado en preferencias,
                // no ve diferencia y no registra nada — dejando el aviso muerto en silencio.
                Geofences(context).reconciliar(elSistemaLosOlvido = true)
            } finally {
                pendiente.finish()
            }
        }
    }
}
