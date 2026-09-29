package ar.lauta.buscarpatentes

import ar.lauta.buscarpatentes.ubicacion.Lectura
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Principio II: la evidencia de un registro es inmutable.
 *
 * Este test no prueba una función, prueba una **ausencia**: que no exista ninguna forma
 * de modificar latitud, longitud, precisión o timestamp de un registro ya creado.
 *
 * Se hace leyendo el fuente del DAO, no por reflexión: las anotaciones de Room tienen
 * retención BINARY, así que `getAnnotation()` devuelve null en runtime y un test por
 * reflexión pasaría en vacío sin detectar nada. El primer intento de este test tenía
 * exactamente ese bug.
 *
 * `daoNoVacio` existe para que este archivo nunca vuelva a pasar por no haber mirado nada.
 *
 * Corre solo en la JVM, porque lee un archivo del repositorio. Las pruebas del módulo corren
 * con la carpeta del módulo como directorio de trabajo.
 */
class InmutabilidadTest {

    private lateinit var fuente: String

    private val camposDeEvidencia = listOf(
        "latitud", "longitud", "precisionmetros", "capturadoen", "precisiondegradada",
    )

    @BeforeTest
    fun leerFuenteDelDao() {
        val archivo = File("src/commonMain/kotlin/ar/lauta/buscarpatentes/data/Daos.kt")
        assertTrue(
            archivo.exists(),
            "No se encontró el fuente del DAO en ${archivo.absolutePath}. Si se movió, hay " +
                "que actualizar este test: sin el fuente no verifica nada.",
        )
        // Sin comentarios: el KDoc del DAO menciona @Update para explicar por qué no lo
        // usa, y eso haría fallar al test contra su propia documentación.
        fuente = sinComentarios(archivo.readText())
    }

    private fun sinComentarios(texto: String): String = texto
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("""//[^\n]*"""), "")

    @Test
    fun daoNoVacio() {
        // Guarda contra el falso negativo: si el fuente no tiene lo que esperamos,
        // los otros dos tests estarían mirando un archivo equivocado.
        assertTrue(fuente.contains("interface RegistroDao"), "El fuente leído no parece el DAO de registros")
        assertTrue(fuente.contains("fun insertar("), "El fuente no tiene el insert esperado")
        assertTrue(fuente.contains("fun adjuntarFoto("), "El fuente no tiene adjuntarFoto")
    }

    @Test
    fun `el DAO no expone ningun Update generico`() {
        assertFalse(
            Regex("""@Update\b""").containsMatchIn(fuente),
            "El DAO usa @Update. Un update genérico permite reescribir la evidencia de un " +
                "registro y rompe el Principio II (FR-005, FR-006).",
        )
    }

    @Test
    fun `ninguna query escribe sobre los campos de evidencia`() {
        val ofensoras = mutableListOf<String>()

        val queries = Regex("""@Query\(\s*"([^"]+)"""", RegexOption.DOT_MATCHES_ALL)
            .findAll(fuente)
            .map { it.groupValues[1] }

        for (sql in queries) {
            val normal = sql.lowercase().replace(Regex("""\s+"""), " ").trim()
            if (!normal.startsWith("update")) continue

            val asignaciones = normal.substringAfter(" set ", "").substringBefore(" where ")
            for (campo in camposDeEvidencia) {
                if (asignaciones.contains(campo)) ofensoras += "$campo en: $normal"
            }
        }

        assertTrue(
            ofensoras.isEmpty(),
            "Hay queries que reescriben evidencia: $ofensoras. Ver Principio II y FR-005. " +
                "Si un registro está mal, se borra y se crea otro con la ubicación real.",
        )
    }

    /**
     * El precalentado del GPS no puede volverse un `getLastLocation()` encubierto.
     *
     * Pedir la posición al tipear el primer dígito está bien: la lectura sigue siendo del
     * momento de la captura. Guardar una de hace cinco minutos con timestamp de ahora, no.
     * Este umbral es lo único que separa una cosa de la otra (D3, Principio II).
     */
    @Test
    fun `una lectura precalentada vieja no se acepta`() {
        assertTrue(Lectura.sirvePrecalentada(0), "Una lectura recién tomada sirve")
        assertTrue(
            Lectura.sirvePrecalentada(Lectura.FRESCURA_MAXIMA_MS - 1),
            "Tipear tres dígitos tiene que entrar en la ventana",
        )
        assertFalse(Lectura.sirvePrecalentada(5 * 60 * 1000), "Una lectura de cinco minutos atrás no es 'de ahora'")
        assertFalse(
            Lectura.sirvePrecalentada(-1),
            "Una edad negativa significa reloj corrido: se descarta, no se confía",
        )
    }
}
