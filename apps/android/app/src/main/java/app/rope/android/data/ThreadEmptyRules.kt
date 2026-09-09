package app.rope.android.data

data class ThreadEmptyCopy(
    val title: String,
    val body: String,
)

/**
 * Telegram-like in-thread empty: a search miss is «Ничего не найдено», not the
 * colloquial «Ничего не нашли» or the idle «Начните переписку».
 */
object ThreadEmptyRules {
    const val SEARCH_TITLE = "Ничего не найдено"
    const val IDLE_TITLE = "Начните переписку"
    const val SEARCH_BODY_FALLBACK = "Попробуйте изменить запрос."
    const val COPY_MAX = 80

    fun copy(query: String): ThreadEmptyCopy {
        if (ChatListRules.searching(query)) {
            return ThreadEmptyCopy(
                title = SEARCH_TITLE,
                body = searchBody(query),
            )
        }
        return ThreadEmptyCopy(
            title = IDLE_TITLE,
            body = RoleRules.threadEmptyBody(),
        )
    }

    fun searchBody(query: String): String {
        val q = ChatListRules.normalize(query)
        val full = "Нет сообщений по запросу «$q»."
        return if (full.length <= COPY_MAX) full else SEARCH_BODY_FALLBACK
    }
}
