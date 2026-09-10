package app.rope.android.net

import app.rope.android.BuildConfig
import app.rope.android.data.LinkPreviewRules
import app.rope.android.data.OgDnsRules
import app.rope.android.media.ImageCodec
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

data class OgPage(
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val imageJpeg: ByteArray? = null,
)

fun interface LinkOgFetcher {
    suspend fun fetch(url: String): OgPage?
}

/**
 * Phone-only Open Graph fetch. Dedicated OkHttp client — never the VPS pin.
 * OkHttp DNS drops loopback / RFC1918 / link-local / ULA.
 * Tests inject [fetcher]; production uses [NetworkLinkOgFetcher].
 */
object LinkUnfurl {
    private val override = AtomicReference<LinkOgFetcher?>(null)
    private val fetchCalls = AtomicInteger(0)

    var fetcher: LinkOgFetcher
        get() = override.get() ?: NetworkLinkOgFetcher
        set(value) {
            override.set(value)
        }

    fun reset() {
        override.set(null)
        fetchCalls.set(0)
    }

    fun fetchCount(): Int = fetchCalls.get()

    suspend fun fetch(url: String): OgPage? {
        fetchCalls.incrementAndGet()
        return fetcher.fetch(url)
    }
}

object OgHtml {
    fun parse(html: String, pageUrl: String): OgPage? {
        if (html.isBlank()) return null
        val ogTitle = meta(html, "og:title")
        val htmlTitle = titleTag(html)
        val title = unescape(ogTitle.ifBlank { htmlTitle })
        val description = unescape(meta(html, "og:description"))
        val imageRaw = meta(html, "og:image")
        val imageUrl = resolveHttps(pageUrl, imageRaw)
        if (title.isBlank() && description.isBlank()) return null
        return OgPage(
            title = title,
            description = description,
            imageUrl = imageUrl,
        )
    }

    fun resolveHttps(pageUrl: String, raw: String): String? {
        val cut = raw.trim()
        if (cut.isEmpty()) return null
        val base = runCatching { URI(pageUrl) }.getOrNull()
        val resolved = when {
            cut.startsWith("https://", ignoreCase = true) -> runCatching { URI(cut) }.getOrNull()
            base != null -> runCatching { base.resolve(cut) }.getOrNull()
            else -> null
        } ?: return null
        if (resolved.scheme?.lowercase() != "https") return null
        val host = resolved.host ?: return null
        if (!LinkPreviewRules.hostAllowed(host)) return null
        return resolved.toString()
    }

    private fun meta(html: String, property: String): String {
        val quoted = Regex.escape(property)
        val propertyFirst = Regex(
            """<meta\s+[^>]*(?:property|name)\s*=\s*["']$quoted["'][^>]*content\s*=\s*["']([^"']*)["']""",
            RegexOption.IGNORE_CASE,
        )
        val contentFirst = Regex(
            """<meta\s+[^>]*content\s*=\s*["']([^"']*)["'][^>]*(?:property|name)\s*=\s*["']$quoted["']""",
            RegexOption.IGNORE_CASE,
        )
        return propertyFirst.find(html)?.groupValues?.getOrNull(1)
            ?: contentFirst.find(html)?.groupValues?.getOrNull(1)
            ?: ""
    }

    private fun titleTag(html: String): String {
        val m = Regex("""<title[^>]*>([^<]*)</title>""", RegexOption.IGNORE_CASE).find(html)
        return m?.groupValues?.getOrNull(1).orEmpty()
    }

    private fun unescape(raw: String): String {
        return raw
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .trim()
    }
}

object NetworkLinkOgFetcher : LinkOgFetcher {
    private val ogDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> =
            OgDnsRules.lookup(hostname) { Dns.SYSTEM.lookup(it) }
    }

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(ogDns)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(5, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .cookieJar(CookieJar.NO_COOKIES)
            .cache(null)
            .build()
    }

    override suspend fun fetch(url: String): OgPage? {
        val parsed = LinkPreviewRules.parse(url) ?: return null
        val html = downloadHtml(parsed.url) ?: return null
        val page = OgHtml.parse(html, parsed.url) ?: return null
        val jpeg = page.imageUrl?.let { downloadJpeg(it) }
        return page.copy(imageJpeg = jpeg)
    }

    private fun downloadHtml(url: String): String? {
        val response = follow(url, accept = "text/html") ?: return null
        response.use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body ?: return null
            val source = body.source()
            source.request(LinkPreviewRules.HTML_MAX_BYTES.toLong() + 1)
            val buf = source.buffer
            val take = minOf(buf.size, LinkPreviewRules.HTML_MAX_BYTES.toLong())
            return buf.readUtf8(take)
        }
    }

    private fun downloadJpeg(url: String): ByteArray? {
        if (LinkPreviewRules.parse(url) == null && OgHtml.resolveHttps(url, url) == null) return null
        val response = follow(url, accept = "image/*") ?: return null
        response.use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body ?: return null
            val source = body.source()
            source.request(LinkPreviewRules.IMAGE_MAX_BYTES.toLong() + 1)
            val buf = source.buffer
            if (buf.size > LinkPreviewRules.IMAGE_MAX_BYTES) return null
            val bytes = buf.readByteArray()
            if (bytes.isEmpty()) return null
            return ImageCodec.compressLinkThumb(bytes)
        }
    }

    private fun follow(startUrl: String, accept: String): Response? {
        var current = startUrl
        var redirects = 0
        while (true) {
            val parsed = LinkPreviewRules.parse(current) ?: return null
            val req = Request.Builder()
                .url(parsed.url)
                .header("User-Agent", "Rope/${BuildConfig.VERSION_NAME}")
                .header("Accept", accept)
                .get()
                .build()
            val resp = runCatching { http.newCall(req).execute() }.getOrNull() ?: return null
            if (!resp.isRedirect) return resp
            val loc = resp.header("Location").orEmpty()
            resp.close()
            if (loc.isBlank() || redirects >= 3) return null
            val next = OgHtml.resolveHttps(parsed.url, loc) ?: return null
            current = next
            redirects++
        }
    }
}
