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

    fun heading(phase: CallPhase, link: CallLinkState): String = when (phase) {
        CallPhase.RINGING_IN -> "Входящий вызов"
        CallPhase.RINGING_OUT -> "Вызов…"
        CallPhase.ENDED -> "Завершён"
        CallPhase.ACTIVE -> when (link) {
            CallLinkState.CONNECTED -> "Разговор"
            CallLinkState.FAILED -> "Нет соединения"
            else -> "Соединение…"
        }
    }

    fun timedOut(elapsedMs: Long, link: CallLinkState): Boolean =
        elapsedMs >= CONNECT_TIMEOUT_MS && link != CallLinkState.CONNECTED && link != CallLinkState.FAILED

    fun weCreateOffer(myDeviceId: String, peerDeviceId: String): Boolean =
        myDeviceId.isNotBlank() && myDeviceId < peerDeviceId

    fun missingTurn(resolved: List<IceServerSpec>): Boolean =
        resolved.none { it.hasTurn }

    fun timeoutDetail(hasTurn: Boolean): String =
        if (hasTurn) {
            "нет пути за 25 с · проверьте TURN (3478 / 443 или 5349) или обновите ядро"
        } else {
            "нет пути за 25 с · TURN не получен · Сервер → Обновить ядро"
        }

    fun missingTurnDetail(): String =
        "TURN не получен с сервера · пробуем STUN. Если зависнет — обновите ядро"

    fun iceFailedDetail(viaRelay: Boolean, hasTurn: Boolean): String = when {
        !hasTurn -> "нет пути · TURN нет на сервере · обновите ядро"
        viaRelay -> "нет пути · TURN не соединил"
        else -> "нет прямого пути · нужен TURN"
    }

    fun disconnectedDetail(): String = "связь прервалась · ищем путь снова…"

    fun connectingDetail(hasTurn: Boolean): String =
        if (hasTurn) "WebRTC · ищем путь…" else missingTurnDetail()

    fun connectedDetail(viaRelay: Boolean): String =
        if (viaRelay) CallMedia.RELAY else CallMedia.DIRECT

    fun applyIce(ice: String, viaRelay: Boolean = false, hasTurn: Boolean = true): Pair<CallLinkState, String> {
        val name = ice.trim().uppercase()
        return when (name) {
            "CONNECTED", "COMPLETED" -> CallLinkState.CONNECTED to connectedDetail(viaRelay)
            "FAILED", "CLOSED" -> CallLinkState.FAILED to iceFailedDetail(viaRelay, hasTurn)
            "DISCONNECTED" -> CallLinkState.CONNECTING to disconnectedDetail()
            "CHECKING" -> CallLinkState.CONNECTING to connectingDetail(hasTurn)
            else -> CallLinkState.CONNECTING to if (hasTurn) "WebRTC · соединяем" else missingTurnDetail()
        }
    }
}
