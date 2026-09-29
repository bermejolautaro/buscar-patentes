package ar.lauta.buscarpatentes.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import ar.lauta.buscarpatentes.domain.FormatoPatente
import ar.lauta.buscarpatentes.domain.Probabilidad

/** Estado de uso de un registro (FR-015). */
enum class EstadoRegistro { PENDIENTE, COMPARTIDA }

/**
 * Una patente vista y anotada.
 *
 * Los campos de evidencia — [latitud], [longitud], [precisionMetros] y [capturadoEn] —
 * son inmutables una vez creado el registro. Esa inmutabilidad no se declara acá con un
 * comentario: se sostiene en [RegistroDao], que sencillamente no expone ninguna
 * operación que los toque (Principio II, FR-005, FR-006).
 */
@Entity(
    tableName = "registro_de_captura",
    indices = [Index("numero"), Index("estado"), Index("recorridoId")],
)
data class RegistroDeCaptura(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** La parte numérica de 3 dígitos, 0 a 999. Es lo que usa el juego. */
    val numero: Int,

    /** Patente completa si el jugador la cargó entera. Null en carga rápida. */
    val patenteTexto: String? = null,

    val formato: FormatoPatente = FormatoPatente.DESCONOCIDO,

    val latitud: Double,
    val longitud: Double,

    /** Precisión reportada por el proveedor de ubicación, en metros (FR-003). */
    val precisionMetros: Float,

    /** True si [precisionMetros] supera [UMBRAL_PRECISION_DEGRADADA] (FR-009). */
    val precisionDegradada: Boolean,

    /** Epoch millis del momento de la captura (FR-004). */
    val capturadoEn: Long,

    val estado: EstadoRegistro = EstadoRegistro.PENDIENTE,

    /** Ruta al archivo en almacenamiento privado. Null si se guardó con `+`. */
    @ColumnInfo(name = "fotoRuta") val fotoRuta: String? = null,

    /** Recorrido durante el cual se capturó, si había uno en curso (FR-027). */
    val recorridoId: Long? = null,

    /**
     * Desde dónde se acumulan los votos de este registro (ver [Probabilidad]).
     *
     * [Probabilidad.INICIAL] para todo registro nuevo. Vale [Probabilidad.INICIAL_VIEJA]
     * solo en los que la v7 encontró por encima de ese valor: siguen contando desde donde
     * se votaron, y muestran lo mismo que antes de migrar.
     */
    val probabilidadInicial: Int = Probabilidad.INICIAL,
) {
    companion object {
        /**
         * Por encima de estos metros la lectura se considera degradada.
         *
         * 50 m es aproximadamente el largo de media cuadra: más que eso y ya no sirve
         * para volver a encontrar el auto, que es el propósito del registro.
         * ponytail: valor calibrable contra uso real, no una constante sagrada.
         */
        const val UMBRAL_PRECISION_DEGRADADA = 50f
    }
}

/** Room no convierte enums solo. */
class Convertidores {
    @TypeConverter
    fun formatoDesdeTexto(valor: String): FormatoPatente = FormatoPatente.valueOf(valor)

    @TypeConverter
    fun formatoATexto(valor: FormatoPatente): String = valor.name

    @TypeConverter
    fun estadoDesdeTexto(valor: String): EstadoRegistro = EstadoRegistro.valueOf(valor)

    @TypeConverter
    fun estadoATexto(valor: EstadoRegistro): String = valor.name

    @TypeConverter
    fun recorridoDesdeTexto(valor: String): EstadoRecorrido = EstadoRecorrido.valueOf(valor)

    @TypeConverter
    fun recorridoATexto(valor: EstadoRecorrido): String = valor.name

    @TypeConverter
    fun modoDesdeTexto(valor: String): ModoMapa = ModoMapa.valueOf(valor)

    @TypeConverter
    fun modoATexto(valor: ModoMapa): String = valor.name
}

/**
 * Cómo se dibujan los recorridos sobre el mapa (FR-002 de la 004).
 *
 * Son tres respuestas a tres preguntas distintas, y por eso son modos y no un color más
 * astuto: [COBERTURA] aplana la diferencia entre salidas para que se lea el hueco, y
 * [ANTIGUEDAD] la exhibe para que se lea hace cuánto que no se pisa una calle. Los dos
 * requisitos son opuestos, y cualquier compromiso los contesta mal a los dos.
 */
enum class ModoMapa {
    /** Todo lo caminado igual y fuerte. Lo que importa es lo que **no** está pintado. */
    COBERTURA,

    /** Tres escalones de tiempo. Lo viejo resalta: es lo que hay que ir a revisar. */
    ANTIGUEDAD,

    /** Sin trazos. El mapa limpio, con las patentes solas. */
    APAGADO,

    ;

    /**
     * El siguiente estado del ciclo. Un toque avanza uno (FR-002).
     *
     * El ciclo vive acá y no en la pantalla porque es lo que define el tipo: si estuviera
     * suelto en el `onClick`, agregar un modo obligaría a acordarse de este `when`.
     *
     * El orden no es arbitrario: cobertura es donde el mapa arranca y donde se planea, y
     * apagado queda al final porque es el estado del que uno quiere salir. Tres toques
     * cierran el ciclo.
     */
    fun siguiente(): ModoMapa = when (this) {
        COBERTURA -> ANTIGUEDAD
        ANTIGUEDAD -> APAGADO
        APAGADO -> COBERTURA
    }
}

/**
 * En qué número va el grupo (FR-020). Una sola fila, [ID_UNICO].
 *
 * Escribir [numeroActual] **dispara reconciliación de geofences** (D4): es el único
 * momento en que cambia el conjunto de registros que merecen aviso. Esa consecuencia
 * llega con la User Story 4.
 */
@Entity(tableName = "estado_del_juego")
data class EstadoDelJuego(
    @PrimaryKey val id: Int = ID_UNICO,

    /** 0 a 999. Lo mantiene el jugador a mano: el grupo de WhatsApp es la fuente de verdad. */
    val numeroActual: Int = 313,

    /** FR-040: el jugador puede apagar los avisos sin perder el resto de la app. */
    val avisosActivos: Boolean = true,

    /**
     * Cuál de las tres preguntas está contestando el mapa (FR-003 de la 004).
     *
     * Vive acá y no en un almacén aparte porque es una preferencia del jugador de la misma
     * naturaleza que [avisosActivos], y esta tabla ya se lee en el mismo efecto de la
     * pantalla principal. Un segundo mecanismo de persistencia para un solo valor sería
     * exactamente lo que el Principio IV prohíbe.
     */
    val modoMapa: ModoMapa = ModoMapa.COBERTURA,
) {
    companion object {
        const val ID_UNICO = 1
    }
}

/** Estado de una salida a buscar patentes. */
enum class EstadoRecorrido { EN_CURSO, TERMINADO }

/**
 * Una salida a buscar patentes (FR-023).
 *
 * El invariante de **un solo recorrido en curso a la vez** no vive acá: lo sostiene
 * [RecorridoDao.iniciar], que cierra el anterior y abre el nuevo en la misma transacción.
 */
@Entity(tableName = "recorrido")
data class Recorrido(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val iniciadoEn: Long,

    /** Null mientras el recorrido está en curso. */
    val finalizadoEn: Long? = null,

    val estado: EstadoRecorrido = EstadoRecorrido.EN_CURSO,

    /**
     * El camino ajustado a las calles, como polilínea codificada (FR-031).
     *
     * Null mientras no se pudo ajustar: sin conexión al terminar la salida, o con el
     * servicio caído. **Ese null es el modo standby**: mientras siga null y el recorrido
     * tenga al menos dos puntos, se vuelve a intentar cada vez que la app abre con red.
     *
     * No reemplaza a los puntos de trayecto. Los puntos son lo que el teléfono midió y no se
     * tocan; esto es una interpretación de esos puntos contra el mapa de calles, y por eso
     * vive aparte y puede recalcularse.
     */
    val caminoAjustado: String? = null,
)

/**
 * Una posición registrada durante un recorrido, con muestreo espaciado (FR-024).
 *
 * La precisión de estos puntos es deliberadamente peor que la de un registro de captura
 * (FR-025): la pregunta que responden es "¿pasé por esta calle?", no "¿dónde exactamente
 * estaba el auto?". Esa segunda pregunta es la que el Principio II protege, y se reserva
 * para [RegistroDeCaptura].
 */
@Entity(tableName = "punto_de_trayecto", indices = [Index("recorridoId")])
data class PuntoDeTrayecto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val recorridoId: Long,

    val latitud: Double,
    val longitud: Double,
    val precisionMetros: Float,

    /** Epoch millis del momento en que el proveedor entregó la posición. */
    val registradoEn: Long,
)

/**
 * Un voto sobre si una patente sigue estando donde se la vio.
 *
 * Los votos se **agregan**, nunca se corrigen ni se reescriben: cada uno es una
 * observación con su propia hora, igual que una captura. Por eso la probabilidad no es
 * una columna del registro sino el resultado de leer los votos en orden; una columna
 * mutable perdería lo único que hace útil al voto, que es *cuándo* se pasó por ahí.
 *
 * [valor] es +1 —"la vi de nuevo"— o -1 —"pasé y no estaba"—. No hay pesos ni medias
 * medidas: el jugador la vio o no la vio.
 */
@Entity(tableName = "voto", indices = [Index("registroId")])
data class Voto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val registroId: Long,

    /** +1 si la patente sigue estando, -1 si no. */
    val valor: Int,

    /** Epoch millis del momento del voto: es el dato que dice a qué hora se pasó. */
    val votadoEn: Long,
)
