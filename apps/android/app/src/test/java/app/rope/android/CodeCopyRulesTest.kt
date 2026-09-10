package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.CodeCopyRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeCopyRulesTest {
    @Test
    fun fenceAndTickInnerPayload() {
        assertEquals("Копировать код", CodeCopyRules.LABEL)
        assertEquals("Скопировано", CodeCopyRules.NOTICE)
        assertEquals("hello", CodeCopyRules.payload("```\nhello\n```"))
        assertEquals("val x = 1", CodeCopyRules.payload("```kotlin\nval x = 1\n```"))
        assertEquals("hello", CodeCopyRules.payload("```hello```"))
        assertEquals("rest", CodeCopyRules.payload("```\nrest"))
        assertEquals("inner", CodeCopyRules.payload("see `inner` here"))
        assertNull(CodeCopyRules.payload("plain text"))
        assertNull(CodeCopyRules.payload("```\n\n```"))
        assertNull(CodeCopyRules.payload("bad\u0000```\nok\n```"))
        val msg = text("```\nfun n()\n```")
        assertTrue(CodeCopyRules.canCopy(msg))
        assertEquals("fun n()", CodeCopyRules.payload(msg))
        assertTrue(CodeCopyRules.canCopy(msg.copy(kind = MessageKind.GROUP_TEXT)))
        assertFalse(CodeCopyRules.canCopy(msg.copy(deleted = true)))
        assertFalse(CodeCopyRules.canCopy(msg.copy(kind = MessageKind.IMAGE)))
        assertFalse(CodeCopyRules.LABEL.contains("FCM", ignoreCase = true))
        assertEquals("0.3.52", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(76, BuildConfig.VERSION_CODE)
        assertEquals(6, LocalStore.VERSION)
    }

    private fun text(body: String) = ChatMessage(
        id = "m",
        peerDeviceId = "p",
        outgoing = false,
        text = body,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = MessageKind.TEXT,
    )
}
