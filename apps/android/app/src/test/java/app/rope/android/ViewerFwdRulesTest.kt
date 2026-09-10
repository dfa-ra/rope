package app.rope.android

import app.rope.android.data.ChatActions
import app.rope.android.data.ChatMessage
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.ViewerFwdRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerFwdRulesTest {
    @Test
    fun inheritStoreAndVersion() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertEquals("Переслать", ViewerFwdRules.ACTION)
        assertFalse(ViewerFwdRules.ACTION.contains("FCM", ignoreCase = true))
    }

    @Test
    fun forwardsCurrentPageAndSkipsDeleted() {
        val photo = img("a", MessageKind.IMAGE, deleted = false)
        val video = img("b", MessageKind.VIDEO, deleted = false)
        val gone = img("c", MessageKind.IMAGE, deleted = true)
        assertTrue(ViewerFwdRules.canForward(photo))
        assertTrue(ViewerFwdRules.canForward(video))
        assertFalse(ViewerFwdRules.canForward(gone))
        assertTrue(ChatActions.canForward(photo))
        val album = listOf(photo, video)
        assertSame(photo, ViewerFwdRules.current(album, 0, video))
        assertSame(video, ViewerFwdRules.current(album, 1, photo))
        assertSame(photo, ViewerFwdRules.current(album, 9, photo))
        assertSame(photo, ViewerFwdRules.current(emptyList(), 0, photo))
    }

    private fun img(id: String, kind: MessageKind, deleted: Boolean) = ChatMessage(
        id = id,
        peerDeviceId = "peer",
        outgoing = false,
        text = "",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = kind,
        deleted = deleted,
    )
}
