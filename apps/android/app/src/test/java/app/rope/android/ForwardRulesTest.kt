package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.ForwardRules
import app.rope.android.data.GroupTextPayload
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageMeta
import app.rope.android.data.MessageStatus
import app.rope.android.data.PackedText
import app.rope.android.data.TextBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForwardRulesTest {
    @Test
    fun headerIsForwardedFromNotMasqueradingReply() {
        assertEquals("Переслано от Анна", ForwardRules.headerLabel("Анна"))
        assertFalse(ForwardRules.headerLabel("Анна").contains("Переслано ·"))
        assertTrue(ForwardRules.isMasqueradingReply("Переслано · Боб"))
        assertFalse(ForwardRules.isMasqueradingReply("Анна"))
    }

    @Test
    fun textBodyPacksForwardWithoutReplyFields() {
        val packed = TextBody.encode("привет", null, "", "", forwardedFrom = "Анна")
        val body = TextBody.decode(packed)
        assertEquals("привет", body.text)
        assertNull(body.replyTo)
        assertEquals("", body.replyName)
        assertEquals("Анна", body.forwardedFrom)
        assertFalse(packed.contains("\"r\""))
        assertFalse(packed.contains("\"rn\""))
        assertTrue(packed.contains("\"ff\""))
        assertEquals("plain", TextBody.encode("plain", null, "", "").let { TextBody.decode(it).text })
    }

    @Test
    fun textBodyReplyStillPacksWithoutForward() {
        val packed = TextBody.encode("ответ", "mid-1", "цитата", "Анна")
        val body = TextBody.decode(packed)
        assertEquals("ответ", body.text)
        assertEquals("mid-1", body.replyTo)
        assertEquals("цитата", body.replyPreview)
        assertEquals("Анна", body.replyName)
        assertNull(body.forwardedFrom)
    }

    @Test
    fun mediaAndGroupCarryForwardedFromInsideJson() {
        val media = MediaPayload("image", "o", "ab", "KEY", "image/jpeg", "p.jpg", 1, forwardedFrom = "Анна")
        val parsed = MediaPayload.parse(media.toJson())
        assertEquals("Анна", parsed.forwardedFrom)
        assertTrue(media.toJson().contains("\"ff\""))
        val group = GroupTextPayload.parse(
            GroupTextPayload("g", "hi", 1, forwardedFrom = "Боб").toJson(),
        )
        assertEquals("Боб", group.forwardedFrom)
        assertNull(group.replyTo)
        assertTrue(group.toJson().contains("\"ff\""))
    }

    @Test
    fun dedicatedFieldHidesQuoteAndLegacyMasqueradeDoesToo() {
        val attributed = msg(forwardedFrom = "Анна")
        assertEquals("Анна", ForwardRules.attributedName(attributed))
        assertTrue(ForwardRules.hidesReplyQuote(attributed))
        val legacy = msg(replyToId = "mid", replyName = "Переслано · Боб", replyPreview = "hi")
        assertEquals("Боб", ForwardRules.attributedName(legacy))
        assertTrue(ForwardRules.hidesReplyQuote(legacy))
        val realReply = msg(replyToId = "mid", replyName = "Анна", replyPreview = "цитата")
        assertNull(ForwardRules.attributedName(realReply))
        assertFalse(ForwardRules.hidesReplyQuote(realReply))
    }

    @Test
    fun metaRoundtripKeepsForwardedFrom() {
        val meta = MessageMeta.parse(MessageMeta(forwardedFrom = "Анна").toJson())
        assertEquals("Анна", meta.forwardedFrom)
        assertNull(meta.replyToId)
    }

    @Test
    fun packedTextDefaultIsPlain() {
        val plain = PackedText("hi")
        assertNull(plain.replyTo)
        assertNull(plain.forwardedFrom)
    }

    private fun msg(
        forwardedFrom: String? = null,
        replyToId: String? = null,
        replyName: String = "",
        replyPreview: String = "",
    ) = ChatMessage(
        id = "m",
        peerDeviceId = "p",
        outgoing = false,
        text = "hi",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1,
        kind = MessageKind.TEXT,
        replyToId = replyToId,
        replyPreview = replyPreview,
        replyName = replyName,
        forwardedFrom = forwardedFrom,
    )
}
