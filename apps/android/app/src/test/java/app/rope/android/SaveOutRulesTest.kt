package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.SaveOutRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SaveOutRulesTest {
    @Test
    fun defaultOffAndOnlyPhotosVideos() {
        assertFalse(SaveOutRules.enabled(null))
        assertFalse(SaveOutRules.enabled("0"))
        assertFalse(SaveOutRules.enabled(""))
        assertTrue(SaveOutRules.enabled("1"))
        assertEquals("1", SaveOutRules.storeValue(true))
        assertEquals("0", SaveOutRules.storeValue(false))

        assertFalse(SaveOutRules.shouldSave(enabled = false, kind = "image"))
        assertTrue(SaveOutRules.shouldSave(enabled = true, kind = "image"))
        assertTrue(SaveOutRules.shouldSave(enabled = true, kind = "video"))
        assertFalse(SaveOutRules.shouldSave(enabled = true, kind = "voice"))
        assertFalse(SaveOutRules.shouldSave(enabled = true, kind = "file"))
        assertFalse(SaveOutRules.shouldSave(enabled = true, kind = "video_note"))

        assertFalse(SaveOutRules.isVideo("image"))
        assertTrue(SaveOutRules.isVideo("video"))
        assertEquals("Pictures/Rope/", SaveOutRules.relativePath(false))
        assertEquals("Movies/Rope/", SaveOutRules.relativePath(true))
        assertEquals("Сохранять исходящие в галерею", SaveOutRules.LABEL)
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun mimeAndPathStayInsideMediaDir() {
        assertEquals("image/jpeg", SaveOutRules.sanitizedMime("image/jpeg"))
        assertEquals(null, SaveOutRules.sanitizedMime("image/jpeg\n"))
        assertEquals(null, SaveOutRules.sanitizedMime("no-slash"))

        val root = File(System.getProperty("java.io.tmpdir"), "rope-save-out-media")
        root.mkdirs()
        val ok = File(root, "obj.jpg")
        ok.writeBytes(ByteArray(16) { 1 })
        assertTrue(SaveOutRules.allow(ok, root))
        assertEquals("obj.jpg", SaveOutRules.displayName(ok))
        assertEquals("image/png", SaveOutRules.mimeFor("image", "image/png", ok))

        val outside = File(System.getProperty("java.io.tmpdir"), "rope-save-out-outside.jpg")
        outside.writeBytes(ByteArray(16) { 1 })
        assertFalse(SaveOutRules.allow(outside, root))

        val tiny = File(root, "tiny.jpg")
        tiny.writeBytes(ByteArray(4))
        assertFalse(SaveOutRules.allow(tiny, root))

        ok.delete()
        tiny.delete()
        outside.delete()
        root.delete()
    }
}
