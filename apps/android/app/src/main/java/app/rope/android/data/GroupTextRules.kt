package app.rope.android.data

/**
 * type=3 GROUP_TEXT is E2EE from any org member who can message you.
 * Insert only into a group this device already knows, and only when
 * the sender is in that group's local member list.
 * Do not use [ChatRouting.mediaChatId]: unknown group media falls back
 * to the sender's 1:1 thread; group text must be dropped instead.
 */
object GroupTextRules {
    /**
     * Chat id for an incoming GROUP_TEXT, or null to ack and skip insert.
     */
    fun chatId(
        groupId: String?,
        senderDeviceId: String?,
        groups: Collection<RopeGroup>,
    ): String? {
        val gid = JsonIds.optional(groupId) ?: return null
        if (PeerIds.normalize(senderDeviceId).isBlank()) return null
        val group = groups.find { PeerIds.same(it.groupId, gid) } ?: return null
        if (!group.members.any { PeerIds.same(it, senderDeviceId) }) return null
        return ChatIds.group(group.groupId)
    }
}
