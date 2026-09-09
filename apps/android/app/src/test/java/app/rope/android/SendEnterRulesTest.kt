package app.rope.android

import app.rope.android.data.SendEnterRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SendEnterRulesTest {
    @Test
    fun defaultImeIsNewline() {
        assertFalse(SendEnterRules.imeIsSend(false))
        assertFalse(SendEnterRules.imeSends(false, "привет", recording = false))
        assertFalse(SendEnterRules.imeSends(false, "", recording = false, pendingMedia = true))
    }

    @Test
    fun enterSendsDraftOrPendingMedia() {
        assertTrue(SendEnterRules.imeIsSend(true))
        assertTrue(SendEnterRules.imeSends(true, "привет", recording = false))
        assertTrue(SendEnterRules.imeSends(true, "", recording = false, pendingMedia = true))
        assertFalse(SendEnterRules.imeSends(true, "  ", recording = false))
        assertFalse(SendEnterRules.imeSends(true, "", recording = false, pendingMedia = false))
    }

    @Test
    fun enterDoesNotSendWhileRecording() {
        assertFalse(SendEnterRules.imeSends(true, "привет", recording = true))
        assertFalse(SendEnterRules.imeSends(true, "", recording = true, recordingLocked = true))
    }

    @Test
    fun copyKeepsLogo() {
        assertEquals("Отправка по Enter", SendEnterRules.TITLE)
        assertEquals("Чат", SendEnterRules.SECTION)
        assertTrue(SendEnterRules.hint().contains("логотипа"))
        assertFalse(SendEnterRules.hint().contains("FCM"))
    }
}
