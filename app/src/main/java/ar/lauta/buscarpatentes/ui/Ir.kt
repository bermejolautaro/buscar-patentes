package ar.lauta.buscarpatentes.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import ar.lauta.buscarpatentes.data.RegistroDeCaptura
import java.util.Locale

/**
 * Llevar al jugador hasta una patente guardada (FR-001, FR-001a, D2), o por varias (US4 de la
 * 005).
 *
 * ## Esta app no rutea
 *
 * Entrega el destino y se corre. El ruteo lo hace la app de mapas que el jugador ya tenga,
 * que lo hace mejor y sin que este proyecto sume un servicio de ruteo, red obligatoria y una
 * dependencia — los tres los descartó la spec con la pregunta hecha al usuario.
 *
 * ## Por qué `geo:` y no `google.navigation:`
 *
 * `google.navigation:` arranca la navegación paso a paso de una, que suena mejor, pero ata
 * la app a Google Maps. `geo:` es el esquema de la **plataforma**: cualquier app de mapas
 * instalada lo declara, así que el jugador usa la que tenga.
 *
 * La forma `geo:0,0?q=lat,lng(etiqueta)` es la documentada para "mostrame este punto con
 * este nombre". El `0,0` del principio no es un error: es el centro del mapa, que la propia
 * consulta `q` después reemplaza.
 *
 * ## Por qué el recorrido por varias sí es de Google Maps
 *
 * `geo:` es un punto y no admite paradas. La forma de abrir un recorrido con paradas es una
 * Maps URL de direcciones, que es de Google (D11 de la 005). El jugador la pidió por nombre, y
 * si la app no está, la misma URL la abre el navegador.
 *
 * ## Por qué se captura la excepción en vez de preguntar antes
 *
 * `resolveActivity` parecería lo prolijo, pero desde Android 11 devuelve null salvo que el
 * manifiesto declare un bloque `<queries>` para el esquema. Serían dos mecanismos para el
 * mismo resultado, y uno de ellos necesita configuración. El `try` no necesita nada.
 */
object Ir {

    /**
     * Abre el destino en la app de mapas del teléfono.
     *
     * Devuelve false si no hay ninguna que lo reciba. **No hace falta ningún respaldo**: la
     * distancia y el rumbo del FR-003 ya están en pantalla cuando esto se llama.
     */
    fun aLaPatente(context: Context, registro: RegistroDeCaptura): Boolean {
        val etiqueta = Uri.encode(
            registro.patenteTexto ?: registro.numero.toString().padStart(3, '0'),
        )
        val destino = "geo:0,0?q=${registro.latitud},${registro.longitud}($etiqueta)"

        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(destino)))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    /**
     * Cuántas paradas acepta un recorrido de Google Maps: 9 intermedias más el destino en la
     * app, 3 más el destino en el navegador de un teléfono (documentación de Maps URLs,
     * parámetro `waypoints`).
     *
     * ponytail: sale de la documentación y se confirma en el teléfono. Si Maps corta antes o
     * acepta más, se cambia acá y los avisos se acomodan solos.
     */
    const val MAXIMO_PARADAS_APP = 10
    const val MAXIMO_PARADAS_NAVEGADOR = 4

    /** Qué hacer con un recorrido según cuántas paradas quedaron incluidas (C3). */
    sealed interface AccionRecorrido {
        /** Ninguna incluida: no hay nada que abrir y el botón se deshabilita. */
        data object Nada : AccionRecorrido

        /** Una sola: es "Ir" hasta ella (FR-020). */
        data object IrALaUnica : AccionRecorrido

        /** Más de las que entran: se avisa cuántas sacar y no se abre nada (FR-019b). */
        data class Sobran(val cuantas: Int) : AccionRecorrido

        data object AbrirMaps : AccionRecorrido
    }

    /**
     * La decisión del botón "Abrir" del recorrido, pura para poder probarla (constitución,
     * Flujo de desarrollo). Es la única rama de la US4: vivía adentro del diálogo, y el
     * análisis de consistencia la sacó para que tuviera prueba.
     *
     * [maximo] cambia según dónde se abra: la app acepta más paradas que el navegador.
     */
    fun accionPara(incluidas: Int, maximo: Int = MAXIMO_PARADAS_APP): AccionRecorrido = when {
        incluidas <= 0 -> AccionRecorrido.Nada
        incluidas == 1 -> AccionRecorrido.IrALaUnica
        incluidas > maximo -> AccionRecorrido.Sobran(incluidas - maximo)
        else -> AccionRecorrido.AbrirMaps
    }

    /**
     * La Maps URL de un recorrido a pie por [paradas], en ese orden (C4 de la 005).
     *
     * La última es el destino y las anteriores son `waypoints`. **Sin `origin`**: Google Maps
     * arranca en la ubicación del teléfono, que es lo que pide el FR-019. A pie porque el
     * jugador camina, y así no propone subir a una avenida.
     *
     * Las coordenadas van con punto decimal **aunque el teléfono esté en español**: con el
     * `Locale` por defecto, `%.7f` escribe coma, y Maps lee "-34,58" como otra cosa.
     */
    internal fun urlDeRecorrido(paradas: List<Pair<Double, Double>>): String {
        fun coordenada(p: Pair<Double, Double>) =
            String.format(Locale.ROOT, "%.7f,%.7f", p.first, p.second)

        val intermedias = paradas.dropLast(1)
        return buildString {
            append("https://www.google.com/maps/dir/?api=1")
            append("&destination=").append(coordenada(paradas.last()))
            if (intermedias.isNotEmpty()) {
                append("&waypoints=").append(intermedias.joinToString("%7C") { coordenada(it) })
            }
            append("&travelmode=walking")
        }
    }

    /** Qué pasó al intentar abrir el recorrido (C4). */
    enum class ResultadoRecorrido { ABIERTO, DEMASIADAS_PARA_EL_NAVEGADOR, SIN_APP }

    /**
     * Abre el recorrido en Google Maps, o en el navegador si no está (FR-019, FR-019a).
     *
     * En el navegador entran menos paradas. Si hubo que caer ahí y sobran, no se abre: Maps
     * descartaría las de más **sin decir nada**, y el jugador saldría a caminar un recorrido
     * que no es el que armó (FR-019b).
     */
    fun enGoogleMaps(context: Context, paradas: List<Pair<Double, Double>>): ResultadoRecorrido {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlDeRecorrido(paradas)))
        return try {
            context.startActivity(Intent(intent).setPackage("com.google.android.apps.maps"))
            ResultadoRecorrido.ABIERTO
        } catch (_: ActivityNotFoundException) {
            if (accionPara(paradas.size, MAXIMO_PARADAS_NAVEGADOR) is AccionRecorrido.Sobran) {
                return ResultadoRecorrido.DEMASIADAS_PARA_EL_NAVEGADOR
            }
            try {
                context.startActivity(intent)
                ResultadoRecorrido.ABIERTO
            } catch (_: ActivityNotFoundException) {
                ResultadoRecorrido.SIN_APP
            }
        }
    }
}
