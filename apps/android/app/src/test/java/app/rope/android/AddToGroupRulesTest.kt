package app.rope.android

import app.rope.android.data.AddToGroupRules
import app.rope.android.data.LocalStore
import app.rope.android.data.RopeGroup
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddToGroupRulesTest {
    private val mine = group("g1", "Команда", members = listOf("me", "bbb"), createdBy = "me")
    private val already = group("g2", "Уже там", members = listOf("me", "aaa111"), createdBy = "me")
    private val notOrg = group("g3", "Чужая", members = listOf("me", "x"), createdBy = "x")
    private val alpha = group("g4", "Альфа", members = listOf("me"), createdBy = "me")

    @Test
    fun eligibleAreManagedGroupsPeerIsNotIn() {
        assertFalse(AddToGroupRules.canShow(null, "me"))
        assertFalse(AddToGroupRules.canShow("me", "me"))
        assertFalse(AddToGroupRules.canShow(SavedMessagesRules.ID, "me"))
        assertTrue(AddToGroupRules.canShow("aaa111", "me"))
        assertFalse(AddToGroupRules.canAdd(already, "aaa111", "me", "guest"))
        assertFalse(AddToGroupRules.canAdd(notOrg, "aaa111", "me", "guest"))
        assertTrue(AddToGroupRules.canAdd(notOrg, "aaa111", "me", "owner"))
        assertEquals(
            listOf("Альфа", "Команда"),
            AddToGroupRules.eligible(listOf(mine, already, notOrg, alpha), "aaa111", "me", "guest")
                .map { it.name },
        )
        assertEquals("Точно в «Команда»", AddToGroupRules.confirm("Команда"))
        assertEquals("Добавлен в «Команда»", AddToGroupRules.notice("Команда"))
        assertEquals("Добавить в группу", AddToGroupRules.SECTION)
        assertEquals(6, LocalStore.VERSION)
    }

    private fun group(
        id: String,
        name: String,
        members: List<String>,
        createdBy: String,
    ) = RopeGroup(groupId = id, name = name, epoch = 1, members = members, createdBy = createdBy)
}
