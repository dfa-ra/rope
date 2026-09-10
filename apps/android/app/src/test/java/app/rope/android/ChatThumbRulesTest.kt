package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatThumbRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatThumbRulesTest {
    private fun msg(
        kind: MessageKind,
        path: String? = "/tmp/a.jpg",
        deleted: Boolean = false,
    ) = ChatMessage(
        "m",
        "p",
        false,
        "",
        MessageStatus.DELIVERED_TO_DEVICE,
        1L,
        kind = kind,
        localPath = path,
        deleted = deleted,
    )

    @Test
    fun onlyCachedImageVideoAndNote() {
        assertTrue(ChatThumbRules.shows(msg(MessageKind.IMAGE)))
        assertTrue(ChatThumbRules.shows(msg(MessageKind.VIDEO, "/tmp/a.mp4")))
        assertTrue(ChatThumbRules.shows(msg(MessageKind.VIDEO_NOTE, "/tmp/n.mp4")))
        assertFalse(ChatThumbRules.shows(null))
        assertFalse(ChatThumbRules.shows(msg(MessageKind.TEXT)))
        assertFalse(ChatThumbRules.shows(msg(MessageKind.VOICE, "/tmp/a.ogg")))
        assertFalse(ChatThumbRules.shows(msg(MessageKind.IMAGE, path = null)))
        assertFalse(ChatThumbRules.shows(msg(MessageKind.IMAGE, path = "  ")))
        assertFalse(ChatThumbRules.shows(msg(MessageKind.IMAGE, deleted = true)))
        assertTrue(ChatThumbRules.isVideo(MessageKind.VIDEO))
        assertTrue(ChatThumbRules.isVideo(MessageKind.VIDEO_NOTE))
        assertFalse(ChatThumbRules.isVideo(MessageKind.IMAGE))
        assertEquals(48, ChatThumbRules.SIZE_DP)
        assertEquals(6, LocalStore.VERSION)
    }
}
