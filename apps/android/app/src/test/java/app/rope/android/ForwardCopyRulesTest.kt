package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.ForwardCopyRules
import app.rope.android.data.ForwardRules
import app.rope.android.data.GroupTextPayload
import app.rope.android.data.LocalStore
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.TextBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForwardCopyRulesTest {
    private fun msg(
        outgoing: Boolean = false,
        senderName: String = "Анна",
        forwardedFrom: String? = null,
        kind: MessageKind = MessageKind.TEXT,
        text: String = "hi",
    ) = ChatMessage(
        id = "m",
        peerDeviceId = "p",
        outgoing = outgoing,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1,
        kind = kind,
        senderName = senderName,
        forwardedFrom = forwardedFrom,
    )

    @Test
    fun hideOmitsStampAndFf() {
        val src = msg()
        assertEquals("Анна", ForwardCopyRules.stampName(src, "Я", hideSender = false))
        assertNull(ForwardCopyRules.stampName(src, "Я", hideSender = true))
        assertEquals("Скрыть отправителя", ForwardCopyRules.CHIP)
        assertFalse(ForwardCopyRules.packsFf(null))
        assertFalse(ForwardCopyRules.packsFf("  "))
        assertTrue(ForwardCopyRules.packsFf("Анна"))
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun hideKeepsOriginWhenOff() {
        val mine = msg(outgoing = true, senderName = "Я")
        assertEquals("Я", ForwardCopyRules.stampName(mine, "Я", hideSender = false))
        assertNull(ForwardCopyRules.stampName(mine, "Я", hideSender = true))
        assertEquals("сообщение", ForwardCopyRules.stampName(msg(senderName = ""), "", hideSender = false))
        assertEquals("Анна", ForwardRules.originName(msg(), "Я"))
    }

    @Test
    fun textBodyOmitsFfWhenHidden() {
        val shown = TextBody.encode("привет", null, "", "", forwardedFrom = "Анна")
        assertTrue(shown.contains("\"ff\""))
        assertEquals("Анна", TextBody.decode(shown).forwardedFrom)
        val hidden = TextBody.encode(
            "привет",
            null,
            "",
            "",
            forwardedFrom = ForwardCopyRules.stampName(msg(), "Я", hideSender = true),
        )
        assertFalse(hidden.contains("\"ff\""))
        assertEquals("привет", hidden)
        assertNull(TextBody.decode(hidden).forwardedFrom)
    }

    @Test
    fun mediaAndGroupOmitFfWhenHidden() {
        val shown = MediaPayload("image", "o", "ab", "KEY", "image/jpeg", "p.jpg", 1, forwardedFrom = "Анна")
        assertTrue(shown.toJson().contains("\"ff\""))
        val hidden = shown.copy(forwardedFrom = ForwardCopyRules.stampName(msg(), "Я", hideSender = true))
        assertFalse(hidden.toJson().contains("\"ff\""))
        assertNull(MediaPayload.parse(hidden.toJson()).forwardedFrom)
        val group = GroupTextPayload.parse(
            GroupTextPayload(
                "g",
                "hi",
                1,
                forwardedFrom = ForwardCopyRules.stampName(msg(), "Я", hideSender = true),
            ).toJson(),
        )
        assertNull(group.forwardedFrom)
        assertFalse(GroupTextPayload("g", "hi", 1, forwardedFrom = null).toJson().contains("\"ff\""))
    }
}
