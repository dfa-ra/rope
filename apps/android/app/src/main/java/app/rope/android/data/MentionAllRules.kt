package app.rope.android.data

/**
 * Group @всем mention. Highlights the token in group threads. No FCM,
 * no mute-mentions, no picker restaff.
 */
object MentionAllRules {
    const val TOKEN = "всем"
    const val LABEL = "@всем"

    fun include(isGroup: Boolean): Boolean = isGroup

    fun names(raw: List<String>, isGroup: Boolean): List<String> {
        val clean = raw.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val withoutAll = clean.filter { !it.equals(TOKEN, ignoreCase = true) }
        if (!isGroup) return withoutAll
        return listOf(TOKEN) + withoutAll
    }

    fun isAll(text: String): Boolean =
        text.trim().equals(LABEL, ignoreCase = true) || text.trim().equals(TOKEN, ignoreCase = true)
}
