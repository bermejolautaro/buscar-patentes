package ar.lauta.buscarpatentes.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.data.EstadoRecorrido
import ar.lauta.buscarpatentes.data.EstadoRegistro
import ar.lauta.buscarpatentes.data.PuntoDeTrayecto
import ar.lauta.buscarpatentes.data.Recorrido
import ar.lauta.buscarpatentes.data.RegistroDeCaptura
import ar.lauta.buscarpatentes.domain.Antiguedad
import ar.lauta.buscarpatentes.domain.CaminoGuardado
import ar.lauta.buscarpatentes.domain.Probabilidad
import ar.lauta.buscarpatentes.mapa.MapaDeFondo
import ar.lauta.buscarpatentes.mapa.Marcador
import ar.lauta.buscarpatentes.mapa.Trazo
import ar.lauta.buscarpatentes.plataforma.Grabacion
import ar.lauta.buscarpatentes.plataforma.ahora
import kotlinx.coroutines.launch

/**
 * Las salidas: cuánto hace que no salgo, y por dónde fue cada una (FR-017 a FR-023).
 *
 * **Reescrita en la 004.** Hasta la 003 esta pantalla era una lista que volcaba todo lo que
 * sabía dentro de cada fila: un mapa de 220 dp y todas las patentes como texto separado por
 * comas. Se construyó como se pidió (FR-010a de la 003) y en uso resultó impracticable —
 * cinco salidas y ya no entraba nada en pantalla. Ahora la lista es lista y el detalle es
 * detalle.
 *
 * El detalle **no es un destino de navegación** par de las otras pantallas: es el adentro de
 * esta, y entra y sale con su estado. Por eso [abierta] y no un quinto valor en el enum de
 * `MainActivity`, que además no lleva argumentos (D10).
 */
@OptIn(ExperimentalComposeUiApi::class) // el BackHandler de Compose Multiplatform
@Composable
fun PantallaRecorridos(onVolver: () -> Unit) {
    val alcance = rememberCoroutineScope()

    // El recorrido en curso cambia lo que hay para mostrar: al terminarlo, la salida
    // aparece cerrada y con su camino completo.
    val enCurso by Grabacion.enCurso.collectAsState()

    var salidas by remember { mutableStateOf<List<ResumenDeSalida>?>(null) }
    var numeroActual by remember { mutableStateOf(0) }

    /** Qué salida está abierta en el detalle. Null es la lista. */
    var abierta by remember { mutableStateOf<Long?>(null) }

    // FR-024: la salida que el jugador pidió borrar, esperando confirmación. Arrancar dos
    // recorridos sin querer es fácil, y hasta ahora no había forma de deshacerlo.
    var aBorrar by remember { mutableStateOf<ResumenDeSalida?>(null) }

    // FR-023: la patente que el jugador tocó en el detalle de una salida.
    var registroTocado by remember { mutableStateOf<RegistroDeCaptura?>(null) }

    // Fuerza la relectura después de borrar. `enCurso` no cambia si la salida borrada ya
    // estaba terminada, así que no alcanza como disparador.
    var recarga by remember { mutableStateOf(0) }

    LaunchedEffect(enCurso, recarga) {
        numeroActual = contenedor.estadoDelJuego.leer()?.numeroActual ?: 0
        // FR-037: una sola consulta de votos para todas las salidas, agrupada por registro.
        val votosPorRegistro = contenedor.votos.todos().groupBy { it.registroId }
        salidas = contenedor.recorridos.todosUnaVez().map { recorrido ->
            val puntos = contenedor.puntos.deRecorrido(recorrido.id)
            val patentes = contenedor.registros.deRecorrido(recorrido.id)
            ResumenDeSalida(
                recorrido = recorrido,
                puntos = puntos,
                patentes = patentes,
                probabilidades = patentes.associate { r ->
                    r.id to Probabilidad.de(
                        votosPorRegistro[r.id].orEmpty().map { it.valor },
                        r.probabilidadInicial,
                    )
                },
            )
        }
    }

    val salidaAbierta = salidas?.firstOrNull { it.recorrido.id == abierta }

    // FR-022 y contrato C5: el gesto de retroceso vuelve a la lista, no a la pantalla
    // principal. `MainActivity` tiene el suyo para eso; el de acá es más interno y gana
    // mientras haya una salida abierta.
    BackHandler(enabled = salidaAbierta != null) { abierta = null }

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
        if (salidaAbierta == null) {
            BarraSuperior("Salidas", onVolver)
            ListaDeSalidas(
                salidas = salidas,
                onAbrir = { abierta = it },
            )
        } else {
            BarraSuperior(fechaLarga(salidaAbierta.recorrido), onVolver = { abierta = null })
            DetalleDeSalida(
                salida = salidaAbierta,
                numeroActual = numeroActual,
                onTocarPatente = { registroTocado = it },
                onBorrar = { aBorrar = salidaAbierta },
            )
        }
    }

    registroTocado?.let { registro ->
        FichaConectada(
            registro = registro,
            onCerrar = { registroTocado = null },
            onCambio = { recarga++ },
        )
    }

    aBorrar?.let { salida ->
        val enCursoEstaSalida = salida.recorrido.estado == EstadoRecorrido.EN_CURSO
        AlertDialog(
            onDismissRequest = { aBorrar = null },
            title = { Text("¿Borrar esta salida?") },
            text = {
                Text(
                    buildString {
                        append("Se va el recorrido y su camino, y no se pueden recuperar. ")
                        if (salida.patentes.isEmpty()) {
                            append("No capturaste ninguna patente en esta salida.")
                        } else {
                            append("Las ")
                            append(salida.patentes.size)
                            append(" patentes que capturaste durante ella se quedan: ")
                            append("borrás el paseo, no lo que viste.")
                        }
                        if (enCursoEstaSalida) {
                            append(" Esta salida está en curso, así que primero se detiene.")
                        }
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    aBorrar = null
                    alcance.launch {
                        // FR-026: primero se detiene, después se borra. Al revés dejaría el
                        // servicio grabando puntos contra un recorrido que ya no existe.
                        if (enCursoEstaSalida) Grabacion.terminar()
                        contenedor.recorridos.borrarConSuTrayecto(salida.recorrido.id)
                        // Contrato C5: borrar desde el detalle vuelve a la lista. Quedarse en
                        // el detalle de algo que ya no existe no es una pantalla, es un error.
                        abierta = null
                        recarga++
                    }
                }) { Text("Borrar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { aBorrar = null }) { Text("Cancelar") }
            },
        )
    }
}

/**
 * Todo lo que una salida necesita mostrar, leído de una vez.
 *
 * Se arma en el efecto de arriba y no en la fila: así las consultas —puntos, patentes y los
 * votos de todas— salen una sola vez para la lista entera, en lugar de una por fila cada vez
 * que el jugador desplaza.
 */
private data class ResumenDeSalida(
    val recorrido: Recorrido,
    val puntos: List<PuntoDeTrayecto>,
    val patentes: List<RegistroDeCaptura>,

    /** La probabilidad de cada patente de esta salida, por id de registro (FR-037). */
    val probabilidades: Map<Long, Int>,
)

/**
 * La lista: hace cuánto fue la última, y una fila corta por salida (FR-017 a FR-020).
 *
 * **Sin ningún total acumulado** —ni de calles, ni de distancia, ni de cuadras— y eso es el
 * requisito, no una omisión (FR-020a). "Qué calles recorrí" ya lo contesta el mapa de la
 * pantalla principal en modo cobertura; un contador al lado sería una segunda respuesta a una
 * pregunta ya respondida, y peor.
 */
@Composable
private fun ListaDeSalidas(salidas: List<ResumenDeSalida>?, onAbrir: (Long) -> Unit) {
    Column(Modifier.padding(16.dp)) {
        when {
            salidas == null -> Text("Cargando…", modifier = Modifier.padding(top = 8.dp))

            salidas.isEmpty() -> Text(
                "Todavía no grabaste ningún recorrido. El botón \"Empezar recorrido\" de la " +
                    "pantalla principal anota por dónde vas mientras buscás.",
                modifier = Modifier.padding(top = 8.dp),
            )

            else -> {
                // FR-020: la pregunta que el jugador trae al entrar acá, contestada sin que
                // toque nada. Es la única de las dos que el mapa no contesta.
                val ultima = salidas.maxOf { it.recorrido.iniciadoEn }
                Text(
                    "Última salida: " + Antiguedad.hace(ultima, ahora()),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                LazyColumn {
                    items(salidas, key = { it.recorrido.id }) { salida ->
                        FilaDeSalida(salida) { onAbrir(salida.recorrido.id) }
                    }
                }
            }
        }
    }
}

/**
 * Una fila: cuándo fue, cuánto duró, cuántas patentes (FR-017, FR-018).
 *
 * Compacta y de altura pareja, y ninguna de las dos cosas es estética: una lista de filas
 * dispares no se puede barrer con la vista, y una fila que se despliega convierte a la lista
 * en un acordeón que hay que administrar.
 *
 * **No lleva ninguna medida de cuánto se caminó.** Qué calles se recorrieron lo contesta el
 * mapa, y un número al lado sería una respuesta peor a la misma pregunta (FR-018).
 */
@Composable
private fun FilaDeSalida(salida: ResumenDeSalida, onAbrir: () -> Unit) {
    val recorrido = salida.recorrido

    OutlinedCard(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onAbrir),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                // FR-019: como tiempo transcurrido, que es la forma en que el jugador se hace
                // la pregunta. La fecha exacta está en el detalle, que es donde importa.
                Text(
                    Antiguedad.hace(recorrido.iniciadoEn, ahora())
                        .replaceFirstChar { it.uppercase() },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    buildString {
                        append(
                            if (recorrido.estado == EstadoRecorrido.EN_CURSO) {
                                "En curso"
                            } else {
                                duracion(recorrido)
                            },
                        )
                        append(" · ")
                        append(
                            when (salida.patentes.size) {
                                0 -> "sin patentes"
                                1 -> "1 patente"
                                else -> "${salida.patentes.size} patentes"
                            },
                        )
                    },
                    fontSize = 13.sp,
                )
            }
        }
    }
}

/**
 * El detalle de una salida: su camino con espacio para verse, y sus patentes (FR-021, FR-023).
 *
 * El mapa se lleva el alto que sobra y no 220 dp fijos adentro de una lista, que era el
 * problema. Las patentes van abajo, en una lista propia con techo: son las de una sola salida,
 * y si son muchas se desplazan sin empujar al mapa fuera de la pantalla.
 */
@Composable
private fun DetalleDeSalida(
    salida: ResumenDeSalida,
    numeroActual: Int,
    onTocarPatente: (RegistroDeCaptura) -> Unit,
    onBorrar: () -> Unit,
) {
    val recorrido = salida.recorrido

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            if (recorrido.estado == EstadoRecorrido.EN_CURSO) {
                "En curso"
            } else {
                duracion(recorrido)
            },
            fontSize = 13.sp,
        )

        if (salida.puntos.isEmpty()) {
            // FR-010b de la 003: una salida sin puntos lo dice, y no abre un mapa vacío.
            Text(
                "Sin recorrido registrado: no hay camino que mostrar.",
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            MapaDeFondo(
                marcadores = salida.patentes.map { r ->
                    Marcador(
                        id = r.id,
                        numero = r.numero,
                        latitud = r.latitud,
                        longitud = r.longitud,
                                toca = r.numero == numeroActual,
                        probabilidad = salida.probabilidades[r.id] ?: r.probabilidadInicial,
                    )
                },
                // Solo el camino de **esta** salida. Es lo único dibujado, así que su escalón
                // de antigüedad no compite con nada; se calcula igual para no inventar un dato.
                trazos = listOf(
                    Trazo(
                        recorridoId = recorrido.id,
                        tramos = CaminoGuardado.leer(recorrido.caminoAjustado)
                            .dibujo(salida.puntos.map { it.latitud to it.longitud }),
                        escalon = escalonDe(recorrido),
                    ),
                ),
                // Acá importa dónde estuvo esa salida, no dónde está parado el jugador ahora.
                // Y con el seguimiento apagado, el mapa encuadra lo que recibe: el camino
                // entero, sin desplazar ni hacer zoom a mano.
                seguirAlJugador = false,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp),
            )
        }

        // FR-023: las patentes se recorren y cada una lleva a su ficha, en lugar de volcarse
        // como una línea de texto separada por comas.
        if (salida.patentes.isEmpty()) {
            Text(
                "Ninguna patente capturada en esta salida.",
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 180.dp)) {
                items(salida.patentes, key = { it.id }) { patente ->
                    OutlinedCard(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onTocarPatente(patente) },
                    ) {
                        Text(
                            patente.patenteTexto
                                ?: patente.numero.toString().padStart(3, '0'),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }

        // FR-022: borrar vive acá y no en la lista. Es el error que esto viene a deshacer
        // —arrancar dos recorridos sin querer—, así que tiene que costar abrir la salida
        // primero y no un toque al pasar el dedo por la lista.
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBorrar) {
                Text("Borrar salida", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun escalonDe(recorrido: Recorrido) = Antiguedad.escalon(
    finalizadoEn = recorrido.finalizadoEn,
    iniciadoEn = recorrido.iniciadoEn,
    ahora = ahora(),
)

/** La fecha exacta, para el título del detalle: acá sí importa cuándo fue. */
private fun fechaLarga(recorrido: Recorrido): String =
    fechaConAnio(recorrido.iniciadoEn)

private fun duracion(recorrido: Recorrido): String {
    val fin = recorrido.finalizadoEn ?: return "Terminado"
    val minutos = (fin - recorrido.iniciadoEn) / 60_000
    return if (minutos < 60) "$minutos min" else "${minutos / 60} h ${minutos % 60} min"
}
