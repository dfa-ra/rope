package app.rope.android

import app.rope.android.data.InviteTtlRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteTtlRulesTest {
    @Test
    fun chipsMatchRelayCapAndVersion() {
        assertEquals("Срок ссылки", InviteTtlRules.TITLE)
        assertEquals(3600, InviteTtlRules.DEFAULT)
        assertEquals(24 * 3600, InviteTtlRules.MAX)
        assertEquals(listOf(3600, 8 * 3600, 24 * 3600), InviteTtlRules.CHIPS.map { it.seconds })
        assertEquals(listOf("1 час", "8 часов", "1 сутки"), InviteTtlRules.CHIPS.map { it.label })
        InviteTtlRules.CHIPS.forEach { chip ->
            assertTrue(chip.seconds in 1..InviteTtlRules.MAX)
            assertFalse(chip.label.contains('\n'))
        }
        assertEquals(3600, InviteTtlRules.clamp(3600))
        assertEquals(InviteTtlRules.DEFAULT, InviteTtlRules.clamp(7))
        assertEquals(InviteTtlRules.DEFAULT, InviteTtlRules.clamp(7 * 24 * 3600))
        assertEquals(InviteTtlRules.DEFAULT, InviteTtlRules.parse("a\n1"))
        assertEquals(8 * 3600, InviteTtlRules.parse(" 28800 "))
        assertEquals("1 сутки", InviteTtlRules.label(InviteTtlRules.MAX))
        assertTrue(InviteTtlRules.enabled(busy = false))
        assertFalse(InviteTtlRules.enabled(busy = true))
        assertFalse(InviteTtlRules.TITLE.contains("FCM", ignoreCase = true))
        assertEquals("0.3.52", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(76, BuildConfig.VERSION_CODE)
        assertEquals(6, LocalStore.VERSION)
    }
}
