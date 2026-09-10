package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.NotifyPrioRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotifyPrioRulesTest {
    @Test
    fun highHeadsUpChannelRejectsCrLf() {
        assertEquals(NotifyPrioRules.DEFAULT, NotifyPrioRules.normalize(null))
        assertEquals(NotifyPrioRules.HIGH, NotifyPrioRules.normalize("high"))
        assertEquals(NotifyPrioRules.DEFAULT, NotifyPrioRules.normalize("nope"))
        assertEquals("rope-messages", NotifyPrioRules.channelId(null))
        assertEquals("rope-messages-high", NotifyPrioRules.channelId(NotifyPrioRules.HIGH))
        assertTrue(NotifyPrioRules.isHigh(NotifyPrioRules.HIGH))
        assertFalse(NotifyPrioRules.isHigh(NotifyPrioRules.DEFAULT))
        assertEquals(NotifyPrioRules.IMPORTANCE_HIGH, NotifyPrioRules.importance(NotifyPrioRules.HIGH))
        assertEquals(NotifyPrioRules.IMPORTANCE_DEFAULT, NotifyPrioRules.importance(null))
        assertEquals(NotifyPrioRules.PRIORITY_HIGH, NotifyPrioRules.compatPriority(NotifyPrioRules.HIGH))
        assertNull(NotifyPrioRules.sanitize("high\n"))
        assertEquals(NotifyPrioRules.DEFAULT, NotifyPrioRules.normalize("a\r1"))
        assertEquals(NotifyPrioRules.DEFAULT, NotifyPrioRules.normalize("high\u0000"))
        assertEquals("Всплывающие", NotifyPrioRules.TITLE)
        assertEquals("Всплывающие", NotifyPrioRules.label(NotifyPrioRules.HIGH))
        assertTrue(NotifyPrioRules.hint().contains("Баннер"))
        assertEquals(6, LocalStore.VERSION)
    }
}
