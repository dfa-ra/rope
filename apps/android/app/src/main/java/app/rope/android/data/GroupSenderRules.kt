package app.rope.android.data

/**
 * Telegram-like tap on a group sender name or avatar. Opens that person's
 * 1:1 chat. No LocalStore bump, no Go.
 */
object GroupSenderRules {
    const val ACTION = "Написать"
    const val NOTICE_MISSING = "Нет в справочнике"

    fun senderId(msg: ChatMessage): String = JsonIds.optional(msg.senderId).orEmpty()

    fun canOpen(isGroup: Boolean, msg: ChatMessage, selfId: String?): Boolean {
        if (!isGroup) return false
        if (msg.outgoing) return false
        val sender = PeerIds.normalize(senderId(msg))
        if (sender.isEmpty() || SavedMessagesRules.isSaved(sender) || ChatIds.isGroup(sender)) {
            return false
        }
        val self = PeerIds.normalize(selfId)
        return self.isEmpty() || sender != self
    }

    fun stubPeer(senderId: String, senderName: String, online: Boolean = false): DirectoryDevice {
        val id = JsonIds.optional(senderId).orEmpty()
        return DirectoryDevice(
            deviceId = id,
            memberId = "",
            displayName = senderName.trim().ifBlank { id.take(8) },
            publicIdentity = ByteArray(0),
            lastSeen = "",
            online = online,
        )
    }

    fun peerFor(
        msg: ChatMessage,
        devices: List<DirectoryDevice>,
        onlineIds: Set<String> = emptySet(),
    ): DirectoryDevice? {
        val sender = senderId(msg)
        if (sender.isBlank()) return null
        PeerIds.resolve(devices, null, sender, onlineIds)?.let { return it }
        return stubPeer(sender, msg.senderName, onlineIds.any { PeerIds.same(it, sender) })
    }
}
