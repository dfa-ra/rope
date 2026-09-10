package app.rope.android.data

/**
 * Telegram-like 1:1 peer profile. Shared photos are the IMAGE rows already
 * in LocalStore for this chat — no FCM, no cloud gallery.
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

    const val FILES = "Файлы"

    fun files(messages: List<ChatMessage>): List<ChatMessage> =
        messages
            .filter { it.kind == MessageKind.FILE && !it.deleted }
            .sortedWith(compareByDescending<ChatMessage> { it.timestampMs }.thenByDescending { it.id })

    fun fileTitle(m: ChatMessage): String = m.text.trim().ifBlank { "Файл" }

    fun filesSectionLabel(count: Int): String = when {
        count <= 0 -> FILES
        else -> "$FILES · $count"
    }

    fun showPhotoEmpty(photoCount: Int, fileCount: Int): Boolean =
        photoCount <= 0 && fileCount <= 0

    fun emptyTitle(): String = "Нет общих медиа"

    fun emptyBody(): String = "Фото из этой переписки появятся здесь."

    fun sectionLabel(count: Int): String = when {
        count <= 0 -> SECTION
        else -> "$SECTION · $count"
    }
}
