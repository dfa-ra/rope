package app.rope.android

import app.rope.android.data.AlbumRules
import app.rope.android.data.LocalStore
import app.rope.android.data.PasteImgRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PasteImgRulesTest {
    @Test
    fun inheritStoreAndRejectControlChars() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertEquals("Вставить фото", PasteImgRules.LABEL)
        assertFalse(PasteImgRules.LABEL.contains("FCM", ignoreCase = true))
        assertNull(PasteImgRules.sanitizeUri("content://ok\nbad"))
        assertNull(PasteImgRules.sanitizeUri("content://ok\rbad"))
        assertNull(PasteImgRules.sanitizeUri("content://ok\u0000bad"))
        assertNull(PasteImgRules.sanitizeUri("https://evil.example/x.png"))
        assertNull(PasteImgRules.sanitizeUri("javascript:alert(1)"))
        assertNull(PasteImgRules.sanitizeUri(""))
        assertNull(PasteImgRules.sanitizeUri("  "))
        assertNull(PasteImgRules.sanitizeUri("content://" + "a".repeat(PasteImgRules.MAX_URI)))
        assertEquals(
            "content://media/external/images/media/7",
            PasteImgRules.sanitizeUri("  content://media/external/images/media/7  "),
        )
        assertEquals("file:///data/x.png", PasteImgRules.sanitizeUri("file:///data/x.png"))
    }

    @Test
    fun onlyImagesWhileComposerIsIdle() {
        assertTrue(PasteImgRules.acceptMime("image/png"))
        assertTrue(PasteImgRules.acceptMime("IMAGE/JPEG"))
        assertTrue(PasteImgRules.acceptMime("image/*"))
        assertFalse(PasteImgRules.acceptMime("text/plain"))
        assertFalse(PasteImgRules.acceptMime("video/mp4"))
        assertFalse(PasteImgRules.acceptMime(null))
        assertTrue(PasteImgRules.canPaste(editing = false, recording = false))
        assertFalse(PasteImgRules.canPaste(editing = true, recording = false))
        assertFalse(PasteImgRules.canPaste(editing = false, recording = true))
    }

    @Test
    fun clipImageUrisStageAndTextOnlyDoesNot() {
        val shot = PasteImgRules.Piece("content://shots/1", "image/png")
        val file = PasteImgRules.Piece("file:///cache/a.jpg", null)
        val note = PasteImgRules.Piece(uri = null, mime = "text/plain", text = "hi")
        assertEquals(
            listOf("content://shots/1"),
            PasteImgRules.intercept(false, false, listOf(shot, note), true),
        )
        assertEquals(
            listOf("file:///cache/a.jpg"),
            PasteImgRules.uris(listOf(file), descriptionHasImage = false),
        )
        assertTrue(PasteImgRules.intercept(false, false, listOf(note), false).isEmpty())
        assertTrue(
            PasteImgRules.intercept(true, false, listOf(shot), true).isEmpty(),
        )
        assertTrue(PasteImgRules.descriptionHasImage(listOf("text/plain", "image/png")))
        assertFalse(PasteImgRules.descriptionHasImage(listOf("text/plain")))
        val many = (0 until AlbumRules.MAX_PHOTOS + 3).map {
            PasteImgRules.Piece("content://img/$it", "image/jpeg")
        }
        assertEquals(AlbumRules.MAX_PHOTOS, PasteImgRules.uris(many, true).size)
    }
}
