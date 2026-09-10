package app.rope.android.data

/**
 * Telegram-like chat-list chip «Черновики»: rows whose composer draft is
 * non-blank. Does not add a ChatListMode (unread/dm/muted chips are sibling
 * tickets). LocalStore stays v6. No FCM.
 */
object DraftFilterRules {
    const val CHIP = "Черновики"
    const val CHIP_ALL = "Все"
    const val EMPTY = "Нет черновиков"
    const val EMPTY_BODY = "Начните писать в чате — черновик появится здесь."

    fun hasDraft(raw: String): Boolean = ChatListPreviewRules.clip(raw).isNotEmpty()

    fun hasDraft(c: Conversation): Boolean = hasDraft(c.draft)

    fun showsChips(mode: ChatListMode): Boolean = mode == ChatListMode.ALL

    fun apply(conversations: List<Conversation>, draftsOnly: Boolean): List<Conversation> =
        if (draftsOnly) conversations.filter(::hasDraft) else conversations

    fun hideArchiveRow(draftsOnly: Boolean): Boolean = draftsOnly

    fun showFab(draftsOnly: Boolean, mode: ChatListMode, query: String, forwarding: Boolean): Boolean =
        !draftsOnly && ChatListEmptyRules.showFab(mode, query, forwarding)

    fun emptyCopy(
        draftsOnly: Boolean,
        mode: ChatListMode,
        query: String,
        forwarding: Boolean,
        role: String?,
    ): ChatListEmptyCopy {
        if (!draftsOnly) return ChatListEmptyRules.copy(mode, query, forwarding, role)
        if (ChatListRules.searching(query)) {
            return ChatListEmptyCopy(
                title = ChatListEmptyRules.SEARCH_TITLE,
                body = searchBody(query),
                actionLabel = null,
                showFab = false,
            )
        }
        return ChatListEmptyCopy(
            title = EMPTY,
            body = EMPTY_BODY,
            actionLabel = null,
            showFab = false,
        )
    }

    fun searchBody(query: String): String {
        val q = ChatListRules.normalize(query)
        val full = "Нет черновиков по запросу «$q»."
        return if (full.length <= ChatListEmptyRules.COPY_MAX) full else ChatListEmptyRules.SEARCH_BODY_FALLBACK
    }
}
