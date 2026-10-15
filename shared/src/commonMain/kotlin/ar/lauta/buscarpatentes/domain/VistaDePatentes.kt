package ar.lauta.buscarpatentes.domain

/**
 * Qué patentes muestra el mapa de la pantalla principal (FR-001 y FR-002 de la 009).
 *
 * Reemplaza al interruptor de la 004, que tenía dos estados. No se guarda: cada arranque abre en
 * [DESDE_LA_QUE_TOCA] (FR-003).
 */
enum class VistaDePatentes {
    /** La que toca y las que vienen: las que ya pasaron no sirven para buscar. */
    DESDE_LA_QUE_TOCA,
    TODAS,
    NINGUNA,

    ;

    /** Un toque avanza uno. El ciclo vive acá por lo mismo que el de [ar.lauta.buscarpatentes.data.ModoMapa]. */
    fun siguiente(): VistaDePatentes = when (this) {
        DESDE_LA_QUE_TOCA -> TODAS
        TODAS -> NINGUNA
        NINGUNA -> DESDE_LA_QUE_TOCA
    }

    /**
     * Si el pin de [numero] se dibuja. [queToca] es null mientras la base no contestó. Con la lupa
     * de la 005, [filtro] gana en las dos vistas que muestran, aunque sea menor que el que toca
     * (FR-006).
     */
    fun muestra(numero: Int, queToca: Int?, filtro: Int?): Boolean = when {
        this == NINGUNA -> false
        filtro != null -> numero == filtro
        this == DESDE_LA_QUE_TOCA -> queToca == null || numero >= queToca
        else -> true
    }
}
