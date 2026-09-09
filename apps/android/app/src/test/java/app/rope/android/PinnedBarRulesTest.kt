package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatPrefs
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.PinnedBarRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinnedBarRulesTest {
    @Test
    fun barShowsForLivePinInDmAndGroup() {
        val dm = text("m1", "встреча в 19:00")
        val group = dm.copy(kind = MessageKind.GROUP_TEXT, peerDeviceId = "g:g1", groupId = "g1")
        assertTrue(PinnedBarRules.visible(dm, hiddenId = null))
        assertTrue(PinnedBarRules.visible(group, hiddenId = null))
        assertEquals("m1", PinnedBarRules.jumpId(dm))
        val copy = PinnedBarRules.copy(dm)
        assertEquals("Закреплённое сообщение", copy.title)
        assertEquals("встреча в 19:00", copy.body)
        assertEquals("m1", copy.jumpId)
        assertEquals("Перейти к закреплённому", copy.jumpContentDescription)
        assertEquals("Скрыть закреплённое", copy.dismissContentDescription)
        assertFalse(copy.dismissContentDescription.contains("Открепить"))
        assertEquals(dm, PinnedBarRules.message(listOf(dm, text("m2", "later")), "m1"))
        assertNull(PinnedBarRules.message(listOf(dm), "missing"))
        assertNull(PinnedBarRules.message(listOf(dm), null))
    }

    @Test
    fun deletedOrMissingPinHidesTheBar() {
        val gone = text("m1", "bye", deleted = true)
        assertFalse(PinnedBarRules.visible(gone, hiddenId = null))
        assertNull(PinnedBarRules.jumpId(gone))
        assertEquals("Сообщение удалено", PinnedBarRules.body(gone))
        assertNull(PinnedBarRules.message(listOf(gone), "m1"))
        assertFalse(PinnedBarRules.visible(null, hiddenId = null))
        assertNull(PinnedBarRules.jumpId(null))
        assertFalse(PinnedBarRules.visible(text("  ", "x").copy(id = "  "), hiddenId = null))
    }

    @Test
    fun closeHidesLocallyWithoutClearingThePin() {
        val pin = text("m1", "закреп")
        assertEquals("m1", PinnedBarRules.hide("m1"))
        assertTrue(PinnedBarRules.isHidden("m1", "m1"))
        assertFalse(PinnedBarRules.visible(pin, hiddenId = "m1"))
        assertFalse(PinnedBarRules.isHidden("m1", "m2"))
        assertFalse(PinnedBarRules.isHidden("m1", null))
        assertFalse(PinnedBarRules.isHidden(null, "m1"))
        assertNull(PinnedBarRules.hide("  "))
        assertNull(PinnedBarRules.hide(null))
        assertEquals("m1", PinnedBarRules.jumpId(pin))
    }

    @Test
    fun newPinUnhidesTheBarSamePinKeepsHide() {
        assertNull(PinnedBarRules.hiddenAfterPinChange("m2", previousHidden = "m1"))
        assertNull(PinnedBarRules.hiddenAfterPinChange(null, previousHidden = "m1"))
        assertEquals("m1", PinnedBarRules.hiddenAfterPinChange("m1", previousHidden = "m1"))
        assertNull(PinnedBarRules.hiddenAfterPinChange("m1", previousHidden = null))
        val pin = text("m2", "новое")
        assertTrue(PinnedBarRules.visible(pin, hiddenId = PinnedBarRules.hiddenAfterPinChange("m2", "m1")))
    }

    @Test
    fun photoWithoutCaptionUsesMediaPreview() {
        val extra = MediaPayload(
            kind = "image",
            objectId = "o",
            sha256 = "s",
            keyB64 = "k",
            mime = "image/jpeg",
            name = "p.jpg",
            size = 1,
        ).toJson()
        val photo = text("p1", "", kind = MessageKind.IMAGE, extra = extra)
        assertEquals("Фото", PinnedBarRules.body(photo))
        assertTrue(PinnedBarRules.visible(photo, hiddenId = null))
        val copy = PinnedBarRules.copy(photo)
        assertEquals("Закреплённое сообщение", copy.title)
        assertEquals("Фото", copy.body)
    }

    @Test
    fun multilineAndLongPreviewClipLikeComposer() {
        assertEquals("первая вторая", PinnedBarRules.clip("первая\nвторая"))
        assertFalse(PinnedBarRules.clip("a\nb").contains('\n'))
        assertEquals("Сообщение", PinnedBarRules.clip("  \n\t "))
        val q = "я".repeat(90)
        val body = PinnedBarRules.clip(q)
        assertTrue(body.endsWith("…"))
        assertEquals(PinnedBarRules.BODY_MAX, body.length)
        val longMsg = text("m", q)
        assertEquals(body, PinnedBarRules.body(longMsg))
    }

    @Test
    fun chatPrefsHideRoundTripsWithoutLocalStoreBump() {
        val prefs = ChatPrefs.parse(
            ChatPrefs(pinnedMessageId = "m1", hiddenPinnedMessageId = "m1").toJson(),
        )
        assertEquals("m1", prefs.pinnedMessageId)
        assertEquals("m1", prefs.hiddenPinnedMessageId)
        val legacy = ChatPrefs.parse(
            """{"pinned":false,"muted":false,"unread":0,"draft":"","pinned_message":"m9","archived":false}""",
        )
        assertEquals("m9", legacy.pinnedMessageId)
        assertNull(legacy.hiddenPinnedMessageId)
        val cleared = ChatPrefs.parse(ChatPrefs(pinnedMessageId = "m1").toJson())
        assertNull(cleared.hiddenPinnedMessageId)
    }

    private fun text(
        id: String,
        text: String,
        deleted: Boolean = false,
        kind: MessageKind = MessageKind.TEXT,
        extra: String = "",
    ) = ChatMessage(
        id = id,
        peerDeviceId = "peer",
        outgoing = false,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = kind,
        extra = extra,
        deleted = deleted,
        senderId = "peer",
    )
}
