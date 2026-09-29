package ar.lauta.buscarpatentes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.data.EstadoRegistro
import ar.lauta.buscarpatentes.data.RegistroDeCaptura
import ar.lauta.buscarpatentes.data.Voto
import ar.lauta.buscarpatentes.domain.Geo
import ar.lauta.buscarpatentes.domain.Probabilidad
import ar.lauta.buscarpatentes.plataforma.Permiso
import ar.lauta.buscarpatentes.plataforma.ahora
import ar.lauta.buscarpatentes.plataforma.rememberPedirPermisos
import ar.lauta.buscarpatentes.plataforma.rememberSacarFoto
import ar.lauta.buscarpatentes.plataforma.tienePermiso
import kotlinx.coroutines.launch

/**
 * La ficha de una patente, abierta desde su marcador en el mapa (FR-012, contrato C3).
 *
 * Es una hoja que sube desde abajo y deja el mapa visible detrás, a propósito: el jugador
 * no pierde de vista dónde estaba el marcador que tocó. Una pantalla completa lo sacaría
 * del mapa, y un diálogo lo taparía entero.
 *
 * **La ubicación y la hora se muestran como dato, nunca como campo.** No están
 * deshabilitadas: no existen como entrada. Un campo bloqueado insinúa que en algún momento
 * se podría editar, y el Principio II no admite ni la insinuación. Lo único corregible es
 * el número, que es lo que el jugador leyó y no lo que el teléfono midió.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FichaDeRegistro(
    registro: RegistroDeCaptura,
    onCerrar: () -> Unit,
    onCorregir: (String) -> Unit,
    onCompartir: () -> Unit,
    onBorrar: () -> Unit,
    /** Dónde está parado el jugador ahora. Null si no hay permiso o todavía no hay lectura. */
    posicionActual: Pair<Double, Double>? = null,
    onIr: () -> Unit = {},
    /** De 0 a 10. Arranca en 0 y sube cada vez que se la vuelve a ver. */
    probabilidad: Int = Probabilidad.INICIAL,
    /** +1 si el jugador la volvió a ver, -1 si pasó y no estaba. */
    onVotar: (Int) -> Unit = {},
    /** Solo se ofrece si el registro no tiene foto (FR-013a de la 005). */
    onAgregarFoto: () -> Unit = {},
) {
    var corrigiendo by remember { mutableStateOf(false) }
    var numeroNuevo by remember(registro.id) { mutableStateOf("") }
    var confirmandoBorrado by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onCerrar) {
        // Contrato C2, y por eso el contrato existía: la hoja es la pantalla nueva de esta
        // feature, y nace con el problema. `ModalBottomSheet` se dibuja a sangre sobre la
        // barra de gestos y no se corre solo cuando sube el teclado, así que sin estas dos
        // líneas el botón de borrar queda debajo de la barra y el campo de corregir, debajo
        // del teclado. Si alguna versión de Material 3 empieza a consumir esos insets, los
        // modificadores ven cero y no suman nada: son seguros de todos modos.
        Column(
            Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {

            Text(
                registro.patenteTexto ?: registro.numero.toString().padStart(3, '0'),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(fechaConAnio(registro.capturadoEn), fontSize = 14.sp)

            Text(
                "${decimales(registro.latitud, 5)}, ${decimales(registro.longitud, 5)} · " +
                    "±${registro.precisionMetros.toInt()} m" +
                    if (registro.precisionDegradada) " (poco exacta)" else "",
                fontSize = 14.sp,
            )

            // FR-003: **siempre**, no como respaldo que aparece cuando falla algo. Con
            // conexión sirve para decidir si vale la pena ir; sin conexión es lo único que
            // hay, y ya estaba en pantalla.
            //
            // No se guarda en el registro: depende de dónde está parado el jugador ahora, y
            // persistirla metería en la evidencia un dato de otro día (Principio II).
            //
            // El texto lo arma `Geo`, que es el mismo que usa cada fila de la barra: la
            // rama del FR-003a —decir que no se sabe en vez de inventar un número— tiene que
            // ser una sola.
            Text(
                Geo.distanciaYRumbo(posicionActual, registro.latitud, registro.longitud),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )

            // FR-004: la advertencia va **antes** de mandar al jugador, no después de que
            // caminó diez cuadras hasta un punto que nunca fue exacto.
            if (registro.precisionDegradada) {
                Text(
                    "Ojo: esta ubicación se midió con poca exactitud, así que el punto puede " +
                        "estar a media cuadra de donde viste el auto.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Text(
                buildString {
                    append(if (registro.fotoRuta != null) "Con foto" else "Sin foto")
                    append(" · ")
                    append(
                        if (registro.estado == EstadoRegistro.COMPARTIDA) "ya compartida"
                        else "pendiente",
                    )
                },
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // Qué tan probable es que el auto siga ahí, según lo que el jugador fue viendo
            // en sus recorridos. No es una medición del teléfono: es la cuenta de los votos,
            // y por eso se puede mover sin tocar la evidencia del registro.
            Text(
                "Probabilidad de que siga estando: $probabilidad de ${Probabilidad.MAXIMA}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            if (corrigiendo) {
                Text(
                    "Solo se corrige el número: lo que leíste vos. El lugar y la hora los " +
                        "midió el teléfono cuando la capturaste, y no se tocan.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = numeroNuevo,
                    onValueChange = { nuevo ->
                        if (nuevo.length <= 3 && nuevo.all { it.isDigit() }) numeroNuevo = nuevo
                    },
                    label = { Text("Número correcto") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onCorregir(numeroNuevo) },
                        enabled = numeroNuevo.isNotEmpty(),
                    ) { Text("Guardar") }
                    TextButton(onClick = { corrigiendo = false }) { Text("Cancelar") }
                }
            } else {
                // FR-001: la acción principal de la ficha. El ruteo lo hace la app de mapas
                // del teléfono; esta app solo entrega el destino.
                Button(
                    onClick = onIr,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                ) { Text("Ir hasta acá") }

                // Votar de a un punto por vez. La hoja queda abierta a propósito: el
                // número de arriba se mueve con el toque, que es la confirmación de que el
                // voto entró sin necesidad de un cartel.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { onVotar(1) },
                        modifier = Modifier.weight(1f),
                        enabled = probabilidad < Probabilidad.MAXIMA,
                    ) { Text("La vi de nuevo") }
                    OutlinedButton(
                        onClick = { onVotar(-1) },
                        modifier = Modifier.weight(1f),
                        enabled = probabilidad > Probabilidad.MINIMA,
                    ) { Text("No estaba") }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onCompartir) { Text("Compartir") }
                    TextButton(onClick = { corrigiendo = true }) { Text("Corregir") }
                    TextButton(onClick = { confirmandoBorrado = true }) { Text("Borrar") }
                    // FR-013a de la 005: vivía en la pantalla de búsqueda, que se retiró, y era
                    // lo único de ella que la ficha no tenía.
                    if (registro.fotoRuta == null) {
                        TextButton(onClick = onAgregarFoto) { Text("Agregar foto") }
                    }
                }
            }
        }
    }

    // Borrar no tiene vuelta atrás y se lleva la foto, así que se confirma: destructivo se
    // pregunta.
    if (confirmandoBorrado) {
        AlertDialog(
            onDismissRequest = { confirmandoBorrado = false },
            title = { Text("¿Borrar la ${registro.numero}?") },
            text = {
                Text(
                    "Se va el registro y su foto, y no se pueden recuperar. Si el número " +
                        "salió mal, probá corregirlo en vez de borrarlo.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmandoBorrado = false
                    onBorrar()
                }) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoBorrado = false }) { Text("Cancelar") }
            },
        )
    }
}

/**
 * La ficha con todo lo que hace falta para que funcione: votos, corregir, compartir, borrar.
 *
 * Existe porque hay **dos** pantallas que abren la misma ficha —el mapa principal y el detalle
 * de una salida— y el cableado son sesenta líneas idénticas. La constitución pide dos usos
 * reales antes de que una abstracción exista; acá los hay, y el saldo es negativo: esto borra
 * más de lo que agrega.
 *
 * [onCambio] es para que la pantalla de atrás relea lo que haya cambiado. Un voto no cierra la
 * ficha pero mueve el marcador del mapa; corregir, compartir y borrar la cierran.
 *
 * [onAviso] es opcional a propósito: la pantalla principal tiene snackbar y el detalle de una
 * salida no. Sin él los avisos se pierden en silencio, que es aceptable para un mensaje de
 * confirmación y no lo sería para un error de guardado.
 */
@Composable
fun FichaConectada(
    registro: RegistroDeCaptura,
    onCerrar: () -> Unit,
    posicionActual: Pair<Double, Double>? = null,
    onAviso: (String) -> Unit = {},
    onCambio: () -> Unit = {},
) {
    val alcance = rememberCoroutineScope()

    // Se lee de los votos al abrir y se recalcula después de cada voto: no se guarda en
    // ningún lado más que en la tabla de votos, que es la única fuente.
    var probabilidad by remember(registro.id) { mutableStateOf(registro.probabilidadInicial) }

    // FR-013a de la 005: agregarle foto a un registro guardado con `+`. Se mudó acá desde la
    // pantalla de búsqueda, con las mismas garantías: toca **solo** `fotoRuta`, nunca la
    // ubicación ni el momento (FR-019 de la 001, Principio II).
    //
    // [actual] es el registro releído después de agregar la foto. El que llegó por parámetro es
    // el de cuando se abrió la ficha: con él, el botón seguiría ofreciendo una foto que ya está,
    // y "Compartir" mandaría la patente sin la foto recién sacada.
    var actual by remember(registro.id) { mutableStateOf(registro) }
    var archivoPendiente by remember { mutableStateOf<String?>(null) }

    val sacarFoto = rememberSacarFoto { salioBien ->
        val archivo = archivoPendiente
        archivoPendiente = null
        if (!salioBien || archivo == null) {
            // Un archivo vacío no prueba nada y ocupa lugar.
            contenedor.fotos.borrar(archivo)
            return@rememberSacarFoto
        }
        alcance.launch {
            val r = Captura.adjuntarFotoA(registro.id, archivo)
            actual = contenedor.registros.porId(registro.id) ?: actual
            onAviso(r.texto)
            onCambio()
        }
    }

    fun abrirCamara() {
        val archivo = contenedor.fotos.archivoNuevo(ahora())
        archivoPendiente = archivo
        sacarFoto(archivo)
    }

    // La cámara se pide recién al primer uso, igual que en la carga con foto.
    val pedirCamara = rememberPedirPermisos { concedidos ->
        if (concedidos[Permiso.CAMARA] == true) abrirCamara()
    }

    LaunchedEffect(registro.id) {
        probabilidad = Probabilidad.de(
            contenedor.votos.valoresDe(registro.id),
            registro.probabilidadInicial,
        )
    }

    FichaDeRegistro(
        registro = actual,
        posicionActual = posicionActual,
        probabilidad = probabilidad,
        onAgregarFoto = {
            if (tienePermiso(Permiso.CAMARA)) {
                abrirCamara()
            } else {
                pedirCamara(listOf(Permiso.CAMARA))
            }
        },
        onVotar = { valor ->
            alcance.launch {
                val votos = contenedor.votos
                votos.insertar(
                    Voto(
                        registroId = registro.id,
                        valor = valor,
                        votadoEn = ahora(),
                    ),
                )
                probabilidad =
                    Probabilidad.de(votos.valoresDe(registro.id), registro.probabilidadInicial)
                // FR-030: el borde del pin dibuja esta misma probabilidad, así que un voto
                // tiene que releer los marcadores. Sin esto el número de la ficha cambia y el
                // mapa detrás sigue mintiendo.
                onCambio()
            }
        },
        onIr = {
            // FR-001a: si no hay app que reciba el destino se avisa. No hace falta respaldo:
            // la distancia y el rumbo ya están en la ficha.
            if (!Ir.aLaPatente(actual)) {
                onAviso(
                    "No hay ninguna app de mapas instalada que pueda abrir el lugar. " +
                        "La distancia y la dirección están en la ficha.",
                )
            }
        },
        onCerrar = onCerrar,
        onCorregir = { nuevo ->
            alcance.launch {
                val r = Captura.corregirNumero(registro.id, nuevo)
                onCerrar()
                onCambio()
                onAviso(r.texto)
            }
        },
        onCompartir = {
            alcance.launch {
                Compartir.registro(actual)
                onCerrar()
                onCambio()
            }
        },
        onBorrar = {
            alcance.launch {
                // Con [actual]: si se agregó una foto, el borrado también se la lleva.
                val r = Captura.borrar(actual)
                onCerrar()
                onCambio()
                onAviso(r.texto)
            }
        },
    )
}
