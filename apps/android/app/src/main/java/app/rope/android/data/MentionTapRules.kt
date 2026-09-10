package app.rope.android.data

/**
 * Telegram-like tap on an @mention in a group bubble. Opens that person's
 * 1:1 chat. No LocalStore bump, no Go.
 */
object MentionTapRules {
    const val ACTION = "Написать"
    const val NOTICE_MISSING = "Нет в справочнике"
    const val TAG = "MENTION"

    fun canOpen(isGroup: Boolean, deviceId: String?, selfId: String?): Boolean {
        if (!isGroup) return false
        val id = PeerIds.normalize(deviceId)
        if (id.isEmpty() || SavedMessagesRules.isSaved(id) || ChatIds.isGroup(id)) return false
        val self = PeerIds.normalize(selfId)
        return self.isEmpty() || id != self
    }

    fun nameInSpan(text: String, range: IntRange): String {
        if (range.first < 0 || range.first >= text.length) return ""
        val end = (range.last + 1).coerceAtMost(text.length)
        if (end <= range.first) return ""
        return text.substring(range.first, end).removePrefix("@").trim()
    }

    fun deviceIdAt(
        text: String,
        offset: Int,
        devices: List<DirectoryDevice>,
        selfName: String?,
        selfId: String?,
    ): String? {
        val names = (devices.map { it.displayName } + listOfNotNull(selfName, GroupChatUx.YOU))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        val span = GroupChatUx.mentionSpans(text, names).firstOrNull { offset in it } ?: return null
        val name = nameInSpan(text, span)
        if (name.isEmpty()) return null
        if (name.equals(GroupChatUx.YOU, ignoreCase = true)) return null
        val self = selfName?.trim().orEmpty()
        if (self.isNotEmpty() && name.equals(self, ignoreCase = true)) return null
        val hit = devices.find { it.displayName.trim().equals(name, ignoreCase = true) } ?: return null
        if (!canOpen(true, hit.deviceId, selfId)) return null
        return hit.deviceId
    }
}
