package app.rope.android

import app.rope.android.data.SendTypingRules
import app.rope.android.data.TypingRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SendTypingRulesTest {
    @Test
    fun offNeverSendsEvenWithDraft() {
        assertFalse(SendTypingRules.shouldSend(enabled = false, lastSentAt = 0, now = 3_000, draft = "hi"))
        assertFalse(SendTypingRules.shouldSend(enabled = false, lastSentAt = 0, now = 3_000, draft = ""))
    }

    @Test
    fun onDefersToTypingThrottle() {
        assertTrue(SendTypingRules.shouldSend(enabled = true, lastSentAt = 0, now = 3_000, draft = "hi"))
        assertFalse(SendTypingRules.shouldSend(enabled = true, lastSentAt = 2_500, now = 3_000, draft = "hi"))
        assertFalse(SendTypingRules.shouldSend(enabled = true, lastSentAt = 0, now = 3_000, draft = ""))
        assertTrue(TypingRules.shouldSend(0, 3_000, "hi"))
    }

    @Test
    fun copyStaysLocal() {
        assertEquals("Печатает…", SendTypingRules.TITLE)
        assertTrue(SendTypingRules.hint().contains("Сервер"))
        assertFalse(SendTypingRules.hint().contains("FCM"))
    }
}
