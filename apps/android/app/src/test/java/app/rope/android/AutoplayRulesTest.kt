package app.rope.android

import app.rope.android.data.AutoplayRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoplayRulesTest {
    @Test
    fun defaultOnUntilExplicitlyOff() {
        assertTrue(AutoplayRules.stored(null))
        assertTrue(AutoplayRules.stored(""))
        assertTrue(AutoplayRules.stored("1"))
        assertFalse(AutoplayRules.stored("0"))
        assertEquals("1", AutoplayRules.write(true))
        assertEquals("0", AutoplayRules.write(false))
        assertEquals(AutoplayRules.KEY, "video_autoplay")
        assertEquals(AutoplayRules.LABEL, "Автовоспроизведение")
        assertTrue(AutoplayRules.HINT.contains("без звука"))
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun visibleFileStartsAndRegularVideoIsMuted() {
        assertFalse(AutoplayRules.startOnVisible(enabled = false, hasPath = true))
        assertFalse(AutoplayRules.startOnVisible(enabled = true, hasPath = false))
        assertTrue(AutoplayRules.startOnVisible(enabled = true, hasPath = true))
        assertTrue(AutoplayRules.startMuted(MessageKind.VIDEO))
        assertFalse(AutoplayRules.startMuted(MessageKind.VIDEO_NOTE))
        assertFalse(AutoplayRules.startMuted(MessageKind.IMAGE))
    }
}
