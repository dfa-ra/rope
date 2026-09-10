package app.rope.android

import app.rope.android.data.CallEffect
import app.rope.android.data.CallMachine
import app.rope.android.data.CallSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallSignalingCrlfTest {
    private fun offer(sdp: String = "v=0") = CallSignal(CallSignal.OFFER, sdp = sdp)

    @Test
    fun localStartRejectsControlInCallIdBeforeTrim() {
        val m = CallMachine()
        assertTrue(m.localStart("c1\n", "bob", "alice").isEmpty())
        assertFalse(m.state.live)
        assertTrue(m.localStart("c1\r", "bob", "alice").isEmpty())
        assertTrue(m.localStart("c1\u0000", "bob", "alice").isEmpty())
        assertTrue(m.localStart("\nc1", "bob", "alice").isEmpty())
    }

    @Test
    fun onWireRingRejectsControlInCallIdBeforeTrim() {
        val m = CallMachine()
        assertTrue(m.onWire("alice", CallSignal.RING, "c1\n", "", "bob").isEmpty())
        assertFalse(m.state.live)
        assertTrue(m.onWire("alice", CallSignal.RING, "c1\r", "", "bob").isEmpty())
        assertTrue(m.onWire("alice", CallSignal.RING, "c1\u0000x", "", "bob").isEmpty())
    }

    @Test
    fun onWireDoesNotBindOfferAfterTrailingNewlineTrim() {
        val m = CallMachine()
        val start = m.localStart("c1", "bob", "alice")
        assertTrue(start.any { it is CallEffect.Send && it.event == CallSignal.RING })
        m.onWire("bob", CallSignal.ACCEPT, "c1", "", "alice")
        m.onSessionAttached()
        m.onLocalOfferSent()

        val injected = m.onWire("bob", CallSignal.OFFER, "c1\n", offer().toJson(), "alice")
        assertTrue(injected.none { it is CallEffect.DeliverRemote })
        assertEquals("c1", m.state.callId)

        val hang = m.onWire("bob", CallSignal.HANGUP, "c1\r", "", "alice")
        assertTrue(hang.isEmpty())
        assertTrue(m.state.live)
    }

    @Test
    fun cleanCallIdsStillRingAndAccept() {
        val m = CallMachine()
        val ring = m.onWire("alice", CallSignal.RING, "c1", "", "bob")
        assertTrue(ring.any { it is CallEffect.NotifyIncoming })
        assertEquals("c1", m.state.callId)
        val accept = m.localAccept()
        assertTrue(accept.any { it is CallEffect.Send && it.event == CallSignal.ACCEPT })
    }
}
