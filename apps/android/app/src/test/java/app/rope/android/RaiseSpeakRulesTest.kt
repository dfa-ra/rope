package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.RaiseSpeakAction
import app.rope.android.data.RaiseSpeakRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RaiseSpeakRulesTest {
    @Test
    fun localStoreStaysV6AndDefaultsOn() {
        assertEquals(6, LocalStore.VERSION)
        assertTrue(RaiseSpeakRules.enabledFromKv(null))
        assertTrue(RaiseSpeakRules.enabledFromKv("1"))
        assertFalse(RaiseSpeakRules.enabledFromKv("0"))
        assertEquals("1", RaiseSpeakRules.persist(true))
        assertEquals("0", RaiseSpeakRules.persist(false))
        assertEquals("Говорить, поднеся к уху", RaiseSpeakRules.TITLE)
        assertEquals("Чат", RaiseSpeakRules.SECTION)
        assertEquals("raise_speak", RaiseSpeakRules.KV)
        assertTrue(RaiseSpeakRules.hint().contains("голосовое"))
    }

    @Test
    fun nearIsEarNotCallRange() {
        assertTrue(RaiseSpeakRules.isNear(0f, 5f))
        assertTrue(RaiseSpeakRules.isNear(3f, 8f))
        assertFalse(RaiseSpeakRules.isNear(5f, 8f))
        assertFalse(RaiseSpeakRules.isNear(-1f, 8f))
        assertTrue(RaiseSpeakRules.isNear(0.5f, 1f))
        assertFalse(RaiseSpeakRules.isNear(1f, 1f))
    }

    @Test
    fun raisedPoseIsPortraitNotFlat() {
        assertTrue(RaiseSpeakRules.isRaisedPose(0f, 9.8f, 0f))
        assertFalse(RaiseSpeakRules.isRaisedPose(0f, 0f, 9.8f))
        assertFalse(RaiseSpeakRules.isRaisedPose(0f, 0f, 0f))
        assertTrue(RaiseSpeakRules.atEar(near = true, raised = null))
        assertFalse(RaiseSpeakRules.atEar(near = true, raised = false))
        assertTrue(RaiseSpeakRules.atEar(near = true, raised = true))
        assertFalse(RaiseSpeakRules.atEar(near = false, raised = true))
    }

    @Test
    fun startSendCancelStayOffHoldAndCall() {
        assertEquals(
            RaiseSpeakAction.START,
            RaiseSpeakRules.action(
                enabled = true,
                inChat = true,
                foreground = true,
                recording = false,
                raiseSession = false,
                recordingVideoNote = false,
                liveCall = false,
                atEar = true,
                sawAway = true,
                hasMic = true,
            ),
        )
        assertEquals(
            RaiseSpeakAction.NONE,
            RaiseSpeakRules.action(
                enabled = true,
                inChat = true,
                foreground = true,
                recording = false,
                raiseSession = false,
                recordingVideoNote = false,
                liveCall = false,
                atEar = true,
                sawAway = false,
                hasMic = true,
            ),
        )
        assertEquals(
            RaiseSpeakAction.SEND,
            RaiseSpeakRules.action(
                enabled = true,
                inChat = true,
                foreground = true,
                recording = true,
                raiseSession = true,
                recordingVideoNote = false,
                liveCall = false,
                atEar = false,
                sawAway = true,
                hasMic = true,
            ),
        )
        assertEquals(
            RaiseSpeakAction.NONE,
            RaiseSpeakRules.action(
                enabled = true,
                inChat = true,
                foreground = true,
                recording = true,
                raiseSession = false,
                recordingVideoNote = false,
                liveCall = false,
                atEar = false,
                sawAway = true,
                hasMic = true,
            ),
        )
        assertEquals(
            RaiseSpeakAction.CANCEL,
            RaiseSpeakRules.action(
                enabled = true,
                inChat = false,
                foreground = true,
                recording = true,
                raiseSession = true,
                recordingVideoNote = false,
                liveCall = false,
                atEar = true,
                sawAway = true,
                hasMic = true,
            ),
        )
        assertEquals(
            RaiseSpeakAction.NONE,
            RaiseSpeakRules.action(
                enabled = true,
                inChat = true,
                foreground = true,
                recording = false,
                raiseSession = false,
                recordingVideoNote = false,
                liveCall = true,
                atEar = true,
                sawAway = true,
                hasMic = true,
            ),
        )
        assertFalse(
            RaiseSpeakRules.shouldListen(
                enabled = true,
                inChat = true,
                liveCall = true,
                foreground = true,
            ),
        )
        assertTrue(
            RaiseSpeakRules.shouldListen(
                enabled = true,
                inChat = true,
                liveCall = false,
                foreground = true,
            ),
        )
    }
}
