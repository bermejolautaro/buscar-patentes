package ar.lauta.buscarpatentes.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/** La única actividad: lo que muestra es la app común, [AppBuscarPatentes] (D9 de la 006). */
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
        setContent { AppBuscarPatentes() }
    }
}
