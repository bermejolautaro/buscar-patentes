package ar.lauta.buscarpatentes.plataforma

import android.content.Context
import ar.lauta.buscarpatentes.data.BaseDeDatos
import java.io.File

/** El contexto de la app. Lo fija `App.onCreate`, antes que cualquier otra cosa. */
lateinit var contextoDeLaApp: Context
    private set

fun iniciarPlataforma(context: Context) {
    contextoDeLaApp = context.applicationContext
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
