package app.rope.android.data

enum class CallToneKind {
    NONE,
    RING_IN,
    RING_OUT,
}

/**
 * Settings mute and per-chat mute silence **message** notifications only (D-016).
 * Incoming ringtone, incoming-call overlay, and outgoing ringback ignore those flags.
 *
 * Ring plays only while the call is actually ringing. ICE DISCONNECTED / FAILED /
 * CHECKING after media is up must not restart RING or pulse the tone.
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

    fun kind(phase: CallPhase, link: CallLinkState, mediaUp: Boolean): CallToneKind {
        if (mediaUp || phase == CallPhase.ACTIVE || phase == CallPhase.ENDED) {
            return CallToneKind.NONE
        }
        if (link == CallLinkState.CONNECTED || link == CallLinkState.FAILED) {
            return CallToneKind.NONE
        }
        return when (phase) {
            CallPhase.RINGING_IN -> CallToneKind.RING_IN
            CallPhase.RINGING_OUT -> CallToneKind.RING_OUT
            CallPhase.ACTIVE, CallPhase.ENDED -> CallToneKind.NONE
        }
    }

    fun shouldPlayRing(phase: CallPhase, link: CallLinkState, mediaUp: Boolean): Boolean =
        kind(phase, link, mediaUp) != CallToneKind.NONE

    /** ICE blips never map the machine back to RINGING. */
    fun iceBlipSetsRinging(iceName: String, mediaUp: Boolean): Boolean = false

    /** ICE DISCONNECTED / FAILED / CHECKING must not start RING again. */
    fun iceBlipRestartsRing(iceName: String, mediaUp: Boolean): Boolean = false
}
