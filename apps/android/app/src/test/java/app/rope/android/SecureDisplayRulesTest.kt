package app.rope.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureDisplayRulesTest {
    @Test
    fun lockProvisionAndJoinNotInvite() {
        assertTrue(SecureDisplayRules.lockRecents(Screen.Provision))
        assertTrue(SecureDisplayRules.lockRecents(Screen.Join))
        assertFalse(SecureDisplayRules.lockRecents(Screen.Invite))
        assertFalse(SecureDisplayRules.lockRecents(Screen.Home))
        assertFalse(SecureDisplayRules.lockRecents(Screen.Chat))
        assertFalse(SecureDisplayRules.lockRecents(Screen.Start))
        assertTrue(SecureDisplayRules.lockRecents(Screen.Chat, appLockEnabled = true))
        assertFalse(SecureDisplayRules.lockRecents(Screen.Chat, appLockEnabled = false))
    }
}
