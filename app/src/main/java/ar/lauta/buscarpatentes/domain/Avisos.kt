package ar.lauta.buscarpatentes.domain

/** Lo mínimo que la decisión de aviso necesita saber de un registro. */
data class RegistroParaAviso(
    val id: Long,
    val numero: Int,
    val compartida: Boolean,
    val latitud: Double,
    val longitud: Double,
)

/**
 * Qué merece geofence y qué merece notificación (D4, FR-028, FR-029, FR-041).
 *
 * Todo acá es función pura. El cableado con `GeofencingClient` y con las notificaciones
 * vive en `ubicacion/`, y consume estas decisiones sin volver a tomarlas.
 */
object Avisos {

    /**
     * Límite del sistema: **100 geofences activos por aplicación**.
     *
     * Por eso [aRegistrar] filtra por número actual en vez de registrar el archivo entero.
     * Con el juego en un número, lo normal es entre cero y unos pocos.
     */
    const val LIMITE_GEOFENCES = 100

    /** Radio del aviso, en metros (FR-028). */
    const val RADIO_METROS = 150f

    /**
     * El conjunto de geofences que deben estar activos ahora mismo (D4).
     *
     * Solo los pendientes cuyo número **ya toca**. FR-029 dice que no hay que avisar por
     * números que todavía no llegaron ni por registros ya compartidos, así que esos no
     * necesitan geofence y no gastan una de las 100 ranuras.
     *
     * Si alguna vez hubiera más de [LIMITE_GEOFENCES] candidatos, se trunca de forma
     * determinista por id en lugar de fallar en silencio (C2).
     */
    fun aRegistrar(registros: List<RegistroParaAviso>, numeroActual: Int): List<RegistroParaAviso> =
        registros
            .filter { !it.compartida && it.numero == numeroActual }
            .sortedBy { it.id }
            .take(LIMITE_GEOFENCES)

    /**
     * ¿Corresponde notificar por este registro? (FR-028, FR-029, FR-041)
     *
     * [yaAvisadoEnEstaSalida] implementa la deduplicación: un aviso por registro por
     * salida, aunque el jugador pase cinco veces por la misma cuadra.
     */
    fun corresponde(
        registro: RegistroParaAviso,
        numeroActual: Int,
        avisosActivos: Boolean,
        yaAvisadoEnEstaSalida: Boolean,
    ): Boolean = avisosActivos &&
        !registro.compartida &&
        registro.numero == numeroActual &&
        !yaAvisadoEnEstaSalida

    /**
     * Los geofences que hay que dar de baja y los que hay que dar de alta.
     *
     * Se calcula por diferencia contra lo que ya está registrado, para no desregistrar y
     * volver a registrar lo que no cambió — eso despierta al sistema de gusto.
     */
    fun reconciliar(
        activos: Set<Long>,
        deseados: List<RegistroParaAviso>,
    ): Reconciliacion {
        val idsDeseados = deseados.map { it.id }.toSet()
        return Reconciliacion(
            aQuitar = activos - idsDeseados,
            aAgregar = deseados.filter { it.id !in activos },
        )
    }

    /**
     * Qué geofences hay **realmente** registrados en el sistema, no cuáles cree la app.
     *
     * Android borra todos los geofences al reiniciar el teléfono. La app guarda su lista en
     * preferencias, y esa lista sobrevive al reinicio: si se la creyera, la reconciliación
     * post-arranque no vería ninguna diferencia y no volvería a registrar nada. El aviso
     * dejaría de llegar sin un solo error a la vista — la falla exacta que T059 y C2 existen
     * para evitar.
     *
     * Por eso, después de un arranque, lo que la app recuerda no vale: el sistema no tiene
     * nada y hay que dar todo de alta de nuevo.
     */
    fun previosSegunElSistema(
        activosSegunLaApp: Set<Long>,
        elSistemaLosOlvido: Boolean,
    ): Set<Long> = if (elSistemaLosOlvido) emptySet() else activosSegunLaApp

    data class Reconciliacion(
        val aQuitar: Set<Long>,
        val aAgregar: List<RegistroParaAviso>,
    ) {
        val sinCambios: Boolean get() = aQuitar.isEmpty() && aAgregar.isEmpty()
    }
}
