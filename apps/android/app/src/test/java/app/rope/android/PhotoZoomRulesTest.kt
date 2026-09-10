package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.PhotoZoomRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoZoomRulesTest {
    @Test
    fun localStoreStaysV6() {
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun onlyImagesPinchZoom() {
        assertTrue(PhotoZoomRules.canZoom(MessageKind.IMAGE))
        assertFalse(PhotoZoomRules.canZoom(MessageKind.VIDEO))
        assertFalse(PhotoZoomRules.canZoom(MessageKind.VIDEO_NOTE))
        assertFalse(PhotoZoomRules.canZoom(MessageKind.FILE))
        assertFalse(PhotoZoomRules.canZoom(MessageKind.TEXT))
    }

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
        assertTrue(PhotoZoomRules.tapCloses(1f))
        assertFalse(PhotoZoomRules.tapCloses(2.5f))
    }

    @Test
    fun restKeepsPagerAndTapToClose() {
        assertFalse(PhotoZoomRules.shouldConsume(zoomed = false, pointerCount = 1, zoom = 1f))
        assertTrue(PhotoZoomRules.isTap(4f, maxPointers = 1, slopPx = PhotoZoomRules.TAP_SLOP_DP))
        assertFalse(PhotoZoomRules.isTap(40f, maxPointers = 1, slopPx = PhotoZoomRules.TAP_SLOP_DP))
        assertFalse(PhotoZoomRules.isTap(0f, maxPointers = 2, slopPx = PhotoZoomRules.TAP_SLOP_DP))
    }

    @Test
    fun pinchDisablesPagerAndConsumes() {
        assertTrue(PhotoZoomRules.shouldConsume(zoomed = true, pointerCount = 1, zoom = 1f))
        assertTrue(PhotoZoomRules.shouldConsume(zoomed = false, pointerCount = 2, zoom = 1f))
        assertTrue(PhotoZoomRules.shouldConsume(zoomed = false, pointerCount = 1, zoom = 1.2f))
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
}
