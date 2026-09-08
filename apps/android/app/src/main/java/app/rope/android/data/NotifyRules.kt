package app.rope.android.data

/**
 * Whether an incoming message should post a heads-up notification.
 *
 * A chat that is open while the process is not STARTED still alerts,
 * and still increments unread in the repository.
 */
object NotifyRules {
    fun shouldAlert(chatOpen: Boolean, appForeground: Boolean, muted: Boolean): Boolean {
        if (muted) return false
        return !(chatOpen && appForeground)
    }
}
