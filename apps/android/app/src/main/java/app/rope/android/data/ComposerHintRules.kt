package app.rope.android.data

enum class ComposerHintKind { REPLY, EDIT }

data class ComposerHintCopy(
    val kind: ComposerHintKind,
    val title: String,
    val body: String,
    val dismissContentDescription: String,
)

/**
 * Telegram-like composer reply/edit chrome: accent title is the peer name (not
 * «Ответ · name»), one-line preview, X dismiss (not «Отмена»).
 */
object ComposerHintRules {
    const val BODY_MAX = 80
    const val FALLBACK_BODY = "Сообщение"
    const val REPLY_FALLBACK_TITLE = "Ответ"
    const val EDIT_TITLE = "Редактирование"
    const val DISMISS_REPLY = "Отменить ответ"
    const val DISMISS_EDIT = "Отменить редактирование"

    fun reply(name: String, preview: String, spanText: String = ""): ComposerHintCopy = ComposerHintCopy(
        kind = ComposerHintKind.REPLY,
        title = name.trim().ifBlank { REPLY_FALLBACK_TITLE },
        body = clipBody(spanText.ifBlank { preview }),
        dismissContentDescription = DISMISS_REPLY,
    )

    fun edit(preview: String): ComposerHintCopy = ComposerHintCopy(
        kind = ComposerHintKind.EDIT,
        title = EDIT_TITLE,
        body = clipBody(preview),
        dismissContentDescription = DISMISS_EDIT,
    )

    fun clipBody(preview: String): String {
        val one = preview.replace(WHITESPACE, " ").trim()
        if (one.isEmpty()) return FALLBACK_BODY
        if (one.length <= BODY_MAX) return one
        return one.take(BODY_MAX - 1).trimEnd() + "…"
    }

    private val WHITESPACE = Regex("\\s+")
}
