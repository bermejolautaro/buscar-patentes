package ar.lauta.buscarpatentes.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Navegacion a mano entre tres pantallas.
 *
 * Sin Navigation-Compose: tres destinos sin argumentos ni deep links no justifican una
 * dependencia mas ni un grafo. Principio IV. Si aparece un destino con argumentos, ahi se
 * agrega.
 *
 * Eran cuatro hasta la 005: la pantalla de busqueda se retiro (FR-013), y su trabajo lo hace la
 * barra de arriba de la principal.
 */
private enum class Destino { PRINCIPAL, RECORRIDOS, AJUSTES }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // FR-015: sin esto la barra de navegacion del sistema queda blanca con iconos
        // blancos, o sea invisible.
        //
        // Desde targetSdk 35 Android la dibuja transparente si o si, y el tinte de sus
        // iconos lo decide `isAppearanceLightNavigationBars`. Nadie lo estaba fijando: se
        // quedaba en lo que trae el tema de plataforma, que en modo claro son iconos claros
        // sobre el fondo claro de la app. `enableEdgeToEdge` instala el comportamiento que
        // el resto de las apps tiene, y lo ajusta solo al cambiar de modo.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TemaBuscarPatentes {
                var destino by remember { mutableStateOf(Destino.PRINCIPAL) }

                // FR-004: el gesto de retroceso del sistema vuelve a la pantalla principal.
                // La navegacion es a mano, asi que sin esto el gesto cerraba la app desde
                // cualquier pantalla en vez de retroceder, que es lo que el jugador espera.
                BackHandler(enabled = destino != Destino.PRINCIPAL) {
                    destino = Destino.PRINCIPAL
                }

                when (destino) {
                    Destino.PRINCIPAL -> PantallaPrincipal(
                        onAjustes = { destino = Destino.AJUSTES },
                        onRecorridos = { destino = Destino.RECORRIDOS },
                    )
                    Destino.RECORRIDOS -> PantallaRecorridos(
                        onVolver = { destino = Destino.PRINCIPAL },
                    )
                    Destino.AJUSTES -> PantallaAjustes(
                        onVolver = { destino = Destino.PRINCIPAL },
                    )
                }
            }
        }
    }
}
