package app.rope.android.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Several pinned messages in one thread. kv `chat_prefs` JSON only —
 * `pinned_messages` plus legacy `pinned_message` as the latest id.
 * Toggle adds or removes; it does not replace the list. LocalStore stays v6.
 */
object PinListRules {
    const val KEY = "pinned_messages"
    const val LEGACY_KEY = "pinned_message"
    const val MAX = 50
    const val LABEL_ONE = "Закреплённое сообщение"
    const val LIST_TITLE = "Закреплённые"
    const val UNPIN_ALL = "Открепить все"

    fun normalize(ids: Collection<String?>): List<String> {
        val out = LinkedHashSet<String>()
        for (raw in ids) {
            val id = JsonIds.optional(raw) ?: continue
            out.add(id)
            if (out.size >= MAX) break
        }
        return out.toList()
    }

    fun stored(ids: List<String>, legacy: String?): List<String> {
        if (ids.isNotEmpty()) return normalize(ids)
        return normalize(listOf(legacy))
    }

    fun latest(ids: List<String>): String? = ids.lastOrNull()

    fun contains(ids: List<String>, id: String?): Boolean {
        val target = JsonIds.optional(id) ?: return false
        return ids.any { it == target }
    }

    /** Add if missing (latest); remove if already pinned. Caps at [MAX] by dropping oldest. */
    fun toggle(ids: List<String>, id: String?): List<String> {
        val target = JsonIds.optional(id) ?: return ids
        if (contains(ids, target)) return ids.filter { it != target }
        return cap(ids.filter { it != target } + target)
    }

    fun applyRemote(ids: List<String>, targetId: String?, clear: Boolean): List<String> {
        val target = JsonIds.optional(targetId) ?: return ids
        return if (clear) {
            ids.filter { it != target }
        } else {
            cap(ids.filter { it != target } + target)
        }
    }

    fun visible(messages: List<ChatMessage>, ids: List<String>): List<ChatMessage> {
        val byId = messages.associateBy { it.id }
        return ids.mapNotNull { id -> byId[id]?.takeIf { !it.deleted } }
    }

    fun barMessage(visible: List<ChatMessage>, showing: String?): ChatMessage? {
        if (visible.isEmpty()) return null
        return visible.lastOrNull { it.id == showing } ?: visible.last()
    }

    /**
     * After jumping to [showing], the bar rotates to the previous (older) pin,
     * wrapping from oldest to newest.
     */
    fun nextShown(ids: List<String>, showing: String?): String? {
        val vis = ids.filter { it.isNotBlank() }
        if (vis.isEmpty()) return null
        val idx = vis.indexOf(showing)
        return when {
            vis.size == 1 -> vis.last()
            idx <= 0 -> vis.last()
            else -> vis[idx - 1]
        }
    }

    fun barLabel(count: Int): String = when {
        count <= 1 -> LABEL_ONE
        else -> "$count закреплённых"
    }

    fun showList(count: Int): Boolean = count > 1

    fun showUnpinAll(count: Int): Boolean = count > 1

    fun parse(o: JSONObject): List<String> {
        val arr = o.optJSONArray(KEY)
        if (arr != null && arr.length() > 0) {
            val fromArr = buildList {
                for (i in 0 until arr.length()) add(arr.optString(i))
            }
            return normalize(fromArr)
        }
        return normalize(listOf(o.optString(LEGACY_KEY)))
    }

    fun toArray(ids: List<String>): JSONArray {
        val arr = JSONArray()
        normalize(ids).forEach { arr.put(it) }
        return arr
    }

    fun withPins(cur: ChatPrefs, ids: List<String>): ChatPrefs {
        val n = normalize(ids)
        return cur.copy(pinnedMessageIds = n, pinnedMessageId = latest(n))
    }

    private fun cap(ids: List<String>): List<String> =
        if (ids.size <= MAX) ids else ids.takeLast(MAX)
}
