package app.rope.android

import app.rope.android.data.IceAtRest
import app.rope.android.data.IceServers
import app.rope.android.data.SecretKv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IceAtRestTest {
    private val xor: (ByteArray) -> ByteArray = { src ->
        ByteArray(src.size) { i -> (src[i].toInt() xor 0x5A).toByte() }
    }

    @Test
    fun sealHidesTurnCredentialAndOpens() {
        val ice = """[{"urls":["turn:vps:3478"],"username":"u","credential":"c"}]"""
        val sealed = IceAtRest.seal(ice, xor)
        assertTrue(SecretKv.isWrapped(sealed))
        assertFalse(sealed.contains("credential"))
        assertFalse(sealed.contains("turn:vps:3478"))
        assertEquals(ice, IceAtRest.open(sealed, xor))
    }

    @Test
    fun openLegacyPlaintext() {
        val ice = """[{"urls":["stun:vps:3478"]}]"""
        assertEquals(ice, IceAtRest.open(ice) { error("must not decrypt plaintext") })
        assertEquals("", IceAtRest.open(null) { it })
        assertEquals("", IceAtRest.open("null") { it })
        assertEquals("", IceAtRest.seal("") { it })
        assertEquals("", IceAtRest.seal("null") { it })
    }

    @Test
    fun openCorruptOrKeystoreMissIsEmpty() {
        val sealed = IceAtRest.seal("""[{"urls":["turn:vps:3478"],"credential":"c"}]""", xor)
        assertEquals("", IceAtRest.open(sealed) { error("keystore miss") })
        assertEquals("", IceAtRest.open(SecretKv.PREFIX + "%%%") { it })
    }

    @Test
    fun unwrapMissDoesNotResolveToPublicStun() {
        val sealed = IceAtRest.seal("""[{"urls":["turn:vps:3478"],"credential":"c"}]""", xor)
        val ice = IceAtRest.open(sealed) { error("keystore miss") }
        assertEquals("", ice)
        val parsed = IceServers.parse(ice)
        val noHost = IceServers.resolve(parsed)
        assertTrue(noHost.isEmpty())
        assertFalse(IceServers.usesPublicStunFallback(noHost))
        assertTrue(noHost.none { spec -> spec.urls.any { it.contains("google") || it.contains("cloudflare") } })
        val hinted = IceServers.resolve(parsed, "203.0.113.9")
        assertEquals(listOf("stun:203.0.113.9:3478"), hinted.flatMap { it.urls })
        assertFalse(IceServers.usesPublicStunFallback(hinted))
        val privateHost = IceServers.resolve(parsed, "10.0.0.8")
        assertTrue(privateHost.isEmpty())
        assertFalse(IceServers.usesPublicStunFallback(privateHost))
        val plan = IceServers.plan(parsed)
        assertTrue(plan.servers.isEmpty())
        assertFalse(plan.forceRelay)
        assertTrue(plan.servers.none { spec -> spec.urls.any { it.contains("google") || it.contains("cloudflare") } })
    }

    @Test
    fun sealIsIdempotent() {
        val once = IceAtRest.seal("""[{"urls":["turn:vps:3478"]}]""", xor)
        assertEquals(once, IceAtRest.seal(once, xor))
    }
}
