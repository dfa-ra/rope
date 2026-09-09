package app.rope.android.data

/**
 * At-rest wrap for cached TURN JSON in the profile kv row.
 * Uses [SecretKv] + Keystore AES-GCM. Not envelope crypto.
 */
object IceAtRest {
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
