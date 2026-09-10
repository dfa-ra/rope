package app.rope.android.data

/**
 * Telegram-like search on the People tab. Filters the local directory by
 * display name (word-prefix over contains). No store bump.
 */
object PeopleSearchRules {
    const val PLACEHOLDER = "Поиск"
    const val SEARCH_TITLE = "Ничего не найдено"
    const val SEARCH_BODY_FALLBACK = "Попробуйте изменить запрос."
    const val COPY_MAX = 80

    fun searching(query: String): Boolean = ChatListRules.searching(query)

    fun showSearch(people: List<DirectoryDevice>): Boolean = people.isNotEmpty()

    fun displayName(d: DirectoryDevice): String =
        d.displayName.ifBlank { d.deviceId.take(8) }

    fun hit(d: DirectoryDevice, query: String): ChatListHit {
        val q = ChatListRules.normalize(query)
        if (q.isEmpty()) return ChatListHit.NONE
        val name = displayName(d)
        val folded = name.lowercase()
        if (folded.startsWith(q) || ChatListRules.wordPrefixIndex(name, query) >= 0) {
            return ChatListHit.TITLE_PREFIX
        }
        if (folded.contains(q)) return ChatListHit.TITLE
        return ChatListHit.NONE
    }

    fun matches(d: DirectoryDevice, query: String): Boolean {
        if (!searching(query)) return true
        return hit(d, query) != ChatListHit.NONE
    }

    fun rows(people: List<DirectoryDevice>, query: String): List<DirectoryDevice> {
        val filtered = people.filter { matches(it, query) }
        if (!searching(query)) return filtered
        return filtered.sortedWith { a, b ->
            val rank = hit(b, query).rank.compareTo(hit(a, query).rank)
            if (rank != 0) return@sortedWith rank
            displayName(a).compareTo(displayName(b), ignoreCase = true)
        }
    }

    fun searchBody(query: String): String {
        val q = ChatListRules.normalize(query)
        val full = "Нет людей по запросу «$q»."
        return if (full.length <= COPY_MAX) full else SEARCH_BODY_FALLBACK
    }
}
