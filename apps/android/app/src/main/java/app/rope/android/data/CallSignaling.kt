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
    val iceReady: Boolean = false,
    val lastIce: String = "",
    val startedAtMs: Long = 0L,
    val connectStartedAtMs: Long = 0L,
    val viaRelay: Boolean = false,
    val relayFellBack: Boolean = false,
    val wssMedia: Boolean = false,
    val video: Boolean = false,
    /** True once ICE/DTLS reached CONNECTED; blips must not hang up. */
    val mediaUp: Boolean = false,
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

    data class StartRtc(val asCaller: Boolean, val video: Boolean = false) : CallEffect()
    data class DeliverRemote(val signals: List<CallSignal>) : CallEffect()
    object StartWssMedia : CallEffect()
    data class DeliverAudio(val signals: List<CallSignal>) : CallEffect()
    object RestartIce : CallEffect()
    object FallbackDirect : CallEffect()
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
    private var lastIncomingRingFrom = ""
    private var lastIncomingRingAtMs = 0L

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
            iceReady = s.iceReady,
            lastIce = s.lastIce,
            startedAtMs = s.startedAtMs,
            video = s.video,
        )
    }

    fun reset() = synchronized(lock) { state = CallMachineState() }

    fun localStart(
        callId: String,
        peerId: String,
        myId: String,
        video: Boolean = false,
    ): List<CallEffect> = synchronized(lock) {
        val id = callId.trim()
        val peer = PeerIds.normalize(peerId)
        if (id.isBlank() || peer.isBlank() || ChatIds.isGroup(peerId)) return emptyList()
        if (state.live && PeerIds.same(state.peerDeviceId, peer) && state.phase == CallPhase.RINGING_IN) {
            return localAcceptLocked()
        }
        if (state.live) return emptyList()
        state = CallMachineState(
            callId = id,
            peerDeviceId = peer,
            outgoing = true,
            phase = CallPhase.RINGING_OUT,
            link = CallLinkState.RINGING,
            media = if (video) "видеовызов" else "ожидаем ответа",
            ended = false,
            startedAtMs = System.currentTimeMillis(),
            video = video,
        )
        return listOf(
            CallEffect.Send(id, peer, CallSignal.RING, VideoCallRules.ringPayload(video)),
            CallEffect.Record(peer, true),
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
            val fromId = PeerIds.normalize(from)
            val id = callId.trim()
            if (fromId.isBlank() || id.isBlank()) return emptyList()
            val mine = PeerIds.normalize(myId)
            when (ev) {
                CallSignal.RING -> onRingLocked(fromId, id, mine, payload)
                CallSignal.ACCEPT -> onAcceptLocked(fromId, id)
                CallSignal.REJECT, CallSignal.HANGUP -> onRemoteEndLocked(fromId, id)
                in CallSignal.EVENTS -> onMediaLocked(fromId, id, ev, payload)
                CallSignal.RELAY, CallSignal.AUDIO -> onWssLocked(fromId, id, ev, payload)
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
        val media = if (hasTurn) {
            if (state.media.contains("нет TURN") || state.media.contains("ice_servers")) {
                if (state.phase == CallPhase.ACTIVE) {
                    CallLink.connectingDetail(true, state.remoteDescriptionReady, state.relayFellBack)
                } else {
                    "ожидаем ответа"
                }
            } else {
                state.media
            }
        } else {
            mediaIfMissing
        }
        state = state.copy(hasTurn = hasTurn, iceReady = true, media = media)
        emptyList()
    }

    fun onIceServers(iceServersJson: String?): List<CallEffect> = synchronized(lock) {
        if (!state.live) return emptyList()
        val updated = CallLink.applyInfo(
            iceServersJson,
            CallInfo(
                callId = state.callId,
                peerDeviceId = state.peerDeviceId,
                peerName = "",
                outgoing = state.outgoing,
                phase = state.phase,
                media = state.media,
                link = state.link,
                hasTurn = state.hasTurn,
                iceReady = state.iceReady,
                lastIce = state.lastIce,
                startedAtMs = state.startedAtMs,
            ),
        )
        state = state.copy(hasTurn = updated.hasTurn, iceReady = updated.iceReady, media = updated.media)
        emptyList()
    }

    fun onIce(name: String, viaRelay: Boolean): List<CallEffect> = synchronized(lock) {
        if (!state.live) return emptyList()
        if (state.wssMedia) return emptyList()
        if (state.phase == CallPhase.RINGING_IN || state.phase == CallPhase.RINGING_OUT) return emptyList()
        if (VideoCallRules.ignorePcClosed(name)) return emptyList()
        val relay = state.viaRelay || viaRelay
        if (VideoCallRules.iceBlipKeepsCall(name, state.mediaUp) && state.mediaUp) {
            val out = mutableListOf<CallEffect>()
            out += CallEffect.StopTone
            if (name.trim().equals("FAILED", ignoreCase = true) &&
                state.hasTurn &&
                !state.iceRestartUsed &&
                state.role == CallRtcRole.OFFERER
            ) {
                state = state.copy(
                    iceRestartUsed = true,
                    lastIce = name,
                    viaRelay = relay,
                    link = CallLinkState.CONNECTED,
                    media = CallLink.iceRestartDetail(),
                )
                out += CallEffect.RestartIce
            } else {
                // Keep CONNECTED. Do not set RINGING. Do not flash DISCONNECTED copy.
                state = state.copy(
                    lastIce = name,
                    viaRelay = relay,
                    link = CallLinkState.CONNECTED,
                )
            }
            return out
        }
        val (link, label) = CallLink.applyIce(
            name,
            viaRelay = relay,
            hasTurn = state.hasTurn,
            remoteReady = state.remoteDescriptionReady,
            fellBack = state.relayFellBack,
        )
        val nextLink = if (state.mediaUp && link != CallLinkState.FAILED) {
            CallLinkState.CONNECTED
        } else {
            link
        }
        state = state.copy(
            link = nextLink,
            media = if (state.mediaUp && nextLink == CallLinkState.CONNECTED) state.media else label,
            lastIce = name,
            iceReady = true,
            viaRelay = relay,
            mediaUp = state.mediaUp || nextLink == CallLinkState.CONNECTED,
        )
        val out = mutableListOf<CallEffect>()
        if (nextLink == CallLinkState.CONNECTED) {
            out += CallEffect.CancelWatch
            out += CallEffect.StopTone
        }
        if (link == CallLinkState.FAILED) {
            if (state.hasTurn && !state.iceRestartUsed && state.role == CallRtcRole.OFFERER) {
                state = state.copy(
                    iceRestartUsed = true,
                    link = CallLinkState.CONNECTING,
                    media = CallLink.iceRestartDetail(),
                )
                out += CallEffect.RestartIce
            } else if (!state.mediaUp) {
                return startWssFallbackLocked(sendRelay = true)
            }
        }
        out
    }

    fun onLocalCandidate(relay: Boolean): List<CallEffect> = synchronized(lock) {
        if (!state.live || !relay) return emptyList()
        state = state.copy(viaRelay = true)
        emptyList()
    }

    fun onConnectTick(elapsedMs: Long): List<CallEffect> = synchronized(lock) {
        if (!state.live || state.phase != CallPhase.ACTIVE) return emptyList()
        if (state.wssMedia) return emptyList()
        if (state.link == CallLinkState.CONNECTED || state.link == CallLinkState.FAILED) return emptyList()
        val decision = IceUnstick.decide(
            IceUnstick.Snapshot(
                elapsedMs = elapsedMs,
                ice = state.lastIce,
                hasRelayCandidate = state.viaRelay,
                preferRelay = state.hasTurn && !state.relayFellBack,
                alreadyFellBack = state.relayFellBack,
                iceRestartUsed = state.iceRestartUsed,
                remoteDescriptionReady = state.remoteDescriptionReady,
                connected = false,
                failed = false,
                isOfferer = state.role == CallRtcRole.OFFERER,
            ),
        )
        if (decision.failSignal || decision.failIce) {
            // WSS audio does not need SDP/ICE. If the call was already accepted, fall through.
            return startWssFallbackLocked(sendRelay = true)
        }
        val out = mutableListOf<CallEffect>()
        if (decision.fallbackDirect) {
            state = state.copy(
                relayFellBack = true,
                media = CallLink.fallbackDirectDetail(),
            )
            out += CallEffect.FallbackDirect
        }
        if (decision.restartIce) {
            state = state.copy(
                iceRestartUsed = true,
                media = if (state.relayFellBack) CallLink.fallbackDirectDetail() else CallLink.iceRestartDetail(),
            )
            out += CallEffect.RestartIce
        }
        out
    }

    fun onConnectTimeout(): List<CallEffect> = synchronized(lock) {
        onConnectTick(IceUnstick.CONNECT_FAIL_MS)
    }

    fun onRingTimeout(): List<CallEffect> = synchronized(lock) {
        if (!state.live) return emptyList()
        if (state.phase != CallPhase.RINGING_IN && state.phase != CallPhase.RINGING_OUT) return emptyList()
        val outgoing = state.phase == CallPhase.RINGING_OUT
        val detail = CallLink.ringTimeoutDetail(outgoing)
        val event = if (outgoing) CallSignal.HANGUP else CallSignal.REJECT
        val send = CallEffect.Send(state.callId, state.peerDeviceId, event)
        if (outgoing) {
            state = state.copy(link = CallLinkState.FAILED, media = detail, rtcWanted = false)
            return listOf(
                send,
                CallEffect.CancelWatch,
                CallEffect.StopTone,
                CallEffect.ClearNotify,
                CallEffect.Notice(detail),
            )
        }
        noteIncomingRing(state.peerDeviceId)
        hardEndLocked()
        return listOf(send, CallEffect.Notice(detail), CallEffect.TearDown)
    }

    fun onRingSendFailed(): List<CallEffect> = synchronized(lock) {
        if (!state.live) return emptyList()
        val detail = CallLink.offlineDetail()
        state = state.copy(link = CallLinkState.FAILED, media = detail, rtcWanted = false)
        return listOf(
            CallEffect.CancelWatch,
            CallEffect.StopTone,
            CallEffect.ClearNotify,
            CallEffect.Notice(detail),
        )
    }

    private fun localAcceptLocked(): List<CallEffect> {
        if (!state.live || state.phase != CallPhase.RINGING_IN) return emptyList()
        enterNegotiating(asCaller = false)
        return listOf(
            CallEffect.Send(state.callId, state.peerDeviceId, CallSignal.ACCEPT),
            CallEffect.StopTone,
            CallEffect.ClearNotify,
            CallEffect.StartRtc(false, state.video),
            CallEffect.WatchConnect,
        ) + drainLocked()
    }

    private fun endLocalLocked(event: String): List<CallEffect> {
        if (!state.live) return emptyList()
        noteIncomingRing(state.peerDeviceId)
        val send = CallEffect.Send(state.callId, state.peerDeviceId, event)
        hardEndLocked()
        return listOf(send, CallEffect.TearDown)
    }

    private fun onRingLocked(from: String, callId: String, myId: String, payload: Any?): List<CallEffect> {
        val incomingVideo = VideoCallRules.parseRingVideo(payload)
        if (state.live && PeerIds.same(state.peerDeviceId, from) && state.outgoing && state.phase == CallPhase.RINGING_OUT) {
            return resolveGlareLocked(from, callId, myId, incomingVideo)
        }
        if (state.live && matchesLocked(from, callId)) return emptyList()
        val live = state.live
        val now = System.currentTimeMillis()
        val flood = !live && !CallLink.acceptIncomingRing(from, lastIncomingRingFrom, now, lastIncomingRingAtMs)
        if (CallLink.rejectSurplusRing(live, from, state.peerDeviceId, flood)) {
            if (!live) noteIncomingRing(from, now)
            return listOf(CallEffect.Send(callId, from, CallSignal.REJECT))
        }
        if (live) return emptyList()
        noteIncomingRing(from, now)
        state = CallMachineState(
            callId = callId,
            peerDeviceId = from,
            outgoing = false,
            phase = CallPhase.RINGING_IN,
            link = CallLinkState.RINGING,
            media = if (incomingVideo) "один тап — ответить · видео" else "один тап — ответить",
            ended = false,
            startedAtMs = System.currentTimeMillis(),
            video = incomingVideo,
        )
        return incomingRingEffects()
    }

    private fun resolveGlareLocked(
        from: String,
        theirCallId: String,
        myId: String,
        incomingVideo: Boolean,
    ): List<CallEffect> {
        val mine = state.callId
        val canon = CallLink.canonicalCallId(mine, theirCallId)
        val alt = if (canon == mine) theirCallId else mine
        val weOffer = CallLink.weCreateOffer(myId, from)
        val video = state.video || incomingVideo
        enterNegotiating(asCaller = weOffer)
        state = state.copy(callId = canon, altCallId = alt, video = video)
        return listOf(
            CallEffect.StopTone,
            CallEffect.StartRtc(weOffer, video),
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
            CallEffect.StartRtc(true, state.video),
            CallEffect.WatchConnect,
        ) + drainLocked()
    }

    private fun onRemoteEndLocked(from: String, callId: String): List<CallEffect> {
        if (!state.live) return emptyList()
        if (!PeerIds.same(state.peerDeviceId, from) && !matchesLocked(from, callId)) return emptyList()
        hardEndLocked()
        return listOf(CallEffect.TearDown)
    }

    private fun onWssLocked(from: String, callId: String, event: String, payload: Any?): List<CallEffect> {
        if (!state.live || !matchesLocked(from, callId)) return emptyList()
        if (state.phase != CallPhase.ACTIVE) return emptyList()
        val out = mutableListOf<CallEffect>()
        if (!state.wssMedia) {
            out += startWssFallbackLocked(sendRelay = true)
        }
        if (event == CallSignal.AUDIO) {
            val sig = CallSignal.parseMedia(event, payload)
            if (sig != null) out += CallEffect.DeliverAudio(listOf(sig))
        }
        if (state.link != CallLinkState.CONNECTED) {
            state = state.copy(link = CallLinkState.CONNECTED, media = CallMedia.CHAT)
            out += CallEffect.CancelWatch
        }
        return out
    }

    private fun startWssFallbackLocked(sendRelay: Boolean): List<CallEffect> {
        if (!state.live) return emptyList()
        val already = state.wssMedia
        state = state.copy(
            wssMedia = true,
            rtcWanted = false,
            phase = CallPhase.ACTIVE,
            lastIce = "",
            link = if (already && state.link == CallLinkState.CONNECTED) {
                CallLinkState.CONNECTED
            } else {
                CallLinkState.CONNECTING
            },
            media = CallMedia.CHAT,
        )
        val out = mutableListOf<CallEffect>()
        if (!already) {
            out += CallEffect.StopTone
            out += CallEffect.StartWssMedia
        }
        out += CallEffect.CancelWatch
        if (sendRelay && !already) {
            out += CallEffect.Send(state.callId, state.peerDeviceId, CallSignal.RELAY)
        }
        return out
    }

    private fun onMediaLocked(from: String, callId: String, event: String, payload: Any?): List<CallEffect> {
        val sig = CallSignal.parseMedia(event, payload)
        if (!state.live) {
            if (event != CallSignal.OFFER) return emptyList()
            if (sig == null || !sig.wireSafe()) return emptyList()
            val video = VideoCallRules.sdpHasVideo(sig.sdp)
            state = CallMachineState(
                callId = callId,
                peerDeviceId = from,
                outgoing = false,
                phase = CallPhase.RINGING_IN,
                link = CallLinkState.RINGING,
                media = if (video) "один тап — ответить · видео" else "один тап — ответить",
                ended = false,
                queue = listOf(sig),
                startedAtMs = System.currentTimeMillis(),
                video = video,
            )
            return incomingRingEffects()
        }
        if (!matchesLocked(from, callId)) return emptyList()
        if (sig == null || !sig.wireSafe()) return emptyList()
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
        if (sig.kind == CallSignal.OFFER && VideoCallRules.sdpHasVideo(sig.sdp)) {
            state = state.copy(video = true)
        }
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
        val becameReady = remote && !state.remoteDescriptionReady
        val media = if (becameReady && state.phase == CallPhase.ACTIVE && state.link == CallLinkState.CONNECTING) {
            CallLink.connectingDetail(state.hasTurn, remoteReady = true, fellBack = state.relayFellBack)
        } else {
            state.media
        }
        state = state.copy(queue = keep2, remoteDescriptionReady = remote, media = media)
        return if (deliver.isEmpty()) emptyList() else listOf(CallEffect.DeliverRemote(deliver))
    }

    private fun enterNegotiating(asCaller: Boolean) {
        val now = System.currentTimeMillis()
        state = state.copy(
            phase = CallPhase.ACTIVE,
            link = CallLinkState.CONNECTING,
            media = CallLink.waitingSdpDetail(),
            role = if (asCaller) CallRtcRole.OFFERER else CallRtcRole.ANSWERER,
            rtcWanted = true,
            connectStartedAtMs = if (state.connectStartedAtMs > 0L) state.connectStartedAtMs else now,
        )
    }

    private fun hardEndLocked() {
        state = CallMachineState()
    }

    private fun matchesLocked(from: String, callId: String): Boolean =
        CallLink.matchesCall(callId, from, state.callId, state.altCallId, state.peerDeviceId)

    private fun noteIncomingRing(from: String, nowMs: Long = System.currentTimeMillis()) {
        val peer = PeerIds.normalize(from)
        if (peer.isBlank()) return
        lastIncomingRingFrom = peer
        lastIncomingRingAtMs = nowMs
    }
}
