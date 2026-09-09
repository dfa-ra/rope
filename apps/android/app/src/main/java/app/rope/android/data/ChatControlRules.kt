package app.rope.android.data

/**
 * RECEIPT envelopes are E2EE from any org member who can message you.
 * UI already limits edit/delete to outgoing; apply the same author check
 * on the wire so a peer cannot tombstone or rewrite someone else's rows.
 * Not envelope crypto.
 */
object ChatControlRules {
    /** Edit/delete only the actor's own messages (sender device id). */
    fun allowAuthorOp(actorId: String?, msg: ChatMessage?): Boolean {
        if (msg == null) return false
        return PeerIds.same(actorId, msg.senderId)
    }

    fun allowEdit(actorId: String?, msg: ChatMessage?): Boolean = allowAuthorOp(actorId, msg)

    fun allowDelete(actorId: String?, msg: ChatMessage?): Boolean = allowAuthorOp(actorId, msg)

    /**
     * Pin the target message in its thread. 1:1: actor must be the chat peer.
     * Group: actor must be in [groupMembers] (local membership).
     */
    fun allowPin(actorId: String?, msg: ChatMessage?, groupMembers: Collection<String>?): Boolean {
        if (msg == null || msg.deleted) return false
        val group = JsonIds.optional(msg.groupId) ?: ChatIds.rawGroupId(msg.peerDeviceId).takeIf { ChatIds.isGroup(msg.peerDeviceId) }
        if (group != null) {
            val members = groupMembers ?: return false
            return members.any { PeerIds.same(it, actorId) }
        }
        return PeerIds.same(msg.peerDeviceId, actorId)
    }

    /**
     * Typing indicator chat id, or null to ignore.
     * Group-shaped targets only if the group is already on this device.
     */
    fun typingChatId(actorId: String?, targetId: String?, knownGroupIds: Collection<String>): String? {
        val actor = PeerIds.normalize(actorId)
        if (actor.isBlank()) return null
        val target = JsonIds.optional(targetId) ?: return actor
        if (ChatIds.isGroup(target)) {
            val gid = ChatIds.rawGroupId(target)
            val known = knownGroupIds.any { PeerIds.same(it, gid) || PeerIds.same(it, target) }
            return if (known) ChatIds.group(gid) else null
        }
        return actor
    }

    /**
     * Same message_id from a different sender must not REPLACE the row.
     * Blank sender (legacy) still merges.
     */
    fun shouldReplace(existingSender: String?, incomingSender: String?): Boolean {
        val a = PeerIds.normalize(existingSender)
        val b = PeerIds.normalize(incomingSender)
        if (a.isBlank() || b.isBlank()) return true
        return a == b
    }
}
