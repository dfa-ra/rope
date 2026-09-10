package app.rope.android

import app.rope.android.data.CallLinkState
import app.rope.android.data.CallMediaStart
import app.rope.android.data.CallPhase
import app.rope.android.data.CallSignal
import app.rope.android.data.SavedMessagesRules
import app.rope.android.data.VideoCallRules
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCallRulesTest {
    @Test
    fun headerHiddenForGroupsAndSaved() {
        assertTrue(VideoCallRules.showHeader("bob", isGroup = false))
        assertFalse(VideoCallRules.showHeader("bob", isGroup = true))
        assertFalse(VideoCallRules.showHeader(SavedMessagesRules.ID, isGroup = false))
        assertFalse(VideoCallRules.showHeader(null, isGroup = false))
    }

    @Test
    fun ringPayloadIsTinyAndParsed() {
        assertEquals("", VideoCallRules.ringPayload(false))
        assertFalse(VideoCallRules.parseRingVideo(null))
        assertFalse(VideoCallRules.parseRingVideo(""))
        assertFalse(VideoCallRules.parseRingVideo("null"))
        val raw = VideoCallRules.ringPayload(true)
        assertTrue(VideoCallRules.parseRingVideo(raw))
        assertTrue(VideoCallRules.parseRingVideo(JSONObject(raw)))
        assertTrue(VideoCallRules.fitsWss(raw))
        assertTrue(VideoCallRules.utf8Bytes(raw) < 64)
    }

    @Test
    fun sdpVideoLineAndWssCap() {
        assertFalse(VideoCallRules.sdpHasVideo("v=0\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\n"))
        assertTrue(VideoCallRules.sdpHasVideo("v=0\nm=audio 9 UDP/TLS/RTP/SAVPF 111\nm=video 9 UDP/TLS/RTP/SAVPF 96 97\n"))
        assertTrue(
            VideoCallRules.sdpHasVideo(
                "v=0\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\nm=video 9 UDP/TLS/RTP/SAVPF 96\r\n",
            ),
        )
        val offer = CallSignal(
            CallSignal.OFFER,
            sdp = representativeVideoSdp(),
        ).toJson()
        assertTrue(VideoCallRules.sdpHasVideo(CallSignal.parse(offer)!!.sdp))
        assertTrue(
            "video offer JSON must stay under the existing 16 KiB WSS cap; do not raise Go",
            VideoCallRules.fitsWss(offer),
        )
        assertTrue(VideoCallRules.utf8Bytes(offer) < 8_000)
        assertFalse(VideoCallRules.fitsWss("x".repeat(VideoCallRules.MAX_WSS_PAYLOAD + 1)))
    }

    @Test
    fun copyAndRecordLabels() {
        assertEquals("Входящий видеовызов", VideoCallRules.incomingHeading(true))
        assertEquals("Входящий вызов", VideoCallRules.incomingHeading(false))
        assertEquals("Видеовызов…", VideoCallRules.outgoingHeading(true))
        assertEquals("Видеозвонок", VideoCallRules.activeHeading(true))
        assertEquals("Исходящий видеозвонок", VideoCallRules.recordLabel(true, outgoing = true))
        assertEquals("Входящий видеозвонок", VideoCallRules.recordLabel(true, outgoing = false))
        assertEquals("Исходящий звонок", VideoCallRules.recordLabel(false, outgoing = true))
        assertEquals("Нет доступа к камере", VideoCallRules.cameraDeniedNotice())
        assertEquals("Камера недоступна · только звук", VideoCallRules.cameraFailedNotice())
        assertEquals(VideoCallRules.cameraFailedNotice(), VideoCallRules.cameraDenyFallbackNotice())
        assertTrue(VideoCallRules.sdpTooLargeNotice().contains("16 КиБ"))
    }

    @Test
    fun cameraDenyFallsBackToAudioNotAbort() {
        assertEquals(CallMediaStart.ABORT, VideoCallRules.afterOutgoingVideoPermission(false, false))
        assertEquals(CallMediaStart.ABORT, VideoCallRules.afterOutgoingVideoPermission(false, true))
        assertEquals(CallMediaStart.VIDEO, VideoCallRules.afterOutgoingVideoPermission(true, true))
        assertEquals(CallMediaStart.AUDIO, VideoCallRules.afterOutgoingVideoPermission(true, false))
        assertTrue(VideoCallRules.proceedIncoming(true))
        assertFalse(VideoCallRules.proceedIncoming(false))
        assertTrue(VideoCallRules.incomingCameraMuted(false))
        assertFalse(VideoCallRules.incomingCameraMuted(true))
        assertFalse(VideoCallRules.shouldStartLocalCamera(wantVideo = true, camMuted = true))
        assertTrue(VideoCallRules.shouldStartLocalCamera(wantVideo = true, camMuted = false))
        assertFalse(VideoCallRules.shouldStartLocalCamera(wantVideo = false, camMuted = false))
        assertEquals("Нет доступа к микрофону", VideoCallRules.micDeniedNotice())
        assertEquals(
            VideoCallRules.cameraFailedNotice(),
            VideoCallRules.noticeCameraDenyFallback(wantVideo = true, cameraGranted = false),
        )
        assertEquals(null, VideoCallRules.noticeCameraDenyFallback(wantVideo = true, cameraGranted = true))
        assertEquals(null, VideoCallRules.noticeCameraDenyFallback(wantVideo = false, cameraGranted = false))
    }

    @Test
    fun micDeniedNoticeLandsOnOverlayWhileCallPresent() {
        assertTrue(VideoCallRules.micDeniedUsesOverlay(hasCall = true))
        assertFalse(VideoCallRules.micDeniedUsesOverlay(hasCall = false))
        assertEquals(VideoCallRules.micDeniedNotice(), VideoCallRules.overlayMicDenied(hasCall = true))
        assertEquals(null, VideoCallRules.overlayMicDenied(hasCall = false))
    }

    @Test
    fun unmuteAfterMuteBeforeConnectReservesAndRenegotiates() {
        assertTrue(VideoCallRules.reserveVideoTransceiver(wantVideo = true, startCamera = false))
        assertFalse(VideoCallRules.reserveVideoTransceiver(wantVideo = true, startCamera = true))
        assertFalse(VideoCallRules.reserveVideoTransceiver(wantVideo = false, startCamera = false))
        assertTrue(VideoCallRules.renegotiateOnCameraUnmute(hadLocalTrack = false))
        assertFalse(VideoCallRules.renegotiateOnCameraUnmute(hadLocalTrack = true))
        assertTrue(VideoCallRules.inCallUnmuteNeedsCameraPermission(unmuting = true, cameraGranted = false))
        assertFalse(VideoCallRules.inCallUnmuteNeedsCameraPermission(unmuting = true, cameraGranted = true))
        assertFalse(VideoCallRules.inCallUnmuteNeedsCameraPermission(unmuting = false, cameraGranted = false))
    }

    @Test
    fun reservedTransceiverDoesNotMakeCalleeOffer() {
        assertTrue(VideoCallRules.reserveVideoTransceiver(wantVideo = true, startCamera = false))
        assertFalse(
            VideoCallRules.offerOnRenegotiationNeeded(
                pcReady = true,
                callee = true,
                signalingStable = true,
            ),
        )
        assertFalse(
            VideoCallRules.offerOnRenegotiationNeeded(
                pcReady = false,
                callee = false,
                signalingStable = true,
            ),
        )
        assertFalse(
            VideoCallRules.offerOnRenegotiationNeeded(
                pcReady = true,
                callee = false,
                signalingStable = false,
            ),
        )
        assertFalse(
            VideoCallRules.offerOnRenegotiationNeeded(
                pcReady = true,
                callee = false,
                signalingStable = true,
                makingOffer = true,
            ),
        )
        assertTrue(
            VideoCallRules.offerOnRenegotiationNeeded(
                pcReady = true,
                callee = false,
                signalingStable = true,
            ),
        )
        assertTrue(
            VideoCallRules.explicitOfferOnCameraUnmute(
                hadLocalTrack = false,
                signalingStable = true,
            ),
        )
        assertFalse(
            VideoCallRules.explicitOfferOnCameraUnmute(
                hadLocalTrack = true,
                signalingStable = true,
            ),
        )
        assertFalse(
            VideoCallRules.explicitOfferOnCameraUnmute(
                hadLocalTrack = false,
                signalingStable = false,
            ),
        )
        assertFalse(
            VideoCallRules.explicitOfferOnCameraUnmute(
                hadLocalTrack = false,
                signalingStable = true,
                makingOffer = true,
            ),
        )
        assertTrue(
            VideoCallRules.ignoreRemoteOfferOnGlare(
                makingOffer = true,
                haveLocalOffer = false,
                polite = false,
            ),
        )
        assertTrue(
            VideoCallRules.ignoreRemoteOfferOnGlare(
                makingOffer = false,
                haveLocalOffer = true,
                polite = false,
            ),
        )
        assertFalse(
            VideoCallRules.ignoreRemoteOfferOnGlare(
                makingOffer = true,
                haveLocalOffer = true,
                polite = true,
            ),
        )
        assertFalse(
            VideoCallRules.ignoreRemoteOfferOnGlare(
                makingOffer = false,
                haveLocalOffer = false,
                polite = false,
            ),
        )
    }

    @Test
    fun unmuteCamResultAndOverlayNoticesClearOnSuccess() {
        assertFalse(VideoCallRules.applyUnmuteCamResult(hasCall = false))
        assertTrue(VideoCallRules.applyUnmuteCamResult(hasCall = true))
        assertTrue(VideoCallRules.noticeUsesOverlay(hasCall = true))
        assertFalse(VideoCallRules.noticeUsesOverlay(hasCall = false))
        assertEquals(null, VideoCallRules.noticeAfterAccept(VideoCallRules.micDeniedNotice()))
        assertEquals(
            VideoCallRules.cameraFailedNotice(),
            VideoCallRules.noticeAfterAccept(VideoCallRules.cameraFailedNotice()),
        )
        assertEquals(null, VideoCallRules.noticeAfterAccept(null))
        assertEquals(null, VideoCallRules.noticeAfterCameraUnmute(VideoCallRules.cameraFailedNotice()))
        assertEquals(null, VideoCallRules.noticeAfterCameraUnmute(VideoCallRules.cameraDeniedNotice()))
        assertEquals(
            VideoCallRules.micDeniedNotice(),
            VideoCallRules.noticeAfterCameraUnmute(VideoCallRules.micDeniedNotice()),
        )
    }

    @Test
    fun videoOfferConstraintsReceiveBothDirections() {
        assertEquals("true", VideoCallRules.offerToReceiveAudio())
        assertEquals("true", VideoCallRules.offerToReceiveVideo(true))
        assertEquals("false", VideoCallRules.offerToReceiveVideo(false))
        assertTrue(VideoCallRules.answerReceivesVideo(true, "v=0\nm=audio 9 UDP/TLS/RTP/SAVPF 111\n"))
        assertTrue(
            VideoCallRules.answerReceivesVideo(
                false,
                "v=0\nm=audio 9 UDP/TLS/RTP/SAVPF 111\nm=video 9 UDP/TLS/RTP/SAVPF 96\n",
            ),
        )
        assertFalse(VideoCallRules.answerReceivesVideo(false, "v=0\nm=audio 9 UDP/TLS/RTP/SAVPF 111\n"))
        val offer = CallSignal(CallSignal.OFFER, sdp = representativeVideoSdp()).toJson()
        assertTrue(VideoCallRules.sdpHasVideo(CallSignal.parse(offer)!!.sdp))
        assertTrue(VideoCallRules.fitsWss(offer))
        assertFalse(VideoCallRules.sdpErrorFailsIce())
        assertTrue(VideoCallRules.cameraTrackAttachedWhenVideoCall(wantVideo = true, camMuted = false))
        assertFalse(VideoCallRules.cameraTrackAttachedWhenVideoCall(wantVideo = true, camMuted = true))
        assertFalse(VideoCallRules.cameraTrackAttachedWhenVideoCall(wantVideo = false, camMuted = false))
        assertTrue(
            VideoCallRules.mountCallRenderer(
                video = true,
                phase = CallPhase.ACTIVE,
                media = "через сервер",
                rtcReady = true,
            ),
        )
        assertFalse(
            VideoCallRules.mountCallRenderer(
                video = true,
                phase = CallPhase.ACTIVE,
                media = "через сервер",
                rtcReady = false,
            ),
        )
        assertFalse(
            VideoCallRules.showIceCompact(
                CallLinkState.CONNECTED,
                "через сервер",
            ),
        )
        assertTrue(
            VideoCallRules.showIceCompact(
                CallLinkState.CONNECTING,
                "WebRTC · ищем путь…",
            ),
        )
    }

    @Test
    fun remoteVideoRendererAndSdpRtcRules() {
        assertFalse(VideoCallRules.callOverlayUsesOffscreenLayer())
        assertFalse(VideoCallRules.rendererSurfaceReady(0, 0))
        assertFalse(VideoCallRules.rendererSurfaceReady(1080, 0))
        assertFalse(VideoCallRules.rendererSurfaceReady(0, 1920))
        assertTrue(VideoCallRules.rendererSurfaceReady(1080, 1920))
        assertTrue(VideoCallRules.bindRemoteFromAddStream())
        val lf = "v=0\nm=audio 9 UDP/TLS/RTP/SAVPF 111\nm=video 9 UDP/TLS/RTP/SAVPF 96\n"
        val rtc = VideoCallRules.sdpForPeerConnection(lf)
        assertTrue(rtc.contains("\r\n"))
        assertTrue(rtc.endsWith("\r\n"))
        assertTrue(VideoCallRules.sdpHasVideo(rtc))
        assertEquals("", VideoCallRules.sdpForPeerConnection(""))
        val already = "v=0\r\nm=video 9 UDP/TLS/RTP/SAVPF 96\r\n"
        val normalized = VideoCallRules.sdpForPeerConnection(already)
        assertEquals("v=0\r\nm=video 9 UDP/TLS/RTP/SAVPF 96\r\n", normalized)
        val huge = "v=0\n" + "a=x:" + "a".repeat(20_000) + "\nm=video 9 UDP/TLS/RTP/SAVPF 96\n"
        val kept = VideoCallRules.sdpForPeerConnection(huge)
        assertTrue(kept.contains("m=video"))
        assertTrue(kept.contains("a".repeat(20_000)))
        assertTrue(kept.length >= huge.length)
        val json = CallSignal(CallSignal.OFFER, sdp = already).toJson()
        val parsed = CallSignal.parse(json)!!
        assertTrue(VideoCallRules.sdpHasVideo(parsed.sdp))
        val applied = VideoCallRules.sdpForPeerConnection(parsed.sdp)
        assertTrue(applied.contains("\r\n"))
        assertTrue(applied.contains("m=video"))
    }

    @Test
    fun oneWayVideoNoticeAfterFourSeconds() {
        assertEquals("нет видео пира", VideoCallRules.oneWayRemoteNotice())
        assertEquals(4_000L, VideoCallRules.ONE_WAY_VIDEO_MS)
        assertNull(
            VideoCallRules.oneWayVideoNotice(
                video = true,
                mediaUp = true,
                wssFallback = false,
                remoteVideoBound = false,
                connectedForMs = 3_999L,
            ),
        )
        assertEquals(
            VideoCallRules.oneWayRemoteNotice(),
            VideoCallRules.oneWayVideoNotice(
                video = true,
                mediaUp = true,
                wssFallback = false,
                remoteVideoBound = false,
                connectedForMs = 4_000L,
            ),
        )
        assertNull(
            VideoCallRules.oneWayVideoNotice(
                video = true,
                mediaUp = true,
                wssFallback = true,
                remoteVideoBound = false,
                connectedForMs = 4_000L,
            ),
        )
        assertNull(
            VideoCallRules.oneWayVideoNotice(
                video = true,
                mediaUp = true,
                wssFallback = false,
                remoteVideoBound = true,
                connectedForMs = 4_000L,
            ),
        )
        assertNull(
            VideoCallRules.oneWayVideoNotice(
                video = false,
                mediaUp = true,
                wssFallback = false,
                remoteVideoBound = false,
                connectedForMs = 4_000L,
            ),
        )
        assertEquals(
            VideoCallRules.cameraFailedNotice(),
            VideoCallRules.keepExistingOverlayNotice(
                VideoCallRules.cameraFailedNotice(),
                VideoCallRules.oneWayRemoteNotice(),
            ),
        )
        assertEquals(
            VideoCallRules.oneWayRemoteNotice(),
            VideoCallRules.keepExistingOverlayNotice(null, VideoCallRules.oneWayRemoteNotice()),
        )
        assertTrue(VideoCallRules.flipCameraWhileSending(camMuted = false))
        assertFalse(VideoCallRules.flipCameraWhileSending(camMuted = true))
        assertTrue(VideoCallRules.activityKeepsSurfacesOnRotate())
        assertTrue(VideoCallRules.localPreviewMirrored(frontFacing = true))
        assertFalse(VideoCallRules.localPreviewMirrored(frontFacing = false))
        assertEquals(64, VideoCallRules.ICE_PER_SESSION_CAP)
        assertTrue(VideoCallRules.acceptIce(0))
        assertTrue(VideoCallRules.acceptIce(63))
        assertFalse(VideoCallRules.acceptIce(64))
        assertFalse(VideoCallRules.acceptIce(-1))
    }

    @Test
    fun wireEventIsHangupRejectsControlBeforeTrim() {
        assertTrue(VideoCallRules.wireEventIsHangup(CallSignal.HANGUP))
        assertTrue(VideoCallRules.wireEventIsHangup("bye"))
        assertFalse(VideoCallRules.wireEventIsHangup("hangup\n"))
        assertFalse(VideoCallRules.wireEventIsHangup("bye\r"))
        assertFalse(VideoCallRules.wireEventIsHangup("hangup\u0000x"))
        assertFalse(VideoCallRules.wireEventIsHangup(CallSignal.ICE))
    }

    @Test
    fun iceFieldRejectsControlBeforeTrim() {
        assertEquals("vps.example", VideoCallRules.iceField("vps.example"))
        assertEquals("u", VideoCallRules.iceField(" u "))
        assertNull(VideoCallRules.iceField(null))
        assertNull(VideoCallRules.iceField(""))
        assertNull(VideoCallRules.iceField("   "))
        assertNull(VideoCallRules.iceField("vps.example\n"))
        assertNull(VideoCallRules.iceField("u\r"))
        assertNull(VideoCallRules.iceField("cred\u0000x"))
        assertNull(VideoCallRules.iceField("vps.example\r\nX: y"))
    }

    /**
     * Compact VP8+opus offer similar to Unified Plan. Real device SDP is larger
     * (ICE, fingerprint, more fmtp) but still typically 2–6 KiB — under 16 KiB.
     */
    private fun representativeVideoSdp(): String = """
        v=0
        o=- 0 0 IN IP4 127.0.0.1
        s=-
        t=0 0
        a=group:BUNDLE 0 1
        a=ice-ufrag:ufrag
        a=ice-pwd:passwordpasswordpassword
        a=fingerprint:sha-256 ${"AB".repeat(32)}
        a=setup:actpass
        m=audio 9 UDP/TLS/RTP/SAVPF 111
        c=IN IP4 0.0.0.0
        a=rtcp-mux
        a=sendrecv
        a=mid:0
        a=rtpmap:111 opus/48000/2
        a=fmtp:111 minptime=10;useinbandfec=1
        m=video 9 UDP/TLS/RTP/SAVPF 96 97
        c=IN IP4 0.0.0.0
        a=rtcp-mux
        a=sendrecv
        a=mid:1
        a=rtpmap:96 VP8/90000
        a=rtcp-fb:96 goog-remb
        a=rtcp-fb:96 nack
        a=rtcp-fb:96 nack pli
        a=rtpmap:97 rtx/90000
        a=fmtp:97 apt=96
    """.trimIndent()
}
