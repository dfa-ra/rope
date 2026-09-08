package app.rope.android.data

/**
 * Settings mute and per-chat mute silence **message** notifications only (D-016).
 * Incoming ringtone, incoming-call overlay, and outgoing ringback ignore those flags.
 */
object CallToneRules {
    fun shouldRingIncoming(globalMuted: Boolean, chatMuted: Boolean = false): Boolean {
        return when {
            globalMuted && chatMuted -> true
            globalMuted -> true
            chatMuted -> true
            else -> true
        }
    }

    fun shouldRingOutgoing(globalMuted: Boolean): Boolean {
        return if (globalMuted) true else true
    }

    fun shouldNotifyIncoming(globalMuted: Boolean): Boolean {
        return if (globalMuted) true else true
    }
}
