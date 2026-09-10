package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.VideoLandRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoLandRulesTest {
    @Test
    fun localStoreStaysV6() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
    }

    @Test
    fun rotateOnlyWhenVideoIsReady() {
        assertTrue(VideoLandRules.showsRotate(isVideo = true, pathReady = true))
        assertFalse(VideoLandRules.showsRotate(isVideo = true, pathReady = false))
        assertFalse(VideoLandRules.showsRotate(isVideo = false, pathReady = true))
    }

    @Test
    fun landscapeFillsAndLocksSensorLandscape() {
        assertTrue(VideoLandRules.fillBleed(landscape = true, isVideo = true))
        assertFalse(VideoLandRules.fillBleed(landscape = false, isVideo = true))
        assertFalse(VideoLandRules.fillBleed(landscape = true, isVideo = false))
        assertEquals(6, VideoLandRules.activityOrientation(wantLandscape = true))
        assertEquals(-1, VideoLandRules.activityOrientation(wantLandscape = false))
        assertEquals(VideoLandRules.ROTATE, VideoLandRules.contentDescription(landscape = false))
        assertEquals(VideoLandRules.EXIT, VideoLandRules.contentDescription(landscape = true))
    }
}
