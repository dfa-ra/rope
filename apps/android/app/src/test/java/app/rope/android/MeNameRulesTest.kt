package app.rope.android

import app.rope.android.data.MeNameRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeNameRulesTest {
    @Test
    fun parseRejectsControlBeforeTrim() {
        assertEquals("ada", MeNameRules.parse(" ada "))
        assertNull(MeNameRules.parse("ada\n"))
        assertNull(MeNameRules.parse("\rada"))
        assertNull(MeNameRules.parse("ada\u0000x"))
        assertNull(MeNameRules.parse("a"))
        assertNull(MeNameRules.parse("ada admin"))
        assertNull(MeNameRules.parse(""))
        assertNull(MeNameRules.parse(null))
    }

    @Test
    fun parseCountsRunesNotUtf16() {
        assertEquals("я".repeat(24), MeNameRules.parse("я".repeat(24)))
        assertNull(MeNameRules.parse("я".repeat(25)))
    }
}
