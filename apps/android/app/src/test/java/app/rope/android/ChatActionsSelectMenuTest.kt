package app.rope.android

import app.rope.android.data.ChatActions
import app.rope.android.data.ChatMessage
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatActionsSelectMenuTest {
    @Test
    fun selectIsLiveMessagesOnly() {
        val live = ChatMessage("1", "p", false, "hi", MessageStatus.DELIVERED_TO_DEVICE, 1L)
        assertEquals("Выбрать", ChatActions.SELECT)
        assertTrue(ChatActions.canSelect(live))
        assertTrue(ChatActions.canSelect(live.copy(outgoing = true)))
        assertFalse(ChatActions.canSelect(live.copy(deleted = true)))
        assertEquals(6, LocalStore.VERSION)
    }
}
