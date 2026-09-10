package app.rope.android

import app.rope.android.data.ChatActions
import app.rope.android.data.ChatMessage
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.ViewerDelRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerDelRulesTest {
    @Test
    fun inheritStoreAndVersion() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertEquals("Удалить", ViewerDelRules.ACTION)
        assertFalse(ViewerDelRules.ACTION.contains("FCM", ignoreCase = true))
    }

    @Test
    fun deletesOutgoingCurrentPageOnly() {
        val mine = img("a", outgoing = true, deleted = false)
        val theirs = img("b", outgoing = false, deleted = false)
        val gone = img("c", outgoing = true, deleted = true)
        assertTrue(ViewerDelRules.canDelete(mine))
        assertFalse(ViewerDelRules.canDelete(theirs))
        assertFalse(ViewerDelRules.canDelete(gone))
        assertEquals(ChatActions.canDelete(mine), ViewerDelRules.canDelete(mine))
        val album = listOf(mine, theirs)
        assertSame(mine, ViewerDelRules.current(album, 0, theirs))
        assertSame(theirs, ViewerDelRules.current(album, 1, mine))
        assertSame(mine, ViewerDelRules.current(emptyList(), 0, mine))
    }

    private fun img(id: String, outgoing: Boolean, deleted: Boolean) = ChatMessage(
        id = id,
        peerDeviceId = "peer",
        outgoing = outgoing,
        text = "",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = MessageKind.IMAGE,
        deleted = deleted,
    )
}
