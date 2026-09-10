package app.rope.android

import app.rope.android.data.FullDateRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageStatus
import app.rope.android.data.MessageTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullDateRulesTest {
    @Test
    fun tapExpandsTodayClockToFullDate() {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, 2026)
            set(java.util.Calendar.MONTH, java.util.Calendar.SEPTEMBER)
            set(java.util.Calendar.DAY_OF_MONTH, 10)
            set(java.util.Calendar.HOUR_OF_DAY, 14)
            set(java.util.Calendar.MINUTE, 5)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val ms = cal.timeInMillis
        assertEquals("14:05", MessageTime.label(ms, ms))
        assertEquals("10.09.2026 14:05", FullDateRules.label(ms))
        assertEquals(
            "14:05 · доставлено",
            FullDateRules.meta(MessageStatus.DELIVERED_TO_DEVICE, true, ms, now = ms, full = false),
        )
        assertEquals(
            "10.09.2026 14:05 · изм. · доставлено",
            FullDateRules.meta(MessageStatus.DELIVERED_TO_DEVICE, true, ms, edited = true, now = ms, full = true),
        )
        assertEquals("", FullDateRules.label(0L))
        assertFalse(FullDateRules.label(ms).contains('\n'))
        assertTrue(FullDateRules.label(ms).startsWith("10.09.2026"))
        assertEquals(6, LocalStore.VERSION)
    }
}
