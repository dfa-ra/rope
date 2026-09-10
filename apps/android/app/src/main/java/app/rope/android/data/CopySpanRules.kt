package app.rope.android.data

/**
 * Copy a selected UTF-16 span from a TEXT / GROUP_TEXT bubble.
 * Not the reply quote picker — [QuoteSpanRules.packed] omits a full-body
 * highlight; copy keeps it. LocalStore stays v6.
 */
object CopySpanRules {
    const val NOTICE = "Скопировано"
    const val HINT = "Выделите фрагмент"

    fun canCopy(msg: ChatMessage): Boolean =
        !msg.deleted &&
            (msg.kind == MessageKind.TEXT || msg.kind == MessageKind.GROUP_TEXT) &&
            msg.text.isNotBlank()

    fun clamp(source: String, start: Int, end: Int): QuoteSpan? =
        QuoteSpanRules.clamp(source, start, end)

    fun excerpt(source: String, span: QuoteSpan): String =
        QuoteSpanRules.excerpt(source, span)

    fun clip(msg: ChatMessage, start: Int, end: Int): String? {
        if (!canCopy(msg)) return null
        val span = clamp(msg.text, start, end) ?: return null
        return excerpt(msg.text, span).takeIf { it.isNotBlank() }
    }

    fun clipOrAll(msg: ChatMessage, span: QuoteSpan?): String? {
        if (!canCopy(msg)) return null
        if (span == null) return msg.text.takeIf { it.isNotBlank() }
        return clip(msg, span.start, span.end)
    }
}
