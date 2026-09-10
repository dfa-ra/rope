package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.ViewerJumpRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerJumpRulesTest {
    @Test
    fun inheritStoreAndVersion() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertEquals("В чате", ViewerJumpRules.ACTION)
        assertFalse(ViewerJumpRules.ACTION.contains("FCM", ignoreCase = true))
    }

    @Test
    fun jumpsCurrentPageNotDeleted() {
        val photo = img("a", deleted = false)
        val gone = img("b", deleted = true)
        assertTrue(ViewerJumpRules.canJump(photo))
        assertFalse(ViewerJumpRules.canJump(gone))
        assertFalse(ViewerJumpRules.canJump(photo.copy(id = "")))
        val album = listOf(photo, gone)
        assertSame(photo, ViewerJumpRules.current(album, 0, gone))
        assertSame(gone, ViewerJumpRules.current(album, 1, photo))
        assertSame(photo, ViewerJumpRules.current(emptyList(), 0, photo))
    }

    private fun img(id: String, deleted: Boolean) = ChatMessage(
        id = id,
        peerDeviceId = "peer",
        outgoing = false,
        text = "",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = MessageKind.IMAGE,
        deleted = deleted,
    )
}
