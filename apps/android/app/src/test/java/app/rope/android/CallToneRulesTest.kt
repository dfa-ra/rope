package app.rope.android

import app.rope.android.data.CallEffect
import app.rope.android.data.CallMachine
import app.rope.android.data.CallSignal
import app.rope.android.data.CallToneRules
import app.rope.android.data.NotifyRules
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
    }

    @Test
    fun outgoingStartAlwaysEmitsRingOutEvenIfGloballyMuted() {
        val m = CallMachine()
        val start = m.localStart("c1", "bob", "alice")
        assertTrue(start.contains(CallEffect.RingOut))
        assertTrue(CallToneRules.shouldRingOutgoing(globalMuted = true))
    }
}
