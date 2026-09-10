package app.rope.android

import app.rope.android.data.SecretKv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class SecretKvTest {
    @Test
    fun wrapAddsPrefixAndRoundtrips() {
        val wrapped = SecretKv.wrap("ghp_secret") { it.reversedArray() }
        assertTrue(SecretKv.isWrapped(wrapped))
        assertFalse(wrapped.contains("ghp_secret"))
        assertEquals("ghp_secret", SecretKv.unwrap(wrapped) { it.reversedArray() })
    }

    @Test
    fun unwrapLegacyPlaintext() {
        assertEquals("ghp_old", SecretKv.unwrap("ghp_old") { error("no decrypt") })
        assertNull(SecretKv.unwrap(null) { it })
        assertNull(SecretKv.unwrap("  ") { it })
    }

    @Test
    fun wrapIsIdempotent() {
        val once = SecretKv.wrap("tok") { it }
        assertEquals(once, SecretKv.wrap(once) { error("must not re-encrypt") })
    }

    @Test
    fun unwrapCorruptOrKeystoreMissIsNull() {
        assertNull(SecretKv.unwrap(SecretKv.PREFIX + "%%%") { it })
        val wrapped = SecretKv.wrap("tok") { it.reversedArray() }
        assertNull(SecretKv.unwrap(wrapped) { error("keystore miss") })
        assertEquals("ghp_old", SecretKv.unwrap("ghp_old") { error("no decrypt") })
    }

    @Test
    fun wrapRejectsControlInPatBeforeTrim() {
        val xor: (ByteArray) -> ByteArray = { it.reversedArray() }
        assertEquals("", SecretKv.wrap("ghp_secret\n", xor))
        assertEquals("", SecretKv.wrap("ghp_secret\r", xor))
        assertEquals("", SecretKv.wrap("ghp_secret\u0000x", xor))
        assertEquals("", SecretKv.wrap("ghp_secret\r\nAuthorization: x", xor))
        assertNull(SecretKv.unwrap("ghp_old\n") { error("no decrypt") })
        assertNull(SecretKv.unwrap("ghp_old\rX") { error("no decrypt") })
        val dirty = SecretKv.PREFIX + Base64.getEncoder().encodeToString("tok\n".toByteArray())
        assertNull(SecretKv.unwrap(dirty) { it })
    }

    @Test
    fun iceJsonWithNewlinesStillWraps() {
        val ice = "[\n{\"urls\":[\"turn:vps:3478\"],\"credential\":\"c\"}\n]"
        val wrapped = SecretKv.wrap(ice) { it.reversedArray() }
        assertTrue(SecretKv.isWrapped(wrapped))
        assertEquals(ice.trim(), SecretKv.unwrap(wrapped) { it.reversedArray() })
        assertEquals(ice, SecretKv.unwrap(ice) { error("no decrypt") })
    }
}
