package app.rope.android

import app.rope.android.data.GroupsInCommonRules
import app.rope.android.data.LocalStore
import app.rope.android.data.RopeGroup
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupsInCommonRulesTest {
    private val team = group("g1", "Команда", members = listOf("me", "aaa111", "bbb"))
    private val other = group("g2", "Другая", members = listOf("me", "zzz"))
    private val alpha = group("g3", "Альфа", members = listOf("me", "aaa111"))

    @Test
    fun listsSharedMembershipSortedAndSkipsSelfSaved() {
        assertFalse(GroupsInCommonRules.canShow(null, "me"))
        assertFalse(GroupsInCommonRules.canShow("me", "me"))
        assertFalse(GroupsInCommonRules.canShow(SavedMessagesRules.ID, "me"))
        assertTrue(GroupsInCommonRules.canShow("aaa111", "me"))
        assertEquals(emptyList<RopeGroup>(), GroupsInCommonRules.of(listOf(team), "aaa111", null))
        assertEquals(emptyList<RopeGroup>(), GroupsInCommonRules.of(listOf(team), "zzz", "me"))
        assertEquals(
            listOf("Другая"),
            GroupsInCommonRules.of(listOf(team, other), "zzz", "me").map { it.name },
        )
        assertEquals(
            listOf("Альфа", "Команда"),
            GroupsInCommonRules.of(listOf(team, other, alpha), "aaa111", "me").map { it.name },
        )
        assertEquals("Общие группы", GroupsInCommonRules.sectionLabel(0))
        assertEquals("Общие группы · 2", GroupsInCommonRules.sectionLabel(2))
        assertEquals(6, LocalStore.VERSION)
    }

    private fun group(id: String, name: String, members: List<String>) = RopeGroup(
        groupId = id,
        name = name,
        epoch = 1,
        members = members,
    )
}
