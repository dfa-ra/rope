package app.rope.android

import app.rope.android.net.WsAuth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WsAuthTest {
    @Test
    fun headerValueIsDeviceTsSig() {
        val v = WsAuth.value("abc", "1700000000", "sig_token")
        assertEquals("abc.1700000000.sig_token", v)
        assertEquals("X-Rope-Ws-Auth", WsAuth.HEADER)
        assertFalse(v.startsWith("Rope "))
    }

    @Test
    fun phaseAKeepsQueryShape() {
        val id = "deadbeef"
        val ts = "99"
        val sig = "s"
        val url = "wss://vps:8443/v1/ws?device_id=$id&ts=$ts&sig=$sig"
        assertTrue(url.contains("sig=$sig"))
        assertEquals("$id.$ts.$sig", WsAuth.value(id, ts, sig))
    }
}
