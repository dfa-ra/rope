package app.rope.android.data

/**
 * Telegram-like «Копировать ссылку» from a bubble. Clipboard is local —
 * https only, same host allowlist as [LinkPreviewRules]. LocalStore stays v6.
 */
object CopyLinkRules {
    const val ACTION = "Копировать ссылку"

    fun canCopy(msg: ChatMessage): Boolean = url(msg) != null

    fun url(msg: ChatMessage): String? {
        if (msg.deleted) return null
        val packed = msg.linkPreview?.url?.let { sanitize(it) }
        if (packed != null) return packed
        return sanitize(LinkPreviewRules.firstHttps(msg.text))
    }

    fun sanitize(raw: String?): String? {
        val cut = raw.orEmpty()
        if (cut.any { it == '\n' || it == '\r' || it == '\u0000' }) return null
        return LinkPreviewRules.parse(cut)?.url
    }
}
