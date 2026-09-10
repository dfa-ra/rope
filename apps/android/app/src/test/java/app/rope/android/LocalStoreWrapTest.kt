package app.rope.android

import app.rope.android.data.LocalStore
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class LocalStoreWrapTest {
    @Test
    fun packUnpackRoundtripsTwelveByteIv() {
        val iv = ByteArray(LocalStore.IV_LEN) { it.toByte() }
        val ct = ByteArray(LocalStore.TAG_LEN + 4) { (it + 3).toByte() }
        val packed = LocalStore.pack(iv, ct)
        val (gotIv, gotCt) = LocalStore.unpack(packed)
        assertEquals(LocalStore.IV_LEN, packed[0].toInt() and 0xff)
        assertArrayEquals(iv, gotIv)
        assertArrayEquals(ct, gotCt)
    }

    @Test
    fun unpackRejectsNonGcmIvLength() {
        val ct = ByteArray(LocalStore.TAG_LEN)
        try {
            LocalStore.unpack(byteArrayOf(1) + ByteArray(1) + ct)
            fail("expected bad wrap")
        } catch (_: IllegalStateException) {
        }
        try {
            LocalStore.unpack(byteArrayOf(16) + ByteArray(16) + ct)
            fail("expected bad wrap")
        } catch (_: IllegalStateException) {
        }
        try {
            LocalStore.unpack(byteArrayOf(0) + ct)
            fail("expected bad wrap")
        } catch (_: IllegalStateException) {
        }
    }

    @Test
    fun unpackRejectsTruncatedTag() {
        try {
            LocalStore.unpack(byteArrayOf(12) + ByteArray(LocalStore.IV_LEN) + ByteArray(15))
            fail("expected bad wrap")
        } catch (_: IllegalStateException) {
        }
        try {
            LocalStore.unpack(byteArrayOf(12))
            fail("expected bad wrap")
        } catch (_: IllegalStateException) {
        }
        try {
            LocalStore.unpack(byteArrayOf())
            fail("expected empty wrap")
        } catch (_: IllegalStateException) {
        }
    }

    @Test
    fun packRejectsNonGcmIv() {
        val ct = ByteArray(LocalStore.TAG_LEN)
        try {
            LocalStore.pack(ByteArray(8), ct)
            fail("expected require")
        } catch (_: IllegalArgumentException) {
        }
    }
}
