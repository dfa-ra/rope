package app.rope.android

import app.rope.android.data.AlbumRules
import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatThreadItem
import app.rope.android.data.ComposerHintKind
import app.rope.android.data.ComposerRules
import app.rope.android.data.MediaPayload
import app.rope.android.data.MediaSendRules
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.VideoRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSendRulesTest {
    @Test
    fun captionRoundtripOmitsBlank() {
        val bare = MediaPayload("image", "o", "ab", "KEY", "image/jpeg", "p.jpg", 1)
        assertFalse(bare.toJson().contains("caption"))
        val with = bare.copy(caption = "привет")
        val parsed = MediaPayload.parse(with.toJson())
        assertEquals("привет", parsed.caption)
        assertTrue(with.toJson().contains("\"caption\":\"привет\""))
        assertEquals("привет", parsed.preview())
        assertEquals("Фото", bare.preview())
        assertNull(MediaPayload.parse(bare.toJson()).caption)
    }

    @Test
    fun captionOnFirstAlbumMemberOnly() {
        assertEquals("hi", MediaSendRules.onFirstOnly(0, " hi "))
        assertNull(MediaSendRules.onFirstOnly(1, "hi"))
        assertNull(MediaSendRules.normalize("  \n"))
        assertEquals("я".repeat(MediaSendRules.CAPTION_MAX), MediaSendRules.normalize("я".repeat(MediaSendRules.CAPTION_MAX + 8)))
    }

    @Test
    fun videosAreAlbumEligibleAndCollapseWithPhotos() {
        assertTrue(VideoRules.albumEligible("video"))
        assertTrue(VideoRules.albumEligible("image"))
        assertFalse(VideoRules.albumEligible("voice"))
        val photo = member("a", "image", 0, 2)
        val video = member("b", "video", 1, 2)
        assertEquals("alb", AlbumRules.albumId(video))
        val collapsed = AlbumRules.collapse(
            listOf(ChatThreadItem.Bubble(photo), ChatThreadItem.Bubble(video)),
        )
        assertEquals(1, collapsed.size)
        val album = collapsed[0] as ChatThreadItem.Album
        assertEquals(listOf("a", "b"), album.members.map { it.id })
        assertEquals("Альбом · 2", AlbumRules.preview(2, videoCount = 1))
        assertEquals("Альбом · 2 видео", AlbumRules.preview(2, videoCount = 2))
        val videoPayload = MediaPayload(
            kind = "video",
            objectId = "o",
            sha256 = "ab",
            keyB64 = "KEY",
            mime = "video/mp4",
            name = "c.mp4",
            size = 1,
            durationMs = 3400,
            albumId = "alb",
            albumIndex = 1,
            albumCount = 2,
        )
        val got = MediaPayload.parse(videoPayload.toJson())
        assertEquals("alb", got.albumId)
        assertEquals(1, got.albumIndex)
        assertTrue(videoPayload.toJson().contains("album_id"))
    }

    @Test
    fun composerSendShowsForPendingMediaAndCaptionHint() {
        assertTrue(ComposerRules.showSendButton("", false, pendingMedia = true))
        assertFalse(ComposerRules.showSendButton("", false, pendingMedia = false))
        assertEquals(3, ComposerRules.PHOTO_TAPS_TO_SEND)
        val hint = MediaSendRules.hint(3, videos = 1)
        assertEquals(ComposerHintKind.MEDIA, hint.kind)
        assertEquals("Альбом · 3", hint.title)
        assertEquals(MediaSendRules.DISMISS, hint.dismissContentDescription)
        assertEquals("Подпись", MediaSendRules.PLACEHOLDER)
        val first = member("a", "image", 0, 2, caption = "шутка")
        val second = member("b", "video", 1, 2)
        assertEquals("шутка", MediaSendRules.albumCaption(listOf(first, second)))
        assertEquals("шутка", first.let { MediaPayload.parse(it.extra).preview() })
    }

    private fun member(
        id: String,
        kind: String,
        index: Int,
        count: Int,
        caption: String? = null,
    ) = ChatMessage(
        id = id,
        peerDeviceId = "p",
        outgoing = false,
        text = "x",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = index.toLong(),
        kind = if (kind == "video") MessageKind.VIDEO else MessageKind.IMAGE,
        extra = MediaPayload(
            kind = kind,
            objectId = "o-$index",
            sha256 = "ab",
            keyB64 = "KEY",
            mime = if (kind == "video") "video/mp4" else "image/jpeg",
            name = "m",
            size = 1,
            albumId = "alb",
            albumIndex = index,
            albumCount = count,
            caption = caption,
        ).toJson(),
    )
}
