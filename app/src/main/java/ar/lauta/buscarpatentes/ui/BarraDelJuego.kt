package ar.lauta.buscarpatentes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.lauta.buscarpatentes.R
import ar.lauta.buscarpatentes.domain.Cobertura
import ar.lauta.buscarpatentes.domain.Probabilidad

/**
 * La barra de arriba de la pantalla principal (C1 de la 005).
 *
 * Reemplaza al `IndicadorDelJuego`, que decía "Tenés la 338 · 7 números seguidos cubiertos" y
 * no hacía nada al tocarlo. Ahora dice lo que el jugador quiere saber —qué número y cuántas
 * tiene— y es el punto de partida de todo lo que sigue: la lupa filtra el mapa, y tocarla
 * despliega la lista de esas patentes.
 *
 * **Dos estados, y el campo existe solo en uno** (FR-005a). Cerrada muestra el número y la
 * lupa; filtrando, el número pasa a ser un campo. Un segundo campo numérico siempre abierto le
 * sumaría ruido a una pantalla que ya tiene el de carga, y que se abre tanto para mirar el mapa
 * como para cargar.
 *
 * Va arriba del todo, sobre el mapa, y no en el camino de la carga rápida (Principio I).
 */
@Composable
fun BarraDelJuego(
    /** El número actual del juego, o el del filtro si hay uno. Null antes de leer la base. */
    numero: Int?,
    /** Cuántas patentes hay de [numero], compartidas o no (FR-003b). */
    cuenta: Int,
    /** True mientras el filtro está abierto: el número es un campo y hay una `X`. */
    filtrando: Boolean,
    onAbrirFiltro: () -> Unit,
    /** Llega recién con el tercer dígito (FR-005b). */
    onFiltrar: (Int) -> Unit,
    onCerrarFiltro: () -> Unit,
    /** Las patentes de [numero], ya ordenadas para revisar (FR-012). */
    filas: List<FilaDeLaBarra> = emptyList(),
    /** Sin posición la lista está ordenada por confianza, y se dice (FR-012b). */
    sinPosicion: Boolean = false,
    listaAbierta: Boolean = false,
    /** Tocar la barra fuera de la lupa y de la `X` (FR-004). */
    onTocarBarra: () -> Unit = {},
    onTocarFila: (Long) -> Unit = {},
    /** Al pie de la lista, con dos o más patentes (US4, C2). */
    onArmarRecorrido: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Tarjeta(numero, cuenta, filtrando, onAbrirFiltro, onFiltrar, onCerrarFiltro, onTocarBarra)
        if (listaAbierta) Lista(filas, sinPosicion, onTocarFila, onArmarRecorrido)
    }
}

/**
 * Una patente en la lista de la barra (FR-011 de la 005).
 *
 * Lo que hace falta para decidir si vale la pena ir: dónde queda y qué tan probable es que siga
 * ahí. La pantalla la arma; la barra solo la muestra.
 */
data class FilaDeLaBarra(
    val id: Long,
    /** La patente completa si se cargó entera; si no, el número con ceros. */
    val texto: String,
    /** "A 320 m al noreste", o que no se sabe (FR-003 de la 003). */
    val distanciaYRumbo: String,
    val confianza: Int,
)

@Composable
private fun Tarjeta(
    numero: Int?,
    cuenta: Int,
    filtrando: Boolean,
    onAbrirFiltro: () -> Unit,
    onFiltrar: (Int) -> Unit,
    onCerrarFiltro: () -> Unit,
    onTocarBarra: () -> Unit,
) {
    // FR-004: hasta la 004 tocar la barra no hacía nada, y el jugador lo notó. La lupa, la `X`
    // y el campo reciben sus propios toques; el resto de la tarjeta despliega la lista.
    OutlinedCard(onClick = onTocarBarra, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                if (filtrando) {
                    CampoDeFiltro(numero = numero, onFiltrar = onFiltrar)
                } else if (numero == null) {
                    Text("…")
                } else {
                    Text("%03d".format(numero), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                if (numero != null) Text(Cobertura.descubiertas(cuenta), fontSize = 13.sp)
            }

            if (filtrando) {
                // FR-008: cerrar el filtro es una sola acción, y es esta. Bajar el teclado no
                // lo cierra (FR-005c).
                IconButton(onClick = onCerrarFiltro) {
                    Icon(
                        painter = painterResource(R.drawable.ic_cerrar),
                        contentDescription = "Mostrar todas las patentes",
                    )
                }
            } else {
                IconButton(onClick = onAbrirFiltro) {
                    Icon(
                        painter = painterResource(R.drawable.ic_buscar),
                        contentDescription = "Ver en el mapa solo las de un número",
                    )
                }
            }
        }
    }
}

/**
 * La lista desplegada debajo de la barra (D7 de la 005, C2).
 *
 * Sobre el mapa y no en una hoja modal: el mapa es lo que la lista describe, y tiene que seguir
 * a la vista. Por eso tiene techo —el 40 % de la pantalla—; con las tres o cuatro patentes que
 * suele tener un número casi nunca hace falta desplazarla.
 */
@Composable
private fun Lista(
    filas: List<FilaDeLaBarra>,
    sinPosicion: Boolean,
    onTocarFila: (Long) -> Unit,
    onArmarRecorrido: () -> Unit,
) {
    val alto = LocalConfiguration.current.screenHeightDp.dp * 0.4f
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
    ) {
        LazyColumn(Modifier.heightIn(max = alto)) {
            if (sinPosicion) {
                item {
                    Text(
                        "Sin tu ubicación: ordenadas por confianza",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            items(filas, key = { it.id }) { fila ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onTocarFila(fila.id) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(fila.texto, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(fila.distanciaYRumbo, fontSize = 13.sp)
                    Text(
                        "Confianza ${fila.confianza} de ${Probabilidad.MAXIMA}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
            }
            // FR-014: con una sola no hay recorrido que armar; para esa está la fila misma.
            if (filas.size >= 2) {
                item {
                    TextButton(
                        onClick = onArmarRecorrido,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    ) { Text("Armar recorrido") }
                }
            }
        }
    }
}

/**
 * El número de la barra, convertido en campo (D5 de la 005).
 *
 * Arranca con el número ya escrito y **los tres dígitos seleccionados**: tipear reemplaza en vez
 * de agregar, así que "otro número" son tres toques de teclado sin borrar nada. Pide el foco
 * al aparecer, que es lo que sube el teclado (clarificación del 2026-09-28: el teclado sube de
 * entrada).
 */
@Composable
private fun CampoDeFiltro(numero: Int?, onFiltrar: (Int) -> Unit) {
    val foco = remember { FocusRequester() }
    val focos = LocalFocusManager.current
    var texto by remember {
        val inicial = numero?.let { "%03d".format(it) }.orEmpty()
        mutableStateOf(TextFieldValue(inicial, selection = TextRange(0, inicial.length)))
    }

    LaunchedEffect(Unit) { foco.requestFocus() }

    OutlinedTextField(
        value = texto,
        onValueChange = { nuevo ->
            // Solo dígitos y como mucho tres, igual que el campo de carga.
            if (nuevo.text.length <= 3 && nuevo.text.all { it.isDigit() }) {
                texto = nuevo
                // FR-005b: con uno o dos dígitos el mapa no cambia. Filtrar por prefijo
                // mostraría decenas de números a la vez, que es casi el mapa completo.
                if (nuevo.text.length == 3) onFiltrar(nuevo.text.toInt())
            }
        },
        modifier = Modifier.width(120.dp).focusRequester(foco),
        singleLine = true,
        textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        // "Listo" baja el teclado y deja el filtro puesto (FR-005c).
        keyboardActions = KeyboardActions(onDone = { focos.clearFocus() }),
    )
}
