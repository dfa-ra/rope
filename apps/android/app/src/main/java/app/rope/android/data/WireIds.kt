package app.rope.android.data

/**
 * MEDIA / GROUP_TEXT ids (object_id, group_id, album_id, reply target).
 * Fail closed on CR/LF/NUL before trim so a newline prefix cannot become a
 * live group, object path, or reply id. Spaces still trim. Caption/text keep
 * newlines. Not envelope crypto and not WSS Phase B.
 */
object WireIds {
    fun parse(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        if (hasControl(raw)) return null
        return JsonIds.optional(raw)
    }

    fun hasControl(raw: String?): Boolean {
        if (raw == null) return false
        return raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0
    }
}
