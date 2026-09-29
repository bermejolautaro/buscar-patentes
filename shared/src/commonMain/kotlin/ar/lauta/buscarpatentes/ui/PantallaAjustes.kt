package ar.lauta.buscarpatentes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.data.AlmacenFotos
import ar.lauta.buscarpatentes.data.EstadoDelJuego
import ar.lauta.buscarpatentes.mapa.CacheDeMapa
import ar.lauta.buscarpatentes.mapa.ConfigMapa
import ar.lauta.buscarpatentes.plataforma.Permiso
import ar.lauta.buscarpatentes.plataforma.Vigilancia
import ar.lauta.buscarpatentes.plataforma.abrirAjustesDelSistema
import ar.lauta.buscarpatentes.plataforma.rememberPedirPermisos
import ar.lauta.buscarpatentes.plataforma.tienePermiso
import ar.lauta.buscarpatentes.plataforma.versionInstalada
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
    var avisos by remember { mutableStateOf(true) }
    var espacioMapa by remember { mutableStateOf<Long?>(null) }
    var espacioFotos by remember { mutableStateOf(0L) }
    var fotosPasadasDeTecho by remember { mutableStateOf(false) }

    var faltaPermiso by remember { mutableStateOf(false) }

    // Identidad del APK que está corriendo, para el pie de esta pantalla.
    val (version, instalada) = remember {
        versionInstalada().let { (version, en) -> version to (en?.let(::fechaSinAnio) ?: "?") }
    }

    val pedirPermisos = rememberPedirPermisos {
        // T061: negar degrada la funcion, no rompe la app. El resto sigue andando igual.
        faltaPermiso = !tienePermiso(Permiso.UBICACION_SIEMPRE)
    }

    // T060: los permisos caros se piden recien al activar los avisos, no al arrancar.
    fun pedirPermisosDeAviso() {
        val faltantes = listOf(Permiso.UBICACION_SIEMPRE, Permiso.NOTIFICACIONES).filterNot(::tienePermiso)
        if (faltantes.isNotEmpty()) pedirPermisos(faltantes)
    }

    LaunchedEffect(Unit) {
        faltaPermiso = !tienePermiso(Permiso.UBICACION_SIEMPRE)
        val estado = contenedor.estadoDelJuego.leer()
            ?: EstadoDelJuego().also { contenedor.estadoDelJuego.guardar(it) }
        numero = estado.numeroActual.toString()
        avisos = estado.avisosActivos
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

        Column(Modifier.padding(16.dp)) {

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
                        // Con la User Story 4, este mismo punto reconcilia los geofences (D4).
                        nuevo.toIntOrNull()?.let { n ->
                            alcance.launch {
                                contenedor.estadoDelJuego.fijarNumero(n)
                                // D4: es el momento en que el conjunto de geofences cambia.
                                Vigilancia.reconciliar()
                            }
                        }
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            HorizontalDivider(Modifier.padding(vertical = 20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Avisarme al pasar cerca", fontWeight = FontWeight.Bold)
                    Text(
                        "Llega con la User Story 4. El interruptor ya guarda tu preferencia.",
                        fontSize = 13.sp,
                    )
                }
                Switch(
                    checked = avisos,
                    onCheckedChange = { activo ->
                        avisos = activo
                        alcance.launch {
                            contenedor.estadoDelJuego.fijarAvisos(activo)
                            Vigilancia.reconciliar()
                        }
                        if (activo) pedirPermisosDeAviso()
                    },
                )
            }

            if (avisos && faltaPermiso) {
                // T061 / C4: se explica y se ofrece ajustes. El resto de la app no se toca.
                Text(
                    "Android no da el permiso de ubicación en segundo plano desde acá. Hay que " +
                        "abrirlo en los ajustes del sistema y elegir \"Permitir todo el tiempo\". " +
                        "Sin eso los avisos no llegan, pero todo lo demás sigue funcionando.",
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(
                    onClick = { abrirAjustesDelSistema() },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text("Abrir ajustes del sistema")
                }
            }

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
}

private fun enMegas(bytes: Long?): String =
    if (bytes == null) "…" else "${decimales(bytes / 1024.0 / 1024.0, 1)} MB"

private fun ConfigMapaMb(): Long = ConfigMapa.CACHE_MAXIMO_BYTES / 1024 / 1024
