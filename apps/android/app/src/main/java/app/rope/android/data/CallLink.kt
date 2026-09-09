package app.rope.android.data

enum class CallLinkState {
    RINGING,
    CONNECTING,
    CONNECTED,
    FAILED,
}

/** Call ICE / UI mapping extracted so unit tests can cover timeout and copy. */
object CallLink {
    const val CONNECT_TIMEOUT_MS = 25_000L
    const val RING_TIMEOUT_MS = 45_000L
    /** Drop back-to-back RING from the same peer after reject/hangup. Glare still uses live RINGING_OUT. */
    const val RING_FLOOD_COOLDOWN_MS = 2_000L

    fun withinRingCooldown(nowMs: Long, lastAtMs: Long): Boolean =
        lastAtMs > 0L && nowMs - lastAtMs < RING_FLOOD_COOLDOWN_MS

    /**
     * New incoming RING while idle. Same-peer RING inside the cooldown is flood.
     * A different peer is always allowed. Live glare is decided before this.
     */
    fun acceptIncomingRing(
        from: String,
        lastFrom: String,
        nowMs: Long,
        lastAtMs: Long,
    ): Boolean {
        if (from.isBlank()) return false
        if (!PeerIds.same(lastFrom, from)) return true
        return !withinRingCooldown(nowMs, lastAtMs)
    }

    fun heading(
        phase: CallPhase,
        link: CallLinkState,
        detail: String = "",
        video: Boolean = false,
    ): String = when (phase) {
        CallPhase.RINGING_IN -> VideoCallRules.incomingHeading(video)
        CallPhase.RINGING_OUT -> when (link) {
            CallLinkState.FAILED -> if (isOffline(detail)) "Не в сети" else "Нет ответа"
            else -> VideoCallRules.outgoingHeading(video)
        }
        CallPhase.ENDED -> "Завершён"
        CallPhase.ACTIVE -> when (link) {
            CallLinkState.CONNECTED -> VideoCallRules.activeHeading(video)
            CallLinkState.FAILED -> "Нет соединения"
            else -> "Соединение…"
        }
    }

    fun timedOut(elapsedMs: Long, link: CallLinkState): Boolean =
        elapsedMs >= CONNECT_TIMEOUT_MS && link != CallLinkState.CONNECTED && link != CallLinkState.FAILED

    fun ringTimedOut(elapsedMs: Long, phase: CallPhase): Boolean =
        elapsedMs >= RING_TIMEOUT_MS &&
            (phase == CallPhase.RINGING_IN || phase == CallPhase.RINGING_OUT)

    fun ringTimedOut(elapsedMs: Long, phase: CallPhase, link: CallLinkState): Boolean =
        ringTimedOut(elapsedMs, phase) &&
            link != CallLinkState.CONNECTED &&
            link != CallLinkState.FAILED &&
            phase == CallPhase.RINGING_OUT

    fun weCreateOffer(myDeviceId: String, peerDeviceId: String): Boolean {
        val me = PeerIds.normalize(myDeviceId)
        val peer = PeerIds.normalize(peerDeviceId)
        return me.isNotBlank() && peer.isNotBlank() && me < peer
    }

    fun canonicalCallId(localId: String, remoteId: String): String {
        val a = localId.trim()
        val b = remoteId.trim()
        if (a.isEmpty()) return b
        if (b.isEmpty()) return a
        return if (a <= b) a else b
    }

    /**
     * Live media (offer/answer/ICE) and hangup bind to the current peer.
     * A matching call id from a third device must not inject SDP into the call.
     */
    fun matchesCall(
        eventCallId: String,
        eventFrom: String,
        callId: String,
        altCallId: String,
        peerDeviceId: String,
    ): Boolean {
        val from = PeerIds.normalize(eventFrom)
        val peer = PeerIds.normalize(peerDeviceId)
        if (from.isBlank() || peer.isBlank() || from != peer) return false
        val ev = eventCallId.trim()
        val id = callId.trim()
        val alt = altCallId.trim()
        if (ev.isBlank() || id.isBlank()) return true
        return ev == id || ev == alt
    }

    fun shouldQueueSignal(
        kind: String,
        sessionReady: Boolean,
        localOfferReady: Boolean,
        remoteDescriptionReady: Boolean,
    ): Boolean {
        if (kind in CallSignal.FALLBACK) return false
        if (kind !in CallSignal.EVENTS) return false
        if (!sessionReady) return true
        return when (kind) {
            CallSignal.ANSWER -> !localOfferReady
            CallSignal.ICE -> !remoteDescriptionReady
            else -> false
        }
    }

    fun chatDetail(): String = CallMedia.CHAT

    fun ringTimeoutDetail(outgoing: Boolean): String =
        if (outgoing) noAnswerDetail() else "пропущен"

    fun missingTurn(resolved: List<IceServerSpec>): Boolean =
        resolved.none { it.hasTurn }

    fun infoHasIceServers(iceServersJson: String?): Boolean =
        IceServers.parse(iceServersJson).isNotEmpty()

    fun timeoutDetail(hasTurn: Boolean): String =
        if (hasTurn) {
            "нет пути за 25 с · TURN не выделил / ICE не соединил · проверьте 3478 / 443"
        } else {
            "нет пути за 25 с · нет TURN · обновите ядро"
        }

    fun missingIceServersDetail(): String =
        "нет TURN · /v1/info без ice_servers · обновите ядро"

    fun missingTurnDetail(): String =
        "нет TURN · обновите ядро · пробуем STUN"

    fun noAnswerDetail(): String = "абонент не ответил"

    fun noSdpDetail(): String = "нет SDP-ответа · это сигналинг, не ICE"

    fun waitingSdpDetail(): String = "WebRTC · ждём SDP…"

    fun fallbackDirectDetail(): String = "WebRTC · пробуем host/srflx…"

    fun iceRestartDetail(): String = "WebRTC · перезапуск ICE…"

    fun offlineDetail(): String = "абонент не в сети"

    fun iceFailedDetail(viaRelay: Boolean, hasTurn: Boolean): String = when {
        !hasTurn -> "ICE failed · нет TURN · обновите ядро"
        viaRelay -> "ICE failed · нет пути · через сервер · TURN не соединил"
        else -> "ICE failed · нет прямого пути · нужен TURN"
    }

    fun disconnectedDetail(): String = "связь прервалась · ищем путь снова…"

    fun connectingDetail(
        hasTurn: Boolean,
        remoteReady: Boolean = true,
        fellBack: Boolean = false,
    ): String = when {
        !remoteReady -> waitingSdpDetail()
        fellBack -> fallbackDirectDetail()
        hasTurn -> "WebRTC · ищем путь…"
        else -> missingTurnDetail()
    }

    fun connectedDetail(viaRelay: Boolean): String =
        if (viaRelay) CallMedia.RELAY else CallMedia.DIRECT

    fun ringingDetail(hasTurn: Boolean, infoMissing: Boolean): String = when {
        infoMissing -> missingIceServersDetail()
        !hasTurn -> missingTurnDetail()
        else -> "ожидаем ответа"
    }

    fun iceCompact(ice: String): String {
        val name = ice.trim()
        if (name.isEmpty()) return ""
        return "ICE ${name.uppercase()}"
    }

    fun clock(elapsedMs: Long, phase: CallPhase, link: CallLinkState): String? {
        if (link == CallLinkState.CONNECTED || link == CallLinkState.FAILED || phase == CallPhase.ENDED) {
            return null
        }
        val limitMs = if (phase == CallPhase.ACTIVE) CONNECT_TIMEOUT_MS else RING_TIMEOUT_MS
        val sec = (elapsedMs.coerceAtLeast(0L) / 1000L).toInt()
        if (sec <= 0) return null
        return "$sec с / ${limitMs / 1000} с"
    }

    fun infoStatusLine(iceServersJson: String?): String {
        val parsed = IceServers.parse(iceServersJson)
        return when {
            parsed.isEmpty() -> "GET /v1/info: нет ice_servers · нет TURN · обновите ядро"
            parsed.none { it.hasTurn } -> "GET /v1/info: STUN есть, TURN нет · обновите ядро"
            else -> "GET /v1/info: TURN получен"
        }
    }

    fun subtitle(call: CallInfo, iceServersJson: String = ""): String {
        if (call.link == CallLinkState.CONNECTED) {
            return call.media.ifBlank { connectedDetail(false) }
        }
        if (call.link == CallLinkState.FAILED) {
            return call.media.ifBlank { iceFailedDetail(false, call.hasTurn) }
        }
        val parsed = IceServers.parse(iceServersJson)
        val noTurn = when {
            call.iceReady -> !call.hasTurn
            iceServersJson.isNotBlank() -> parsed.none { it.hasTurn }
            else -> false
        }
        if (noTurn || (call.iceReady && parsed.isEmpty())) {
            val warning = if (parsed.isEmpty()) missingIceServersDetail() else missingTurnDetail()
            return if (call.phase == CallPhase.RINGING_IN) {
                "один тап — ответить · $warning"
            } else {
                warning
            }
        }
        return call.media.ifBlank {
            when (call.phase) {
                CallPhase.RINGING_IN -> "один тап — ответить"
                CallPhase.RINGING_OUT -> ringingDetail(hasTurn = true, infoMissing = false)
                CallPhase.ACTIVE -> connectingDetail(true)
                CallPhase.ENDED -> ""
            }
        }
    }

    fun applyIce(
        ice: String,
        viaRelay: Boolean = false,
        hasTurn: Boolean = true,
        remoteReady: Boolean = true,
        fellBack: Boolean = false,
    ): Pair<CallLinkState, String> {
        val name = ice.trim().uppercase()
        return when (name) {
            "CONNECTED", "COMPLETED" -> CallLinkState.CONNECTED to connectedDetail(viaRelay)
            "FAILED" -> CallLinkState.FAILED to iceFailedDetail(viaRelay, hasTurn)
            "CLOSED", "DISCONNECTED" -> CallLinkState.CONNECTING to disconnectedDetail()
            "CHECKING", "CONNECTING", "NEW" ->
                CallLinkState.CONNECTING to connectingDetail(hasTurn, remoteReady, fellBack)
            else -> CallLinkState.CONNECTING to if (hasTurn) "WebRTC · соединяем" else missingTurnDetail()
        }
    }

    fun iceIsFailed(ice: String): Boolean {
        val name = ice.trim().uppercase()
        return name == "FAILED"
    }

    fun applyInfo(iceServersJson: String?, current: CallInfo): CallInfo {
        val parsed = IceServers.parse(iceServersJson)
        val resolved = IceServers.resolve(parsed, null)
        val hasTurn = !missingTurn(resolved)
        if (current.link == CallLinkState.CONNECTED || current.link == CallLinkState.FAILED) {
            return current.copy(hasTurn = hasTurn, iceReady = true)
        }
        val infoMissing = parsed.isEmpty()
        val media = when {
            current.phase == CallPhase.RINGING_IN -> current.media
            infoMissing -> missingIceServersDetail()
            !hasTurn -> missingTurnDetail()
            current.media.contains("нет TURN") || current.media.contains("ice_servers") ->
                if (current.phase == CallPhase.ACTIVE) connectingDetail(true) else "ожидаем ответа"
            current.media.isBlank() && current.phase == CallPhase.RINGING_OUT -> "ожидаем ответа"
            else -> current.media
        }
        return current.copy(hasTurn = hasTurn, iceReady = true, media = media)
    }

    private fun isOffline(detail: String): Boolean =
        detail == offlineDetail() || detail.contains("не в сети")
}
