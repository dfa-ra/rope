package app.rope.android.data

/**
 * Telegram-like «В чате» from the fullscreen photo/video viewer.
 * Closes the overlay and scrolls to the bubble. LocalStore stays v6.
 */
object ViewerJumpRules {
    const val ACTION = "В чате"

    fun canJump(msg: ChatMessage): Boolean = !msg.deleted && msg.id.isNotBlank()

    fun current(album: List<ChatMessage>, page: Int, fallback: ChatMessage): ChatMessage =
        album.getOrNull(page) ?: fallback
}
