package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.GroupMemberRules
import app.rope.android.data.LocalStore
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupMemberRulesTest {
    @Test
    fun canOpenOtherMembersNotSelf() {
        assertTrue(GroupMemberRules.canOpen("peer-1", "me"))
        assertTrue(GroupMemberRules.canOpen("  PEER-1  ", "me"))
        assertTrue(GroupMemberRules.canOpen("peer-1", null))
        assertFalse(GroupMemberRules.canOpen(null, "me"))
        assertFalse(GroupMemberRules.canOpen("", "me"))
        assertFalse(GroupMemberRules.canOpen("me", "me"))
        assertFalse(GroupMemberRules.canOpen("ME", "me"))
        assertFalse(GroupMemberRules.canOpen(SavedMessagesRules.ID, "me"))
        assertFalse(GroupMemberRules.canOpen(ChatIds.group("g-1"), "me"))
        assertEquals("Написать", GroupMemberRules.ACTION)
        assertEquals("Нет в справочнике", GroupMemberRules.NOTICE_MISSING)
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }
}
