package ar.lauta.buscarpatentes.plataforma

import androidx.compose.runtime.Composable
import ar.lauta.buscarpatentes.ui.Ir
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

/*
 * La costura del iPhone (contrato P de la 006). Lo que todavía no existe tira NotImplementedError
 * con la story que lo implementa. Lo que las pantallas llaman solas, sin que el jugador lo pida,
 * no tira: no hace nada, así la app abre igual.
 */

actual object Grabacion {
    actual val enCurso: StateFlow<Long?> = MutableStateFlow(null)
    actual val puntosGuardados: StateFlow<Int> = MutableStateFlow(0)

    actual fun empezar(): Unit = throw NotImplementedError("US6 (T068): grabar la salida")

    actual fun terminar(): Unit = throw NotImplementedError("US6 (T068): grabar la salida")
}

actual object Vigilancia {
    // Se llama después de cada captura: sin avisos todavía, no vigila nada (US5, T062).
    actual suspend fun reconciliar(elSistemaLosOlvido: Boolean) {}
}

actual fun abrirPunto(latitud: Double, longitud: Double, etiqueta: String): Boolean =
    throw NotImplementedError("US4 (T060): abrir el lugar en mapas")

actual fun abrirRecorrido(url: String, paradas: Int): Ir.ResultadoRecorrido =
    throw NotImplementedError("US4 (T060): abrir el recorrido en Maps")

actual fun abrirAjustesDelSistema() {
    val ajustes = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
    UIApplication.sharedApplication.openURL(ajustes, emptyMap<Any?, Any>(), null)
}

actual fun compartir(texto: String, foto: String?): Unit = throw NotImplementedError("US4 (T059): compartir")

// Sin `postear` no hay a quién preguntarle: el ajuste a calles queda pendiente, como sin red (US6, T070).
actual fun hayConexion(): Boolean = false

actual suspend fun postear(url: String, cuerpo: String): String? = throw NotImplementedError("US6 (T070): postear")

actual fun versionInstalada(): Pair<String, Long?> =
    (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "?") to null

@Composable
actual fun rememberSacarFoto(alTerminar: (Boolean) -> Unit): (destino: String) -> Unit =
    { throw NotImplementedError("US4 (T058): sacar foto") }
