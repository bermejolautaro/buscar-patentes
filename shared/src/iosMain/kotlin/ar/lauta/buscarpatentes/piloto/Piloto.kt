@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package ar.lauta.buscarpatentes.piloto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import platform.CoreLocation.CLCircularRegion
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.CLRegion
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSISOLatin1StringEncoding
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIDevice
import platform.UIKit.UIViewController
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject

/**
 * Prueba piloto de la 006 (D21). **Temporal**: se borra en la T041.
 *
 * Contesta, en el iPhone del jugador y con el Apple ID gratis, lo que depende de Apple antes de
 * portar nada: que reinstalar conserve la base, que la región avise con la app cerrada, que la
 * ubicación siga con la pantalla bloqueada, que se lea el vencimiento, y que Room con KSP y el
 * mapa de maplibre-compose compilen y corran en iOS con Kotlin 2.4.
 */

// --- Una base de Room mínima: si reinstalar la borra, se para la feature ---

@Entity
data class Marca(@PrimaryKey(autoGenerate = true) val id: Long = 0, val escritaEn: Long)

@Dao
interface MarcaDao {
    @Insert
    suspend fun insertar(marca: Marca)

    @Query("SELECT * FROM Marca ORDER BY id")
    suspend fun todas(): List<Marca>
}

@Database(entities = [Marca::class], version = 1, exportSchema = false)
@ConstructedBy(PilotoBaseConstructor::class)
abstract class PilotoBase : RoomDatabase() {
    abstract fun marcas(): MarcaDao
}

@Suppress("KotlinNoActualForExpect")
expect object PilotoBaseConstructor : RoomDatabaseConstructor<PilotoBase> {
    override fun initialize(): PilotoBase
}

private fun carpetaDeSoporte(): String {
    val ruta = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
        .first() as String
    NSFileManager.defaultManager.createDirectoryAtPath(ruta, true, null, null)
    return ruta
}

private val base: PilotoBase by lazy {
    Room.databaseBuilder<PilotoBase>(name = carpetaDeSoporte() + "/piloto.db")
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

private fun ahora(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

// --- Ubicación: un solo manager, creado al arrancar ---

private object Estado {
    var puntos by mutableStateOf(0)
    var ultimo by mutableStateOf("—")
    var regiones by mutableStateOf(0)
}

private class Delegado : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val ubicacion = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        Estado.puntos += 1
        ubicacion.coordinate.useContents {
            Estado.ultimo = "${latitude.toString().take(9)}, ${longitude.toString().take(9)} " +
                "±${ubicacion.horizontalAccuracy.toInt()} m"
        }
    }

    override fun locationManager(manager: CLLocationManager, didEnterRegion: CLRegion) {
        notificar("Entraste a la región", "Llegó el aviso de ${didEnterRegion.identifier}.")
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        Estado.ultimo = "Error: ${didFailWithError.localizedDescription}"
    }
}

private val delegado = Delegado()
private lateinit var manager: CLLocationManager

/** La llama el AppDelegate, antes que cualquier pantalla (D11). */
fun iniciarPiloto() {
    manager = CLLocationManager().apply {
        delegate = delegado
        desiredAccuracy = kCLLocationAccuracyBest
    }
    Estado.regiones = manager.monitoredRegions.size
}

private fun notificar(titulo: String, texto: String) {
    val contenido = UNMutableNotificationContent().apply {
        setTitle(titulo)
        setBody(texto)
    }
    val pedido = UNNotificationRequest.requestWithIdentifier("piloto-${ahora()}", contenido, null)
    UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(pedido, null)
}

private fun vigilarAca() {
    manager.requestAlwaysAuthorization()
    UNUserNotificationCenter.currentNotificationCenter()
        .requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { _, _ -> }
    val posicion = manager.location ?: run {
        manager.requestLocation()
        Estado.ultimo = "Sin posición todavía: tocá otra vez en unos segundos"
        return
    }
    val region = posicion.coordinate.useContents {
        CLCircularRegion(
            center = posicion.coordinate,
            radius = 150.0,
            identifier = "piloto-${latitude.toString().take(8)}",
        )
    }
    region.notifyOnEntry = true
    region.notifyOnExit = false
    manager.startMonitoringForRegion(region)
    Estado.regiones = manager.monitoredRegions.size + 1
}

private fun grabar() {
    manager.requestAlwaysAuthorization()
    manager.allowsBackgroundLocationUpdates = true
    manager.pausesLocationUpdatesAutomatically = false
    manager.showsBackgroundLocationIndicator = true
    manager.distanceFilter = 10.0
    manager.startUpdatingLocation()
}

// --- Vencimiento: la fecha que Sideloadly mete al firmar (D15) ---

private fun vencimiento(): String {
    val ruta = NSBundle.mainBundle.pathForResource("embedded", "mobileprovision")
        ?: return "sin perfil (¿sin firmar?)"
    val datos = NSData.dataWithContentsOfFile(ruta) ?: return "no se pudo leer el perfil"
    val texto = NSString.create(data = datos, encoding = NSISOLatin1StringEncoding)?.toString()
        ?: return "perfil ilegible"
    return Regex("<key>ExpirationDate</key>\\s*<date>([^<]+)</date>").find(texto)?.groupValues?.get(1)
        ?: "sin ExpirationDate"
}

// --- La pantalla ---

@Composable
private fun PantallaPiloto() {
    val alcance = rememberCoroutineScope()
    var marcas by remember { mutableStateOf("…") }

    suspend fun leerMarcas() {
        val todas = base.marcas().todas()
        marcas = if (todas.isEmpty()) "ninguna" else "${todas.size}: " + todas.joinToString { it.escritaEn.toString() }
    }
    LaunchedEffect(Unit) { leerMarcas() }

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.safeDrawingPadding().padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Prueba piloto — buscar patentes", style = MaterialTheme.typography.titleLarge)
                Text("iOS ${UIDevice.currentDevice.systemVersion}")
                Text("Vence: ${vencimiento()}")
                Text("Marcas en Room: $marcas")
                Button(onClick = {
                    alcance.launch {
                        base.marcas().insertar(Marca(escritaEn = ahora()))
                        leerMarcas()
                    }
                }) { Text("Escribir marca") }
                Text("Regiones vigiladas: ${Estado.regiones}")
                Button(onClick = { vigilarAca() }) { Text("Vigilar acá (150 m)") }
                Text("Puntos grabados: ${Estado.puntos}")
                Text("Última posición: ${Estado.ultimo}")
                Button(onClick = { grabar() }) { Text("Grabar") }
                Box(Modifier.fillMaxWidth().height(260.dp)) {
                    MaplibreMap(
                        state = rememberMapState(
                            baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
                            initialCameraPosition = CameraPosition(
                                target = Position(longitude = -58.3816, latitude = -34.6037),
                                zoom = 14.0,
                            ),
                        ),
                    )
                }
            }
        }
    }
}

fun MainViewController(): UIViewController = ComposeUIViewController { PantallaPiloto() }
