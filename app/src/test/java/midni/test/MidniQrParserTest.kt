package midni.test

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * Tests unitarios de [MidniQrParser].
 *
 * Usa las imÃ¡genes de ejemplo en midni-qr-spec/assets/qr-ejemplos/:
 *  - "Ejemplo 1.jpg" : QR MiDNI vÃ¡lido (expira 17-06-2030)
 *  - "Ejemplo 2.jpg" : QR numÃ©rico puro ("55449934144655"), NO es formato MiDNI
 */
class MidniQrParserTest {

    // ---------------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------------

    /**
     * Decodifica la imagen QR del classpath (test resources) y devuelve los bytes
     * binarios en ISO-8859-1, tal como hace la app al escanear con ZXing.
     */
    private fun qrBytesOf(resourceName: String): ByteArray {
        val stream = javaClass.getResourceAsStream("/$resourceName")
            ?: error("Recurso de test no encontrado: $resourceName. " +
                     "Comprueba que sourceSets.test.resources.srcDirs incluye qr-ejemplos/")
        val image = ImageIO.read(stream)
            ?: error("No se pudo leer la imagen: $resourceName")
        val hints = mapOf(
            DecodeHintType.CHARACTER_SET to "ISO-8859-1",
            DecodeHintType.TRY_HARDER to true
        )
        // Normaliza siempre a RGB puro para evitar problemas con CMYK, ARGB, etc.
        fun toRgb(src: BufferedImage, w: Int = src.width, h: Int = src.height): BufferedImage {
            val dst = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
            val g2 = dst.createGraphics()
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g2.drawImage(src, 0, 0, w, h, null)
            g2.dispose()
            return dst
        }
        val norm = toRgb(image)
        val norm2x = toRgb(image, image.width * 2, image.height * 2)
        val readers = listOf(MultiFormatReader(), QRCodeReader())
        for (candidate in listOf(norm, norm2x)) {
            val source = BufferedImageLuminanceSource(candidate)
            for (bitmap in listOf(
                BinaryBitmap(HybridBinarizer(source)),
                BinaryBitmap(GlobalHistogramBinarizer(source)),
                BinaryBitmap(HybridBinarizer(source.invert())),
                BinaryBitmap(GlobalHistogramBinarizer(source.invert()))
            )) {
                for (reader in readers) {
                    try {
                        return reader.decode(bitmap, hints).text
                            .toByteArray(Charsets.ISO_8859_1)
                    } catch (_: Exception) { /* prueba con el siguiente */ }
                }
            }
        }
        error("ZXing no pudo decodificar el QR de la imagen: $resourceName")
    }

    // ---------------------------------------------------------------------------
    // Ejemplo 1 â€“ QR MiDNI vÃ¡lido (expira 17-06-2030)
    // ---------------------------------------------------------------------------

    @Test
    fun `parse - Ejemplo1 se parsea sin excepcion`() {
        val parsed = MidniQrParser.parse(qrBytesOf("Ejemplo 1.jpg"))
        assertNotNull("Firma nula", parsed.signature)
        assertTrue("signedContent vacÃ­o", parsed.signedContent.isNotEmpty())
    }

    @Test
    fun `parse - Ejemplo1 country es ES`() {
        val parsed = MidniQrParser.parse(qrBytesOf("Ejemplo 1.jpg"))
        assertEquals("ES", parsed.header.country)
    }

    @Test
    fun `parse - Ejemplo1 issuerCountry es ES`() {
        val parsed = MidniQrParser.parse(qrBytesOf("Ejemplo 1.jpg"))
        assertEquals("ES", parsed.header.issuerCountry)
    }

    @Test
    fun `parse - Ejemplo1 certReference no esta vacio`() {
        val parsed = MidniQrParser.parse(qrBytesOf("Ejemplo 1.jpg"))
        assertTrue("certReference vacÃ­o", parsed.header.certReference.isNotBlank())
    }

    @Test
    fun `parse - Ejemplo1 contiene TLV de caducidad tag 0x80`() {
        val parsed = MidniQrParser.parse(qrBytesOf("Ejemplo 1.jpg"))
        val expiry = parsed.tlvs.firstOrNull { it.tag == 0x80 }
        assertNotNull("Falta tag 0x80 (caducidad)", expiry)
        assertTrue(
            "Valor de caducidad vacÃ­o",
            String(expiry!!.value, Charsets.UTF_8).isNotBlank()
        )
    }

    @Test
    fun `parse - Ejemplo1 documentFeatureRef es 7 8 o 9`() {
        val parsed = MidniQrParser.parse(qrBytesOf("Ejemplo 1.jpg"))
        assertTrue(
            "documentFeatureRef inesperado: 0x${parsed.header.documentFeatureRef.toString(16)}",
            parsed.header.documentFeatureRef in setOf(0x07, 0x08, 0x09)
        )
    }

    @Test
    fun `parse - Ejemplo1 firma tiene longitud par para ECDSA raw rs`() {
        val parsed = MidniQrParser.parse(qrBytesOf("Ejemplo 1.jpg"))
        assertEquals(
            "Longitud de firma impar (invÃ¡lida para ECDSA raw r||s)",
            0, parsed.signature.size % 2
        )
    }

    @Test
    fun `parse - Ejemplo1 signedContent es menor que los bytes totales del QR`() {
        val bytes = qrBytesOf("Ejemplo 1.jpg")
        val parsed = MidniQrParser.parse(bytes)
        assertTrue(
            "signedContent debe ser menor que los bytes totales del QR",
            parsed.signedContent.size < bytes.size
        )
    }

    // ---------------------------------------------------------------------------
    // Ejemplo 2 â€“ QR numÃ©rico puro ("55449934144655"), NO es formato MiDNI
    // ---------------------------------------------------------------------------

    @Test
    fun `parse - Ejemplo2 QR no MiDNI lanza IllegalArgumentException con mensaje Magic`() {
        val bytes = qrBytesOf("Ejemplo 2.jpg")
        try {
            MidniQrParser.parse(bytes)
            fail("DeberÃ­a haber lanzado IllegalArgumentException para QR no-MiDNI")
        } catch (e: IllegalArgumentException) {
            assertTrue(
                "Mensaje de excepciÃ³n inesperado: ${e.message}",
                e.message?.contains("Magic", ignoreCase = true) == true ||
                e.message?.contains("magic", ignoreCase = true) == true
            )
        }
    }

    @Test
    fun `parse - Ejemplo2 QR decodifica a contenido numerico`() {
        val bytes = qrBytesOf("Ejemplo 2.jpg")
        val text = String(bytes, Charsets.ISO_8859_1)
        assertTrue(
            "Se esperaba QR con solo dÃ­gitos, pero contenido es: $text",
            text.all { it.isDigit() }
        )
    }

    // ---------------------------------------------------------------------------
    // Ejemplo 3 â€“ QR MiDNI binario (regresiÃ³n: magic constant 0x40)
    // ---------------------------------------------------------------------------

    @Test
    fun `parse - Ejemplo3 primer byte es 0xDC no 0x40`() {
        val bytes = qrBytesOf("ejemplo 3.png")
        assertEquals(
            "El primer byte debe ser el magic constant 0xDC, no 0x${bytes[0].toUByte().toString(16)}. " +
            "Causa probable: se estÃ¡n usando los raw codewords del QR (RESULT_BYTES) en lugar del texto decodificado.",
            0xDC, bytes[0].toUByte().toInt()
        )
    }

    @Test
    fun `parse - Ejemplo3 se parsea sin excepcion`() {
        val parsed = MidniQrParser.parse(qrBytesOf("ejemplo 3.png"))
        assertNotNull("Firma nula", parsed.signature)
        assertTrue("signedContent vacÃ­o", parsed.signedContent.isNotEmpty())
    }

    @Test
    fun `parse - Ejemplo3 country es ES`() {
        val parsed = MidniQrParser.parse(qrBytesOf("ejemplo 3.png"))
        assertEquals("ES", parsed.header.country)
    }
}

