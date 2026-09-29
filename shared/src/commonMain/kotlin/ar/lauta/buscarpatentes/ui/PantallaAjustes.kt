package ar.lauta.buscarpatentes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.data.AlmacenFotos
import ar.lauta.buscarpatentes.data.EstadoDelJuego
import ar.lauta.buscarpatentes.mapa.CacheDeMapa
import ar.lauta.buscarpatentes.mapa.ConfigMapa
import ar.lauta.buscarpatentes.plataforma.Grabacion
import ar.lauta.buscarpatentes.plataforma.mandarRespaldo
import ar.lauta.buscarpatentes.plataforma.rememberElegirRespaldo
import ar.lauta.buscarpatentes.plataforma.versionInstalada
import ar.lauta.buscarpatentes.respaldo.Respaldo
import ar.lauta.buscarpatentes.respaldo.Vencimiento
import kotlinx.coroutines.launch

/**
 * Ajustes: número del juego (FR-020), interruptor de avisos (FR-040) y espacio del fondo
 * de mapa guardado (FR-045).
 *
 * Los tres viven juntos porque son las tres cosas que el jugador toca de vez en cuando,
 * no en la calle. Nada de acá está en el camino de la carga rápida.
 */
@Composable
fun PantallaAjustes(onVolver: () -> Unit) {
    val alcance = rememberCoroutineScope()

    var numero by remember { mutableStateOf("") }
    var espacioMapa by remember { mutableStateOf<Long?>(null) }
    var espacioFotos by remember { mutableStateOf(0L) }
    var fotosPasadasDeTecho by remember { mutableStateOf(false) }


    // Respaldo (US2 y US3 de la 006). `vuelta` recarga todo después de restaurar: la base es otra.
    var vuelta by remember { mutableStateOf(0) }
    var ultimoRespaldo by remember { mutableStateOf<Long?>(null) }
    var ocupado by remember { mutableStateOf<String?>(null) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var aConfirmar by remember { mutableStateOf<Confirmacion?>(null) }

    fun restaurar(archivo: String, cuentas: Respaldo.Cuentas, conRespaldoPrevio: Boolean) {
        ocupado = "Restaurando…"
        alcance.launch {
            mensaje = try {
                Respaldo.restaurar(archivo, conRespaldoPrevio)
                vuelta++
                // R6.7
                "Listo: ${cuentas.patentes} patentes, ${cuentas.salidas} salidas, ${cuentas.fotos} fotos." +
                    if (cuentas.fotosFaltantes > 0) {
                        " Faltaron ${cuentas.fotosFaltantes} fotos: esas patentes quedan sin foto."
                    } else {
                        ""
                    }
            } catch (e: Exception) {
                "No se pudo restaurar: ${e.message}. Lo de este teléfono quedó como estaba."
            }
            ocupado = null
        }
    }

    val elegirRespaldo = rememberElegirRespaldo { archivo ->
        if (archivo != null) {
            ocupado = "Revisando el respaldo…"
            alcance.launch {
                when (val validacion = Respaldo.validar(archivo)) {
                    is Respaldo.Validacion.Rechazado -> mensaje = validacion.motivo
                    is Respaldo.Validacion.Valido -> {
                        val aca = contenedor.registros.todosUnaVez()
                        // R5: sin patentes no hay nada que perder, y no se pregunta.
                        if (aca.isEmpty()) {
                            restaurar(archivo, validacion.cuentas, conRespaldoPrevio = false)
                            return@launch
                        }
                        aConfirmar = Confirmacion(archivo, validacion.cuentas, aca.size, aca.maxOf { it.capturadoEn })
                    }
                }
                ocupado = null
            }
        }
    }

    // Identidad del APK que está corriendo, para el pie de esta pantalla.
    val (version, instalada) = remember {
        versionInstalada().let { (version, en) -> version to (en?.let(::fechaSinAnio) ?: "?") }
    }

    LaunchedEffect(vuelta) {
        ultimoRespaldo = Respaldo.ultimo()
        val estado = contenedor.estadoDelJuego.leer()
            ?: EstadoDelJuego().also { contenedor.estadoDelJuego.guardar(it) }
        numero = estado.numeroActual.toString()
        espacioMapa = CacheDeMapa.espacioOcupado()
        espacioFotos = contenedor.fotos.espacioOcupado()
        fotosPasadasDeTecho = contenedor.fotos.superoElTecho()
    }

    // Dos de las tres causas del modo oscuro roto se cierran acá (D1):
    // `background` pinta el fondo del tema, que antes no pintaba nadie y dejaba ver la
    // ventana oscura por detrás del texto claro. Va antes de los paddings para que llegue
    // hasta los bordes y no quede una franja sin pintar bajo la barra de estado.
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        BarraSuperior("Ajustes", onVolver)

        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {

            Text(
                "En qué número va el grupo",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                "Lo llevás a mano. El grupo de WhatsApp es la fuente de verdad; la app solo lo espeja.",
                fontSize = 13.sp,
            )
            OutlinedTextField(
                value = numero,
                onValueChange = { nuevo ->
                    if (nuevo.length <= 3 && nuevo.all { it.isDigit() }) {
                        numero = nuevo
                        // US3 escenario 4: cambiar el contador recalcula qué está cubierto.
                        nuevo.toIntOrNull()?.let { n ->
                            alcance.launch { contenedor.estadoDelJuego.fijarNumero(n) }
                        }
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            HorizontalDivider(Modifier.padding(vertical = 20.dp))

            Text("Espacio ocupado", fontWeight = FontWeight.Bold)
            Text("Fondo de mapa guardado: ${enMegas(espacioMapa)} de ${ConfigMapaMb()} MB", fontSize = 14.sp)
            // SC-014: el techo de las fotos se declara y se ve, no se aplica solo. Borrar una
            // foto es tirar evidencia, y esa decisión es del jugador (Principio II).
            Text(
                "Fotos: ${enMegas(espacioFotos)} de ${AlmacenFotos.TECHO_BYTES / 1024 / 1024} MB",
                fontSize = 14.sp,
            )
            if (fotosPasadasDeTecho) {
                Text(
                    "Las fotos pasaron el techo declarado. La app no borra ninguna sola: si " +
                        "querés recuperar espacio, borrá registros ya compartidos desde la ficha de cada patente.",
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                "El fondo de mapa se guarda solo de las zonas por donde pasás, con un techo de " +
                    "${ConfigMapaMb()} MB. Al llegar al límite libera lo menos usado.",
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Button(
                onClick = {
                    alcance.launch {
                        CacheDeMapa.borrar()
                        espacioMapa = CacheDeMapa.espacioOcupado()
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Borrar el fondo de mapa guardado")
            }

            HorizontalDivider(Modifier.padding(vertical = 20.dp))

            // FR-010, FR-013, FR-020: la fecha en que la app deja de abrir y cómo llevarse todo.
            Text("Respaldo", fontWeight = FontWeight.Bold)
            Vencimiento.deEstaInstalacion?.let {
                Text(
                    "La instalación vence el ${fechaConAnio(it)}. Reinstalala antes desde la PC: los datos quedan.",
                    fontSize = 13.sp,
                )
            }
            Text(
                "Último respaldo: ${ultimoRespaldo?.let { fechaConAnio(it) } ?: "nunca"}",
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "Un archivo con todas las patentes, salidas y fotos. Sirve para pasar todo a otro teléfono.",
                fontSize = 13.sp,
            )
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        ocupado = "Sacando el respaldo…"
                        alcance.launch {
                            try {
                                val ruta = Respaldo.sacar()
                                ultimoRespaldo = Respaldo.ultimo()
                                mensaje = null
                                mandarRespaldo(ruta)
                            } catch (e: Exception) {
                                mensaje = "No se pudo sacar el respaldo: ${e.message}"
                            }
                            ocupado = null
                        }
                    },
                ) { Text("Sacar respaldo") }
                OutlinedButton(
                    onClick = {
                        // Cerrar la base con una salida grabando le cortaría los puntos.
                        if (Grabacion.enCurso.value != null) {
                            mensaje = "Terminá la salida antes de restaurar."
                        } else {
                            mensaje = null
                            elegirRespaldo()
                        }
                    },
                ) { Text("Restaurar respaldo") }
            }
            mensaje?.let { Text(it, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp)) }

            // Para saber si el teléfono tiene el APK que se acaba de compilar. Sin esto,
            // "el arreglo no funcionó" y "el arreglo nunca se instaló" son indistinguibles
            // desde acá, y ya nos costó dos vueltas de diagnóstico confundirlos.
            //
            // El dato sale del sistema, no de una constante generada en el build: un
            // `buildConfigField` con la hora de compilación queda congelado por la caché de
            // configuración de Gradle, y un sello que miente es peor que no tener ninguno.
            Text(
                "Versión $version · instalada $instalada",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }

    // FR-018: mientras dura, no se toca nada. Con la base cerrada a mitad de restaurar, cualquier
    // toque que escriba algo cerraría la app. El diálogo tapa la pantalla y el gesto de volver.
    ocupado?.let { texto ->
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        ) {
            Surface(shape = MaterialTheme.shapes.large) {
                Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text(texto, modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }

    // R5
    aConfirmar?.let { c ->
        AlertDialog(
            onDismissRequest = { aConfirmar = null },
            title = { Text("¿Restaurar el respaldo?") },
            text = {
                Text(
                    "En este teléfono: ${c.patentesAca} patentes, la última del ${fechaConAnio(c.ultimaAca)}.\n" +
                        "En el respaldo: ${c.respaldo.patentes} patentes" +
                        (c.respaldo.ultimaCaptura?.let { ", la última del ${fechaConAnio(it)}" } ?: "") + ".\n\n" +
                        "Restaurar reemplaza todo lo de este teléfono. Antes se guarda un respaldo de lo que hay.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    aConfirmar = null
                    restaurar(c.archivo, c.respaldo, conRespaldoPrevio = true)
                }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { aConfirmar = null }) { Text("Cancelar") } },
        )
    }
}

private class Confirmacion(
    val archivo: String,
    val respaldo: Respaldo.Cuentas,
    val patentesAca: Int,
    val ultimaAca: Long,
)

private fun enMegas(bytes: Long?): String =
    if (bytes == null) "…" else "${decimales(bytes / 1024.0 / 1024.0, 1)} MB"

private fun ConfigMapaMb(): Long = ConfigMapa.CACHE_MAXIMO_BYTES / 1024 / 1024
