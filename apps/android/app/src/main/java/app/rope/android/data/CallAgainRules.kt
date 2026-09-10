package app.rope.android.data

/**
 * Telegram-like redial from a CALL bubble. Header call buttons stay as-is.
 * Groups and Избранное have no 1:1 redial.
 */
object CallAgainRules {
    const val A11Y_AUDIO = "Позвонить"
    const val A11Y_VIDEO = "Видеозвонок"

    fun canRedial(msg: ChatMessage, isGroup: Boolean, saved: Boolean): Boolean =
        msg.kind == MessageKind.CALL &&
            !msg.deleted &&
            !isGroup &&
            !saved

    fun usesVideo(text: String): Boolean =
        text.contains("видео", ignoreCase = true)

    fun a11y(text: String): String =
        if (usesVideo(text)) A11Y_VIDEO else A11Y_AUDIO
}
