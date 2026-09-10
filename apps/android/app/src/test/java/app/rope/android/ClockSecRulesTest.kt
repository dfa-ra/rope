package app.rope.android

import app.rope.android.data.ClockSecRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageStatus
import app.rope.android.data.MessageTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockSecRulesTest {
    @Test
    fun inheritStoreAndVersion() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertEquals("Секунды", ClockSecRules.TITLE)
        assertEquals("Время", ClockSecRules.SECTION)
        assertFalse(ClockSecRules.TITLE.contains("FCM", ignoreCase = true))
        assertTrue(ClockSecRules.hint().contains("логотипа"))
    }

    @Test
    fun secondsAppendHhMmSs() {
        assertEquals("14:05", ClockSecRules.hm(14, 5, 32, seconds = false))
        assertEquals("14:05:32", ClockSecRules.hm(14, 5, 32, seconds = true))
        assertEquals("00:00:00", ClockSecRules.hm(0, 0, 0, seconds = true))
        assertEquals("09:05:07", ClockSecRules.hm(9, 5, 7, seconds = true))
        assertEquals("23:59:59", ClockSecRules.hm(23, 59, 59, seconds = true))
        assertEquals("00:05", ClockSecRules.hm(0, 5, 9, seconds = false))
    }

    @Test
    fun messageTimeUsesSecondsFlag() {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 14)
            set(java.util.Calendar.MINUTE, 5)
            set(java.util.Calendar.SECOND, 32)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val ms = cal.timeInMillis
        assertEquals("14:05", MessageTime.label(ms, ms, seconds = false))
        assertEquals("14:05:32", MessageTime.label(ms, ms, seconds = true))
        assertEquals(
            "14:05:32 · доставлено",
            MessageTime.meta(MessageStatus.DELIVERED_TO_DEVICE, true, ms, now = ms, seconds = true),
        )
        assertEquals(
            "14:05 · доставлено",
            MessageTime.meta(MessageStatus.DELIVERED_TO_DEVICE, true, ms, now = ms, seconds = false),
        )
        assertEquals("", MessageTime.label(0L, seconds = true))
    }
}
