package app.rope.android.data

/**
 * Group avatar on Group info. Local file + kv `group_photos` only.
 * Distinct from own Settings photo. Not envelope crypto. LocalStore stays v6.
 */
object GroupPhotoRules {
    const val TITLE = "Фото группы"
    const val HINT = "Только на этом устройстве. Не шифрование."
    const val NEED_PHOTO = "Нужно фото"

    fun parsePath(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) return null
        val v = raw.trim()
        return v.takeIf { it.isNotEmpty() }
    }

    fun key(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) return null
        val v = raw.trim()
        if (v.isEmpty()) return null
        val stripped = if (ChatIds.isGroup(v)) ChatIds.rawGroupId(v) else v
        return stripped.takeIf { it.isNotEmpty() }
    }

    fun acceptsMime(mime: String): Boolean = mime.lowercase().startsWith("image/")

    fun canSet(
        isMember: Boolean,
        myId: String?,
        organizerId: String,
        serverRole: String?,
    ): Boolean = RoleRules.canRenameGroup(isMember, myId, organizerId, serverRole)

    fun lookup(photos: Map<String, String>, id: String?): String? {
        val k = key(id) ?: return null
        return parsePath(photos[k])
    }

    fun parseMap(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val o = org.json.JSONObject(raw)
            buildMap {
                o.keys().forEach { key ->
                    val k = key(key) ?: return@forEach
                    val path = parsePath(o.optString(key)) ?: return@forEach
                    put(k, path)
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
            val path = parsePath(v) ?: return@forEach
            o.put(key, path)
        }
        return o.toString()
    }
}
