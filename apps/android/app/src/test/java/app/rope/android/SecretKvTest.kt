package app.rope.android

import app.rope.android.data.SecretKv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
