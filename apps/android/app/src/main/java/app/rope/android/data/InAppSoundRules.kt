package app.rope.android.data

/**
 * Settings in-app sound for incoming message heads-up while Rope is open.
 * Background shade sound stays on the notification channel. Not call tones,
 * not envelope crypto, not FCM.
 */
object InAppSoundRules {
    /** ToneGenerator.TONE_PROP_BEEP */
    const val TONE = 27
    const val DURATION_MS = 120
    const val VOLUME = 80

    fun enabledFromKv(raw: String?): Boolean = raw != "0"

    fun shouldPlay(alert: Boolean, soundEnabled: Boolean, appForeground: Boolean): Boolean =
        alert && soundEnabled && appForeground

    /** Foreground heads-up uses our beep; mute the channel so it does not double-play. */
    fun suppressChannelSound(appForeground: Boolean): Boolean = appForeground

    fun hint(): String =
        "Короткий звук у входящего баннера, пока приложение открыто. Не звонок."
}
