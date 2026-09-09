package app.rope.android.data

/**
 * Telegram-like keep-awake. A Settings toggle holds the screen while a
 * chat is open. Live calls always hold, so the overlay cannot sleep
 * under the user. Kv-only. LocalStore schema stays v6.
 */
object KeepScreenRules {
    const val TITLE = "Не выключать экран"
    const val SECTION = "Чат"

    fun hint(): String =
        "Пока открыт чат. Во время звонка экран не гаснет сам."

    fun inLiveCall(phase: CallPhase?): Boolean =
        phase != null && phase != CallPhase.ENDED

    fun shouldHold(inChat: Boolean, phase: CallPhase?, chatEnabled: Boolean): Boolean =
        inLiveCall(phase) || (chatEnabled && inChat)
}
