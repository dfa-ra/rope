package app.rope.android

import app.rope.android.data.SecretKv
import app.rope.android.data.SshTargetAtRest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SshTargetAtRestTest {
    private val xor: (ByteArray) -> ByteArray = { src ->
        ByteArray(src.size) { i -> (src[i].toInt() xor 0x5A).toByte() }
    }

    private val json = """{"host":"10.0.0.8","sshPort":22,"user":"root","listenPort":8443}"""

    @Test
    fun sealHidesHostAndOpens() {
        val sealed = SshTargetAtRest.seal(json, xor)
        assertTrue(SecretKv.isWrapped(sealed))
        assertFalse(sealed.contains("10.0.0.8"))
        assertFalse(sealed.contains("root"))
        assertEquals(json, SshTargetAtRest.open(sealed, xor))
    }

    @Test
    fun openLegacyPlaintext() {
        assertEquals(json, SshTargetAtRest.open(json) { error("must not decrypt plaintext") })
        assertEquals("", SshTargetAtRest.open(null) { it })
        assertEquals("", SshTargetAtRest.open("  ") { it })
        assertEquals("", SshTargetAtRest.seal("") { it })
        assertEquals("", SshTargetAtRest.seal("   ") { it })
    }

    @Test
    fun openCorruptOrKeystoreMissIsEmpty() {
        val sealed = SshTargetAtRest.seal(json, xor)
        assertEquals("", SshTargetAtRest.open(sealed) { error("keystore miss") })
        assertEquals("", SshTargetAtRest.open(SecretKv.PREFIX + "%%%") { it })
    }

    @Test
    fun sealIsIdempotent() {
        val once = SshTargetAtRest.seal(json, xor)
        assertEquals(once, SshTargetAtRest.seal(once, xor))
    }
}
