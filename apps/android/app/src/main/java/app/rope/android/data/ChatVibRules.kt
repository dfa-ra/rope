package app.rope.android.data

/**
 * Per-chat vibration for message notifications. Default on. Stored in
 * `chat_prefs.vibrate` (missing key = on). LocalStore stays v6. Not FCM.
 */
object ChatVibRules {
    const val CHANNEL_VIB = "rope-messages"
    const val CHANNEL_NOVIB = "rope-messages-novib"
    const val MENU_ON = "Без вибрации"
    const val MENU_OFF = "Вибрация"

    fun parse(hasKey: Boolean, value: Boolean): Boolean = if (hasKey) value else true

    fun shouldVibrate(alert: Boolean, chatVibrate: Boolean): Boolean = alert && chatVibrate

    fun channelId(vibrate: Boolean): String = if (vibrate) CHANNEL_VIB else CHANNEL_NOVIB

    fun pattern(vibrate: Boolean): LongArray =
        if (vibrate) longArrayOf(0, 40, 80, 40) else longArrayOf(0)

    fun menuLabel(vibrate: Boolean): String = if (vibrate) MENU_ON else MENU_OFF

    fun canToggle(chatId: String): Boolean = !SavedMessagesRules.isSaved(chatId)

    fun hint(): String =
        "Вибрация входящего баннера в этом чате. Звук чата отдельно. Не звонок."
}
