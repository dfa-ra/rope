package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.VideoTapRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTapRulesTest {
    @Test
    fun localStoreStaysV6() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
    }

    @Test
    fun photoTapClosesVideoTapToggles() {
        assertTrue(VideoTapRules.tapCloses(isVideo = false))
        assertFalse(VideoTapRules.tapCloses(isVideo = true))
        assertTrue(VideoTapRules.tapToggles(isVideo = true))
        assertFalse(VideoTapRules.tapToggles(isVideo = false))
        assertEquals("Смотреть", VideoTapRules.PLAY)
        assertEquals("Пауза", VideoTapRules.PAUSE)
    }
}
