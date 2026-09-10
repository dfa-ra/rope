package app.rope.android.data

import org.json.JSONArray

/**
 * Telegram-like view-once photo/video. Extra JSON `once`. Incoming opens
 * once in the viewer; later the bubble is «Просмотрено». Not media spoiler,
 * not envelope crypto.
 */
object ViewOnceRules {
    const val LABEL = "Один просмотр"
    const val VIEWED = "Просмотрено"
    const val KEY = "view_once_seen"
    const val MAX_IDS = 400

    fun canMark(kind: String): Boolean = kind == "image" || kind == "video"

    fun canMark(kind: MessageKind): Boolean =
        kind == MessageKind.IMAGE || kind == MessageKind.VIDEO

    fun pack(kind: String, once: Boolean): Boolean = once && canMark(kind)

    fun flagged(extra: String): Boolean {
        if (extra.isBlank()) return false
        return runCatching { MediaPayload.parse(extra).once }.getOrDefault(false)
    }

    fun flagged(msg: ChatMessage): Boolean = flagged(msg.extra)

    fun canOpen(outgoing: Boolean, flagged: Boolean, viewed: Boolean): Boolean {
        if (!flagged) return true
        if (outgoing) return true
        return !viewed
    }

    fun consumeOnOpen(outgoing: Boolean, flagged: Boolean): Boolean =
        flagged && !outgoing

    fun skipDownload(outgoing: Boolean, flagged: Boolean, viewed: Boolean): Boolean =
        flagged && viewed && !outgoing

    fun conceal(msg: ChatMessage): Boolean =
        canMark(msg.kind) && flagged(msg) && !msg.outgoing

    fun locked(msg: ChatMessage, viewedIds: Set<String>): Boolean =
        conceal(msg) && msg.id !in viewedIds

    fun placeholder(msg: ChatMessage, viewedIds: Set<String>): Boolean =
        conceal(msg) && msg.id in viewedIds

    fun viewerAlone(msg: ChatMessage): Boolean = flagged(msg)

    fun pendingEligible(mimes: List<String>, names: List<String> = emptyList()): Boolean =
        mimes.indices.any { i ->
            val mime = mimes[i]
            val name = names.getOrElse(i) { "" }
            mime.startsWith("image/", ignoreCase = true) || VideoRules.looksLikeVideo(name, mime)
        }

    fun sanitizeId(id: String): String? {
        if (id.isBlank()) return null
        if ('\n' in id || '\r' in id || '\u0000' in id) return null
        return id
    }

    fun viewedIds(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return try {
            val arr = JSONArray(raw)
            buildSet {
                for (i in 0 until arr.length().coerceAtMost(MAX_IDS)) {
                    sanitizeId(arr.optString(i))?.let { add(it) }
                }
            }
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun putViewed(ids: Set<String>): String {
        val arr = JSONArray()
        ids.mapNotNull { sanitizeId(it) }.take(MAX_IDS).forEach { arr.put(it) }
        return arr.toString()
    }

    fun mark(ids: Set<String>, id: String): Set<String> {
        val clean = sanitizeId(id) ?: return ids
        if (clean in ids) return ids
        val next = ids + clean
        return if (next.size <= MAX_IDS) next else next.toList().takeLast(MAX_IDS).toSet()
    }
}
