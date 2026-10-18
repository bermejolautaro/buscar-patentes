package ar.lauta.buscarpatentes.plataforma

import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.data.PuntoDeTrayecto
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject

/**
 * La salida del iPhone, con la pantalla bloqueada (P3, D12 de la 006).
 *
 * Lo que en el Android es un servicio en primer plano, en el iPhone es un `CLLocationManager` con
 * `allowsBackgroundLocationUpdates`: si arranca con la app adelante, sigue con la pantalla apagada
 * aun con el permiso "mientras se usa", y la barra de estado muestra el indicador azul. Tiene su
 * propio manager: la captura apaga el suyo cada vez que termina de leer.
 *
 * Si el sistema mata la app, o el jugador la cierra deslizándola, la grabación muere con ella. Lo
 * guardado queda, y al abrir la app la salida se cierra y se avisa (FR-028).
 *
 * Desde la 010 también se avisa en el momento, con una notificación que la app posterga mientras
 * graba: si la app muere, deja de postergarla y suena sola (D4 y contrato C5 de la 010).
 */
actual object Grabacion {
    private val enCursoMutable = MutableStateFlow<Long?>(null)
    private val puntosMutable = MutableStateFlow(0)

    actual val enCurso: StateFlow<Long?> = enCursoMutable
    actual val puntosGuardados: StateFlow<Int> = puntosMutable

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val delegado = Delegado()

    /** El ciclo que posterga el aviso de la salida cortada. Null sin salida. */
    private var postergador: Job? = null

    /** Se crea en el hilo principal, donde Core Location entrega lo que mide. */
    private val manager: CLLocationManager by lazy {
        CLLocationManager().apply {
            delegate = delegado
            desiredAccuracy = kCLLocationAccuracyBest
            distanceFilter = DISTANCIA_MINIMA_M
            pausesLocationUpdatesAutomatically = false
        }
    }

    actual fun empezar() {
        if (enCursoMutable.value != null) return
        alcance.launch {
            val id = contenedor.recorridos.iniciar(ahora())
            withContext(Dispatchers.Main) {
                enCursoMutable.value = id
                manager.allowsBackgroundLocationUpdates = true
                manager.showsBackgroundLocationIndicator = true
                manager.startUpdatingLocation()
            }
            postergador = alcance.launch {
                // El `finally` corre después de la última reprogramación, aunque `terminar` llegue
                // en el medio de una: así no queda ningún aviso pendiente de una salida terminada.
                // Si la app muere, no corre, y el aviso suena: es lo que se busca.
                try {
                    while (enCursoMutable.value == id) {
                        postergarAviso()
                        delay(CADA_CUANTO_SE_POSTERGA_MS)
                    }
                } finally {
                    borrarAviso(entregado = true)
                }
            }
        }
    }

    actual fun terminar() {
        // FR-013 de la 010: terminar con el botón no deja ningún aviso.
        postergador?.cancel()
        postergador = null
        borrarAviso(entregado = true)
        manager.stopUpdatingLocation()
        manager.allowsBackgroundLocationUpdates = false
        enCursoMutable.value = null
        alcance.launch { contenedor.recorridos.cerrarLosAbiertos(ahora()) }
    }

    /** Cada punto va a la base apenas llega, como en el Android: si la app muere, lo caminado queda. */
    @OptIn(ExperimentalForeignApi::class)
    private fun guardar(posiciones: List<CLLocation>) {
        val id = enCursoMutable.value ?: return
        val puntos = posiciones
            // FR-036: un punto que el teléfono declara malo miente sobre por dónde se pasó.
            .filter { it.horizontalAccuracy >= 0 && it.horizontalAccuracy <= PRECISION_MAXIMA_M }
            .map { posicion ->
                posicion.coordinate.useContents {
                    PuntoDeTrayecto(
                        recorridoId = id,
                        latitud = latitude,
                        longitud = longitude,
                        precisionMetros = posicion.horizontalAccuracy.toFloat(),
                        // La hora del GPS: con la pantalla apagada iOS puede entregarlos juntos.
                        registradoEn = (posicion.timestamp.timeIntervalSince1970 * 1000).toLong(),
                    )
                }
            }
        if (puntos.isEmpty()) return
        alcance.launch {
            puntos.forEach { contenedor.puntos.insertar(it) }
            puntosMutable.value += puntos.size
        }
    }

    /**
     * Corre el aviso para dentro de [SUENA_EN_S]: con el mismo identificador, reemplaza al pendiente.
     * Si uno llegó a sonar con la app viva —el sistema la frenó un rato sin matarla—, se borra:
     * la salida no se cortó (FR-013 de la 010).
     */
    private fun postergarAviso() {
        val centro = UNUserNotificationCenter.currentNotificationCenter()
        centro.removeDeliveredNotificationsWithIdentifiers(listOf(AVISO))
        val contenido = UNMutableNotificationContent().apply {
            setTitle("Se cortó la salida")
            setBody("La app se cerró y dejó de grabar. Abrila para empezar otra salida.")
        }
        val cuando = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(SUENA_EN_S, repeats = false)
        centro.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(AVISO, contenido, cuando), null)
    }

    /**
     * Borra el aviso pendiente, y con [entregado] también el que ya sonó. Al arrancar se borra solo
     * el pendiente: si la app volvió antes de que suene, no tiene que sonar después, y uno que ya
     * sonó sigue diciendo lo que pasó.
     */
    fun borrarAviso(entregado: Boolean) {
        val centro = UNUserNotificationCenter.currentNotificationCenter()
        centro.removePendingNotificationRequestsWithIdentifiers(listOf(AVISO))
        if (entregado) centro.removeDeliveredNotificationsWithIdentifiers(listOf(AVISO))
    }

    private class Delegado : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            Grabacion.guardar(didUpdateLocations.filterIsInstance<CLLocation>())
        }
    }

    /** Contrato C5 de la 010: se posterga cada 30 s, a 75 s. Si la app muere, suena antes de 2 min. */
    private const val AVISO = "salida-cortada"
    private const val CADA_CUANTO_SE_POSTERGA_MS = 30_000L
    private const val SUENA_EN_S = 75.0

    /** Los mismos que `ServicioRecorrido` del Android (P3). */
    private const val DISTANCIA_MINIMA_M = 10.0
    private const val PRECISION_MAXIMA_M = 30.0
}
