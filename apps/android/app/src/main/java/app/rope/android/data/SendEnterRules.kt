package app.rope.android.data

/**
 * Telegram-like send-by-Enter. Kv-only. Default off so mobile Enter
 * stays a newline. LocalStore schema stays v6.
 */
object SendEnterRules {
    const val SECTION = "Чат"
    const val TITLE = "Отправка по Enter"

    fun hint(): String =
        "Enter отправляет. Новая строка — когда выключено. Цвета логотипа не меняются."

    fun imeIsSend(enabled: Boolean): Boolean = enabled

    fun imeSends(
        enabled: Boolean,
        draft: String,
        recording: Boolean,
        recordingLocked: Boolean = false,
        pendingMedia: Boolean = false,
    ): Boolean =
        enabled &&
            !recording &&
            ComposerRules.showSendButton(draft, recording, recordingLocked, pendingMedia)
}
