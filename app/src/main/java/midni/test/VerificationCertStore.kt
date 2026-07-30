package midni.test

import android.content.res.AssetManager
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.Locale

class VerificationCertStore(assetManager: AssetManager) {

    private val certMap: Map<String, String>

    init {
        val json = assetManager.open("verification_certs.json").use { input ->
            String(input.readBytes(), StandardCharsets.UTF_8)
        }

        val obj = JSONObject(json)
        val result = mutableMapOf<String, String>()
        for (key in obj.keys()) {
            result[key.uppercase(Locale.ROOT)] = obj.getString(key)
        }
        certMap = result
    }

    fun findCertificate(reference: String): String? {
        return certMap[reference.uppercase(Locale.ROOT)]
    }
}

