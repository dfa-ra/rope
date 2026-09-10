package app.rope.android.data

/**
 * At-rest wrap for local message `extra` (object keys) and `meta` TEXT.
 * Uses [SecretKv] + Keystore AES-GCM. Not envelope crypto.
 */
object MessageAtRest {
    fun seal(plain: String, encrypt: (ByteArray) -> ByteArray): String {
        val text = JsonIds.optional(plain).orEmpty()
        if (text.isEmpty()) return ""
        return SecretKv.wrap(text, encrypt)
    }

    fun open(stored: String?, decrypt: (ByteArray) -> ByteArray): String {
        val text = JsonIds.optional(stored).orEmpty()
        if (text.isEmpty()) return ""
        return try {
            JsonIds.optional(SecretKv.unwrap(text, decrypt)).orEmpty()
        } catch (_: Exception) {
            ""
        }
    }
}
