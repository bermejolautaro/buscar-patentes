package ar.lauta.buscarpatentes.data

import android.content.Context
import java.io.File

/**
 * Fotos como archivos en almacenamiento privado de la app (D5).
 *
 * No van a la galería del sistema a propósito: una foto en la galería la puede editar o
 * borrar cualquier otra app, y eso rompería el Principio II. Tampoco van como BLOB en la
 * base: varios MB por fila degradan toda consulta que toque la tabla.
 */
class AlmacenFotos(private val context: Context) {

    private val directorio: File
        get() = File(context.filesDir, CARPETA).apply { if (!exists()) mkdirs() }

    /** Archivo destino para una captura nueva. El nombre lo fija el momento. */
    fun archivoNuevo(capturadoEn: Long): File = File(directorio, "$capturadoEn.jpg")

    fun archivo(ruta: String): File = File(ruta)

    fun existe(ruta: String?): Boolean = ruta != null && File(ruta).exists()

    fun borrar(ruta: String?) {
        if (ruta != null) File(ruta).delete()
    }

    /** Bytes que ocupan todas las fotos juntas. Alimenta SC-014. */
    fun espacioOcupado(): Long =
        directorio.listFiles()?.sumOf { it.length() } ?: 0L

    /** SC-014: ¿las fotos ya pasaron el techo declarado? */
    fun superoElTecho(): Boolean = espacioOcupado() > TECHO_BYTES

    companion object {
        const val CARPETA = "fotos"

        /**
         * Techo declarado de las fotos (SC-014).
         *
         * SC-014 pide que el espacio se mantenga "por debajo de un límite conocido y visible
         * para el jugador", y las Assumptions dimensionan ese límite contra las fotos y el
         * fondo de mapa. El fondo ya tiene su techo con desalojo automático; las fotos no
         * pueden tenerlo: **borrar una foto es tirar evidencia**, y eso lo decide el jugador,
         * nunca la app (Principio II).
         *
         * Así que acá el techo se declara y se avisa, no se aplica. Con fotos de patente de
         * ~2 MB, 300 MB son unos 150 registros con foto: bastante más de lo que las
         * Assumptions esperan para toda la vida del juego.
         * ponytail: si el aviso llega a molestar seguido, el paso siguiente es ofrecer borrar
         * las fotos de los registros ya compartidos, no purgar solo.
         */
        const val TECHO_BYTES = 300L * 1024 * 1024
    }
}
