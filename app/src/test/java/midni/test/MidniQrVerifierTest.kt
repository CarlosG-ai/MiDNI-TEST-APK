package midni.test

import android.content.res.AssetManager
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * Tests unitarios de [MidniQrVerifier].
 *
 * Mockea [AssetManager] para devolver el contenido real de verification_certs.json
 * (incluido en test resources desde app/src/main/assets/).
 *
 * ImÃ¡genes de ejemplo en midni-qr-spec/assets/qr-ejemplos/:
 *  - "Ejemplo 1.jpg" : QR MiDNI (expira 17-06-2030). La verificaciÃ³n depende de
 *                      si el certRef del QR estÃ¡ en verification_certs.json.
 *  - "Ejemplo 2.jpg" : QR numÃ©rico puro ("55449934144655"), NO es formato MiDNI.
 */
class MidniQrVerifierTest {

    private lateinit var verifier: MidniQrVerifier

    /** Bytes del JSON de certificados, cargados una sola vez. */
    private val certJsonBytes: ByteArray by lazy {
        javaClass.getResourceAsStream("/verification_certs.json")
            ?.readBytes()
            ?: error("verification_certs.json no encontrado en test resources. " +
                     "Comprueba sourceSets.test.resources.srcDirs en build.gradle.")
    }

    @Before
    fun setUp() {
        val mockAssets = mock<AssetManager>()
        // Cada llamada a open() devuelve un nuevo stream del mismo JSON
        whenever(mockAssets.open("verification_certs.json"))
            .thenAnswer { certJsonBytes.inputStream() }
        verifier = MidniQrVerifier(mockAssets)
    }

    // ---------------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------------

    private fun qrBytesOf(resourceName: String): ByteArray {
        val stream = javaClass.getResourceAsStream("/$resourceName")
            ?: error("Recurso de test no encontrado: $resourceName")
        val image = ImageIO.read(stream)
            ?: error("No se pudo leer imagen: $resourceName")
        val hints = mapOf(
            DecodeHintType.CHARACTER_SET to "ISO-8859-1",
            DecodeHintType.TRY_HARDER to true
        )
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

    private fun verify(file: String) = verifier.verify(qrBytesOf(file))

    // ---------------------------------------------------------------------------
    // Ejemplo 1 â€“ QR MiDNI estructuralmente vÃ¡lido (expira 17-06-2030)
    // ---------------------------------------------------------------------------

    @Test
    fun `verify - Ejemplo1 no lanza excepcion`() {
        val result = verify("Ejemplo 1.jpg")
        assertNotNull("El resultado no debe ser nulo", result)
    }

    @Test
    fun `verify - Ejemplo1 devuelve userSummary no vacio`() {
        val result = verify("Ejemplo 1.jpg")
        assertTrue("userSummary debe estar informado", result.userSummary.isNotBlank())
    }

    @Test
    fun `verify - Ejemplo1 QR MiDNI valido firma correcta`() {
        val result = verify("Ejemplo 1.jpg")
        // Ejemplo 1 es un QR MiDNI bien formado con fecha de caducidad futura (17-06-2030).
        // Si el certificado de verificaciÃ³n estÃ¡ en verification_certs.json, success debe ser true.
        assertTrue("Se esperaba verificaciÃ³n exitosa: ${result.userSummary}", result.success)
    }

    @Test
    fun `verify - Ejemplo1 valido tiene personalData`() {
        val result = verify("Ejemplo 1.jpg")
        if (result.success) {
            assertNotNull("personalData debe estar presente en QR vÃ¡lido", result.personalData)
        }
    }

    @Test
    fun `verify - Ejemplo1 valido tiene perfil informado`() {
        val result = verify("Ejemplo 1.jpg")
        if (result.success) {
            assertNotNull("profile debe estar informado en QR vÃ¡lido", result.profile)
        }
    }

    // ---------------------------------------------------------------------------
    // Ejemplo 2 â€“ QR numÃ©rico puro ("55449934144655"), NO es MiDNI
    // ---------------------------------------------------------------------------

    @Test
    fun `verify - Ejemplo2 QR no MiDNI devuelve success false`() {
        val result = verify("Ejemplo 2.jpg")
        assertFalse(
            "QR numÃ©rico no debe verificarse como MiDNI: ${result.userSummary}",
            result.success
        )
    }

    @Test
    fun `verify - Ejemplo2 no lanza excepcion y tiene userSummary`() {
        val result = verify("Ejemplo 2.jpg")
        assertNotNull("El resultado no debe ser nulo", result)
        assertTrue("userSummary debe estar informado", result.userSummary.isNotBlank())
    }

    @Test
    fun `verify - Ejemplo2 personalData es nulo`() {
        val result = verify("Ejemplo 2.jpg")
        assertNull("personalData debe ser nulo en QR invÃ¡lido", result.personalData)
    }

    // ---------------------------------------------------------------------------
    // Casos extremos â€“ no requieren imÃ¡genes
    // ---------------------------------------------------------------------------

    @Test
    fun `verify - bytes vacios devuelve failure sin lanzar excepcion`() {
        val result = verifier.verify(ByteArray(0))
        assertFalse("ByteArray vacÃ­o debe fallar", result.success)
        assertTrue("userSummary debe estar informado", result.userSummary.isNotBlank())
    }

    @Test
    fun `verify - bytes aleatorios devuelve failure sin lanzar excepcion`() {
        val garbage = ByteArray(100) { it.toByte() }
        val result = verifier.verify(garbage)
        assertFalse("Bytes aleatorios deben fallar", result.success)
        assertTrue("userSummary debe estar informado", result.userSummary.isNotBlank())
    }
}


