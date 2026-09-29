package ar.lauta.buscarpatentes.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * La única prueba nueva de la 004, y la única lógica pura que la feature agrega (D9).
 *
 * El foco está en los **bordes**, no en el medio. Que una salida de hace un mes caiga en
 * [Escalon.VIEJO] no puede fallar sin que sea obvio mirando el mapa; que una de hace
 * exactamente 3 días caiga del lado correcto del corte, sí: se ve igual de plausible en
 * cualquiera de los dos escalones, y nadie lo notaría hasta confiar en un color equivocado
 * para decidir por dónde caminar.
 */
class AntiguedadTest {

    private val ahora = 1_756_600_000_000L

    private fun haceDias(dias: Long) = ahora - TimeUnit.DAYS.toMillis(dias)

    private fun escalonDeHace(dias: Long, horas: Long = 0) = Antiguedad.escalon(
        finalizadoEn = haceDias(dias) - TimeUnit.HOURS.toMillis(horas),
        iniciadoEn = 0L,
        ahora = ahora,
    )

    @Test
    fun `una salida de recien es reciente`() {
        assertEquals(Escalon.RECIENTE, escalonDeHace(0))
    }

    @Test
    fun `el borde de los 3 dias cae del lado reciente`() {
        assertEquals(Escalon.RECIENTE, escalonDeHace(3))
        // Y sigue siendo reciente hasta cumplir los 4: los días se truncan hacia abajo.
        assertEquals(Escalon.RECIENTE, escalonDeHace(3, horas = 20))
        assertEquals(Escalon.MEDIO, escalonDeHace(4))
    }

    @Test
    fun `el borde de los 14 dias cae del lado medio`() {
        assertEquals(Escalon.MEDIO, escalonDeHace(14))
        assertEquals(Escalon.MEDIO, escalonDeHace(14, horas = 20))
        assertEquals(Escalon.VIEJO, escalonDeHace(15))
    }

    @Test
    fun `dos salidas del mismo escalon se ven iguales`() {
        // FR-015: la escala no promete un detalle que no tiene. 5 y 12 días son lo mismo.
        assertEquals(escalonDeHace(5), escalonDeHace(12))
    }

    @Test
    fun `una salida de hace un ano es vieja`() {
        assertEquals(Escalon.VIEJO, escalonDeHace(365))
    }

    @Test
    fun `una salida en curso no tiene fin, y su antiguedad es cero`() {
        // El caso borde de la spec: `finalizadoEn` nulo cae en `iniciadoEn`, que es hoy.
        val enCurso = Antiguedad.escalon(
            finalizadoEn = null,
            iniciadoEn = haceDias(0),
            ahora = ahora,
        )
        assertEquals(Escalon.RECIENTE, enCurso)
    }

    @Test
    fun `una salida en curso que arranco hace mucho sigue contando desde su inicio`() {
        // Un recorrido que el sistema nunca cerró. No es reciente por estar abierto: la
        // referencia sigue siendo cuándo se caminó, que es lo que la pregunta quiere saber.
        val abiertaHaceUnMes = Antiguedad.escalon(
            finalizadoEn = null,
            iniciadoEn = haceDias(30),
            ahora = ahora,
        )
        assertEquals(Escalon.VIEJO, abiertaHaceUnMes)
    }

    @Test
    fun `el tiempo transcurrido se dice como lo diria una persona`() {
        assertEquals("hoy", Antiguedad.hace(haceDias(0), ahora))
        assertEquals("ayer", Antiguedad.hace(haceDias(1), ahora))
        assertEquals("hace 3 días", Antiguedad.hace(haceDias(3), ahora))
        assertEquals("hace 29 días", Antiguedad.hace(haceDias(29), ahora))
    }

    @Test
    fun `pasado el mes cambia de unidad, porque los dias dejan de significar algo`() {
        assertEquals("hace un mes", Antiguedad.hace(haceDias(45), ahora))
        assertEquals("hace 2 meses", Antiguedad.hace(haceDias(75), ahora))
        assertEquals("hace un año", Antiguedad.hace(haceDias(400), ahora))
        assertEquals("hace 2 años", Antiguedad.hace(haceDias(800), ahora))
    }

    @Test
    fun `una fecha futura se dice hoy y no una cuenta negativa`() {
        // Reloj corrido: "hace -2 días" sería peor que impreciso, sería absurdo.
        assertEquals("hoy", Antiguedad.hace(ahora + TimeUnit.DAYS.toMillis(2), ahora))
    }

    @Test
    fun `una fecha futura por reloj corrido no rompe y cae en reciente`() {
        val futura = Antiguedad.escalon(
            finalizadoEn = ahora + TimeUnit.DAYS.toMillis(2),
            iniciadoEn = 0L,
            ahora = ahora,
        )
        assertEquals(Escalon.RECIENTE, futura)
    }
}
