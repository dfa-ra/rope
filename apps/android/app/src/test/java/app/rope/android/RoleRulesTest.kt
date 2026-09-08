package app.rope.android

import app.rope.android.data.RoleRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleRulesTest {
    @Test
    fun guestCannotSeeInviteQrOrKernelUpdate() {
        for (role in listOf("guest", "member", "GUEST", null, "")) {
            assertFalse(role.toString(), RoleRules.isOwner(role))
            assertFalse(role.toString(), RoleRules.canInvite(role))
            assertFalse(role.toString(), RoleRules.canShowInviteQr(role))
            assertFalse(role.toString(), RoleRules.canUpgradeCore(role))
            assertFalse(role.toString(), RoleRules.canShowAdminCards(role))
            assertFalse(role.toString(), RoleRules.canEditServerSettings(role))
            assertFalse(role.toString(), RoleRules.canWipeOrReinstall(role))
            assertNull(role.toString(), RoleRules.peopleInviteAction(role))
            assertFalse(role.toString(), RoleRules.peopleEmptyHint(role).contains("QR", ignoreCase = true))
            assertFalse(role.toString(), RoleRules.chatsEmptyBody(role).contains("QR", ignoreCase = true))
            assertFalse(role.toString(), RoleRules.groupNoMembersHint(role).contains("QR", ignoreCase = true))
        }
    }

    @Test
    fun ownerSeesInviteQrAndKernelUpdate() {
        for (role in listOf("owner", "OWNER", "Owner")) {
            assertTrue(role, RoleRules.isOwner(role))
            assertTrue(role, RoleRules.canInvite(role))
            assertTrue(role, RoleRules.canShowInviteQr(role))
            assertTrue(role, RoleRules.canUpgradeCore(role))
            assertTrue(role, RoleRules.canShowAdminCards(role))
            assertTrue(role, RoleRules.canEditServerSettings(role))
            assertTrue(role, RoleRules.canWipeOrReinstall(role))
            assertEquals("Пригласить", RoleRules.peopleInviteAction(role))
            assertTrue(role, RoleRules.peopleEmptyHint(role).contains("QR"))
            assertTrue(role, RoleRules.chatsEmptyBody(role).contains("QR"))
        }
    }

    @Test
    fun groupManageIsOrganizerOrServerOwner() {
        assertTrue(RoleRules.canManageGroupMembers(true, "org", "org", "guest"))
        assertFalse(RoleRules.canManageGroupMembers(true, "mem", "org", "guest"))
        assertTrue(RoleRules.canManageGroupMembers(true, "mem", "org", "owner"))
        assertTrue(RoleRules.canLeaveGroup(true))
        assertFalse(RoleRules.canLeaveGroup(false))
    }

    @Test
    fun guestStillSeesStatusAndApkUpdate() {
        assertTrue(RoleRules.canOpenStatus("guest"))
        assertTrue(RoleRules.canOpenStatus("member"))
        assertTrue(RoleRules.canOpenStatus(null))
        assertTrue(RoleRules.canUpdateApp("guest"))
        assertTrue(RoleRules.canUpdateApp("member"))
        assertTrue(RoleRules.canUpdateApp("owner"))
        assertTrue(RoleRules.canOpenStatus("owner"))
    }
}
