package app.rope.android

import app.rope.android.data.CallInfo
import app.rope.android.data.CallLinkState
import app.rope.android.data.CallPhase
import app.rope.android.data.LocalStore
import app.rope.android.data.RaiseSpeakRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RaiseSpeakRulesTest {
    private fun call(phase: CallPhase, video: Boolean = false) = CallInfo(
        callId = "c1",
        peerDeviceId = "peer",
        peerName = "Ada",
        outgoing = true,
        phase = phase,
        link = CallLinkState.CONNECTED,
        video = video,
    )

    @Test
    fun listensOnlyOnActiveAudioCall() {
        assertFalse(RaiseSpeakRules.listen(null))
        assertFalse(RaiseSpeakRules.listen(call(CallPhase.RINGING_IN)))
        assertFalse(RaiseSpeakRules.listen(call(CallPhase.RINGING_OUT)))
        assertFalse(RaiseSpeakRules.listen(call(CallPhase.ENDED)))
        assertFalse(RaiseSpeakRules.listen(call(CallPhase.ACTIVE, video = true)))
        assertTrue(RaiseSpeakRules.listen(call(CallPhase.ACTIVE)))
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun nearUsesEarpieceWithoutClearingUserSpeakerPref() {
        assertTrue(RaiseSpeakRules.speakerOn(userSpeakerOn = true, proximityNear = false, listening = true))
        assertFalse(RaiseSpeakRules.speakerOn(userSpeakerOn = true, proximityNear = true, listening = true))
        assertFalse(RaiseSpeakRules.speakerOn(userSpeakerOn = false, proximityNear = true, listening = true))
        assertTrue(RaiseSpeakRules.speakerOn(userSpeakerOn = true, proximityNear = true, listening = false))
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun proximityNearIsBelowMaxRange() {
        assertTrue(RaiseSpeakRules.isNear(0f, 5f))
        assertFalse(RaiseSpeakRules.isNear(5f, 5f))
        assertTrue(RaiseSpeakRules.isNear(0f, 1f))
        assertFalse(RaiseSpeakRules.isNear(1f, 1f))
        assertEquals(6, LocalStore.VERSION)
    }
}
