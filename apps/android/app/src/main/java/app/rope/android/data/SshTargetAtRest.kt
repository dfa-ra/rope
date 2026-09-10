package app.rope.android.data

/**
 * At-rest wrap for the ssh_target kv row (host/user/port, not a PEM).
 * Uses [SecretKv] + Keystore AES-GCM. Not envelope crypto.
 * Leftover v1 plaintext is opened and re-wrapped on read.
 */
object SshTargetAtRest {
    fun seal(plain: String, encrypt: (ByteArray) -> ByteArray): String {
        val text = plain.trim()
        if (text.isEmpty()) return ""
        return SecretKv.wrap(text, encrypt)
    }

    fun open(stored: String?, decrypt: (ByteArray) -> ByteArray): String {
        if (stored.isNullOrBlank()) return ""
        return SecretKv.unwrap(stored, decrypt).orEmpty()
    }
}
