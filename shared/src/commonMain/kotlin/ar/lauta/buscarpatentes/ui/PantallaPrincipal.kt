package ar.lauta.buscarpatentes.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.data.EstadoDelJuego
import ar.lauta.buscarpatentes.data.ModoMapa
import ar.lauta.buscarpatentes.data.RegistroDeCaptura
import ar.lauta.buscarpatentes.domain.Antiguedad
import ar.lauta.buscarpatentes.domain.Candidato
import ar.lauta.buscarpatentes.domain.Geo
import ar.lauta.buscarpatentes.domain.Prioridad
import ar.lauta.buscarpatentes.domain.Polilinea
import ar.lauta.buscarpatentes.domain.Probabilidad
import ar.lauta.buscarpatentes.mapa.EstadoDelMapa
import ar.lauta.buscarpatentes.mapa.MapaDeFondo
import ar.lauta.buscarpatentes.mapa.Marcador
import ar.lauta.buscarpatentes.mapa.Trazo
import ar.lauta.buscarpatentes.plataforma.Grabacion
import ar.lauta.buscarpatentes.plataforma.Permiso
import ar.lauta.buscarpatentes.plataforma.Ubicacion
import ar.lauta.buscarpatentes.plataforma.ahora
import ar.lauta.buscarpatentes.plataforma.rememberPedirPermisos
import ar.lauta.buscarpatentes.plataforma.rememberSacarFoto
import ar.lauta.buscarpatentes.plataforma.tienePermiso as concedido
import ar.lauta.buscarpatentes.recursos.Res
import ar.lauta.buscarpatentes.recursos.ic_ajustes
import ar.lauta.buscarpatentes.recursos.ic_antiguedad
import ar.lauta.buscarpatentes.recursos.ic_cobertura
import ar.lauta.buscarpatentes.recursos.ic_patentes
import ar.lauta.buscarpatentes.recursos.ic_patentes_ocultas
import ar.lauta.buscarpatentes.recursos.ic_recentrar
import ar.lauta.buscarpatentes.recursos.ic_recorridos_ocultos
import ar.lauta.buscarpatentes.recursos.ic_salidas
import ar.lauta.buscarpatentes.ubicacion.AjustarACalles
import ar.lauta.buscarpatentes.ubicacion.LecturaUbicacion
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * Pantalla principal: mapa de fondo, un campo, tres botones (FR-016, FR-017, FR-036).
 *
 * El orden de composición importa y es deliberado. El campo y el teclado se piden en la
 * primera composición, **antes** de que el mapa haya cargado nada. Ese orden es lo que
 * cumple FR-038 por construcción, no por disciplina: aunque el mapa tarde o nunca cargue,
 * el jugador ya puede tipear y guardar.
 */
@Composable
fun PantallaPrincipal(
    onAjustes: () -> Unit,
    onRecorridos: () -> Unit,
) {
    val alcance = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val teclado = LocalSoftwareKeyboardController.current

    // FR-030: el campo arranca **vacío**, con los tres puntos como placeholder.
    //
    // La primera versión precargaba el número que el juego está pidiendo. Se probó y se
    // descartó: en la salida del 29/08 se capturaron 32 números distintos y ninguno era el
    // actual, así que el valor precargado era algo para borrar casi siempre. El placeholder
    // dice lo mismo sin ocupar el lugar de lo que el jugador va a escribir.
    //
    // Volvió a ser un `String`: el `TextFieldValue` existía solo para dejar el número
    // precargado seleccionado, y sin precarga no hay nada que seleccionar.
    var numero by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    var tienePermiso by remember { mutableStateOf(concedido(Permiso.UBICACION)) }

    // FR-003: dónde está parado el jugador, para la distancia hasta una patente. Se lee al
    // abrir la ficha y no de continuo: es un dato de consulta, no seguimiento.
    var posicionActual by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    // Archivo destino de la foto en curso. Se elige antes de abrir la cámara porque la cámara
    // necesita dónde escribir.
    var fotoPendiente by remember { mutableStateOf<String?>(null) }

    // Lectura de ubicación pedida al empezar a tipear, para que el GPS enganche mientras el
    // jugador escribe. `Captura` la descarta si llega vieja.
    var lecturaEnVuelo by remember { mutableStateOf<Deferred<LecturaUbicacion>?>(null) }

    // FR-003 de la 005: qué número toca y cuántas hay, respondido sin que el jugador busque.
    var numeroActual by remember { mutableStateOf<Int?>(null) }
    var marcadores by remember { mutableStateOf<List<Marcador>>(emptyList()) }

    // Todos los registros, leídos junto con los marcadores. La cuenta de la barra y la lista
    // de la 005 salen de acá en memoria, sin una consulta más.
    var registros by remember { mutableStateOf<List<RegistroDeCaptura>>(emptyList()) }

    // FR-006a: el histórico completo de recorridos. Al retirarse la grilla, el trazo quedó
    // como la única respuesta a "¿por dónde ya anduve?".
    var trazos by remember { mutableStateOf<List<Trazo>>(emptyList()) }

    // FR-002 y FR-003: cuál de las tres preguntas contesta el mapa. Arranca en cobertura y se
    // pisa con lo guardado apenas la base contesta, así que el primer cuadro nunca muestra un
    // modo que el jugador no eligió.
    var modoMapa by remember { mutableStateOf(ModoMapa.COBERTURA) }

    // Esconder las patentes para mirar solo por donde se anduvo.
    //
    // **No se persiste, a diferencia de [ModoMapa].** Un mapa que abre sin patentes se ve
    // igual que un mapa sin nada guardado, y el jugador que dejó el interruptor apagado hace
    // tres días no se acuerda: el próximo arranque parece pérdida de datos. El modo de los
    // recorridos puede recordarse porque su leyenda siempre está a la vista; esto vuelve solo
    // a mostrarlas, que es el estado en el que la app es la app.
    var patentesVisibles by remember { mutableStateOf(true) }

    // FR-004 a FR-010 de la 005: el mapa filtrado por un número, desde la lupa de la barra.
    //
    // `remember` y no `rememberSaveable`, por el FR-009 y la misma razón que [patentesVisibles]:
    // un arranque con menos pines de los que hay se confunde con una pérdida de datos. Y se va
    // con la pantalla: salir a Salidas o Ajustes y volver lo encuentra cerrado.
    //
    // No llega al mapa como filtro: le llegan menos marcadores (D4). Filtrar con una expresión
    // de capa dejaría a la fuente agrupando las ocultas con las visibles.
    var filtro by remember { mutableStateOf<Int?>(null) }
    var filtrando by remember { mutableStateOf(false) }

    // FR-004 de la 005: la lista de las patentes del número de la barra, desplegada.
    var listaAbierta by remember { mutableStateOf(false) }

    // US4 de la 005: el recorrido que se está armando, o null si el diálogo está cerrado. No se
    // guarda en ningún lado (FR-022): cerrar el diálogo lo tira.
    var paradasPlaneadas by remember { mutableStateOf<List<Parada>?>(null) }

    // FR-003a de la 003: si no se puede leer, null, y la ficha y la lista dicen que no saben.
    // Nunca un número inventado. Tiene dos llamadores: tocar un pin y desplegar la lista.
    suspend fun leerPosicion(): Pair<Double, Double>? =
        if (!tienePermiso) {
            null
        } else {
            (Ubicacion.leerAhora() as? LecturaUbicacion.Ok)
                ?.lectura
                ?.let { it.latitud to it.longitud }
        }

    // FR-023: el recorrido en curso lo manda el servicio, que es quien sabe la verdad.
    val recorrido by Grabacion.enCurso.collectAsState()

    // FR-008: y cada punto que el servicio graba, también. `recorrido` cambia al empezar y
    // al terminar, nada más, así que sin esto el trazo de la salida en curso se quedaba como
    // estaba al arrancar —vacío— mientras el jugador caminaba mirando el mapa.
    val puntosGrabados by Grabacion.puntosGuardados.collectAsState()

    // FR-006 y FR-007: la pantalla necesita saber si el mapa sigue al jugador, y poder
    // pedirle que vuelva.
    val estadoDelMapa = remember { EstadoDelMapa() }

    // FR-012: el registro cuya ficha está abierta. No se persiste: cerrar la app cierra la
    // ficha, y reabrirla en una ficha sería raro (data-model, estado de interfaz).
    var registroTocado by remember { mutableStateOf<RegistroDeCaptura?>(null) }

    // Contador para forzar la relectura cuando algo cambió por fuera de guardar: corregir
    // un número, compartir o borrar desde la ficha. Es explícito a propósito — la
    // alternativa era invertir `guardando`, que significa otra cosa y le mentiría a los
    // botones que dependen de él.
    var recarga by remember { mutableStateOf(0) }

    val pedirPermiso = rememberPedirPermisos { concedidos ->
        tienePermiso = concedidos.values.any { it }
        if (!tienePermiso) {
            alcance.launch {
                snackbar.mostrar(
                    "Sin permiso de ubicación no se puede guardar dónde viste la patente, " +
                        "que es para qué sirve la app.",
                )
            }
        }
    }

    val sacarFoto = rememberSacarFoto { salioBien ->
        val archivo = fotoPendiente
        fotoPendiente = null

        if (!salioBien || archivo == null) {
            // T032 / US2 escenario 3: cancelar no crea registro y no pierde lo tipeado.
            contenedor.fotos.borrar(archivo)
            return@rememberSacarFoto
        }

        alcance.launch {
            guardando = true
            val r = Captura.guardar(numero, fotoRuta = archivo)
            guardando = false
            if (r.exito) {
                numero = ""
            } else {
                // Si no se pudo guardar el registro, la foto suelta no sirve para nada.
                contenedor.fotos.borrar(archivo)
            }
            snackbar.mostrar(r.texto)
        }
    }

    // La foto va a almacenamiento privado (D5), a un archivo que se elige antes de abrir la
    // cámara (T029, T030).
    fun abrirCamara() {
        val archivo = contenedor.fotos.archivoNuevo(ahora())
        fotoPendiente = archivo
        sacarFoto(archivo)
    }

    // T034: la cámara se pide recién al primer uso. Negarla no toca el botón `+`.
    val pedirCamara = rememberPedirPermisos { concedidos ->
        if (concedidos[Permiso.CAMARA] == true) {
            abrirCamara()
        } else {
            alcance.launch {
                snackbar.mostrar("Sin permiso de cámara. El botón + sigue guardando igual.")
            }
        }
    }

    // T060 / FR-030: sin notificaciones el recorrido igual graba, pero su aviso persistente
    // no se ve, y FR-023 pide que se vea. Se pide recién al empezar el primero, no al abrir.
    val pedirNotificaciones = rememberPedirPermisos { }

    // T086 / FR-030: el permiso de segundo plano se pide antes de habilitar el recorrido.
    // Negarlo no lo impide —el foreground service graba igual— pero sí apaga los avisos, y
    // el jugador tiene que enterarse de eso acá y no cuando el aviso no llegue.
    val pedirSegundoPlano = rememberPedirPermisos { concedidos ->
        if (concedidos[Permiso.UBICACION_SIEMPRE] != true) {
            alcance.launch {
                snackbar.mostrar(
                    "El recorrido graba igual. Sin ubicación en segundo plano lo que no " +
                        "llega es el aviso al pasar cerca de una patente: se activa en Ajustes.",
                )
            }
        }
    }

    // La app abre con el mapa a la vista y el teclado guardado (FR-017 de la 002, que
    // reemplaza al FR-016 de la 001). El teclado sube cuando el jugador toca el campo.
    //
    // Eran dos causas: este `requestFocus`, y `stateAlwaysVisible` en el manifiesto, que
    // levanta el teclado al arrancar la actividad aunque nadie pida foco.
    //
    // El costo es un toque, y por eso la constitución 1.1.0 subió el presupuesto de la
    // carga rápida de 3 interacciones a 4. Cuatro es el techo, no un punto de partida.
    LaunchedEffect(Unit) {
        if (!tienePermiso) pedirPermiso(listOf(Permiso.UBICACION))
    }

    // Se recalcula cada vez que se guarda algo, se vuelve de ajustes, y al empezar o
    // terminar un recorrido. **`puntosGrabados` ya no es clave de este efecto**, y ese es el
    // punto: la T066 lo agregó cuando el servicio grababa un punto cada 30 s y 50 m, y la
    // T071 lo subió a cada 5 s y 10 m para el FR-036. Con esa clave acá, cada punto salía a
    // buscar el estado, los pendientes, todos los votos, todos los registros, todas las
    // salidas y **todos los puntos de trayecto de todas las salidas**, decodificaba cada
    // polilínea guardada y subía el GeoJSON entero al mapa — cada cinco segundos mientras el
    // jugador camina, que es justo lo que el FR-010 y el SC-005 prohíben.
    //
    // Lo que el punto nuevo cambia es un solo trazo, y eso lo hace el efecto de abajo.
    LaunchedEffect(guardando, recorrido, recarga) {
        if (guardando) return@LaunchedEffect
        val estado = contenedor.estadoDelJuego.leer()
            ?: EstadoDelJuego().also { contenedor.estadoDelJuego.guardar(it) }
        numeroActual = estado.numeroActual
        modoMapa = estado.modoMapa

        // FR-035: todos los registros sobre el mapa, distinguidos por si toca. FR-037: y por
        // la probabilidad de que sigan estando, que va en el anillo. Una sola consulta de
        // votos para todos, agrupada acá: de a uno sería una por marcador.
        val votosPorRegistro = contenedor.votos.todos().groupBy { it.registroId }
        registros = contenedor.registros.todosUnaVez()
        marcadores = registros.map { r ->
            Marcador(
                id = r.id,
                numero = r.numero,
                latitud = r.latitud,
                longitud = r.longitud,
                toca = r.numero == estado.numeroActual,
                probabilidad = Probabilidad.de(
                    votosPorRegistro[r.id].orEmpty().map { it.valor },
                    r.probabilidadInicial,
                ),
            )
        }

        // FR-007 de la 004: todas las salidas dibujadas, y **ninguna destacada**. Acá vivía
        // el cálculo de `aDestacar` —el recorrido en curso, o el más reciente— que pintaba la
        // última fuerte y el resto apagado. Se fue con el FR-007a de la 003: al planear, la
        // salida de ayer no vale más que la de hace un mes, y apagar el histórico escondía
        // justo lo que hay que mirar.
        val salidas = contenedor.recorridos.todosUnaVez()
        val puntosPorRecorrido = contenedor.puntos.todos().groupBy { it.recorridoId }
        val ahora = ahora()

        // FR-013 y D5: **de la más vieja a la más nueva**, y ese orden es el mecanismo, no
        // una prolijidad. MapLibre dibuja las features en el orden de la fuente, así que la
        // salida reciente queda encima de la vieja donde se pisan, sin calcular una sola
        // intersección de geometría.
        trazos = salidas.sortedBy { it.iniciadoEn }.map { salida ->
            // FR-031: el camino ajustado a las calles cuando existe, el crudo cuando no.
            // Nunca los dos: serían dos líneas casi iguales encimadas.
            val ajustado = salida.caminoAjustado?.let { Polilinea.decodificar(it) }
            Trazo(
                recorridoId = salida.id,
                puntos = ajustado?.takeIf { it.isNotEmpty() }
                    ?: puntosPorRecorrido[salida.id].orEmpty().map { it.latitud to it.longitud },
                escalon = Antiguedad.escalon(salida.finalizadoEn, salida.iniciadoEn, ahora),
                ajustado = ajustado?.isNotEmpty() == true,
            )
        }
    }

    // FR-006a de la 005: cada vez que el filtro cambia de número, el mapa muestra todas las de
    // ese número y al jugador. Sin ninguna de ese número, `encuadrar` no se mueve.
    LaunchedEffect(filtro) {
        val n = filtro ?: return@LaunchedEffect
        estadoDelMapa.encuadrar(
            registros.filter { it.numero == n }.map { it.latitud to it.longitud },
        )
    }

    // FR-008: el trazo de la salida en curso crece mientras el jugador camina.
    //
    // Es lo único que un punto nuevo puede cambiar: los marcadores, los votos y el resto del
    // histórico no se mueven porque el servicio haya grabado una posición más. Así el
    // refresco de cada 5 segundos cuesta **una consulta acotada a un recorrido** —el índice
    // por `recorridoId` existe desde la 001— en lugar de la base entera (FR-010, SC-005).
    //
    // El trazo en curso nunca viene ajustado: el ajuste corre sobre salidas terminadas
    // (FR-032), así que acá siempre se reemplazan los puntos crudos.
    LaunchedEffect(puntosGrabados, recorrido) {
        val id = recorrido ?: return@LaunchedEffect
        val puntos = contenedor.puntos.deRecorrido(id).map { it.latitud to it.longitud }
        trazos = trazos.map { if (it.recorridoId == id) it.copy(puntos = puntos) else it }
    }

    // FR-032 y FR-035: el standby del ajuste a calles, **en su propio efecto**.
    //
    // Estaba adentro del efecto que carga marcadores y trazos, y eso lo hacía dos cosas
    // malas a la vez. Una: ese efecto se dispara con cada guardado, así que cada patente
    // cargada salía a la red. Dos, y peor: el resto del efecto esperaba a que la llamada
    // terminara, así que después de guardar el marcador nuevo podía tardar hasta el timeout
    // completo en aparecer. El FR-035 dice que el ajuste no puede demorar ninguna pantalla,
    // y ahí la estaba demorando.
    //
    // Acá corre una sola vez por entrada a la pantalla, sin que nadie lo espere, y recién
    // cuando ajustó algo pide la relectura. Sin conexión no hace nada y no cuesta nada.
    LaunchedEffect(Unit) {
        if (AjustarACalles.pendientes() > 0) recarga++
    }

    // FR-008 exige que un fallo se vea. Sin `imePadding` el cartel sale abajo del teclado,
    // que en esta pantalla está arriba siempre: el error existía y era invisible.
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar, Modifier.imePadding()) },
    ) { relleno ->
        Box(Modifier.fillMaxSize()) {

            // Detrás de todo. Su estado de carga no afecta nada de lo de arriba (FR-038).
            MapaDeFondo(
                // Esconderlas es no mandarlas: el mapa ya sabe dibujar una lista vacía, y
                // filtrar acá deja el estado de la pantalla intacto —los marcadores siguen
                // leídos y contados— para que volver a mostrarlas sea inmediato. El filtro por
                // número de la 005 es lo mismo, a medias. Esconder gana: es lo más general.
                marcadores = when {
                    !patentesVisibles -> emptyList()
                    filtro != null -> marcadores.filter { it.numero == filtro }
                    else -> marcadores
                },
                trazos = trazos,
                modo = modoMapa,
                estado = estadoDelMapa,
                onTocarMarcador = { id ->
                    alcance.launch {
                        registroTocado = contenedor.registros.porId(id)
                        posicionActual = leerPosicion()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = relleno.calculateTopPadding())
                    // Desde targetSdk 35 Android fuerza edge-to-edge, y `adjustResize` deja
                    // de achicar la ventana: sin esto el teclado tapa el campo y los
                    // botones, y la carga rápida se vuelve imposible (FR-016, FR-036).
                    // El mapa queda afuera a propósito: es fondo, va a sangre.
                    //
                    // **`union` y no los dos paddings sumados.** Antes esto tomaba el relleno
                    // entero del `Scaffold` —que incluye la barra de navegación— y encima
                    // `imePadding`, así que con el teclado abierto quedaba una franja muerta
                    // del alto de la barra entre el panel y el teclado. El inset del teclado
                    // ya se mide desde el borde de la pantalla, o sea que **ya contiene** a
                    // la barra de navegación: lo que hace falta es el mayor de los dos, no la
                    // suma.
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
                verticalArrangement = Arrangement.Bottom,
            ) {
                // FR-003 de la 005: la barra mira el número del filtro si hay uno, y si no el
                // actual. FR-003b: la cuenta incluye las compartidas. Es el mismo conjunto que
                // el mapa dibuja, y la barra no puede contar algo distinto de lo que se ve.
                val numeroDeLaBarra = filtro ?: numeroActual
                val delNumero = registros.filter { it.numero == numeroDeLaBarra }

                // FR-012: las de ese número en el orden en que conviene revisarlas. La
                // confianza es la misma que el marcador ya dibuja en el borde del pin.
                val confianzas = marcadores.associate { it.id to it.probabilidad }
                val paraRevisar = Prioridad.paraRevisar(
                    delNumero.map { r ->
                        Candidato(r.id, r.latitud, r.longitud, confianzas[r.id] ?: 0, r.capturadoEn)
                    },
                    posicionActual,
                )
                val porId = delNumero.associateBy { it.id }

                BarraDelJuego(
                    numero = numeroDeLaBarra,
                    cuenta = delNumero.size,
                    filtrando = filtrando,
                    filas = paraRevisar.mapNotNull { c ->
                        val r = porId[c.id] ?: return@mapNotNull null
                        FilaDeLaBarra(
                            id = r.id,
                            texto = r.patenteTexto ?: tresCifras(r.numero),
                            distanciaYRumbo = Geo.distanciaYRumbo(posicionActual, r.latitud, r.longitud),
                            confianza = c.confianza,
                        )
                    },
                    sinPosicion = posicionActual == null,
                    listaAbierta = listaAbierta,
                    onTocarBarra = {
                        // Con ninguna no hay lista que desplegar: la cuenta ya lo dijo.
                        if (delNumero.isEmpty()) return@BarraDelJuego
                        listaAbierta = !listaAbierta
                        if (listaAbierta) {
                            // D7: la posición se lee una vez al desplegar, igual que al abrir
                            // la ficha. Es un dato de consulta, no seguimiento.
                            alcance.launch { posicionActual = leerPosicion() }
                        }
                    },
                    onTocarFila = { id ->
                        val r = porId[id] ?: return@BarraDelJuego
                        listaAbierta = false
                        // FR-011a: el mapa va hasta ella y se abre su ficha, por el mismo
                        // camino que tocar el pin.
                        estadoDelMapa.centrarEn(r.latitud, r.longitud)
                        registroTocado = r
                    },
                    onArmarRecorrido = {
                        listaAbierta = false
                        // FR-015: todas incluidas, en el orden de caminar y no en el de revisar.
                        paradasPlaneadas = Prioridad.ordenDeParadas(paraRevisar, posicionActual)
                            .mapNotNull { c ->
                                val r = porId[c.id] ?: return@mapNotNull null
                                Parada(
                                    registro = r,
                                    distanciaYRumbo = Geo.distanciaYRumbo(
                                        posicionActual, r.latitud, r.longitud,
                                    ),
                                    confianza = c.confianza,
                                )
                            }
                    },
                    onAbrirFiltro = {
                        filtrando = true
                        // FR-005: la lupa ya filtra por el que toca. Sin número cargado, el
                        // campo abre vacío y el mapa espera los tres dígitos.
                        numeroActual?.let { filtro = it }
                    },
                    onFiltrar = { filtro = it },
                    onCerrarFiltro = {
                        // FR-006c: cerrar no mueve el mapa. Queda donde el jugador lo dejó.
                        filtro = null
                        filtrando = false
                    },
                )

                Spacer(Modifier.weight(1f))

                // FR-026: **todo lo que se toca vive en esta franja**, justo encima del
                // campo y de los botones de captura. Antes Buscar, Salidas y Ajustes
                // colgaban del indicador, pegados al borde de arriba: para llegar había que
                // recolocar el teléfono en la mano, y el Principio I pide una sola mano.
                //
                // No flota con un margen absoluto sobre el mapa: la primera versión del
                // recentrado usaba `align(BottomEnd)` con 96dp y quedaba justo encima del
                // campo, tapándolo y sin recibir el toque. Dentro de la columna, el layout
                // garantiza que nada se pise.
                //
                // FR-023: cada control es un botón tonal, con su propio fondo. Sobre el mapa
                // un icono suelto se pierde entre las calles según por dónde ande el jugador.
                // FR-004: la línea que dice qué está mostrando el mapa. Va acá arriba y no
                // pegada al botón porque lo que describe es el mapa, no el control.
                LeyendaDelMapa(
                    modo = modoMapa,
                    patentesVisibles = patentesVisibles,
                    filtro = filtro,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // FR-001 y FR-002: el interruptor del mapa. Vive en esta franja y no
                    // flotando sobre el mapa por el FR-026 de la 003, que lo dice por
                    // experiencia propia: el botón de recentrar flotaba con un margen
                    // absoluto, quedaba encima del campo de número y no recibía el toque.
                    FilledTonalIconButton(onClick = {
                        val siguiente = modoMapa.siguiente()
                        modoMapa = siguiente
                        alcance.launch {
                            contenedor.estadoDelJuego.fijarModoMapa(siguiente)
                        }
                    }) {
                        Icon(
                            painter = painterResource(
                                when (modoMapa) {
                                    ModoMapa.COBERTURA -> Res.drawable.ic_cobertura
                                    ModoMapa.ANTIGUEDAD -> Res.drawable.ic_antiguedad
                                    ModoMapa.APAGADO -> Res.drawable.ic_recorridos_ocultos
                                },
                            ),
                            contentDescription = "Cómo se ven los recorridos en el mapa",
                        )
                    }
                    // Esconder las patentes. Al lado del interruptor de recorridos porque
                    // son las dos mitades de la misma pregunta —que se dibuja sobre el
                    // mapa—, y separadas de Salidas y Ajustes, que llevan a otra pantalla.
                    FilledTonalIconButton(onClick = { patentesVisibles = !patentesVisibles }) {
                        Icon(
                            painter = painterResource(
                                if (patentesVisibles) {
                                    Res.drawable.ic_patentes
                                } else {
                                    Res.drawable.ic_patentes_ocultas
                                },
                            ),
                            contentDescription = if (patentesVisibles) {
                                "Esconder las patentes del mapa"
                            } else {
                                "Mostrar las patentes en el mapa"
                            },
                        )
                    }
                    // Acá estaba el botón de la pantalla de búsqueda. Se fue en la 005 (FR-013):
                    // su trabajo lo hace la barra de arriba, y el campo de número gana su ancho.
                    FilledTonalIconButton(onClick = onRecorridos) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_salidas),
                            contentDescription = "Salidas",
                        )
                    }
                    FilledTonalIconButton(onClick = onAjustes) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_ajustes),
                            contentDescription = "Ajustes",
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // FR-007: aparece solo cuando el mapa dejó de seguir al jugador, que es
                    // cuando sirve para algo.
                    if (!estadoDelMapa.siguiendo) {
                        FilledTonalIconButton(onClick = { estadoDelMapa.recentrar() }) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_recentrar),
                                contentDescription = "Volver a mi ubicación",
                            )
                        }
                    }
                }

                // FR-017 y FR-018: el campo y los botones van sobre una superficie propia.
                //
                // Antes flotaban sueltos sobre el mapa, con el campo transparente: lo que se
                // leía detrás cambiaba según por dónde anduviera el jugador, así que un
                // número tipeado sobre una avenida se leía distinto que sobre un parque. Un
                // solo fondo para los dos resuelve las dos cosas, y es menos que darle fondo
                // a cada control.
                //
                // Las esquinas de arriba redondeadas dejan claro que es una franja apoyada
                // sobre el mapa y no el borde de la pantalla.
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    tonalElevation = 3.dp,
                ) {
                    // FR-017a y FR-018: **todo lo que se toca vive en una sola fila**.
                    //
                    // Antes eran dos: el campo centrado arriba y los tres botones abajo, con
                    // el de recorrido ocupando todo el ancho que sobraba. Dos filas de
                    // controles sobre un mapa es una franja del doble de alto para la misma
                    // cantidad de acciones, y el campo quedaba enorme por estar solo en su
                    // renglon.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BotonesAccion(
                            habilitados = numero.isNotEmpty() && !guardando && tienePermiso,
                            onGuardarRapido = {
                                val previa = lecturaEnVuelo
                                lecturaEnVuelo = null
                                alcance.launch {
                                    guardando = true
                                    val r = Captura.guardar(
                                        numero,
                                        lecturaPrevia = previa,
                                    )
                                    guardando = false
                                    if (r.exito) numero = ""
                                    snackbar.mostrar(r.texto)
                                }
                            },
                            onGuardarConFoto = {
                                if (concedido(Permiso.CAMARA)) {
                                    abrirCamara()
                                } else {
                                    pedirCamara(listOf(Permiso.CAMARA))
                                }
                            },
                            recorridoEnCurso = recorrido != null,
                            onRecorrido = {
                                when {
                                    recorrido != null -> Grabacion.terminar()

                                    !tienePermiso -> alcance.launch {
                                        snackbar.mostrar(
                                            "Sin permiso de ubicacion no se puede grabar por donde pasas.",
                                        )
                                    }

                                    else -> {
                                        // El recorrido necesita mostrar su notificacion
                                        // persistente para cumplir FR-023.
                                        if (!concedido(Permiso.NOTIFICACIONES)) {
                                            pedirNotificaciones(listOf(Permiso.NOTIFICACIONES))
                                        }

                                        // FR-030 pide el permiso de segundo plano antes de
                                        // habilitar los avisos **o los recorridos**.
                                        // Tecnicamente el foreground service graba sin el; la
                                        // spec lo pide igual. Negarlo no bloquea nada.
                                        if (!concedido(Permiso.UBICACION_SIEMPRE)) {
                                            pedirSegundoPlano(listOf(Permiso.UBICACION_SIEMPRE))
                                        }
                                        Grabacion.empezar()
                                        alcance.launch {
                                            snackbar.mostrar(
                                                "Recorrido empezado. Podes apagar la pantalla.",
                                            )
                                        }
                                    }
                                }
                            },
                        )

                        // FR-017a: el campo se lleva lo que sobra de la fila, y nada mas.
                        BasicTextField(
                            value = numero,
                            onValueChange = { nuevo ->
                                if (nuevo.length <= 3 && nuevo.all { it.isDigit() }) {
                                    val estabaVacio = numero.isEmpty()
                                    numero = nuevo

                                    // El GPS empieza a buscar con el primer digito, no al
                                    // tocar `+`. Los segundos que el jugador pasa tipeando el
                                    // resto los gasta el GPS enganchando.
                                    if (estabaVacio && nuevo.isNotEmpty() && tienePermiso) {
                                        lecturaEnVuelo = alcance.async {
                                            Ubicacion.leerAhora()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(
                                fontSize = 26.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                            // FR-037: bajar el teclado deja ver el mapa entero. Tocar el campo
                            // lo vuelve a subir, sin salir de la pantalla. No se usa un
                            // overlay tactil sobre el mapa porque le robaria los gestos.
                            keyboardActions = KeyboardActions(onDone = { teclado?.hide() }),
                            decorationBox = { entrada ->
                                Box(
                                    Modifier
                                        .height(52.dp)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline,
                                            MaterialTheme.shapes.small,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (numero.isEmpty()) {
                                        Text(
                                            "\u00b7\u00b7\u00b7",
                                            fontSize = 26.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    entrada()
                                }
                            },
                        )
                    }
                }
            }

            // FR-012 a FR-014: la ficha del registro tocado en el mapa. Va adentro del Box
            // para que la hoja suba sobre el mapa, con el marcador todavía a la vista.
            registroTocado?.let { registro ->
                FichaConectada(
                    registro = registro,
                    posicionActual = posicionActual,
                    onAviso = { alcance.launch { snackbar.mostrar(it) } },
                    onCambio = { recarga++ },
                    onCerrar = { registroTocado = null },
                )
            }

            // US4 de la 005: armar el recorrido por las patentes del número de la barra.
            paradasPlaneadas?.let { paradas ->
                ArmarRecorrido(
                    numero = paradas.first().registro.numero,
                    inicial = paradas,
                    onCerrar = { paradasPlaneadas = null },
                )
            }
        }
    }
}

private suspend fun SnackbarHostState.mostrar(texto: String) {
    currentSnackbarData?.dismiss()
    showSnackbar(texto)
}
