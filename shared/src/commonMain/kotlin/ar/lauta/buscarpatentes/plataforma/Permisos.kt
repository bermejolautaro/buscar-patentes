package ar.lauta.buscarpatentes.plataforma

import androidx.compose.runtime.Composable

/**
 * Los permisos que la app necesita, igual en los dos teléfonos (C4, P5 de la 006).
 *
 * Regla transversal: **negar un permiso degrada una función, nunca rompe la app**. Por eso cada
 * consulta es independiente y ninguna pantalla asume que las demás están concedidas.
 */
enum class Permiso {
    /** Ubicación mientras se usa la app: alcanza para anotar (FR-014 de la 001). */
    UBICACION,

    /** Ubicación siempre: la piden los avisos con la app cerrada (FR-027 de la 006). */

    NOTIFICACIONES,
    CAMARA,
}

expect fun tienePermiso(permiso: Permiso): Boolean

/**
 * Devuelve la función que pide [Permiso]s. Cuando el sistema contesta, llama a [alResponder] con
 * lo que quedó concedido, un valor por cada permiso pedido.
 */
@Composable
expect fun rememberPedirPermisos(alResponder: (Map<Permiso, Boolean>) -> Unit): (List<Permiso>) -> Unit
