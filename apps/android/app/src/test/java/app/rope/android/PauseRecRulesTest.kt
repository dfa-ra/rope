package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.PauseRecRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PauseRecRulesTest {
    @Test
    fun pauseOnlyWhenLockedRecording() {
        assertFalse(PauseRecRules.canPause(recording = true, locked = false))
        assertFalse(PauseRecRules.canPause(recording = false, locked = true))
        assertTrue(PauseRecRules.canPause(recording = true, locked = true))
        assertTrue(PauseRecRules.nextPaused(false))
        assertFalse(PauseRecRules.nextPaused(true))
        assertTrue(PauseRecRules.clockRuns(paused = false))
        assertFalse(PauseRecRules.clockRuns(paused = true))
        assertEquals("Пауза", PauseRecRules.PAUSE)
        assertEquals("Продолжить", PauseRecRules.RESUME)
        assertTrue(PauseRecRules.caption(true).contains("пауз"))
        assertEquals(6, LocalStore.VERSION)
    }
}
