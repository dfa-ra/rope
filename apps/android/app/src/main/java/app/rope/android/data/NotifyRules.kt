package app.rope.android.data

/**
 * Whether an incoming message should post a heads-up notification.
 *
 * A chat that is open while the process is not STARTED still alerts,
 * and still increments unread in the repository.
 * Global mute (Settings) silences messages only — not the incoming-call overlay.
 */
object NotifyRules {
    /**
     * Shade body for an incoming message. Off hides the ciphertext-derived
     * text; the sender title is unchanged. Album collapse still keys off the
     * raw body in the repository.
     */
    fun messageBody(raw: String, preview: Boolean): String {
        if (!preview) return "Новое сообщение"
        return raw.trim().ifEmpty { "Сообщение" }
    }

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
