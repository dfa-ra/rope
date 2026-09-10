package app.rope.android.data

/**
 * Telegram-like 1:1 peer profile. Shared media are IMAGE and VIDEO rows
 * already in LocalStore for this chat — no FCM, no cloud gallery.
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
            .filter { isSharedMedia(it.kind) && !it.deleted }
            .sortedWith(compareByDescending<ChatMessage> { it.timestampMs }.thenByDescending { it.id })

    fun isSharedMedia(kind: MessageKind): Boolean =
        kind == MessageKind.IMAGE || kind == MessageKind.VIDEO

    fun tileLabel(kind: MessageKind): String =
        if (kind == MessageKind.VIDEO) "Видео" else "Фото"

    fun emptyTitle(): String = "Нет общих медиа"

    fun emptyBody(): String = "Фото и видео из этой переписки появятся здесь."

    fun sectionLabel(count: Int): String = when {
        count <= 0 -> SECTION
        else -> "$SECTION · $count"
    }
}
