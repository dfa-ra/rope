package app.rope.android.data

/**
 * Telegram-like restrict forwarding/copy in a chat on this device.
 * ChatPrefs `protect`. Not view-once, not envelope crypto.
 */
object ProtectContentRules {
    const val TITLE = "Запретить пересылку"
    const val HINT = "С этого телефона нельзя переслать и копировать. Не шифрование."

    fun applies(chatId: String?): Boolean =
        !chatId.isNullOrBlank() && !SavedMessagesRules.isSaved(chatId)

    fun canForward(msg: ChatMessage, protect: Boolean): Boolean =
        !msg.deleted && !protect

    fun canCopy(msg: ChatMessage, protect: Boolean): Boolean =
        !msg.deleted && msg.text.isNotBlank() && !protect

    fun blocked(protect: Boolean): Boolean = protect
}
