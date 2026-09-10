package app.rope.android

import app.rope.android.data.CompactChatRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompactChatRulesTest {
    @Test
    fun inheritStoreAndDefaults() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertFalse(CompactChatRules.enabledFromKv(null))
        assertFalse(CompactChatRules.enabledFromKv("0"))
        assertTrue(CompactChatRules.enabledFromKv("1"))
        assertEquals("Компактный список", CompactChatRules.TITLE)
    }

    @Test
    fun density() {
        assertEquals(46, CompactChatRules.avatarDp(false))
        assertEquals(36, CompactChatRules.avatarDp(true))
        assertEquals(12, CompactChatRules.rowPadV(false))
        assertEquals(6, CompactChatRules.rowPadV(true))
        assertFalse(CompactChatRules.titleCompact(false))
        assertTrue(CompactChatRules.titleCompact(true))
        assertTrue(CompactChatRules.hint().contains("списк", ignoreCase = true))
        assertFalse(CompactChatRules.hint().contains('\n'))
    }
}
