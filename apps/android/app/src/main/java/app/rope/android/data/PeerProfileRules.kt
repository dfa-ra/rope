package app.rope.android.data

/**
 * Telegram-like 1:1 peer profile. Shared photos are IMAGE rows; shared
 * voice notes are VOICE rows already in LocalStore — no FCM.
 */
object PeerProfileRules {
    const val GRID_COLUMNS = 3
    const val SECTION = "Общие медиа"

    fun headerClickable(isGroup: Boolean, hasPeer: Boolean, saved: Boolean = false): Boolean =
        !saved && (isGroup || hasPeer)

    fun opensPeerProfile(isGroup: Boolean, hasPeer: Boolean, saved: Boolean = false): Boolean =
        !saved && !isGroup && hasPeer

    fun title(peerName: String?): String = peerName?.trim().orEmpty().ifBlank { "Профиль" }

    fun photos(messages: List<ChatMessage>): List<ChatMessage> =
        messages
            .filter { it.kind == MessageKind.IMAGE && !it.deleted }
            .sortedWith(compareByDescending<ChatMessage> { it.timestampMs }.thenByDescending { it.id })

    const val VOICES = "Голосовые"

    fun voices(messages: List<ChatMessage>): List<ChatMessage> =
        messages
            .filter { it.kind == MessageKind.VOICE && !it.deleted }
            .sortedWith(compareByDescending<ChatMessage> { it.timestampMs }.thenByDescending { it.id })

    fun voiceDurationMs(m: ChatMessage): Long {
        if (m.extra.isBlank()) return 0L
        return runCatching { MediaPayload.parse(m.extra).durationMs }.getOrDefault(0L)
    }

    fun voiceTitle(m: ChatMessage): String {
        val ms = voiceDurationMs(m)
        return if (ms > 0L) "Голосовое · ${MediaPayload.formatDuration(ms)}" else "Голосовое"
    }

    fun voicesSectionLabel(count: Int): String = when {
        count <= 0 -> VOICES
        else -> "$VOICES · $count"
    }

    fun showPhotoEmpty(photoCount: Int, voiceCount: Int): Boolean =
        photoCount <= 0 && voiceCount <= 0

    fun emptyTitle(): String = "Нет общих медиа"

    fun emptyBody(): String = "Фото из этой переписки появятся здесь."

    fun sectionLabel(count: Int): String = when {
        count <= 0 -> SECTION
        else -> "$SECTION · $count"
    }
}
