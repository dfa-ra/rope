package app.rope.android

import app.rope.android.ui.SplashTiming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashTimingTest {
    @Test
    fun reduceMotionWhenAnimatorScaleIsZero() {
        assertTrue(SplashTiming.isReduceMotion(0f))
        assertTrue(SplashTiming.isReduceMotion(1f, 0f))
        assertFalse(SplashTiming.isReduceMotion(1f))
        assertFalse(SplashTiming.isReduceMotion(0.5f, 1f))
    }

    @Test
    fun coldStartWaitsForReadyThenMinimumPaint() {
        assertTrue(SplashTiming.shouldHoldColdStart(200, sessionReady = false, seenThisProcess = false))
        assertTrue(SplashTiming.shouldHoldColdStart(200, sessionReady = true, seenThisProcess = false))
        assertFalse(SplashTiming.shouldHoldColdStart(1_400, sessionReady = true, seenThisProcess = false))
        assertTrue(SplashTiming.shouldHoldColdStart(1_399, sessionReady = true, seenThisProcess = false))
    }

    @Test
    fun coldStartNeverBlocksForeverAndSkipsReplay() {
        assertFalse(
            SplashTiming.shouldHoldColdStart(
                SplashTiming.MAX_HOLD_MS,
                sessionReady = false,
                seenThisProcess = false,
            ),
        )
        assertFalse(SplashTiming.shouldHoldColdStart(0, sessionReady = false, seenThisProcess = true))
        assertFalse(SplashTiming.shouldHoldColdStart(0, sessionReady = true, seenThisProcess = true))
    }

    @Test
    fun reduceMotionSkipsMinimumHoldButStillCaps() {
        assertFalse(
            SplashTiming.shouldHoldColdStart(10, sessionReady = true, seenThisProcess = false, reduceMotion = true),
        )
        assertTrue(
            SplashTiming.shouldHoldColdStart(10, sessionReady = false, seenThisProcess = false, reduceMotion = true),
        )
        assertFalse(
            SplashTiming.shouldHoldColdStart(
                SplashTiming.REDUCE_MOTION_MAX_MS,
                sessionReady = false,
                seenThisProcess = false,
                reduceMotion = true,
            ),
        )
    }

    @Test
    fun longLoadAppearsAfterThresholdAndYieldsToErrorsOrCalls() {
        assertFalse(SplashTiming.shouldShowLongLoad(100, busy = true, hasError = false, callActive = false))
        assertTrue(
            SplashTiming.shouldShowLongLoad(
                SplashTiming.LONG_LOAD_AFTER_MS,
                busy = true,
                hasError = false,
                callActive = false,
            ),
        )
        assertFalse(
            SplashTiming.shouldShowLongLoad(
                2_000,
                busy = true,
                hasError = true,
                callActive = false,
            ),
        )
        assertFalse(
            SplashTiming.shouldShowLongLoad(
                2_000,
                busy = true,
                hasError = false,
                callActive = true,
            ),
        )
        assertFalse(
            SplashTiming.shouldShowLongLoad(
                2_000,
                busy = true,
                hasError = false,
                callActive = false,
                reduceMotion = true,
            ),
        )
        assertFalse(SplashTiming.shouldShowLongLoad(2_000, busy = false, hasError = false, callActive = false))
    }

    @Test
    fun captionsAndStaggerAndPaintEase() {
        assertEquals("Устанавливаем сервер…", SplashTiming.busyCaption(Screen.Provision, true))
        assertEquals("Входим…", SplashTiming.busyCaption(Screen.Join, false))
        assertEquals("Обновление…", SplashTiming.busyCaption(Screen.Status, true))
        assertEquals("Подключаемся…", SplashTiming.busyCaption(Screen.Home, false))
        assertEquals("Подождите…", SplashTiming.busyCaption(Screen.Chats, true))
        assertEquals(0, SplashTiming.staggerDelayMs(0))
        assertEquals(72, SplashTiming.staggerDelayMs(2))
        assertEquals(SplashTiming.STAGGER_CAP_MS, SplashTiming.staggerDelayMs(40))
        assertEquals(0f, SplashTiming.paintProgress(0))
        assertEquals(1f, SplashTiming.paintProgress(SplashTiming.PAINT_MS))
        assertEquals(1f, SplashTiming.paintProgress(10, reduceMotion = true))
        val mid = SplashTiming.paintProgress(800)
        assertTrue(mid > 0.5f && mid < 1f)
    }
}
