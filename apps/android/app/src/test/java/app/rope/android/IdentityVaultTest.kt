package app.rope.android

import app.rope.android.data.IdentityVault
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class IdentityVaultTest {
    @Test
    fun packUnpackRoundtripsTwelveByteIv() {
        val iv = ByteArray(IdentityVault.IV_LEN) { it.toByte() }
        val ct = ByteArray(IdentityVault.TAG_LEN + 4) { (it + 3).toByte() }
        val packed = IdentityVault.pack(iv, ct)
        val (gotIv, gotCt) = IdentityVault.unpack(packed)
        assertEquals(IdentityVault.IV_LEN, packed[0].toInt() and 0xff)
        assertArrayEquals(iv, gotIv)
        assertArrayEquals(ct, gotCt)
    }

    @Test
    fun unpackRejectsNonGcmIvLength() {
        val ct = ByteArray(IdentityVault.TAG_LEN)
        try {
            IdentityVault.unpack(byteArrayOf(1) + ByteArray(1) + ct)
            fail("expected bad wrap")
        } catch (_: IllegalStateException) {
        }
        try {
            IdentityVault.unpack(byteArrayOf(16) + ByteArray(16) + ct)
            fail("expected bad wrap")
        } catch (_: IllegalStateException) {
        }
    }

    @Test
    fun unpackRejectsTruncatedTag() {
        try {
            IdentityVault.unpack(byteArrayOf(12) + ByteArray(IdentityVault.IV_LEN) + ByteArray(15))
            fail("expected bad wrap")
        } catch (_: IllegalStateException) {
        }
        try {
            IdentityVault.unpack(byteArrayOf(12))
            fail("expected bad wrap")
        } catch (_: IllegalStateException) {
        }
    }

    @Test
    fun packRejectsNonGcmIv() {
        val ct = ByteArray(IdentityVault.TAG_LEN)
        try {
            IdentityVault.pack(ByteArray(8), ct)
            fail("expected require")
        } catch (_: IllegalArgumentException) {
        }
    }
}
