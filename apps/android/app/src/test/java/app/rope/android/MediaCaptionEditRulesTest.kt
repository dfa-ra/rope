package app.rope.android

import app.rope.android.data.ChatActions
import app.rope.android.data.ChatMessage
import app.rope.android.data.ComposerRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MediaCaptionEditRules
import app.rope.android.data.MediaPayload
import app.rope.android.data.MediaSendRules
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaCaptionEditRulesTest {
    @Test
    fun outgoingPhotoAndVideoCanEditCaption() {
        val photo = media(MessageKind.IMAGE, outgoing = true)
        val video = media(MessageKind.VIDEO, outgoing = true, caption = "клип")
        assertTrue(ChatActions.canEdit(photo))
        assertTrue(ChatActions.canEdit(video))
        assertTrue(MediaCaptionEditRules.canEdit(photo))
        assertTrue(MediaCaptionEditRules.allowBlankCommit(photo))
        assertEquals("", MediaCaptionEditRules.draft(photo))
        assertEquals("клип", MediaCaptionEditRules.draft(video))
        assertFalse(ChatActions.canEdit(photo.copy(outgoing = false)))
        assertFalse(ChatActions.canEdit(photo.copy(deleted = true)))
    }

    @Test
    fun voiceFileAndVideoNoteStayUneditable() {
        assertFalse(ChatActions.canEdit(media(MessageKind.VOICE, outgoing = true)))
        assertFalse(ChatActions.canEdit(media(MessageKind.FILE, outgoing = true)))
        assertFalse(ChatActions.canEdit(media(MessageKind.VIDEO_NOTE, outgoing = true)))
        assertFalse(MediaCaptionEditRules.eligibleKind(media(MessageKind.VOICE, outgoing = true)))
    }

    @Test
    fun albumCaptionLivesOnFirstMember() {
        val first = media(MessageKind.IMAGE, outgoing = true, albumIndex = 0, albumCount = 2, caption = "альбом")
        val later = media(MessageKind.VIDEO, outgoing = true, albumIndex = 1, albumCount = 2)
        assertTrue(ChatActions.canEdit(first))
        assertFalse(ChatActions.canEdit(later))
        assertEquals("альбом", MediaCaptionEditRules.draft(first))
        assertEquals("", MediaCaptionEditRules.draft(later))
    }

    @Test
    fun applyRewritesCaptionKeepsObjectAndOmitsBlank() {
        val photo = media(MessageKind.IMAGE, outgoing = true, caption = "старое")
        val applied = MediaCaptionEditRules.apply(photo, "  новое  ")!!
        val parsed = MediaPayload.parse(applied.extra)
        assertEquals("новое", parsed.caption)
        assertEquals("новое", applied.preview)
        assertEquals("obj-1", parsed.objectId)
        assertTrue(applied.extra.contains("\"caption\":\"новое\""))
        val cleared = MediaCaptionEditRules.apply(photo, "  \n")!!
        val clearedPayload = MediaPayload.parse(cleared.extra)
        assertNull(clearedPayload.caption)
        assertFalse(cleared.extra.contains("caption"))
        assertEquals("Фото", cleared.preview)
        assertNull(MediaCaptionEditRules.apply(textMsg(), "новое"))
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun textEditDraftUnchangedAndBlankBlocked() {
        val mine = textMsg()
        assertTrue(ChatActions.canEdit(mine))
        assertEquals("hi", MediaCaptionEditRules.draft(mine))
        assertFalse(MediaCaptionEditRules.allowBlankCommit(mine))
        assertFalse(ComposerRules.showSendButton("", false))
        assertTrue(ComposerRules.showSendButton("", false, allowEmpty = true))
        assertTrue(ComposerRules.showSendButton("подпись", false, allowEmpty = true))
    }

    @Test
    fun captionNormalizeStillCapsAtSendLimit() {
        val long = "я".repeat(MediaSendRules.CAPTION_MAX + 8)
        val applied = MediaCaptionEditRules.apply(media(MessageKind.VIDEO, outgoing = true), long)!!
        assertEquals(MediaSendRules.CAPTION_MAX, MediaPayload.parse(applied.extra).caption!!.length)
    }

    private fun textMsg() = ChatMessage(
        id = "t1",
        peerDeviceId = "p",
        outgoing = true,
        text = "hi",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
    )

    private fun media(
        kind: MessageKind,
        outgoing: Boolean,
        caption: String? = null,
        albumIndex: Int = 0,
        albumCount: Int = 1,
        deleted: Boolean = false,
    ): ChatMessage {
        val payloadKind = when (kind) {
            MessageKind.VIDEO -> "video"
            MessageKind.VOICE -> "voice"
            MessageKind.VIDEO_NOTE -> "video_note"
            MessageKind.FILE -> "file"
            else -> "image"
        }
        val extra = MediaPayload(
            kind = payloadKind,
            objectId = "obj-1",
            sha256 = "ab",
            keyB64 = "KEY",
            mime = if (kind == MessageKind.VIDEO) "video/mp4" else "image/jpeg",
            name = "m",
            size = 1,
            albumId = if (albumCount > 1) "alb" else null,
            albumIndex = albumIndex,
            albumCount = albumCount,
            caption = caption,
        ).toJson()
        return ChatMessage(
            id = "m1",
            peerDeviceId = "p",
            outgoing = outgoing,
            text = MediaPayload.parse(extra).preview(),
            status = MessageStatus.DELIVERED_TO_DEVICE,
            timestampMs = 1L,
            kind = kind,
            extra = extra,
            deleted = deleted,
        )
    }
}
