package app.rope.android.data

/**
 * Local group about on Group info. Distinct from Settings «О себе» and from
 * group avatar. Kv `group_abouts` only — no PATCH, no LocalStore bump.
 */
object GroupAboutRules {
    const val KV = "group_abouts"
    const val TITLE = "О группе"
    const val HINT = "Только на этом устройстве. Не «О себе» и не фото группы."
    const val MAX = 120

    fun sanitize(raw: String?): String {
        if (raw.isNullOrEmpty()) return ""
        val t = buildString {
            for (ch in raw) {
                if (ch != '\n' && ch != '\r' && ch != '\u0000') append(ch)
            }
        }.trim()
        return if (t.length <= MAX) t else t.take(MAX)
    }

    fun remaining(raw: String?): Int = (MAX - sanitize(raw).length).coerceAtLeast(0)

    fun key(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) return null
        val v = raw.trim()
        if (v.isEmpty()) return null
        val stripped = if (ChatIds.isGroup(v)) ChatIds.rawGroupId(v) else v
        return stripped.takeIf { it.isNotEmpty() }
    }

    fun canSet(
        isMember: Boolean,
        myId: String?,
        organizerId: String,
        serverRole: String?,
    ): Boolean = RoleRules.canRenameGroup(isMember, myId, organizerId, serverRole)

    fun lookup(abouts: Map<String, String>, id: String?): String {
        val k = key(id) ?: return ""
        return sanitize(abouts[k])
    }

    fun parseMap(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val o = org.json.JSONObject(raw)
            buildMap {
                o.keys().forEach { key ->
                    val k = key(key) ?: return@forEach
                    val about = sanitize(o.optString(key))
                    if (about.isNotEmpty()) put(k, about)
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun encode(map: Map<String, String>): String {
        val o = org.json.JSONObject()
        map.forEach { (k, v) ->
            val key = key(k) ?: return@forEach
            val about = sanitize(v)
            if (about.isNotEmpty()) o.put(key, about)
        }
        return o.toString()
    }
}
