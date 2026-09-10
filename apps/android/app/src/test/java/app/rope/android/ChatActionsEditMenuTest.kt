package app.rope.android

import app.rope.android.data.ChatActions
import app.rope.android.data.ChatMessage
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatActionsEditMenuTest {
    @Test
    fun editLabelAndOnlyLiveOutgoingText() {
        val incoming = ChatMessage("1", "p", false, "hi", MessageStatus.DELIVERED_TO_DEVICE, 1L)
        val mine = incoming.copy(id = "2", outgoing = true)
        assertEquals("Изменить", ChatActions.EDIT)
        assertTrue(ChatActions.canEdit(mine))
        assertFalse(ChatActions.canEdit(incoming))
        assertFalse(ChatActions.canEdit(mine.copy(deleted = true)))
        assertFalse(ChatActions.canEdit(mine.copy(kind = MessageKind.IMAGE)))
        assertTrue(ChatActions.canEdit(mine.copy(kind = MessageKind.GROUP_TEXT)))
        assertEquals(6, LocalStore.VERSION)
    }
}
