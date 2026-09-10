package app.rope.android

import app.rope.android.data.DirectoryDevice
import app.rope.android.data.PeerIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PeerIdsTest {
    private fun device(
        id: String,
        member: String = "mem-$id",
        online: Boolean = false,
        identity: ByteArray = byteArrayOf(1),
    ) = DirectoryDevice(id, member, id.take(4), identity, "", online)

    @Test
    fun normalizeAndSameIgnoreCase() {
        assertEquals("abc", PeerIds.normalize("ABC"))
        assertEquals("abc", PeerIds.normalize(" abc "))
        assertTrue(PeerIds.same("AbC", "abc"))
        assertFalse(PeerIds.same("abc", "abd"))
        assertFalse(PeerIds.same("", ""))
        assertTrue(PeerIds.looksLikeDevice("A".repeat(64)))
        assertFalse(PeerIds.looksLikeDevice("not-a-device"))
    }

    @Test
    fun newlinePrefixIsNotALiveDeviceId() {
        val hex = "a".repeat(64)
        assertEquals("", PeerIds.normalize("\n$hex"))
        assertEquals("", PeerIds.normalize("$hex\n"))
        assertEquals("", PeerIds.normalize("$hex\r"))
        assertEquals("", PeerIds.normalize("$hex\u0000"))
        assertFalse(PeerIds.same("\n$hex", hex))
        assertFalse(PeerIds.looksLikeDevice("\n$hex"))
        assertEquals("", PeerIds.wireId(null, "\n$hex"))
    }

    @Test
    fun resolvePrefersOnlineDeviceOfSameMember() {
        val offline = device("d".repeat(64), member = "member-1", online = false)
        val online = device("e".repeat(64), member = "member-1", online = true)
        val other = device("f".repeat(64), member = "member-2", online = true)
        val devices = listOf(offline, online, other)
        val hit = PeerIds.resolve(devices, hint = offline, rawId = "MEMBER-1", onlineIds = setOf(online.deviceId))
        assertEquals(online.deviceId, hit?.deviceId)
    }

    @Test
    fun resolveDoesNotTargetGroupOrWrongPeerFallback() {
        val alice = device("a".repeat(64), member = "ma")
        val bob = device("b".repeat(64), member = "mb")
        assertNull(PeerIds.resolve(listOf(alice, bob), hint = alice, rawId = "g:crew"))
        assertNull(PeerIds.resolve(listOf(alice), hint = alice, rawId = bob.deviceId))
        val leftover = DirectoryDevice(bob.deviceId.uppercase(), "", "Боб", ByteArray(0), "", false)
        assertEquals(bob.deviceId, PeerIds.resolve(listOf(alice, bob), leftover, leftover.deviceId)?.deviceId)
    }

    @Test
    fun wireIdNeverUsesADifferentOpenChat() {
        val bob = device("b".repeat(64))
        assertEquals(bob.deviceId, PeerIds.wireId(bob, "other"))
        assertEquals("c".repeat(64), PeerIds.wireId(null, "C".repeat(64)))
    }
}