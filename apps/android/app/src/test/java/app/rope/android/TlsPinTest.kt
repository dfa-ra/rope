package app.rope.android

import app.rope.android.net.PinnedClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TlsPinTest {
    @Test
    fun blankPinFailsClosed() {
        assertFalse(PinnedClient.tlsPinAllows("abc", ""))
        assertFalse(PinnedClient.tlsPinAllows("abc", "  "))
        assertFalse(PinnedClient.tlsPinAllows("", ""))
    }

    @Test
    fun configuredPinFailsClosedOnMismatch() {
        assertTrue(PinnedClient.tlsPinAllows("deadbeef", "DEADBEEF"))
        assertTrue(PinnedClient.tlsPinAllows("Aa", "aa"))
        assertFalse(PinnedClient.tlsPinAllows("deadbeef", "cafebabe"))
        assertFalse(PinnedClient.tlsPinAllows("", "deadbeef"))
        assertFalse(PinnedClient.tlsPinAllows("deadbeef ", "cafebabe"))
    }

    @Test
    fun fingerprintHexIsLowercaseSha256() {
        val empty = PinnedClient.fingerprintHex(ByteArray(0))
        assertEquals(64, empty.length)
        assertEquals(empty, empty.lowercase())
        assertEquals(empty, PinnedClient.fingerprintHex(ByteArray(0)))
    }
}
