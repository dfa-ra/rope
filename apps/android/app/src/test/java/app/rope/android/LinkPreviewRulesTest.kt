package app.rope.android

import app.rope.android.data.GroupTextPayload
import app.rope.android.data.LinkPreviewRules
import app.rope.android.data.PackedLinkPreview
import app.rope.android.data.TextBody
import app.rope.android.net.LinkOgFetcher
import app.rope.android.net.LinkUnfurl
import app.rope.android.net.OgHtml
import app.rope.android.net.OgPage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkPreviewRulesTest {
    @Test
    fun firstHttpsUrlWinsAndStripsJunk() {
        val text = "смотри https://Example.COM/path?q=1. и ещё https://other.org"
        val preview = LinkPreviewRules.first(text)!!
        assertEquals("https://example.com/path?q=1", preview.url)
        assertEquals("example.com", preview.host)
        val spans = LinkPreviewRules.spans(text)
        assertEquals(2, spans.size)
        assertEquals("other.org", spans[1].host)
        assertEquals("https://other.org", LinkPreviewRules.firstHttps("http://skip.example https://other.org"))
    }

    @Test
    fun wwwStrippedFromDisplayHost() {
        val preview = LinkPreviewRules.parse("https://www.example.com/foo")!!
        assertEquals("example.com", preview.host)
        assertEquals("https://www.example.com/foo", preview.url)
    }

    @Test
    fun rejectsNonHttpsJavascriptFtpBareHostAndLocalhost() {
        assertNull(LinkPreviewRules.first("ftp://files.example.com/a"))
        assertNull(LinkPreviewRules.first("javascript:alert(1)"))
        assertNull(LinkPreviewRules.first("example.com/no-scheme"))
        assertNull(LinkPreviewRules.parse("https://localhost/x"))
        assertNull(LinkPreviewRules.parse("https:///nohost"))
        assertNull(LinkPreviewRules.first(""))
        assertNull(LinkPreviewRules.parse("http://example.com/x"))
        assertNull(LinkPreviewRules.first("http://example.com"))
    }

    @Test
    fun parensAndSentencePunctuation() {
        val wrapped = LinkPreviewRules.spans("см. (https://news.ycombinator.com/item?id=1)")
        assertEquals(1, wrapped.size)
        assertEquals("news.ycombinator.com", wrapped[0].host)
        assertTrue(wrapped[0].url.contains("item?id=1"))
        val end = LinkPreviewRules.first("https://kotlinlang.org.")!!
        assertEquals("kotlinlang.org", end.host)
        assertEquals("https://kotlinlang.org", end.url)
    }

    @Test
    fun httpsOnlyRejectsHttpAndIpLiterals() {
        assertNotNull(LinkPreviewRules.parse("https://example.com"))
        assertNull(LinkPreviewRules.parse("http://example.com"))
        assertNull(LinkPreviewRules.parse("https://127.0.0.1/x"))
        assertNull(LinkPreviewRules.parse("https://192.168.0.1/a"))
        assertNull(LinkPreviewRules.parse("https://8.8.8.8/"))
        assertNull(LinkPreviewRules.parse("https://[::1]/"))
        assertNull(LinkPreviewRules.parse("https://exa..mple.com"))
        assertNull(LinkPreviewRules.parse("https://evil.local/x"))
    }

    @Test
    fun shouldFetchHonorsSettingsAndRecording() {
        val text = "см https://example.com/a"
        assertTrue(LinkPreviewRules.shouldFetch(enabled = true, recording = false, text = text))
        assertFalse(LinkPreviewRules.shouldFetch(enabled = false, recording = false, text = text))
        assertFalse(LinkPreviewRules.shouldFetch(enabled = true, recording = true, text = text))
        assertFalse(LinkPreviewRules.shouldFetch(enabled = true, recording = false, text = "нет ссылки"))
    }

    @Test
    fun textBodyEncodeWithLpIsJsonAndRoundTrips() {
        val lp = PackedLinkPreview(
            url = "https://example.com/a",
            host = "example.com",
            title = "Page title",
            description = "OG description, one line.",
        )
        val packed = TextBody.encode("смотри https://example.com/a", null, "", "", preview = lp)
        assertTrue(packed.trim().startsWith("{"))
        assertTrue(packed.contains("\"lp\""))
        assertFalse(packed.contains("jpeg"))
        val body = TextBody.decode(packed)
        assertEquals("смотри https://example.com/a", body.text)
        assertEquals("https://example.com/a", body.linkPreview!!.url)
        assertEquals("example.com", body.linkPreview!!.host)
        assertEquals("Page title", body.linkPreview!!.title)
        assertEquals("OG description, one line.", body.linkPreview!!.description)
        assertFalse(body.linkPreview!!.hasImage())
        assertEquals("plain", TextBody.encode("plain", null, "", "").let { TextBody.decode(it).text })
        val reply = TextBody.decode(TextBody.encode("ответ", "mid-1", "цитата", "Анна"))
        assertEquals("mid-1", reply.replyTo)
        assertNull(reply.linkPreview)
        val forward = TextBody.decode(TextBody.encode("hi", null, "", "", forwardedFrom = "Анна"))
        assertEquals("Анна", forward.forwardedFrom)
        assertNull(forward.linkPreview)
    }

    @Test
    fun imageFieldsOmittedWhenObjectAbsentAndKeptTogether() {
        val noImage = PackedLinkPreview("https://example.com/a", "example.com", "T", "D")
        val json = noImage.toJsonObject()
        assertFalse(json.has("o"))
        assertFalse(json.has("s"))
        assertFalse(json.has("k"))
        val withImage = noImage.copy(
            objectId = "11111111-2222-3333-4444-555555555555",
            sha256 = "ab",
            keyB64 = "KEY",
            mime = "image/jpeg",
            name = "lp.jpg",
            size = 18432,
        )
        val round = LinkPreviewRules.read(withImage.toJsonObject())!!
        assertTrue(round.hasImage())
        assertEquals("11111111-2222-3333-4444-555555555555", round.objectId)
        assertEquals(18432L, round.size)
        val partial = LinkPreviewRules.read(org.json.JSONObject("""{"u":"https://example.com/a","h":"example.com","t":"T","o":"oid"}"""))!!
        assertFalse(partial.hasImage())
    }

    @Test
    fun receivePackedJsonDoesNotInvokeFetcher() {
        LinkUnfurl.reset()
        var calls = 0
        LinkUnfurl.fetcher = LinkOgFetcher {
            calls += 1
            OgPage("leaked", "no")
        }
        val raw = """{"t":"смотри https://example.com/a","lp":{"u":"https://example.com/a","h":"example.com","t":"Page title","d":"OG description"}}"""
        val body = TextBody.decode(raw)
        val group = GroupTextPayload.parse(
            """{"g":"gid","t":"hi https://example.com/a","e":1,"lp":{"u":"https://example.com/a","h":"example.com","t":"Page title"}}""",
        )
        assertEquals("Page title", body.linkPreview!!.title)
        assertEquals("Page title", group.linkPreview!!.title)
        assertEquals(0, calls)
        assertEquals(0, LinkUnfurl.fetchCount())
        runBlocking { LinkUnfurl.fetch("https://example.com/a") }
        assertEquals(1, calls)
        LinkUnfurl.reset()
    }

    @Test
    fun ogHtmlFixturesPreferOgTagsAndRejectEmpty() {
        val html = """
            <html><head>
            <meta property="og:title" content="Page title">
            <meta content="OG description, one line." property="og:description">
            <meta property="og:image" content="/img.jpg">
            <title>Fallback</title>
            </head></html>
        """.trimIndent()
        val page = OgHtml.parse(html, "https://example.com/a")!!
        assertEquals("Page title", page.title)
        assertEquals("OG description, one line.", page.description)
        assertEquals("https://example.com/img.jpg", page.imageUrl)
        val titleOnly = OgHtml.parse("<html><head><title>Just title</title></head></html>", "https://example.com")!!
        assertEquals("Just title", titleOnly.title)
        assertNull(OgHtml.parse("<html><body>no tags</body></html>", "https://example.com"))
        val packed = LinkPreviewRules.fromOg("https://example.com/a", page.title, page.description)!!
        assertEquals("Page title", packed.title)
        assertNull(LinkPreviewRules.fromOg("https://example.com/a", "  ", "  "))
        assertNull(OgHtml.resolveHttps("https://example.com", "http://insecure.example/x"))
        assertNull(OgHtml.resolveHttps("https://example.com", "https://127.0.0.1/x"))
    }

    @Test
    fun keepUnfurlSurvivesSendClearingDraft() {
        val url = "https://example.com/a"
        assertFalse(LinkPreviewRules.shouldFetch(enabled = true, recording = false, text = ""))
        assertTrue(LinkPreviewRules.keepUnfurl(url, ""))
        assertTrue(LinkPreviewRules.keepUnfurl(url, "смотри $url"))
        assertFalse(LinkPreviewRules.keepUnfurl(url, "смотри https://other.org/x"))
        assertFalse(LinkPreviewRules.writeComposerCard(url, ""))
        assertTrue(LinkPreviewRules.writeComposerCard(url, "смотри $url"))
    }

    @Test
    fun fastSendPacksLpWhenUnfurlJobSeesEmptyDraft() {
        val sendText = "смотри https://example.com/a"
        val url = LinkPreviewRules.firstHttps(sendText)!!
        assertNull(
            LinkPreviewRules.pickPackedForSend(
                url,
                attached = null,
                jobResult = null,
                composerAfterWait = null,
            ),
        )
        val stashed = LinkPreviewRules.fromOg(url, "Page title", "OG description")
        assertEquals(
            "Page title",
            LinkPreviewRules.pickPackedForSend(url, attached = null, jobResult = stashed)!!.title,
        )
        val packed = runBlocking {
            LinkPreviewRules.resolveSendPreview(
                enabled = true,
                sendText = sendText,
                dismissedUrl = null,
                attached = null,
                jobResultAfterWait = {
                    // Composer job saw cleared draftText and returned without a card.
                    assertFalse(LinkPreviewRules.shouldFetch(true, false, ""))
                    assertTrue(LinkPreviewRules.keepUnfurl(url, ""))
                    null
                },
                fetch = { fetchUrl ->
                    LinkPreviewRules.fromOg(fetchUrl, "Page title", "OG description")
                },
            )
        }
        assertEquals("Page title", packed!!.title)
        assertEquals(url, packed.url)
        val attached = runBlocking {
            LinkPreviewRules.resolveSendPreview(
                enabled = true,
                sendText = sendText,
                dismissedUrl = null,
                attached = stashed,
                jobResultAfterWait = { error("attached send must not wait") },
                fetch = { error("attached send must not fetch") },
            )
        }
        assertEquals("Page title", attached!!.title)
        assertNull(
            runBlocking {
                LinkPreviewRules.resolveSendPreview(
                    enabled = true,
                    sendText = sendText,
                    dismissedUrl = url,
                    attached = null,
                    jobResultAfterWait = { error("dismissed send must not wait") },
                    fetch = { error("dismissed send must not fetch") },
                )
            },
        )
    }
}
