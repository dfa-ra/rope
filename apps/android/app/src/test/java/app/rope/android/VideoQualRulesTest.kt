package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.VideoQualRules
import app.rope.android.data.VideoRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoQualRulesTest {
    @Test
    fun inheritStoreAndDefaults() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertEquals(VideoQualRules.COMPRESSED, VideoQualRules.normalize(null))
        assertEquals(VideoQualRules.COMPRESSED, VideoQualRules.normalize("  "))
        assertEquals(VideoQualRules.COMPRESSED, VideoQualRules.normalize("raw"))
        assertEquals(VideoQualRules.HD, VideoQualRules.normalize("hd"))
        assertEquals(VideoQualRules.ORIGINAL, VideoQualRules.normalize("original"))
        assertEquals("Качество видео", VideoQualRules.TITLE)
        assertEquals(3, VideoQualRules.OPTIONS.size)
    }

    @Test
    fun skipAndHeights() {
        assertTrue(VideoQualRules.skipTranscode(VideoQualRules.COMPRESSED, 2_000_000, "video/mp4", "a.mp4"))
        assertFalse(VideoQualRules.skipTranscode(VideoQualRules.COMPRESSED, 1_000_000, "video/quicktime", "a.mov"))
        assertTrue(VideoQualRules.skipTranscode(VideoQualRules.ORIGINAL, 2_000_000, "video/quicktime", "a.mov"))
        assertFalse(
            VideoQualRules.skipTranscode(
                VideoQualRules.ORIGINAL,
                VideoRules.MAX_OBJECT_BYTES.toLong() + 1,
                "video/mp4",
                "a.mp4",
            ),
        )
        assertEquals(listOf(720, 480, 360), VideoQualRules.heights(VideoQualRules.COMPRESSED))
        assertEquals(listOf(1080, 720, 480), VideoQualRules.heights(VideoQualRules.HD))
        assertTrue(VideoQualRules.keepOriginalContainer(VideoQualRules.ORIGINAL))
        assertFalse(VideoQualRules.keepOriginalContainer(VideoQualRules.HD))
        assertTrue(VideoQualRules.hint().contains("оригинал", ignoreCase = true))
        assertFalse(VideoQualRules.hint().contains('\n'))
    }
}
