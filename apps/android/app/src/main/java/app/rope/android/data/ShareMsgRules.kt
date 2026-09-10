package app.rope.android.data

/**
 * Telegram-like «Поделиться» of a text bubble via ACTION_SEND.
 * IMAGE / VIDEO / FILE stay in-app (or the media share path). LocalStore v6.
 */
object ShareMsgRules {
    const val ACTION = "Поделиться"
    const val CHOOSER = "Поделиться"
    const val SEND_ACTION = "android.intent.action.SEND"
    const val EXTRA_TEXT = "android.intent.extra.TEXT"
    const val MIME = "text/plain"
    const val BODY_MAX = 4096

    fun canShare(msg: ChatMessage): Boolean = body(msg) != null

    fun body(msg: ChatMessage): String? {
        if (msg.deleted) return null
        if (msg.kind != MessageKind.TEXT && msg.kind != MessageKind.GROUP_TEXT) return null
        if (msg.text.any { it == '\u0000' }) return null
        val t = msg.text.trim()
        if (t.isEmpty()) return null
        if (t.length <= BODY_MAX) return t
        return t.take(BODY_MAX - 1).trimEnd() + "…"
    }
}
