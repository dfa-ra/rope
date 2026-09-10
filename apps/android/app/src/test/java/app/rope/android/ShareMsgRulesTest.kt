package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.ShareMsgRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareMsgRulesTest {
    @Test
    fun textAndGroupTextShareTrimmedBody() {
        assertEquals("привет", ShareMsgRules.body(msg("  привет  ")))
        assertTrue(ShareMsgRules.canShare(msg("привет", MessageKind.GROUP_TEXT)))
        assertEquals("Поделиться", ShareMsgRules.ACTION)
        assertEquals("Поделиться", ShareMsgRules.CHOOSER)
        assertEquals("android.intent.action.SEND", ShareMsgRules.SEND_ACTION)
        assertEquals("android.intent.extra.TEXT", ShareMsgRules.EXTRA_TEXT)
        assertEquals("text/plain", ShareMsgRules.MIME)
        val long = "a".repeat(ShareMsgRules.BODY_MAX + 8)
        val clipped = ShareMsgRules.body(msg(long))!!
        assertTrue(clipped.endsWith("…"))
        assertEquals(ShareMsgRules.BODY_MAX, clipped.length)
    }

    @Test
    fun rejectsMediaDeletedNulAndBlank() {
        assertFalse(ShareMsgRules.canShare(msg("hi", MessageKind.IMAGE)))
        assertFalse(ShareMsgRules.canShare(msg("hi", MessageKind.VIDEO)))
        assertFalse(ShareMsgRules.canShare(msg("hi", MessageKind.FILE)))
        assertFalse(ShareMsgRules.canShare(msg("hi", MessageKind.VOICE)))
        assertFalse(ShareMsgRules.canShare(msg("hi", deleted = true)))
        assertFalse(ShareMsgRules.canShare(msg("  ")))
        assertNull(ShareMsgRules.body(msg("hi\u0000")))
        assertEquals(6, LocalStore.VERSION)
    }

    private fun msg(
        text: String,
        kind: MessageKind = MessageKind.TEXT,
        deleted: Boolean = false,
    ) = ChatMessage(
        id = "m1",
        peerDeviceId = "p",
        outgoing = false,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = kind,
        deleted = deleted,
    )
}
