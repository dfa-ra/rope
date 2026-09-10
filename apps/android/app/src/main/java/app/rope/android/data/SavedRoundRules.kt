package app.rope.android.data

enum class SavedRoundChip { ALL, ROUND }

/**
 * Избранное chip **Кружки**. Distinct from Saved Видео (#345) and
 * Голосовые (#324). LocalStore stays v6.
 */
object SavedRoundRules {
    const val CHIP_ALL = "Все"
    const val LABEL = "Кружки"
    const val EMPTY_TITLE = "Нет кружков"
    const val EMPTY_BODY = "В Избранном нет кружков."

    fun shows(peerId: String?, isGroup: Boolean): Boolean =
        SavedMessagesRules.isSaved(peerId) && !isGroup

    fun chips(): List<SavedRoundChip> =
        listOf(SavedRoundChip.ALL, SavedRoundChip.ROUND)

    fun label(chip: SavedRoundChip): String = when (chip) {
        SavedRoundChip.ALL -> CHIP_ALL
        SavedRoundChip.ROUND -> LABEL
    }

    fun parse(raw: String?): SavedRoundChip = when (raw) {
        "round" -> SavedRoundChip.ROUND
        else -> SavedRoundChip.ALL
    }

    fun kv(chip: SavedRoundChip): String = when (chip) {
        SavedRoundChip.ALL -> "all"
        SavedRoundChip.ROUND -> "round"
    }

    fun hits(msg: ChatMessage, chip: SavedRoundChip): Boolean {
        if (chip == SavedRoundChip.ALL) return true
        if (msg.deleted) return false
        return msg.kind == MessageKind.VIDEO_NOTE
    }

    fun apply(messages: List<ChatMessage>, chip: SavedRoundChip): List<ChatMessage> =
        if (chip == SavedRoundChip.ALL) messages else messages.filter { hits(it, chip) }

    fun emptyTitle(chip: SavedRoundChip): String = when (chip) {
        SavedRoundChip.ALL -> SavedMessagesRules.IDLE_TITLE
        SavedRoundChip.ROUND -> EMPTY_TITLE
    }

    fun emptyBody(chip: SavedRoundChip): String = when (chip) {
        SavedRoundChip.ALL -> SavedMessagesRules.IDLE_BODY
        SavedRoundChip.ROUND -> EMPTY_BODY
    }

    fun emptyCopy(chip: SavedRoundChip, query: String): ThreadEmptyCopy {
        if (ChatListRules.searching(query)) return ThreadEmptyRules.copy(query, saved = true)
        if (chip == SavedRoundChip.ALL) return ThreadEmptyRules.copy("", saved = true)
        return ThreadEmptyCopy(emptyTitle(chip), emptyBody(chip))
    }
}
