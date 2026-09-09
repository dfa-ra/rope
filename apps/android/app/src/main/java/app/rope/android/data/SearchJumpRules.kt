package app.rope.android.data

/**
 * Telegram-like in-chat search jump: keep the thread, step older/newer hits.
 * Count is 1-based from the oldest match. Local only. LocalStore stays v6.
 */
object SearchJumpRules {
    const val OLDER = "Предыдущее"
    const val NEWER = "Следующее"
    const val NONE = "Нет"

    fun searching(query: String): Boolean = ChatListRules.searching(query)

    fun hits(messages: List<ChatMessage>, query: String): List<String> {
        if (!searching(query)) return emptyList()
        return messages
            .filter { !it.deleted && MessageSearch.matches(it, query) }
            .map { it.id }
    }

    fun indexOf(ids: List<String>, current: String?): Int {
        if (ids.isEmpty()) return -1
        val i = current?.let { ids.indexOf(it) } ?: -1
        return if (i >= 0) i else ids.lastIndex
    }

    fun latest(ids: List<String>): String? = ids.lastOrNull()

    fun older(ids: List<String>, current: String?): String? {
        if (ids.isEmpty()) return null
        val i = indexOf(ids, current)
        return ids[if (i <= 0) ids.lastIndex else i - 1]
    }

    fun newer(ids: List<String>, current: String?): String? {
        if (ids.isEmpty()) return null
        val i = indexOf(ids, current)
        return ids[(i + 1) % ids.size]
    }

    fun label(ids: List<String>, current: String?): String {
        if (ids.isEmpty()) return NONE
        return "${indexOf(ids, current) + 1} из ${ids.size}"
    }

    fun keepPlace(query: String): Boolean = searching(query)
}
