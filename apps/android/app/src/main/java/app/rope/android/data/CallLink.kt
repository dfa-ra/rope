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

    fun heading(phase: CallPhase, link: CallLinkState, detail: String = ""): String = when (phase) {
        CallPhase.RINGING_IN -> "Входящий вызов"
        CallPhase.RINGING_OUT -> when (link) {
            CallLinkState.FAILED -> if (isOffline(detail)) "Не в сети" else "Нет ответа"
            else -> "Вызов…"
        }
        CallPhase.ENDED -> "Завершён"
        CallPhase.ACTIVE -> when (link) {
            CallLinkState.CONNECTED -> "Разговор"
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

    fun matchesCall(
        eventCallId: String,
        eventFrom: String,
        callId: String,
        altCallId: String,
        peerDeviceId: String,
    ): Boolean {
        val from = PeerIds.normalize(eventFrom)
        val peer = PeerIds.normalize(peerDeviceId)
        val ev = eventCallId.trim()
        val id = callId.trim()
        val alt = altCallId.trim()
        if (from.isNotBlank() && from == peer) {
            if (ev.isBlank() || id.isBlank()) return true
            return ev == id || ev == alt
        }
        return ev.isNotBlank() && (ev == id || ev == alt)
    }

    fun shouldQueueSignal(
        kind: String,
        sessionReady: Boolean,
        localOfferReady: Boolean,
        remoteDescriptionReady: Boolean,
    ): Boolean {
        if (kind !in CallSignal.EVENTS) return false
        if (!sessionReady) return true
        return when (kind) {
            CallSignal.ANSWER -> !localOfferReady
            CallSignal.ICE -> !remoteDescriptionReady
            else -> false
        }
    }

    fun ringTimeoutDetail(outgoing: Boolean): String =
        if (outgoing) noAnswerDetail() else "пропущен"

    fun missingTurn(resolved: List<IceServerSpec>): Boolean =
        resolved.none { it.hasTurn }

    fun infoHasIceServers(iceServersJson: String?): Boolean =
        IceServers.parse(iceServersJson).isNotEmpty()

    fun timeoutDetail(hasTurn: Boolean): String =
        if (hasTurn) {
            "нет пути за 25 с · проверьте TURN (3478 / 443 или 5349) или обновите ядро"
        } else {
            "нет пути за 25 с · нет TURN · обновите ядро"
        }

    fun missingIceServersDetail(): String =
        "нет TURN · /v1/info без ice_servers · обновите ядро"

    fun missingTurnDetail(): String =
        "нет TURN · обновите ядро · пробуем STUN"

    fun noAnswerDetail(): String = "абонент не ответил"

    fun offlineDetail(): String = "абонент не в сети"

    fun iceFailedDetail(viaRelay: Boolean, hasTurn: Boolean): String = when {
        !hasTurn -> "ICE failed · нет TURN · обновите ядро"
        viaRelay -> "ICE failed · нет пути · через сервер · TURN не соединил"
        else -> "ICE failed · нет прямого пути · нужен TURN"
    }

    fun disconnectedDetail(): String = "связь прервалась · ищем путь снова…"

    fun connectingDetail(hasTurn: Boolean): String =
        if (hasTurn) "WebRTC · ищем путь…" else missingTurnDetail()

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

    fun applyIce(ice: String, viaRelay: Boolean = false, hasTurn: Boolean = true): Pair<CallLinkState, String> {
        val name = ice.trim().uppercase()
        return when (name) {
            "CONNECTED", "COMPLETED" -> CallLinkState.CONNECTED to connectedDetail(viaRelay)
            "FAILED", "CLOSED" -> CallLinkState.FAILED to iceFailedDetail(viaRelay, hasTurn)
            "DISCONNECTED" -> CallLinkState.CONNECTING to disconnectedDetail()
            "CHECKING", "CONNECTING", "NEW" -> CallLinkState.CONNECTING to connectingDetail(hasTurn)
            else -> CallLinkState.CONNECTING to if (hasTurn) "WebRTC · соединяем" else missingTurnDetail()
        }
    }

    fun iceIsFailed(ice: String): Boolean {
        val name = ice.trim().uppercase()
        return name == "FAILED" || name == "CLOSED"
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
