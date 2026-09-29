package ar.lauta.buscarpatentes.ubicacion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Estado de los permisos que la app necesita (C4).
 *
 * Regla transversal del contrato: **negar un permiso degrada una función, nunca rompe la
 * app**. Por eso cada consulta es independiente y ninguna pantalla asume que las demás
 * están concedidas. Es lo que sostiene que la User Story 1 sea usable sola.
 */
object Permisos {

    fun tieneUbicacionPrecisa(context: Context): Boolean =
        concedido(context, Manifest.permission.ACCESS_FINE_LOCATION)

    fun tieneUbicacionEnSegundoPlano(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            concedido(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            // Antes de Android 10 no existía el permiso aparte: alcanza con el de ubicación.
            tieneUbicacionPrecisa(context)
        }

    fun tieneNotificaciones(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            concedido(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }

    fun tieneCamara(context: Context): Boolean =
        concedido(context, Manifest.permission.CAMERA)

    /** Lo que hay que pedir antes de habilitar la captura (FR-014). */
    val PARA_CAPTURA = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    private fun concedido(context: Context, permiso: String): Boolean =
        ContextCompat.checkSelfPermission(context, permiso) == PackageManager.PERMISSION_GRANTED
}
