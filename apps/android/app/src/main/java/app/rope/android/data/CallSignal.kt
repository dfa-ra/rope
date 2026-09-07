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

        fun parse(raw: String): CallSignal? {
            if (raw.isBlank()) return null
            return try {
                val o = JSONObject(raw)
                val kind = o.optString("kind")
                if (kind !in EVENTS) return null
                CallSignal(
                    kind = kind,
                    sdp = o.optString("sdp"),
                    candidate = o.optString("candidate"),
                    sdpMid = o.optString("sdp_mid"),
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

    val STUN_URLS = listOf(
        "stun:stun.l.google.com:19302",
        "stun:stun.cloudflare.com:3478",
    )

    fun label(ice: String, failed: Boolean = false): String = when {
        failed || ice.equals("FAILED", true) -> "нет прямого пути · нужен TURN"
        ice.equals("CONNECTED", true) || ice.equals("COMPLETED", true) -> "WebRTC · DTLS-SRTP"
        ice.equals("CHECKING", true) -> "WebRTC · ищем путь…"
        else -> "WebRTC · соединяем"
    }
}
