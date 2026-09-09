package app.rope.android.data

import org.json.JSONObject
import java.net.URI

/**
 * HTTPS URL detect + `lp` pack/read. The phone fetches OG; recipients only
 * render packed ciphertext. Not crypto. Go never sees these fields.
 */
data class PackedLinkPreview(
    val url: String,
    val host: String,
    val title: String,
    val description: String = "",
    val objectId: String? = null,
    val sha256: String = "",
    val keyB64: String = "",
    val mime: String = "",
    val name: String = "",
    val size: Long = 0,
    val localPath: String? = null,
) {
    fun hasImage(): Boolean =
        !objectId.isNullOrBlank() && sha256.isNotBlank() && keyB64.isNotBlank()

    fun withoutImage(): PackedLinkPreview = copy(
        objectId = null,
        sha256 = "",
        keyB64 = "",
        mime = "",
        name = "",
        size = 0,
    )

    fun toJsonObject(): JSONObject = JSONObject()
        .put("u", url)
        .put("h", host)
        .put("t", title)
        .apply {
            if (description.isNotBlank()) put("d", description)
            val oid = JsonIds.optional(objectId)
            if (oid != null && sha256.isNotBlank() && keyB64.isNotBlank()) {
                put("o", oid)
                put("s", sha256)
                put("k", keyB64)
                put("m", mime.ifBlank { "image/jpeg" })
                put("n", name.ifBlank { "lp.jpg" })
                put("z", size)
            }
        }
}

data class LinkSpan(
    val start: Int,
    val endExclusive: Int,
    val url: String,
    val host: String,
)

object LinkPreviewRules {
    const val URL_MAX = 2048
    const val HOST_MAX = 64
    const val TITLE_MAX = 120
    const val DESC_MAX = 240
    const val HTML_MAX_BYTES = 256 * 1024
    const val IMAGE_MAX_BYTES = 512 * 1024
    const val THUMB_MAX_BYTES = 64 * 1024
    const val THUMB_EDGE = 320
    const val UNFURL_DEBOUNCE_MS = 400L
    const val SEND_WAIT_MS = 3000L

    private val finder = Regex("""https://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private val trailingJunk = Regex("""[.,;:!?…»"')\]]+$""")
    private val ipv4 = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

    fun firstHttps(text: String): String? = spans(text).firstOrNull()?.url

    fun first(text: String): PackedLinkPreview? {
        val span = spans(text).firstOrNull() ?: return null
        return PackedLinkPreview(url = span.url, host = span.host, title = span.host)
    }

    fun spans(text: String): List<LinkSpan> {
        if (text.isEmpty()) return emptyList()
        val out = mutableListOf<LinkSpan>()
        finder.findAll(text).forEach { match ->
            val trimmed = trimJunk(match.value)
            val parsed = parse(trimmed) ?: return@forEach
            val start = match.range.first
            val end = start + trimmed.length
            if (end <= start || end > text.length) return@forEach
            out += LinkSpan(start, end, parsed.url, parsed.host)
        }
        return out
    }

    fun parse(raw: String): PackedLinkPreview? {
        val cut = trimJunk(raw).trim()
        if (cut.length < 10 || cut.length > URL_MAX) return null
        val uri = runCatching { URI(cut) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() != "https") return null
        val hostRaw = uri.host?.trim()?.trim('.')?.lowercase() ?: return null
        if (!hostAllowed(hostRaw)) return null
        val host = displayHost(hostRaw)
        if (host.isEmpty()) return null
        val url = canonicalHttps(uri, hostRaw) ?: return null
        if (url.length > URL_MAX) return null
        return PackedLinkPreview(url = url, host = host, title = host)
    }

    fun hostAllowed(host: String): Boolean {
        val h = host.trim().trim('.').lowercase()
        if (h.isEmpty() || ' ' in h || '.' !in h) return false
        if (h.startsWith('.') || h.endsWith('.') || ".." in h) return false
        if (h == "localhost" || h.endsWith(".localhost") || h.endsWith(".local")) return false
        if (':' in h || h.startsWith("[")) return false
        if (ipv4.matches(h)) return false
        return true
    }

    fun shouldFetch(enabled: Boolean, recording: Boolean, text: String): Boolean {
        if (!enabled || recording) return false
        return firstHttps(text) != null
    }

    fun fromOg(url: String, title: String, description: String): PackedLinkPreview? {
        val parsed = parse(url) ?: return null
        val cleanTitle = clip(title, TITLE_MAX)
        val cleanDesc = clip(description, DESC_MAX)
        if (cleanTitle.isEmpty() && cleanDesc.isEmpty()) return null
        val shown = cleanTitle.ifBlank { parsed.host }
        if (shown.isEmpty()) return null
        return parsed.copy(title = shown, description = cleanDesc)
    }

    fun put(o: JSONObject, preview: PackedLinkPreview?) {
        if (preview == null || preview.title.isBlank() || preview.url.isBlank()) return
        o.put("lp", preview.toJsonObject())
    }

    fun read(raw: JSONObject?): PackedLinkPreview? {
        if (raw == null) return null
        val url = raw.optString("u").trim()
        val host = clip(raw.optString("h"), HOST_MAX).ifBlank {
            parse(url)?.host.orEmpty()
        }
        val title = clip(raw.optString("t"), TITLE_MAX).ifBlank { host }
        if (url.isEmpty() || !url.startsWith("https://", ignoreCase = true) || title.isEmpty() || host.isEmpty()) {
            return null
        }
        if (parse(url) == null) return null
        val objectId = JsonIds.optional(raw.optString("o"))
        val sha = raw.optString("s").trim()
        val key = raw.optString("k").trim()
        return PackedLinkPreview(
            url = url.take(URL_MAX),
            host = host,
            title = title,
            description = clip(raw.optString("d"), DESC_MAX),
            objectId = objectId,
            sha256 = sha,
            keyB64 = key,
            mime = raw.optString("m").ifBlank { "image/jpeg" },
            name = clip(raw.optString("n"), 64).ifBlank { "lp.jpg" },
            size = raw.optLong("z"),
        ).let { packed ->
            if (objectId == null || sha.isBlank() || key.isBlank()) packed.withoutImage() else packed
        }
    }

    fun displayHost(host: String): String {
        val stripped = if (host.startsWith("www.") && host.length > 4) host.removePrefix("www.") else host
        return if (stripped.length <= HOST_MAX) stripped else stripped.take(HOST_MAX)
    }

    fun clip(value: String, max: Int): String {
        val one = value.replace(WHITESPACE, " ").trim()
        if (one.isEmpty()) return ""
        if (one.length <= max) return one
        return one.take(max).trimEnd()
    }

    private fun canonicalHttps(uri: URI, hostRaw: String): String? {
        return buildString {
            append("https://")
            append(hostRaw)
            if (uri.port > 0 && uri.port != 443) {
                append(':')
                append(uri.port)
            }
            val path = uri.rawPath.orEmpty()
            if (path.isNotEmpty()) append(path)
            val q = uri.rawQuery
            if (!q.isNullOrEmpty()) {
                append('?')
                append(q)
            }
        }
    }

    private fun trimJunk(raw: String): String {
        var s = raw.trim()
        while (s.isNotEmpty() && trailingJunk.containsMatchIn(s.takeLast(1))) {
            if (s.last() == ')' && s.count { it == '(' } > s.count { it == ')' } - 1) break
            s = s.dropLast(1)
        }
        return s
    }

    private val WHITESPACE = Regex("\\s+")
}
