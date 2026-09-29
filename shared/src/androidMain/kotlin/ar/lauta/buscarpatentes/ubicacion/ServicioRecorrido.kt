package ar.lauta.buscarpatentes.ubicacion

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.data.PuntoDeTrayecto
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Graba por dónde pasa el jugador mientras hay un recorrido en curso (C3, FR-023 a FR-026).
 *
 * Es un foreground service porque FR-026 exige seguir registrando con la pantalla apagada,
 * y desde Android 8 esa es la única forma honesta de pedirlo. La notificación persistente
 * es a la vez una imposición del sistema y el requisito FR-023: mostrar de forma visible
 * que hay un recorrido en curso.
 *
 * **El muestreo es fino** (FR-036): `PRIORITY_HIGH_ACCURACY` cada 5 s o 10 m, descartando
 * los puntos que llegan con precisión peor que [PRECISION_MAXIMA_M]. La versión anterior
 * muestreaba barato —prioridad balanceada, 30 s, 50 m— porque el trazo solo alimentaba una
 * grilla de celdas: bastaba con saber en qué cuadra se había estado. Desde que el trazo se
 * dibuja como camino, esa configuración lo volvía inservible: la prioridad balanceada
 * resuelve por antenas y wifi con decenas de metros de error, y con una manzana midiendo
 * cien, el camino salía sobre la calle de al lado. Y un punto cada 50 m se saltea las
 * esquinas, así que el trazo cortaba diagonal por dentro de las manzanas.
 */
class ServicioRecorrido : Service() {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cliente by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private var recorridoId = 0L

    private val escucha = object : LocationCallback() {
        override fun onLocationResult(resultado: LocationResult) {
            val id = recorridoId
            if (id == 0L) return

            // FR-033 / C3: cada punto se escribe a Room apenas llega. Nada se acumula en
            // memoria, así que si el sistema mata el proceso lo ya recorrido sobrevive.
            for (posicion in resultado.locations) {
                // FR-036: un punto que el teléfono mismo declara malo miente sobre por dónde
                // se pasó. Se descarta acá y no al dibujar: lo que no sirve no se guarda.
                if (posicion.accuracy > PRECISION_MAXIMA_M) continue

                alcance.launch {
                    contenedor.puntos.insertar(
                        PuntoDeTrayecto(
                            recorridoId = id,
                            latitud = posicion.latitude,
                            longitud = posicion.longitude,
                            precisionMetros = posicion.accuracy,
                            registradoEn = System.currentTimeMillis(),
                        ),
                    )
                    // FR-008: avisar que hay un punto más, para que el mapa lo dibuje
                    // mientras el jugador camina y no recién al terminar la salida.
                    puntosGuardados.value++
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Se llama siempre y primero, incluso en el camino de terminar: el sistema mata la
        // app si un servicio arrancado con startForegroundService no lo hace en 5 segundos.
        ServiceCompat.startForeground(this, ID_NOTIFICACION, notificacion(), tipoDeServicio())

        if (intent?.action == ACCION_TERMINAR) {
            terminarAhora()
            return START_NOT_STICKY
        }

        if (recorridoId == 0L) empezarAhora()

        // START_NOT_STICKY a propósito: si el sistema mata el servicio, no queremos que
        // reviva solo y abra un recorrido nuevo. El trayecto juntado queda en la base, y al
        // reabrir la app el recorrido se cierra como TERMINADO (FR-033, T073).
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun empezarAhora() {
        alcance.launch {
            val id = contenedor.recorridos.iniciar(System.currentTimeMillis())
            recorridoId = id
            enCurso.value = id

            val pedido = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVALO_MS)
                .setMinUpdateDistanceMeters(DISTANCIA_MINIMA_M)
                .build()
            cliente.requestLocationUpdates(pedido, escucha, Looper.getMainLooper())
        }
    }

    private fun terminarAhora() {
        cliente.removeLocationUpdates(escucha)
        recorridoId = 0L
        enCurso.value = null

        // Alcance aparte, no [alcance]: `stopSelf` dispara `onDestroy`, que lo cancela, y
        // el cierre del recorrido se perdería a mitad de camino.
        CoroutineScope(Dispatchers.IO).launch {
            contenedor.recorridos.cerrarLosAbiertos(System.currentTimeMillis())
        }

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        cliente.removeLocationUpdates(escucha)
        enCurso.value = null
        alcance.cancel()
        super.onDestroy()
    }

    /** FR-023: mientras dura el recorrido esto está a la vista, y no se puede descartar. */
    private fun notificacion(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CANAL, "Recorrido en curso", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "Se ve mientras la app anota por dónde vas." },
            )
        }

        val volver = PendingIntent.getActivity(
            this,
            0,
            // La actividad de arranque, sin nombrarla: vive en el módulo de la app.
            packageManager.getLaunchIntentForPackage(packageName)!!,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val terminar = PendingIntent.getService(
            this,
            1,
            Intent(this, ServicioRecorrido::class.java).setAction(ACCION_TERMINAR),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Recorrido en curso")
            .setContentText("Anotando por dónde pasás. Podés apagar la pantalla.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(volver)
            .addAction(0, "Terminar", terminar)
            .build()
    }

    private fun tipoDeServicio(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }

    companion object {

        /**
         * El recorrido en curso, o null.
         *
         * La pantalla principal lo lee para saber si el botón dice "Empezar recorrido" o
         * "Terminar recorrido", y [ar.lauta.buscarpatentes.ui.Captura], por `Grabacion`, para asociarle las
         * capturas (FR-027). Vive en memoria y no en la base porque si el proceso muere el
         * servicio muere con él: no hay estado que recuperar, hay un recorrido que cerrar.
         */
        val enCurso = MutableStateFlow<Long?>(null)

        /**
         * Cuántos puntos lleva grabados el proceso. Sube con cada uno que se escribe (FR-008).
         *
         * Es una señal, no una cuenta con significado: nadie la muestra, y no arranca de
         * cero para un recorrido nuevo. Existe porque [enCurso] cambia solo al empezar y al
         * terminar, así que un mapa que dependiera únicamente de él dibujaría el camino como
         * estaba al empezar —o sea, vacío— hasta que la salida terminara.
         *
         * Un contador y no un `Flow` de los puntos: quien lo escucha ya sabe leerlos de la
         * base, y lo único que le falta es enterarse de que hay uno más. Vive en memoria
         * junto a [enCurso] y por la misma razón: si el proceso muere, el trayecto está en
         * la base y la pantalla lo relee al abrir.
         */
        val puntosGuardados = MutableStateFlow(0)

        fun empezar(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ServicioRecorrido::class.java),
            )
        }

        fun terminar(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ServicioRecorrido::class.java).setAction(ACCION_TERMINAR),
            )
        }

        private const val ACCION_TERMINAR = "ar.lauta.buscarpatentes.TERMINAR_RECORRIDO"
        private const val CANAL = "recorrido_en_curso"
        private const val ID_NOTIFICACION = 1

        /** FR-036: uno cada 5 s, y solo si se movió 10 m. Caminando eso es cada 7 s. */
        private const val INTERVALO_MS = 5_000L

        /**
         * 10 m: lo bastante corto para que una esquina caiga entre dos puntos y el trazo la
         * doble en vez de cortarla en diagonal. Quedarse quieto sigue sin agregar puntos.
         * ponytail: calibrable; bajarlo agrega puntos y ruido, subirlo devuelve las diagonales.
         */
        private const val DISTANCIA_MINIMA_M = 10f

        /**
         * Precisión peor que esto y el punto se tira (FR-036).
         *
         * 30 m es alrededor de un cuarto de manzana: por encima de eso el punto ya no dice
         * en qué calle se estaba. Es el mismo criterio que el aviso de precisión degradada
         * de una captura, con el umbral más flojo que corresponde a un dato de contexto.
         * ponytail: si en zonas con edificios altos el trazo sale con huecos, aflojarlo.
         */
        private const val PRECISION_MAXIMA_M = 30f
    }
}
