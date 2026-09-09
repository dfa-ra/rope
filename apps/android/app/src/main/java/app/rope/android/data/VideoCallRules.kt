package app.rope.android.data

import org.json.JSONObject

enum class CallMediaStart {
    ABORT,
    AUDIO,
    VIDEO,
}

/**
 * Telegram-like 1:1 video calls on the existing WebRTC + WSS `type=call` path.
 * Live media stays DTLS-SRTP. Do not put camera frames on WSS `audio` or mailbox.
 * Go payload cap stays 16384 — Android must not send oversized SDP.
 */
object VideoCallRules {
    const val MAX_WSS_PAYLOAD = 16384
    const val WIDTH = 640
    const val HEIGHT = 480
    const val FPS = 24
    const val TRACK_ID = "rope-video"
    const val STREAM_ID = "rope"

    fun showHeader(peerId: String?, isGroup: Boolean): Boolean =
        !isGroup && !peerId.isNullOrBlank() && SavedMessagesRules.canCall(peerId)

    fun ringPayload(video: Boolean): String =
        if (video) JSONObject().put("v", 1).put("video", true).toString() else ""

    fun parseRingVideo(payload: Any?): Boolean = when (payload) {
        null -> false
        is Boolean -> payload
        is JSONObject -> payload.optBoolean("video")
        is String -> {
            val t = payload.trim()
            if (t.isEmpty() || t.equals("null", ignoreCase = true)) {
                false
            } else {
                runCatching { JSONObject(t).optBoolean("video") }.getOrDefault(false)
            }
        }
        else -> false
    }

    fun sdpHasVideo(sdp: String): Boolean =
        sdp.lineSequence().any { it.startsWith("m=video") }

    fun utf8Bytes(payload: String): Int = payload.toByteArray(Charsets.UTF_8).size

    fun fitsWss(payload: String): Boolean = utf8Bytes(payload) <= MAX_WSS_PAYLOAD

    fun cameraDeniedNotice(): String = "Нет доступа к камере"

    fun cameraFailedNotice(): String = "Камера недоступна · только звук"

    fun cameraDenyFallbackNotice(): String = cameraFailedNotice()

    /**
     * Mic is required. Camera deny on an outgoing video request starts an
     * audio call instead of aborting. Incoming accept still proceeds without camera.
     */
    fun afterOutgoingVideoPermission(micGranted: Boolean, cameraGranted: Boolean): CallMediaStart = when {
        !micGranted -> CallMediaStart.ABORT
        cameraGranted -> CallMediaStart.VIDEO
        else -> CallMediaStart.AUDIO
    }

    fun proceedIncoming(micGranted: Boolean): Boolean = micGranted

    fun noticeCameraDenyFallback(wantVideo: Boolean, cameraGranted: Boolean): String? =
        if (wantVideo && !cameraGranted) cameraDenyFallbackNotice() else null

    fun sdpTooLargeNotice(): String =
        "SDP слишком большой для релея · кап 16 КиБ без повышения"

    fun recordLabel(video: Boolean, outgoing: Boolean): String = when {
        video && outgoing -> "Исходящий видеозвонок"
        video && !outgoing -> "Входящий видеозвонок"
        outgoing -> "Исходящий звонок"
        else -> "Входящий звонок"
    }

    fun incomingHeading(video: Boolean): String =
        if (video) "Входящий видеовызов" else "Входящий вызов"

    fun outgoingHeading(video: Boolean): String =
        if (video) "Видеовызов…" else "Вызов…"

    fun activeHeading(video: Boolean): String =
        if (video) "Видеозвонок" else "Разговор"
}
