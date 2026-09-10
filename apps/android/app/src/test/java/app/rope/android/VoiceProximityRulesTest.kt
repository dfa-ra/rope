package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.VoiceProximityAction
import app.rope.android.data.VoiceProximityRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceProximityRulesTest {
    @Test
    fun inheritStoreAndNear() {
        assertEquals(6, LocalStore.VERSION)
        assertTrue(VoiceProximityRules.isNear(0f, 5f))
        assertTrue(VoiceProximityRules.isNear(3f, 8f))
        assertFalse(VoiceProximityRules.isNear(5f, 8f))
        assertFalse(VoiceProximityRules.isNear(-1f, 8f))
        assertTrue(VoiceProximityRules.isNear(0.5f, 1f))
        assertFalse(VoiceProximityRules.isNear(1f, 1f))
    }

    @Test
    fun pauseAtEarAndResumeWhenAway() {
        assertEquals(
            VoiceProximityAction.PAUSE,
            VoiceProximityRules.action(playing = true, pausedByProximity = false, near = true),
        )
        assertEquals(
            VoiceProximityAction.NONE,
            VoiceProximityRules.action(playing = false, pausedByProximity = false, near = true),
        )
        assertEquals(
            VoiceProximityAction.RESUME,
            VoiceProximityRules.action(playing = false, pausedByProximity = true, near = false),
        )
        assertEquals(
            VoiceProximityAction.NONE,
            VoiceProximityRules.action(playing = true, pausedByProximity = false, near = false),
        )
        assertFalse(VoiceProximityRules.afterUserToggle())
    }
}
