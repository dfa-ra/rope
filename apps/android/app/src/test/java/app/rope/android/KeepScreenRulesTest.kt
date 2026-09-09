package app.rope.android

import app.rope.android.data.CallPhase
import app.rope.android.data.KeepScreenRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepScreenRulesTest {
    @Test
    fun chatHoldNeedsSettingAndOpenChat() {
        assertFalse(KeepScreenRules.shouldHold(inChat = true, phase = null, chatEnabled = false))
        assertFalse(KeepScreenRules.shouldHold(inChat = false, phase = null, chatEnabled = true))
        assertTrue(KeepScreenRules.shouldHold(inChat = true, phase = null, chatEnabled = true))
    }

    @Test
    fun liveCallAlwaysHoldsEvenIfChatSettingOff() {
        assertTrue(KeepScreenRules.shouldHold(inChat = false, phase = CallPhase.RINGING_IN, chatEnabled = false))
        assertTrue(KeepScreenRules.shouldHold(inChat = false, phase = CallPhase.RINGING_OUT, chatEnabled = false))
        assertTrue(KeepScreenRules.shouldHold(inChat = true, phase = CallPhase.ACTIVE, chatEnabled = false))
        assertFalse(KeepScreenRules.shouldHold(inChat = false, phase = CallPhase.ENDED, chatEnabled = false))
    }

    @Test
    fun endedCallIsNotLive() {
        assertFalse(KeepScreenRules.inLiveCall(null))
        assertFalse(KeepScreenRules.inLiveCall(CallPhase.ENDED))
        assertTrue(KeepScreenRules.inLiveCall(CallPhase.ACTIVE))
        assertEquals("Не выключать экран", KeepScreenRules.TITLE)
    }
}
