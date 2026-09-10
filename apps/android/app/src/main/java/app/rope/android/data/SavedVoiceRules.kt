package app.rope.android.data

enum class SavedVoiceChip { ALL, VOICE }

/**
 * Избранное chip **Голосовые**. Distinct from Saved kind chips (#281
 * Все / Фото / Файлы / Ссылки) and from in-chat search-voice: this sits on
 * the Saved thread even when search is closed. LocalStore stays v6.
 */
object SavedVoiceRules {
    const val CHIP_ALL = "Все"
    const val LABEL = "Голосовые"
    const val EMPTY_TITLE = "Нет голосовых"
    const val EMPTY_BODY = "В Избранном нет голосовых."

    fun shows(peerId: String?, isGroup: Boolean): Boolean =
        SavedMessagesRules.isSaved(peerId) && !isGroup

    fun chips(): List<SavedVoiceChip> =
        listOf(SavedVoiceChip.ALL, SavedVoiceChip.VOICE)

    fun label(chip: SavedVoiceChip): String = when (chip) {
        SavedVoiceChip.ALL -> CHIP_ALL
        SavedVoiceChip.VOICE -> LABEL
    }

    fun parse(raw: String?): SavedVoiceChip = when (raw) {
        "voice" -> SavedVoiceChip.VOICE
        else -> SavedVoiceChip.ALL
    }

    fun kv(chip: SavedVoiceChip): String = when (chip) {
        SavedVoiceChip.ALL -> "all"
        SavedVoiceChip.VOICE -> "voice"
    }

    fun hits(msg: ChatMessage, chip: SavedVoiceChip): Boolean {
        if (chip == SavedVoiceChip.ALL) return true
        if (msg.deleted) return false
        return msg.kind == MessageKind.VOICE
    }

    fun apply(messages: List<ChatMessage>, chip: SavedVoiceChip): List<ChatMessage> =
        if (chip == SavedVoiceChip.ALL) messages else messages.filter { hits(it, chip) }

    fun emptyTitle(chip: SavedVoiceChip): String = when (chip) {
        SavedVoiceChip.ALL -> SavedMessagesRules.IDLE_TITLE
        SavedVoiceChip.VOICE -> EMPTY_TITLE
    }

    fun emptyBody(chip: SavedVoiceChip): String = when (chip) {
        SavedVoiceChip.ALL -> SavedMessagesRules.IDLE_BODY
        SavedVoiceChip.VOICE -> EMPTY_BODY
    }

    fun emptyCopy(chip: SavedVoiceChip, query: String): ThreadEmptyCopy {
        if (ChatListRules.searching(query)) return ThreadEmptyRules.copy(query, saved = true)
        if (chip == SavedVoiceChip.ALL) return ThreadEmptyRules.copy("", saved = true)
        return ThreadEmptyCopy(emptyTitle(chip), emptyBody(chip))
    }
}
