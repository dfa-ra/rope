package app.rope.android.data

enum class SavedKind { ALL, PHOTO, FILE, LINK }

/**
 * Избранное chips Все / Фото / Файлы / Ссылки. Distinct from in-chat
 * search-kind (#209): these sit on the Saved thread even when search is closed.
 */
object SavedKindRules {
    const val CHIP_ALL = "Все"
    const val PHOTO = "Фото"
    const val FILE = "Файлы"
    const val LINK = "Ссылки"

    fun shows(peerId: String?, isGroup: Boolean): Boolean =
        SavedMessagesRules.isSaved(peerId) && !isGroup

    fun chips(): List<SavedKind> =
        listOf(SavedKind.ALL, SavedKind.PHOTO, SavedKind.FILE, SavedKind.LINK)

    fun label(kind: SavedKind): String = when (kind) {
        SavedKind.ALL -> CHIP_ALL
        SavedKind.PHOTO -> PHOTO
        SavedKind.FILE -> FILE
        SavedKind.LINK -> LINK
    }

    fun parse(raw: String?): SavedKind = when (raw) {
        "photo" -> SavedKind.PHOTO
        "file" -> SavedKind.FILE
        "link" -> SavedKind.LINK
        else -> SavedKind.ALL
    }

    fun kv(kind: SavedKind): String = when (kind) {
        SavedKind.ALL -> "all"
        SavedKind.PHOTO -> "photo"
        SavedKind.FILE -> "file"
        SavedKind.LINK -> "link"
    }

    fun hasLink(msg: ChatMessage): Boolean =
        msg.linkPreview != null || !LinkPreviewRules.firstHttps(msg.text).isNullOrBlank()

    fun hits(msg: ChatMessage, kind: SavedKind): Boolean {
        if (msg.deleted && kind != SavedKind.ALL) return false
        return when (kind) {
            SavedKind.ALL -> true
            SavedKind.PHOTO -> msg.kind == MessageKind.IMAGE || msg.kind == MessageKind.VIDEO
            SavedKind.FILE -> msg.kind == MessageKind.FILE
            SavedKind.LINK -> hasLink(msg)
        }
    }

    fun apply(messages: List<ChatMessage>, kind: SavedKind): List<ChatMessage> =
        if (kind == SavedKind.ALL) messages else messages.filter { hits(it, kind) }

    fun emptyTitle(kind: SavedKind): String = when (kind) {
        SavedKind.ALL -> SavedMessagesRules.IDLE_TITLE
        SavedKind.PHOTO -> "Нет фото"
        SavedKind.FILE -> "Нет файлов"
        SavedKind.LINK -> "Нет ссылок"
    }

    fun emptyBody(kind: SavedKind): String = when (kind) {
        SavedKind.ALL -> SavedMessagesRules.IDLE_BODY
        SavedKind.PHOTO -> "В Избранном нет фото и видео."
        SavedKind.FILE -> "В Избранном нет файлов."
        SavedKind.LINK -> "В Избранном нет ссылок."
    }

    fun emptyCopy(kind: SavedKind, query: String): ThreadEmptyCopy {
        if (ChatListRules.searching(query)) return ThreadEmptyRules.copy(query, saved = true)
        if (kind == SavedKind.ALL) return ThreadEmptyRules.copy("", saved = true)
        return ThreadEmptyCopy(emptyTitle(kind), emptyBody(kind))
    }
}
