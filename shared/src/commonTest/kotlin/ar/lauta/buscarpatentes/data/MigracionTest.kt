package ar.lauta.buscarpatentes.data

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import ar.lauta.buscarpatentes.respaldo.ESQUEMA_7
import ar.lauta.buscarpatentes.respaldo.Respaldo.lista
import ar.lauta.buscarpatentes.respaldo.Respaldo.numero
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory

/**
 * La migración de la 7 a la 8 (T006 de la 008): suma las zonas y no toca nada de lo que había.
 *
 * Room no se abre acá. Que el SQL sea el que Room espera lo garantiza haberlo copiado de su
 * `createAllTables`, y lo confirma la app al abrir una base migrada (quickstart §2).
 */
class MigracionTest {

    private val archivo = Path(SystemTemporaryDirectory, "migracion-${Random.nextLong().toULong()}.db")

    @AfterTest
    fun limpiar() {
        SystemFileSystem.delete(archivo, mustExist = false)
    }

    @Test
    fun `la 8 suma zona y cuadra sin tocar las salidas`() {
        abrir().use { con ->
            ESQUEMA_7.forEach(con::execSQL)
            con.execSQL("INSERT INTO recorrido VALUES (1, 1789970000000, 1789975000000, 'TERMINADO', '3:abc')")
            con.execSQL("INSERT INTO punto_de_trayecto VALUES (1, 1, -34.6000001, -58.4000001, 4.25, 1789970100000)")
            val antes = volcado(con)

            SQL_7_8.forEach(con::execSQL)

            val tablas = con.lista("SELECT name FROM sqlite_master WHERE type = 'table'") { it.getText(0) }
            assertTrue("zona" in tablas && "cuadra" in tablas, "faltan tablas: $tablas")
            con.execSQL(
                "INSERT INTO zona (nombre, borde, creadaEn, cuentaDesde, estado) VALUES ('Zona del 30/09', 'abc', 1, 1, 'ACTIVA')",
            )
            con.execSQL("INSERT INTO cuadra (zonaId, nombre, forma, quitada, recorridaAlTerminar) VALUES (1, NULL, 'abc', 0, 0)")
            assertEquals(1L, con.numero("SELECT COUNT(*) FROM cuadra"))
            assertEquals(antes, volcado(con))
        }
    }

    private fun volcado(con: SQLiteConnection): List<String> =
        con.lista("SELECT * FROM recorrido") { s -> (0 until s.getColumnCount()).joinToString { s.getText(it) } } +
            con.lista("SELECT * FROM punto_de_trayecto") { s -> (0 until s.getColumnCount()).joinToString { s.getText(it) } }

    private fun abrir(): SQLiteConnection = BundledSQLiteDriver().open(archivo.toString())
}
