package app.rope.android.data

enum class CallRtcRole { NONE, OFFERER, ANSWERER }

data class CallMachineState(
    val callId: String = "",
    val altCallId: String = "",
    val peerDeviceId: String = "",
    val outgoing: Boolean = false,
    val phase: CallPhase = CallPhase.ENDED,
    val link: CallLinkState = CallLinkState.RINGING,
    val media: String = "",
    val hasTurn: Boolean = false,
    val role: CallRtcRole = CallRtcRole.NONE,
    val sessionReady: Boolean = false,
    val localOfferReady: Boolean = false,
    val remoteDescriptionReady: Boolean = false,
    val queue: List<CallSignal> = emptyList(),
    val ended: Boolean = true,
    val iceRestartUsed: Boolean = false,
    val rtcWanted: Boolean = false,
) {
    val live: Boolean get() = !ended && callId.isNotBlank()
}

sealed class CallEffect {
    data class Send(
        val callId: String,
        val peerId: String,
        val event: String,
        val payload: String = "",
    ) : CallEffect()

    data class StartRtc(val asCaller: Boolean) : CallEffect()
    data class DeliverRemote(val signals: List<CallSignal>) : CallEffect()
    object RestartIce : CallEffect()
    object TearDown : CallEffect()
    object RingOut : CallEffect()
    object RingIn : CallEffect()
    object StopTone : CallEffect()
    object ClearNotify : CallEffect()
    data class Record(val peerId: String, val outgoing: Boolean) : CallEffect()
    object NotifyIncoming : CallEffect()
    object PrefetchIce : CallEffect()
    object WatchConnect : CallEffect()
    object WatchRing : CallEffect()
    object CancelWatch : CallEffect()
    data class Notice(val message: String) : CallEffect()
}

/**
 * Deterministic 1:1 call signaling:
 * ring → accept → offer/answer → trickle ICE → connected | failed | hangup
 *
 * Early answer/ICE are queued until the local offer / remote description is ready.
 * Glare (both sides ring) picks one offerer and a shared call id.
 */
class CallMachine {
    private val lock = Any()

    var state = CallMachineState()
        private set

    fun snapshot(peerName: String): CallInfo? {
        val s = state
        if (!s.live) return null
        return CallInfo(
            callId = s.callId,
            peerDeviceId = s.peerDeviceId,
            peerName = peerName,
            outgoing = s.outgoing,
            phase = s.phase,
            media = s.media,
            link = s.link,
            hasTurn = s.hasTurn,
        )
    }

    fun reset() = synchronized(lock) { state = CallMachineState() }

    fun localStart(callId: String, peerId: String, myId: String): List<CallEffect> = synchronized(lock) {
        if (callId.isBlank() || peerId.isBlank()) return emptyList()
        if (state.live && state.peerDeviceId == peerId && state.phase == CallPhase.RINGING_IN) {
            return localAcceptLocked()
        }
        if (state.live) return emptyList()
        state = CallMachineState(
            callId = callId,
            peerDeviceId = peerId,
            outgoing = true,
            phase = CallPhase.RINGING_OUT,
            link = CallLinkState.RINGING,
            media = "WebRTC · соединяем",
            ended = false,
        )
        return listOf(
            CallEffect.Send(callId, peerId, CallSignal.RING),
            CallEffect.Record(peerId, true),
            CallEffect.PrefetchIce,
            CallEffect.RingOut,
            CallEffect.WatchRing,
        )
    }

    fun localAccept(): List<CallEffect> = synchronized(lock) { localAcceptLocked() }

    fun localReject(): List<CallEffect> = synchronized(lock) { endLocalLocked(CallSignal.REJECT) }

    fun localHangup(): List<CallEffect> = synchronized(lock) { endLocalLocked(CallSignal.HANGUP) }

    fun onWire(from: String, event: String, callId: String, payload: Any?, myId: String): List<CallEffect> =
        synchronized(lock) {
            val ev = CallSignal.parseEvent(event) ?: return emptyList()
            if (from.isBlank() || callId.isBlank()) return emptyList()
            when (ev) {
                CallSignal.RING -> onRingLocked(from, callId, myId)
                CallSignal.ACCEPT -> onAcceptLocked(from, callId)
                CallSignal.REJECT, CallSignal.HANGUP -> onRemoteEndLocked(from, callId)
                in CallSignal.EVENTS -> onMediaLocked(from, callId, ev, payload)
                else -> emptyList()
            }
        }

    fun onSessionAttached(): List<CallEffect> = synchronized(lock) {
        if (!state.live) return emptyList()
        state = state.copy(sessionReady = true)
        drainLocked()
    }

    fun onLocalOfferSent(): List<CallEffect> = synchronized(lock) {
        if (!state.live) return emptyList()
        state = state.copy(localOfferReady = true)
        drainLocked()
    }

    fun onHasTurn(hasTurn: Boolean, mediaIfMissing: String): List<CallEffect> = synchronized(lock) {
        if (!state.live) return emptyList()
        val media = if (hasTurn) state.media else mediaIfMissing
        state = state.copy(hasTurn = hasTurn, media = media)
        emptyList()
    }

    fun onIce(name: String, viaRelay: Boolean): List<CallEffect> = synchronized(lock) {
        if (!state.live) return emptyList()
        if (state.phase == CallPhase.RINGING_IN || state.phase == CallPhase.RINGING_OUT) return emptyList()
        val (link, label) = CallLink.applyIce(name, viaRelay, state.hasTurn)
        state = state.copy(link = link, media = label)
        val out = mutableListOf<CallEffect>()
        if (link == CallLinkState.CONNECTED) out += CallEffect.CancelWatch
        if (link == CallLinkState.FAILED && state.hasTurn && !state.iceRestartUsed && state.role == CallRtcRole.OFFERER) {
            state = state.copy(
                iceRestartUsed = true,
                link = CallLinkState.CONNECTING,
                media = CallLink.connectingDetail(true),
            )
            out += CallEffect.RestartIce
            out += CallEffect.WatchConnect
        }
        out
    }

    fun onConnectTimeout(): List<CallEffect> = synchronized(lock) {
        if (!state.live || state.phase != CallPhase.ACTIVE) return emptyList()
        if (state.link == CallLinkState.CONNECTED || state.link == CallLinkState.FAILED) return emptyList()
        if (state.hasTurn && !state.iceRestartUsed && state.role == CallRtcRole.OFFERER) {
            state = state.copy(iceRestartUsed = true)
            return listOf(CallEffect.RestartIce, CallEffect.WatchConnect)
        }
        val detail = CallLink.timeoutDetail(state.hasTurn)
        state = state.copy(link = CallLinkState.FAILED, media = detail)
        return listOf(CallEffect.CancelWatch, CallEffect.Notice(detail))
    }

    fun onRingTimeout(): List<CallEffect> = synchronized(lock) {
        if (!state.live) return emptyList()
        if (state.phase != CallPhase.RINGING_IN && state.phase != CallPhase.RINGING_OUT) return emptyList()
        val outgoing = state.phase == CallPhase.RINGING_OUT
        val detail = CallLink.ringTimeoutDetail(outgoing)
        val event = if (outgoing) CallSignal.HANGUP else CallSignal.REJECT
        val send = CallEffect.Send(state.callId, state.peerDeviceId, event)
        hardEndLocked()
        return listOf(send, CallEffect.Notice(detail), CallEffect.TearDown)
    }

    fun onRingSendFailed(): List<CallEffect> = synchronized(lock) {
        if (!state.live) return emptyList()
        hardEndLocked()
        return listOf(CallEffect.Notice(CallLink.offlineDetail()), CallEffect.TearDown)
    }

    private fun localAcceptLocked(): List<CallEffect> {
        if (!state.live || state.phase != CallPhase.RINGING_IN) return emptyList()
        enterNegotiating(asCaller = false)
        return listOf(
            CallEffect.Send(state.callId, state.peerDeviceId, CallSignal.ACCEPT),
            CallEffect.StopTone,
            CallEffect.ClearNotify,
            CallEffect.StartRtc(false),
            CallEffect.WatchConnect,
        ) + drainLocked()
    }

    private fun endLocalLocked(event: String): List<CallEffect> {
        if (!state.live) return emptyList()
        val send = CallEffect.Send(state.callId, state.peerDeviceId, event)
        hardEndLocked()
        return listOf(send, CallEffect.TearDown)
    }

    private fun onRingLocked(from: String, callId: String, myId: String): List<CallEffect> {
        if (state.live && state.peerDeviceId == from && state.outgoing && state.phase == CallPhase.RINGING_OUT) {
            return resolveGlareLocked(from, callId, myId)
        }
        if (state.live && matchesLocked(from, callId)) return emptyList()
        if (state.live) return emptyList()
        state = CallMachineState(
            callId = callId,
            peerDeviceId = from,
            outgoing = false,
            phase = CallPhase.RINGING_IN,
            link = CallLinkState.RINGING,
            media = "WebRTC · соединяем",
            ended = false,
        )
        return incomingRingEffects()
    }

    private fun resolveGlareLocked(from: String, theirCallId: String, myId: String): List<CallEffect> {
        val mine = state.callId
        val canon = CallLink.canonicalCallId(mine, theirCallId)
        val alt = if (canon == mine) theirCallId else mine
        val weOffer = CallLink.weCreateOffer(myId, from)
        enterNegotiating(asCaller = weOffer)
        state = state.copy(callId = canon, altCallId = alt)
        return listOf(
            CallEffect.StopTone,
            CallEffect.StartRtc(weOffer),
            CallEffect.WatchConnect,
        ) + drainLocked()
    }

    private fun onAcceptLocked(from: String, callId: String): List<CallEffect> {
        if (!state.live || !matchesLocked(from, callId)) return emptyList()
        if (state.rtcWanted) return emptyList()
        if (state.phase != CallPhase.RINGING_OUT) return emptyList()
        enterNegotiating(asCaller = true)
        return listOf(
            CallEffect.StopTone,
            CallEffect.StartRtc(true),
            CallEffect.WatchConnect,
        ) + drainLocked()
    }

    private fun onRemoteEndLocked(from: String, callId: String): List<CallEffect> {
        if (!state.live) return emptyList()
        if (state.peerDeviceId != from && !matchesLocked(from, callId)) return emptyList()
        hardEndLocked()
        return listOf(CallEffect.TearDown)
    }

    private fun onMediaLocked(from: String, callId: String, event: String, payload: Any?): List<CallEffect> {
        val sig = CallSignal.parseMedia(event, payload)
        if (!state.live) {
            if (event != CallSignal.OFFER) return emptyList()
            state = CallMachineState(
                callId = callId,
                peerDeviceId = from,
                outgoing = false,
                phase = CallPhase.RINGING_IN,
                link = CallLinkState.RINGING,
                media = "WebRTC · соединяем",
                ended = false,
                queue = listOfNotNull(sig),
            )
            return incomingRingEffects()
        }
        if (!matchesLocked(from, callId)) return emptyList()
        if (sig == null) return emptyList()
        return enqueueLocked(sig)
    }

    private fun incomingRingEffects(): List<CallEffect> = listOf(
        CallEffect.Record(state.peerDeviceId, false),
        CallEffect.NotifyIncoming,
        CallEffect.PrefetchIce,
        CallEffect.RingIn,
        CallEffect.WatchRing,
    )

    private fun enqueueLocked(sig: CallSignal): List<CallEffect> {
        if (CallLink.shouldQueueSignal(
                sig.kind,
                state.sessionReady,
                state.localOfferReady,
                state.remoteDescriptionReady,
            )
        ) {
            state = state.copy(queue = state.queue + sig)
            return emptyList()
        }
        return drainLocked(sig)
    }

    private fun drainLocked(extra: CallSignal? = null): List<CallEffect> {
        val all = state.queue + listOfNotNull(extra)
        if (all.isEmpty()) return emptyList()
        val deliver = mutableListOf<CallSignal>()
        val keep = mutableListOf<CallSignal>()
        var remote = state.remoteDescriptionReady
        for (s in all) {
            if (CallLink.shouldQueueSignal(s.kind, state.sessionReady, state.localOfferReady, remote)) {
                keep += s
            } else {
                deliver += s
                if (s.kind == CallSignal.OFFER || s.kind == CallSignal.ANSWER) remote = true
            }
        }
        val keep2 = mutableListOf<CallSignal>()
        for (s in keep) {
            if (CallLink.shouldQueueSignal(s.kind, state.sessionReady, state.localOfferReady, remote)) {
                keep2 += s
            } else {
                deliver += s
            }
        }
        state = state.copy(queue = keep2, remoteDescriptionReady = remote)
        return if (deliver.isEmpty()) emptyList() else listOf(CallEffect.DeliverRemote(deliver))
    }

    private fun enterNegotiating(asCaller: Boolean) {
        state = state.copy(
            phase = CallPhase.ACTIVE,
            link = CallLinkState.CONNECTING,
            media = CallMedia.label("CHECKING"),
            role = if (asCaller) CallRtcRole.OFFERER else CallRtcRole.ANSWERER,
            rtcWanted = true,
        )
    }

    private fun hardEndLocked() {
        state = CallMachineState()
    }

    private fun matchesLocked(from: String, callId: String): Boolean =
        CallLink.matchesCall(callId, from, state.callId, state.altCallId, state.peerDeviceId)
}
