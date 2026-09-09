package app.rope.android

import app.rope.android.data.ReduceMotionRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReduceMotionRulesTest {
    @Test
    fun prefOrSystemReduces() {
        assertFalse(ReduceMotionRules.shouldReduce(system = false, pref = false))
        assertTrue(ReduceMotionRules.shouldReduce(system = true, pref = false))
        assertTrue(ReduceMotionRules.shouldReduce(system = false, pref = true))
        assertTrue(ReduceMotionRules.shouldReduce(system = true, pref = true))
    }

    @Test
    fun copyMentionsLogoColorsStay() {
        assertEquals("Меньше анимации", ReduceMotionRules.TITLE)
        assertTrue(ReduceMotionRules.hint().contains("логотипа"))
        assertFalse(ReduceMotionRules.hint().contains("FCM"))
    }
}
