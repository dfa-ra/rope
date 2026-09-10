package app.rope.android.data

/**
 * Whether an incoming message should post a heads-up notification.
 *
 * A chat that is open while the process is not STARTED still alerts,
 * and still increments unread in the repository.
 * Global mute (Settings) silences messages only — not the incoming-call overlay.
 * A muted chat still alerts on @mentions of you ([GroupChatUx.mentionSpans]).
 */
object NotifyRules {
    const val MUTED_A11Y = "Только упоминания"

    fun shouldAlert(
        chatOpen: Boolean,
        appForeground: Boolean,
        muted: Boolean,
        globalMuted: Boolean = false,
        mentioned: Boolean = false,
    ): Boolean {
        if (globalMuted) return false
        if (muted && !mentioned) return false
        return !(chatOpen && appForeground)
    }

    fun selfMentionNames(displayName: String?): List<String> {
        val n = displayName?.trim().orEmpty()
        return if (n.isEmpty()) listOf(GroupChatUx.YOU) else listOf(n, GroupChatUx.YOU).distinct()
    }

    fun mentionsMe(text: String, names: List<String>): Boolean =
        GroupChatUx.mentionSpans(text, names).isNotEmpty()

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
