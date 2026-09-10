package app.rope.android

import app.rope.android.data.MessageAtRest
import app.rope.android.data.MessageMeta
import app.rope.android.data.SecretKv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageAtRestTest {
    private val xor: (ByteArray) -> ByteArray = { src ->
        ByteArray(src.size) { i -> (src[i].toInt() xor 0x5A).toByte() }
    }

    @Test
    fun sealHidesObjectKeyAndOpens() {
        val extra = """{"kind":"image","object_id":"o","sha256":"s","key_b64":"SECRETKEY","mime":"image/jpeg","name":"p.jpg","size":1}"""
        val sealed = MessageAtRest.seal(extra, xor)
        assertTrue(SecretKv.isWrapped(sealed))
        assertFalse(sealed.contains("key_b64"))
        assertFalse(sealed.contains("SECRETKEY"))
        assertEquals(extra, MessageAtRest.open(sealed, xor))
    }

    @Test
    fun sealHidesReplyPreviewMeta() {
        val meta = MessageMeta("r1", "секретный превью", "Имя").toJson()
        val sealed = MessageAtRest.seal(meta, xor)
        assertTrue(SecretKv.isWrapped(sealed))
        assertFalse(sealed.contains("секретный превью"))
        assertEquals(meta, MessageAtRest.open(sealed, xor))
    }

    @Test
    fun openLegacyPlaintext() {
        val extra = """{"kind":"voice","object_id":"o","sha256":"s","key_b64":"k","mime":"audio/mp4","name":"v.m4a","size":1}"""
        assertEquals(extra, MessageAtRest.open(extra) { error("must not decrypt plaintext") })
        assertEquals("", MessageAtRest.open(null) { it })
        assertEquals("", MessageAtRest.open("null") { it })
        assertEquals("", MessageAtRest.seal("") { it })
        assertEquals("", MessageAtRest.seal("null") { it })
    }

    @Test
    fun openCorruptOrKeystoreMissIsEmpty() {
        val sealed = MessageAtRest.seal("""{"key_b64":"c"}""", xor)
        assertEquals("", MessageAtRest.open(sealed) { error("keystore miss") })
        assertEquals("", MessageAtRest.open(SecretKv.PREFIX + "%%%") { it })
    }

    @Test
    fun sealIsIdempotent() {
        val once = MessageAtRest.seal("""{"key_b64":"c"}""", xor)
        assertEquals(once, MessageAtRest.seal(once, xor))
    }
}
