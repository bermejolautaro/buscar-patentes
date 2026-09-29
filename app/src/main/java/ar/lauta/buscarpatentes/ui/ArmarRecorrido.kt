package ar.lauta.buscarpatentes.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.lauta.buscarpatentes.data.RegistroDeCaptura
import ar.lauta.buscarpatentes.ui.Ir.AccionRecorrido
import ar.lauta.buscarpatentes.ui.Ir.ResultadoRecorrido

/** Una parada del recorrido que se está armando (data-model de la 005). */
data class Parada(
    val registro: RegistroDeCaptura,
    /** "A 320 m al noreste", para decidir si vale la pena incluirla (FR-018). */
    val distanciaYRumbo: String,
    val confianza: Int,
    val incluida: Boolean = true,
)

/**
 * Armar un recorrido por varias patentes de un número y abrirlo en Google Maps (US4 de la 005,
 * D10, C3).
 *
 * **Flechas y no arrastrar** para cambiar el orden (FR-017): arrastrar en una lista de Compose
 * no viene hecho, y con las tres o cuatro paradas de un número, una flecha es un toque por
 * posición y cero código de gestos.
 *
 * Sacar una parada no la mueve de lugar (FR-016): si el jugador se arrepiente, vuelve a estar
 * donde estaba.
 *
 * El recorrido **no se guarda** (FR-022) y **no arranca ninguna salida** (FR-021): planear a
 * dónde ir y grabar por dónde se fue son cosas separadas. Cerrar el diálogo lo tira.
 *
 * Los avisos van adentro del diálogo y no en el snackbar de la pantalla: el diálogo oscurece lo
 * que tiene detrás, y un aviso ahí abajo se perdería justo cuando hay que leerlo.
 */
@Composable
fun ArmarRecorrido(
    numero: Int,
    /** En el orden propuesto por `Prioridad.ordenDeParadas`, todas incluidas (FR-015). */
    inicial: List<Parada>,
    onCerrar: () -> Unit,
) {
    val context = LocalContext.current
    var paradas by remember { mutableStateOf(inicial) }
    var aviso by remember { mutableStateOf<String?>(null) }

    val incluidas = paradas.filter { it.incluida }
    val accion = Ir.accionPara(incluidas.size)

    fun mover(desde: Int, hasta: Int) {
        paradas = paradas.toMutableList().apply { add(hasta, removeAt(desde)) }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Recorrido por las %03d".format(numero)) },
        text = {
            Column {
                LazyColumn(
                    Modifier.heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.5f),
                ) {
                    itemsIndexed(paradas, key = { _, p -> p.registro.id }) { i, parada ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = parada.incluida,
                                onCheckedChange = { marcada ->
                                    paradas = paradas.toMutableList()
                                        .also { it[i] = parada.copy(incluida = marcada) }
                                    aviso = null
                                },
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    parada.registro.patenteTexto
                                        ?: "%03d".format(parada.registro.numero),
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "${parada.distanciaYRumbo} · conf. ${parada.confianza}",
                                    fontSize = 12.sp,
                                )
                            }
                            IconButton(onClick = { mover(i, i - 1) }, enabled = i > 0) {
                                Text("▲")
                            }
                            IconButton(
                                onClick = { mover(i, i + 1) },
                                enabled = i < paradas.lastIndex,
                            ) { Text("▼") }
                        }
                    }
                }

                // FR-019b: con más de las que entran se dice cuántas sacar, antes de tocar
                // nada. El botón queda deshabilitado: abrir cortaría el recorrido en silencio.
                val sobran = accion as? AccionRecorrido.Sobran
                val texto = sobran?.let {
                    "Google Maps acepta hasta ${Ir.MAXIMO_PARADAS_APP} paradas: sacá ${it.cuantas}."
                } ?: aviso
                if (texto != null) {
                    Text(
                        texto,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = accion == AccionRecorrido.IrALaUnica || accion == AccionRecorrido.AbrirMaps,
                onClick = {
                    when (accion) {
                        // FR-020: una sola parada es "Ir".
                        AccionRecorrido.IrALaUnica -> {
                            if (Ir.aLaPatente(context, incluidas.first().registro)) {
                                onCerrar()
                            } else {
                                aviso = "No hay ninguna app de mapas que pueda abrir el lugar."
                            }
                        }
                        AccionRecorrido.AbrirMaps -> {
                            val puntos = incluidas.map { it.registro.latitud to it.registro.longitud }
                            when (Ir.enGoogleMaps(context, puntos)) {
                                ResultadoRecorrido.ABIERTO -> onCerrar()
                                ResultadoRecorrido.DEMASIADAS_PARA_EL_NAVEGADOR ->
                                    aviso = "Google Maps no está instalado, y en el navegador " +
                                        "entran ${Ir.MAXIMO_PARADAS_NAVEGADOR} paradas: sacá " +
                                        "${incluidas.size - Ir.MAXIMO_PARADAS_NAVEGADOR}."
                                ResultadoRecorrido.SIN_APP ->
                                    aviso = "No hay ninguna app que pueda abrir el recorrido."
                            }
                        }
                        AccionRecorrido.Nada, is AccionRecorrido.Sobran -> Unit
                    }
                },
            ) { Text("Abrir en Google Maps") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}
