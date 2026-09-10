package app.rope.android.data

/**
 * Telegram-like 1:1 peer profile. Shared photos are IMAGE rows; shared
 * HTTPS links come from bubble text or packed `lp`. Not the Saved chips.
 */
data class SharedLink(
    val messageId: String,
    val url: String,
    val host: String,
    val timestampMs: Long,
)

object PeerProfileRules {
    const val GRID_COLUMNS = 3
    const val SECTION = "Общие медиа"
    const val LINKS = "Ссылки"

    fun headerClickable(isGroup: Boolean, hasPeer: Boolean, saved: Boolean = false): Boolean =
        !saved && (isGroup || hasPeer)

    fun opensPeerProfile(isGroup: Boolean, hasPeer: Boolean, saved: Boolean = false): Boolean =
        !saved && !isGroup && hasPeer

    fun title(peerName: String?): String = peerName?.trim().orEmpty().ifBlank { "Профиль" }

    fun photos(messages: List<ChatMessage>): List<ChatMessage> =
        messages
            .filter { it.kind == MessageKind.IMAGE && !it.deleted }
            .sortedWith(compareByDescending<ChatMessage> { it.timestampMs }.thenByDescending { it.id })

    fun links(messages: List<ChatMessage>): List<SharedLink> {
        val out = ArrayList<SharedLink>()
        messages.forEach { m ->
            if (m.deleted) return@forEach
            val seen = HashSet<String>()
            LinkPreviewRules.spans(m.text).forEach { span ->
                if (seen.add(span.url)) {
                    out += SharedLink(m.id, span.url, span.host, m.timestampMs)
                }
            }
            val packed = m.linkPreview
            if (packed != null && packed.url.startsWith("https://", ignoreCase = true) && seen.add(packed.url)) {
                val host = packed.host.ifBlank { LinkPreviewRules.parse(packed.url)?.host.orEmpty() }
                if (host.isNotBlank()) {
                    out += SharedLink(m.id, packed.url, host, m.timestampMs)
                }
            }
        }
        return out.sortedWith(
            compareByDescending<SharedLink> { it.timestampMs }.thenByDescending { it.messageId },
        )
    }

    fun linksSectionLabel(count: Int): String = when {
        count <= 0 -> LINKS
        else -> "$LINKS · $count"
    }

    fun showPhotoEmpty(photoCount: Int, linkCount: Int): Boolean =
        photoCount <= 0 && linkCount <= 0

    fun emptyTitle(): String = "Нет общих медиа"

    fun emptyBody(): String = "Фото из этой переписки появятся здесь."

    fun sectionLabel(count: Int): String = when {
        count <= 0 -> SECTION
        else -> "$SECTION · $count"
    }
}
