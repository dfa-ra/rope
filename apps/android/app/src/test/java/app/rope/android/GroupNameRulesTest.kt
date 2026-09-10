package app.rope.android

import app.rope.android.data.GroupNameRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupNameRulesTest {
    @Test
    fun parseRejectsControlBeforeTrim() {
        assertEquals("crew", GroupNameRules.parse(" crew "))
        assertNull(GroupNameRules.parse("crew\n"))
        assertNull(GroupNameRules.parse("crew\radmin"))
        assertNull(GroupNameRules.parse("crew\u0000x"))
        assertNull(GroupNameRules.parse(""))
        assertNull(GroupNameRules.parse("   "))
        assertNull(GroupNameRules.parse(null))
    }

    @Test
    fun parseCountsRunesNotUtf16() {
        assertEquals("я".repeat(40), GroupNameRules.parse("я".repeat(40)))
        assertNull(GroupNameRules.parse("я".repeat(41)))
        assertEquals("🙂".repeat(40), GroupNameRules.parse("🙂".repeat(40)))
        assertNull(GroupNameRules.parse("🙂".repeat(41)))
    }
}
