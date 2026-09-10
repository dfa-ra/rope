package app.rope.android

import app.rope.android.data.GroupDeleteRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupDeleteRulesTest {
    @Test
    fun organizerOrOwnerCanDeleteNotLeave() {
        assertEquals("Удалить группу", GroupDeleteRules.LABEL)
        assertEquals("Точно удалить группу", GroupDeleteRules.CONFIRM)
        assertEquals("Отмена", GroupDeleteRules.CANCEL)
        assertEquals("Группа удалена", GroupDeleteRules.NOTICE)
        assertTrue(GroupDeleteRules.canDelete(true, "org", "org", "member"))
        assertTrue(GroupDeleteRules.canDelete(true, "me", "org", "owner"))
        assertFalse(GroupDeleteRules.canDelete(true, "me", "org", "member"))
        assertFalse(GroupDeleteRules.canDelete(false, "org", "org", "owner"))
        assertEquals("«Чат» удалена", GroupDeleteRules.notice("Чат"))
        assertEquals(GroupDeleteRules.NOTICE, GroupDeleteRules.notice("bad\nname"))
        assertFalse(GroupDeleteRules.LABEL.contains("Выйти"))
        assertFalse(GroupDeleteRules.LABEL.contains("FCM", ignoreCase = true))
        assertEquals("0.3.52", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(76, BuildConfig.VERSION_CODE)
        assertEquals(6, LocalStore.VERSION)
    }
}
