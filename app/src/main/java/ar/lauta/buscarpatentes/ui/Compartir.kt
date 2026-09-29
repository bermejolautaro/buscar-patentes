package ar.lauta.buscarpatentes.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.data.RegistroDeCaptura
import java.io.File

/**
 * Compartir un registro hacia otra app del teléfono (FR-012, contrato C1).
 *
 * **Solo va el número.** Ni ubicación ni timestamp viajan en el mensaje: son evidencia
 * privada del jugador, no parte de la jugada. El grupo pide una patente, no un informe.
 */
object Compartir {

    /**
     * Arma el chooser y marca el registro como compartido.
     *
     * El cambio de estado ocurre al **lanzar** el chooser, no al confirmar el envío:
     * Android no informa si el usuario completó el envío. Es una imprecisión aceptada y
     * documentada en C1 — puede quedar marcada como compartida una patente que al final
     * no se mandó. El jugador lo ve y lo sabe; lo contrario sería no marcar nunca nada.
     */
    suspend fun registro(context: Context, registro: RegistroDeCaptura) {
        val texto = registro.patenteTexto ?: registro.numero.toString().padStart(3, '0')

        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, texto)

            val foto = registro.fotoRuta?.let { File(context.contenedor.fotos.archivo(it)) }
            if (foto != null && foto.exists()) {
                type = "image/jpeg"
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    foto,
                )
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
        }

        context.startActivity(
            Intent.createChooser(intent, "Mandar la $texto")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )

        // FR-015: queda archivada como usada, no se borra. Y deja de generar avisos (FR-029).
        context.contenedor.registros.marcarCompartida(registro.id)
        // FR-029: una compartida deja de avisar, asi que sale del conjunto de geofences.
        context.contenedor.geofences.reconciliar()
    }
}
