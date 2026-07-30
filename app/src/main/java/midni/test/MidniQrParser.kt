package midni.test

import java.lang.StringBuilder

object MidniQrParser {

    fun parse(data: ByteArray): ParsedQr {
        require(data.size > 10) { "Datos QR demasiado cortos." }

        var index = 0
        val magic = data[index++].toUByte().toInt()
        val version = data[index++].toUByte().toInt()

        require(magic == 0xDC) { "Magic constant invalida: 0x${magic.toString(16)}" }
        require(version == 0x03) { "Version de VDS no soportada: 0x${version.toString(16)}" }

        val country = C40.decode(data.copyOfRange(index, index + 2))
        index += 2

        val signerPrefixBlock = data.copyOfRange(index, index + 4)
        val signerPrefixDecoded = C40.decode(signerPrefixBlock)
        require(signerPrefixDecoded.length >= 6) { "Cabecera de firmante C40 invalida." }
        val issuerCountry = signerPrefixDecoded.substring(0, 2)
        val issuerEntity = signerPrefixDecoded.substring(2, 4)
        val certRefLength = signerPrefixDecoded.substring(4, 6).toInt(16)
        index += 4

        val encodedCertRefLength = ((certRefLength + 2) / 3) * 2
        val certRef = C40.decode(data.copyOfRange(index, index + encodedCertRefLength))
        index += encodedCertRefLength

        val issueDateRaw = data.copyOfRange(index, index + 3)
        index += 3

        val signatureDateRaw = data.copyOfRange(index, index + 3)
        index += 3

        val docFeatureRef = data[index++].toUByte().toInt()
        val docCategory = data[index++].toUByte().toInt()

        val tlvs = mutableListOf<Tlv>()
        var signature: ByteArray? = null
        var signedUntil = data.size

        while (index < data.size) {
            val tlvStart = index
            val tag = data[index++].toUByte().toInt()
            val lengthAndBytes = readLength(data, index)
            val length = lengthAndBytes.first
            index = lengthAndBytes.second

            require(index + length <= data.size) { "TLV fuera de rango para tag 0x${tag.toString(16)}" }

            val value = data.copyOfRange(index, index + length)
            index += length

            if (tag == 0xFF) {
                signature = value
                signedUntil = tlvStart
                break
            }

            tlvs.add(Tlv(tag, length, value))
        }

        require(signature != null) { "No se encontro TLV de firma (0xFF)." }

        val header = Header(
            country = country,
            issuerCountry = issuerCountry,
            issuerEntity = issuerEntity,
            certReference = certRef,
            issueDateRaw = issueDateRaw,
            signatureDateRaw = signatureDateRaw,
            documentFeatureRef = docFeatureRef,
            documentCategory = docCategory,
        )

        return ParsedQr(
            header = header,
            tlvs = tlvs,
            signature = signature,
            signedContent = data.copyOfRange(0, signedUntil),
            debugDump = buildDebug(header, tlvs, signature),
        )
    }

    private fun readLength(data: ByteArray, start: Int): Pair<Int, Int> {
        require(start < data.size) { "Longitud TLV fuera de rango." }
        val first = data[start].toUByte().toInt()

        if (first and 0x80 == 0) {
            return Pair(first, start + 1)
        }

        val size = first and 0x7F
        require(size in 1..4) { "Longitud TLV invalida." }
        require(start + 1 + size <= data.size) { "Longitud TLV truncada." }

        var value = 0
        for (i in 0 until size) {
            value = (value shl 8) or data[start + 1 + i].toUByte().toInt()
        }

        return Pair(value, start + 1 + size)
    }

    private fun buildDebug(header: Header, tlvs: List<Tlv>, signature: ByteArray): String {
        val sb = StringBuilder()
        sb.appendLine("Header:")
        sb.appendLine("- country: ${header.country}")
        sb.appendLine("- issuer: ${header.issuerCountry}${header.issuerEntity}")
        sb.appendLine("- certReference: ${header.certReference}")
        sb.appendLine("- docFeatureRef: 0x${header.documentFeatureRef.toString(16)}")
        sb.appendLine("- docCategory: 0x${header.documentCategory.toString(16)}")
        sb.appendLine()
        sb.appendLine("TLVs:")
        for (tlv in tlvs) {
            val valuePreview = if (tlv.tag == 0x50) {
                "<imagen ${tlv.length} bytes>"
            } else {
                String(tlv.value, Charsets.UTF_8)
            }
            sb.appendLine("- tag=0x${tlv.tag.toString(16)} len=${tlv.length} value=$valuePreview")
        }
        sb.appendLine("- signature len=${signature.size}")
        return sb.toString()
    }
}

data class ParsedQr(
    val header: Header,
    val tlvs: List<Tlv>,
    val signature: ByteArray,
    val signedContent: ByteArray,
    val debugDump: String,
)

data class Header(
    val country: String,
    val issuerCountry: String,
    val issuerEntity: String,
    val certReference: String,
    val issueDateRaw: ByteArray,
    val signatureDateRaw: ByteArray,
    val documentFeatureRef: Int,
    val documentCategory: Int,
)

data class Tlv(
    val tag: Int,
    val length: Int,
    val value: ByteArray,
)

