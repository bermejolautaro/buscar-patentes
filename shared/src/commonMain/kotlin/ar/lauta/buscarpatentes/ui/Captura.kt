package ar.lauta.buscarpatentes.ui

import ar.lauta.buscarpatentes.contenedor
import ar.lauta.buscarpatentes.data.RegistroDeCaptura
import ar.lauta.buscarpatentes.data.Voto
import ar.lauta.buscarpatentes.domain.Geo
import ar.lauta.buscarpatentes.domain.Patente
import ar.lauta.buscarpatentes.domain.Probabilidad
import ar.lauta.buscarpatentes.plataforma.Grabacion
import ar.lauta.buscarpatentes.plataforma.Ubicacion
import ar.lauta.buscarpatentes.plataforma.Vigilancia
import ar.lauta.buscarpatentes.plataforma.ahora
import ar.lauta.buscarpatentes.ubicacion.Lectura
import ar.lauta.buscarpatentes.ubicacion.LecturaUbicacion
import kotlinx.coroutines.Deferred

/** Resultado de una captura, en términos de lo que hay que mostrarle al jugador. */
data class Resultado(val exito: Boolean, val texto: String)

/**
 * Guardado de un registro de captura, con o sin foto.
 *
 * Las dos formas comparten todo salvo la foto: leen la ubicación **en el momento**, la
 * guardan con su precisión real, y fallan de forma visible si algo sale mal. Esa igualdad
 * es FR-018: los dos botones difieren únicamente en la foto.
 */
object Captura {

    /**
     * Dos capturas del mismo número a menos de esto son el mismo auto, no dos patentes.
     *
     * 10 m es poco más que el largo de un auto: alcanza para reconocer que se volvió a
     * pasar por el mismo lugar —con el temblor normal del GPS urbano de por medio— y no
     * tanto como para fusionar dos autos distintos estacionados en la misma cuadra.
     *
     * ponytail: calibrable contra uso real. Si en el barrio el GPS salta más que esto, el
     * síntoma va a ser pines repetidos encimados, y el arreglo es subir este número.
     */
    const val RADIO_MISMA_PATENTE_M = 10.0

    /**
     * El botón `+` (FR-002, FR-003, FR-004, FR-018, FR-019).
     *
     * [fotoRuta] llega no-nula desde el flujo de cámara. La ubicación y el timestamp se
     * leen acá en ambos casos, así que la foto **hereda** la evidencia del registro y
     * nunca la redefine (data-model, Principio II).
     */
    suspend fun guardar(
        entrada: String,
        fotoRuta: String? = null,
        lecturaPrevia: Deferred<LecturaUbicacion>? = null,
    ): Resultado {
        val patente = Patente.parsear(entrada)
            ?: return Resultado(false, "\"$entrada\" no es un número de patente de 3 dígitos.")

        // Si el precalentado no sirvió —falló, o quedó viejo— se pide de nuevo. Eso puede
        // costar un segundo intento completo, y está bien: mejor tardar que no guardar.
        val lecturaFinal = precalentadaUtil(lecturaPrevia) ?: Ubicacion.leerAhora()

        return when (val lectura = lecturaFinal) {
            is LecturaUbicacion.SinPermiso ->
                Resultado(false, "Falta el permiso de ubicación. Sin eso no se guarda nada.")

            // El mensaje dice qué hacer, no solo que falló. Un teléfono sin proveedor de
            // ubicación por red depende del GNSS crudo, que bajo techo no engancha nunca:
            // "probá de nuevo en unos segundos" mandaba al jugador a reintentar en el mismo
            // lugar, para siempre (FR-008).
            is LecturaUbicacion.SinLectura ->
                Resultado(
                    false,
                    "El GPS todavía no tiene posición. Necesita cielo a la vista: salí a la " +
                        "vereda y probá de nuevo.",
                )

            is LecturaUbicacion.Ok -> {
                val l = lectura.lectura
                val registro = RegistroDeCaptura(
                    numero = patente.numero,
                    patenteTexto = patente.texto,
                    formato = patente.formato,
                    latitud = l.latitud,
                    longitud = l.longitud,
                    precisionMetros = l.precisionMetros,
                    // FR-009: se guarda igual, marcada. Nunca se redondea ni se rellena.
                    precisionDegradada = l.precisionMetros > RegistroDeCaptura.UMBRAL_PRECISION_DEGRADADA,
                    capturadoEn = ahora(),
                    fotoRuta = fotoRuta,
                    // FR-027: si hay un recorrido en curso, la captura queda asociada a él.
                    // Se lee del servicio y no de la base: es un campo en memoria contra un
                    // query, y esto está en el camino de los 10 segundos del Principio I.
                    recorridoId = Grabacion.enCurso.value,
                )

                try {
                    // ¿Esta patente ya está anotada acá mismo? El mismo número a menos de
                    // [RADIO_MISMA_PATENTE_M] es el mismo auto visto de nuevo, y eso no es
                    // una captura nueva: es una confirmación de que sigue estando.
                    val yaEstaba = Geo.masCercano(
                        latitud = l.latitud,
                        longitud = l.longitud,
                        candidatos = contenedor.registros.porNumero(patente.numero),
                        radioMetros = RADIO_MISMA_PATENTE_M,
                    ) { it.latitud to it.longitud }

                    if (yaEstaba != null) return confirmar(yaEstaba, fotoRuta)

                    contenedor.registros.insertar(registro)
                    // D4: guardar una patente del numero actual cambia el conjunto.
                    Vigilancia.reconciliar()
                    val conFoto = if (fotoRuta != null) " con foto" else ""
                    val aviso = if (registro.precisionDegradada) {
                        " Ojo: precisión de ${l.precisionMetros.toInt()} m, poco exacta."
                    } else {
                        ""
                    }
                    Resultado(true, "Guardada la ${patente.numero}$conFoto.$aviso")
                } catch (e: Exception) {
                    // FR-008: el fallo se ve. Nunca un éxito silencioso.
                    Resultado(false, "No se pudo guardar: ${e.message ?: e::class.simpleName}")
                }
            }
        }
    }

    /**
     * Volver a ver una patente que ya estaba registrada en ese mismo lugar.
     *
     * No crea un registro nuevo: suma un voto de "sigue estando" al que ya existe, que es
     * literalmente lo que la segunda vista **es** —una observación de permanencia— y lo que
     * la ficha y el borde del pin ya saben leer. Guardarla otra vez habría partido la
     * historia de esa patente en dos pines encimados, cada uno con media confianza y
     * ninguno con la verdad.
     *
     * El registro viejo no se toca en nada que sea evidencia (Principio II): la ubicación y
     * el timestamp siguen siendo los de la primera vez, que es cuando se la midió. Lo nuevo
     * viaja en el voto, que trae su propia hora.
     *
     * La foto no se tira a la basura: si el registro viejo no tenía, se la queda —la
     * evidencia mejora—; si ya tenía, el archivo recién sacado se borra en vez de quedar
     * huérfano ocupando lugar.
     */
    private suspend fun confirmar(
        registro: RegistroDeCaptura,
        fotoRuta: String?,
    ): Resultado {
        contenedor.votos.insertar(
            Voto(registroId = registro.id, valor = 1, votadoEn = ahora()),
        )

        if (fotoRuta != null) {
            if (registro.fotoRuta == null) {
                contenedor.registros.adjuntarFoto(registro.id, fotoRuta)
            } else {
                contenedor.fotos.borrar(fotoRuta)
            }
        }

        val probabilidad =
            Probabilidad.de(contenedor.votos.valoresDe(registro.id), registro.probabilidadInicial)
        return Resultado(
            true,
            "La ${registro.numero} ya estaba acá: confianza $probabilidad de " +
                "${Probabilidad.MAXIMA}.",
        )
    }

    /**
     * La lectura precalentada, solo si todavía cuenta como "de ahora".
     *
     * Devuelve null si el precalentado falló o si envejeció, y en los dos casos el llamador
     * pide una lectura nueva. Ese descarte **es** la garantía: sin él, esto sería guardar una
     * posición vieja con timestamp de ahora (Principio II, D3).
     */
    private suspend fun precalentadaUtil(previa: Deferred<LecturaUbicacion>?): LecturaUbicacion? {
        val lectura = previa?.await() as? LecturaUbicacion.Ok ?: return null
        return lectura.takeIf { Lectura.sirvePrecalentada(it.lectura.edadMs) }
    }

    /**
     * Corregir el número de un registro ya guardado (FR-014, FR-015 de la 002).
     *
     * Se parsea con el mismo [Patente] que usa la captura, así que una entrada que no dé
     * tres dígitos se rechaza acá y nunca llega a la base: corregir no puede dejar un
     * registro peor de lo que estaba.
     *
     * Cambiar el número cambia si ese registro ya toca por el contador del juego, así que
     * dispara la misma reconciliación de avisos que guardar y compartir.
     */
    suspend fun corregirNumero(registroId: Long, entrada: String): Resultado {
        val patente = Patente.parsear(entrada)
            ?: return Resultado(false, "\"$entrada\" no es un número de patente de 3 dígitos.")

        return try {
            contenedor.registros.corregirNumero(
                id = registroId,
                numero = patente.numero,
                texto = patente.texto,
                formato = patente.formato.name,
            )
            Vigilancia.reconciliar()
            Resultado(true, "Corregida a ${patente.numero}.")
        } catch (e: Exception) {
            Resultado(false, "No se pudo corregir: ${e.message ?: e::class.simpleName}")
        }
    }

    /**
     * Borrar un registro equivocado.
     *
     * La evidencia sigue sin poder editarse: el Principio II lo prohíbe, y la ubicación, la
     * precisión y el timestamp de un registro son de cuando el teléfono los midió.
     *
     * Lo que sí se puede desde la 002 es corregir el número, que es lo que el jugador
     * **leyó** y no lo que el teléfono midió: si tipeó 314 cuando el auto decía 341, la
     * salida es [corregirNumero], no borrar. Borrar queda para el registro que no debería
     * existir —una captura duplicada, una patente de otro juego—, no para el número mal
     * tipeado. La ficha lo dice con todas las letras antes de confirmar.
     *
     * Se lleva la foto con él: un archivo huérfano ocupa espacio y no prueba nada.
     */
    suspend fun borrar(registro: RegistroDeCaptura): Resultado =
        try {
            contenedor.fotos.borrar(registro.fotoRuta)
            contenedor.registros.borrar(registro.id)
            // Los votos son sobre un registro que ya no existe: se van con el.
            contenedor.votos.borrarDe(registro.id)
            // El conjunto de geofences puede cambiar: quizá era el del número actual.
            Vigilancia.reconciliar()
            Resultado(true, "Borrada la ${registro.numero}.")
        } catch (e: Exception) {
            Resultado(false, "No se pudo borrar: ${e.message ?: e::class.simpleName}")
        }

    /**
     * Agregar una foto a un registro guardado antes con `+` (FR-019).
     *
     * Toca **solo** `fotoRuta`. La ubicación y el timestamp del registro original quedan
     * exactamente como estaban: son de cuando el jugador vio la patente, no de cuando se
     * acordó de sacarle la foto.
     */
    suspend fun adjuntarFotoA(registroId: Long, fotoRuta: String): Resultado =
        try {
            contenedor.registros.adjuntarFoto(registroId, fotoRuta)
            Resultado(true, "Foto agregada.")
        } catch (e: Exception) {
            Resultado(false, "No se pudo agregar la foto: ${e.message ?: e::class.simpleName}")
        }
}
