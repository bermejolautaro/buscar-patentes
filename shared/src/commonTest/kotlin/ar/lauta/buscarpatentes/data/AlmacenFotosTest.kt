package ar.lauta.buscarpatentes.data

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.writeString

/**
 * Las fotos se buscan por nombre, no por la ruta guardada (D7 de la 006).
 *
 * Es lo que hace que una patente traída del Android muestre su foto en el iPhone, y que la foto
 * del iPhone siga apareciendo después de una reinstalación que le cambió la ruta a la app.
 */
class AlmacenFotosTest {

    private val carpeta = Path(SystemTemporaryDirectory, "fotos-${Random.nextLong().toULong()}")
        .also { SystemFileSystem.createDirectories(it) }
    private val almacen = AlmacenFotos(carpeta.toString())

    @AfterTest
    fun limpiar() {
        SystemFileSystem.list(carpeta).forEach { SystemFileSystem.delete(it) }
        SystemFileSystem.delete(carpeta)
    }

    private fun escribir(nombre: String, contenido: String = "jpeg") =
        SystemFileSystem.sink(Path(carpeta, nombre)).buffered().use { it.writeString(contenido) }

    @Test
    fun `una ruta del Android resuelve a la carpeta de este telefono`() {
        val deAndroid = "/data/user/0/ar.lauta.buscarpatentes/files/fotos/1788016023887.jpg"
        assertEquals(Path(carpeta, "1788016023887.jpg").toString(), almacen.archivo(deAndroid))
    }

    @Test
    fun `la foto existe si su nombre esta en la carpeta venga de donde venga la ruta`() {
        escribir("123.jpg")
        assertTrue(almacen.existe("/otra/carpeta/de/otro/telefono/123.jpg"))
        assertFalse(almacen.existe("/otra/carpeta/de/otro/telefono/456.jpg"))
        assertFalse(almacen.existe(null))
    }

    @Test
    fun `una foto nueva se llama como el momento de la captura`() {
        assertEquals(Path(carpeta, "42.jpg").toString(), almacen.archivoNuevo(42))
    }

    @Test
    fun `borrar por la ruta de otro telefono borra la de aca`() {
        escribir("7.jpg")
        almacen.borrar("C:\\otra\\7.jpg")
        assertFalse(almacen.existe("7.jpg"))
    }

    @Test
    fun `el espacio ocupado suma las fotos`() {
        escribir("1.jpg", "abc")
        escribir("2.jpg", "defgh")
        assertEquals(8L, almacen.espacioOcupado())
    }
}
