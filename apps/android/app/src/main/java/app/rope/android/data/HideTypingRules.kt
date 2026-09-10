package app.rope.android.data

/**
 * Settings «Скрывать набор»: skip outgoing typing so the peer does not see the indicator.
 * Default off. Not FCM, not envelope crypto.
 */
object HideTypingRules {
    fun enabledFromKv(raw: String?): Boolean = raw == "1"

    fun persist(enabled: Boolean): String = if (enabled) "1" else "0"

    fun hint(): String =
        "Собеседник не видит, что вы набираете. Индикатор у него не появляется."
}
