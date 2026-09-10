package app.rope.android.data

/**
 * WSS call `to` peer ids. Fail closed on CR/LF/NUL before trim so a newline
 * prefix cannot be stripped into a live device/member id.
 */
object CallToRules {
    fun parse(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) {
            return null
        }
        val id = raw.trim().lowercase()
        if (id.isEmpty() || id != raw.lowercase()) return null
        return id
    }
}
