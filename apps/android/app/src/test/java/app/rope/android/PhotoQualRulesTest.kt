package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.PhotoQualRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoQualRulesTest {
    @Test
    fun inheritStoreAndDefaults() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertEquals(PhotoQualRules.COMPRESSED, PhotoQualRules.normalize(null))
        assertEquals(PhotoQualRules.COMPRESSED, PhotoQualRules.normalize("  "))
        assertEquals(PhotoQualRules.COMPRESSED, PhotoQualRules.normalize("raw"))
        assertEquals(PhotoQualRules.HD, PhotoQualRules.normalize("hd"))
        assertEquals(PhotoQualRules.ORIGINAL, PhotoQualRules.normalize("original"))
        assertEquals("Качество фото", PhotoQualRules.TITLE)
        assertEquals(3, PhotoQualRules.OPTIONS.size)
    }

    @Test
    fun compressAndEdges() {
        assertTrue(PhotoQualRules.compress(PhotoQualRules.COMPRESSED))
        assertTrue(PhotoQualRules.compress(PhotoQualRules.HD))
        assertFalse(PhotoQualRules.compress(PhotoQualRules.ORIGINAL))
        assertEquals(2048, PhotoQualRules.maxEdge(PhotoQualRules.COMPRESSED))
        assertEquals(2560, PhotoQualRules.maxEdge(PhotoQualRules.HD))
        assertEquals(85, PhotoQualRules.jpegQuality(PhotoQualRules.COMPRESSED))
        assertEquals(90, PhotoQualRules.jpegQuality(PhotoQualRules.HD))
        assertTrue(PhotoQualRules.hint().contains("оригинал", ignoreCase = true))
        assertFalse(PhotoQualRules.hint().contains('\n'))
    }
}
