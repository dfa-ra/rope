package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.NewGroupWithRules
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewGroupWithRulesTest {
    @Test
    fun profileCreateGroupSkipsSelfAndSaved() {
        assertFalse(NewGroupWithRules.canStart(null, "me"))
        assertFalse(NewGroupWithRules.canStart("", "me"))
        assertFalse(NewGroupWithRules.canStart("me", "me"))
        assertFalse(NewGroupWithRules.canStart(SavedMessagesRules.ID, "me"))
        assertTrue(NewGroupWithRules.canStart("aaa111", "me"))
        assertEquals(setOf("aaa111"), NewGroupWithRules.picks("aaa111"))
        assertEquals("Создать группу", NewGroupWithRules.ACTION)
        assertEquals(6, LocalStore.VERSION)
    }
}
