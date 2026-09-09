package app.rope.android.data

import java.util.Base64

/**
 * At-rest wrap for kv secrets (GitHub token). Prefix + Base64 of Keystore AES-GCM.
 * Not envelope crypto — Android Keystore wrap only.
 */
object SecretKv {
    const val PREFIX = "ks1:"

    fun wrap(plain: String, encrypt: (ByteArray) -> ByteArray): String {
        val text = plain.trim()
        if (text.isEmpty()) return ""
        if (isWrapped(text)) return text
        val ct = encrypt(text.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.getEncoder().encodeToString(ct)
    }

    fun unwrap(stored: String?, decrypt: (ByteArray) -> ByteArray): String? {
        if (stored.isNullOrBlank()) return null
        if (!isWrapped(stored)) return stored
        val raw = stored.substring(PREFIX.length)
        val ct = Base64.getDecoder().decode(raw)
        return String(decrypt(ct), Charsets.UTF_8)
    }

    fun isWrapped(stored: String): Boolean = stored.startsWith(PREFIX)
}
