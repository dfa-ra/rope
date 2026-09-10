package app.rope.android.data

/**
 * Telegram-like forward from the fullscreen photo/video viewer.
 * Closes the viewer via startForward. LocalStore stays v6.
 */
object ViewerFwdRules {
    const val ACTION = "Переслать"

    fun canForward(msg: ChatMessage): Boolean = ChatActions.canForward(msg)

    fun current(album: List<ChatMessage>, page: Int, fallback: ChatMessage): ChatMessage =
        album.getOrNull(page) ?: fallback
}
