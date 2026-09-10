package app.rope.android.data

/**
 * Telegram-like delete from the fullscreen photo/video viewer.
 * Outgoing only; closes the overlay. LocalStore stays v6.
 */
object ViewerDelRules {
    const val ACTION = "Удалить"

    fun canDelete(msg: ChatMessage): Boolean = ChatActions.canDelete(msg)

    fun current(album: List<ChatMessage>, page: Int, fallback: ChatMessage): ChatMessage =
        album.getOrNull(page) ?: fallback
}
