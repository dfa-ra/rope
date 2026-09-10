package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.QuoteCopyRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteCopyRulesTest {
    @Test
    fun inheritStoreAndClipboard() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("Копировать цитату", QuoteCopyRules.LABEL)
        assertEquals("привет", QuoteCopyRules.clipboard("  привет  "))
        assertEquals("строка\nдве", QuoteCopyRules.clipboard("строка\nдве"))
        assertNull(QuoteCopyRules.clipboard("   "))
        assertNull(QuoteCopyRules.clipboard("ok\u0000no"))
        assertTrue(QuoteCopyRules.enabled("цитата"))
        assertFalse(QuoteCopyRules.enabled("\u0000"))
    }
}
