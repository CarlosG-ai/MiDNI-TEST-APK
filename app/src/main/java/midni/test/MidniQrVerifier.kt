package midni.test

import android.content.res.AssetManager
import java.util.Base64
import org.bouncycastle.asn1.ASN1EncodableVector
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DERSequence
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.TimeZone

class MidniQrVerifier(assetManager: AssetManager) {

    private val certStore = VerificationCertStore(assetManager)

    fun verify(rawBytes: ByteArray): VerificationResult {
        return runCatching {
            val parsed = MidniQrParser.parse(rawBytes)

            val certRef = parsed.header.certReference.uppercase(Locale.ROOT)
            val certBase64 = certStore.findCertificate(certRef)
                ?: return VerificationResult(
                    success = false,
                    userSummary = "No existe certificado para la referencia $certRef",
                    debugSummary = parsed.debugDump,
                )

            val signerCert = loadCertificate(certBase64)
            signerCert.checkValidity(Date())

            val serialHex = signerCert.serialNumber.toString(16).uppercase(Locale.ROOT).trimStart('0')
            if (serialHex != certRef.trimStart('0')) {
                return VerificationResult(
                    success = false,
                    userSummary = "La referencia de certificado del QR no coincide con el certificado cargado.",
                    debugSummary = parsed.debugDump,
                )
            }

            val signatureDer = rsToDer(parsed.signature)
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(signerCert.publicKey)
            verifier.update(parsed.signedContent)
            val signatureOk = verifier.verify(signatureDer)

            if (!signatureOk) {
                return VerificationResult(
                    success = false,
                    userSummary = "Firma digital incorrecta.",
                    debugSummary = parsed.debugDump,
                )
            }

            val dataExpiry = parsed.tlvs.firstOrNull { it.tag == 0x80 }?.let { String(it.value, Charsets.UTF_8) }
            if (dataExpiry != null) {
                val expiryInstant = parseUtcDateTime(dataExpiry)
                    ?: return VerificationResult(
                        success = false,
                        userSummary = "No se pudo interpretar la fecha de caducidad del QR.",
                        debugSummary = parsed.debugDump,
                    )

                if (expiryInstant.before(Date())) {
                    return VerificationResult(
                        success = false,
                        userSummary = "El QR esta caducado (tag 0x80).",
                        debugSummary = parsed.debugDump,
                    )
                }
            } else {
                return VerificationResult(
                    success = false,
                    userSummary = "No se encontro el campo de caducidad (tag 0x80).",
                    debugSummary = parsed.debugDump,
                )
            }

            val profile = when (parsed.header.documentFeatureRef) {
                0x07 -> "DNI simple"
                0x08 -> "DNI completo"
                0x09 -> "Verificacion de edad"
                else -> "Perfil desconocido"
            }

            val docNumber = parsed.tlvs.firstOrNull { it.tag == 0x40 }?.let { String(it.value, Charsets.UTF_8) } ?: "N/D"

            val personalData = PersonalData(
                documentNumber = parsed.tlvs.firstOrNull { it.tag == 0x40 }?.let { String(it.value, Charsets.UTF_8) },
                dateOfBirth = parsed.tlvs.firstOrNull { it.tag == 0x42 }?.let { String(it.value, Charsets.UTF_8) },
                name = parsed.tlvs.firstOrNull { it.tag == 0x44 }?.let { String(it.value, Charsets.UTF_8) },
                surnames = parsed.tlvs.firstOrNull { it.tag == 0x46 }?.let { String(it.value, Charsets.UTF_8) },
                sex = parsed.tlvs.firstOrNull { it.tag == 0x48 }?.let { String(it.value, Charsets.UTF_8) },
                documentExpiration = parsed.tlvs.firstOrNull { it.tag == 0x4C }?.let { String(it.value, Charsets.UTF_8) },
                photoBytes = parsed.tlvs.firstOrNull { it.tag == 0x50 }?.value,
                address = parsed.tlvs.firstOrNull { it.tag == 0x60 }?.let { String(it.value, Charsets.UTF_8) },
                supportNumber = parsed.tlvs.firstOrNull { it.tag == 0x68 }?.let { String(it.value, Charsets.UTF_8) },
                isOfAge = parsed.tlvs.firstOrNull { it.tag == 0x70 }?.let { String(it.value, Charsets.UTF_8) == "Y" } ?: false
            )

            VerificationResult(
                success = true,
                userSummary = "Firma correcta. Perfil: $profile. Documento: $docNumber.",
                debugSummary = parsed.debugDump,
                personalData = personalData,
                profile = profile
            )
        }.getOrElse { ex ->
            VerificationResult(
                success = false,
                userSummary = "Error validando QR: ${ex.message}",
                debugSummary = ex.stackTraceToString(),
                personalData = null,
                profile = null
            )
        }
    }

    private fun loadCertificate(certBase64: String): X509Certificate {
        val bytes = Base64.getDecoder().decode(certBase64)
        val factory = CertificateFactory.getInstance("X.509")
        return factory.generateCertificate(ByteArrayInputStream(bytes)) as X509Certificate
    }

    private fun rsToDer(rs: ByteArray): ByteArray {
        require(rs.size % 2 == 0) { "Firma ECDSA invalida: tamano impar." }

        val half = rs.size / 2
        val r = BigInteger(1, rs.copyOfRange(0, half))
        val s = BigInteger(1, rs.copyOfRange(half, rs.size))

        val vec = ASN1EncodableVector()
        vec.add(ASN1Integer(r))
        vec.add(ASN1Integer(s))
        return DERSequence(vec).encoded
    }

    private fun parseUtcDateTime(value: String): Date? {
        return try {
            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ROOT)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(value)
        } catch (_: Exception) {
            null
        }
    }
}

data class VerificationResult(
    val success: Boolean,
    val userSummary: String,
    val debugSummary: String,
    val personalData: PersonalData? = null,
    val profile: String? = null
)

data class PersonalData(
    val documentNumber: String?,
    val dateOfBirth: String?,
    val name: String?,
    val surnames: String?,
    val sex: String?,
    val documentExpiration: String?,
    val photoBytes: ByteArray?,
    val address: String?,
    val supportNumber: String?,
    val isOfAge: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PersonalData

        if (documentNumber != other.documentNumber) return false
        if (dateOfBirth != other.dateOfBirth) return false
        if (name != other.name) return false
        if (surnames != other.surnames) return false
        if (sex != other.sex) return false
        if (documentExpiration != other.documentExpiration) return false
        if (photoBytes != null) {
            if (other.photoBytes == null) return false
            if (!photoBytes.contentEquals(other.photoBytes)) return false
        } else if (other.photoBytes != null) return false
        if (address != other.address) return false
        if (supportNumber != other.supportNumber) return false
        if (isOfAge != other.isOfAge) return false

        return true
    }

    override fun hashCode(): Int {
        var result = documentNumber?.hashCode() ?: 0
        result = 31 * result + (dateOfBirth?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (surnames?.hashCode() ?: 0)
        result = 31 * result + (sex?.hashCode() ?: 0)
        result = 31 * result + (documentExpiration?.hashCode() ?: 0)
        result = 31 * result + (photoBytes?.contentHashCode() ?: 0)
        result = 31 * result + (address?.hashCode() ?: 0)
        result = 31 * result + (supportNumber?.hashCode() ?: 0)
        result = 31 * result + isOfAge.hashCode()
        return result
    }
}

