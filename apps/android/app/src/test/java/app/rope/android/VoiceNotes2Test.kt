package app.rope.android

import app.rope.android.data.ChatActions
import app.rope.android.data.ChatMessage
import app.rope.android.data.EnvelopeTypes
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.VideoNoteRules
import app.rope.android.data.VideoRules
import app.rope.android.data.VoicePlayback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceNotes2Test {
    @Test
    fun speedCyclesTelegramOrder() {
        assertEquals(1.0f, VoicePlayback.clampSpeed(0.5f))
        assertEquals(1.5f, VoicePlayback.clampSpeed(1.4f), 0.0001f)
        assertEquals(2.0f, VoicePlayback.clampSpeed(2.4f), 0.0001f)
        assertEquals(1.5f, VoicePlayback.nextSpeed(1.0f), 0.0001f)
        assertEquals(2.0f, VoicePlayback.nextSpeed(1.5f), 0.0001f)
        assertEquals(1.0f, VoicePlayback.nextSpeed(2.0f), 0.0001f)
        assertEquals("1x", VoicePlayback.speedLabel(1.0f))
        assertEquals("1.5x", VoicePlayback.speedLabel(1.5f))
        assertEquals("2x", VoicePlayback.speedLabel(2.0f))
        assertEquals(listOf(1.0f, 1.5f, 2.0f), VoicePlayback.SPEEDS)
    }

    @Test
    fun seekMapsFractionAndPointerX() {
        assertEquals(0L, VoicePlayback.seekMs(-1f, 10_000))
        assertEquals(5_000L, VoicePlayback.seekMs(0.5f, 10_000))
        assertEquals(10_000L, VoicePlayback.seekMs(2f, 10_000))
        assertEquals(0L, VoicePlayback.seekMs(0.5f, 0))
        assertEquals(2_500L, VoicePlayback.seekMsAt(25f, 100f, 10_000))
        assertEquals(0L, VoicePlayback.seekMsAt(10f, 0f, 10_000))
        assertEquals(-1, VoicePlayback.litBarIndex(0f, 22))
        assertEquals(-1, VoicePlayback.litBarIndex(0.5f, 0))
        assertEquals(10, VoicePlayback.litBarIndex(0.5f, 22))
        assertEquals(21, VoicePlayback.litBarIndex(1f, 22))
    }

    @Test
    fun liveAmplitudesBecomeStoredWaveform() {
        val bars = VoicePlayback.barsFromAmplitudes(listOf(10, 80, 40, 200, 30, 90))
        assertEquals(VoicePlayback.BARS, bars.size)
        assertTrue(bars.all { it in 0.18f..1f })
        val encoded = VoicePlayback.encodeWaveform(bars)
        assertEquals(VoicePlayback.BARS, encoded.size)
        assertTrue(encoded.all { it in 0..31 })
        val resolved = VoicePlayback.resolveBars(encoded, "msg-live")
        assertEquals(VoicePlayback.BARS, resolved.size)
        val fallback = VoicePlayback.resolveBars(emptyList(), "msg-old")
        assertEquals(VoicePlayback.waveform("msg-old"), fallback)
    }

    @Test
    fun voicePayloadRoundtripsWaveform() {
        val levels = List(VoicePlayback.BARS) { (it * 3) % 32 }
        val p = MediaPayload(
            "voice",
            "obj-1",
            "ab",
            "KEY",
            "audio/mp4",
            "v.m4a",
            1200,
            3400,
            waveform = levels,
        )
        val got = MediaPayload.parse(p.toJson())
        assertEquals(levels, got.waveform)
        assertTrue(p.toJson().contains("\"wf\""))
        val plain = MediaPayload("voice", "o", "h", "k", "a", "n", 1)
        assertFalse(plain.toJson().contains("\"wf\""))
    }

    @Test
    fun videoNoteIsOwnKindNotAlbum() {
        assertEquals("video_note", VideoNoteRules.KIND)
        assertEquals(60_000L, VideoNoteRules.MAX_MS)
        assertTrue(VideoNoteRules.fitsDuration(1_000))
        assertFalse(VideoNoteRules.fitsDuration(100))
        assertFalse(VideoNoteRules.fitsDuration(61_000))
        assertFalse(VideoRules.albumEligible(VideoNoteRules.KIND))
        assertFalse(VideoNoteRules.albumEligible(VideoNoteRules.KIND))
        val p = MediaPayload(VideoNoteRules.KIND, "obj", "ab", "KEY", "video/mp4", "n.mp4", 800, 2500)
        val got = MediaPayload.parse(p.toJson())
        assertEquals(VideoNoteRules.KIND, got.kind)
        assertEquals(MessageKind.VIDEO_NOTE, got.messageKind())
        assertEquals("Видеосообщение · 0:02", got.preview())
        assertEquals(
            MessageKind.VIDEO_NOTE,
            EnvelopeTypes.kindOf(EnvelopeTypes.MEDIA, p.toJson()),
        )
        val note = ChatMessage(
            id = "n1",
            peerDeviceId = "p",
            outgoing = true,
            text = got.preview(),
            status = MessageStatus.CREATED,
            timestampMs = 1L,
            kind = MessageKind.VIDEO_NOTE,
            extra = p.toJson(),
        )
        assertTrue(VideoNoteRules.isNote(note))
        assertFalse(ChatActions.canOpen(note))
        assertTrue(ChatActions.canForward(note))
        assertFalse(ChatActions.canEdit(note))
    }

    @Test
    fun cameraOrientationMatchesAndroidFormula() {
        assertEquals(90, VideoNoteRules.previewOrientation(true, 270, 0))
        assertEquals(90, VideoNoteRules.previewOrientation(false, 90, 0))
        assertEquals(90, VideoNoteRules.recordingHint(true, 270))
        assertEquals(90, VideoNoteRules.recordingHint(false, 90))
        assertEquals(1, VideoNoteRules.pickFrontCamera(3) { it == 1 })
        assertEquals(0, VideoNoteRules.pickFrontCamera(2) { false })
        assertEquals(-1, VideoNoteRules.pickFrontCamera(0) { true })
    }

    @Test
    fun backCancelsVideoNoteRecording() {
        val chat = UiState(screen = Screen.Chat, backStack = listOf(Screen.Chats, Screen.Chat))
        assertEquals(BackLayer.CancelRecording, BackStack.decide(chat.copy(recordingVideoNote = true)))
        assertEquals(BackLayer.CancelRecording, BackStack.decide(chat.copy(attachCameraOpen = true)))
        assertEquals(BackLayer.Pop, BackStack.decide(chat))
    }
}
