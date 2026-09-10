package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.ComposerHintKind
import app.rope.android.data.ComposerRules
import app.rope.android.data.ForwardCommentRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForwardCommentRulesTest {
    @Test
    fun pickerIsNotCommentAndDestChatIs() {
        val src = msg()
        assertTrue(ForwardCommentRules.picking(false, src))
        assertFalse(ForwardCommentRules.commenting(false, src))
        assertFalse(ForwardCommentRules.picking(true, src))
        assertTrue(ForwardCommentRules.commenting(true, src))
        assertFalse(ForwardCommentRules.picking(false, null))
        assertFalse(ForwardCommentRules.commenting(true, null))
        assertEquals(BackLayer.CancelForward, BackStack.decide(UiState(screen = Screen.Chats, forwarding = src)))
        assertEquals(
            BackLayer.CancelForward,
            BackStack.decide(UiState(screen = Screen.Chat, backStack = listOf(Screen.Chats, Screen.Chat), forwarding = src)),
        )
    }

    @Test
    fun hintAndSendShowCommentThenForward() {
        val hint = ForwardCommentRules.hint("привет мир")
        assertEquals(ComposerHintKind.FORWARD, hint.kind)
        assertEquals("Переслать", hint.title)
        assertEquals("привет мир", hint.body)
        assertEquals("Отменить пересылку", hint.dismissContentDescription)
        assertTrue(ForwardCommentRules.sendComment("  комментарий "))
        assertFalse(ForwardCommentRules.sendComment("  "))
        assertTrue(ComposerRules.showSendButton("", false, pendingForward = true))
        assertFalse(ComposerRules.showSendButton("", false, pendingForward = false))
        assertFalse(ComposerRules.showMicButton("", false, pendingForward = true))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }

    private fun msg() = ChatMessage(
        id = "m",
        peerDeviceId = "p",
        outgoing = false,
        text = "hi",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = MessageKind.TEXT,
    )
}
