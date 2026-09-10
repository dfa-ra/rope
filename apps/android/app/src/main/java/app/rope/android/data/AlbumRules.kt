package app.rope.android.data

import org.json.JSONObject
import java.util.UUID

/**
 * Telegram-like grouped photo/video albums. Each item is still its own type=2 envelope;
 * [albumId] lives inside the encrypted object JSON (not visible to Go).
 */
object AlbumRules {
    const val MAX_PHOTOS = 10

    data class Slot(
        val albumId: String?,
        val index: Int,
        val count: Int,
    ) {
        val grouped: Boolean get() = albumId != null && count > 1
    }

    fun cap(size: Int): Int = size.coerceIn(0, MAX_PHOTOS)

    /**
     * One slot per outgoing photo or video. A single item is a normal image/video (no album_id).
     * Two to [MAX_PHOTOS] share one [albumId] with 0-based index order.
     */
    fun slots(count: Int, albumId: String? = null): List<Slot> {
        val n = cap(count)
        if (n <= 0) return emptyList()
        if (n == 1) return listOf(Slot(albumId = null, index = 0, count = 1))
        val id = albumKey(albumId) ?: UUID.randomUUID().toString()
        return (0 until n).map { Slot(id, it, n) }
    }

    fun isMemberKind(kind: MessageKind): Boolean =
        kind == MessageKind.IMAGE || kind == MessageKind.VIDEO

    fun albumId(msg: ChatMessage): String? {
        if (msg.deleted || !isMemberKind(msg.kind) || msg.extra.isBlank()) return null
        return runCatching {
            albumKey(JSONObject(msg.extra).optString("album_id"))
        }.getOrNull()
    }

    fun albumIndex(msg: ChatMessage): Int =
        runCatching { MediaPayload.parse(msg.extra).albumIndex }.getOrDefault(0)

    fun members(messages: List<ChatMessage>, albumId: String): List<ChatMessage> {
        val id = albumKey(albumId) ?: return emptyList()
        return messages.filter { albumId(it) == id }
            .sortedWith(compareBy<ChatMessage> { albumIndex(it) }.thenBy { it.timestampMs }.thenBy { it.id })
    }

    fun siblings(messages: List<ChatMessage>, msg: ChatMessage): List<ChatMessage> {
        if (!isMemberKind(msg.kind) || msg.deleted) return emptyList()
        val id = albumId(msg) ?: return listOf(msg)
        val found = members(messages, id)
        return found.ifEmpty { listOf(msg) }
    }

    fun collapse(items: List<ChatThreadItem>): List<ChatThreadItem> {
        val out = ArrayList<ChatThreadItem>(items.size)
        var i = 0
        while (i < items.size) {
            val item = items[i]
            if (item !is ChatThreadItem.Bubble) {
                out += item
                i++
                continue
            }
            val aid = albumId(item.msg)
            if (aid == null) {
                out += item
                i++
                continue
            }
            val grouped = mutableListOf(item.msg)
            var j = i + 1
            while (j < items.size) {
                val next = items[j] as? ChatThreadItem.Bubble ?: break
                if (albumId(next.msg) != aid) break
                grouped += next.msg
                j++
            }
            val ordered = grouped.sortedWith(
                compareBy<ChatMessage> { albumIndex(it) }.thenBy { it.timestampMs }.thenBy { it.id },
            )
            if (ordered.size == 1) out += ChatThreadItem.Bubble(ordered.first())
            else out += ChatThreadItem.Album(ordered)
            i = j
        }
        return out
    }

    fun indexOfMessage(items: List<ChatThreadItem>, id: String): Int =
        items.indexOfFirst { item ->
            when (item) {
                is ChatThreadItem.Day -> false
                is ChatThreadItem.Unread -> id == UnreadSeparatorRules.KEY
                is ChatThreadItem.Bubble -> item.msg.id == id
                is ChatThreadItem.Album -> item.members.any { it.id == id }
            }
        }

    fun notifyId(body: String, albumId: String?): Int {
        val key = albumKey(albumId) ?: body
        return key.hashCode()
    }

    fun preview(count: Int, videoCount: Int = 0): String = when {
        count <= 1 && videoCount >= 1 -> "Видео"
        count <= 1 -> "Фото"
        videoCount <= 0 -> "Альбом · $count фото"
        videoCount >= count -> "Альбом · $count видео"
        else -> "Альбом · $count"
    }

    /**
     * Fail closed on CR/LF/NUL before trim so a newline prefix cannot join a
     * live mosaic. Spaces still trim. Caption/text keep newlines.
     */
    internal fun albumKey(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) {
            return null
        }
        return JsonIds.optional(raw)
    }
}
