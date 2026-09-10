package app.rope.android.data

enum class MessageSearchKind { ALL, PHOTO, FILE, LINK }

/**
 * In-chat Telegram chips: Фото / Файлы / Ссылки. Text query still uses
 * [MessageSearch]. Not the media-hub hub, not chat-list search.
 */
object MessageSearchKindRules {
    const val PHOTO = "Фото"
    const val FILE = "Файлы"
    const val LINK = "Ссылки"

    fun chips(): List<MessageSearchKind> =
        listOf(MessageSearchKind.PHOTO, MessageSearchKind.FILE, MessageSearchKind.LINK)

    fun label(kind: MessageSearchKind): String = when (kind) {
        MessageSearchKind.ALL -> ""
        MessageSearchKind.PHOTO -> PHOTO
        MessageSearchKind.FILE -> FILE
        MessageSearchKind.LINK -> LINK
    }

    fun toggle(current: MessageSearchKind, tapped: MessageSearchKind): MessageSearchKind =
        if (current == tapped) MessageSearchKind.ALL else tapped

    fun searching(query: String, kind: MessageSearchKind): Boolean =
        kind != MessageSearchKind.ALL || ChatListRules.searching(query)

    fun hits(msg: ChatMessage, kind: MessageSearchKind): Boolean = when (kind) {
        MessageSearchKind.ALL -> true
        MessageSearchKind.PHOTO -> msg.kind == MessageKind.IMAGE || msg.kind == MessageKind.VIDEO
        MessageSearchKind.FILE -> msg.kind == MessageKind.FILE
        MessageSearchKind.LINK -> hasLink(msg)
    }

    fun hasLink(msg: ChatMessage): Boolean =
        msg.linkPreview != null || !LinkPreviewRules.firstHttps(msg.text).isNullOrBlank()

    fun emptyBody(kind: MessageSearchKind): String = when (kind) {
        MessageSearchKind.PHOTO -> "Нет фото в этом чате."
        MessageSearchKind.FILE -> "Нет файлов в этом чате."
        MessageSearchKind.LINK -> "Нет ссылок в этом чате."
        MessageSearchKind.ALL -> ThreadEmptyRules.SEARCH_BODY_FALLBACK
    }
}
