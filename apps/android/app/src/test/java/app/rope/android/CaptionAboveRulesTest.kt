package app.rope.android

import app.rope.android.data.CaptionAboveRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionAboveRulesTest {
    @Test
    fun inheritStoreAndLayout() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("Подпись сверху", CaptionAboveRules.LABEL)
        assertTrue(CaptionAboveRules.showAbove(true))
        assertFalse(CaptionAboveRules.showBelow(true))
        assertFalse(CaptionAboveRules.showAbove(false))
        assertTrue(CaptionAboveRules.showBelow(false))
        assertTrue(CaptionAboveRules.hint().contains("Telegram"))
        assertFalse(CaptionAboveRules.hint().contains("FCM"))
        assertTrue(CaptionAboveRules.hint().contains("сервер") || CaptionAboveRules.hint().contains("не уходит"))
    }
}
