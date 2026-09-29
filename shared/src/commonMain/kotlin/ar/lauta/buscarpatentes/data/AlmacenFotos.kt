package ar.lauta.buscarpatentes.data

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * Fotos como archivos en almacenamiento privado de la app (D5).
 *
 * No van a la galería del sistema a propósito: una foto en la galería la puede editar o
 * borrar cualquier otra app, y eso rompería el Principio II. Tampoco van como BLOB en la
 * base: varios MB por fila degradan toda consulta que toque la tabla.
 *
 * [carpeta] es `Carpetas.fotos` en la app, y una carpeta temporal en las pruebas.
 */
class AlmacenFotos(private val carpeta: String) {

    /** Ruta destino para una captura nueva. El nombre lo fija el momento. */
    fun archivoNuevo(capturadoEn: Long): String = Path(carpeta, "$capturadoEn.jpg").toString()

    /**
     * Dónde está, en este teléfono, la foto que un registro nombra (D7 de la 006).
     *
     * `fotoRuta` guarda la ruta absoluta del teléfono donde se sacó. En el iPhone esa ruta no
     * existe, y la del propio iPhone puede cambiar al reinstalar. El nombre del archivo, en
     * cambio, es el mismo en todos lados: se busca por nombre dentro de la carpeta de fotos, y
     * el dato guardado no se reescribe.
     */
    fun archivo(ruta: String): String =
        Path(carpeta, ruta.substringAfterLast('/').substringAfterLast('\\')).toString()

    fun existe(ruta: String?): Boolean = ruta != null && SystemFileSystem.exists(Path(archivo(ruta)))

    fun borrar(ruta: String?) {
        if (ruta != null) SystemFileSystem.delete(Path(archivo(ruta)), mustExist = false)
    }

    /** Bytes que ocupan todas las fotos juntas. Alimenta SC-014. */
    fun espacioOcupado(): Long {
        val directorio = Path(carpeta)
        if (!SystemFileSystem.exists(directorio)) return 0L
        return SystemFileSystem.list(directorio).sumOf { SystemFileSystem.metadataOrNull(it)?.size ?: 0L }
    }

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
