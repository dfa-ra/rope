package app.rope.android

import app.rope.android.data.ChatActions
import app.rope.android.data.ChatMessage
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.VideoRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoRulesTest {
    @Test
    fun galleryVideoIsKindVideoNotFile() {
        assertEquals("video", VideoRules.kind("video/mp4", "clip.mp4"))
        assertEquals("video", VideoRules.kind("application/octet-stream", "story.mov"))
        assertEquals("image", VideoRules.kind("image/jpeg", "a.jpg"))
        assertEquals("file", VideoRules.kind("application/pdf", "doc.pdf"))
        assertTrue(VideoRules.looksLikeVideo("clip.mp4", "video/mp4"))
        assertFalse(VideoRules.looksLikeVideo("a.jpg", "image/jpeg"))
        assertTrue(VideoRules.albumEligible("image"))
        assertFalse(VideoRules.albumEligible("video"))
    }

    @Test
    fun objectCapAndCompressGate() {
        assertEquals(25 * 1024 * 1024, VideoRules.MAX_OBJECT_BYTES)
        assertTrue(VideoRules.fitsCap(1_000))
        assertFalse(VideoRules.fitsCap(0))
        assertFalse(VideoRules.fitsCap(VideoRules.MAX_OBJECT_BYTES.toLong() + 1))
        assertFalse(VideoRules.mustCompress(2_000_000, "video/mp4", "a.mp4"))
        assertTrue(VideoRules.mustCompress(VideoRules.MAX_OBJECT_BYTES.toLong() + 1, "video/mp4", "a.mp4"))
        assertTrue(VideoRules.mustCompress(1_000_000, "video/quicktime", "a.mov"))
        assertEquals("mp4", VideoRules.extension("video/mp4", "a.mov"))
    }

    @Test
    fun payloadRoundtripKindVideo() {
        val p = MediaPayload("video", "obj-1", "ab", "KEY", "video/mp4", "c.mp4", 1200, 3400)
        val got = MediaPayload.parse(p.toJson())
        assertEquals("video", got.kind)
        assertEquals(MessageKind.VIDEO, got.messageKind())
        assertEquals("Видео · 0:03", got.preview())
        assertEquals(3400, got.durationMs)
        assertFalse(got.toJson().contains("album_id"))
        val clip = ChatMessage(
            id = "v1",
            peerDeviceId = "p",
            outgoing = true,
            text = got.preview(),
            status = MessageStatus.CREATED,
            timestampMs = 1L,
            kind = MessageKind.VIDEO,
            extra = p.toJson(),
        )
        assertTrue(ChatActions.canOpen(clip))
        assertFalse(ChatActions.canOpen(clip.copy(deleted = true)))
        assertFalse(ChatActions.canOpen(clip.copy(kind = MessageKind.FILE)))
        assertEquals("Видео", VideoRules.preview(0))
        assertTrue(VideoRules.videoBitrateBps(10_000) in 250_000..2_500_000)
    }
}
