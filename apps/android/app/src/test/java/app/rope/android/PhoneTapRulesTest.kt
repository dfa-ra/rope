package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.PhoneTapRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneTapRulesTest {
    @Test
    fun inheritStoreAndTelUri() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("tel:+79991234567", PhoneTapRules.telUri("+7 999 123-45-67"))
        assertEquals("tel:89991234567", PhoneTapRules.telUri("8 (999) 123-45-67"))
        assertNull(PhoneTapRules.telUri("123"))
        assertNull(PhoneTapRules.telUri("+7 999\n1234567"))
        assertNull(PhoneTapRules.telUri("+7999\u00001234567"))
    }

    @Test
    fun spansSkipOccupiedAndControl() {
        val text = "пиши +7 999 123-45-67 или https://example.com/a"
        val phones = PhoneTapRules.spans(text)
        assertEquals(1, phones.size)
        assertEquals("tel:+79991234567", phones.single().tel)
        assertTrue(phones.single().start >= 0)
        val blocked = PhoneTapRules.spans(text, listOf(phones.single().start until phones.single().endExclusive))
        assertTrue(blocked.isEmpty())
        assertTrue(PhoneTapRules.spans("нет номера").isEmpty())
        assertTrue(PhoneTapRules.spans("+7 999\r1234567").isEmpty())
    }
}
