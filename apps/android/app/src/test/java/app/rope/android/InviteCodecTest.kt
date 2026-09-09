package app.rope.android

import app.rope.android.data.MessageStatus
import app.rope.android.protocol.InviteCodec
import app.rope.android.protocol.InviteLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteCodecTest {
    @Test
    fun roundtrip() {
        val link = InviteLink(1, "203.0.113.10", 8443, "sid", "deadbeef", "tok-1", "Ada")
        val url = InviteCodec.build(link)
        assertTrue(url.startsWith("rope://join?"))
        val parsed = InviteCodec.parse(url)
        assertEquals(link.host, parsed.host)
        assertEquals(link.token, parsed.token)
        assertEquals("deadbeef", parsed.fingerprint)
        InviteCodec.verify(parsed, "sid", "DEADBEEF")
    }

    @Test(expected = IllegalArgumentException::class)
    fun fingerprintMismatch() {
        val parsed = InviteCodec.parse("rope://join?v=1&host=h&port=8443&sid=s&fp=aa&tok=t")
        InviteCodec.verify(parsed, "s", "bb")
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsupportedVersion() {
        InviteCodec.parse("rope://join?v=2&host=h&port=8443&sid=s&fp=aa&tok=t")
    }

    @Test
    fun originBracketsIpv6() {
        assertEquals("https://vps.example:8443", InviteCodec.origin("https", "vps.example", 8443))
        assertEquals("https://[2001:db8::1]:8443", InviteCodec.origin("https", "2001:db8::1", 8443))
        assertEquals("wss://[2001:db8::1]:8443", InviteCodec.origin("wss", "[2001:db8::1]", 8443))
    }

    @Test
    fun hostRejectsShellAndUrlMetacharacters() {
        assertTrue(InviteCodec.hostOK("vps.example.com"))
        assertTrue(InviteCodec.hostOK("10.0.0.8"))
        assertTrue(InviteCodec.hostOK("2001:db8::1"))
        assertFalse(InviteCodec.hostOK(""))
        assertFalse(InviteCodec.hostOK("vps.example.com; id"))
        assertFalse(InviteCodec.hostOK("vps.example.com@evil"))
        assertFalse(InviteCodec.hostOK("vps.example.com/steal"))
        assertFalse(InviteCodec.hostOK("vps\$HOST"))
        assertFalse(InviteCodec.hostOK("vps example"))
        assertFalse(InviteCodec.hostOK("vps\nexample"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseRejectsInjectedHost() {
        InviteCodec.parse("rope://join?v=1&host=vps.example.com%3B%20id&port=8443&sid=s&fp=aa&tok=t")
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseRejectsPortZero() {
        InviteCodec.parse("rope://join?v=1&host=vps.example&port=0&sid=s&fp=aa&tok=t")
    }

    @Test(expected = IllegalArgumentException::class)
    fun originRejectsBadHost() {
        InviteCodec.origin("https", "evil.com/steal", 8443)
    }
}

class MessageStatusTest {
    @Test
    fun progression() {
        assertEquals(MessageStatus.SENT_TO_SERVER, next(MessageStatus.CREATED, "queued"))
        assertEquals(MessageStatus.DELIVERED_TO_DEVICE, next(MessageStatus.SENT_TO_SERVER, "delivered"))
    }

    private fun next(current: MessageStatus, event: String): MessageStatus = when (event) {
        "queued" -> MessageStatus.SENT_TO_SERVER
        "delivered" -> MessageStatus.DELIVERED_TO_DEVICE
        else -> current
    }
}
