package ar.lauta.buscarpatentes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a los registros de captura.
 *
 * **Cada escritura de este DAO está acotada a su campo**: crear, adjuntar foto, marcar
 * compartida y corregir el número. No hay `@Update`, no hay `editarUbicacion`, y ninguna
 * sentencia nombra latitud, longitud, precisión ni timestamp en su `SET`.
 *
 * Esa ausencia **es** el Principio II. No es un descuido ni una funcionalidad pendiente:
 * agregar acá un update general rompería la garantía central del producto.
 *
 * La línea que separa lo corregible de lo intocable no es arbitraria: el número y el texto
 * de la patente son lo que el jugador **leyó**, y la ubicación, la precisión y el momento
 * son lo que el teléfono **midió**. El principio protege la medición, porque es lo único
 * que distingue una patente vista de una inventada.
 *
 * `InmutabilidadTest` lee este archivo y falla si alguna sentencia escribe sobre un campo
 * de evidencia. Verificado en rojo al agregar `corregirNumero`.
 */
@Dao
interface RegistroDao {

    @Insert
    suspend fun insertar(registro: RegistroDeCaptura): Long

    /** Único update permitido sobre la foto (FR-019). No toca la evidencia. */
    @Query("UPDATE registro_de_captura SET fotoRuta = :ruta WHERE id = :id")
    suspend fun adjuntarFoto(id: Long, ruta: String)

    /** Único update permitido sobre el estado (FR-015). No toca la evidencia. */
    @Query("UPDATE registro_de_captura SET estado = 'COMPARTIDA' WHERE id = :id")
    suspend fun marcarCompartida(id: Long)

    /**
     * Corregir un número mal tipeado (FR-014 de la especificación 002).
     *
     * Toca **solo** lo que el jugador leyó: el número, el texto de la patente y el formato
     * detectado. La ubicación, la precisión y el timestamp son lo que el teléfono **midió**,
     * y no aparecen en el `SET`. Esa es la distinción que hace que esta operación no rompa
     * el Principio II: el principio protege la medición, no la transcripción.
     *
     * Corregir es la única alternativa honesta a borrar y volver a capturar, que obligaría
     * a estar de nuevo en el lugar. Recrear el registro "con la misma ubicación" sería
     * carga retroactiva de metadata, que es justamente lo que el principio prohíbe.
     */
    @Query(
        "UPDATE registro_de_captura SET numero = :numero, patenteTexto = :texto, " +
            "formato = :formato WHERE id = :id",
    )
    suspend fun corregirNumero(id: Long, numero: Int, texto: String?, formato: String)

    @Query("DELETE FROM registro_de_captura WHERE id = :id")
    suspend fun borrar(id: Long)

    @Query("SELECT * FROM registro_de_captura WHERE id = :id")
    suspend fun porId(id: Long): RegistroDeCaptura?

    @Query("SELECT * FROM registro_de_captura WHERE numero = :numero ORDER BY capturadoEn DESC")
    suspend fun porNumero(numero: Int): List<RegistroDeCaptura>

    @Query("SELECT * FROM registro_de_captura ORDER BY capturadoEn DESC")
    fun todos(): Flow<List<RegistroDeCaptura>>

    @Query("SELECT * FROM registro_de_captura ORDER BY capturadoEn DESC")
    suspend fun todosUnaVez(): List<RegistroDeCaptura>

    @Query("SELECT * FROM registro_de_captura WHERE estado = 'PENDIENTE'")
    suspend fun pendientes(): List<RegistroDeCaptura>

    /** Las patentes capturadas durante un recorrido (FR-027, FR-031). */
    @Query("SELECT * FROM registro_de_captura WHERE recorridoId = :recorridoId ORDER BY capturadoEn")
    suspend fun deRecorrido(recorridoId: Long): List<RegistroDeCaptura>
}

/** Estado del juego: una sola fila (FR-020). */
@Dao
interface EstadoDelJuegoDao {

    @Query("SELECT * FROM estado_del_juego WHERE id = :id")
    fun observar(id: Int = EstadoDelJuego.ID_UNICO): Flow<EstadoDelJuego?>

    @Query("SELECT * FROM estado_del_juego WHERE id = :id")
    suspend fun leer(id: Int = EstadoDelJuego.ID_UNICO): EstadoDelJuego?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(estado: EstadoDelJuego)

    @Query("UPDATE estado_del_juego SET numeroActual = :numero WHERE id = :id")
    suspend fun fijarNumero(numero: Int, id: Int = EstadoDelJuego.ID_UNICO)


    /**
     * FR-003 de la 004: el modo del mapa se recuerda entre aperturas.
     *
     * Acotado a su columna, como los dos de arriba. Esta tabla no guarda evidencia, así que
     * el Principio II no lo exige; se escribe igual porque un `@Update` general acá sería la
     * puerta por la que el patrón se pierde.
     */
    @Query("UPDATE estado_del_juego SET modoMapa = :modo WHERE id = :id")
    suspend fun fijarModoMapa(modo: ModoMapa, id: Int = EstadoDelJuego.ID_UNICO)
}

/**
 * Recorridos (FR-023).
 *
 * Clase abstracta y no interfaz por [iniciar]: el invariante del data model —**como
 * máximo un recorrido `EN_CURSO` a la vez**— se sostiene cerrando el anterior y abriendo
 * el nuevo dentro de una misma transacción, y eso necesita un cuerpo de método. Por lo
 * mismo `insertar` es `protected`: nadie de afuera puede crear un recorrido salteándose
 * el cierre del que estaba abierto.
 */
@Dao
abstract class RecorridoDao {

    @Insert
    protected abstract suspend fun insertar(recorrido: Recorrido): Long

    @Query("SELECT * FROM recorrido WHERE estado = 'EN_CURSO' LIMIT 1")
    abstract suspend fun enCurso(): Recorrido?

    @Query("SELECT * FROM recorrido ORDER BY iniciadoEn DESC")
    abstract suspend fun todosUnaVez(): List<Recorrido>

    /**
     * Cierra cualquier recorrido abierto conservando sus puntos (FR-033).
     *
     * Es la operación que usa el arranque de la app para recuperarse de un servicio que
     * el sistema mató: el trayecto parcial queda, nunca se descarta.
     */
    @Query(
        "UPDATE recorrido SET estado = 'TERMINADO', finalizadoEn = :ahora " +
            "WHERE estado = 'EN_CURSO'",
    )
    abstract suspend fun cerrarLosAbiertos(ahora: Long): Int

    @Transaction
    open suspend fun iniciar(ahora: Long): Long {
        cerrarLosAbiertos(ahora)
        return insertar(Recorrido(iniciadoEn = ahora))
    }

    /**
     * Las salidas que todavia esperan que se les ajuste el camino (FR-031, FR-032).
     *
     * Solo las terminadas: mientras el recorrido sigue en curso van a llegar puntos nuevos y
     * ajustar seria trabajo tirado. La condicion sobre `caminoAjustado` **es** el standby: lo
     * que no se pudo ajustar sin conexion sigue en la cola sin que nada lo administre.
     *
     * Desde la 007 tambien entra un camino guardado con el criterio anterior, que no empieza con
     * `CaminoGuardado.PREFIJO`: vuelve a la cola solo, sin una migracion ni una marca aparte
     * (D3 de la 007). Un `2:` solo, lo que el servicio no pudo emparejar, no vuelve.
     */
    @Query(
        "SELECT * FROM recorrido WHERE estado = 'TERMINADO' " +
            "AND (caminoAjustado IS NULL OR caminoAjustado NOT LIKE '2:%')",
    )
    abstract suspend fun sinAjustar(): List<Recorrido>

    /**
     * Guarda el camino ajustado. Toca **solo** esa columna: los puntos de trayecto son lo que
     * el telefono midio y no se reescriben nunca.
     */
    @Query("UPDATE recorrido SET caminoAjustado = :polilinea WHERE id = :id")
    abstract suspend fun guardarCaminoAjustado(id: Long, polilinea: String)

    @Query("DELETE FROM punto_de_trayecto WHERE recorridoId = :id")
    protected abstract suspend fun borrarPuntosDe(id: Long)

    @Query("DELETE FROM recorrido WHERE id = :id")
    protected abstract suspend fun borrarRecorrido(id: Long)

    /**
     * Borra una salida y su trayecto (FR-024, FR-025).
     *
     * **No toca ninguna patente**, y eso es el requisito, no una simplificación. Una salida
     * es un registro de actividad —arrancar el servicio dos veces sin querer es exactamente
     * el caso que motivó esto—; una patente es evidencia, y el Principio II la protege
     * aunque se borre el recorrido que la rodeaba. Se borra el paseo, no lo que se vio.
     *
     * Los registros capturados durante la salida se quedan con un `recorridoId` que ya no
     * apunta a nada. Es deliberado: limpiarlo exigiría **escribir sobre un registro**, y
     * esta feature no escribe sobre evidencia por ninguna razón. La consulta `deRecorrido`
     * de un id borrado devuelve vacío, que es la respuesta correcta.
     *
     * No hay riesgo de que un recorrido futuro herede ese id: Room declara la clave como
     * `AUTOINCREMENT`, que nunca reutiliza valores.
     */
    @Transaction
    open suspend fun borrarConSuTrayecto(id: Long) {
        borrarPuntosDe(id)
        borrarRecorrido(id)
    }
}

/** Puntos de trayecto: se escriben de a uno, apenas llegan (FR-033, C3). */
@Dao
interface PuntoDeTrayectoDao {

    @Insert
    suspend fun insertar(punto: PuntoDeTrayecto): Long

    /**
     * Todos los puntos, **agrupables por recorrido y en orden dentro de cada uno**.
     *
     * El orden no es cosmético: es lo que separa un camino de un garabato. Sin `ORDER BY`
     * la base los devuelve en el orden que le conviene, y el trazo de la 003 uniría los
     * vértices salteados.
     */
    @Query("SELECT * FROM punto_de_trayecto ORDER BY recorridoId, registradoEn")
    suspend fun todos(): List<PuntoDeTrayecto>

    @Query("SELECT * FROM punto_de_trayecto WHERE recorridoId = :recorridoId ORDER BY registradoEn")
    suspend fun deRecorrido(recorridoId: Long): List<PuntoDeTrayecto>
}

/**
 * Votos sobre la permanencia de una patente.
 *
 * Solo inserta y lee: un voto emitido no se edita ni se retira. Si el jugador se
 * arrepiente, vota al revés y la cuenta lo absorbe, que es la misma historia contada
 * completa en vez de una fila reescrita.
 *
 * `borrarDe` existe para acompañar el borrado de un registro. No es una excepción a lo
 * anterior: no reescribe nada, se lleva los votos de algo que ya no existe.
 */
@Dao
interface VotoDao {

    @Insert
    suspend fun insertar(voto: Voto): Long

    /** Los votos de un registro **en orden**: la probabilidad se calcula acumulando. */
    @Query("SELECT valor FROM voto WHERE registroId = :registroId ORDER BY votadoEn, id")
    suspend fun valoresDe(registroId: Long): List<Int>

    /**
     * Todos los votos, agrupables por registro (FR-037).
     *
     * El mapa necesita la probabilidad de cada patente que dibuja, y pedirlas de a una sería
     * una consulta por marcador. Acá se traen todas y se agrupan en memoria, que es lo mismo
     * que ya se hace con los puntos de trayecto.
     */
    @Query("SELECT * FROM voto ORDER BY registroId, votadoEn, id")
    suspend fun todos(): List<Voto>

    @Query("DELETE FROM voto WHERE registroId = :registroId")
    suspend fun borrarDe(registroId: Long)
}
