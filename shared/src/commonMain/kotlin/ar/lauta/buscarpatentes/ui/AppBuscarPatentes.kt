package ar.lauta.buscarpatentes.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import ar.lauta.buscarpatentes.plataforma.programarAvisoDeVencimiento
import ar.lauta.buscarpatentes.respaldo.Vencimiento

/**
 * Navegacion a mano entre tres pantallas, la misma en los dos teléfonos (D9 de la 006).
 *
 * Sin Navigation-Compose: tres destinos sin argumentos ni deep links no justifican una
 * dependencia mas ni un grafo. Principio IV. Si aparece un destino con argumentos, ahi se
 * agrega.
 *
 * Eran cuatro hasta la 005: la pantalla de busqueda se retiro (FR-013), y su trabajo lo hace la
 * barra de arriba de la principal.
 */
private enum class Destino { PRINCIPAL, RECORRIDOS, ZONAS, AJUSTES }

@OptIn(ExperimentalComposeUiApi::class) // el BackHandler de Compose Multiplatform
@Composable
fun AppBuscarPatentes() {
    TemaBuscarPatentes {
        var destino by remember { mutableStateOf(Destino.PRINCIPAL) }

        // FR-012: el aviso de "vence mañana", programado de nuevo en cada arranque.
        LaunchedEffect(Unit) { Vencimiento.deEstaInstalacion?.let(::programarAvisoDeVencimiento) }

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
                onZonas = { destino = Destino.ZONAS },
            )
            Destino.RECORRIDOS -> PantallaRecorridos(
                onVolver = { destino = Destino.PRINCIPAL },
            )
            Destino.ZONAS -> PantallaZonas(
                onVolver = { destino = Destino.PRINCIPAL },
            )
            Destino.AJUSTES -> PantallaAjustes(
                onVolver = { destino = Destino.PRINCIPAL },
            )
        }
    }
}

/** El alto de la ventana, para las listas que no pueden pasar de una fracción de pantalla. */
@Composable
internal fun altoDePantalla(): Dp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
