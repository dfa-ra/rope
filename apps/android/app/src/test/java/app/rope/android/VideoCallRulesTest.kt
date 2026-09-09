package app.rope.android

import app.rope.android.data.CallMediaStart
import app.rope.android.data.CallSignal
import app.rope.android.data.SavedMessagesRules
import app.rope.android.data.VideoCallRules
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
