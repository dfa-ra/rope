package app.rope.android.data

/**
 * Chat-list last-message line for packed `lp`. URL-only rows show the OG
 * title (or host) instead of the raw HTTPS string. Recipients never refetch.
 * LocalStore stays v6.
 */
object LpChatRules {
    const val LABEL = "Ссылка"

    fun display(lp: PackedLinkPreview): String {
        val title = lp.title.trim()
        val raw = if (title.isNotEmpty() && !title.equals(lp.host, ignoreCase = true)) {
            title
        } else {
            lp.host.trim().ifBlank { LABEL }
        }
        return ChatListPreviewRules.clip(raw)
    }

    fun urlOnly(text: String, lp: PackedLinkPreview): Boolean {
        if (text.isBlank()) return true
        val spans = LinkPreviewRules.spans(text)
        if (spans.size != 1) return false
        if (spans[0].url != lp.url && !text.contains(lp.host)) return false
        val rest = (text.substring(0, spans[0].start) + text.substring(spans[0].endExclusive)).trim()
        return rest.isEmpty() || rest.all { it in ".,;:!?…" }
    }

    fun body(msg: ChatMessage): String {
        if (msg.deleted) return msg.preview()
        val lp = msg.linkPreview ?: return msg.preview()
        if (!urlOnly(msg.text, lp)) return msg.preview()
        return display(lp)
    }
}
