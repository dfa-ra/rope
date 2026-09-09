package app.rope.android

import app.rope.android.net.PinnedClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.cert.X509Certificate

class TlsPinTest {
    @Test
    fun blankAndShortPinFailsClosed() {
        assertFalse(PinnedClient.tlsPinAllows("abc", ""))
        assertFalse(PinnedClient.tlsPinAllows("abc", "  "))
        assertFalse(PinnedClient.tlsPinAllows("", ""))
        assertFalse(PinnedClient.sha256HexPin(""))
        assertFalse(PinnedClient.sha256HexPin("deadbeef"))
        assertFalse(PinnedClient.tlsPinAllows("aa", "aa"))
    }

    @Test
    fun configuredPinFailsClosedOnMismatch() {
        val a = "a".repeat(64)
        val b = "b".repeat(64)
        val aUpper = "A".repeat(64)
        assertTrue(PinnedClient.tlsPinAllows(a, aUpper))
        assertFalse(PinnedClient.tlsPinAllows(a, b))
        assertFalse(PinnedClient.tlsPinAllows("", a))
        assertFalse(PinnedClient.tlsPinAllows(a, "cafebabe"))
        assertFalse(PinnedClient.sha256HexPin("g".repeat(64)))
    }

    @Test
    fun emptyChainHasNoLeafPin() {
        assertNull(PinnedClient.presentedLeafHex(emptyArray<X509Certificate>()))
    }

    @Test
    fun fingerprintHexIsLowercaseSha256() {
        val empty = PinnedClient.fingerprintHex(ByteArray(0))
        assertEquals(64, empty.length)
        assertEquals(empty, empty.lowercase())
        assertEquals(empty, PinnedClient.fingerprintHex(ByteArray(0)))
        assertTrue(PinnedClient.sha256HexPin(empty))
        assertTrue(PinnedClient.tlsPinAllows(empty, empty))
    }
}
