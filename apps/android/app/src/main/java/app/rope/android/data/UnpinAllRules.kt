package app.rope.android.data

/**
 * Telegram-like unpin-all on the chat list. Same kv `chat_prefs.pinned`
 * as long-press pin. No LocalStore bump.
 */
object UnpinAllRules {
    const val ACTION = "Открепить все"
    const val CONFIRM = "Точно открепить все"
    const val MIN = 2

    fun ids(conversations: List<Conversation>): List<String> =
        conversations.filter { it.pinned }.map { it.id }

    fun visible(pinnedCount: Int, forwarding: Boolean, searching: Boolean): Boolean =
        pinnedCount >= MIN && !forwarding && !searching

    fun showInRowMenu(pinned: Boolean, pinnedCount: Int): Boolean =
        pinned && pinnedCount >= MIN

    fun prefsAfter(cur: ChatPrefs): ChatPrefs = cur.copy(pinned = false)
}
