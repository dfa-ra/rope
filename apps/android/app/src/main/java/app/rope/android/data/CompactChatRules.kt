package app.rope.android.data

/**
 * Compact chat-list rows (Telegram-like density). Default off. kv `compact_chats`.
 * LocalStore stays v6.
 */
object CompactChatRules {
    const val KEY = "compact_chats"
    const val TITLE = "Компактный список"
    const val AVATAR = 46
    const val AVATAR_COMPACT = 36
    const val PAD_V = 12
    const val PAD_H = 16
    const val PAD_V_COMPACT = 6

    fun enabledFromKv(raw: String?): Boolean = raw == "1"

    fun avatarDp(compact: Boolean): Int = if (compact) AVATAR_COMPACT else AVATAR

    fun rowPadV(compact: Boolean): Int = if (compact) PAD_V_COMPACT else PAD_V

    fun titleCompact(compact: Boolean): Boolean = compact

    fun hint(): String =
        "Меньше отступы и аватар в списке чатов. Как в Telegram. Не папки."
}
