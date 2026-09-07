package app.rope.android.data

import org.json.JSONArray
import org.json.JSONObject

data class IceServerSpec(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null,
) {
    val hasTurn: Boolean
        get() = urls.any { IceServers.isTurnUrl(it) }
}

object IceServers {
    fun parse(raw: String?): List<IceServerSpec> {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty() || text.equals("null", ignoreCase = true)) return emptyList()
        return try {
            parseArray(JSONArray(text))
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun fromInfo(obj: JSONObject): List<IceServerSpec> =
        parseArray(obj.optJSONArray("ice_servers"))

    fun parseArray(arr: JSONArray?): List<IceServerSpec> {
        if (arr == null) return emptyList()
        val out = mutableListOf<IceServerSpec>()
        for (i in 0 until arr.length()) {
            val item = arr.opt(i) ?: continue
            if (item is String) {
                val url = item.trim()
                if (url.isNotEmpty() && !url.equals("null", ignoreCase = true)) {
                    out += IceServerSpec(listOf(url))
                }
                continue
            }
            if (item !is JSONObject) continue
            val urls = urlsOf(item)
            if (urls.isEmpty()) continue
            out += IceServerSpec(
                urls = urls,
                username = JsonIds.optional(item.optString("username")),
                credential = JsonIds.optional(item.optString("credential")),
            )
        }
        return out
    }

    fun resolve(serverProvided: List<IceServerSpec>): List<IceServerSpec> {
        val cleaned = serverProvided.map { spec ->
            spec.copy(
                urls = spec.urls.map { it.trim() }.filter { it.isNotEmpty() && !it.equals("null", ignoreCase = true) },
                username = JsonIds.optional(spec.username),
                credential = JsonIds.optional(spec.credential),
            )
        }.filter { it.urls.isNotEmpty() }
        return if (cleaned.isNotEmpty()) cleaned else fallbackStun()
    }

    fun fallbackStun(): List<IceServerSpec> =
        CallMedia.STUN_URLS.map { IceServerSpec(listOf(it)) }

    fun usesPublicStunFallback(resolved: List<IceServerSpec>): Boolean {
        if (resolved.isEmpty()) return true
        val urls = resolved.flatMap { it.urls }
        return urls.isNotEmpty() && urls.all { it in CallMedia.STUN_URLS }
    }

    fun isTurnUrl(url: String): Boolean {
        val u = url.trim().lowercase()
        return u.startsWith("turn:") || u.startsWith("turns:")
    }

    private fun urlsOf(obj: JSONObject): List<String> {
        val raw = obj.opt("urls")
        val values = when (raw) {
            is JSONArray -> buildList {
                for (i in 0 until raw.length()) add(raw.optString(i))
            }
            is String -> listOf(raw)
            else -> emptyList()
        }
        return values.map { it.trim() }.filter { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
    }
}
