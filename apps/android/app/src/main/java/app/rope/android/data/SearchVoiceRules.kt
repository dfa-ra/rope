package app.rope.android.data

/**
 * In-chat chip «Голосовые». Not the search-kind photo/file/link bar,
 * not media-hub. Local filter only.
 */
object SearchVoiceRules {
    const val LABEL = "Голосовые"
    const val EMPTY_BODY = "Нет голосовых в этом чате."

    fun hits(msg: ChatMessage, voiceOnly: Boolean): Boolean {
        if (!voiceOnly) return true
        return !msg.deleted && msg.kind == MessageKind.VOICE
    }

    fun matches(msg: ChatMessage, query: String, voiceOnly: Boolean): Boolean =
        hits(msg, voiceOnly) && MessageSearch.matches(msg, query)

    fun searching(query: String, voiceOnly: Boolean): Boolean =
        voiceOnly || ChatListRules.searching(query)

    fun emptyBody(query: String, voiceOnly: Boolean): String =
        if (voiceOnly && !ChatListRules.searching(query)) EMPTY_BODY else ThreadEmptyRules.searchBody(query)
}
