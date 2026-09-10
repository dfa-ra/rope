package app.rope.android.data

import org.json.JSONArray

/**
 * Picker «Недавние»: most-recent-first, capped, local kv JSON.
 * Inner JSON is not envelope crypto.
 */
object RecentEmojiRules {
    const val ID = "recent"
    const val LABEL = "Недавние"
    const val ICON = "🕒"
    const val CAP = 24

    fun parse(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            normalize(
                buildList {
                    for (i in 0 until arr.length()) {
                        add(arr.optString(i))
                    }
                },
            )
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun toJson(list: List<String>): String {
        val arr = JSONArray()
        normalize(list).forEach { arr.put(it) }
        return arr.toString()
    }

    fun remember(existing: List<String>, emoji: String): List<String> =
        normalize(listOf(emoji) + existing)

    fun normalize(list: List<String>): List<String> =
        list.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(CAP)

    fun tabs(recents: List<String>): List<EmojiCategory> =
        listOf(EmojiCategory(ID, LABEL, ICON, recents)) + EmojiPack.categories

    fun initialCategory(recents: List<String>): String =
        if (recents.isNotEmpty()) ID else EmojiPack.categories.first().id

    fun shown(categoryId: String, query: String, recents: List<String>): List<String> {
        val q = query.trim()
        if (q.isNotEmpty()) return EmojiPack.search(q)
        if (categoryId == ID) return recents
        return EmojiPack.category(categoryId)?.emojis.orEmpty()
    }
}
