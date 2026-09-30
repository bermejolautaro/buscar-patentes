package ar.lauta.buscarpatentes.respaldo

import androidx.room.useWriterConnection
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.data.AlmacenFotos
import ar.lauta.buscarpatentes.data.BaseDeDatos
import ar.lauta.buscarpatentes.plataforma.Carpetas
import ar.lauta.buscarpatentes.plataforma.ahora
import ar.lauta.buscarpatentes.plataforma.sistema
import ar.lauta.buscarpatentes.reabrirContenedor
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

/**
 * El archivo que lleva todo de un teléfono al otro (contrato R de la 006, D8).
 *
 * Es una copia de la base hecha por SQLite, más las fotos adentro. Lo escribe y lo lee este mismo
 * código en los dos teléfonos, así que no hay formato que traducir: lo que sale del Android entra
 * en el iPhone tal cual, bit por bit.
 */
object Respaldo {

    /** Versión del contrato R. Sube solo si cambian las tablas o cómo se saca. */
    const val FORMATO = 1

    /** La de `@Database` en [BaseDeDatos]. Un respaldo de una base más nueva no se puede abrir acá. */
    const val VERSION_BASE = 8

    const val NO_ES = "Ese archivo no es un respaldo de la app."
    const val MAS_NUEVO = "Ese respaldo es de una versión más nueva de la app. Actualizala primero."
    const val DANADO = "El respaldo está dañado."

    private const val PREFIJO = "buscar-patentes-"
    private const val AUTOMATICO = "-antes-de-restaurar"
    private const val EXTENSION = ".respaldo"

    internal val TABLAS = listOf("registro_de_captura", "estado_del_juego", "recorrido", "punto_de_trayecto", "voto")

    data class Cuentas(
        val patentes: Int,
        val salidas: Int,
        val fotos: Int,
        val fotosFaltantes: Int,
        val ultimaCaptura: Long?,
    )

    sealed interface Validacion {
        data class Valido(val cuentas: Cuentas) : Validacion
        data class Rechazado(val motivo: String) : Validacion
    }

    // --- Sacar (R3) ---

    /**
     * Saca un respaldo a [Carpetas.respaldos] y devuelve su ruta. Tira si algo falla, y en ese
     * caso no deja nada a medias. **La base no se escribe nunca.**
     *
     * `VACUUM INTO` va sobre la conexión de escritura de Room: así la copia incluye lo que todavía
     * está en el WAL, aunque haya una salida grabando.
     */
    suspend fun sacar(automatico: Boolean = false): String = withContext(Dispatchers.IO) {
        val temporal = Path(Carpetas.temporal, "respaldo-en-curso.db")
        borrarTodo(temporal)
        try {
            contenedor.baseDeDatos.useWriterConnection { conexion ->
                conexion.usePrepared("VACUUM INTO ?") { it.bindText(1, temporal.toString()); it.step() }
            }
            val sacadoEn = ahora()
            completar(temporal.toString(), contenedor.fotos, sistema, sacadoEn)
            val destino = Path(Carpetas.respaldos, nombre(sacadoEn, automatico))
            SystemFileSystem.atomicMove(temporal, destino)
            destino.toString()
        } catch (e: Throwable) {
            borrarTodo(temporal)
            throw e
        }
    }

    /**
     * El paso 2 de R3, sobre una copia ya hecha: suma las fotos y las cuentas. Separado para
     * probarlo en la PC, donde no hay Room abierto.
     */
    internal fun completar(copia: String, fotos: AlmacenFotos, origen: String, sacadoEn: Long): Cuentas =
        abrir(copia).usar { con ->
            // Un solo archivo, sin WAL al lado: es lo que viaja.
            con.lista("PRAGMA journal_mode = DELETE") { it.getText(0) }
            con.execSQL("BEGIN")
            con.execSQL("CREATE TABLE respaldo_info (clave TEXT PRIMARY KEY, valor TEXT NOT NULL)")
            con.execSQL("CREATE TABLE respaldo_foto (nombre TEXT PRIMARY KEY, datos BLOB NOT NULL)")

            var faltantes = 0
            val rutas = con.lista("SELECT fotoRuta FROM registro_de_captura WHERE fotoRuta IS NOT NULL") { it.getText(0) }
            con.prepare("INSERT OR IGNORE INTO respaldo_foto (nombre, datos) VALUES (?, ?)").use { insertar ->
                for (ruta in rutas) {
                    if (!fotos.existe(ruta)) {
                        faltantes++
                        continue
                    }
                    val archivo = Path(fotos.archivo(ruta))
                    insertar.bindText(1, archivo.name)
                    insertar.bindBlob(2, SystemFileSystem.source(archivo).buffered().use { it.readByteArray() })
                    insertar.step()
                    insertar.reset()
                }
            }

            val cuentas = Cuentas(
                patentes = con.numero("SELECT count(*) FROM registro_de_captura").toInt(),
                salidas = con.numero("SELECT count(*) FROM recorrido").toInt(),
                fotos = con.numero("SELECT count(*) FROM respaldo_foto").toInt(),
                fotosFaltantes = faltantes,
                ultimaCaptura = con.lista("SELECT max(capturadoEn) FROM registro_de_captura") {
                    if (it.isNull(0)) null else it.getLong(0)
                }.first(),
            )
            con.prepare("INSERT INTO respaldo_info (clave, valor) VALUES (?, ?)").use { insertar ->
                listOf(
                    "formato" to FORMATO.toString(),
                    "sacadoEn" to sacadoEn.toString(),
                    "origen" to origen,
                    "patentes" to cuentas.patentes.toString(),
                    "salidas" to cuentas.salidas.toString(),
                    "fotos" to cuentas.fotos.toString(),
                    "fotosFaltantes" to cuentas.fotosFaltantes.toString(),
                    "ultimaCaptura" to (cuentas.ultimaCaptura?.toString() ?: ""),
                ).forEach { (clave, valor) ->
                    insertar.bindText(1, clave)
                    insertar.bindText(2, valor)
                    insertar.step()
                    insertar.reset()
                }
            }
            con.execSQL("COMMIT")
            cuentas
        }

    /** `buscar-patentes-AAAAMMDD-HHMM.respaldo`, con la hora de este teléfono (R1). */
    @OptIn(ExperimentalTime::class)
    internal fun nombre(sacadoEn: Long, automatico: Boolean): String {
        val t = Instant.fromEpochMilliseconds(sacadoEn).toLocalDateTime(TimeZone.currentSystemDefault())
        fun dos(n: Int) = n.toString().padStart(2, '0')
        val sello = "${t.year}${dos(t.month.ordinal + 1)}${dos(t.day)}-${dos(t.hour)}${dos(t.minute)}"
        return PREFIJO + sello + (if (automatico) AUTOMATICO else "") + EXTENSION
    }

    /**
     * Cuándo se sacó el último respaldo que está en el teléfono, o null (FR-020). Sale del nombre
     * de los archivos, que ya lo dice: no hace falta anotarlo aparte.
     * ponytail: si el jugador borra los respaldos desde Archivos, vuelve a decir "Nunca". Anotarlo
     * en preferencias si eso confunde.
     */
    @OptIn(ExperimentalTime::class)
    fun ultimo(): Long? {
        val carpeta = Path(Carpetas.respaldos)
        val patron = Regex(
            Regex.escape(PREFIJO) + """(\d{4})(\d{2})(\d{2})-(\d{2})(\d{2})""" +
                "(" + Regex.escape(AUTOMATICO) + ")?" + Regex.escape(EXTENSION),
        )
        return SystemFileSystem.list(carpeta).mapNotNull { patron.matchEntire(it.name) }.maxOfOrNull { m ->
            val (a, me, d, h, mi) = m.destructured
            LocalDateTime(a.toInt(), me.toInt(), d.toInt(), h.toInt(), mi.toInt())
                .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        }
    }

    // --- Validar (R4) ---

    suspend fun validar(archivo: String): Validacion = withContext(Dispatchers.IO) { validarYa(archivo) }

    /**
     * Cualquier "no" corta con su motivo. Solo lee.
     *
     * La cabecera separa los dos primeros motivos: un archivo que empieza como SQLite y después
     * no se puede leer es un respaldo roto, no otra cosa.
     */
    internal fun validarYa(archivo: String): Validacion = try {
        val cabecera = runCatching {
            SystemFileSystem.source(Path(archivo)).buffered().use { it.readByteArray(CABECERA.size) }
        }.getOrNull()
        exigir(cabecera.contentEquals(CABECERA), NO_ES)
        abrir(archivo).usar { con ->
            val tablas = si(DANADO) { con.lista("SELECT name FROM sqlite_master WHERE type = 'table'") { it.getText(0) }.toSet() }
            exigir("respaldo_info" in tablas, NO_ES)
            val info = si(DANADO) { con.lista("SELECT clave, valor FROM respaldo_info") { it.getText(0) to it.getText(1) }.toMap() }
            val formato = info["formato"]?.toIntOrNull()
            exigir(formato != null, NO_ES)
            exigir(formato!! <= FORMATO, MAS_NUEVO)
            exigir(si(DANADO) { con.numero("PRAGMA user_version") } <= VERSION_BASE, MAS_NUEVO)
            exigir(si(DANADO) { con.lista("PRAGMA integrity_check") { it.getText(0) } } == listOf("ok"), DANADO)
            exigir(tablas.containsAll(TABLAS), DANADO)
            Validacion.Valido(
                Cuentas(
                    patentes = info["patentes"]?.toIntOrNull() ?: 0,
                    salidas = info["salidas"]?.toIntOrNull() ?: 0,
                    fotos = info["fotos"]?.toIntOrNull() ?: 0,
                    fotosFaltantes = info["fotosFaltantes"]?.toIntOrNull() ?: 0,
                    ultimaCaptura = info["ultimaCaptura"]?.toLongOrNull(),
                ),
            )
        }
    } catch (r: Rechazo) {
        Validacion.Rechazado(r.motivo)
    } catch (_: Exception) {
        Validacion.Rechazado(DANADO)
    }

    private val CABECERA = "SQLite format 3\u0000".encodeToByteArray()

    private class Rechazo(val motivo: String) : Exception(motivo)

    private fun exigir(condicion: Boolean, motivo: String) {
        if (!condicion) throw Rechazo(motivo)
    }

    private inline fun <T> si(motivo: String, bloque: () -> T): T = try {
        bloque()
    } catch (r: Rechazo) {
        throw r
    } catch (_: Exception) {
        throw Rechazo(motivo)
    }

    // --- Restaurar (R6) ---

    /**
     * Reemplaza todo lo del teléfono por lo del respaldo, que ya pasó por [validar]. **Nunca
     * fusiona** (FR-017): lo que había queda en el respaldo automático, si había algo.
     *
     * Hasta el cambio de nombres, la base no se toca. Si el cambio o la reapertura fallan, se
     * deshace y el teléfono queda como estaba.
     */
    suspend fun restaurar(archivo: String, conRespaldoPrevio: Boolean): Unit =
        withContext(Dispatchers.IO) {
            if (conRespaldoPrevio) sacar(automatico = true)

            val preparado = Path(Carpetas.temporal, "preparado")
            preparar(archivo, preparado)

            val base = Path(Carpetas.base, BaseDeDatos.ARCHIVO)
            val fotos = Path(Carpetas.fotos)
            contenedor.baseDeDatos.close()
            val cambio = try {
                cambiar(preparado, base, fotos)
            } catch (e: Throwable) {
                reabrirContenedor()
                throw e
            }
            try {
                reabrirContenedor()
                // Room abre la base recién acá: valida el esquema y, si el respaldo es viejo, migra.
                contenedor.registros.todosUnaVez()
            } catch (e: Throwable) {
                runCatching { contenedor.baseDeDatos.close() }
                cambio.deshacer()
                reabrirContenedor()
                throw e
            }
            cambio.confirmar()
            borrarTodo(preparado)
        }

    /** R6 paso 2: la base y las fotos listas en [carpeta], sin tocar nada del teléfono. */
    internal fun preparar(archivo: String, carpeta: Path) {
        borrarTodo(carpeta)
        val fotos = Path(carpeta, AlmacenFotos.CARPETA)
        SystemFileSystem.createDirectories(fotos)
        val base = Path(carpeta, BaseDeDatos.ARCHIVO)
        SystemFileSystem.source(Path(archivo)).buffered().use { origen ->
            SystemFileSystem.sink(base).buffered().use { origen.transferTo(it) }
        }
        abrir(base.toString()).usar { con ->
            con.prepare("SELECT nombre, datos FROM respaldo_foto").use { fila ->
                while (fila.step()) {
                    // El nombre viene de afuera: solo el nombre, nunca una ruta que salga de la carpeta.
                    val nombre = Path(fila.getText(0)).name
                    if (nombre.isEmpty() || nombre == "." || nombre == "..") continue
                    SystemFileSystem.sink(Path(fotos, nombre)).buffered().use { it.write(fila.getBlob(1)) }
                }
            }
            con.execSQL("DROP TABLE respaldo_info")
            con.execSQL("DROP TABLE respaldo_foto")
            con.execSQL("VACUUM")
        }
    }

    /**
     * R6 paso 3: lo único que toca los datos. La base, sus archivos de al lado y la carpeta de fotos
     * se apartan con `.anterior`, y lo preparado ocupa su lugar. Si algo falla a mitad, se deshace
     * antes de tirar.
     */
    internal fun cambiar(preparado: Path, base: Path, fotos: Path): Cambio {
        val deLaBase = listOf("", "-wal", "-shm", "-journal").map { Path(base.toString() + it) }
        deLaBase.forEach { borrarTodo(anterior(it)) }
        borrarTodo(anterior(fotos))

        val cambio = Cambio(deLaBase.drop(1))
        try {
            (deLaBase + fotos).filter { SystemFileSystem.exists(it) }.forEach { cambio.mover(it, anterior(it)) }
            cambio.mover(Path(preparado, BaseDeDatos.ARCHIVO), base)
            cambio.mover(Path(preparado, AlmacenFotos.CARPETA), fotos)
        } catch (e: Throwable) {
            cambio.deshacer()
            throw e
        }
        return cambio
    }

    internal class Cambio(private val deAlLado: List<Path>) {
        private val movidos = mutableListOf<Pair<Path, Path>>()
        private val estaban = deAlLado.filter { SystemFileSystem.exists(it) }

        fun mover(de: Path, a: Path) {
            SystemFileSystem.atomicMove(de, a)
            movidos += de to a
        }

        fun deshacer() {
            // Si Room llegó a abrir la base nueva, dejó su `-wal` y su `-shm`: se borran, salvo que
            // sean los de la base vieja que todavía no se habían apartado.
            deAlLado.forEach { p -> if (p !in estaban || movidos.any { it.first == p }) borrarTodo(p) }
            movidos.asReversed().forEach { (de, a) -> SystemFileSystem.atomicMove(a, de) }
            movidos.clear()
        }

        fun confirmar() {
            movidos.filter { (_, a) -> a.name.endsWith(ANTERIOR) }.forEach { (_, a) -> borrarTodo(a) }
        }
    }

    private const val ANTERIOR = ".anterior"

    private fun anterior(p: Path) = Path(p.toString() + ANTERIOR)

    // --- SQLite y archivos ---

    private fun abrir(archivo: String): SQLiteConnection = BundledSQLiteDriver().open(archivo)

    private inline fun <T> SQLiteConnection.usar(bloque: (SQLiteConnection) -> T): T = try {
        bloque(this)
    } finally {
        close()
    }

    internal fun <T> SQLiteConnection.lista(sql: String, fila: (SQLiteStatement) -> T): List<T> =
        prepare(sql).use { s -> buildList { while (s.step()) add(fila(s)) } }

    internal fun SQLiteConnection.numero(sql: String): Long = lista(sql) { it.getLong(0) }.first()

    /** Borra un archivo o una carpeta con todo lo que tenga. Si no existe, nada. */
    internal fun borrarTodo(p: Path) {
        val datos = SystemFileSystem.metadataOrNull(p) ?: return
        if (datos.isDirectory) SystemFileSystem.list(p).forEach(::borrarTodo)
        SystemFileSystem.delete(p, mustExist = false)
    }
}
