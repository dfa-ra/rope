package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.PhotoZoomRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoZoomRulesTest {
    @Test
    fun clampScaleAndDoubleTap() {
        assertEquals(1f, PhotoZoomRules.clampScale(0.2f), 0.0001f)
        assertEquals(4f, PhotoZoomRules.clampScale(9f), 0.0001f)
        assertEquals(2.5f, PhotoZoomRules.clampScale(2.5f), 0.0001f)
        assertEquals(2.5f, PhotoZoomRules.doubleTapScale(1f), 0.0001f)
        assertEquals(1f, PhotoZoomRules.doubleTapScale(2.5f), 0.0001f)
        assertFalse(PhotoZoomRules.isZoomed(1f))
        assertTrue(PhotoZoomRules.isZoomed(2.5f))
        assertTrue(PhotoZoomRules.canPageSwipe(1f))
        assertFalse(PhotoZoomRules.canPageSwipe(2.5f))
    }

    @Test
    fun offsetZeroAtFitAndClampedWhenZoomed() {
        assertEquals(0f to 0f, PhotoZoomRules.clampOffset(40f, 10f, 1f, 100f, 200f))
        val mid = PhotoZoomRules.clampOffset(10f, -10f, 2f, 100f, 200f)
        assertEquals(10f, mid.first, 0.0001f)
        assertEquals(-10f, mid.second, 0.0001f)
        val edge = PhotoZoomRules.clampOffset(999f, -999f, 2f, 100f, 200f)
        assertEquals(50f, edge.first, 0.0001f)
        assertEquals(-100f, edge.second, 0.0001f)
        assertEquals(0f to 0f, PhotoZoomRules.clampOffset(5f, 5f, 2f, 0f, 200f))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }
}
