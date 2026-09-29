package ar.lauta.buscarpatentes.ui

import ar.lauta.buscarpatentes.R
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource

/**
 * La barra superior de las pantallas secundarias (FR-003, FR-004).
 *
 * Existe por el re-diagnóstico de D2. El reporte original decía que el botón "Volver"
 * estaba "tan arriba que ni se ve y casi tapado por la barra de notificaciones", y el
 * primer arreglo fue de insets. Pero ese reporte se hizo sobre una versión que **ya tenía**
 * el inset aplicado: el control no quedaba *debajo* de la barra de estado, quedaba
 * *pegado* a ella.
 *
 * El problema no era la posición sino la ausencia de estructura: las tres pantallas
 * arrancaban con una fila de título y un `TextButton` de texto, sin altura, sin aire y sin
 * jerarquía. Un control bien posicionado pero apretado contra el borde sigue siendo
 * incómodo, y por eso arreglar los insets no cambió la percepción.
 *
 * Dos usos reales —Salidas y Ajustes—, así que el Principio IV la admite: no es una
 * abstracción especulativa, es la que evita repetir el mismo encabezado. Eran tres hasta la
 * 005, que retiró la pantalla de búsqueda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperior(titulo: String, onVolver: () -> Unit) {
    TopAppBar(
        title = { Text(titulo) },
        navigationIcon = {
            IconButton(onClick = onVolver) {
                Icon(
                    painter = painterResource(R.drawable.ic_volver),
                    contentDescription = "Volver",
                )
            }
        },
    )
}
