package app.rope.android.data

enum class SavedVideoChip { ALL, VIDEO }

/**
 * Избранное chip **Видео**. Distinct from Saved kind chips (#281
 * Все / Фото / Файлы / Ссылки) and from Saved Голосовые. LocalStore stays v6.
 */
object SavedVideoRules {
    const val CHIP_ALL = "Все"
    const val LABEL = "Видео"
    const val EMPTY_TITLE = "Нет видео"
    const val EMPTY_BODY = "В Избранном нет видео."

    fun shows(peerId: String?, isGroup: Boolean): Boolean =
        SavedMessagesRules.isSaved(peerId) && !isGroup

    fun chips(): List<SavedVideoChip> =
        listOf(SavedVideoChip.ALL, SavedVideoChip.VIDEO)

    fun label(chip: SavedVideoChip): String = when (chip) {
        SavedVideoChip.ALL -> CHIP_ALL
        SavedVideoChip.VIDEO -> LABEL
    }

    fun parse(raw: String?): SavedVideoChip = when (raw) {
        "video" -> SavedVideoChip.VIDEO
        else -> SavedVideoChip.ALL
    }

    fun kv(chip: SavedVideoChip): String = when (chip) {
        SavedVideoChip.ALL -> "all"
        SavedVideoChip.VIDEO -> "video"
    }

    fun hits(msg: ChatMessage, chip: SavedVideoChip): Boolean {
        if (chip == SavedVideoChip.ALL) return true
        if (msg.deleted) return false
        return msg.kind == MessageKind.VIDEO
    }

    fun apply(messages: List<ChatMessage>, chip: SavedVideoChip): List<ChatMessage> =
        if (chip == SavedVideoChip.ALL) messages else messages.filter { hits(it, chip) }

    fun emptyTitle(chip: SavedVideoChip): String = when (chip) {
        SavedVideoChip.ALL -> SavedMessagesRules.IDLE_TITLE
        SavedVideoChip.VIDEO -> EMPTY_TITLE
    }

    fun emptyBody(chip: SavedVideoChip): String = when (chip) {
        SavedVideoChip.ALL -> SavedMessagesRules.IDLE_BODY
        SavedVideoChip.VIDEO -> EMPTY_BODY
    }

    fun emptyCopy(chip: SavedVideoChip, query: String): ThreadEmptyCopy {
        if (ChatListRules.searching(query)) return ThreadEmptyRules.copy(query, saved = true)
        if (chip == SavedVideoChip.ALL) return ThreadEmptyRules.copy("", saved = true)
        return ThreadEmptyCopy(emptyTitle(chip), emptyBody(chip))
    }
}
