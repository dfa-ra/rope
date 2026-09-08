package app.rope.android

import app.rope.android.data.CallEffect
import app.rope.android.data.CallLink
import app.rope.android.data.CallLinkState
import app.rope.android.data.CallMachine
import app.rope.android.data.CallPhase
import app.rope.android.data.CallRtcRole
import app.rope.android.data.CallSignal
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallSignalingTest {
    private fun offer(sdp: String = "v=0") = CallSignal(CallSignal.OFFER, sdp = sdp)
    private fun answer(sdp: String = "v=0") = CallSignal(CallSignal.ANSWER, sdp = sdp)
    private fun ice(cand: String = "typ host") =
        CallSignal(CallSignal.ICE, candidate = cand, sdpMid = "0", sdpMLineIndex = 0)

    private fun CallMachine.media(from: String, event: String, callId: String, sig: CallSignal, me: String) =
        onWire(from, event, callId, sig.toJson(), me)

    @Test
    fun callerOrderRingAcceptOfferAnswerIceConnected() {
        val m = CallMachine()
        val start = m.localStart("c1", "bob", "alice")
        assertTrue(start.any { it is CallEffect.Send && it.event == CallSignal.RING })
        assertEquals(CallPhase.RINGING_OUT, m.state.phase)

        val accept = m.onWire("bob", CallSignal.ACCEPT, "c1", "", "alice")
        assertTrue(accept.any { it is CallEffect.StartRtc && it.asCaller })
        assertEquals(CallPhase.ACTIVE, m.state.phase)
        assertEquals(CallRtcRole.OFFERER, m.state.role)

        assertTrue(m.onSessionAttached().none { it is CallEffect.DeliverRemote })
        assertTrue(m.onLocalOfferSent().none { it is CallEffect.DeliverRemote })

        val ans = m.media("bob", CallSignal.ANSWER, "c1", answer(), "alice")
        val delivered = ans.filterIsInstance<CallEffect.DeliverRemote>().single()
        assertEquals(listOf(CallSignal.ANSWER), delivered.signals.map { it.kind })

        val trickle = m.media("bob", CallSignal.ICE, "c1", ice(), "alice")
        assertTrue(trickle.any { it is CallEffect.DeliverRemote })

        m.onIce("CHECKING", false)
        assertEquals(CallLinkState.CONNECTING, m.state.link)
        m.onIce("CONNECTED", false)
        assertEquals(CallLinkState.CONNECTED, m.state.link)
        assertEquals("Разговор", CallLink.heading(m.state.phase, m.state.link))
    }

    @Test
    fun calleeOrderRingAcceptOfferIce() {
        val m = CallMachine()
        val ring = m.onWire("alice", CallSignal.RING, "c1", "", "bob")
        assertTrue(ring.any { it is CallEffect.NotifyIncoming })
        assertEquals(CallPhase.RINGING_IN, m.state.phase)

        val accept = m.localAccept()
        assertTrue(accept.any { it is CallEffect.Send && it.event == CallSignal.ACCEPT })
        assertTrue(accept.any { it is CallEffect.StartRtc && !it.asCaller })
        assertEquals(CallRtcRole.ANSWERER, m.state.role)

        m.onSessionAttached()
        val off = m.media("alice", CallSignal.OFFER, "c1", offer(), "bob")
        assertEquals(
            listOf(CallSignal.OFFER),
            off.filterIsInstance<CallEffect.DeliverRemote>().single().signals.map { it.kind },
        )
    }

    @Test
    fun answerBeforeLocalOfferIsQueuedThenFlushed() {
        val m = CallMachine()
        m.localStart("c1", "bob", "alice")
        m.onWire("bob", CallSignal.ACCEPT, "c1", "", "alice")

        val early = m.media("bob", CallSignal.ANSWER, "c1", answer("v=early"), "alice")
        assertTrue(early.none { it is CallEffect.DeliverRemote })
        assertEquals(1, m.state.queue.size)
        assertEquals(CallSignal.ANSWER, m.state.queue.single().kind)

        m.onSessionAttached()
        assertEquals(1, m.state.queue.size)

        val flushed = m.onLocalOfferSent()
        val delivered = flushed.filterIsInstance<CallEffect.DeliverRemote>().single()
        assertEquals("v=early", delivered.signals.single().sdp)
        assertTrue(m.state.queue.isEmpty())
        assertTrue(m.state.remoteDescriptionReady)
    }

    @Test
    fun iceBeforeRemoteDescriptionIsQueuedUntilOffer() {
        val m = CallMachine()
        m.onWire("alice", CallSignal.RING, "c1", "", "bob")
        m.localAccept()

        val earlyIce = m.media("alice", CallSignal.ICE, "c1", ice("typ relay"), "bob")
        assertTrue(earlyIce.none { it is CallEffect.DeliverRemote })
        assertEquals(1, m.state.queue.size)

        m.media("alice", CallSignal.OFFER, "c1", offer(), "bob")
        assertEquals(2, m.state.queue.size)

        val flushed = m.onSessionAttached()
        val delivered = flushed.filterIsInstance<CallEffect.DeliverRemote>().single().signals
        assertEquals(listOf(CallSignal.OFFER, CallSignal.ICE), delivered.map { it.kind })
        assertTrue(m.state.queue.isEmpty())
    }

    @Test
    fun glarePicksOneOffererAndSharedCallId() {
        val a = CallMachine()
        val b = CallMachine()
        a.localStart("call-aaa", "bbb", "aaa")
        b.localStart("call-bbb", "aaa", "bbb")

        val aGlare = a.onWire("bbb", CallSignal.RING, "call-bbb", "", "aaa")
        val bGlare = b.onWire("aaa", CallSignal.RING, "call-aaa", "", "bbb")

        val shared = CallLink.canonicalCallId("call-aaa", "call-bbb")
        assertEquals(shared, a.state.callId)
        assertEquals(shared, b.state.callId)
        assertTrue(a.state.altCallId.isNotBlank())
        assertTrue(b.state.altCallId.isNotBlank())

        val aStarts = aGlare.filterIsInstance<CallEffect.StartRtc>().single()
        val bStarts = bGlare.filterIsInstance<CallEffect.StartRtc>().single()
        assertTrue(aStarts.asCaller)
        assertFalse(bStarts.asCaller)
        assertEquals(CallRtcRole.OFFERER, a.state.role)
        assertEquals(CallRtcRole.ANSWERER, b.state.role)

        b.onSessionAttached()
        val offerOnAlt = b.media("aaa", CallSignal.OFFER, "call-aaa", offer(), "bbb")
        assertTrue(offerOnAlt.any { it is CallEffect.DeliverRemote })
    }

    @Test
    fun hangupTearsDownAndDropsQueuedAnswer() {
        val m = CallMachine()
        m.localStart("c1", "bob", "alice")
        m.onWire("bob", CallSignal.ACCEPT, "c1", "", "alice")
        m.media("bob", CallSignal.ANSWER, "c1", answer(), "alice")
        assertTrue(m.state.queue.isNotEmpty())

        val hang = m.localHangup()
        assertTrue(hang.any { it is CallEffect.Send && it.event == CallSignal.HANGUP })
        assertTrue(hang.contains(CallEffect.TearDown))
        assertFalse(m.state.live)
        assertTrue(m.state.queue.isEmpty())

        val after = m.media("bob", CallSignal.ANSWER, "c1", answer("v=late"), "alice")
        assertTrue(after.isEmpty())
        assertFalse(m.state.live)
    }

    @Test
    fun rejectTearsDownBothSides() {
        val callee = CallMachine()
        callee.onWire("alice", CallSignal.RING, "c1", "", "bob")
        val reject = callee.localReject()
        assertTrue(reject.any { it is CallEffect.Send && it.event == CallSignal.REJECT })
        assertTrue(reject.contains(CallEffect.TearDown))
        assertFalse(callee.state.live)

        val caller = CallMachine()
        caller.localStart("c1", "bob", "alice")
        val remote = caller.onWire("bob", CallSignal.REJECT, "c1", "", "alice")
        assertEquals(listOf(CallEffect.TearDown), remote)
        assertFalse(caller.state.live)
    }

    @Test
    fun remoteHangupAlwaysTearsDownSamePeer() {
        val m = CallMachine()
        m.localStart("mine", "bob", "alice")
        m.onWire("bob", CallSignal.RING, "theirs", "", "alice")
        assertTrue(m.state.live)
        val end = m.onWire("bob", CallSignal.HANGUP, "theirs", "", "alice")
        assertEquals(listOf(CallEffect.TearDown), end)
        assertFalse(m.state.live)
    }

    @Test
    fun connectTimeoutSurfacesFailedWithoutSpin() {
        val m = CallMachine()
        m.localStart("c1", "bob", "alice")
        m.onWire("bob", CallSignal.ACCEPT, "c1", "", "alice")
        m.onHasTurn(true, "")
        m.onSessionAttached()
        m.onLocalOfferSent()
        m.media("bob", CallSignal.ANSWER, "c1", answer(), "alice")
        m.onIce("CHECKING", false)

        val first = m.onConnectTimeout()
        assertEquals(CallLinkState.FAILED, m.state.link)
        assertFalse(first.contains(CallEffect.RestartIce))
        assertTrue(first.any { it is CallEffect.Notice && it.message.contains("25") })
        assertTrue(m.state.live)

        val again = m.onConnectTimeout()
        assertTrue(again.isEmpty())
    }

    @Test
    fun outgoingRingTimeoutStaysFailedOnScreen() {
        val m = CallMachine()
        m.localStart("c1", "bob", "alice")
        val timed = m.onRingTimeout()
        assertEquals(CallLinkState.FAILED, m.state.link)
        assertEquals(CallLink.noAnswerDetail(), m.state.media)
        assertTrue(m.state.live)
        assertTrue(timed.any { it is CallEffect.Send && it.event == CallSignal.HANGUP })
        assertFalse(timed.contains(CallEffect.TearDown))
        assertEquals("Нет ответа", CallLink.heading(m.state.phase, m.state.link, m.state.media))
    }

    @Test
    fun offlineRingStaysFailedOnScreen() {
        val m = CallMachine()
        m.localStart("c1", "bob", "alice")
        val failed = m.onRingSendFailed()
        assertEquals(CallLinkState.FAILED, m.state.link)
        assertEquals(CallLink.offlineDetail(), m.state.media)
        assertTrue(m.state.live)
        assertFalse(failed.contains(CallEffect.TearDown))
        assertEquals("Не в сети", CallLink.heading(m.state.phase, m.state.link, m.state.media))
    }

    @Test
    fun droppedRingRecoveredByEarlyOffer() {
        val m = CallMachine()
        val effects = m.media("alice", CallSignal.OFFER, "c1", offer(), "bob")
        assertEquals(CallPhase.RINGING_IN, m.state.phase)
        assertEquals(1, m.state.queue.size)
        assertTrue(effects.any { it is CallEffect.NotifyIncoming })
        m.localAccept()
        val flushed = m.onSessionAttached()
        assertEquals(
            CallSignal.OFFER,
            flushed.filterIsInstance<CallEffect.DeliverRemote>().single().signals.single().kind,
        )
    }

    @Test
    fun envelopeType4AndWssPayloadParseTheSame() {
        val sig = offer("v=offer")
        val body = JSONObject(CallSignal.envelopeJson("c9", CallSignal.OFFER, sig.toJson()))
        assertEquals("c9", body.getString("call_id"))
        assertEquals(CallSignal.OFFER, body.getString("event"))
        val parsed = CallSignal.parseMedia(body.getString("event"), body.opt("payload"))!!
        assertEquals(CallSignal.OFFER, parsed.kind)
        assertEquals("v=offer", parsed.sdp)
        assertEquals(CallSignal.RING, CallSignal.parseEvent("RING"))
        assertEquals(null, CallSignal.parse("""{"kind":"ring"}"""))
    }

    @Test
    fun startWhileIncomingIsAccept() {
        val m = CallMachine()
        m.onWire("alice", CallSignal.RING, "c1", "", "bob")
        val effects = m.localStart("ignored", "alice", "bob")
        assertTrue(effects.any { it is CallEffect.Send && it.event == CallSignal.ACCEPT })
        assertEquals(CallPhase.ACTIVE, m.state.phase)
        assertEquals("c1", m.state.callId)
    }
}
