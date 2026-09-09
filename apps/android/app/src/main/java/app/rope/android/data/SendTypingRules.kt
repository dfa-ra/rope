package app.rope.android.data

/**
 * Telegram-like typing privacy. Kv-only. When off, this phone does not
 * send TYPING receipts. Incoming indicators are unchanged.
 * LocalStore schema stays v6.
 */
object SendTypingRules {
    const val SECTION = "Конфиденциальность"
    const val TITLE = "Печатает…"

    fun hint(): String =
        "Пока включено, в чате видно, что вы набираете. Сервер текст не видит."

    fun shouldSend(enabled: Boolean, lastSentAt: Long, now: Long, draft: String): Boolean =
        enabled && TypingRules.shouldSend(lastSentAt, now, draft)
}
