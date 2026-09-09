package app.rope.android.data

import org.json.JSONObject

/**
 * Local contact nicknames. Kv JSON map deviceId → name. Not ChatPrefs.
 * LocalStore schema stays v6. Directory displayName is unchanged on the server.
 */
object NicknameRules {
    const val MAX = 32
    const val ACTION = "Имя"
    const val DIALOG_TITLE = "Имя в контактах"
    const val SAVE = "Сохранить"
    const val RESET = "Сбросить"
    const val CANCEL = "Отмена"

    fun normalize(raw: String?): String {
        val collapsed = raw.orEmpty().trim().replace(Regex("\\s+"), " ")
        if (collapsed.isEmpty()) return ""
        return if (collapsed.length <= MAX) collapsed else collapsed.take(MAX).trimEnd()
    }

    fun display(nick: String?, directoryName: String?, deviceId: String?): String {
        val n = normalize(nick)
        if (n.isNotEmpty()) return n
        val d = directoryName?.trim().orEmpty()
        if (d.isNotEmpty()) return d
        return deviceId.orEmpty().take(8).ifBlank { "Чат" }
    }

    fun isCustom(nick: String?): Boolean = normalize(nick).isNotEmpty()

    fun originalLine(nick: String?, directoryName: String?): String? {
        if (!isCustom(nick)) return null
        val d = directoryName?.trim().orEmpty()
        if (d.isEmpty() || d == normalize(nick)) return null
        return d
    }

    fun parse(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val o = JSONObject(raw)
            buildMap {
                o.keys().forEach { key ->
                    val id = key.trim()
                    val name = normalize(o.optString(key))
                    if (id.isNotEmpty() && name.isNotEmpty()) put(id, name)
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun toJson(map: Map<String, String>): String {
        val o = JSONObject()
        map.forEach { (k, v) ->
            val id = k.trim()
            val name = normalize(v)
            if (id.isNotEmpty() && name.isNotEmpty()) o.put(id, name)
        }
        return o.toString()
    }
}
