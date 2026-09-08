package app.rope.android.data

import org.json.JSONObject

data class CallSignal(
    val kind: String,
    val sdp: String = "",
    val candidate: String = "",
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val frame: String = "",
) {
    fun toJson(): String = JSONObject()
        .put("v", 1)
        .put("kind", kind)
        .put("sdp", sdp)
        .put("candidate", candidate)
        .put("sdp_mid", sdpMid)
        .put("sdp_mline", sdpMLineIndex)
        .put("frame", frame)
        .toString()

    companion object {
        const val RING = "ring"
        const val ACCEPT = "accept"
        const val REJECT = "reject"
        const val HANGUP = "hangup"
        const val OFFER = "offer"
        const val ANSWER = "answer"
        const val ICE = "ice"
        const val RELAY = "relay"
        const val AUDIO = "audio"

        val EVENTS = setOf(OFFER, ANSWER, ICE)
        val CONTROL = setOf(RING, ACCEPT, REJECT, HANGUP)
        val FALLBACK = setOf(RELAY, AUDIO)
        val WIRE = EVENTS + CONTROL + FALLBACK

        fun parseEvent(raw: String?): String? {
            val v = JsonIds.optional(raw)?.lowercase() ?: return null
            return if (v in WIRE) v else null
        }

        fun skipMailbox(event: String): Boolean = event in FALLBACK

        fun parseMedia(event: String?, payload: Any?): CallSignal? {
            val ev = parseEvent(event) ?: return null
            when (ev) {
                RELAY -> return CallSignal(kind = RELAY)
                AUDIO -> {
                    val parsed = parsePayload(payload)
                    return when {
                        parsed == null -> CallSignal(kind = AUDIO)
                        parsed.kind == AUDIO -> parsed
                        else -> parsed.copy(kind = AUDIO)
                    }
                }
                in EVENTS -> {
                    val parsed = parsePayload(payload) ?: return null
                    return if (parsed.kind == ev) parsed else parsed.copy(kind = ev)
                }
                else -> return null
            }
        }

        fun envelopeJson(callId: String, event: String, payload: String = ""): String =
            JSONObject()
                .put("call_id", callId)
                .put("event", event)
                .put("payload", payload)
                .toString()

        fun parsePayload(raw: Any?): CallSignal? = when (raw) {
            null -> null
            is JSONObject -> parse(raw.toString())
            is String -> parse(raw)
            else -> parse(raw.toString())
        }

        fun parse(raw: String): CallSignal? {
            if (raw.isBlank() || raw.equals("null", ignoreCase = true)) return null
            return try {
                val o = JSONObject(raw)
                val kind = JsonIds.optional(o.optString("kind"))?.lowercase() ?: return null
                if (kind !in EVENTS && kind !in FALLBACK) return null
                val sdp = JsonIds.optional(o.optString("sdp")).orEmpty()
                val candidate = JsonIds.optional(o.optString("candidate")).orEmpty()
                val frame = JsonIds.optional(o.optString("frame")).orEmpty()
                if ((kind == OFFER || kind == ANSWER) && sdp.isBlank()) return null
                CallSignal(
                    kind = kind,
                    sdp = sdp,
                    candidate = candidate,
                    sdpMid = JsonIds.optional(o.optString("sdp_mid")).orEmpty(),
                    sdpMLineIndex = o.optInt("sdp_mline"),
                    frame = frame,
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

object CallMedia {
    const val PROTOCOL = "WebRTC"
    const val SECURITY = "DTLS-SRTP"
    const val RELAY = "WebRTC · через сервер"
    const val DIRECT = "WebRTC · DTLS-SRTP"
    const val CHAT = "через чат · E2EE"

    val STUN_URLS = listOf(
        "stun:stun.l.google.com:19302",
        "stun:stun.cloudflare.com:3478",
    )

    fun isRelayCandidate(sdp: String): Boolean {
        val t = sdp.lowercase()
        return t.contains(" typ relay") || t.contains(" typ=relay")
    }

    fun label(ice: String, failed: Boolean = false, viaRelay: Boolean = false): String = when {
        failed || ice.equals("FAILED", true) ->
            if (viaRelay) "нет пути · TURN не соединил" else "нет прямого пути · нужен TURN"
        ice.equals("CONNECTED", true) || ice.equals("COMPLETED", true) ->
            if (viaRelay) RELAY else DIRECT
        ice.equals("CHECKING", true) -> "WebRTC · ищем путь…"
        else -> "WebRTC · соединяем"
    }
}
