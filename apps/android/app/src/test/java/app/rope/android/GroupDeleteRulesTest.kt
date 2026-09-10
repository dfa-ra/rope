package app.rope.android

import app.rope.android.data.GroupDeleteRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupDeleteRulesTest {
    @Test
    fun parseIdRejectsControlBeforeTrim() {
        val id = "11111111-1111-1111-1111-111111111111"
        assertEquals(id, GroupDeleteRules.parseId(id))
        assertNull(GroupDeleteRules.parseId(id + "\n"))
        assertNull(GroupDeleteRules.parseId("\r$id"))
        assertNull(GroupDeleteRules.parseId("$id\u0000"))
        assertNull(GroupDeleteRules.parseId(" $id"))
        assertNull(GroupDeleteRules.parseId("not-a-uuid"))
        assertNull(GroupDeleteRules.parseId(""))
        assertNull(GroupDeleteRules.parseId(null))
    }

    @Test
    fun deleteIsOrganizerOrServerOwner() {
        assertTrue(GroupDeleteRules.canDelete(true, "org", "org", "guest"))
        assertFalse(GroupDeleteRules.canDelete(true, "mem", "org", "guest"))
        assertTrue(GroupDeleteRules.canDelete(true, "mem", "org", "owner"))
        assertFalse(GroupDeleteRules.canDelete(false, "org", "org", "owner"))
    }
}
