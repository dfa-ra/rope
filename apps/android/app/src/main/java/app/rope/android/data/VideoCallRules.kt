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
    const val BYE = "bye"

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
        sdp.lineSequence().any { it.trim().startsWith("m=video") }

    /**
     * libwebrtc's SDP parser is CRLF-strict. JSON roundtrip and
     * [JsonIds.optional] trim can leave LF-only or a missing trailing CRLF.
     * Never truncate — 16 KiB refusal stays at send time.
     */
    fun sdpForPeerConnection(sdp: String): String {
        if (sdp.isEmpty()) return sdp
        val lf = sdp.replace("\r\n", "\n").replace("\r", "\n").trimEnd() + "\n"
        return lf.replace("\n", "\r\n")
    }

    /** Compose `graphicsLayer` (FadeIn) makes TextureView draw black. */
    fun callOverlayUsesOffscreenLayer(): Boolean = false

    /** FillMaxSize TextureView often reports 0×0 before the first layout. */
    fun rendererSurfaceReady(width: Int, height: Int): Boolean = width > 0 && height > 0

    /** Plan-B onAddStream still carries the remote video track on some devices. */
    fun bindRemoteFromAddStream(): Boolean = true

    fun offerToReceiveAudio(): String = "true"

    /** Unified Plan still honors this Plan-B key; video offers/answers must recv. */
    fun offerToReceiveVideo(video: Boolean): String = if (video) "true" else "false"

    fun answerReceivesVideo(wantVideo: Boolean, remoteSdp: String): Boolean =
        wantVideo || sdpHasVideo(remoteSdp)

    /** SDP setLocal/setRemote failure is not ICE failed and must not hang up. */
    fun sdpErrorFailsIce(): Boolean = false

    /**
     * PeerConnection CLOSED is local teardown (or the peer already left).
     * Do not map it to ICE failed / WSS fallback / hangup.
     */
    fun ignorePcClosed(iceName: String): Boolean =
        iceName.trim().equals("CLOSED", ignoreCase = true)

    /**
     * After DTLS is up, FAILED / DISCONNECTED / CHECKING / CONNECTING are path
     * blips (mute, flip, camera-deny, renegotiation, PeerConnection CONNECTING).
     * They must not hang up the remote or reset the in-call UI back to ringing.
     */
    fun iceBlipKeepsCall(iceName: String, mediaWasUp: Boolean): Boolean {
        val name = iceName.trim().uppercase()
        if (name == "DISCONNECTED" || name == "CLOSED") return true
        if (!mediaWasUp) return false
        return name == "FAILED" || name == "CHECKING" || name == "CONNECTING" || name == "NEW"
    }

    /** ICE compact ("ICE CHECKING") stays off once the overlay is in-call. */
    fun showIceCompact(link: CallLinkState, media: String): Boolean =
        link != CallLinkState.CONNECTED &&
            link != CallLinkState.FAILED &&
            media != CallMedia.CHAT

    /** Compose must not mount the sink until the PC EGL context exists. */
    fun mountCallRenderer(video: Boolean, phase: CallPhase, media: String, rtcReady: Boolean): Boolean =
        video && phase == CallPhase.ACTIVE && media != CallMedia.CHAT && rtcReady

    /** Local camera track is added in WebRtcSession init, before createOffer. */
    fun cameraTrackAttachedWhenVideoCall(wantVideo: Boolean, camMuted: Boolean): Boolean =
        shouldStartLocalCamera(wantVideo, camMuted)

    /** WSS audio fallback only when ICE never connected. Never a user hangup. */
    fun iceFailedFallsBackToWss(iceName: String, mediaWasUp: Boolean): Boolean =
        !mediaWasUp && iceName.trim().equals("FAILED", ignoreCase = true)

    fun wireEventIsHangup(event: String): Boolean {
        if (!CallSignal.singleLine(event)) return false
        val v = event.trim().lowercase()
        return v == CallSignal.HANGUP || v == BYE
    }

    /**
     * TURN username / credential / TLS SNI hostname. CR/LF/NUL must not
     * collapse onto a live field after trim.
     */
    fun iceField(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        if (!CallSignal.singleLine(raw)) return null
        return raw.trim().takeIf { it.isNotEmpty() }
    }

    fun utf8Bytes(payload: String): Int = payload.toByteArray(Charsets.UTF_8).size

    fun fitsWss(payload: String): Boolean = utf8Bytes(payload) <= MAX_WSS_PAYLOAD

    fun cameraDeniedNotice(): String = "Нет доступа к камере"

    fun cameraFailedNotice(): String = "Камера недоступна · только звук"

    fun cameraDenyFallbackNotice(): String = cameraFailedNotice()

    const val ONE_WAY_VIDEO_MS = 4_000L

    fun oneWayRemoteNotice(): String = "нет видео пира"

    /**
     * After ICE is up, a video call with no remote track is one-way media —
     * local preview can still exist. Do not overwrite camera-deny copy.
     */
    fun oneWayVideoNotice(
        video: Boolean,
        mediaUp: Boolean,
        wssFallback: Boolean,
        remoteVideoBound: Boolean,
        connectedForMs: Long,
    ): String? {
        if (!video || !mediaUp || wssFallback || remoteVideoBound) return null
        if (connectedForMs < ONE_WAY_VIDEO_MS) return null
        return oneWayRemoteNotice()
    }

    fun keepExistingOverlayNotice(existing: String?, incoming: String?): String? =
        existing?.takeIf { it.isNotBlank() } ?: incoming

    fun flipCameraWhileSending(camMuted: Boolean): Boolean = !camMuted

    /** Front camera PIP is mirrored; rear is not. */
    fun localPreviewMirrored(frontFacing: Boolean): Boolean = frontFacing

    /**
     * Trickle ICE is a handful of candidates. An unbounded queue before
     * setRemote (or after) is a memory/CPU hammer from the live peer.
     */
    const val ICE_PER_SESSION_CAP = 64

    fun acceptIce(accepted: Int): Boolean = accepted in 0 until ICE_PER_SESSION_CAP

    /**
     * MainActivity handles orientation itself so an in-call TextureView is
     * not destroyed (EGL 0×0 / black remote after rotate).
     */
    fun activityKeepsSurfacesOnRotate(): Boolean = true

    fun micDeniedNotice(): String = "Нет доступа к микрофону"

    /** Incoming Accept + mic deny must land on the overlay, not a Scaffold snackbar. */
    fun noticeUsesOverlay(hasCall: Boolean): Boolean = hasCall

    fun micDeniedUsesOverlay(hasCall: Boolean): Boolean = noticeUsesOverlay(hasCall)

    fun overlayMicDenied(hasCall: Boolean): String? =
        if (hasCall) micDeniedNotice() else null

    /**
     * Video sessions keep an m=video transceiver even when the local camera is
     * muted or denied, so later unmute can send without a missing m-line.
     * Reserving that m-line must not make the callee send the first offer.
     */
    fun reserveVideoTransceiver(wantVideo: Boolean, startCamera: Boolean): Boolean =
        wantVideo && !startCamera

    /**
     * Observer-driven offer (perfect negotiation): only the initial offerer,
     * only while STABLE, never to dodge the callee guard with renegotiate=true.
     */
    fun offerOnRenegotiationNeeded(
        pcReady: Boolean,
        callee: Boolean,
        signalingStable: Boolean,
        makingOffer: Boolean = false,
    ): Boolean = pcReady && !callee && signalingStable && !makingOffer

    /** Adding the first local video track after mute-before-connect needs a new offer. */
    fun renegotiateOnCameraUnmute(hadLocalTrack: Boolean): Boolean = !hadLocalTrack

    /**
     * Explicit unmute offer when STABLE. Callee uses this path because the
     * observer must not offer. Skip if an offer is already in flight (glare).
     */
    fun explicitOfferOnCameraUnmute(
        hadLocalTrack: Boolean,
        signalingStable: Boolean,
        makingOffer: Boolean = false,
    ): Boolean = renegotiateOnCameraUnmute(hadLocalTrack) && signalingStable && !makingOffer

    /**
     * Impolite (caller) drops a remote offer while making one. Polite (callee)
     * accepts and relies on implicit rollback.
     */
    fun ignoreRemoteOfferOnGlare(
        makingOffer: Boolean,
        haveLocalOffer: Boolean,
        polite: Boolean,
    ): Boolean = (makingOffer || haveLocalOffer) && !polite

    fun applyUnmuteCamResult(hasCall: Boolean): Boolean = hasCall

    /** Mic-deny banner drops once Accept actually proceeds. Camera-deny stays. */
    fun noticeAfterAccept(notice: String?): String? =
        if (notice == micDeniedNotice()) null else notice

    /** In-call unmute that starts a track clears the «только звук» banner. */
    fun noticeAfterCameraUnmute(notice: String?): String? =
        if (notice == cameraFailedNotice() || notice == cameraDeniedNotice()) null else notice

    fun inCallUnmuteNeedsCameraPermission(unmuting: Boolean, cameraGranted: Boolean): Boolean =
        unmuting && !cameraGranted

    /** Incoming accept with camera deny still proceeds; local camera stays muted. */
    fun incomingCameraMuted(cameraGranted: Boolean): Boolean = !cameraGranted

    fun shouldStartLocalCamera(wantVideo: Boolean, camMuted: Boolean): Boolean =
        wantVideo && !camMuted

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
