package app.rope.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginRulesTest {
    @Test
    fun acceptsNormalLogins() {
        assertTrue(LoginRules.isValid("ann"))
        assertTrue(LoginRules.isValid("Владик"))
        assertTrue(LoginRules.isValid("user_1"))
        assertTrue(LoginRules.isValid("a.b-c"))
    }

    @Test
    fun rejectsBadLogins() {
        assertFalse(LoginRules.isValid(""))
        assertFalse(LoginRules.isValid("a"))
        assertFalse(LoginRules.isValid("has space"))
        assertFalse(LoginRules.isValid("bad!"))
        assertFalse(LoginRules.isValid("a".repeat(25)))
    }
}
