package ar.lauta.buscarpatentes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.data.Cuadra
import ar.lauta.buscarpatentes.data.EstadoZona
import ar.lauta.buscarpatentes.data.Zona
import ar.lauta.buscarpatentes.data.Zonas
import ar.lauta.buscarpatentes.domain.Borde
import ar.lauta.buscarpatentes.domain.CuadraParaContar
import ar.lauta.buscarpatentes.domain.CuentaDeZona
import ar.lauta.buscarpatentes.mapa.ClaseDeCuadra
import ar.lauta.buscarpatentes.mapa.CuadraDibujada
import ar.lauta.buscarpatentes.mapa.DibujoDeZona
import ar.lauta.buscarpatentes.mapa.EstadoDelMapa
import ar.lauta.buscarpatentes.mapa.MapaDeFondo
import ar.lauta.buscarpatentes.plataforma.ahora
import ar.lauta.buscarpatentes.ubicacion.BuscarCuadras
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/**
 * Las zonas: dibujar una, ver cuánto falta, quitar cuadras y terminar el objetivo (008).
 *
 * Como en Salidas, el detalle y el dibujo son el adentro de esta pantalla y no destinos de
 * navegación: entran y salen con su estado.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PantallaZonas(onVolver: () -> Unit) {
    val alcance = rememberCoroutineScope()

    var zonas by remember { mutableStateOf<List<ZonaConCuenta>?>(null) }
    var recarga by remember { mutableStateOf(0) }

    /** La zona abierta en el detalle. Null es la lista. */
    var abierta by remember { mutableStateOf<Long?>(null) }
    var dibujando by remember { mutableStateOf(false) }

    // D8: con conexión, las zonas que esperan sus cuadras las buscan al entrar.
    LaunchedEffect(Unit) {
        if (BuscarCuadras.pendientes() > 0) {
            Zonas.revisar()
            recarga++
        }
    }

    LaunchedEffect(recarga) {
        // Las salidas se leen una vez para todas las zonas.
        val salidas = Zonas.salidasParaContar()
        zonas = contenedor.zonas.todas().map { zona ->
            val cuadras = contenedor.zonas.cuadrasDe(zona.id)
            val paraContar = cuadras.map(Zonas::paraContar)
            ZonaConCuenta(
                zona = zona,
                cuadras = cuadras,
                paraContar = paraContar,
                cuenta = if (zona.estado == EstadoZona.ACTIVA) Zonas.cuenta(zona, paraContar, salidas) else null,
            )
        }
    }

    val zonaAbierta = zonas?.firstOrNull { it.zona.id == abierta }
    BackHandler(enabled = dibujando || zonaAbierta != null) {
        if (dibujando) dibujando = false else abierta = null
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        when {
            dibujando -> DibujoDeZonaNueva(
                onCancelar = { dibujando = false },
                onCrear = { nombre, esquinas, desde ->
                    alcance.launch {
                        val id = Zonas.crear(nombre, esquinas, desde, ahora())
                        // La zona se abre enseguida, diciendo "Buscando las calles", y no después
                        // de la búsqueda: con mala señal puede tardar los 20 s de espera.
                        dibujando = false
                        abierta = id
                        recarga++
                        if (BuscarCuadras.pendientes() > 0) {
                            Zonas.revisar()
                            recarga++
                        }
                    }
                },
            )

            zonaAbierta != null -> {
                BarraSuperior(zonaAbierta.zona.nombre, onVolver = { abierta = null })
                DetalleDeZona(
                    z = zonaAbierta,
                    onCambio = { recarga++ },
                    onBorrada = {
                        abierta = null
                        recarga++
                    },
                )
            }

            else -> {
                BarraSuperior("Zonas", onVolver)
                ListaDeZonas(zonas, onNueva = { dibujando = true }, onAbrir = { abierta = it })
            }
        }
    }
}

/** Todo lo que una zona necesita para mostrarse, leído de una vez. */
private class ZonaConCuenta(
    val zona: Zona,
    val cuadras: List<Cuadra>,
    val paraContar: List<CuadraParaContar>,
    /** Solo las activas: una terminada muestra lo que quedó guardado. */
    val cuenta: CuentaDeZona?,
)

/** Activas arriba y terminadas abajo, cada una con su resultado (FR-017). */
@Composable
private fun ListaDeZonas(zonas: List<ZonaConCuenta>?, onNueva: () -> Unit, onAbrir: (Long) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Button(onClick = onNueva, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("Nueva zona")
        }
        when {
            zonas == null -> Text("Cargando…")
            zonas.isEmpty() -> Text(
                "Dibujá una zona y proponete recorrer todas sus calles. La app cuenta cuántas cuadras " +
                    "caminaste y cuánto tiempo te lleva.",
            )
            else -> {
                val (terminadas, enCurso) = zonas.partition {
                    it.zona.estado == EstadoZona.COMPLETADA || it.zona.estado == EstadoZona.CERRADA
                }
                LazyColumn {
                    items(enCurso, key = { it.zona.id }) { FilaDeZona(it) { onAbrir(it.zona.id) } }
                    if (terminadas.isNotEmpty()) {
                        item {
                            Text(
                                "Terminadas",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                            )
                        }
                        items(terminadas, key = { it.zona.id }) { FilaDeZona(it) { onAbrir(it.zona.id) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaDeZona(z: ZonaConCuenta, onAbrir: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onAbrir)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(z.zona.nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(resumen(z), fontSize = 13.sp)
        }
    }
}

/** La segunda línea de la fila y del detalle: cómo va, o cómo terminó. */
private fun resumen(z: ZonaConCuenta): String {
    val zona = z.zona
    val cuenta = z.cuenta
    return when (zona.estado) {
        EstadoZona.BUSCANDO -> zona.problema ?: "Buscando las calles…"
        EstadoZona.ACTIVA -> if (cuenta == null) "" else
            "${cuenta.porcentaje}% · ${faltan(cuenta.faltan)} · ${lleva(zona.cuentaDesde, ahora())}"
        EstadoZona.COMPLETADA -> "Completada ${enCuanto(zona.cuentaDesde, zona.terminadaEn ?: zona.cuentaDesde)}"
        EstadoZona.CERRADA ->
            "Cerrada con ${zona.porcentajeFinal ?: 0}% ${enCuanto(zona.cuentaDesde, zona.terminadaEn ?: zona.cuentaDesde)}"
    }
}

private fun faltan(n: Int) = when (n) {
    0 -> "no falta ninguna"
    1 -> "falta 1 cuadra"
    else -> "faltan $n cuadras"
}

private fun lleva(desde: Long, hasta: Long) = duracion(desde, hasta).let { if (it == DESDE_HOY) "arrancó hoy" else "lleva $it" }

private fun enCuanto(desde: Long, hasta: Long) = duracion(desde, hasta).let { if (it == DESDE_HOY) "en el día" else "en $it" }

/**
 * El detalle: cuánto falta, desde cuándo cuenta y el mapa de la zona (FR-007, FR-008), con lo que
 * se puede hacer según su estado: quitar cuadras y cerrar si está activa, y borrar siempre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetalleDeZona(z: ZonaConCuenta, onCambio: () -> Unit, onBorrada: () -> Unit) {
    val alcance = rememberCoroutineScope()
    val zona = z.zona
    val activa = zona.estado == EstadoZona.ACTIVA
    val cuenta = z.cuenta

    var eligiendoFecha by remember { mutableStateOf(false) }
    var cuadraTocada by remember { mutableStateOf<Cuadra?>(null) }
    var aCerrar by remember { mutableStateOf(false) }
    var aBorrar by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (activa && cuenta != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${cuenta.porcentaje}%", fontSize = 40.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.padding(horizontal = 6.dp))
                Column(Modifier.padding(bottom = 6.dp)) {
                    Text("${faltan(cuenta.faltan).replaceFirstChar { it.uppercase() }} de ${cuenta.total}")
                    Text(lleva(zona.cuentaDesde, ahora()).replaceFirstChar { it.uppercase() }, fontSize = 13.sp)
                }
            }
            Text(
                "Desde cuándo cuenta: ${fechaCorta(zona.cuentaDesde)}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { eligiendoFecha = true }.padding(vertical = 6.dp),
            )
        } else {
            Text(resumen(z), modifier = Modifier.padding(vertical = 8.dp))
        }

        val borde = remember(zona.borde) { Borde.leer(zona.borde) }
        MapaDeFondo(
            zonas = listOf(DibujoDeZona(borde, dibujadas(z), bordeContinuo = true)),
            seguirAlJugador = false,
            onTocarCuadra = if (activa) { id -> cuadraTocada = z.cuadras.firstOrNull { it.id == id } } else null,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp),
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { aBorrar = true }) {
                Text("Borrar zona", color = MaterialTheme.colorScheme.error)
            }
            if (activa) FilledTonalButton(onClick = { aCerrar = true }) { Text("Cerrar objetivo") }
        }
    }

    if (eligiendoFecha) {
        ElegirFecha(
            inicial = zona.cuentaDesde,
            onElegir = { desde ->
                eligiendoFecha = false
                alcance.launch {
                    Zonas.cambiarCuentaDesde(zona.id, desde, ahora())
                    onCambio()
                }
            },
            onCancelar = { eligiendoFecha = false },
        )
    }

    cuadraTocada?.let { cuadra ->
        val calle = cuadra.nombre ?: "Calle sin nombre"
        fun elegir(todaLaCalle: Boolean) {
            cuadraTocada = null
            alcance.launch {
                Zonas.quitar(zona.id, cuadra.id, todaLaCalle, quitada = !cuadra.quitada)
                onCambio()
            }
        }
        AlertDialog(
            onDismissRequest = { cuadraTocada = null },
            title = { Text(calle) },
            text = {
                Text(
                    if (cuadra.quitada) {
                        "Esta cuadra está quitada: no cuenta para el porcentaje."
                    } else {
                        "Si no la vas a caminar, quitala: deja de contar para el porcentaje y la podés volver a sumar."
                    },
                )
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { elegir(todaLaCalle = false) }) {
                        Text(if (cuadra.quitada) "Volver a sumar" else "Quitar esta cuadra")
                    }
                    if (cuadra.nombre != null) {
                        TextButton(onClick = { elegir(todaLaCalle = true) }) {
                            Text(if (cuadra.quitada) "Volver a sumar toda la calle" else "Quitar toda la calle en la zona")
                        }
                    }
                    TextButton(onClick = { cuadraTocada = null }) { Text("Cancelar") }
                }
            },
        )
    }

    if (aCerrar) {
        AlertDialog(
            onDismissRequest = { aCerrar = false },
            title = { Text("¿Cerrar el objetivo?") },
            text = {
                Text(
                    "La zona queda cerrada con ${cuenta?.porcentaje ?: 0}% y no se vuelve a abrir. " +
                        "Las salidas que hagas después ya no la cambian.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    aCerrar = false
                    alcance.launch {
                        Zonas.cerrar(zona.id, ahora())
                        onCambio()
                    }
                }) { Text("Cerrar objetivo") }
            },
            dismissButton = { TextButton(onClick = { aCerrar = false }) { Text("Cancelar") } },
        )
    }

    if (aBorrar) {
        AlertDialog(
            onDismissRequest = { aBorrar = false },
            title = { Text("¿Borrar esta zona?") },
            text = { Text("Se va la zona con sus cuadras y lo que quitaste. Tus salidas no se tocan.") },
            confirmButton = {
                TextButton(onClick = {
                    aBorrar = false
                    alcance.launch {
                        Zonas.borrar(zona.id)
                        onBorrada()
                    }
                }) { Text("Borrar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { aBorrar = false }) { Text("Cancelar") } },
        )
    }
}

/**
 * Cada cuadra con su clase (contrato Z4). Quitada gana: no cuenta, esté recorrida o no. Una zona
 * terminada se pinta con lo que quedó guardado al terminar.
 */
private fun dibujadas(z: ZonaConCuenta): List<CuadraDibujada> {
    val quitadas = z.cuadras.filter { it.quitada }.map { it.id }.toSet()
    val recorridas = z.cuenta?.recorridas ?: z.cuadras.filter { it.recorridaAlTerminar }.map { it.id }.toSet()
    return z.paraContar.map { c ->
        val clase = when (c.id) {
            in quitadas -> ClaseDeCuadra.QUITADA
            in recorridas -> ClaseDeCuadra.RECORRIDA
            else -> ClaseDeCuadra.PENDIENTE
        }
        CuadraDibujada(c.id, c.forma, clase)
    }
}

/**
 * Dibujar una zona nueva tocando sus esquinas (FR-001 a FR-003). Con Listo pide el nombre y desde
 * cuándo cuenta.
 */
@Composable
private fun DibujoDeZonaNueva(
    onCancelar: () -> Unit,
    onCrear: (nombre: String, esquinas: List<Pair<Double, Double>>, desde: Long) -> Unit,
) {
    var esquinas by remember { mutableStateOf(listOf<Pair<Double, Double>>()) }
    var confirmando by remember { mutableStateOf(false) }
    // Sigue al jugador hasta que arrastra, como la pantalla principal: sin esto cada lectura de
    // ubicación traería la cámara de vuelta mientras se dibuja lejos.
    val estadoDelMapa = remember { EstadoDelMapa() }
    val problema = Borde.problema(esquinas)

    BarraSuperior("Nueva zona", onCancelar)
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "Tocá las esquinas de la zona, una por una. Entre toque y toque podés mover el mapa.",
            fontSize = 13.sp,
        )
        MapaDeFondo(
            zonas = listOf(DibujoDeZona(esquinas, emptyList(), bordeContinuo = true, conEsquinas = true)),
            estado = estadoDelMapa,
            onTocarMapa = { lat, lon -> esquinas = esquinas + (lat to lon) },
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { esquinas = esquinas.dropLast(1) }, enabled = esquinas.isNotEmpty()) {
                Text("Deshacer")
            }
            Text(problema ?: "${esquinas.size} esquinas", fontSize = 13.sp)
            Button(onClick = { confirmando = true }, enabled = problema == null) { Text("Listo") }
        }
    }

    if (confirmando) {
        ConfirmarZonaNueva(
            onCrear = { nombre, desde ->
                confirmando = false
                onCrear(nombre, esquinas, desde)
            },
            onCancelar = { confirmando = false },
        )
    }
}

@Composable
private fun ConfirmarZonaNueva(onCrear: (nombre: String, desde: Long) -> Unit, onCancelar: () -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var desde by remember { mutableStateOf(inicioDelDia(ahora())) }
    var eligiendoFecha by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nueva zona") },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre (opcional)") },
                    singleLine = true,
                )
                Text(
                    "Desde cuándo cuenta: ${if (desde == inicioDelDia(ahora())) "hoy" else fechaCorta(desde)}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { eligiendoFecha = true }.padding(top = 12.dp),
                )
                Text(
                    "Las salidas de antes de esa fecha no cuentan. Correla para atrás para sumar lo que ya caminaste.",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onCrear(nombre, desde) }) { Text("Crear") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )

    if (eligiendoFecha) {
        ElegirFecha(
            inicial = desde,
            onElegir = {
                desde = it
                eligiendoFecha = false
            },
            onCancelar = { eligiendoFecha = false },
        )
    }
}

/**
 * El calendario para "desde cuándo cuenta", sin días posteriores a hoy (FR-003). Entra y sale en
 * el comienzo del día en la hora del teléfono; el `DatePicker` trabaja en medianoches de UTC.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun ElegirFecha(inicial: Long, onElegir: (Long) -> Unit, onCancelar: () -> Unit) {
    val local = TimeZone.currentSystemDefault()
    fun aUtc(millis: Long) = Instant.fromEpochMilliseconds(millis).toLocalDateTime(local).date
        .atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    fun deUtc(millis: Long) = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
        .atStartOfDayIn(local).toEpochMilliseconds()

    val hoy = aUtc(ahora())
    val estado = rememberDatePickerState(
        initialSelectedDateMillis = aUtc(inicial),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= hoy
        },
    )
    DatePickerDialog(
        onDismissRequest = onCancelar,
        confirmButton = {
            TextButton(onClick = { estado.selectedDateMillis?.let { onElegir(deUtc(it)) } ?: onCancelar() }) {
                Text("Listo")
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    ) {
        DatePicker(state = estado, title = null, headline = null, showModeToggle = false)
    }
}
