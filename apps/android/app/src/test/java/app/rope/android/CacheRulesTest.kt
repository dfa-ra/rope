package app.rope.android

import app.rope.android.data.CacheRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class CacheRulesTest {
    @Test
    fun labelBuckets() {
        assertEquals("Пусто", CacheRules.label(0))
        assertEquals("Пусто", CacheRules.label(-1))
        assertEquals("512 Б", CacheRules.label(512))
        assertEquals("2 КБ", CacheRules.label(2048))
        assertEquals("1,5 МБ", CacheRules.label((1.5 * 1024 * 1024).toLong()))
        assertEquals("12 МБ", CacheRules.label(12L * 1024 * 1024))
    }

    @Test
    fun deleteOnlyMediaTree() {
        val root = createTempDirectory("rope-cache").toFile()
        try {
            val media = CacheRules.dir(root)
            File(media, "a.bin").apply { parentFile.mkdirs(); writeBytes(ByteArray(8)) }
            val identity = File(root, "identity.ropi.enc").apply { writeText("nope") }
            assertEquals(8L, CacheRules.bytesOf(media))
            assertTrue(CacheRules.deleteTree(media))
            assertEquals(0L, CacheRules.bytesOf(media))
            assertTrue(identity.isFile)
            assertFalse(media.exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
