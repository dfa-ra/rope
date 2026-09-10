package app.rope.android.data

data class ChatListEmptyCopy(
    val title: String,
    val body: String,
    val actionLabel: String?,
    val showFab: Boolean,
)

/**
 * Telegram-like chat-list empty: a search miss is «Ничего не найдено», not the idle
 * «Пока никого нет» / forward empty. Hide FAB and CTA while searching.
 */
object ChatListEmptyRules {
    const val SEARCH_TITLE = "Ничего не найдено"
    const val SEARCH_BODY_FALLBACK = "Попробуйте изменить запрос."
    const val COPY_MAX = 80

    fun showFab(mode: ChatListMode, query: String, forwarding: Boolean): Boolean =
        !forwarding &&
            mode != ChatListMode.CALLS &&
            mode != ChatListMode.ARCHIVE &&
            mode != ChatListMode.UNREAD &&
            !ChatListRules.searching(query)

    fun copy(
        mode: ChatListMode,
        query: String,
        forwarding: Boolean,
        role: String?,
    ): ChatListEmptyCopy {
        if (ChatListRules.searching(query)) {
            return ChatListEmptyCopy(
                title = SEARCH_TITLE,
                body = searchBody(mode, query),
                actionLabel = null,
                showFab = false,
            )
        }
        val title = when (mode) {
            ChatListMode.GROUPS -> "Групп пока нет"
            ChatListMode.CALLS -> "Звонков ещё не было"
            ChatListMode.ARCHIVE -> ArchiveRules.EMPTY_TITLE
            ChatListMode.UNREAD -> ChatListRules.EMPTY_UNREAD
            ChatListMode.ALL -> "Пока никого нет"
        }
        val body = when {
            forwarding -> if (RoleRules.canInvite(role)) {
                "Некуда переслать. Пригласите человека или создайте группу."
            } else {
                "Некуда переслать. Когда появятся чаты, можно будет переслать сюда."
            }
            mode == ChatListMode.GROUPS -> RoleRules.groupsEmptyBody()
            mode == ChatListMode.CALLS -> RoleRules.callsEmptyBody(role)
            mode == ChatListMode.ARCHIVE -> ArchiveRules.DEVICE_ONLY
            mode == ChatListMode.UNREAD -> ChatListRules.EMPTY_UNREAD_BODY
            else -> RoleRules.chatsEmptyBody(role)
        }
        val actionLabel = if (mode == ChatListMode.GROUPS && !forwarding) "Новая группа" else null
        return ChatListEmptyCopy(
            title = title,
            body = body,
            actionLabel = actionLabel,
            showFab = showFab(mode, query, forwarding),
        )
    }

    fun searchBody(mode: ChatListMode, query: String): String {
        val q = ChatListRules.normalize(query)
        val noun = when (mode) {
            ChatListMode.GROUPS -> "групп"
            ChatListMode.CALLS -> "звонков"
            ChatListMode.ALL, ChatListMode.UNREAD, ChatListMode.ARCHIVE -> "чатов"
        }
        val full = "Нет $noun по запросу «$q»."
        return if (full.length <= COPY_MAX) full else SEARCH_BODY_FALLBACK
    }
}
