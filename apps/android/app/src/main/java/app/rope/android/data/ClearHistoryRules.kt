package app.rope.android.data

/**
 * Telegram-like local clear history. Wipes that peer's messages in
 * LocalStore and resets thread prefs (draft / unread / pin) — chat row,
 * mute, and archive stay. No Go, no DeviceBackup, no schema bump.
 */
object ClearHistoryRules {
    const val ACTION = "Очистить историю"
    const val CONFIRM = "Точно очистить"
    const val CANCEL = "Отмена"
    const val BODY = "История сотрётся только на этом телефоне. Чат останется."

    fun canClear(id: String?): Boolean = !id.isNullOrBlank()

    fun afterClear(cur: ChatPrefs): ChatPrefs = cur.copy(
        unread = 0,
        lastReadMs = 0,
        draft = "",
        pinnedMessageId = null,
    )
}
