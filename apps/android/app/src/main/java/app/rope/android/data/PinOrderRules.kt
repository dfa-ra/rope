package app.rope.android.data

import org.json.JSONObject

/**
 * Telegram-like reorder of pinned chats. Order lives in kv `pinRank`
 * (chat id → rank). No LocalStore bump, distinct from unpin-all.
 */
object PinOrderRules {
    const val KV = "pinRank"
    const val MOVE_UP = "Выше"
    const val MOVE_DOWN = "Ниже"
    const val UNRANKED = Int.MAX_VALUE
    const val MIN = 2

    fun parse(raw: String?): Map<String, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val o = JSONObject(raw)
            buildMap {
                o.keys().forEach { key ->
                    if (key.isNotBlank()) put(key, o.optInt(key, UNRANKED))
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun toJson(ranks: Map<String, Int>): String {
        val o = JSONObject()
        ranks.forEach { (id, rank) ->
            if (id.isNotBlank()) o.put(id, rank)
        }
        return o.toString()
    }

    fun rankOf(id: String, ranks: Map<String, Int>): Int = ranks[id] ?: UNRANKED

    fun ranksOf(ids: List<String>): Map<String, Int> =
        ids.mapIndexed { i, id -> id to i }.toMap()

    fun visible(pinnedCount: Int, forwarding: Boolean, searching: Boolean): Boolean =
        pinnedCount >= MIN && !forwarding && !searching

    fun canMoveUp(ids: List<String>, id: String): Boolean {
        val i = ids.indexOf(id)
        return i > 0
    }

    fun canMoveDown(ids: List<String>, id: String): Boolean {
        val i = ids.indexOf(id)
        return i >= 0 && i < ids.lastIndex
    }

    fun move(ids: List<String>, id: String, delta: Int): List<String> {
        if (delta == 0) return ids
        val i = ids.indexOf(id)
        if (i < 0) return ids
        val j = i + delta
        if (j !in ids.indices) return ids
        val out = ids.toMutableList()
        val item = out.removeAt(i)
        out.add(j, item)
        return out
    }

    fun afterPin(ranks: Map<String, Int>, id: String): Map<String, Int> {
        if (id.isBlank() || ranks.isEmpty() || id in ranks) return ranks
        val min = ranks.values.minOrNull() ?: 0
        return ranks + (id to min - 1)
    }

    fun afterUnpin(ranks: Map<String, Int>, id: String): Map<String, Int> {
        if (ranks.isEmpty() || id.isBlank()) return ranks
        return ranks.filterKeys { it != id }
    }
}
