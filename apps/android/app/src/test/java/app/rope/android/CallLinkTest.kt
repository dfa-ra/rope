package app.rope.android

import app.rope.android.data.CallLink
import app.rope.android.data.CallLinkState
import app.rope.android.data.CallPhase
import app.rope.android.data.CallSignal
import app.rope.android.data.IceServerSpec
import app.rope.android.data.IceServers
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallLinkTest {
    @Test
    fun headingMapsConnectingInsteadOfFakeTalk() {
        assertEquals("Входящий вызов", CallLink.heading(CallPhase.RINGING_IN, CallLinkState.RINGING))
        assertEquals("Вызов…", CallLink.heading(CallPhase.RINGING_OUT, CallLinkState.RINGING))
        assertEquals("Соединение…", CallLink.heading(CallPhase.ACTIVE, CallLinkState.CONNECTING))
        assertEquals("Разговор", CallLink.heading(CallPhase.ACTIVE, CallLinkState.CONNECTED))
        assertEquals("Нет соединения", CallLink.heading(CallPhase.ACTIVE, CallLinkState.FAILED))
        assertEquals("Завершён", CallLink.heading(CallPhase.ENDED, CallLinkState.FAILED))
    }

    @Test
    fun connectingTimesOutAfter25sUnlessConnected() {
        assertFalse(CallLink.timedOut(0, CallLinkState.CONNECTING))
        assertFalse(CallLink.timedOut(24_999, CallLinkState.CONNECTING))
        assertTrue(CallLink.timedOut(25_000, CallLinkState.CONNECTING))
        assertTrue(CallLink.timedOut(30_000, CallLinkState.RINGING))
        assertFalse(CallLink.timedOut(60_000, CallLinkState.CONNECTED))
        assertFalse(CallLink.timedOut(60_000, CallLinkState.FAILED))
    }

    @Test
    fun iceStatesMapToUiAndFailedCopy() {
        assertEquals(CallLinkState.CONNECTED, CallLink.applyIce("CONNECTED").first)
        assertEquals(CallLinkState.CONNECTED, CallLink.applyIce("completed", viaRelay = true).first)
        assertEquals("WebRTC · через сервер", CallLink.applyIce("CONNECTED", viaRelay = true).second)
        assertEquals(CallLinkState.FAILED, CallLink.applyIce("FAILED").first)
        assertTrue(CallLink.applyIce("FAILED", hasTurn = false).second.contains("ядро"))
        assertEquals(CallLinkState.FAILED, CallLink.applyIce("CLOSED").first)
        assertEquals(CallLinkState.CONNECTING, CallLink.applyIce("DISCONNECTED").first)
        assertTrue(CallLink.applyIce("DISCONNECTED").second.contains("прервалась"))
        assertEquals(CallLinkState.CONNECTING, CallLink.applyIce("CHECKING").first)
        assertTrue(CallLink.applyIce("NEW", hasTurn = false).second.contains("TURN"))
    }

    @Test
    fun timeoutCopyMentionsCoreWhenTurnMissing() {
        val missing = CallLink.timeoutDetail(hasTurn = false)
        assertTrue(missing.contains("TURN"))
        assertTrue(missing.contains("ядро"))
        val withTurn = CallLink.timeoutDetail(hasTurn = true)
        assertTrue(withTurn.contains("3478"))
        assertTrue(CallLink.missingTurnDetail().contains("обновите ядро"))
    }

    @Test
    fun glarePicksStableOfferer() {
        assertTrue(CallLink.weCreateOffer("aaa", "bbb"))
        assertFalse(CallLink.weCreateOffer("bbb", "aaa"))
        assertFalse(CallLink.weCreateOffer("", "bbb"))
    }

    @Test
    fun callSignalRejectsNullSdpAndAcceptsObjectPayload() {
        assertNull(CallSignal.parse("""{"kind":"offer","sdp":null}"""))
        assertNull(CallSignal.parse("""{"kind":"answer","sdp":""}"""))
        assertNull(CallSignal.parse("null"))
        val obj = JSONObject()
            .put("kind", "offer")
            .put("sdp", "v=0")
        val parsed = CallSignal.parsePayload(obj)!!
        assertEquals(CallSignal.OFFER, parsed.kind)
        assertEquals("v=0", parsed.sdp)
        assertNull(CallSignal.parsePayload(null))
        assertNull(CallSignal.parsePayload("null"))
    }

    @Test
    fun resolveHintsVpsStunWhenInfoHasNoIce() {
        val resolved = IceServers.resolve(emptyList(), "203.0.113.9")
        assertTrue(resolved.any { it.urls.contains("stun:203.0.113.9:3478") })
        assertTrue(IceServers.missingTurn(resolved))
        assertNull(IceServers.stunHint("localhost"))
        assertEquals(listOf("stun:[2001:db8::1]:3478"), IceServers.stunHint("2001:db8::1")!!.urls)
        assertFalse(IceServers.missingTurn(listOf(IceServerSpec(listOf("turn:vps:3478"), "u", "c"))))
    }
}
