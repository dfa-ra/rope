package app.rope.android

import app.rope.android.data.LinkPreviewRules
import app.rope.android.data.LocalStore
import app.rope.android.data.PackedLinkPreview
import app.rope.android.data.TextBody
import app.rope.android.data.TextFmtKind
import app.rope.android.data.TextFmtRules
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextFmtRulesTest {
    @Test
    fun inheritStoreAndTextBodyKeys() {
        assertEquals(6, LocalStore.VERSION)
        val plain = TextBody.encode("*hi* _x_ `c`", null, "", "")
        assertEquals("*hi* _x_ `c`", plain)
        val reply = JSONObject(TextBody.encode("*hi*", "mid-1", "цитата", "Анна"))
        assertEquals(setOf("t", "r", "rp", "rn"), reply.keys().asSequence().toSet())
        assertEquals("*hi*", reply.getString("t"))
        val quoted = JSONObject(
            TextBody.encode("ответ", "mid-1", "цитата", "Анна", quoteText = "ци", quoteStart = 0, quoteEnd = 2),
        )
        assertEquals(setOf("t", "r", "rp", "rn", "qt", "qo"), quoted.keys().asSequence().toSet())
        val lp = PackedLinkPreview("https://example.com", "example.com", "Example")
        val withLp = JSONObject(TextBody.encode("смотри", null, "", "", preview = lp))
        assertEquals(setOf("t", "lp"), withLp.keys().asSequence().toSet())
        val fwd = JSONObject(TextBody.encode("*hi*", null, "", "", forwardedFrom = "Анна"))
        assertEquals(setOf("t", "ff"), fwd.keys().asSequence().toSet())
        assertFalse(fwd.has("entities"))
        assertFalse(fwd.has("fmt"))
    }

    @Test
    fun boldItalicCodePreStrike() {
        val bold = TextFmtRules.parse("скажи *привет*")
        assertEquals("скажи привет", bold.display)
        assertEquals(listOf(TextFmtKind.BOLD), bold.spans.map { it.kind })
        assertEquals("привет", bold.display.substring(bold.spans[0].start, bold.spans[0].endExclusive))

        val italic = TextFmtRules.parse("_курсив_")
        assertEquals("курсив", italic.display)
        assertEquals(listOf(TextFmtKind.ITALIC), italic.spans.map { it.kind })

        val code = TextFmtRules.parse("run `ls -l` now")
        assertEquals("run ls -l now", code.display)
        assertEquals(listOf(TextFmtKind.CODE), code.spans.map { it.kind })
        assertEquals("ls -l", code.display.substring(code.spans[0].start, code.spans[0].endExclusive))

        val pre = TextFmtRules.parse("```\nfun x()\n```")
        assertEquals("fun x()", pre.display)
        assertEquals(listOf(TextFmtKind.PRE), pre.spans.map { it.kind })

        val strike = TextFmtRules.parse("это ~нет~ да")
        assertEquals("это нет да", strike.display)
        assertEquals(listOf(TextFmtKind.STRIKE), strike.spans.map { it.kind })
        val double = TextFmtRules.parse("~~старое~~")
        assertEquals("старое", double.display)
        assertEquals(listOf(TextFmtKind.STRIKE), double.spans.map { it.kind })
    }

    @Test
    fun nestedBoldItalicAndCodeStaysLiteral() {
        val nested = TextFmtRules.parse("*bold _italic_ still*")
        assertEquals("bold italic still", nested.display)
        assertTrue(nested.spans.any { it.kind == TextFmtKind.BOLD && it.start == 0 && it.endExclusive == nested.display.length })
        val ital = nested.spans.first { it.kind == TextFmtKind.ITALIC }
        assertEquals("italic", nested.display.substring(ital.start, ital.endExclusive))

        val code = TextFmtRules.parse("`*not* _fmt_`")
        assertEquals("*not* _fmt_", code.display)
        assertEquals(listOf(TextFmtKind.CODE), code.spans.map { it.kind })
        assertTrue(TextFmtRules.parse("```\n*raw*\n```").spans.none { it.kind == TextFmtKind.BOLD })
    }

    @Test
    fun unmatchedAndEscapedStayLiteral() {
        assertEquals("*один", TextFmtRules.plain("*один"))
        assertEquals("**", TextFmtRules.plain("**"))
        assertEquals("*звёзды*", TextFmtRules.plain("\\*звёзды\\*"))
        assertEquals("a_b_c", TextFmtRules.plain("a\\_b\\_c"))
        assertEquals("plain", TextFmtRules.plain("plain"))
        assertTrue(TextFmtRules.parse("plain").spans.isEmpty())
        assertEquals("*две\nстроки*", TextFmtRules.plain("*две\nстроки*"))
    }

    @Test
    fun preLanguageAndInlineFence() {
        val fenced = TextFmtRules.parse("```kt\nval x = 1\n```")
        assertEquals("val x = 1", fenced.display)
        assertEquals(listOf(TextFmtKind.PRE), fenced.spans.map { it.kind })
        val inline = TextFmtRules.parse("use ```pre``` here")
        assertEquals("use pre here", inline.display)
        assertEquals(listOf(TextFmtKind.PRE), inline.spans.map { it.kind })
    }

    @Test
    fun mentionsAndLinksUseDisplayOffsets() {
        val raw = "смотри *https://example.com/a* и @Анна"
        val fmt = TextFmtRules.parse(raw)
        assertEquals("смотри https://example.com/a и @Анна", fmt.display)
        val links = LinkPreviewRules.spans(fmt.display)
        assertEquals(1, links.size)
        assertEquals("https://example.com/a", fmt.display.substring(links[0].start, links[0].endExclusive))
    }
}
