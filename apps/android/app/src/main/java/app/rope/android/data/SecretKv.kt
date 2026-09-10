package app.rope.android.data

import java.util.Base64

/**
 * At-rest wrap for kv secrets (GitHub token). Prefix + Base64 of Keystore AES-GCM.
 * Not envelope crypto — Android Keystore wrap only.
 */
object SecretKv {
    const val PREFIX = "ks1:"

    fun wrap(plain: String, encrypt: (ByteArray) -> ByteArray): String {
        if (!allowPlain(plain)) return ""
        val text = plain.trim()
        if (text.isEmpty()) return ""
        if (isWrapped(text)) return text
        val ct = encrypt(text.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.getEncoder().encodeToString(ct)
    }

    fun unwrap(stored: String?, decrypt: (ByteArray) -> ByteArray): String? {
        if (stored.isNullOrBlank()) return null
        if (!isWrapped(stored)) {
            return stored.takeIf { allowPlain(it) }
        }
        val raw = stored.substring(PREFIX.length)
        return try {
            val ct = Base64.getDecoder().decode(raw)
            val plain = String(decrypt(ct), Charsets.UTF_8)
            plain.takeIf { allowPlain(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun isWrapped(stored: String): Boolean = stored.startsWith(PREFIX)

    /**
     * GitHub PAT / kv secrets must not carry CR/LF/NUL into HTTP headers after trim.
     * ICE JSON blobs may be pretty-printed.
     */
    private fun allowPlain(raw: String): Boolean {
        if (raw.indexOf('\u0000') >= 0) return false
        val t = raw.trim()
        if (t.startsWith("{") || t.startsWith("[")) return true
        return raw.indexOf('\n') < 0 && raw.indexOf('\r') < 0
    }
}
