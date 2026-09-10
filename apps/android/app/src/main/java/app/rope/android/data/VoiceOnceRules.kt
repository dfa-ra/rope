package app.rope.android.data

import org.json.JSONArray

/**
 * Telegram-like one-time voice: extra JSON `once`. Incoming play-through
 * is remembered in kv. Sender can still replay. Not envelope crypto.
 */
object VoiceOnceRules {
    const val LABEL = "Один раз"
    const val HEARD = "Прослушано"
    const val KEY = "voice_once_heard"
    const val MAX_IDS = 400

    fun canMark(kind: String): Boolean = kind == "voice"

    fun pack(kind: String, once: Boolean): Boolean = once && canMark(kind)

    fun flagged(extra: String): Boolean {
        if (extra.isBlank()) return false
        return runCatching { MediaPayload.parse(extra).once }.getOrDefault(false)
    }

    fun flagged(msg: ChatMessage): Boolean = flagged(msg.extra)

    fun canPlay(outgoing: Boolean, flagged: Boolean, heard: Boolean): Boolean {
        if (!flagged) return true
        if (outgoing) return true
        return !heard
    }

    fun canSeek(flagged: Boolean): Boolean = !flagged

    fun consumeOnComplete(outgoing: Boolean, flagged: Boolean): Boolean =
        flagged && !outgoing

    fun skipDownload(outgoing: Boolean, flagged: Boolean, heard: Boolean): Boolean =
        flagged && heard && !outgoing

    fun sanitizeId(id: String): String? {
        if (id.isBlank()) return null
        if ('\n' in id || '\r' in id || '\u0000' in id) return null
        return id
    }

    fun heardIds(raw: String?): Set<String> {
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

    fun putHeard(ids: Set<String>): String {
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
