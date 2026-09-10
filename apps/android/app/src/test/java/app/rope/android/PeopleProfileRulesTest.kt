package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.PeopleProfileRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeopleProfileRulesTest {
    @Test
    fun avatarOpensOthersNotSelf() {
        assertTrue(PeopleProfileRules.canOpen("aaa", "me"))
        assertTrue(PeopleProfileRules.canOpen("BBB", "aaa"))
        assertFalse(PeopleProfileRules.canOpen("AAA", "aaa"))
        assertFalse(PeopleProfileRules.canOpen("me", "me"))
        assertFalse(PeopleProfileRules.canOpen("ME", "me"))
        assertFalse(PeopleProfileRules.canOpen("", "me"))
        assertFalse(PeopleProfileRules.canOpen(null, "me"))
        assertEquals("Профиль", PeopleProfileRules.AVATAR)
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }
}
