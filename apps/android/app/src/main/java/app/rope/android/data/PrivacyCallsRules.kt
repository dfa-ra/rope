package app.rope.android.data

/**
 * Settings local auto-reject for incoming calls on this device.
 * Not call-min, not FCM, not mute. LocalStore kv, no bump.
 */
object PrivacyCallsRules {
    const val KEY = "reject_incoming_calls"
    const val TITLE = "Звонки"
    const val LABEL = "Не принимать входящие"
    const val HINT = "На этом телефоне входящие сразу отклоняются. Не сворачивание звонка."

    fun parse(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        if ('\n' in raw || '\r' in raw || '\u0000' in raw) return false
        return raw.trim() == "1"
    }

    fun persist(enabled: Boolean): String = if (enabled) "1" else "0"

    fun shouldAutoReject(enabled: Boolean): Boolean = enabled
}
