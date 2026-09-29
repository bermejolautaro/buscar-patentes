package ar.lauta.buscarpatentes.respaldo

import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import ar.lauta.buscarpatentes.data.AlmacenFotos
import ar.lauta.buscarpatentes.data.BaseDeDatos
import ar.lauta.buscarpatentes.respaldo.Respaldo.lista
import ar.lauta.buscarpatentes.respaldo.Respaldo.numero
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.readByteArray
import kotlinx.io.writeString

/**
 * El contrato R de la 006, en la PC: lo que sale de un teléfono entra en el otro idéntico.
 *
 * Room no se abre acá. El respaldo de verdad empieza con un `VACUUM INTO` sobre la conexión de
 * Room; la prueba lo hace a mano sobre una base armada con el mismo esquema, y de ahí en adelante
 * corre el mismo código que la app (R8).
 */
class RespaldoTest {

    private val raiz = Path(SystemTemporaryDirectory, "respaldo-${Random.nextLong().toULong()}")
        .also { SystemFileSystem.createDirectories(it) }
    private val fotosDeOrigen = carpeta("fotos-origen")
    private val foto = Random(7).nextBytes(64 * 1024)

    @AfterTest
    fun limpiar() = Respaldo.borrarTodo(raiz)

    // --- Sacar ---

    @Test
    fun `sacar cuenta todo, deja la version y no toca la base`() {
        val origen = baseDeOrigen()
        val antes = volcado(origen)

        val copia = copiaDeRoom(origen)
        val cuentas = Respaldo.completar(copia.toString(), AlmacenFotos(fotosDeOrigen.toString()), "android", 1_790_000_000_000)

        assertEquals(Respaldo.Cuentas(patentes = 3, salidas = 1, fotos = 1, fotosFaltantes = 1, ultimaCaptura = 1_789_990_100_000), cuentas)
        abrir(copia).use { con ->
            val info = con.lista("SELECT clave, valor FROM respaldo_info") { it.getText(0) to it.getText(1) }.toMap()
            assertEquals("1", info["formato"])
            assertEquals("android", info["origen"])
            assertEquals("1790000000000", info["sacadoEn"])
            assertEquals("1", info["fotosFaltantes"])
            assertEquals("1789990100000", info["ultimaCaptura"])
            assertEquals(7, con.numero("PRAGMA user_version"))
            assertEquals(listOf("100.jpg"), con.lista("SELECT nombre FROM respaldo_foto") { it.getText(0) })
        }
        assertEquals(antes, volcado(origen))
    }

    // --- Ida y vuelta ---

    @Test
    fun `lo restaurado es identico bit por bit, con la foto por su nombre`() {
        val origen = baseDeOrigen()
        val respaldo = respaldoDe(origen)
        val (base, fotos) = destinoConOtrosDatos()

        val validacion = Respaldo.validarYa(respaldo.toString())
        assertTrue(validacion is Respaldo.Validacion.Valido, validacion.toString())
        assertEquals(3, validacion.cuentas.patentes)

        val preparado = Path(raiz, "preparado")
        Respaldo.preparar(respaldo.toString(), preparado)
        Respaldo.cambiar(preparado, base, fotos).confirmar()

        assertEquals(volcado(origen), volcado(base))
        abrir(base).use { con ->
            assertEquals(7, con.numero("PRAGMA user_version"))
            val tablas = con.lista("SELECT name FROM sqlite_master WHERE type = 'table'") { it.getText(0) }
            assertFalse("respaldo_info" in tablas || "respaldo_foto" in tablas)
        }
        assertContentEquals(foto, leer(Path(fotos, "100.jpg")))
        assertEquals(listOf("100.jpg"), SystemFileSystem.list(fotos).map { it.name })
        assertTrue(SystemFileSystem.list(Path(raiz, "destino")).none { it.name.endsWith(".anterior") })
    }

    // --- Rechazos ---

    @Test
    fun `lo que no es un respaldo se rechaza con su motivo`() {
        val texto = Path(raiz, "notas.respaldo").also { escribir(it, "no soy una base") }
        assertEquals(Respaldo.NO_ES, motivo(texto))

        val sinInfo = baseDeOrigen()
        assertEquals(Respaldo.NO_ES, motivo(sinInfo))

        val formatoNuevo = respaldoDe(baseDeOrigen("formato"))
        abrir(formatoNuevo).use { it.execSQL("UPDATE respaldo_info SET valor = '2' WHERE clave = 'formato'") }
        assertEquals(Respaldo.MAS_NUEVO, motivo(formatoNuevo))

        val baseNueva = respaldoDe(baseDeOrigen("version"))
        abrir(baseNueva).use { it.execSQL("PRAGMA user_version = 99") }
        assertEquals(Respaldo.MAS_NUEVO, motivo(baseNueva))

        val cortado = respaldoDe(baseDeOrigen("cortado"))
        val bytes = leer(cortado)
        SystemFileSystem.sink(cortado).buffered().use { it.write(bytes, 0, bytes.size * 6 / 10) }
        assertEquals(Respaldo.DANADO, motivo(cortado))
    }

    // --- Fallas ---

    @Test
    fun `si el cambio falla a mitad, el telefono queda como estaba`() {
        val respaldo = respaldoDe(baseDeOrigen())
        val (base, fotos) = destinoConOtrosDatos()
        val antes = volcado(base)
        val preparado = Path(raiz, "preparado")
        Respaldo.preparar(respaldo.toString(), preparado)
        // La base ya se apartó y la nueva ya está en su lugar cuando falta la carpeta de fotos.
        Respaldo.borrarTodo(Path(preparado, AlmacenFotos.CARPETA))

        assertFails { Respaldo.cambiar(preparado, base, fotos) }

        assertEquals(antes, volcado(base))
        assertEquals(listOf("999.jpg"), SystemFileSystem.list(fotos).map { it.name })
        assertTrue(SystemFileSystem.list(Path(raiz, "destino")).none { it.name.endsWith(".anterior") })
    }

    @Test
    fun `si la base nueva no abre, deshacer devuelve la vieja y borra lo que dejo la nueva`() {
        val respaldo = respaldoDe(baseDeOrigen())
        val (base, fotos) = destinoConOtrosDatos()
        val antes = volcado(base)
        val preparado = Path(raiz, "preparado")
        Respaldo.preparar(respaldo.toString(), preparado)

        val cambio = Respaldo.cambiar(preparado, base, fotos)
        escribir(Path("$base-wal"), "lo que dejó Room al abrir la nueva")
        cambio.deshacer()

        assertEquals(antes, volcado(base))
        assertFalse(SystemFileSystem.exists(Path("$base-wal")))
        assertEquals(listOf("999.jpg"), SystemFileSystem.list(fotos).map { it.name })
    }

    // --- Armado ---

    /** Una base como la de Room en la versión 7, con decimales largos, fotos, una salida y votos. */
    private fun baseDeOrigen(nombre: String = "origen"): Path {
        val p = Path(raiz, "$nombre.db")
        escribir(Path(fotosDeOrigen, "100.jpg"), foto)
        abrir(p).use { con ->
            ESQUEMA.forEach(con::execSQL)
            con.execSQL("PRAGMA user_version = 7")
            val registro = con.prepare(
                "INSERT INTO registro_de_captura (numero, patenteTexto, formato, latitud, longitud, precisionMetros, " +
                    "precisionDegradada, capturadoEn, estado, fotoRuta, recorridoId, probabilidadInicial) " +
                    "VALUES (?, ?, 'MERCOSUR', ?, ?, ?, ?, ?, 'PENDIENTE', ?, ?, 0)",
            )
            registro.use {
                fun fila(n: Long, texto: String?, lat: Double, lon: Double, precision: Float, en: Long, foto: String?, recorrido: Long?) {
                    it.bindLong(1, n)
                    if (texto == null) it.bindNull(2) else it.bindText(2, texto)
                    it.bindDouble(3, lat)
                    it.bindDouble(4, lon)
                    it.bindDouble(5, precision.toDouble())
                    it.bindLong(6, if (precision > 30f) 1 else 0)
                    it.bindLong(7, en)
                    if (foto == null) it.bindNull(8) else it.bindText(8, foto)
                    if (recorrido == null) it.bindNull(9) else it.bindLong(9, recorrido)
                    it.step()
                    it.reset()
                }
                // Una ruta del Android: la foto se busca por su nombre (D7).
                fila(313, null, -34.603738291234567, -58.381570123456789, 7.3456f, 1_789_990_000_000, "/data/user/0/ar.lauta.buscarpatentes/files/fotos/100.jpg", null)
                fila(314, null, -34.61, -58.42, 45.5f, 1_789_990_100_000, "/data/user/0/ar.lauta.buscarpatentes/files/fotos/200.jpg", null)
                fila(315, "AB123CD", 0.1 + 0.2, -58.1234567890123, 3.1f, 1_789_980_000_000, null, 1)
            }
            con.execSQL("INSERT INTO estado_del_juego VALUES (1, 316, 1, 'COBERTURA')")
            con.execSQL("INSERT INTO recorrido VALUES (1, 1789970000000, 1789975000000, 'TERMINADO', '[[-34.6,-58.4],[-34.61,-58.41]]')")
            con.execSQL("INSERT INTO punto_de_trayecto VALUES (1, 1, -34.6000001, -58.4000001, 4.25, 1789970100000)")
            con.execSQL("INSERT INTO punto_de_trayecto VALUES (2, 1, -34.6100002, -58.4100002, 12.75, 1789970200000)")
            con.execSQL("INSERT INTO voto VALUES (1, 1, 1, 1789990500000)")
        }
        return p
    }

    /** Lo que hace el `VACUUM INTO` de la app sobre la conexión de Room. */
    private fun copiaDeRoom(origen: Path): Path {
        val copia = Path(raiz, "${origen.name}.copia")
        abrir(origen).use { con ->
            con.prepare("VACUUM INTO ?").use { it.bindText(1, copia.toString()); it.step() }
        }
        return copia
    }

    private fun respaldoDe(origen: Path): Path = copiaDeRoom(origen).also {
        Respaldo.completar(it.toString(), AlmacenFotos(fotosDeOrigen.toString()), "android", 1_790_000_000_000)
    }

    /** Un teléfono que ya tiene otra cosa: una base con otra patente y otra foto. */
    private fun destinoConOtrosDatos(): Pair<Path, Path> {
        val destino = carpeta("destino")
        val base = Path(destino, BaseDeDatos.ARCHIVO)
        abrir(base).use { con ->
            ESQUEMA.forEach(con::execSQL)
            con.execSQL("PRAGMA user_version = 7")
            con.execSQL(
                "INSERT INTO registro_de_captura VALUES (1, 999, NULL, 'MERCOSUR', -31.4, -64.2, 5.0, 0, 1700000000000, 'PENDIENTE', NULL, NULL, 0)",
            )
        }
        val fotos = Path(destino, AlmacenFotos.CARPETA).also { SystemFileSystem.createDirectories(it) }
        escribir(Path(fotos, "999.jpg"), "otra foto")
        return base to fotos
    }

    private fun motivo(archivo: Path): String? =
        (Respaldo.validarYa(archivo.toString()) as? Respaldo.Validacion.Rechazado)?.motivo

    /** Cada fila de cada tabla de la base, con cada `REAL` por sus bits. */
    private fun volcado(p: Path): Map<String, List<List<String>>> = abrir(p).use { con ->
        Respaldo.TABLAS.associateWith { tabla -> con.filas("SELECT * FROM $tabla ORDER BY rowid") }
    }

    private fun SQLiteConnection.filas(sql: String): List<List<String>> = lista(sql) { s ->
        (0 until s.getColumnCount()).map { i ->
            when (s.getColumnType(i)) {
                SQLITE_DATA_INTEGER -> "i:${s.getLong(i)}"
                SQLITE_DATA_FLOAT -> "f:${s.getDouble(i).toRawBits()}"
                SQLITE_DATA_TEXT -> "t:${s.getText(i)}"
                SQLITE_DATA_BLOB -> "b:${s.getBlob(i).contentToString()}"
                else -> "null"
            }
        }
    }

    private fun abrir(p: Path): SQLiteConnection = BundledSQLiteDriver().open(p.toString())

    private fun carpeta(nombre: String) = Path(raiz, nombre).also { SystemFileSystem.createDirectories(it) }

    private fun escribir(p: Path, texto: String) = SystemFileSystem.sink(p).buffered().use { it.writeString(texto) }

    private fun escribir(p: Path, bytes: ByteArray) = SystemFileSystem.sink(p).buffered().use { it.write(bytes) }

    private fun leer(p: Path): ByteArray = SystemFileSystem.source(p).buffered().use { it.readByteArray() }

    private companion object {
        /** El de Room en la versión 7 (ver `ConstruirBase.kt` del Android). */
        val ESQUEMA = listOf(
            "CREATE TABLE registro_de_captura (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, numero INTEGER NOT NULL, " +
                "patenteTexto TEXT, formato TEXT NOT NULL, latitud REAL NOT NULL, longitud REAL NOT NULL, " +
                "precisionMetros REAL NOT NULL, precisionDegradada INTEGER NOT NULL, capturadoEn INTEGER NOT NULL, " +
                "estado TEXT NOT NULL, fotoRuta TEXT, recorridoId INTEGER, probabilidadInicial INTEGER NOT NULL DEFAULT 0)",
            "CREATE TABLE estado_del_juego (id INTEGER NOT NULL PRIMARY KEY, numeroActual INTEGER NOT NULL, " +
                "avisosActivos INTEGER NOT NULL, modoMapa TEXT NOT NULL DEFAULT 'COBERTURA')",
            "CREATE TABLE recorrido (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, iniciadoEn INTEGER NOT NULL, " +
                "finalizadoEn INTEGER, estado TEXT NOT NULL, caminoAjustado TEXT)",
            "CREATE TABLE punto_de_trayecto (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, recorridoId INTEGER NOT NULL, " +
                "latitud REAL NOT NULL, longitud REAL NOT NULL, precisionMetros REAL NOT NULL, registradoEn INTEGER NOT NULL)",
            "CREATE TABLE voto (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, registroId INTEGER NOT NULL, " +
                "valor INTEGER NOT NULL, votadoEn INTEGER NOT NULL)",
        )
    }
}
