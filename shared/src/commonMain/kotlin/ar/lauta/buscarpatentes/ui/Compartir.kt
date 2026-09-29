package ar.lauta.buscarpatentes.ui

import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.data.RegistroDeCaptura
import ar.lauta.buscarpatentes.plataforma.Vigilancia
import ar.lauta.buscarpatentes.plataforma.compartir

/**
 * Compartir un registro hacia otra app del teléfono (FR-012, contrato C1).
 *
 * **Solo va el número.** Ni ubicación ni timestamp viajan en el mensaje: son evidencia
 * privada del jugador, no parte de la jugada. El grupo pide una patente, no un informe.
 */
object Compartir {

    /**
     * Abre la hoja de compartir y marca el registro como compartido.
     *
     * El cambio de estado ocurre al **abrir** la hoja, no al confirmar el envío: ningún sistema
     * informa si el usuario completó el envío (D14 de la 006). Es una imprecisión aceptada y
     * documentada en C1 — puede quedar marcada como compartida una patente que al final
     * no se mandó. El jugador lo ve y lo sabe; lo contrario sería no marcar nunca nada.
     */
    suspend fun registro(registro: RegistroDeCaptura) {
        val texto = registro.patenteTexto ?: registro.numero.toString().padStart(3, '0')
        val foto = registro.fotoRuta?.let { contenedor.fotos.archivo(it) }
            ?.takeIf { contenedor.fotos.existe(it) }
        compartir(texto, foto)

        // FR-015: queda archivada como usada, no se borra. Y deja de generar avisos (FR-029).
        contenedor.registros.marcarCompartida(registro.id)
        // FR-029: una compartida deja de avisar, asi que sale del conjunto de geofences.
        Vigilancia.reconciliar()
    }
}
