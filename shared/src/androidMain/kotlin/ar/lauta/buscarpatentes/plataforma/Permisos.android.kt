package ar.lauta.buscarpatentes.plataforma

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.content.ContextCompat

/**
 * Qué permisos del sistema hay detrás de cada [Permiso]. Los que el Android de este teléfono no
 * tiene —segundo plano antes de Android 10, notificaciones antes de 13— no se piden: se dan por
 * concedidos, que es lo que el sistema hace.
 */
private fun manifiesto(permiso: Permiso): List<String> = when (permiso) {
    // Fina y gruesa juntas: lo que se pedía antes de habilitar la captura (FR-014).
    Permiso.UBICACION -> listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    Permiso.UBICACION_SIEMPRE ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        else emptyList()
    Permiso.NOTIFICACIONES ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) listOf(Manifest.permission.POST_NOTIFICATIONS)
        else emptyList()
    Permiso.CAMARA -> listOf(Manifest.permission.CAMERA)
}

private fun concedido(permiso: String): Boolean =
    ContextCompat.checkSelfPermission(contextoDeLaApp, permiso) == PackageManager.PERMISSION_GRANTED

actual fun tienePermiso(permiso: Permiso): Boolean = when (permiso) {
    // La captura necesita la fina: con la gruesa sola la patente quedaría a cuadras.
    Permiso.UBICACION -> concedido(Manifest.permission.ACCESS_FINE_LOCATION)
    // Antes de Android 10 no existía el permiso aparte: alcanza con el de ubicación.
    Permiso.UBICACION_SIEMPRE ->
        manifiesto(permiso).all(::concedido) && tienePermiso(Permiso.UBICACION)
    else -> manifiesto(permiso).all(::concedido)
}

@Composable
actual fun rememberPedirPermisos(alResponder: (Map<Permiso, Boolean>) -> Unit): (List<Permiso>) -> Unit {
    val responder by rememberUpdatedState(alResponder)
    // Lo pedido vive entre el pedido y la respuesta, que llega en otra composición.
    val pedidos = remember { mutableListOf<Permiso>() }
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { concedidos ->
        responder(
            pedidos.associateWith { permiso ->
                val delSistema = manifiesto(permiso)
                // Ubicación: cualquiera de las dos alcanza para seguir, como hasta ahora.
                delSistema.isEmpty() || delSistema.any { concedidos[it] ?: concedido(it) }
            },
        )
    }
    return { lista ->
        pedidos.clear()
        pedidos.addAll(lista)
        val delSistema = lista.flatMap(::manifiesto)
        if (delSistema.isEmpty()) responder(lista.associateWith { true })
        else lanzador.launch(delSistema.toTypedArray())
    }
}
