package app.rope.android

import app.rope.android.data.ChatActions
import app.rope.android.data.ChatMessage
import app.rope.android.data.CopyCaptionRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyCaptionRulesTest {
    @Test
    fun captionInExtraEnablesCopyWhenTextBlank() {
        val photo = media(MessageKind.IMAGE, "image", caption = "рассвет")
        assertTrue(CopyCaptionRules.hasCaption(photo))
        assertEquals("рассвет", CopyCaptionRules.caption(photo))
        assertEquals("рассвет", CopyCaptionRules.clip(photo))
        assertTrue(ChatActions.canCopy(photo))
        assertFalse(ChatActions.canCopy(photo.copy(deleted = true)))
        assertNull(CopyCaptionRules.clip(photo.copy(deleted = true)))
    }

    @Test
    fun videoCaptionCopiesFromPayloadNotPlaceholderText() {
        val video = media(MessageKind.VIDEO, "video", caption = "клип", text = "Видео")
        assertEquals("клип", CopyCaptionRules.clip(video))
        assertTrue(ChatActions.canCopy(video))
        val bare = media(MessageKind.VIDEO, "video", caption = null, text = "")
        assertFalse(CopyCaptionRules.hasCaption(bare))
        assertFalse(ChatActions.canCopy(bare))
        assertNull(CopyCaptionRules.clip(bare))
    }

    @Test
    fun blankAndWhitespaceCaptionsDoNotCopy() {
        assertFalse(CopyCaptionRules.hasCaption(media(MessageKind.IMAGE, "image", caption = "  ")))
        assertFalse(CopyCaptionRules.hasCaption(media(MessageKind.IMAGE, "image", caption = null)))
        assertFalse(ChatActions.canCopy(media(MessageKind.IMAGE, "image", caption = null, text = "")))
        assertFalse(CopyCaptionRules.isPhotoOrVideo(text("hi")))
        assertNull(CopyCaptionRules.caption(text("hi")))
        assertEquals("hi", CopyCaptionRules.clip(text("hi")))
        assertTrue(ChatActions.canCopy(text("hi")))
        assertFalse(ChatActions.canCopy(text("hi").copy(deleted = true)))
    }

    @Test
    fun laterAlbumMemberWithoutCaptionIsNotCopyable() {
        val first = media(MessageKind.IMAGE, "image", caption = "альбом", index = 0, count = 2)
        val later = media(MessageKind.VIDEO, "video", caption = null, index = 1, count = 2, text = "")
        assertEquals("альбом", CopyCaptionRules.clip(first))
        assertTrue(ChatActions.canCopy(first))
        assertFalse(ChatActions.canCopy(later))
        assertNull(CopyCaptionRules.caption(later))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }

    private fun text(body: String) = ChatMessage(
        id = "t",
        peerDeviceId = "p",
        outgoing = false,
        text = body,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
    )

    private fun media(
        kind: MessageKind,
        payloadKind: String,
        caption: String?,
        text: String = "",
        index: Int = 0,
        count: Int = 1,
    ) = ChatMessage(
        id = "m$index",
        peerDeviceId = "p",
        outgoing = true,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = index.toLong(),
        kind = kind,
        extra = MediaPayload(
            kind = payloadKind,
            objectId = "o$index",
            sha256 = "ab",
            keyB64 = "KEY",
            mime = if (payloadKind == "video") "video/mp4" else "image/jpeg",
            name = "m",
            size = 1,
            albumId = if (count > 1) "alb" else null,
            albumIndex = index,
            albumCount = count,
            caption = caption,
        ).toJson(),
    )
}
