package app.rope.android

import app.rope.android.data.ComposerHintRules
import app.rope.android.data.GroupTextPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageMeta
import app.rope.android.data.PackedQuote
import app.rope.android.data.QuoteSpan
import app.rope.android.data.QuoteSpanRules
import app.rope.android.data.SwipeToReplyRules
import app.rope.android.data.TextBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteSpanRulesTest {
    @Test
    fun clampRejectsEmptyAndSnapsSurrogates() {
        assertNull(QuoteSpanRules.clamp("", 0, 1))
        assertNull(QuoteSpanRules.clamp("hi", 2, 2))
        assertNull(QuoteSpanRules.clamp("hi", 3, 1))
        val emoji = "a😀b"
        val span = QuoteSpanRules.clamp(emoji, 1, 3)!!
        assertEquals("😀", QuoteSpanRules.excerpt(emoji, span))
        val mid = QuoteSpanRules.clamp(emoji, 2, 3)!!
        assertFalse(emoji[mid.start].isLowSurrogate())
    }

    @Test
    fun packedOmitsFullBodyAndBlank() {
        val src = "привет мир"
        assertNull(QuoteSpanRules.packed(src, null))
        assertNull(QuoteSpanRules.packed(src, QuoteSpan(0, src.length)))
        assertNull(QuoteSpanRules.packed(src, QuoteSpan(3, 3)))
        val packed = QuoteSpanRules.packed(src, QuoteSpan(0, 6))!!
        assertEquals("привет", packed.text)
        assertEquals(0, packed.start)
        assertEquals(6, packed.end)
        assertTrue(QuoteSpanRules.isPartial(src, packed.span()))
        assertEquals("привет", QuoteSpanRules.preview(src, packed.span()))
        assertEquals(src, QuoteSpanRules.preview(src, null))
    }

    @Test
    fun textBodyRoundtripQtQoViaExistingJson() {
        val packed = TextBody.encode(
            "ответ",
            "mid-1",
            "привет",
            "Анна",
            quoteText = "при",
            quoteStart = 0,
            quoteEnd = 3,
        )
        val o = JSONObject(packed)
        assertEquals("ответ", o.getString("t"))
        assertEquals("mid-1", o.getString("r"))
        assertEquals("привет", o.getString("rp"))
        assertEquals("при", o.getString("qt"))
        assertEquals(0, o.getJSONArray("qo").getInt(0))
        assertEquals(3, o.getJSONArray("qo").getInt(1))
        val body = TextBody.decode(packed)
        assertEquals("ответ", body.text)
        assertEquals("mid-1", body.replyTo)
        assertEquals("при", body.quoteText)
        assertEquals(0, body.quoteStart)
        assertEquals(3, body.quoteEnd)
        val plain = TextBody.encode("ответ", "mid-1", "цитата", "Анна")
        assertFalse(plain.contains("\"qt\""))
        assertFalse(plain.contains("\"qo\""))
        val decodedPlain = TextBody.decode(plain)
        assertEquals("", decodedPlain.quoteText)
        assertEquals(-1, decodedPlain.quoteStart)
    }

    @Test
    fun groupPayloadAndMetaRoundtripSpan() {
        val group = GroupTextPayload.parse(
            GroupTextPayload(
                "g",
                "hi",
                1,
                "r",
                "полное",
                "Боб",
                quoteText = "лно",
                quoteStart = 2,
                quoteEnd = 5,
            ).toJson(),
        )
        assertEquals("лно", group.quoteText)
        assertEquals(2, group.quoteStart)
        assertEquals(5, group.quoteEnd)
        assertTrue(group.toJson().contains("\"qt\""))
        val meta = MessageMeta.parse(
            MessageMeta("r1", "prev", "Имя", quoteText = "prev", quoteStart = 1, quoteEnd = 4).toJson(),
        )
        assertEquals("prev", meta.quoteText)
        assertEquals(1, meta.quoteStart)
        assertEquals(4, meta.quoteEnd)
        assertEquals("выдел", QuoteSpanRules.displayPreview("полное тело", "выдел"))
        assertEquals("полное тело", QuoteSpanRules.displayPreview("полное тело", "  "))
    }

    @Test
    fun chromePrefersSpanOverFullPreview() {
        val copy = ComposerHintRules.reply("Аня", "полное сообщение целиком", "выделенный фрагмент")
        assertEquals("выделенный фрагмент", copy.body)
        val full = ComposerHintRules.reply("Аня", "полное сообщение целиком")
        assertEquals("полное сообщение целиком", full.body)
    }

    @Test
    fun canSelectTextNotMedia() {
        val text = app.rope.android.data.ChatMessage(
            id = "m",
            peerDeviceId = "p",
            outgoing = false,
            text = "hello world",
            status = app.rope.android.data.MessageStatus.DELIVERED_TO_DEVICE,
            timestampMs = 1L,
            kind = MessageKind.TEXT,
        )
        assertTrue(QuoteSpanRules.canSelect(text))
        assertFalse(QuoteSpanRules.canSelect(text.copy(kind = MessageKind.IMAGE, text = "Фото")))
        assertFalse(QuoteSpanRules.canSelect(text.copy(deleted = true)))
        val read = QuoteSpanRules.read(JSONObject("""{"qt":"hi","qo":[1,3]}"""))
        assertEquals(PackedQuote("hi", 1, 3), read)
    }

    @Test
    fun swipeCommitThresholdStays48dp() {
        assertEquals(48f, SwipeToReplyRules.COMMIT_DP)
        assertFalse(SwipeToReplyRules.shouldCommit(-47f))
        assertTrue(SwipeToReplyRules.shouldCommit(-48f))
        assertTrue(SwipeToReplyRules.shouldLock(-SwipeToReplyRules.SLOP_DP, 0f))
        assertFalse(SwipeToReplyRules.shouldLock(0f, 0f))
    }
}
