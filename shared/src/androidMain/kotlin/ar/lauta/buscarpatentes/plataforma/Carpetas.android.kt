package ar.lauta.buscarpatentes.plataforma

import android.content.Context
import ar.lauta.buscarpatentes.data.BaseDeDatos
import ar.lauta.buscarpatentes.mapa.ConfigMapa
import java.io.File
import kotlinx.io.files.Path
import org.maplibre.compose.map.DefaultMapRuntime
import org.maplibre.compose.map.MapRuntimeOptions

/** El contexto de la app. Lo fija `App.onCreate`, antes que cualquier otra cosa. */
lateinit var contextoDeLaApp: Context
    private set

fun iniciarPlataforma(context: Context) {
    contextoDeLaApp = context.applicationContext

    // El caché de teselas del mapa, en una carpeta que el sistema no vacía y con su techo
    // (FR-044). Tiene que ir antes del primer mapa: después el runtime ya existe.
    DefaultMapRuntime.configure(
        MapRuntimeOptions(
            cacheFile = Path(ConfigMapa.rutaCache),
            maximumCacheSizeBytes = ConfigMapa.CACHE_MAXIMO_BYTES,
        ),
    )
    // El caché del MapLibre Android SDK de antes de la 006 ya no lo lee nadie.
    File(contextoDeLaApp.filesDir, "mbgl-offline.db").delete()
}

actual object Carpetas {
    actual val base: String
        get() = contextoDeLaApp.getDatabasePath(BaseDeDatos.ARCHIVO).parentFile!!.absolutePath

    actual val fotos: String
        get() = carpeta(contextoDeLaApp.filesDir, "fotos")

    /** `filesDir` y no la caché: el sistema no vacía esto, y ahí queda el respaldo previo (P1). */
    actual val respaldos: String
        get() = carpeta(contextoDeLaApp.filesDir, "respaldos")

    actual val temporal: String
        get() = contextoDeLaApp.cacheDir.absolutePath

    private fun carpeta(padre: File, nombre: String): String =
        File(padre, nombre).apply { if (!exists()) mkdirs() }.absolutePath
}
