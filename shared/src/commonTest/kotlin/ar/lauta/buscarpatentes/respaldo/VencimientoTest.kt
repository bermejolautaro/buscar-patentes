package ar.lauta.buscarpatentes.respaldo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VencimientoTest {

    private val plist = """
        <?xml version="1.0" encoding="UTF-8"?>
        <plist version="1.0"><dict>
            <key>CreationDate</key>
            <date>2026-09-29T12:00:00Z</date>
            <key>ExpirationDate</key>
            <date>2026-10-06T12:00:00Z</date>
        </dict></plist>
    """.trimIndent()

    /** Como el de verdad: la firma binaria antes y después del plist, con bytes que no son texto. */
    private fun perfil(adentro: String): ByteArray =
        byteArrayOf(0x30, 0x82.toByte(), 0xFF.toByte(), 0x00, 0xC3.toByte()) +
            adentro.encodeToByteArray() +
            byteArrayOf(0x00, 0xA0.toByte(), 0xFE.toByte())

    @Test
    fun `lee la fecha de vencimiento y no la de creacion`() {
        assertEquals(1_791_288_000_000, Vencimiento.leer(perfil(plist)))
    }

    @Test
    fun `sin perfil sin la clave o con la fecha rota da null`() {
        assertNull(Vencimiento.leer(null))
        assertNull(Vencimiento.leer(perfil(plist.replace("ExpirationDate", "OtraClave"))))
        assertNull(Vencimiento.leer(perfil(plist.replace("2026-10-06T12:00:00Z", "el martes"))))
    }

    @Test
    fun `el aviso aparece con 48 horas o menos`() {
        val vence = 1_791_288_000_000
        val minuto = 60_000L
        assertTrue(Vencimiento.mostrarAviso(vence - Vencimiento.AVISO_MS + minuto, vence))
        assertTrue(Vencimiento.mostrarAviso(vence - Vencimiento.AVISO_MS, vence))
        assertFalse(Vencimiento.mostrarAviso(vence - Vencimiento.AVISO_MS - minuto, vence))
        assertTrue(Vencimiento.mostrarAviso(vence + minuto, vence))
        assertFalse(Vencimiento.mostrarAviso(vence, null))
    }
}
