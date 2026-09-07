package app.rope.android.data

import org.json.JSONObject

data class CallSignal(
    val kind: String,
    val sdp: String = "",
    val candidate: String = "",
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
) {
    fun toJson(): String = JSONObject()
        .put("v", 1)
        .put("kind", kind)
        .put("sdp", sdp)
        .put("candidate", candidate)
        .put("sdp_mid", sdpMid)
        .put("sdp_mline", sdpMLineIndex)
        .toString()

    companion object {
        const val OFFER = "offer"
        const val ANSWER = "answer"
        const val ICE = "ice"

        val EVENTS = setOf(OFFER, ANSWER, ICE)

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
                val kind = JsonIds.optional(o.optString("kind")) ?: return null
                if (kind !in EVENTS) return null
                val sdp = JsonIds.optional(o.optString("sdp")).orEmpty()
                val candidate = JsonIds.optional(o.optString("candidate")).orEmpty()
                if ((kind == OFFER || kind == ANSWER) && sdp.isBlank()) return null
                CallSignal(
                    kind = kind,
                    sdp = sdp,
                    candidate = candidate,
                    sdpMid = JsonIds.optional(o.optString("sdp_mid")).orEmpty(),
                    sdpMLineIndex = o.optInt("sdp_mline"),
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
