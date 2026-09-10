package app.rope.android.data

/**
 * Settings vibration for incoming message heads-up. Not call tones,
 * not envelope crypto, not FCM.
 */
object InAppVibRules {
    const val WAVEFORM_NO_REPEAT = -1

    fun enabledFromKv(raw: String?): Boolean = raw != "0"

    fun shouldVibrate(alert: Boolean, vibrateEnabled: Boolean): Boolean =
        alert && vibrateEnabled

    fun pattern(enabled: Boolean): LongArray =
        if (enabled) longArrayOf(0, 40, 80, 40) else longArrayOf(0)

    fun hint(): String =
        "Короткий вибросигнал у входящего баннера, когда чат не на экране. Не звонок."
}
