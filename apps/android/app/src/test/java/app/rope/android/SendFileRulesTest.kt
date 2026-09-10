package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.SendFileRules
import app.rope.android.data.VideoRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SendFileRulesTest {
    @Test
    fun asFileForcesDocumentKind() {
        assertEquals("file", SendFileRules.kind("image/jpeg", "a.jpg", asFile = true))
        assertEquals("file", SendFileRules.kind("video/mp4", "clip.mp4", asFile = true))
        assertEquals("image", SendFileRules.kind("image/jpeg", "a.jpg", asFile = false))
        assertEquals("video", SendFileRules.kind("video/mp4", "clip.mp4", asFile = false))
        assertEquals("file", SendFileRules.kind("application/pdf", "doc.pdf", asFile = false))
        assertEquals(VideoRules.kind("image/png", "b.png"), SendFileRules.kind("image/png", "b.png", asFile = false))
    }

    @Test
    fun asFileSkipsAlbumAndVisualPrep() {
        assertTrue(SendFileRules.skipVisualPrep(true))
        assertFalse(SendFileRules.skipVisualPrep(false))
        assertFalse(SendFileRules.albumEligible("image", asFile = true))
        assertFalse(SendFileRules.albumEligible("video", asFile = true))
        assertTrue(SendFileRules.albumEligible("image", asFile = false))
        assertFalse(SendFileRules.albumEligible("file", asFile = false))
    }

    @Test
    fun composerCopyIsSendAsFile() {
        assertEquals("Как файл", SendFileRules.ACTION)
        assertEquals("file", SendFileRules.KIND)
        val one = SendFileRules.hint(1)
        assertEquals("Файл", one.title)
        assertEquals("без сжатия, как документ", one.body)
        assertEquals("Отменить вложение", one.dismissContentDescription)
        assertEquals("Файлы", SendFileRules.hint(3).title)
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }
}
