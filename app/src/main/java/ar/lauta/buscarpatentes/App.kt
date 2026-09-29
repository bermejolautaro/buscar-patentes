package ar.lauta.buscarpatentes

import android.app.Application
import android.content.Context
import ar.lauta.buscarpatentes.data.AlmacenFotos
import ar.lauta.buscarpatentes.data.BaseDeDatos
import ar.lauta.buscarpatentes.ubicacion.Geofences
import ar.lauta.buscarpatentes.ubicacion.LectorUbicacion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Contenedor manual de dependencias (D7).
 *
 * Sin Hilt ni Koin: un módulo, una base, un almacén de fotos y un lector de ubicación.
 * El Principio IV dice que un framework de inyección acá es infraestructura que nadie
 * pidió. Si algún día construir esto a mano duele de verdad, se agrega el framework
 * entonces.
 */
class Contenedor(context: Context) {
    private val app = context.applicationContext

    val baseDeDatos: BaseDeDatos by lazy { BaseDeDatos.obtener(app) }
    val registros by lazy { baseDeDatos.registros() }
    val estadoDelJuego by lazy { baseDeDatos.estadoDelJuego() }
    val recorridos by lazy { baseDeDatos.recorridos() }
    val puntos by lazy { baseDeDatos.puntos() }
    val votos by lazy { baseDeDatos.votos() }
    val fotos: AlmacenFotos by lazy { AlmacenFotos(app) }
    val ubicacion: LectorUbicacion by lazy { LectorUbicacion(app) }
    val geofences: Geofences by lazy { Geofences(app) }
}

class App : Application() {

    lateinit var contenedor: Contenedor
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = Contenedor(this)

        CoroutineScope(Dispatchers.IO).launch {
            // T073 / FR-033: un recorrido EN_CURSO cuando el proceso recien arranca es de un
            // servicio que ya no existe — si estuviera vivo, el proceso no se habria vuelto a
            // crear. Se cierra como TERMINADO conservando los puntos que alcanzo a juntar,
            // que es exactamente lo que FR-033 pide: nada de descartar el tramo.
            contenedor.recorridos.cerrarLosAbiertos(System.currentTimeMillis())

            // T058 / C2: Android borra los geofences al reiniciar el telefono y al apagar la
            // ubicacion. Reconciliar en cada arranque es la red de seguridad del BootReceiver,
            // y por eso da todo de alta de nuevo en vez de confiar en lo que quedo anotado:
            // si el proceso recien arranca, lo anotado puede ser de antes del reinicio.
            contenedor.geofences.reconciliar(elSistemaLosOlvido = true)
        }
    }
}

/** Acceso al contenedor desde cualquier Context. */
val Context.contenedor: Contenedor
    get() = (applicationContext as App).contenedor
