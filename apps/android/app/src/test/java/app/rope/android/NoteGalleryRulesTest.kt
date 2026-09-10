package app.rope.android

import app.rope.android.data.ComposerHintKind
import app.rope.android.data.LocalStore
import app.rope.android.data.MediaPayload
import app.rope.android.data.MediaSendRules
import app.rope.android.data.MessageKind
import app.rope.android.data.NoteGalleryRules
import app.rope.android.data.VideoNoteRules
import app.rope.android.data.VideoRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteGalleryRulesTest {
    @Test
    fun inheritStoreAndVersion() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertEquals("Как видеосообщение", NoteGalleryRules.ACTION)
        assertEquals("Видеосообщение", NoteGalleryRules.TITLE)
        assertFalse(NoteGalleryRules.ACTION.contains("FCM", ignoreCase = true))
        assertFalse(NoteGalleryRules.TOO_LONG.contains("FCM", ignoreCase = true))
    }

    @Test
    fun singleShortGalleryVideoCanBeANote() {
        assertTrue(NoteGalleryRules.shows(1, 1, 1_000))
        assertTrue(NoteGalleryRules.shows(1, 1, 0))
        assertTrue(NoteGalleryRules.shows(1, 1, VideoNoteRules.MAX_MS))
        assertFalse(NoteGalleryRules.shows(1, 1, 100))
        assertFalse(NoteGalleryRules.shows(1, 1, 61_000))
        assertFalse(NoteGalleryRules.shows(2, 2, 1_000))
        assertFalse(NoteGalleryRules.shows(1, 0, 1_000))
        assertFalse(NoteGalleryRules.shows(0, 1, 1_000))
        assertTrue(NoteGalleryRules.sendAsNote(true, 1, 1, 12_000))
        assertFalse(NoteGalleryRules.sendAsNote(false, 1, 1, 12_000))
        assertFalse(NoteGalleryRules.sendAsNote(true, 2, 1, 12_000))
        assertTrue(NoteGalleryRules.keepToggle(true, 1))
        assertFalse(NoteGalleryRules.keepToggle(true, 2))
        assertFalse(NoteGalleryRules.keepToggle(false, 1))
    }

    @Test
    fun noteKindDropsCaptionAndAlbum() {
        assertEquals(VideoNoteRules.KIND, NoteGalleryRules.kind(true))
        assertEquals("video", NoteGalleryRules.kind(false))
        assertNull(NoteGalleryRules.caption(true, "подпись"))
        assertEquals("hi", NoteGalleryRules.caption(false, "hi"))
        assertFalse(VideoRules.albumEligible(NoteGalleryRules.kind(true)))
        assertTrue(VideoRules.albumEligible(NoteGalleryRules.kind(false)))
        val p = MediaPayload(
            NoteGalleryRules.kind(true),
            "obj",
            "ab",
            "KEY",
            "video/mp4",
            "clip.mp4",
            800,
            2_500,
        )
        val got = MediaPayload.parse(p.toJson())
        assertEquals(VideoNoteRules.KIND, got.kind)
        assertEquals(MessageKind.VIDEO_NOTE, got.messageKind())
        assertEquals("Видеосообщение · 0:02", got.preview())
        assertFalse(got.toJson().contains("album_id"))
        assertFalse(got.toJson().contains("caption"))
    }

    @Test
    fun composerHintSwitchesToRoundNote() {
        val note = NoteGalleryRules.hint(true, 1, 1)
        assertEquals(ComposerHintKind.MEDIA, note.kind)
        assertEquals("Видеосообщение", note.title)
        assertEquals("кружок из галереи", note.body)
        assertEquals(MediaSendRules.DISMISS, note.dismissContentDescription)
        val video = NoteGalleryRules.hint(false, 1, 1)
        assertEquals("Видео", video.title)
        val album = NoteGalleryRules.hint(false, 2, 1)
        assertEquals("Альбом · 2", album.title)
        assertEquals("до 60 с для видеосообщения", NoteGalleryRules.TOO_LONG)
    }
}
