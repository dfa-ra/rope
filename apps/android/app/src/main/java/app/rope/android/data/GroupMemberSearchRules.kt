package app.rope.android.data

/**
 * Telegram-like search of group members on group info. Filters the local
 * member list by display name (word-prefix over contains). No store bump.
 */
object GroupMemberSearchRules {
    const val PLACEHOLDER = "Поиск"
    const val SEARCH_TITLE = "Ничего не найдено"
    const val SEARCH_BODY_FALLBACK = "Попробуйте изменить запрос."
    const val COPY_MAX = 80

    fun searching(query: String): Boolean = ChatListRules.searching(query)

    fun showSearch(memberCount: Int): Boolean = memberCount > 0

    fun displayName(id: String, myId: String?, names: Map<String, String>): String =
        GroupChatUx.memberDisplayName(id, myId, names)

    fun hit(label: String, query: String): ChatListHit {
        val q = ChatListRules.normalize(query)
        if (q.isEmpty()) return ChatListHit.NONE
        val folded = label.lowercase()
        if (folded.startsWith(q) || ChatListRules.wordPrefixIndex(label, query) >= 0) {
            return ChatListHit.TITLE_PREFIX
        }
        if (folded.contains(q)) return ChatListHit.TITLE
        return ChatListHit.NONE
    }

    fun matches(label: String, query: String): Boolean {
        if (!searching(query)) return true
        return hit(label, query) != ChatListHit.NONE
    }

    fun rows(
        memberIds: List<String>,
        myId: String?,
        names: Map<String, String>,
        query: String,
    ): List<String> {
        val labeled = memberIds.map { it to displayName(it, myId, names) }
        val filtered = labeled.filter { matches(it.second, query) }
        if (!searching(query)) return filtered.map { it.first }
        return filtered.sortedWith { a, b ->
            val rank = hit(b.second, query).rank.compareTo(hit(a.second, query).rank)
            if (rank != 0) return@sortedWith rank
            a.second.compareTo(b.second, ignoreCase = true)
        }.map { it.first }
    }

    fun searchBody(query: String): String {
        val q = ChatListRules.normalize(query)
        val full = "Нет участников по запросу «$q»."
        return if (full.length <= COPY_MAX) full else SEARCH_BODY_FALLBACK
    }
}
