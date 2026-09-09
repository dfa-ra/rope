package app.rope.android

import app.rope.android.provision.HostKeyPin
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HostKeyPinTest {
    @Test
    fun firstKeyIsAcceptedAndPinned() {
        val key = byteArrayOf(1, 2, 3)
        val (ok, pinned) = HostKeyPin.accept(null, key)
        assertTrue(ok)
        assertArrayEquals(key, pinned)
        assertFalse(key === pinned)
    }

    @Test
    fun sameKeyOnRetryIsAccepted() {
        val key = byteArrayOf(9, 8, 7)
        val (_, pinned) = HostKeyPin.accept(null, key)
        val (ok, still) = HostKeyPin.accept(pinned, key.copyOf())
        assertTrue(ok)
        assertArrayEquals(key, still)
    }

    @Test
    fun differentKeyOnRetryIsRejected() {
        val (okFirst, pinned) = HostKeyPin.accept(null, byteArrayOf(1))
        assertTrue(okFirst)
        val (okSecond, still) = HostKeyPin.accept(pinned, byteArrayOf(2))
        assertFalse(okSecond)
        assertArrayEquals(byteArrayOf(1), still)
    }

    @Test
    fun emptyPresentedIsRejected() {
        val (ok, pinned) = HostKeyPin.accept(null, byteArrayOf())
        assertFalse(ok)
        assertTrue(pinned == null)
    }
}
