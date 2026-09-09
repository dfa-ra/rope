package app.rope.android

import app.rope.android.data.CallEffect
import app.rope.android.data.CallLinkState
import app.rope.android.data.CallMachine
import app.rope.android.data.CallPhase
import app.rope.android.data.CallSignal
import app.rope.android.data.CallToneKind
import app.rope.android.data.CallToneRules
import app.rope.android.data.NotifyRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallToneRulesTest {
    @Test
    fun globalMuteSilencesMessagesButNotIncomingRingtone() {
        for (global in listOf(true, false)) {
            for (chat in listOf(true, false)) {
                assertTrue(CallToneRules.shouldRingIncoming(global, chat))
                assertTrue(CallToneRules.shouldNotifyIncoming(global))
                assertTrue(CallToneRules.shouldRingOutgoing(global))
            }
        }
        assertFalse(NotifyRules.shouldAlert(false, false, muted = false, globalMuted = true))
        assertFalse(NotifyRules.shouldAlert(false, false, muted = true, globalMuted = false))
        assertTrue(NotifyRules.shouldAlert(false, false, muted = false, globalMuted = false))
    }

    @Test
    fun incomingWireRingAlwaysEmitsRingInAndOverlay() {
        val m = CallMachine()
        val ring = m.onWire("alice", CallSignal.RING, "c1", "", "bob")
        assertTrue(ring.contains(CallEffect.RingIn))
        assertTrue(ring.contains(CallEffect.NotifyIncoming))
        assertTrue(CallToneRules.shouldRingIncoming(globalMuted = true, chatMuted = true))
        assertTrue(CallToneRules.shouldNotifyIncoming(globalMuted = true))
        assertTrue(CallToneRules.shouldPlayRing(m.state.phase, m.state.link, m.state.mediaUp))
        assertEquals(CallToneKind.RING_IN, CallToneRules.kind(m.state.phase, m.state.link, m.state.mediaUp))
    }

    @Test
    fun outgoingStartAlwaysEmitsRingOutEvenIfGloballyMuted() {
        val m = CallMachine()
        val start = m.localStart("c1", "bob", "alice")
        assertTrue(start.contains(CallEffect.RingOut))
        assertTrue(CallToneRules.shouldRingOutgoing(globalMuted = true))
        assertTrue(CallToneRules.shouldPlayRing(m.state.phase, m.state.link, m.state.mediaUp))
        assertEquals(CallToneKind.RING_OUT, CallToneRules.kind(m.state.phase, m.state.link, m.state.mediaUp))
    }

    @Test
    fun tonePlaysOnlyWhileRingingNotAfterMediaUp() {
        assertTrue(
            CallToneRules.shouldPlayRing(CallPhase.RINGING_OUT, CallLinkState.RINGING, mediaUp = false),
        )
        assertTrue(
            CallToneRules.shouldPlayRing(CallPhase.RINGING_IN, CallLinkState.RINGING, mediaUp = false),
        )
        assertFalse(
            CallToneRules.shouldPlayRing(CallPhase.ACTIVE, CallLinkState.CONNECTING, mediaUp = false),
        )
        assertFalse(
            CallToneRules.shouldPlayRing(CallPhase.ACTIVE, CallLinkState.CONNECTED, mediaUp = true),
        )
        assertFalse(
            CallToneRules.shouldPlayRing(CallPhase.RINGING_OUT, CallLinkState.FAILED, mediaUp = false),
        )
        assertEquals(
            CallToneKind.NONE,
            CallToneRules.kind(CallPhase.ACTIVE, CallLinkState.CONNECTED, mediaUp = true),
        )
        val m = CallMachine()
        m.localStart("c1", "bob", "alice")
        assertTrue(CallToneRules.shouldPlayRing(m.state.phase, m.state.link, m.state.mediaUp))
        m.onWire("bob", CallSignal.ACCEPT, "c1", "", "alice")
        assertEquals(CallPhase.ACTIVE, m.state.phase)
        assertFalse(CallToneRules.shouldPlayRing(m.state.phase, m.state.link, m.state.mediaUp))
        m.onHasTurn(true, "")
        m.onSessionAttached()
        m.onLocalOfferSent()
        m.onIce("CONNECTED", true)
        assertTrue(m.state.mediaUp)
        assertFalse(CallToneRules.shouldPlayRing(m.state.phase, m.state.link, m.state.mediaUp))
        m.onIce("DISCONNECTED", true)
        assertFalse(CallToneRules.shouldPlayRing(m.state.phase, m.state.link, m.state.mediaUp))
        assertEquals(CallToneKind.NONE, CallToneRules.kind(m.state.phase, m.state.link, m.state.mediaUp))
    }

    @Test
    fun iceBlipDoesNotSetRingingOrRestartRing() {
        for (name in listOf("DISCONNECTED", "FAILED", "CHECKING", "CONNECTING", "NEW")) {
            assertFalse(CallToneRules.iceBlipSetsRinging(name, mediaUp = true))
            assertFalse(CallToneRules.iceBlipRestartsRing(name, mediaUp = true))
            assertFalse(CallToneRules.iceBlipSetsRinging(name, mediaUp = false))
            assertFalse(CallToneRules.iceBlipRestartsRing(name, mediaUp = false))
        }
    }
}
