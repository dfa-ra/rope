package app.rope.android

import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
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
    }

    @Test
    fun optionalChatLockHidesThreadNotInvite() {
        assertTrue(SecureDisplayRules.lockRecents(Screen.Chat, hideChats = true))
        assertTrue(SecureDisplayRules.lockRecents(Screen.PeerProfile, hideChats = true))
        assertTrue(SecureDisplayRules.lockRecents(Screen.GroupInfo, hideChats = true))
        assertFalse(SecureDisplayRules.lockRecents(Screen.Invite, hideChats = true))
        assertFalse(SecureDisplayRules.lockRecents(Screen.Chats, hideChats = true))
        assertTrue(SecureDisplayRules.lockRecents(Screen.Join, hideChats = true))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }
}
