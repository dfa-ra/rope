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
    ): Boolean {
        if (globalMuted || muted) return false
        return !(chatOpen && appForeground)
    }

    data class IncomingCallText(
        val title: String,
        val privateBody: String,
        val publicBody: String,
    )

    /**
     * Shade may show the peer. Lockscreen public version is title-only.
     */
    fun incomingCallText(peerName: String): IncomingCallText =
        IncomingCallText(
            title = "Входящий вызов",
            privateBody = peerName,
            publicBody = "",
        )
}
