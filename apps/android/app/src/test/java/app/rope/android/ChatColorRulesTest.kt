package app.rope.android

import app.rope.android.data.ChatColorRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatColorRulesTest {
    @Test
    fun localStoreStaysV6() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
    }

    @Test
    fun defaultIsTodaysGraphite() {
        assertEquals(ChatColorRules.DEFAULT, ChatColorRules.parse(null))
        assertEquals(ChatColorRules.DEFAULT, ChatColorRules.parse("nope"))
        assertEquals("blue", ChatColorRules.parse("BLUE"))
        assertEquals(0xFF3F3F46, ChatColorRules.outArgb(ChatColorRules.DEFAULT, dark = true))
        assertEquals(0xFF3F3F46, ChatColorRules.outArgb(ChatColorRules.DEFAULT, dark = false))
        assertEquals(0xFF1D4ED8, ChatColorRules.outArgb("blue", dark = true))
        assertEquals(0xFFFFFFFF, ChatColorRules.outFgArgb("blue", dark = true))
        assertEquals(0xFFF4F4F5, ChatColorRules.outFgArgb(ChatColorRules.DEFAULT, dark = true))
        assertEquals(5, ChatColorRules.OPTIONS.size)
        assertEquals("Графит", ChatColorRules.label(ChatColorRules.DEFAULT))
        assertTrue(ChatColorRules.hint().contains("Исходящие"))
    }
}
