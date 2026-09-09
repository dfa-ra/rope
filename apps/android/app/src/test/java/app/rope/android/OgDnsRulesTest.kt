package app.rope.android

import app.rope.android.data.OgDnsRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

class OgDnsRulesTest {
    @Test
    fun blocksLoopbackRfc1918LinkLocalAndUla() {
        assertTrue(OgDnsRules.isBlocked(addr4(127, 0, 0, 1)))
        assertTrue(OgDnsRules.isBlocked(addr4(127, 255, 255, 254)))
        assertTrue(OgDnsRules.isBlocked(InetAddress.getByName("::1")))
        assertTrue(OgDnsRules.isBlocked(addr4(10, 0, 0, 1)))
        assertTrue(OgDnsRules.isBlocked(addr4(172, 16, 0, 1)))
        assertTrue(OgDnsRules.isBlocked(addr4(172, 31, 255, 255)))
        assertTrue(OgDnsRules.isBlocked(addr4(192, 168, 1, 1)))
        assertTrue(OgDnsRules.isBlocked(addr4(169, 254, 1, 1)))
        assertTrue(OgDnsRules.isBlocked(InetAddress.getByName("fe80::1")))
        assertTrue(OgDnsRules.isBlocked(InetAddress.getByName("fc00::1")))
        assertTrue(OgDnsRules.isBlocked(InetAddress.getByName("fd12:3456:789a::1")))
        assertTrue(OgDnsRules.isBlocked(InetAddress.getByName("::ffff:10.1.2.3")))
        assertTrue(OgDnsRules.isBlocked(InetAddress.getByName("::ffff:127.0.0.1")))
        assertTrue(OgDnsRules.isBlocked(InetAddress.getByName("::ffff:169.254.9.1")))
    }

    @Test
    fun keepsPublicV4AndV6() {
        assertFalse(OgDnsRules.isBlocked(addr4(8, 8, 8, 8)))
        assertFalse(OgDnsRules.isBlocked(addr4(1, 1, 1, 1)))
        assertFalse(OgDnsRules.isBlocked(addr4(203, 0, 113, 9)))
        assertFalse(OgDnsRules.isBlocked(addr4(172, 15, 0, 1)))
        assertFalse(OgDnsRules.isBlocked(addr4(172, 32, 0, 1)))
        assertFalse(OgDnsRules.isBlocked(InetAddress.getByName("2001:4860:4860::8888")))
        assertFalse(OgDnsRules.isBlocked(InetAddress.getByName("::ffff:8.8.8.8")))
    }

    @Test
    fun lookupDropsBlockedKeepsPublicOrder() {
        val pubA = addr4(8, 8, 8, 8)
        val pubB = addr4(1, 1, 1, 1)
        val priv = addr4(10, 1, 2, 3)
        val ula = InetAddress.getByName("fd00::1")
        val out = OgDnsRules.lookup("host.example") { listOf(priv, pubA, ula, pubB) }
        assertEquals(listOf(pubA, pubB), out)
    }

    @Test
    fun lookupAllBlockedThrowsUnknownHost() {
        try {
            OgDnsRules.lookup("evil.example") {
                listOf(addr4(192, 168, 0, 1), InetAddress.getByName("::1"))
            }
            fail("expected UnknownHostException")
        } catch (e: UnknownHostException) {
            assertEquals("evil.example", e.message)
        }
    }

    private fun addr4(a: Int, b: Int, c: Int, d: Int): InetAddress =
        InetAddress.getByAddress(byteArrayOf(a.toByte(), b.toByte(), c.toByte(), d.toByte()))
}
