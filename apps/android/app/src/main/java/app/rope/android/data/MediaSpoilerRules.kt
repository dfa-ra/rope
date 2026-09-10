package app.rope.android.data

/**
 * Telegram-like media spoiler: photos/videos stay blurred until tap.
 * Flag lives in type=2 extra JSON (`spoiler`). Not text `||secret||`,
 * not envelope crypto.
 */
object MediaSpoilerRules {
    const val LABEL = "Скрыть"
    const val REVEAL = "Показать"
    const val KEY = "spoiler"

    fun canMark(kind: String): Boolean = kind == "image" || kind == "video"

    fun canMark(kind: MessageKind): Boolean =
        kind == MessageKind.IMAGE || kind == MessageKind.VIDEO

    fun pack(kind: String, spoiler: Boolean): Boolean = spoiler && canMark(kind)

    fun flagged(extra: String): Boolean {
        if (extra.isBlank()) return false
        return runCatching { MediaPayload.parse(extra).spoiler }.getOrDefault(false)
    }

    fun hidden(msg: ChatMessage, revealedIds: Set<String>): Boolean {
        if (msg.deleted || msg.id in revealedIds) return false
        if (!canMark(msg.kind)) return false
        return flagged(msg.extra)
    }

    fun pendingEligible(mimes: List<String>, names: List<String> = emptyList()): Boolean =
        mimes.indices.any { i ->
            val mime = mimes[i]
            val name = names.getOrElse(i) { "" }
            mime.startsWith("image/", ignoreCase = true) || VideoRules.looksLikeVideo(name, mime)
        }

    fun reveal(ids: Set<String>, id: String): Set<String> {
        if (id.isBlank()) return ids
        return ids + id
    }
}
