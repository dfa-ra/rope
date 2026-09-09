package app.rope.android.data

enum class MessageStatus {
    CREATED,
    SENT_TO_SERVER,
    DELIVERED_TO_DEVICE,
}

enum class MessageKind {
    TEXT,
    VOICE,
    IMAGE,
    VIDEO,
    FILE,
    GROUP_TEXT,
    CALL,
    UNKNOWN,
}

data class ServerProfile(
    val host: String,
    val port: Int,
    val serverId: String,
    val fingerprint: String,
    val useTls: Boolean,
    val role: String,
    val memberId: String,
    val deviceId: String,
    val displayName: String = "",
    val iceServersJson: String = "",
)

data class SshTarget(
    val host: String,
    val sshPort: Int = 22,
    val user: String = "root",
    val listenPort: Int = 8443,
)

data class DirectoryDevice(
    val deviceId: String,
    val memberId: String,
    val displayName: String,
    val publicIdentity: ByteArray,
    val lastSeen: String,
    val online: Boolean = false,
    val role: String = "member",
)

data class ChatMessage(
    val id: String,
    val peerDeviceId: String,
    val outgoing: Boolean,
    val text: String,
    val status: MessageStatus,
    val timestampMs: Long,
    val envelope: ByteArray? = null,
    val kind: MessageKind = MessageKind.TEXT,
    val extra: String = "",
    val groupId: String? = null,
    val localPath: String? = null,
    val senderId: String = "",
    val senderName: String = "",
    val reactions: List<Reaction> = emptyList(),
    val replyToId: String? = null,
    val replyPreview: String = "",
    val replyName: String = "",
    val forwardedFrom: String? = null,
    val edited: Boolean = false,
    val deleted: Boolean = false,
    val quoteText: String = "",
    val quoteStart: Int = -1,
    val quoteEnd: Int = -1,
) {
    fun preview(): String = when {
        deleted -> "Сообщение удалено"
        text.isNotBlank() -> text
        else -> "Сообщение"
    }
}

data class RopeGroup(
    val groupId: String,
    val name: String,
    val epoch: Int,
    val members: List<String>,
    val createdBy: String = "",
)

data class Conversation(
    val id: String,
    val title: String,
    val subtitle: String,
    val isGroup: Boolean,
    val online: Boolean,
    val last: ChatMessage?,
    val peer: DirectoryDevice? = null,
    val group: RopeGroup? = null,
    val pinned: Boolean = false,
    val muted: Boolean = false,
    val unread: Int = 0,
)

enum class CallPhase { RINGING_IN, RINGING_OUT, ACTIVE, ENDED }

data class CallInfo(
    val callId: String,
    val peerDeviceId: String,
    val peerName: String,
    val outgoing: Boolean,
    val phase: CallPhase,
    val payload: String = "",
    val media: String = "",
    val link: CallLinkState = CallLinkState.RINGING,
    val hasTurn: Boolean = false,
    val iceReady: Boolean = false,
    val lastIce: String = "",
    val startedAtMs: Long = 0L,
    val video: Boolean = false,
)

object ChatIds {
    const val GROUP_PREFIX = "g:"
    const val SAVED = SavedMessagesRules.ID

    fun group(groupId: String): String = GROUP_PREFIX + groupId

    fun isGroup(id: String): Boolean = id.startsWith(GROUP_PREFIX)

    fun isSaved(id: String?): Boolean = SavedMessagesRules.isSaved(id)

    fun rawGroupId(id: String): String = id.removePrefix(GROUP_PREFIX)

    fun isOpenableGroup(id: String): Boolean {
        if (!isGroup(id)) return false
        return JsonIds.optional(rawGroupId(id)) != null
    }
}

/** Android org.json.optString(JSONObject.NULL) returns the literal "null". */
object JsonIds {
    fun optional(raw: String?): String? {
        val v = raw?.trim().orEmpty()
        if (v.isEmpty() || v.equals("null", ignoreCase = true)) return null
        return v
    }
}

object ChatRouting {
    fun mediaChatId(groupId: String?, senderDeviceId: String, knownGroups: Set<String>): String {
        val gid = JsonIds.optional(groupId)
        return if (gid != null && gid in knownGroups) ChatIds.group(gid) else senderDeviceId
    }

    fun showLeftoverThread(id: String): Boolean = !ChatIds.isGroup(id) && !ChatIds.isSaved(id)
}

/** Device / member ids on the wire are 64-hex or UUID; compare them case-insensitively. */
object PeerIds {
    fun normalize(raw: String?): String = JsonIds.optional(raw)?.lowercase().orEmpty()

    fun same(a: String?, b: String?): Boolean {
        val x = normalize(a)
        val y = normalize(b)
        return x.isNotBlank() && x == y
    }

    fun looksLikeDevice(id: String?): Boolean {
        val v = normalize(id)
        return v.length == 64 && v.all { it in '0'..'9' || it in 'a'..'f' }
    }

    fun findDevice(devices: List<DirectoryDevice>, id: String?): DirectoryDevice? {
        val n = normalize(id)
        if (n.isBlank()) return null
        return devices.find { same(it.deviceId, n) }
    }

    fun devicesForMember(devices: List<DirectoryDevice>, memberId: String?): List<DirectoryDevice> {
        val n = normalize(memberId)
        if (n.isBlank()) return emptyList()
        return devices.filter { same(it.memberId, n) }
    }

    fun preferReachable(candidates: List<DirectoryDevice>, onlineIds: Set<String> = emptySet()): DirectoryDevice? {
        if (candidates.isEmpty()) return null
        val online = onlineIds.map { normalize(it) }.toSet()
        return candidates.find { it.online || normalize(it.deviceId) in online } ?: candidates.first()
    }

    /**
     * Map a chat hint (device id, member id, or leftover stub) to the device we should ring.
     * Never returns a group id. Prefers an online device of the same member.
     */
    fun resolve(
        devices: List<DirectoryDevice>,
        hint: DirectoryDevice?,
        rawId: String?,
        onlineIds: Set<String> = emptySet(),
    ): DirectoryDevice? {
        if (ChatIds.isGroup(rawId.orEmpty()) || ChatIds.isGroup(hint?.deviceId.orEmpty())) return null
        if (ChatIds.isSaved(rawId) || ChatIds.isSaved(hint?.deviceId)) return null
        findDevice(devices, rawId)?.let { return it }
        preferReachable(devicesForMember(devices, rawId), onlineIds)?.let { return it }
        if (hint == null) return null
        val hintMatches = rawId.isNullOrBlank() ||
            same(hint.deviceId, rawId) ||
            same(hint.memberId, rawId)
        if (!hintMatches) return null
        findDevice(devices, hint.deviceId)?.let { return it }
        val memberKey = hint.memberId.ifBlank { hint.deviceId }
        preferReachable(devicesForMember(devices, memberKey), onlineIds)?.let { return it }
        if (hint.publicIdentity.isNotEmpty() && looksLikeDevice(hint.deviceId)) return hint
        return null
    }

    fun wireId(peer: DirectoryDevice?, fallback: String?): String {
        val fromPeer = normalize(peer?.deviceId)
        if (fromPeer.isNotBlank()) return fromPeer
        return normalize(fallback)
    }
}

enum class ThemeMode { LIGHT, DARK }
