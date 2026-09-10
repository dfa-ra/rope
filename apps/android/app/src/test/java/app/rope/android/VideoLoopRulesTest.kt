package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.VideoLoopRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoLoopRulesTest {
    @Test
    fun inheritStoreAndLoopCycle() {
        assertEquals(6, LocalStore.VERSION)
        assertTrue(VideoLoopRules.toggle(false))
        assertFalse(VideoLoopRules.toggle(true))
        assertEquals("один раз", VideoLoopRules.label(false))
        assertEquals("повтор", VideoLoopRules.label(true))
        assertEquals("Зациклить", VideoLoopRules.contentDescription(false))
        assertEquals("Играть один раз", VideoLoopRules.contentDescription(true))
        assertFalse(VideoLoopRules.label(false).contains("FCM", ignoreCase = true))
    }
}
