package app.rope.android.data

/**
 * Telegram-like «Ответить в личке» from a group bubble.
 * Opens the existing 1:1 with that sender, or the normal new 1:1 thread.
 * Sends stay on the 1:1 `send` path — no group_send, no new protocol type.
 */
object ReplyInDmRules {
    const val LABEL = "Ответить в личке"

    fun canShow(isGroup: Boolean, msg: ChatMessage, myDeviceId: String?): Boolean {
        if (!isGroup) return false
        if (!ChatActions.canReply(msg)) return false
        if (msg.outgoing) return false
        val sender = senderId(msg)
        if (sender.isBlank()) return false
        if (PeerIds.same(sender, myDeviceId)) return false
        if (ChatIds.isGroup(sender) || ChatIds.isSaved(sender)) return false
        return true
    }

    fun senderId(msg: ChatMessage): String = JsonIds.optional(msg.senderId).orEmpty()

    fun existingDm(conversations: List<Conversation>, senderId: String): Conversation? {
        if (PeerIds.normalize(senderId).isBlank()) return null
        return conversations.firstOrNull { c -> matchesSender(c, senderId) }
    }

    fun matchesSender(c: Conversation, senderId: String): Boolean {
        if (c.isGroup || SavedMessagesRules.isSaved(c) || ChatIds.isGroup(c.id)) return false
        return PeerIds.same(c.id, senderId) ||
            PeerIds.same(c.peer?.deviceId, senderId) ||
            PeerIds.same(c.peer?.memberId, senderId)
    }

    /** App already has composer reply — attach quote metadata when opening the DM. */
    fun attachReply(msg: ChatMessage): Boolean = ChatActions.canReply(msg)

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
        conversations: List<Conversation>,
        devices: List<DirectoryDevice>,
        onlineIds: Set<String> = emptySet(),
    ): DirectoryDevice? {
        val sender = senderId(msg)
        if (sender.isBlank()) return null
        val existing = existingDm(conversations, sender)
        existing?.peer?.let { return it }
        val hint = existing?.id ?: sender
        PeerIds.resolve(devices, existing?.peer, hint, onlineIds)?.let { return it }
        if (hint != sender) {
            PeerIds.resolve(devices, null, sender, onlineIds)?.let { return it }
        }
        existing?.let { return stubPeer(it.id, it.title, it.online) }
        return stubPeer(sender, msg.senderName)
    }

    fun chatId(existing: Conversation?, peer: DirectoryDevice): String = existing?.id ?: peer.deviceId
}
