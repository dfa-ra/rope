package app.rope.android

import app.rope.android.data.ClockRules
import app.rope.android.data.MessageTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockRulesTest {
    @Test
    fun twentyFourHourPads() {
        assertEquals("00:05", ClockRules.hm(0, 5, hour24 = true))
        assertEquals("09:05", ClockRules.hm(9, 5, hour24 = true))
        assertEquals("14:05", ClockRules.hm(14, 5, hour24 = true))
        assertEquals("12:00", ClockRules.hm(12, 0, hour24 = true))
    }

    @Test
    fun twelveHourDropsPadAndWraps() {
        assertEquals("12:05", ClockRules.hm(0, 5, hour24 = false))
        assertEquals("9:05", ClockRules.hm(9, 5, hour24 = false))
        assertEquals("12:00", ClockRules.hm(12, 0, hour24 = false))
        assertEquals("2:05", ClockRules.hm(14, 5, hour24 = false))
        assertEquals("11:59", ClockRules.hm(23, 59, hour24 = false))
    }

    @Test
    fun messageTimeUsesClockRules() {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 14)
            set(java.util.Calendar.MINUTE, 5)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        assertEquals("14:05", MessageTime.label(cal.timeInMillis, cal.timeInMillis, hour24 = true))
        assertEquals("2:05", MessageTime.label(cal.timeInMillis, cal.timeInMillis, hour24 = false))
    }

    @Test
    fun copyKeepsLogo() {
        assertEquals("24 часа", ClockRules.TITLE)
        assertTrue(ClockRules.hint().contains("логотипа"))
        assertFalse(ClockRules.hint().contains("FCM"))
    }
}
