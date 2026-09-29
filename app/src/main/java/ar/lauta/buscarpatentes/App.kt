package ar.lauta.buscarpatentes

import android.app.Application
import ar.lauta.buscarpatentes.data.AlmacenFotos
import ar.lauta.buscarpatentes.data.construirBase
import ar.lauta.buscarpatentes.plataforma.Carpetas
import ar.lauta.buscarpatentes.plataforma.Vigilancia
import ar.lauta.buscarpatentes.plataforma.iniciarPlataforma
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        iniciarPlataforma(this)
        iniciarContenedor({ construirBase(this) }, AlmacenFotos(Carpetas.fotos))

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
            Vigilancia.reconciliar(elSistemaLosOlvido = true)
        }
    }
}
