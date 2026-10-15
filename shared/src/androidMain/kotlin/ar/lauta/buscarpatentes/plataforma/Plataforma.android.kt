package ar.lauta.buscarpatentes.plataforma

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.content.FileProvider
import ar.lauta.buscarpatentes.ubicacion.LectorUbicacion
import ar.lauta.buscarpatentes.ubicacion.LecturaUbicacion
import ar.lauta.buscarpatentes.ubicacion.ServicioRecorrido
import ar.lauta.buscarpatentes.ui.Ir
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Lo que se abre desde acá sale del contexto de la app y no de una actividad, así que va en una
// tarea nueva: sin la bandera, Android no deja abrir nada.
private fun abrir(intent: Intent) {
    contextoDeLaApp.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private val autoridadDeFotos: String
    get() = "${contextoDeLaApp.packageName}.fileprovider"

actual object Ubicacion {
    private val lector by lazy { LectorUbicacion(contextoDeLaApp) }

    actual suspend fun leerAhora(): LecturaUbicacion = lector.leerAhora()
}

actual fun ubicacionExacta(): Boolean = true

actual object Grabacion {
    actual val enCurso: StateFlow<Long?> get() = ServicioRecorrido.enCurso
    actual val puntosGuardados: StateFlow<Int> get() = ServicioRecorrido.puntosGuardados

    actual fun empezar() = ServicioRecorrido.empezar(contextoDeLaApp)

    actual fun terminar() = ServicioRecorrido.terminar(contextoDeLaApp)
}

/**
 * `geo:` y no `google.navigation:`: es el esquema de la plataforma, así que el jugador usa la
 * app de mapas que tenga. `geo:0,0?q=lat,lng(etiqueta)` es la forma documentada de "mostrame
 * este punto con este nombre"; el `0,0` lo reemplaza la consulta.
 *
 * Se captura la excepción en vez de preguntar antes: desde Android 11 `resolveActivity` devuelve
 * null salvo que el manifiesto declare un `<queries>` para el esquema.
 */
actual fun abrirPunto(latitud: Double, longitud: Double, etiqueta: String): Boolean = try {
    abrir(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$latitud,$longitud(${Uri.encode(etiqueta)})")))
    true
} catch (_: ActivityNotFoundException) {
    false
}

/**
 * En Google Maps, o en el navegador si no está (FR-019, FR-019a). En el navegador entran menos
 * paradas, y si sobran no se abre: Maps descartaría las de más sin decir nada (FR-019b).
 */
actual fun abrirRecorrido(url: String, paradas: Int): Ir.ResultadoRecorrido {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    return try {
        abrir(Intent(intent).setPackage("com.google.android.apps.maps"))
        Ir.ResultadoRecorrido.ABIERTO
    } catch (_: ActivityNotFoundException) {
        if (Ir.accionPara(paradas, Ir.MAXIMO_PARADAS_NAVEGADOR) is Ir.AccionRecorrido.Sobran) {
            return Ir.ResultadoRecorrido.DEMASIADAS_PARA_EL_NAVEGADOR
        }
        try {
            abrir(intent)
            Ir.ResultadoRecorrido.ABIERTO
        } catch (_: ActivityNotFoundException) {
            Ir.ResultadoRecorrido.SIN_APP
        }
    }
}

actual fun abrirAjustesDelSistema() {
    abrir(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", contextoDeLaApp.packageName, null),
        ),
    )
}

/** La foto viaja por el FileProvider: un `file://` lanza `FileUriExposedException` desde Android 7 (C1). */
actual fun compartir(texto: String, foto: String?) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, texto)
        val archivo = foto?.let(::File)
        if (archivo != null && archivo.exists()) {
            type = "image/jpeg"
            putExtra(
                Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(contextoDeLaApp, autoridadDeFotos, archivo),
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            type = "text/plain"
        }
    }
    abrir(Intent.createChooser(intent, "Mandar la $texto"))
}

/**
 * `NET_CAPABILITY_VALIDATED` y no solo "hay una red": una wifi de hotel que todavía pide login
 * está conectada y no llega a internet.
 */
actual fun hayConexion(): Boolean {
    val cm = contextoDeLaApp.getSystemService(ConnectivityManager::class.java) ?: return false
    val capacidades = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

actual suspend fun postear(url: String, cuerpo: String): String? = withContext(Dispatchers.IO) {
    val conexion = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = ESPERA_MS
        readTimeout = ESPERA_MS
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        // Overpass rechaza con 406 un pedido sin identificación propia, y el servidor de Valhalla
        // pide lo mismo en sus reglas de uso (D1 de la 008).
        setRequestProperty("User-Agent", "buscar-patentes")
    }
    try {
        conexion.outputStream.bufferedWriter().use { it.write(cuerpo) }
        if (conexion.responseCode != HttpURLConnection.HTTP_OK) null
        else conexion.inputStream.bufferedReader().readText()
    } finally {
        conexion.disconnect()
    }
}

private const val ESPERA_MS = 20_000

actual val sistema: String = "android"

actual fun perfilDeAprovisionamiento(): ByteArray? = null

actual fun programarAvisoDeVencimiento(vence: Long) {}

actual fun mandarRespaldo(ruta: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(contextoDeLaApp, autoridadDeFotos, File(ruta)))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    abrir(Intent.createChooser(intent, "Mandar el respaldo"))
}

/** El archivo elegido se copia a la caché: el `content://` que devuelve el sistema no es una ruta. */
@Composable
actual fun rememberElegirRespaldo(alElegir: (String?) -> Unit): () -> Unit {
    val elegir by rememberUpdatedState(alElegir)
    val alcance = rememberCoroutineScope()
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            elegir(null)
        } else {
            alcance.launch {
                val copia = withContext(Dispatchers.IO) {
                    val destino = File(Carpetas.temporal, "elegido.respaldo")
                    contextoDeLaApp.contentResolver.openInputStream(uri)?.use { entrada ->
                        destino.outputStream().use { entrada.copyTo(it) }
                    }
                    destino.absolutePath
                }
                elegir(copia)
            }
        }
    }
    return { lanzador.launch(arrayOf("*/*")) }
}

actual fun versionInstalada(): Pair<String, Long?> {
    val info = contextoDeLaApp.packageManager.getPackageInfo(contextoDeLaApp.packageName, 0)
    return (info.versionName ?: "?") to info.lastUpdateTime
}

@Composable
actual fun rememberSacarFoto(alTerminar: (Boolean) -> Unit): (destino: String) -> Unit {
    val terminar by rememberUpdatedState(alTerminar)
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { terminar(it) }
    return { destino ->
        lanzador.launch(FileProvider.getUriForFile(contextoDeLaApp, autoridadDeFotos, File(destino)))
    }
}
