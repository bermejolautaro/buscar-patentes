@file:OptIn(ExperimentalForeignApi::class)

package ar.lauta.buscarpatentes.plataforma

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUserDomainMask

actual object Carpetas {
    /** `Application Support`: no se ve desde Archivos, así la base no queda al alcance de un dedo. */
    actual val base: String
        get() = carpeta(NSApplicationSupportDirectory)

    actual val fotos: String
        get() = crear(carpeta(NSApplicationSupportDirectory) + "/fotos")

    /** `Documents`: se ve desde Archivos y desde iTunes, por donde viajan los respaldos (D8). */
    actual val respaldos: String
        get() = crear(carpeta(NSDocumentDirectory) + "/respaldos")

    actual val temporal: String
        get() = NSTemporaryDirectory().trimEnd('/')

    private fun carpeta(cual: NSSearchPathDirectory): String =
        crear(NSSearchPathForDirectoriesInDomains(cual, NSUserDomainMask, true).first() as String)

    private fun crear(ruta: String): String {
        NSFileManager.defaultManager.createDirectoryAtPath(ruta, true, null, null)
        return ruta
    }
}
