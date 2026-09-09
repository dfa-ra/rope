package app.rope.android.data

/**
 * Whether an incoming message should post a heads-up notification.
 *
 * A chat that is open while the process is not STARTED still alerts,
 * and still increments unread in the repository.
 * Global mute (Settings) silences messages only — not the incoming-call overlay.
 */
object NotifyRules {
    fun shouldAlert(
        chatOpen: Boolean,
        appForeground: Boolean,
        muted: Boolean,
        globalMuted: Boolean = false,
        silent: Boolean = false,
    ): Boolean {
        if (silent) return false
        if (globalMuted || muted) return false
        return !(chatOpen && appForeground)
    }
}
