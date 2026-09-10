package app.rope.android.data

/**
 * Per-chat link-preview switch. Default on. Off skips unfurl in that
 * chat. Hidden on Saved. Distinct from Settings `link_previews`.
 * LocalStore stays v6. Not envelope crypto.
 */
object LpOffRules {
    const val TITLE = "Предпросмотр ссылок"
    const val HINT = "В этом чате. Выкл — без карточки при отправке. Не шифрование."

    fun applies(chatId: String?): Boolean =
        !chatId.isNullOrBlank() && !SavedMessagesRules.isSaved(chatId)

    fun effective(chatId: String?, prefsOn: Boolean): Boolean =
        if (!applies(chatId)) true else prefsOn

    fun enabled(global: Boolean, chatOn: Boolean): Boolean = global && chatOn

    fun parsePref(hasKey: Boolean, value: Boolean): Boolean = if (!hasKey) true else value
}
