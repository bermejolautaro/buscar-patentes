@file:OptIn(ExperimentalNativeApi::class)

package ar.lauta.buscarpatentes

import androidx.compose.ui.window.ComposeUIViewController
import ar.lauta.buscarpatentes.data.AlmacenFotos
import ar.lauta.buscarpatentes.data.construirBase
import ar.lauta.buscarpatentes.mapa.ConfigMapa
import ar.lauta.buscarpatentes.plataforma.Carpetas
import ar.lauta.buscarpatentes.plataforma.Ubicacion
import ar.lauta.buscarpatentes.ui.AppBuscarPatentes
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import org.maplibre.compose.map.DefaultMapRuntime
import org.maplibre.compose.map.MapRuntimeOptions
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIViewController

/**
 * El arranque del iPhone. Lo llama el `AppDelegate` en `didFinishLaunching`, antes que cualquier
 * pantalla: iOS relanza la app en segundo plano al entrar a una región vigilada, sin mostrar nada,
 * y lo que atienda ese evento tiene que existir ya (D11 de la 006).
 */
fun iniciar() {
    // Sin Mac no hay consola: el último fallo de Kotlin queda en Archivos, en `fallo.txt`.
    setUnhandledExceptionHook { error ->
        SystemFileSystem.sink(archivoDeFallo()).buffered().use { it.writeString(error.stackTraceToString()) }
    }

    // El caché de teselas, en la carpeta de la base y con su techo (FR-044), como en el Android.
    // Tiene que ir antes del primer mapa: después el motor ya existe.
    DefaultMapRuntime.configure(
        MapRuntimeOptions(
            cacheFile = Path(ConfigMapa.rutaCache),
            maximumCacheSizeBytes = ConfigMapa.CACHE_MAXIMO_BYTES,
        ),
    )

    iniciarContenedor(construirBase(), AlmacenFotos(Carpetas.fotos))

    // Core Location entrega lo que mide en el hilo donde se creó el manager: este, el principal.
    Ubicacion.manager
}

fun MainViewController(): UIViewController = ComposeUIViewController { AppBuscarPatentes() }

private fun archivoDeFallo(): Path = Path(
    NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String,
    "fallo.txt",
)
