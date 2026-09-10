package app.rope.android

import app.rope.android.data.DirectoryDevice
import app.rope.android.data.RevokeRules
import app.rope.android.data.RoleRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RevokeRulesTest {
    private fun device(
        id: String,
        member: String,
        name: String,
        role: String = "member",
    ) = DirectoryDevice(id, member, name, ByteArray(0), "", false, role)

    @Test
    fun guestsNeverSeeRevoke() {
        val target = device("dev-b", "mem-b", "Боб")
        for (role in listOf("guest", "member", "GUEST", null, "")) {
            assertFalse(role.toString(), RevokeRules.canRevoke(role))
            assertFalse(role.toString(), RoleRules.canRevoke(role))
            assertFalse(
                role.toString(),
                RevokeRules.canRevokeTarget(role, "mem-a", "dev-a", target),
            )
        }
    }

    @Test
    fun ownerCanRevokeMemberNotSelfNotOtherOwner() {
        val bob = device("dev-b", "mem-b", "Боб")
        val self = device("dev-a", "mem-a", "Анна", "owner")
        val otherOwner = device("dev-c", "mem-c", "Кира", "owner")
        assertTrue(RevokeRules.canRevoke("owner"))
        assertTrue(RevokeRules.canRevoke("OWNER"))
        assertTrue(RoleRules.canRevoke("owner"))
        assertTrue(RevokeRules.canRevokeTarget("owner", "mem-a", "dev-a", bob))
        assertFalse(RevokeRules.canRevokeTarget("owner", "mem-a", "dev-a", self))
        assertFalse(RevokeRules.canRevokeTarget("owner", "mem-a", "dev-a", otherOwner))
        assertFalse(RevokeRules.canRevokeTarget("owner", "mem-b", "dev-a", bob))
        assertFalse(RevokeRules.canRevokeTarget("owner", "mem-a", "dev-a", bob.copy(memberId = "")))
    }

    @Test
    fun lastOwnerDoesNotExposeRevokeDevice() {
        val self = device("dev-a", "mem-a", "Анна", "owner")
        val spare = device("dev-a2", "mem-a", "Анна", "owner")
        val otherOwner = device("dev-c", "mem-c", "Кира", "owner")
        val bob = device("dev-b", "mem-b", "Боб")
        assertFalse(RevokeRules.canRevokeDevice("owner", "dev-a", self, ownerDeviceCount = 1))
        assertFalse(RevokeRules.canRevokeDevice("owner", "dev-a", otherOwner, ownerDeviceCount = 1))
        assertFalse(RevokeRules.canRevokeDevice("guest", "dev-a", bob, ownerDeviceCount = 1))
        assertTrue(RevokeRules.canRevokeDevice("owner", "dev-a", bob, ownerDeviceCount = 1))
        assertTrue(RevokeRules.canRevokeDevice("owner", "dev-a", otherOwner, ownerDeviceCount = 2))
        assertTrue(RevokeRules.canRevokeDevice("owner", "dev-a", spare, ownerDeviceCount = 2))
        assertFalse(RevokeRules.canRevokeDevice("owner", "dev-a", otherOwner, ownerDeviceCount = 1))
        assertFalse(RevokeRules.canRevokeDevice("owner", "dev-a", bob.copy(deviceId = ""), ownerDeviceCount = 1))
    }

    @Test
    fun copyIsShortAndHasNoFcmOrCrypto() {
        assertEquals("Исключить", RevokeRules.actionLabel())
        assertEquals("Точно исключить", RevokeRules.confirmAction())
        assertEquals("Отмена", RevokeRules.cancelAction())
        assertEquals("Исключить Боб с сервера?", RevokeRules.confirmPrompt("Боб"))
        assertEquals("Исключить этого человека с сервера?", RevokeRules.confirmPrompt("  "))
        assertEquals("Боб исключён", RevokeRules.noticeRevoked("Боб"))
        val lines = listOf(
            RevokeRules.confirmBody(),
            RevokeRules.peopleHint(),
            RevokeRules.settingsHint(),
            RevokeRules.confirmPrompt("Боб"),
            RevokeRules.noticeRevoked("Боб"),
        )
        for (s in lines) {
            assertFalse(s, s.contains("FCM", ignoreCase = true))
            assertFalse(s, s.contains("шифр", ignoreCase = true))
            assertFalse(s, s.contains("WebRTC"))
            assertFalse(s, s.contains('\n'))
            assertTrue(s, s.isNotBlank())
            assertTrue(s, s.length <= 80)
        }
        assertTrue(RevokeRules.settingsHint().contains("Люди"))
        assertTrue(RevokeRules.peopleHint().contains("сервер"))
    }

    @Test
    fun crlfNameFallsBackBeforeTrim() {
        assertEquals(
            "Исключить этого человека с сервера?",
            RevokeRules.confirmPrompt("Боб\nAdmin"),
        )
        assertEquals("человек исключён", RevokeRules.noticeRevoked("Боб\r"))
        assertEquals("человек исключён", RevokeRules.noticeRevoked("Боб\u0000x"))
        assertEquals(
            "Исключить этого человека с сервера?",
            RevokeRules.confirmPrompt("Боб\n"),
        )
        assertFalse(RevokeRules.confirmPrompt("a\nb").contains('\n'))
        assertEquals("Исключить Боб с сервера?", RevokeRules.confirmPrompt("  Боб  "))
    }
}
