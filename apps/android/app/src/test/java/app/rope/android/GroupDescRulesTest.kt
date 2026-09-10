package app.rope.android

import app.rope.android.data.GroupDescRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupDescRulesTest {
    @Test
    fun parseRejectsControlBeforeTrim() {
        assertEquals("crew notes", GroupDescRules.parse("  crew notes  "))
        assertEquals("", GroupDescRules.parse(""))
        assertEquals("", GroupDescRules.parse("   "))
        assertNull(GroupDescRules.parse("crew\nnotes"))
        assertNull(GroupDescRules.parse("\rcrew"))
        assertNull(GroupDescRules.parse("crew\u0000"))
        assertNull(GroupDescRules.parse(null))
        assertEquals("я".repeat(120), GroupDescRules.parse("я".repeat(120)))
        assertNull(GroupDescRules.parse("я".repeat(121)))
    }

    @Test
    fun editIsOrganizerOrServerOwner() {
        assertTrue(GroupDescRules.canEdit(true, "org", "org", "guest"))
        assertFalse(GroupDescRules.canEdit(true, "mem", "org", "guest"))
        assertTrue(GroupDescRules.canEdit(true, "mem", "org", "owner"))
        assertFalse(GroupDescRules.canEdit(false, "org", "org", "owner"))
    }
}
