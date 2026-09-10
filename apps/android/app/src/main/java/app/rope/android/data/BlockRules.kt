package app.rope.android.data

import org.json.JSONArray

/**
 * Telegram-like local block list. Device ids live in the existing kv table —
 * no LocalStore bump, no Go, no DeviceBackup.
 */
object BlockRules {
    const val KEY = "blocked"
    const val ACTION_BLOCK = "Заблокировать"
    const val ACTION_UNBLOCK = "Разблокировать"
    const val CONFIRM = "Точно заблокировать?"
    const val BARRIER = "Пользователь в чёрном списке"
    const val SECTION = "Чёрный список"
    const val EMPTY = "Никого нет. Блок только на этом устройстве."
    const val NOTICE_BLOCKED = "В чёрном списке"
    const val NOTICE_UNBLOCKED = "Разблокирован"
    const val PREVIEW_MAX = 12

    fun canBlock(peerId: String?, selfId: String?): Boolean {
        val peer = PeerIds.normalize(peerId)
        if (peer.isEmpty() || SavedMessagesRules.isSaved(peer) || ChatIds.isGroup(peer)) return false
        val self = PeerIds.normalize(selfId)
        return self.isEmpty() || peer != self
    }

    fun isBlocked(ids: Set<String>, peerId: String?): Boolean {
        val peer = PeerIds.normalize(peerId)
        return peer.isNotEmpty() && ids.any { PeerIds.same(it, peer) }
    }

    fun apply(ids: Set<String>, peerId: String, blocked: Boolean): Set<String> {
        val id = PeerIds.normalize(peerId)
        if (id.isEmpty() || SavedMessagesRules.isSaved(id) || ChatIds.isGroup(id)) return ids
        return if (blocked) ids + id else ids.filterNot { PeerIds.same(it, id) }.toSet()
    }

    fun dropDirect(ids: Set<String>, senderId: String?, groupChat: Boolean): Boolean =
        !groupChat && isBlocked(ids, senderId)

    fun action(blocked: Boolean): String = if (blocked) ACTION_UNBLOCK else ACTION_BLOCK

    fun preview(id: String?): String {
        val t = PeerIds.normalize(id)
        if (t.isEmpty()) return ""
        return if (t.length <= PREVIEW_MAX) t else t.take(8) + "…"
    }

    fun label(id: String, devices: List<DirectoryDevice>): String {
        val name = devices.find { PeerIds.same(it.deviceId, id) }?.displayName?.trim().orEmpty()
        return name.ifBlank { preview(id) }
    }

    fun parse(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return try {
            val arr = JSONArray(raw)
            buildSet {
                for (i in 0 until arr.length()) {
                    val id = PeerIds.normalize(arr.optString(i))
                    if (id.isNotEmpty()) add(id)
                }
            }
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun encode(ids: Set<String>): String {
        val arr = JSONArray()
        ids.map { PeerIds.normalize(it) }.filter { it.isNotEmpty() }.sorted().forEach { arr.put(it) }
        return arr.toString()
    }
}
