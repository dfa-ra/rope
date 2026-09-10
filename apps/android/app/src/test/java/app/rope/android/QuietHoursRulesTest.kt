package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.QuietHoursRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietHoursRulesTest {
    @Test
    fun overnightWindowSilencesShadeNotCalls() {
        assertEquals(QuietHoursRules.OFF, QuietHoursRules.normalize(null))
        assertFalse(QuietHoursRules.active(QuietHoursRules.OFF, 23))
        assertTrue(QuietHoursRules.active("22-8", 22))
        assertTrue(QuietHoursRules.active("22-8", 3))
        assertFalse(QuietHoursRules.active("22-8", 8))
        assertFalse(QuietHoursRules.active("22-8", 12))
        assertTrue(QuietHoursRules.active("0-6", 0))
        assertTrue(QuietHoursRules.active("0-6", 5))
        assertFalse(QuietHoursRules.active("0-6", 6))
        assertTrue(QuietHoursRules.active("23-7", 23))
        assertFalse(QuietHoursRules.active("23-7", 7))
        assertNull(QuietHoursRules.sanitize("22-8\n"))
        assertEquals(QuietHoursRules.OFF, QuietHoursRules.normalize("a\r1"))
        assertEquals("Не беспокоить", QuietHoursRules.TITLE)
        assertEquals("22:00–08:00", QuietHoursRules.label("22-8"))
        assertTrue(QuietHoursRules.hint().contains("Звонки"))
        assertEquals(6, LocalStore.VERSION)
    }
}
