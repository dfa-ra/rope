package app.rope.android.data

/**
 * Telegram-like group info shared photos. IMAGE rows already in LocalStore
 * for this group thread — not the 1:1 peer profile, no FCM.
 */
object GroupInfoRules {
    const val GRID_COLUMNS = 3
    const val SECTION = "Общие медиа"

    fun photos(messages: List<ChatMessage>): List<ChatMessage> =
        messages
            .filter { it.kind == MessageKind.IMAGE && !it.deleted }
            .sortedWith(compareByDescending<ChatMessage> { it.timestampMs }.thenByDescending { it.id })

    fun sectionLabel(count: Int): String = when {
        count <= 0 -> SECTION
        else -> "$SECTION · $count"
    }
}
